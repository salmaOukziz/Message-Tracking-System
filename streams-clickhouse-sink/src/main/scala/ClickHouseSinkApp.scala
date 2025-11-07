package com.message.streaming

import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala.serialization.Serdes._
import play.api.libs.json._
import ru.yandex.clickhouse.ClickHouseDataSource
import java.util.Properties
import java.sql.PreparedStatement
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime

case class MessageEvent(
  message_id: String,
  conversation_id: String,
  sender_id: String,
  receiver_id: String,
  content: String,
  timestamp: String,
  message_type: String,
  user_id: String = "",
  event_type: String = "message_sent"
)

case class MessageViewEvent(
  message_id: String,
  user_id: String,
  viewed_at: String,
  device: String = "web"
)

object ClickHouseSinkApp extends App {
  println("Starting Enhanced ClickHouse Sink with Message Views...")
  
  val props = new Properties()
  props.put(StreamsConfig.APPLICATION_ID_CONFIG, "clickhouse-sink-enhanced")
  props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092")
  props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, "exactly_once_v2")

  val builder = new StreamsBuilder()
  
    // Corrected ClickHouse configuration
  val dataSource = new ClickHouseDataSource(
    "jdbc:clickhouse://clickhouse:8123/message_db?user=admin&password=password"
  )

  // Formatter for timestamps
  val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

  // Process normal messages
  builder.stream[String, String]("chat.messages")
    .foreach { (key, value) =>
      try {
        println(s"Processing message: ${value.take(100)}...")
        val message = parseMessage(value)
        saveMessageToClickHouse(message)
        println(s"SUCCESS: Stored message ${message.message_id}")
      } catch {
        case e: Exception => 
          println(s"ERROR processing message: ${e.getMessage}")
          e.printStackTrace()
      }
    }

  // Message views
  builder.stream[String, String]("message-views")
    .foreach { (key, value) =>
      try {
        println(s"👀 Processing message view: $value")
        val view = parseMessageView(value)
        updateMessageAsRead(view)
        println(s"SUCCESS: Message ${view.message_id} marked as read by ${view.user_id}")
      } catch {
        case e: Exception => 
          println(s"ERROR processing message view: ${e.getMessage}")
          e.printStackTrace()
      }
    }

  val streams = new KafkaStreams(builder.build(), props)
  
  streams.setStateListener((newState, oldState) => {
    println(s"Kafka Streams State changed: $oldState -> $newState")
  })
  
  streams.setUncaughtExceptionHandler((thread, exception) => {
    println(s"Uncaught exception in thread $thread: ${exception.getMessage}")
    exception.printStackTrace()
  })

  // Start streams
  streams.start()
  println("Enhanced ClickHouse Sink Started successfully!")

  // Shutdown hook
  sys.addShutdownHook {
    println("Shutting down ClickHouse Sink...")
    streams.close()
  }

  private def parseMessage(json: String): MessageEvent = {
    val parsed = Json.parse(json)
    MessageEvent(
      message_id = (parsed \ "message_id").as[String],
      conversation_id = (parsed \ "conversation_id").as[String],
      sender_id = (parsed \ "sender_id").as[String],
      receiver_id = (parsed \ "receiver_id").as[String],
      content = (parsed \ "content").as[String],
      timestamp = (parsed \ "timestamp").as[String],
      message_type = (parsed \ "message_type").as[String],
      user_id = (parsed \ "user_id").asOpt[String].getOrElse(""),
      event_type = (parsed \ "event_type").asOpt[String].getOrElse("message_sent")
    )
  }

  private def parseMessageView(json: String): MessageViewEvent = {
    val parsed = Json.parse(json)
    MessageViewEvent(
      message_id = (parsed \ "message_id").as[String],
      user_id = (parsed \ "user_id").as[String],
      viewed_at = (parsed \ "viewed_at").as[String],
      device = (parsed \ "device").asOpt[String].getOrElse("web")
    )
  }

  private def saveMessageToClickHouse(event: MessageEvent): Unit = {
    val connection = dataSource.getConnection()
    try {
      val sql = """
        INSERT INTO messages 
        (message_id, conversation_id, sender_id, receiver_id, content, created_at, message_type, user_id, event_type, read_status) 
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'sent')
      """
      
      val statement = connection.prepareStatement(sql)
      statement.setString(1, event.message_id)
      statement.setString(2, event.conversation_id)
      statement.setString(3, event.sender_id)
      statement.setString(4, event.receiver_id)
      statement.setString(5, event.content)
      
    // Robust timestamp management
      val timestamp = try {
        LocalDateTime.parse(event.timestamp.replace("Z", ""), formatter)
      } catch {
        case _: Exception => LocalDateTime.now()
      }
      statement.setObject(6, timestamp)
      
      statement.setString(7, event.message_type)
      statement.setString(8, if (event.user_id.nonEmpty) event.user_id else event.sender_id)
      statement.setString(9, event.event_type)
      
      val result = statement.executeUpdate()
      println(s"Inserted $result row(s) for message ${event.message_id}")
      statement.close()
    } catch {
      case e: Exception =>
        println(s"DATABASE ERROR in saveMessage: ${e.getMessage}")
        throw e
    } finally {
      connection.close()
    }
  }

  private def updateMessageAsRead(view: MessageViewEvent): Unit = {
    val connection = dataSource.getConnection()
    try {
      val updateSql = """
        ALTER TABLE messages 
        UPDATE read_status = 'read', read_at = ?
        WHERE message_id = ?
      """
      
      val statement = connection.prepareStatement(updateSql)
      
      // Robust view timestamp management
      val viewedAt = try {
        LocalDateTime.parse(view.viewed_at.replace("Z", ""), formatter)
      } catch {
        case _: Exception => LocalDateTime.now()
      }
      statement.setObject(1, viewedAt)
      statement.setString(2, view.message_id)
      
      val rowsUpdated = statement.executeUpdate()
      println(s"Updated $rowsUpdated row(s) for message ${view.message_id}")
      statement.close()
      
      if (rowsUpdated == 0) {
        println(s"No message found with ID: ${view.message_id}")
      }
    } catch {
      case e: Exception =>
        println(s"DATABASE ERROR in updateMessageAsRead: ${e.getMessage}")
        throw e
    } finally {
      connection.close()
    }
  }
}
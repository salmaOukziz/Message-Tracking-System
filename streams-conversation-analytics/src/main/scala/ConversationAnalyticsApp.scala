package com.message.streaming

import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala.serialization.Serdes._
import org.apache.kafka.streams.kstream.{TimeWindows, Suppressed}
import java.time.Duration
import java.util.Properties
import play.api.libs.json._

object ConversationAnalyticsApp extends App {
  println("Starting Conversation Analytics...")
  
  val props = new Properties()
  props.put(StreamsConfig.APPLICATION_ID_CONFIG, "conversation-analytics-v1")
  props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092")

  val builder = new StreamsBuilder()

  builder.stream[String, String]("chat.messages")
    .map((_, value) => {
      val json = Json.parse(value)
      val conversationId = (json \ "conversation_id").as[String]
      (conversationId, 1L)
    })
    .groupByKey
    .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(30)))
    .count()
    .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
    .toStream
    .foreach { (windowedKey, count) =>
      println(s" ${windowedKey.key()}: $count messages/30s")
    }

  val streams = new KafkaStreams(builder.build(), props)
  streams.start()

  println("Conversation Analytics Started!")
}
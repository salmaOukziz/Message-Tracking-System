file://<WORKSPACE>/streams-scala/src/main/scala/com/message/streaming/RealTimeAnalytics.scala
empty definition using pc, found symbol in pc: 
semanticdb not found
empty definition using fallback
non-local guesses:
	 -org/apache/kafka/streams/scala/ImplicitConversions.builder.
	 -org/apache/kafka/streams/scala/serialization/Serdes.builder.
	 -builder.
	 -scala/Predef.builder.
offset: 949
uri: file://<WORKSPACE>/streams-scala/src/main/scala/com/message/streaming/RealTimeAnalytics.scala
text:
```scala
// streams-scala/src/main/scala/com/message/streaming/RealTimeAnalytics.scala
package com.message.streaming

import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala.serialization.Serdes._
import org.apache.kafka.streams.kstream.{TimeWindows, Printed}
import java.time.Duration
import java.util.Properties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

class RealTimeAnalytics {
  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)  // ← CORRECTION: sans parenthèses

  def start(props: Properties): Unit = {
    val builder = new StreamsBuilder()
    
    calculateUserEngagement(builder)
    trackMessageTopics(builder)
    monitorSystemHealth(builder)

    val streams = new KafkaStreams(bui@@lder.build(), props)
    streams.start()
    
    println("✅ RealTimeAnalytics started!")
  }

  private def calculateUserEngagement(builder: StreamsBuilder): Unit = {
    builder.stream[String, String]("messages")
      .groupBy((_, value) => extractUserId(value))
      .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
      .count()
      .toStream
      .foreach { (windowedKey, count) =>
        println(s"📊 User ${windowedKey.key()} sent $count messages in 5min")
      }
  }

  private def trackMessageTopics(builder: StreamsBuilder): Unit = {
    builder.stream[String, String]("messages")
      .mapValues { value =>
        try {
          val message = mapper.readValue(value, classOf[MessageEvent])
          categorizeTopic(message.content)
        } catch {
          case e: Exception => "unknown"
        }
      }
      .groupBy((_, topic) => topic)
      .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(3)))
      .count()
      .toStream
      .foreach { (windowedKey, count) =>
        println(s"🏷️  Topic '${windowedKey.key()}': $count messages in 3min")
      }
  }

  private def monitorSystemHealth(builder: StreamsBuilder): Unit = {
    // Monitorer le débit des messages
    builder.stream[String, String]("messages")
      .groupBy((_, _) => "total_messages")
      .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
      .count()
      .toStream
      .foreach { (_, count) =>
        println(s"📈 System: $count messages/min")
      }
  }

  private def extractUserId(json: String): String = {
    try {
      json.split("\"user_id\":\"")(1).split("\"")(0)
    } catch {
      case e: Exception => "unknown"
    }
  }

  private def categorizeTopic(text: String): String = {
    val lower = text.toLowerCase
    if (lower.contains("meeting") || lower.contains("project") || lower.contains("work")) "work"
    else if (lower.contains("lunch") || lower.contains("dinner") || lower.contains("food")) "food"
    else if (lower.contains("movie") || lower.contains("netflix") || lower.contains("game")) "entertainment"
    else if (lower.contains("kafka") || lower.contains("scala") || lower.contains("programming")) "tech"
    else "general"
  }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 
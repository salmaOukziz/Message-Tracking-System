error id: file://<WORKSPACE>/streams-trend-detection/src/main/scala/TrendDetectionApp.scala:
file://<WORKSPACE>/streams-trend-detection/src/main/scala/TrendDetectionApp.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -org/apache/kafka/streams/scala/ImplicitConversions.content.toLowerCase.
	 -org/apache/kafka/streams/scala/ImplicitConversions.content.toLowerCase#
	 -org/apache/kafka/streams/scala/ImplicitConversions.content.toLowerCase().
	 -org/apache/kafka/streams/scala/serialization/Serdes.content.toLowerCase.
	 -org/apache/kafka/streams/scala/serialization/Serdes.content.toLowerCase#
	 -org/apache/kafka/streams/scala/serialization/Serdes.content.toLowerCase().
	 -play/api/libs/json/content/toLowerCase.
	 -play/api/libs/json/content/toLowerCase#
	 -play/api/libs/json/content/toLowerCase().
	 -content/toLowerCase.
	 -content/toLowerCase#
	 -content/toLowerCase().
	 -scala/Predef.content.toLowerCase.
	 -scala/Predef.content.toLowerCase#
	 -scala/Predef.content.toLowerCase().
offset: 888
uri: file://<WORKSPACE>/streams-trend-detection/src/main/scala/TrendDetectionApp.scala
text:
```scala
package com.message.streaming

import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala.serialization.Serdes._
import org.apache.kafka.streams.kstream.{TimeWindows, Suppressed}
import java.time.Duration
import java.util.Properties
import play.api.libs.json._

object TrendDetectionApp extends App {
  println("🚀 Starting Trend Detection...")
  
  val props = new Properties()
  props.put(StreamsConfig.APPLICATION_ID_CONFIG, "trend-detection-v1")
  props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092")

  val builder = new StreamsBuilder()

  builder.stream[String, String]("chat.messages")
    .flatMapValues { value =>
      val json = Json.parse(value)
      val content = (json \ "content").as[String]
      content.@@toLowerCase()
        .split("\\W+")
        .filter(_.length > 3)
        .distinct
        .toList
    }
    .groupBy((_, word) => word)
    .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(2)))
    .count()
    .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
    .toStream
    .filter((_, count) => count > 2)
    .foreach { (windowedKey, count) =>
      println(s"🔥 '${windowedKey.key()}' : $count occurrences/2min")
    }

  val streams = new KafkaStreams(builder.build(), props)
  streams.start()

  println("✅ Trend Detection Started!")
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 
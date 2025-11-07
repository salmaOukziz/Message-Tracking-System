package com.message.streaming

import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala.serialization.Serdes._
import org.apache.kafka.streams.kstream.{TimeWindows, Suppressed}
import java.time.Duration
import java.util.Properties
import play.api.libs.json._

object AnomalyDetectionApp extends App {
  println("Starting Anomaly Detection...")
  
  val props = new Properties()
  props.put(StreamsConfig.APPLICATION_ID_CONFIG, "anomaly-detection-v1")
  props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092")

  val builder = new StreamsBuilder()

  builder.stream[String, String]("chat.messages")
    .map((_, value) => {
      val json = Json.parse(value)
      val userId = (json \ "sender_id").as[String]
      (userId, 1L)
    })
    .groupByKey
    .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
    .count()
    .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
    .toStream
    .filter((_, count) => count > 8)
    .foreach { (windowedKey, count) =>
      println(s"SPAM: ${windowedKey.key()} sent $count messages/1min")
    }

  val streams = new KafkaStreams(builder.build(), props)
  streams.start()

  println(" Anomaly Detection Started!")
}
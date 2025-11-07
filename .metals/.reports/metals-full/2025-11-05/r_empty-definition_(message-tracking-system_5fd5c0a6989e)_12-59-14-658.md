error id: file://<WORKSPACE>/streams-scala/src/main/scala/com/message/streaming/TrendDetector.scala:StreamsBuilder
file://<WORKSPACE>/streams-scala/src/main/scala/com/message/streaming/TrendDetector.scala
empty definition using pc, found symbol in pc: 
semanticdb not found

found definition using fallback; symbol StreamsBuilder
offset: 1006
uri: file://<WORKSPACE>/streams-scala/src/main/scala/com/message/streaming/TrendDetector.scala
text:
```scala
// streams-scala/src/main/scala/com/message/streaming/TrendDetector.scala
package com.message.streaming

import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala.serialization.Serdes._
import org.apache.kafka.streams.kstream.{TimeWindows, Suppressed}
import java.time.Duration
import java.util.Properties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

case class MessageEvent(message_id: String, user_id: String, content: String, 
                       timestamp: String, event_type: String)
case class TrendEvent(keyword: String, count: Long, trend_score: Double)

class TrendDetector {
  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)  // ← CORRECTION: sans parenthèses

  def start(props: Properties): Unit = {
    val builder = new StreamsBui@@lder()
    
    // Détection des mots tendance
    builder.stream[String, String]("messages")
      .flatMapValues { value =>
        try {
          val message = mapper.readValue(value, classOf[MessageEvent])
          extractKeywords(message.content).map(_.toLowerCase)
        } catch {
          case e: Exception => 
            println(s"❌ Error parsing message: ${e.getMessage}")
            List.empty[String]
        }
      }
      .groupBy((_, keyword) => keyword)
      .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(2)))
      .count()
      .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
      .toStream
      .map { (windowedKey, count) =>
        val trendScore = calculateTrendScore(count)
        (windowedKey.key(), TrendEvent(  // ← CORRECTION: retourner un tuple (key, value)
          keyword = windowedKey.key(),
          count = count,
          trend_score = trendScore
        ))
      }
      .filter((_, trend) => trend.trend_score > 0.5)
      .foreach { (keyword, trend) =>
        println(s"🚀 TRENDING: '$keyword' (score: ${trend.trend_score}, count: ${trend.count})")
      }

    val streams = new KafkaStreams(builder.build(), props)
    streams.start()
    
    println("✅ TrendDetector started!")
  }

  private def extractKeywords(text: String): List[String] = {
    val stopWords = Set("the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "a", "is", "this", "that")
    text.split("\\W+")
      .filter(_.length > 3)
      .filterNot(stopWords.contains)
      .filter(_.forall(_.isLetter))
      .toList
  }

  private def calculateTrendScore(count: Long): Double = {
    count match {
      case c if c > 10 => 1.0
      case c if c > 5 => 0.7
      case c if c > 2 => 0.5
      case _ => 0.0
    }
  }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 
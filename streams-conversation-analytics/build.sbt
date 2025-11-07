name := "streams-conversation-analytics"
version := "1.0"
scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka-streams" % "3.5.0",
  "org.apache.kafka" %% "kafka-streams-scala" % "3.5.0",
  "com.typesafe" % "config" % "1.4.2",
  "org.slf4j" % "slf4j-simple" % "1.7.36",
  "com.typesafe.play" %% "play-json" % "2.9.4"
)
name := "streams-clickhouse-sink"
version := "1.0"
scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka-streams" % "3.5.0",
  "org.apache.kafka" %% "kafka-streams-scala" % "3.5.0",
  "com.typesafe" % "config" % "1.4.2",
  "ru.yandex.clickhouse" % "clickhouse-jdbc" % "0.3.2",
  "org.slf4j" % "slf4j-simple" % "1.7.36",
  "com.typesafe.play" %% "play-json" % "2.9.4"
)

// Stratégie pour résoudre les conflits d'assembly
ThisBuild / assemblyMergeStrategy := {
  case PathList("META-INF", "versions", "9", "module-info.class") => MergeStrategy.discard
  case PathList("module-info.class") => MergeStrategy.discard
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case PathList("META-INF", "DEPENDENCIES") => MergeStrategy.discard
  case PathList("META-INF", "LICENSE") => MergeStrategy.discard
  case PathList("META-INF", "LICENSE.txt") => MergeStrategy.discard
  case PathList("META-INF", "NOTICE") => MergeStrategy.discard
  case PathList("META-INF", "NOTICE.txt") => MergeStrategy.discard
  case PathList("META-INF", "services", _) => MergeStrategy.concat
  case PathList("META-INF", _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}
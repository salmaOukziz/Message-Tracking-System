FROM eclipse-temurin:11-jre

WORKDIR /app

# Copier le JAR assembly (nom différent)
COPY target/scala-2.13/streams-clickhouse-sink-assembly-*.jar /app/app.jar

CMD ["java", "-jar", "app.jar"]
FROM openjdk:25-jdk-slim
WORKDIR /app
COPY target/processor-1.0-SNAPSHOT.jar /app/processor.jar
CMD ["java", "-jar", "processor.jar"]
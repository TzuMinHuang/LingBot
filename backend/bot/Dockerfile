FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/bot-0.0.1-SNAPSHOT.jar app.jar
#ENTRYPOINT ["java", "-jar", "app.jar"]
# 預設 ENTRYPOINT（支援環境變數傳入）
ENTRYPOINT ["sh", "-c", "java -jar app.jar"]
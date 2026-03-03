FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/seat-booking-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms128m -Xmx256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# ENTRYPOINT ["java", "-jar", "app.jar"]


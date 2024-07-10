# Stage 1: Build application
FROM maven:3.9.2-amazoncorretto-17 AS builder

WORKDIR /app
COPY . /app
# Install the local dependency JAR into the Maven repository
RUN mvn install:install-file \
    -Dfile=lib/vas-integrations-pseudonymisation-0.8.4-SNAPSHOT-jar-with-dependencies.jar \
    -DgroupId=be.smals.vas.integrations \
    -DartifactId=vas-integrations-pseudonymisation \
    -Dversion=0.8.4-SNAPSHOT \
    -Dpackaging=jar

RUN mvn clean install -DskipTests=true


# Stage 2: Run application
FROM eclipse-temurin:17-jre-jammy

RUN mkdir /app
COPY --from=builder /app/target/fhir-test-services.jar /app/app.jar
RUN sh -c 'touch /app/app.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Xmx2048m","-jar","/app/app.jar"]
EXPOSE 8181
WORKDIR /app

FROM eclipse-temurin:17-jre

RUN apt-get update && apt-get install -y curl socat && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the custom-built FHIR Validator CLI jar (includes /testdata, /matchetype, etc.)
COPY validator_cli.jar /app/validator.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=10s --retries=5 --start-period=120s \
  CMD curl -sf -X POST http://localhost:8081/validateResource \
    -H "Content-Type: application/fhir+json" -d '{"resourceType":"Patient"}' || exit 1

# Use tail -f /dev/null to provide a blocking stdin that never returns EOF
# The CLI's "Press any key to stop" reads stdin - EOF causes immediate shutdown
ENTRYPOINT ["sh", "-c", "socat TCP-LISTEN:8081,fork,reuseaddr,bind=0.0.0.0 TCP:127.0.0.1:8080 & tail -f /dev/null | java -Xmx2g -jar /app/validator.jar -version 4.0 -server 8080"]

# FHIR Validator Service

A Docker image wrapping the [HL7 FHIR Validator CLI](https://github.com/hapifhir/org.hl7.fhir.core) as a persistent HTTP server. It provides profile validation, test data generation, structural comparison, and FHIRPath evaluation -- all over HTTP, ready to plug into an [Interoperability Test Bed (ITB)](https://interoperable-europe.ec.europa.eu/collection/interoperability-test-bed-repository/solution/interoperability-test-bed) composition or any other environment that needs FHIR validation.

## Quick Start

Pull the image and add it to your `docker-compose.override.yml` (or `docker-compose.yml`):

```yaml
services:
  fhir-validator:
    image: costateixeira/fhir-validator:6.8.3
    container_name: fhir-validator
    restart: unless-stopped
    ports:
      - "8088:8080"
    environment:
      - JAVA_TOOL_OPTIONS=-Xmx2g
    healthcheck:
      test: ["CMD-SHELL", "curl -sf -X POST http://localhost:8080/validateResource -H 'Content-Type: application/fhir+json' -d '{\"resourceType\":\"Patient\"}' || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 120s
```

Then start it:

```bash
docker compose up -d fhir-validator
```

The service takes 60--120 seconds to load FHIR core definitions. Wait for the health check to pass (`docker compose ps` shows **healthy**) before making requests.

## What It Does

The validator exposes these endpoints on port 8080 (mapped to 8088 on the host by default):

| Endpoint | Purpose |
|----------|---------|
| `/validateResource` | Validate a FHIR resource against the base spec and/or specific profiles |
| `/loadIG` | Load an Implementation Guide (profiles, extensions, value sets) at runtime |
| `/testdata` | Generate conformant FHIR resources from a profile and a data table |
| `/matchetype` | Compare a resource against an expected pattern (wildcards supported) |
| `/fhirpath` | Evaluate FHIRPath expressions against a resource |
| `/convert` | Convert between JSON and XML formats |
| `/docs` | Swagger UI -- interactive API documentation with "Try it out" |

### Try it

Once the container is healthy, validate a minimal Patient:

```bash
curl -X POST http://localhost:8088/validateResource \
  -H "Content-Type: application/fhir+json" \
  -d '{"resourceType":"Patient"}'
```

Load a national Implementation Guide and validate against it:

```bash
# Load the Belgian Core IG
curl -X POST http://localhost:8088/loadIG \
  -H "Content-Type: application/json" \
  -d '{"ig":"hl7.fhir.be.core#2.1.2"}'

# Validate a Patient against the Belgian Patient profile
curl -X POST "http://localhost:8088/validateResource?profiles=https://www.ehealth.fgov.be/standards/fhir/core/StructureDefinition/be-patient" \
  -H "Content-Type: application/fhir+json" \
  -d '{"resourceType":"Patient","name":[{"family":"Dupont"}],"gender":"female"}'
```

## Using in ITB Test Cases

From inside the Docker network (e.g., in GITB TDL test cases), the service is reachable at `http://fhir-validator:8080` (container name + internal port):

```xml
<send id="loadIG" handler="HttpMessagingV2" from="User" to="FHIRValidator">
  <input name="uri">"http://fhir-validator:8080/loadIG"</input>
  <input name="method">"POST"</input>
  <input name="headers">$jsonHeaders</input>
  <input name="body">'{"ig":"hl7.fhir.be.core#2.1.2"}'</input>
</send>
```

## Building from Source

If you need to build the image locally instead of pulling it (e.g., to use a different validator JAR):

1. Place your `validator_cli.jar` in this directory
2. Build:
   ```bash
   docker build -t costateixeira/fhir-validator:6.8.3 .
   ```
3. Switch the compose service from `image:` back to `build:`:
   ```yaml
   fhir-validator:
     build: fhir-validator
     # image: costateixeira/fhir-validator:6.8.3
   ```

## Configuration

| Setting | Default | Notes |
|---------|---------|-------|
| FHIR version | R4 (`-version 4.0`) | Change in Dockerfile ENTRYPOINT for R5 |
| Java heap | 2 GB (`-Xmx2g`) | Increase via `JAVA_TOOL_OPTIONS` for large IGs |
| Server port | 8080 | Internal port; map to any host port |
| Start-up time | 60--120s | Health check `start_period` accounts for this |

## Further Reading

- [Usage Guide](../usage.md) -- full endpoint reference with examples, mapping syntax, and ITB test case patterns
- [HL7 FHIR Validation](https://hl7.org/fhir/validation.html) -- why the Validator is the most comprehensive validation method
- [FHIR Validator CLI source](https://github.com/hapifhir/org.hl7.fhir.core) -- upstream project

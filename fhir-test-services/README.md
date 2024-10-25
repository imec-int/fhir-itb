# Introduction

This project implements the supporting test services for the FHIR conformance testing. Currently, this provides an
implementation of a GITB messaging and validation services.

The pseudonymization service can be found in the `be.smals.vas.integration.helper` package under `fhir-pseudo-service`
module. For more information, please refer to the project's README.

# Services

The service `fhir-itb-service` is implemented in Java, using
the [Spring Boot framework](https://spring.io/projects/spring-boot).
It is built and packaged using [Apache Maven](https://maven.apache.org/), and also via Docker Compose.

## Pseudonymization Service

Implemented within the spring boot application's `fhir-pseudo-service`, its packages under `eu.europa.ec.fhir`:

- The `gitb` package contains the implementation of the GITB messaging and validation services.
- The `handlers` package implements the logic for various actions.
- The `state` package manages the ongoing state of test sessions.
- The `utils` package provides general supporting utilities.
- The `psudo` package includes a service for pseudonymizing patient information. Its properties are configured via a
  `config.properties` file, which links to a certificate file. The SSIN code can also be provided by the test case,
  taking precedence over the SSIN specified in the `config.properties` file.

A typical `config.properties` file **must** include the following properties, or an error will be returned by the
system. The values for these properties can be left empty:

- `domain.key`
- `client.id`
- `realm`
- `certificate.dir`
- `certificateFilePath`
- `certificatePassword`
- `identifier`
- `ehealth.from.header.value`
- `ssin`

## Proxy Service

The FHIR proxy service is implemented within the `fhir-itb-service` module.

The proxy service offers an HTTP endpoint that forwards requests to a FHIR server while triggering relevant test
cases in ITB.

To use, simply point a FHIR client to the proxy service instead of a real FHIR server and configure the
`fhir.proxy.endpoint` to that server.

### Configuration

- `itb.base_url`: The URL of the gitb-ui instance providing the automation API for starting test cases.
- `fhir.proxy.endpoint`: The URL of the FHIR server to proxy requests to.

## Prerequisites

The following prerequisites are required:

* To build: JDK 17+, Maven 3.8+.
* To run: JRE 17+.

## Building and running

1. Build using `mvn clean package`.
2. Once built, enter the submodule fhir-itb-service by `cd fhir-itb-service` you can run the application in two ways:\
   a. With maven: `mvn spring-boot:run`.  
   b. Standalone: `java -jar ./target/fhir-itb-service.jar`.
3. The services are available at:
   a. For the proxy messaging service: http://localhost:8181/fhir/services/messaging/proxy?wsdl  
   b. For the validation service: http://localhost:8181/fhir/services/validation/fhir?wsdl
4. For receiving calls from FHIR clients, the proxy services are exposed as follows:
   a. POST: http://localhost:8181/fhir/server/api/* (for
   example http://localhost:8181/fhir/server/api/AllergyIntolerance)

### Live reload for development

This project uses Spring Boot's live reloading capabilities. When running the application from your IDE or through
Maven, any change in classpath resources is automatically detected to restart the application.

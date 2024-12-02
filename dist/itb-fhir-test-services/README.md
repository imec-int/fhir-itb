# itb-fhir-test-services

The `itb-fhir-test-services` image packages the `fhir-itb-service` Spring
application that extends the ITB core
features.

The image
is [publicly available in Docker Hub](https://hub.docker.com/r/jungst46/itb-fhir-test-services).

## Building

To build and publish the image, follow the steps below:

1. Run the `docker build` command from the project's root
   folder context.
    ```shell
    docker build -f dist/itb-fhir-test-services/Dockerfile -t jungst46/itb-fhir-test-services:latest .
    ```
2. Push the image to the public Docker Hub repository.
    ```shell
    docker push jungst46/itb-fhir-test-services:latest
    ```
   > [!NOTE]
   > The image is currently hosted under a personal account (`jungst46`).
   > We should push it to an organization in DockerHub instead.

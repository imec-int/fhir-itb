# GITB FHIR Sandbox Distribution

The GITB FHIR Sandbox is a repackaged version of the [GITB UI](https://hub.docker.com/r/isaitb/gitb-ui) image
pre-configured with the FHIR community configuration, found in this repository.

# Building

This folder contains the `Dockerfile` for building the `gitb-ui-fhir-sandbox` image.

The image is [publicly available in Docker Hub](https://hub.docker.com/r/jungst46/gitb-ui-fhir-sandbox).

> [!NOTE]
> The image is currently hosted under a personal account (`jungst46`).
> We should push it to an organization in DockerHub instead.

If changes are made to the gitb configuration or a new version of gitb-ui is used, we must
rebuild this image.

To rebuild the image, follow the steps below:

1. Extract the "community" configuration from GITB UI and place it in the "config/data" directory.
    + more information here

2. Run the `docker build` command from the project's root folder.
    ```shell
    docker build -f dist/Dockerfile -t jungst46/gitb-ui-fhir-sandbox:latest .
    ```     

3. Push the image to the public Docker Hub repository.
    ```shell
    docker push jungst46/gitb-ui-fhir-sandbox:latest
    ```

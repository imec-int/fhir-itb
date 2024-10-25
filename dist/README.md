# Distribution images

This folder contains the Dockerfiles required to build the necessary images for the distribution of the ITB FHIR
Testing Sandbox.

Read more each image in their respective subfolder.

# Running the Sandbox

To run the sandbox, simply use `docker compose` to start the services.

```shell
# from the project root folder
docker compose -f dist/docker-compose.yml up -d
```

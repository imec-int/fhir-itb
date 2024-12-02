# gitb-ui-fhir-sandbox

The gitb-ui-fhir-sandbox is a repackaged version of
the [GITB UI](https://hub.docker.com/r/isaitb/gitb-ui) image
pre-configured with the FHIR community configuration.

The image
is [publicly available in Docker Hub](https://hub.docker.com/r/jungst46/gitb-ui-fhir-sandbox).

> [!WARNING]
> If changes are made to the gitb configuration or a new version of `gitb-ui`
> image is needed, we must build and publish a new version of this image.
>
> See [here](../../test-suites/README.md) for more information.

# Sandbox Configuration

The GITB Sandbox environment comes pre-configured with everything we need to run
tests against FHIR servers and clients.

## Users

Currently, there are three users configured.

+ admin@itb: The root admin user.
    + Password: `Admin.123!`
+ vendor_admin: The admin user for the "vendor" organization.
    + Password: `Fhir_Admin.123!`
+ vendor_user: The test user for the "vendor" organization.
    + Password: `Fhir.123!`

# Building

To build the image, follow the steps below:

1. Run the `docker build` command from the project's root folder context.
    ```shell
    docker build -f dist/gitb-ui-fhir-sandbox/Dockerfile -t jungst46/gitb-ui-fhir-sandbox:latest .
    ```     
2. (optional) When ready to publish, push the image to the public Docker Hub
   repository.
    ```shell
    docker push jungst46/gitb-ui-fhir-sandbox:latest
    ```
   > [!NOTE]
   > The image is currently hosted under a personal account (`jungst46`).
   > We should push it to an organization in DockerHub instead.

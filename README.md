# ITB FHIR Test Services

This project provides a sandbox environment for testing FHIR server and client application through the Interoperability
Test Bed (ITB).

More information on the `fhir-itb-services`, can be found in
the [fhir-itb-services/README.md](fhir-itb-services/README.md) file.

Information on ITB can be found at https://www.itb.ec.europa.eu/.

## Development

During **development**, one should run the sandbox using the root `docker-compose.yml`.

E.g.:

```shell
docker compose up 
```

To include the `fhir-test-services` image, add the `full` profile:

```shell
docker compose --profiles full up 
```

# Distribution

The sandbox is distributed as a set of docker images that can be run using the docker-compose file located in the
`dist` folder.

Read more about building the distribution images in the [dist/README.md](dist/README.md) file.

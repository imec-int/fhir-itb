# Repackages the GITB UI image to include the FHIR configuration.
FROM isaitb/gitb-ui:1.24.0

COPY config/data /gitb-repository/data/in/

ENV DATA_ARCHIVE_KEY fhir
ENV AUTOMATION_API_ENABLED true
ENV THEME fhir

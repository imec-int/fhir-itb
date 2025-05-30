package eu.europa.ec.fhir.gitb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitb.core.LogLevel;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.BAR;
import com.gitb.tr.ObjectFactory;
import com.gitb.tr.TAR;
import com.gitb.tr.TestAssertionGroupReportsType;
import com.gitb.tr.TestAssertionReportType;
import com.gitb.tr.TestResultType;
import com.gitb.tr.ValidationCounters;
import com.gitb.vs.GetModuleDefinitionResponse;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import com.gitb.vs.ValidationService;
import com.gitb.vs.Void;
import eu.europa.ec.fhir.utils.ITBUtils;
import jakarta.annotation.Resource;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.ws.WebServiceContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of the GITB validation API to handle FHIR Resource Validation.
 */
@Component
public class FhirResourceValidationService implements ValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(FhirResourceValidationService.class);

    @Value("${fhir.validation.endpoint}")
    private String fhirValidationEndpoint;

    @Value("${fhir.contentTypeFull}")
    private String fhirContentType;

    @Autowired
    RestClient restClient;

    @Autowired
    private DeferredRequestMapper deferredRequests;

    @Autowired
    private TestBedNotifier testBedNotifier;
    @Autowired
    private ObjectMapper objectMapper;
    @Resource
    private WebServiceContext wsContext;
    private final ObjectFactory objectFactory = new ObjectFactory();

    /**
     * This method normally returns documentation on how the service is expected to be used. It is meaningful
     * to implement this if this service would be a published utility that other test developers would want to
     * query and reuse. As it is an internal service we can skip this and return an empty implementation.
     *
     * @param aVoid No parameters.
     * @return The result.
     */
    @Override
    public GetModuleDefinitionResponse getModuleDefinition(Void aVoid) {
        return new GetModuleDefinitionResponse();
    }

    /**
     * Validate the provided inputs and produce a validation report.
     * <p>
     * To handle the validation we delegate to the configured FHIR server by making a
     * validation POST and retrieving the report.
     * Alternatively, this could be replaced by a call to the FHIR validator as a Java library.
     *
     * @param validateRequest The inputs to validate.
     * @return The response.
     */
    @Override
    public ValidationResponse validate(ValidateRequest validateRequest) {
        ValidationResponse response = new ValidationResponse();
        String resourceType = ITBUtils.getRequiredString(validateRequest.getInput(), "resource");
        String payload = ITBUtils.getRequiredString(validateRequest.getInput(), "payload");
        URI uri = URI.create(String.format("%s/%s/$validate", fhirValidationEndpoint, resourceType));
        String sessionId = validateRequest.getSessionId();

        var deferredRequest = deferredRequests.get(sessionId);

        if (deferredRequest.isPresent()) {
            var requestParams = deferredRequest.get().getRequestParams();
            try {
                var result = restClient.method(HttpMethod.POST)
                        .uri(uri)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                Objects.requireNonNull(
                                        requestParams.headers()
                                                .get(HttpHeaders.AUTHORIZATION)
                                        )
                                        .toArray(new String[0])
                        )
                        .contentType(MediaType.valueOf(fhirContentType))
                        .body(payload)
                        .retrieve()
                        .body(String.class);

                // Convert the FHIR server's validation report to a TAR validation report expected by the Test Bed.
                TAR report = convertToTestBedReport(validateRequest.getSessionId(), payload, result);
                response.setReport(report);
            } catch (Exception requestException) {
                // The validation call resulted in a failure.
                response.setReport(ITBUtils.createReport(TestResultType.FAILURE));
                var addr = ITBUtils.getReplyToAddressFromHeaders(wsContext);

                if (addr.isPresent()) {
                    try {
                        testBedNotifier.sendLogMessage(
                                sessionId,
                                addr.get(),
                                "Validation call to FHIR server failed.",
                                LogLevel.ERROR);
                    } catch (Exception logException) {
                        LOG.warn("Error while sending log message to Test Bed for session [{}]", sessionId, logException);
                    }
                } else {
                    LOG.warn("Missing \"reply-to\" address in validation service request headers.");
                }
            }
        }

        return response;
    }

    /**
     * Convert the FHIR server's validation report to a TAR validation report expected by the Test Bed.
     *
     * @param testSessionId      The test session ID (used only for logging).
     * @param validationRequest  The validation request (to be placed in the report's context).
     * @param validationResponse The response produced by the internal FHIR server.
     * @return The TAR validation report.
     */
    private TAR convertToTestBedReport(String testSessionId, String validationRequest, String validationResponse) {
        TAR report = ITBUtils.createReport(TestResultType.SUCCESS);
        // Add the validated input and produced "raw" validation reports as context items.
        report.getContext()
                .getItem()
                .add(ITBUtils.createAnyContent("input", validationRequest, ValueEmbeddingEnumeration.STRING, MediaType.APPLICATION_JSON_VALUE));
        report.getContext()
                .getItem()
                .add(ITBUtils.createAnyContent("report", validationResponse, ValueEmbeddingEnumeration.STRING, MediaType.APPLICATION_JSON_VALUE));
        // Parse the reported issues to convert them to report items.
        try {
            var root = objectMapper.readTree(validationResponse);
            var issues = root.get("issue");
            if (!issues.isMissingNode() && issues.isArray() && !issues.isEmpty()) {
                // Iterate issues and extract report items.
                report.setReports(new TestAssertionGroupReportsType());
                List<JAXBElement<TestAssertionReportType>> errors = new ArrayList<>();
                List<JAXBElement<TestAssertionReportType>> warnings = new ArrayList<>();
                List<JAXBElement<TestAssertionReportType>> infoMessages = new ArrayList<>();
                for (int i = 0; i < issues.size(); i++) {
                    var issue = issues.get(i);
                    if (!issue.isMissingNode()) {
                        var severity = getTextNode(issue, "severity");
                        var description = getTextNode(issue, "diagnostics");
                        if (description.isPresent() && severity.isPresent()) {
                            var reportItem = new BAR();
                            reportItem.setDescription(description.get());
                            /*
                             * You can set line numbers on report items, referring also to the name of the report context item
                             * that corresponds to the relevant content ("input" in this case). Apart from including this
                             * in XML and PDF reports, this allows the Test Bed's UI to remove up the specific content and
                             * location when the item in question is clicked.
                             */
                            var lineNumber = getLineNumber(issue);
                            reportItem.setLocation("input:%s:0".formatted(lineNumber.orElse(0)));
                            if ("error".equals(severity.get())) {
                                errors.add(objectFactory.createTestAssertionGroupReportsTypeError(reportItem));
                            } else if ("warning".equals(severity.get())) {
                                warnings.add(objectFactory.createTestAssertionGroupReportsTypeWarning(reportItem));
                            } else {
                                infoMessages.add(objectFactory.createTestAssertionGroupReportsTypeInfo(reportItem));
                            }
                        }
                    }
                }
                report.getReports().getInfoOrWarningOrError().addAll(errors);
                report.getReports().getInfoOrWarningOrError().addAll(warnings);
                report.getReports()
                        .getInfoOrWarningOrError()
                        .addAll(infoMessages);
                report.setCounters(new ValidationCounters());
                report.getCounters()
                        .setNrOfErrors(BigInteger.valueOf(errors.size()));
                report.getCounters()
                        .setNrOfWarnings(BigInteger.valueOf(warnings.size()));
                report.getCounters()
                        .setNrOfAssertions(BigInteger.valueOf(infoMessages.size()));
                if (!errors.isEmpty()) {
                    report.setResult(TestResultType.FAILURE);
                } else if (!warnings.isEmpty()) {
                    report.setResult(TestResultType.WARNING);
                }
            }
        } catch (JsonProcessingException e) {
            LOG.error("Unable to parse FHIR server validation response.", e);
            testBedNotifier.sendLogMessage(testSessionId, ITBUtils.getReplyToAddressFromHeaders(wsContext)
                    .orElseThrow(), "Unable to parse FHIR server validation response.", LogLevel.ERROR);
            report = ITBUtils.createReport(TestResultType.FAILURE);
        }
        return report;
    }

    /**
     * Get the text node child under the provided issue node.
     *
     * @param issue    The issue node.
     * @param nodeName The name of the child.
     * @return The child's text (if defined).
     */
    private Optional<String> getTextNode(JsonNode issue, String nodeName) {
        var node = issue.get(nodeName);
        if (!node.isMissingNode()) {
            return Optional.ofNullable(StringUtils.defaultIfBlank(node.asText(), null));
        }
        return Optional.empty();
    }

    /**
     * Get the line number from the provided issue node.
     *
     * @param issue The issue node.
     * @return The line number (if found).
     */
    private Optional<Integer> getLineNumber(JsonNode issue) {
        var extensions = issue.get("extension");
        if (!extensions.isMissingNode() && extensions.isArray()) {
            for (int i = 0; i < extensions.size(); i++) {
                var extension = extensions.get(0);
                if ("http://hl7.org/fhir/StructureDefinition/operationoutcome-issue-line".equals(extension.get("url")
                        .asText())) {
                    return Optional.of(extension.get("valueInteger").asInt());
                }
            }
        }
        return Optional.empty();
    }

}

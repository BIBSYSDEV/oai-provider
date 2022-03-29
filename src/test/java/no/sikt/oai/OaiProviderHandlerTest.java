package no.sikt.oai;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import static no.sikt.oai.RestApiConfig.restServiceObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OaiProviderHandlerTest {

    public static final String BLANK = " ";
    private OaiProviderHandler handler;
    private Environment environment;
    private Context context;

    @BeforeEach
    public void init() {
        environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        context = mock(Context.class);
        handler = new OaiProviderHandler(environment);
    }

    @Test
    public void handleRequestReturnsOaiResponse() throws IOException {
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.Identify.name());
        handler.handleRequest(handlerInputStream(queryParameters), output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertEquals(Verb.Identify.name(), responseBody);
    }

    @Test
    public void shouldReturnBadRequestWithUnknownVerb() throws IOException {
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, "UnknownVerb");
        handler.handleRequest(handlerInputStream(queryParameters), output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiProviderHandler.BAD_ARGUMENT)));
    }

    @Test
    public void shouldReturnBadRequestWithMissingVerb() throws IOException {
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, BLANK);
        handler.handleRequest(handlerInputStream(queryParameters), output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiProviderHandler.VERB_IS_MISSING)));
    }

    @Test
    public void shouldReturnBadRequestWhenListRecordsWithoutResumptionToken() throws IOException {
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiProviderHandler.METADATA_PREFIX_IS_A_REQUIRED)));
    }

    @Test
    public void shouldReturnBadRequestWhenGetRecordWithoutResumptionToken() throws IOException {
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.GetRecord.name());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiProviderHandler.METADATA_PREFIX_IS_A_REQUIRED)));
    }

    @Test
    public void shouldReturnBadRequestWhenRequestWithInvalidQueryParam() throws IOException {
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.GetRecord.name());
        queryParameters.put(randomString(),  randomString());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiProviderHandler.NOT_A_LEGAL_PARAMETER)));
    }

    @Test
    public void shouldReturnGetRecordResponseWhenAskedForGetRecordWithMetadataPrefix() throws IOException {
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.GetRecord.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, MetadatFormat.QDC.name());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertEquals(Verb.GetRecord.name(), responseBody);
    }

    @Test
    public void shouldReturnBadRequestWhenAskedForListRecordsWithNonExistingMetadataFormat() throws IOException {
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.GetRecord.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, randomString());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiProviderHandler.METADATA_FORMAT_NOT_SUPPORTED)));
    }

    @Test
    public void shouldReturnBadRequestWhenAskedForListRecordsWithNonExistingSetSpec() throws IOException {
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, MetadatFormat.OAI_DATACITE.name());
        queryParameters.put(ValidParameterKey.SET.key, randomString());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiProviderHandler.UNKNOWN_SET_NAME)));
    }

    @Test
    public void shouldReturnListRecordsWhenAskedForListRecordsWithExistingSetSpec() throws IOException {
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, MetadatFormat.OAI_DATACITE.name());
        queryParameters.put(ValidParameterKey.SET.key, "NTNU");
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertEquals(Verb.ListRecords.name(), responseBody);
    }

    @Test
    public void shouldReturnListRecordsResponseWhenAskedForListRecordsWithResumptionToken() throws IOException {
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.RESUMPTIONTOKEN.key, randomString());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, MetadatFormat.OAI_DATACITE.name());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertEquals(Verb.ListRecords.name(), responseBody);
    }

    @Test
    public void shouldReturnBadRequestWhenAskedWithInvalidFromParam() throws IOException {
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key,  randomString());
        queryParameters.put(ValidParameterKey.FROM.key,  randomString());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiProviderHandler.ILLEGAL_DATE_FROM)));
    }

    @Test
    public void shouldReturnBadRequestWhenAskedWithNullFromParam() throws IOException {
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, randomString());
        queryParameters.put(ValidParameterKey.FROM.key, BLANK);
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiProviderHandler.ILLEGAL_DATE_FROM)));
    }

    @Test
    public void shouldReturnBadRequestWhenAskedWithInvalidUntilParam() throws IOException {
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key,  randomString());
        queryParameters.put(ValidParameterKey.UNTIL.key,  randomString());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiProviderHandler.ILLEGAL_DATE_UNTIL)));
    }

    @Test
    public void shouldReturnBadRequestWhenAskedWithDifferentLengthFromUntilParam() throws IOException {
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key,  randomString());
        queryParameters.put(ValidParameterKey.FROM.key, "2006-06-06");
        queryParameters.put(ValidParameterKey.UNTIL.key, "2007-06-06T00:00:00Z");
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiProviderHandler.DIFFERENT_DATE_GRANULARITIES)));
    }

    @Test
    public void shouldReturnListRecordsResponseWhenAskedWithSameLengthFromUntilParam() throws IOException {
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key,  MetadatFormat.QDC.name());
        queryParameters.put(ValidParameterKey.FROM.key, "2006-06-06T00:00:00Z");
        queryParameters.put(ValidParameterKey.UNTIL.key, "2007-06-06T00:00:00Z");
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertEquals(Verb.ListRecords.name(), responseBody);
    }

    private InputStream handlerInputStream(Map<String, String> queryParameters) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(restServiceObjectMapper)
                .withHttpMethod("GET")
                .withQueryParameters(queryParameters)
                .build();
    }

    private GatewayResponse<String> parseSuccessResponse(String output) throws JsonProcessingException {
        var typeRef = restServiceObjectMapper.getTypeFactory()
                .constructParametricType(GatewayResponse.class, String.class);
        return restServiceObjectMapper.readValue(output, typeRef);
    }

}

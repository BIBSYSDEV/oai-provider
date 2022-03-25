package no.sikt.oai;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import static no.sikt.oai.OaiProviderHandler.QUERY_PARAM_METADATA_PREFIX;
import static no.sikt.oai.OaiProviderHandler.QUERY_PARAM_RESUMPTION_TOKEN;
import static no.sikt.oai.OaiProviderHandler.QUERY_PARAM_VERB;
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

    @AfterEach
    public void tearDown() {

    }

    @Test
    public void handleRequestReturnsOaiResponse() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(handlerInputStreamWithVerbQueryParameters(Verb.Identify.name()), output, context);
        GatewayResponse<String> gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        String responseBody = gatewayResponse.getBody();
        assertEquals(Verb.Identify.name(), responseBody);
    }

    @Test
    public void shouldReturnBadRequestWithUnknownVerb() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(handlerInputStreamWithVerbQueryParameters("Unknown verb"), output, context);
        GatewayResponse<String> gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
        String responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiProviderHandler.BAD_ARGUMENT)));
    }

    @Test
    public void shouldReturnBadRequestWithMissingVerb() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(handlerInputStreamWithVerbQueryParameters(BLANK), output, context);
        GatewayResponse<String> gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
        String responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiProviderHandler.VERB_IS_MISSING)));
    }

    @Test
    public void shouldReturnBadRequestWhenListRecordsWithoutResumptionToken() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(handlerInputStreamWithVerbQueryParameters(Verb.ListRecords.name()), output, context);
        GatewayResponse<String> gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
        String responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiProviderHandler.METADATA_PREFIX_IS_A_REQUIRED)));
    }

    @Test
    public void shouldReturnBadRequestWhenGetRecordWithoutResumptionToken() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(handlerInputStreamWithVerbQueryParameters(Verb.GetRecord.name()), output, context);
        GatewayResponse<String> gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
        String responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiProviderHandler.METADATA_PREFIX_IS_A_REQUIRED)));
    }

    @Test
    public void shouldReturnGetRecordResponseWhenAskedForGetRecordWithMetadataPrefix() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        InputStream inputStream = handlerInputStreamWithQueryParameters(Verb.GetRecord.name(),
                QUERY_PARAM_METADATA_PREFIX,  randomString() );
        handler.handleRequest(inputStream, output, context);
        GatewayResponse<String> gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        String responseBody = gatewayResponse.getBody();
        assertEquals(Verb.GetRecord.name(), responseBody);
    }

    @Test
    public void shouldReturnGetRecordResponseWhenAskedForGetRecordWithResumptionToken() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        InputStream inputStream = handlerInputStreamWithQueryParameters(Verb.GetRecord.name(),
                QUERY_PARAM_RESUMPTION_TOKEN,  randomString() );
        handler.handleRequest(inputStream, output, context);
        GatewayResponse<String> gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        String responseBody = gatewayResponse.getBody();
        assertEquals(Verb.GetRecord.name(), responseBody);
    }

    private InputStream handlerInputStreamWithVerbQueryParameters(String verb) throws JsonProcessingException {
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(QUERY_PARAM_VERB, verb);
        return new HandlerRequestBuilder<Void>(restServiceObjectMapper)
                .withHttpMethod("GET")
                .withQueryParameters(queryParameters)
                .build();
    }

    private InputStream handlerInputStreamWithQueryParameters(String verb, String key, String value) throws JsonProcessingException {
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(QUERY_PARAM_VERB, verb);
        queryParameters.put(key, value);
        return new HandlerRequestBuilder<Void>(restServiceObjectMapper)
                .withHttpMethod("GET")
                .withQueryParameters(queryParameters)
                .build();
    }

    private GatewayResponse<String> parseSuccessResponse(String output) throws JsonProcessingException {
        JavaType typeRef = restServiceObjectMapper.getTypeFactory()
                .constructParametricType(GatewayResponse.class, String.class);
        return restServiceObjectMapper.readValue(output, typeRef);
    }

}

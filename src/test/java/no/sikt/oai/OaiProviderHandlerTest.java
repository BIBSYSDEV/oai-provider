package no.sikt.oai;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static no.sikt.oai.MetadataFormat.OAI_DC;
import static no.sikt.oai.MetadataFormat.QDC;
import static no.sikt.oai.OaiConstants.BAD_ARGUMENT;
import static no.sikt.oai.OaiConstants.BAD_VERB;
import static no.sikt.oai.OaiConstants.CLIENT_NAME_ENV;
import static no.sikt.oai.OaiConstants.CLIENT_TYPE_DLR;
import static no.sikt.oai.OaiConstants.CLIENT_TYPE_NVA;
import static no.sikt.oai.OaiConstants.DIFFERENT_DATE_GRANULARITIES;
import static no.sikt.oai.OaiConstants.ID_DOES_NOT_EXIST;
import static no.sikt.oai.OaiConstants.ILLEGAL_DATE_FROM;
import static no.sikt.oai.OaiConstants.ILLEGAL_DATE_UNTIL;
import static no.sikt.oai.OaiConstants.METADATA_FORMAT_NOT_SUPPORTED;
import static no.sikt.oai.OaiConstants.METADATA_PREFIX_IS_A_REQUIRED;
import static no.sikt.oai.OaiConstants.NOT_A_LEGAL_PARAMETER;
import static no.sikt.oai.OaiConstants.NO_RECORDS_MATCH;
import static no.sikt.oai.OaiConstants.NO_SET_HIERARCHY;
import static no.sikt.oai.OaiConstants.RECORDS_URI_ENV;
import static no.sikt.oai.OaiConstants.RECORD_URI_ENV;
import static no.sikt.oai.OaiConstants.SETS_URI_ENV;
import static no.sikt.oai.OaiConstants.UNKNOWN_SET_NAME;
import static no.sikt.oai.OaiConstants.VERB_IS_MISSING;
import static no.sikt.oai.RestApiConfig.restServiceObjectMapper;
import static no.sikt.oai.adapter.NvaAdapter.ERROR_UNEXPECTED_RESPONSE_FROM_DATA_SOURCE;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static nva.commons.apigateway.RestRequestHandler.EMPTY_STRING;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import no.sikt.oai.adapter.Adapter;
import no.sikt.oai.adapter.DlrAdapter;
import no.sikt.oai.adapter.NvaAdapter;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class OaiProviderHandlerTest {

    public static final String BLANK = " ";
    public static final String UNKNOWN_CLIENT_NAME = "Unknown client name";
    public static final String UNKNOWN_VERB = "UnknownVerb";
    public static final String FAKE_OAI_IDENTIFIER_NVA = "oai:nva.unit.no:"
                                                         + "018067600e39-9ed63653-a74b-454f-aab9-9a120d319b9f";
    public static final String INVALID_NVA_IDENTIFIER = "oai:nva.unit.no:9a1eae92-38bf-4002-a1a9-d21035242d";
    public static final String REAL_OAI_IDENTIFIER_DLR = "oai:dlr.unit.no:9a1eae92-38bf-4002-a1a9-d21035242d30";
    public static final String INVALID_DLR_IDENTIFIER = "oai:dlr.unit.no:9a1eae92-38bf-4002-a1a9-d21035242d30-36";
    public static final String VALID_DLR_IDENTIFIER = "oai:dlr.unit.no:00000000-0000-0000-0000-000000000000";
    public static final String FAULTY_JSON = "faultyJson";
    public static final String UUID_REGEX = "^/[^/]+/(?:[0-9a-f]{12}-)?[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0"
                                            + "-9a-f]{3}-[0-9a-f]{12}$";
    public static final String RESUMPTION_TOKEN = "lr~sikt~~~qdc~50";
    public static final String SET_NAME_SIKT = "sikt";
    public static final String EXCEPTION = "Exception";
    public static final String METADATA_TAG = "<metadata>";
    public static final String UIO_CUSTUMER_ID = "1bd2e3f7-a570-442a-b444-cb02e6cc70e4";
    private AuthorizedBackendClient authorizedBackendClient;
    private OaiProviderHandler handler;
    private Adapter adapter;
    private Environment environment;
    private Context context;
    private WireMockServer httpServer;
    private HttpClient httpClient;
    private URI serverUriSets;
    private URI serverUriRecord;
    private URI serverUriRecords;

    public void init(String adapterName) {
        environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        when(environment.readEnv(CLIENT_NAME_ENV)).thenReturn(adapterName);
        httpClient = WiremockHttpClient.create();
        authorizedBackendClient = new AuthorizedBackendClient(null, null, null) {
            @Override
            public <T> HttpResponse<T> send(HttpRequest.Builder request, BodyHandler<T> responseBodyHandler)
                throws IOException, InterruptedException {
                return httpClient.send(request.build(), responseBodyHandler);
            }
        };
        startWiremockServer(adapterName);
        when(environment.readEnv(SETS_URI_ENV)).thenReturn(serverUriSets.toString());
        when(environment.readEnv(RECORD_URI_ENV)).thenReturn(serverUriRecord.toString());
        when(environment.readEnv(RECORDS_URI_ENV)).thenReturn(serverUriRecords.toString());
        context = mock(Context.class);
        mockSetsResponse(adapterName);
        mockRecordResponse(adapterName);
        mockRecordsResponse(adapterName);
        createAdapter(adapterName, environment);
        handler = new OaiProviderHandler(environment, adapter);
    }

    @AfterEach
    public void tearDown() {
        if (httpServer != null && httpServer.isRunning()) {
            httpServer.stop();
        }
    }

    @Test
    public void handleRequestReturnsIdentifyOaiResponseDLR() throws IOException {
        init(CLIENT_TYPE_DLR);
        TimeUtils timeUtils = new TimeUtils();
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.Identify.name());
        handler.handleRequest(handlerInputStream(queryParameters), output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(Verb.Identify.name())));
    }

    @Test
    public void handleRequestReturnsIdentifyOaiResponseNVA() throws IOException {
        init(CLIENT_TYPE_NVA);
        TimeUtils.date2String(null, null);
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.Identify.name());
        handler.handleRequest(handlerInputStream(queryParameters), output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(Verb.Identify.name())));
    }

    @Test
    public void shouldReturnErrorWithUnknownVerb() throws IOException {
        init(CLIENT_TYPE_DLR);
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, UNKNOWN_VERB);
        handler.handleRequest(handlerInputStream(queryParameters), output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(BAD_VERB)));
    }

    @Test
    public void shouldReturnErrorWithMissingVerb() throws IOException {
        init(CLIENT_TYPE_DLR);
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, BLANK);
        handler.handleRequest(handlerInputStream(queryParameters), output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(VERB_IS_MISSING)));
    }

    @Test
    public void shouldReturnErrorWhenListRecordsWithoutResumptionToken() throws IOException {
        init(CLIENT_TYPE_DLR);
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(METADATA_PREFIX_IS_A_REQUIRED)));
    }

    @Test
    public void shouldReturnErrorWhenGetRecordWithInvalidIdentifier() throws IOException {
        init(CLIENT_TYPE_DLR);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.GetRecord.name());
        queryParameters.put(ValidParameterKey.IDENTIFIER.key, "1234");
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(BAD_ARGUMENT)));
    }

    @Test
    public void shouldReturnErrorWhenGetRecordWhenApiIsDown() throws IOException {
        init(CLIENT_TYPE_DLR);
        mockErrorRecordResponse();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.GetRecord.name());
        queryParameters.put(ValidParameterKey.IDENTIFIER.key, REAL_OAI_IDENTIFIER_DLR);
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(ID_DOES_NOT_EXIST)));
    }

    @Test
    public void shouldReturnExceptionWhenGetRecordWhenCommunicationCrashes() throws IOException {
        init(CLIENT_TYPE_DLR);
        when(environment.readEnv(RECORD_URI_ENV)).thenReturn(FAULTY_JSON);
        createAdapter(CLIENT_TYPE_DLR, environment);
        handler = new OaiProviderHandler(environment, adapter);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.GetRecord.name());
        queryParameters.put(ValidParameterKey.IDENTIFIER.key, REAL_OAI_IDENTIFIER_DLR);
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_UNAVAILABLE, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(EXCEPTION)));
    }

    @Test
    public void shouldReturnExceptionWhenGetRecordWithFaultyJsonResponse() throws IOException {
        init(CLIENT_TYPE_DLR);
        mockFaultyRecordResponse();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.GetRecord.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        queryParameters.put(ValidParameterKey.SET.key, SET_NAME_SIKT);
        queryParameters.put(ValidParameterKey.IDENTIFIER.key, REAL_OAI_IDENTIFIER_DLR);
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_UNAVAILABLE, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(FAULTY_JSON)));
    }

    @Test
    public void shouldReturnCannotDisseminateFormatErrorWhenGetRecordWithoutMetadataPrefix() throws IOException {
        init(CLIENT_TYPE_DLR);
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.GetRecord.name());
        queryParameters.put(ValidParameterKey.IDENTIFIER.key, VALID_DLR_IDENTIFIER);
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(METADATA_FORMAT_NOT_SUPPORTED)));
    }

    @Test
    public void shouldReturnErrorWhenRequestWithInvalidQueryParam() throws IOException {
        init(CLIENT_TYPE_DLR);
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.GetRecord.name());
        queryParameters.put(randomString(), randomString());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(NOT_A_LEGAL_PARAMETER)));
    }

    @Test
    public void shouldReturnListRecordsResponseWhenAskedForListRecordsWithMetadataPrefixQdcAndSetSiktAndIdentifierNVA()
        throws IOException {
        init(CLIENT_TYPE_NVA);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        queryParameters.put(ValidParameterKey.SET.key, UIO_CUSTUMER_ID);
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(Verb.ListRecords.name())));
    }

    @Test
    public void shouldReturnGetRecordResponseWhenAskedForGetRecordWithMetadataPrefixAndIdentifier() throws IOException {
        init(CLIENT_TYPE_DLR);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.GetRecord.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        queryParameters.put(ValidParameterKey.SET.key, UIO_CUSTUMER_ID);
        queryParameters.put(ValidParameterKey.IDENTIFIER.key, REAL_OAI_IDENTIFIER_DLR);
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(Verb.GetRecord.name())));
    }

    @ParameterizedTest(name = "Should return GetRecordResponse for metadataPrefix: {0}")
    @ValueSource(strings = {"qdc", "oai_datacite", "oai_dc"})
    public void shouldReturnGetRecordResponseWithVerbGetRecordWithMetadataPrefixAndIdentifierNVA(String metadataPrefix)
        throws IOException {
        init(CLIENT_TYPE_NVA);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.GetRecord.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, metadataPrefix);
        queryParameters.put(ValidParameterKey.SET.key, SET_NAME_SIKT);
        queryParameters.put(ValidParameterKey.IDENTIFIER.key, FAKE_OAI_IDENTIFIER_NVA);
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(Verb.GetRecord.name())));
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedForGetRecordWithInvalidIdentifier() throws IOException {
        init(CLIENT_TYPE_DLR);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.GetRecord.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        queryParameters.put(ValidParameterKey.SET.key, SET_NAME_SIKT);
        queryParameters.put(ValidParameterKey.IDENTIFIER.key, INVALID_DLR_IDENTIFIER);
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(ID_DOES_NOT_EXIST)));
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedForGetRecordWithInvalidIdentifierNVA() throws IOException {
        init(CLIENT_TYPE_NVA);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.GetRecord.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        queryParameters.put(ValidParameterKey.SET.key, SET_NAME_SIKT);
        queryParameters.put(ValidParameterKey.IDENTIFIER.key, INVALID_NVA_IDENTIFIER);
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(ID_DOES_NOT_EXIST)));
    }

    @Test
    public void shouldReturnListMetadataFormatsResponseWhenAskedForListMetadataFormats() throws IOException {
        init(CLIENT_TYPE_DLR);
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListMetadataFormats.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(Verb.ListMetadataFormats.name())));
    }

    @ParameterizedTest(name = "Should return ListSets for init: {0}")
    @ValueSource(strings = {CLIENT_TYPE_DLR, CLIENT_TYPE_NVA})
    public void shouldReturnListSetsResponseWhenAskedForListSets(String clientType) throws IOException {
        init(clientType);
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListSets.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, OAI_DC.name());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(Verb.ListSets.name())));
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedForListSetsButApiIsDown() throws IOException {
        init(CLIENT_TYPE_DLR);
        mockErrorSetsResponse(CLIENT_TYPE_DLR);
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListSets.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, OAI_DC.name());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(NO_SET_HIERARCHY)));
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedForListSetsButSomeMisbehaviorInServerCommunication()
        throws IOException {
        init(CLIENT_TYPE_DLR);
        when(environment.readEnv(SETS_URI_ENV)).thenReturn(FAULTY_JSON);
        createAdapter(CLIENT_TYPE_DLR, environment);
        handler = new OaiProviderHandler(environment, adapter);
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListSets.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, OAI_DC.name());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_UNAVAILABLE, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(EXCEPTION)));
    }

    @Test
    public void shouldReturnListIdentifiersResponseWhenAskedForListIdentifiers() throws IOException {
        init(CLIENT_TYPE_DLR);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListIdentifiers.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        queryParameters.put(ValidParameterKey.SET.key, SET_NAME_SIKT);
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(Verb.ListIdentifiers.name())));
        assertThat(responseBody, is(not(containsString(METADATA_TAG))));
    }

    @Test
    public void shouldReturnListIdentifiersResponseWhenAskedForListIdentifiersWithValidResumptionToken()
        throws IOException {
        init(CLIENT_TYPE_DLR);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListIdentifiers.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        queryParameters.put(ValidParameterKey.SET.key, SET_NAME_SIKT);
        queryParameters.put(ValidParameterKey.RESUMPTIONTOKEN.key, RESUMPTION_TOKEN);
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(Verb.ListIdentifiers.name())));
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedForListIdentifiersButListSetResponseIsFaulty() throws IOException {
        init(CLIENT_TYPE_DLR);
        mockFaultySetsResponse(CLIENT_TYPE_DLR);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListIdentifiers.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        queryParameters.put(ValidParameterKey.SET.key, SET_NAME_SIKT);
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_UNAVAILABLE, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(FAULTY_JSON)));
    }

    @Test
    public void shouldReturnErrorWhenAskedForListIdentifiersWithoutMetadataPrefix() throws IOException {
        init(CLIENT_TYPE_DLR);
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListIdentifiers.name());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(METADATA_PREFIX_IS_A_REQUIRED)));
    }

    @Test
    public void shouldReturnErrorWhenAskedForGettRecordWithNonExistingMetadataFormat() throws IOException {
        init(CLIENT_TYPE_DLR);
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.GetRecord.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, randomString());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(METADATA_FORMAT_NOT_SUPPORTED)));
    }

    @Test
    public void shouldReturnErrorWhenAskedForListRecordsWithNonExistingSetSpec() throws IOException {
        init(CLIENT_TYPE_DLR);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, MetadataFormat.OAI_DATACITE.name());
        queryParameters.put(ValidParameterKey.SET.key, randomString());
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(UNKNOWN_SET_NAME)));
    }

    @Test
    public void shouldReturnErrorWhenAskedForListRecordsAndApiIsDown() throws IOException {
        init(CLIENT_TYPE_DLR);
        mockErrorRecordsResponse();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, MetadataFormat.OAI_DATACITE.name());
        queryParameters.put(ValidParameterKey.SET.key, SET_NAME_SIKT);
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(NO_RECORDS_MATCH)));
    }

    @Test
    public void shouldReturnExceptionWhenAskedForListRecordsAndServerCommunicationFails() throws IOException {
        init(CLIENT_TYPE_DLR);
        when(environment.readEnv(RECORDS_URI_ENV)).thenReturn(FAULTY_JSON);
        createAdapter(CLIENT_TYPE_DLR, environment);
        handler = new OaiProviderHandler(environment, adapter);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, MetadataFormat.OAI_DATACITE.name());
        queryParameters.put(ValidParameterKey.SET.key, SET_NAME_SIKT);
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_UNAVAILABLE, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(EXCEPTION)));
    }

    @Test
    public void shouldReturnListRecordsWhenAskedForListRecordsWithExistingSetSpec() throws IOException {
        init(CLIENT_TYPE_DLR);
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, MetadataFormat.OAI_DATACITE.name());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(Verb.ListRecords.name())));
    }

    @Test
    public void shouldReturnExceptionWhenAskedForListRecordsWithFaultyJsonResponse() throws IOException {
        init(CLIENT_TYPE_DLR);
        mockFaultyRecordsResponse();
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, MetadataFormat.OAI_DATACITE.name());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_UNAVAILABLE, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(FAULTY_JSON)));
    }

    @ParameterizedTest(name = "Should return ListRecordsResponse for metadataPrefix: {0}")
    @ValueSource(strings = {"qdc", "oai_datacite", "oai_dc"})
    public void shouldReturnListRecordsWhenAskedForListRecordsWithExistingNvaSetSpec(String metadataPrefix)
        throws IOException {
        init(CLIENT_TYPE_NVA);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, metadataPrefix);
        queryParameters.put(ValidParameterKey.SET.key, UIO_CUSTUMER_ID);
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(Verb.ListRecords.name())));
    }

    @Test
    public void shouldReturnListRecordsWhenAskedForListRecordsWithExistingDlrSetSpecAndOaiDcMetadataPrefix()
        throws IOException {
        init(CLIENT_TYPE_DLR);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, OAI_DC.name());
        queryParameters.put(ValidParameterKey.SET.key, "BI");
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(Verb.ListRecords.name())));
        assertThat(responseBody, is(containsString("<oai_dc:dc")));
    }

    @Test
    public void shouldReturnDeletedRecordWhenAskedForGetRecordWithExistingWithIdentifierToDeletedRecord()
        throws IOException {
        init(CLIENT_TYPE_DLR);
        mockDeletedRecordResponse();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.GetRecord.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, OAI_DC.name());
        queryParameters.put(ValidParameterKey.IDENTIFIER.key, REAL_OAI_IDENTIFIER_DLR);
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(Verb.GetRecord.name())));
        assertThat(responseBody, is(containsString("<header status=\"deleted\">")));
        assertThat(responseBody, is(not(containsString(METADATA_TAG))));
    }

    @Test
    public void shouldReturnErrorWhenAskedForListRecordsWithNonExistingNvaSetSpec() throws IOException {
        init(CLIENT_TYPE_NVA);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, MetadataFormat.OAI_DATACITE.name());
        queryParameters.put(ValidParameterKey.SET.key, randomString());
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(UNKNOWN_SET_NAME)));
    }

    @Test
    public void shouldReturnErrorWhenAskedForListRecordsAndSourceNvaJsonIsInvalid() throws IOException {
        init(CLIENT_TYPE_NVA);
        mockFaultyRecordsResponse();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, MetadataFormat.OAI_DATACITE.name());
        queryParameters.put(ValidParameterKey.SET.key, UIO_CUSTUMER_ID);
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_UNAVAILABLE, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(ERROR_UNEXPECTED_RESPONSE_FROM_DATA_SOURCE)));
    }

    @Test
    public void shouldReturnErrorWhenClientNameFromEnvironmentIsUnknown() {
        environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        when(environment.readEnv(CLIENT_NAME_ENV)).thenReturn(UNKNOWN_CLIENT_NAME);
        context = mock(Context.class);
        assertThrows(RuntimeException.class, () -> handler = new OaiProviderHandler(environment, adapter));
    }

    @Test
    public void shouldReturnListRecordsResponseWhenAskedForListRecordsWithResumptionToken() throws IOException {
        init(CLIENT_TYPE_DLR);
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.RESUMPTIONTOKEN.key, RESUMPTION_TOKEN);
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(Verb.ListRecords.name())));
    }

    @Test
    public void shouldReturnErrorWhenAskedWithInvalidFromParam() throws IOException {
        init(CLIENT_TYPE_DLR);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        queryParameters.put(ValidParameterKey.FROM.key, randomString());
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(ILLEGAL_DATE_FROM)));
    }

    @Test
    public void shouldReturnErrorWhenAskedWithNullFromParam() throws IOException {
        init(CLIENT_TYPE_DLR);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        queryParameters.put(ValidParameterKey.FROM.key, BLANK);
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(ILLEGAL_DATE_FROM)));
    }

    @Test
    public void shouldReturnErrorWhenAskedWithInvalidUntilParam() throws IOException {
        init(CLIENT_TYPE_DLR);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        queryParameters.put(ValidParameterKey.UNTIL.key, randomString());
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(ILLEGAL_DATE_UNTIL)));
    }

    @Test
    public void shouldReturnErrorWhenAskedWithDifferentLengthFromUntilParam() throws IOException {
        init(CLIENT_TYPE_DLR);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        queryParameters.put(ValidParameterKey.FROM.key, "2006-06-06");
        queryParameters.put(ValidParameterKey.UNTIL.key, "2007-06-06T00:00:00Z");
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(DIFFERENT_DATE_GRANULARITIES)));
    }

    @Test
    public void shouldReturnListRecordsResponseWhenAskedWithSameLengthFromUntilParam() throws IOException {
        init(CLIENT_TYPE_DLR);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        queryParameters.put(ValidParameterKey.FROM.key, "2006-06-06");
        queryParameters.put(ValidParameterKey.UNTIL.key, "2007-06-06");
        var output = new ByteArrayOutputStream();
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(Verb.ListRecords.name())));
    }

    @Test
    public void shouldReturnListRecordsResponseWhenAskedWithOnlyFromParam() throws IOException {
        init(CLIENT_TYPE_DLR);
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        queryParameters.put(ValidParameterKey.FROM.key, "2006-06-06");
        queryParameters.put(ValidParameterKey.UNTIL.key, "");
        var inputStream = handlerInputStream(queryParameters);
        var output = new ByteArrayOutputStream();
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(Verb.ListRecords.name())));
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

    private void createAdapter(String adapterName, Environment environment) {
        if (CLIENT_TYPE_DLR.equalsIgnoreCase(adapterName)) {
            adapter = new DlrAdapter(environment, httpClient);
        } else if (CLIENT_TYPE_NVA.equalsIgnoreCase(adapterName)) {
            adapter = new NvaAdapter(environment, authorizedBackendClient);
        }
    }

    private void startWiremockServer(String adapter) {
        httpServer = new WireMockServer(options().dynamicHttpsPort());
        httpServer.start();
        serverUriSets = URI.create(httpServer.baseUrl() + "/" + adapter + "/sets");
        serverUriRecord = URI.create(httpServer.baseUrl() + "/record");
        serverUriRecords = URI.create(httpServer.baseUrl() + "/records");
    }

    private void mockSetsResponse(String adapter) {
        ObjectNode responseBody = createSetsResponse(adapter);
        stubFor(get(urlPathEqualTo("/" + adapter + "/sets")).willReturn(ok().withBody(responseBody.toPrettyString())));
    }

    private void mockFaultySetsResponse(String adapter) {
        stubFor(get(urlPathMatching("/" + adapter + "/sets")).willReturn(ok().withBody(FAULTY_JSON)));
    }

    private void mockErrorSetsResponse(String adapter) {
        stubFor(get(urlPathMatching("/" + adapter + "/sets")).willReturn(serverError()));
    }

    private void mockRecordResponse(String adapter) {
        if (CLIENT_TYPE_DLR.equalsIgnoreCase(adapter)) {
            ObjectNode responseBody = createRecordResponse();
            stubFor(get(urlPathMatching(UUID_REGEX)).willReturn(ok().withBody(responseBody.toPrettyString())));
        } else if (CLIENT_TYPE_NVA.equalsIgnoreCase(adapter)) {
            String publicationJson =
                IoUtils.stringFromResources(Path.of(EMPTY_STRING, "publication.json"));
            stubFor(get(urlPathMatching(UUID_REGEX)).willReturn(ok().withBody(publicationJson)));
        }
    }

    private void mockFaultyRecordResponse() {
        stubFor(get(urlPathMatching(UUID_REGEX)).willReturn(ok().withBody(FAULTY_JSON)));
    }

    private void mockErrorRecordResponse() {
        stubFor(get(urlPathMatching(UUID_REGEX)).willReturn(serverError()));
    }

    private void mockDeletedRecordResponse() {
        ObjectNode deletedResponseBody = createDeletedRecordResponse();
        stubFor(get(urlPathMatching(UUID_REGEX)).willReturn(ok().withBody(deletedResponseBody.toPrettyString())));
    }

    private void mockRecordsResponse(String adapter) {
        if (CLIENT_TYPE_DLR.equalsIgnoreCase(adapter)) {
            ObjectNode responseBody = createRecordsResponse();
            stubFor(get(urlPathMatching("/records")).willReturn(ok().withBody(responseBody.toPrettyString())));
        } else if (CLIENT_TYPE_NVA.equalsIgnoreCase(adapter)) {
            String publicationJson =
                IoUtils.stringFromResources(Path.of(EMPTY_STRING, "publications.json"));
            stubFor(get(urlPathMatching("/records")).willReturn(ok().withBody(publicationJson)));
        }
    }

    private void mockFaultyRecordsResponse() {
        stubFor(get(urlPathMatching("/records")).willReturn(ok().withBody(FAULTY_JSON)));
    }

    private void mockErrorRecordsResponse() {
        stubFor(get(urlPathMatching("/records")).willReturn(serverError()));
    }

    private ObjectNode createSetsResponse(String adapter) {
        if (CLIENT_TYPE_DLR.equalsIgnoreCase(adapter)) {
            var objectArray = dtoObjectMapper.createArrayNode();
            objectArray.add("bi");
            objectArray.add("diku");
            objectArray.add("ntnu");
            objectArray.add(SET_NAME_SIKT);
            objectArray.add("uit");
            var responseBodyElement = dtoObjectMapper.createObjectNode();
            responseBodyElement.set("institutions", objectArray);
            return responseBodyElement;
        } else if (CLIENT_TYPE_NVA.equalsIgnoreCase(adapter)) {
            var objectNode1 = dtoObjectMapper.createObjectNode();
            objectNode1.put("createdDate", "2022-04-05T21:08:30.971981Z");
            objectNode1.put("displayName", "Sikt");
            objectNode1.put("id", "https://api.dev.nva.aws.unit.no/customer/f50dff3a-e244-48c7-891d-cc4d75597321");
            var objectArray = dtoObjectMapper.createArrayNode();
            objectArray.add(objectNode1);
            var objectNode2 = dtoObjectMapper.createObjectNode();
            objectNode2.put("createdDate", "2022-04-06T06:28:59.673041Z");
            objectNode2.put("displayName", "Universitetet i Oslo");
            objectNode2.put("id", UIO_CUSTUMER_ID);
            objectArray.add(objectNode2);
            var objectNode3 = dtoObjectMapper.createObjectNode();
            objectNode3.put("createdDate", "2022-04-06T06:28:59.673041Z");
            objectNode3.put("displayName", "Universitetet i Oslo");
            objectNode3.put("id", "https://api.dev.nva.aws.unit.no/customer/f50dff3a-e244-48c7-891d-cc4d75597322");
            objectArray.add(objectNode3);
            var responseBodyElement = dtoObjectMapper.createObjectNode();
            responseBodyElement.put("@context", "https://bibsysdev.github.io/src/customer-context.json");
            responseBodyElement.set("customers", objectArray);
            responseBodyElement.put("id", "https://api.dev.nva.aws.unit.no/customer");
            return responseBodyElement;
        }
        return null;
    }

    private ObjectNode createRecordsResponse() {
        var responseBodyElement = dtoObjectMapper.createObjectNode();
        responseBodyElement.put("offset", "0");
        responseBodyElement.put("limit", "50");
        responseBodyElement.put("numFound", 400);
        responseBodyElement.put("queryTime", 0);
        var objectArray = dtoObjectMapper.createArrayNode();
        objectArray.add("{\"identifier\":\"fc2eff7c-5061-47d1-9828-7b3f64c57c67\","
                        + "\"features\":{\"dlr_access\":\"open\",\"dlr_app\":\"learning\","
                        + "\"dlr_content\":\"masse_text.txt\","
                        + "\"dlr_content_type\":\"file\",\"dlr_description\":\"Redigert etter padfasd f\\nlisering\","
                        + "\"dlr_identifier\":\"fc2eff7c-5061-47d1-9828-7b3f64c57c67\","
                        + "\"dlr_licensehelper_contains_other_peoples_work\":\"no\",\"dlr_resource\":\"true\","
                        + "\"dlr_resource_learning\":\"true\",\"dlr_rights_license_name\":\"CC BY 4.0\","
                        + "\"dlr_status_published\":\"true\",\"dlr_storage_id\":\"unit\",\"dlr_submitter_email\":"
                        + "\"billyga@ntnu.no\",\"dlr_time_created\":\"2021-05-05T09:15:40.028Z\","
                        + "\"dlr_time_published\":"
                        + "\"2021-05-05T09:16:02.045Z\",\"dlr_time_updated\":\"2021-05-05T09:15:40.798Z\","
                        + "\"dlr_title\":"
                        + "\"masse text redigert 3\",\"dlr_type\":\"Presentation\"},"
                        + "\"subjects\":[],\"courses\":[],\"tags\":[\"Nytt emneord\",\"test\"],"
                        + "\"types\":[\"learning\"],"
                        + "\"projects\":[],\"funders\":[],\"geographicalCoverages\":[],\"observationalUnits\":[],"
                        + "\"processMethods\":[],\"creators\":[{\"features\":{\"dlr_creator_identifier\":"
                        + "\"b72c3dad-e671-40c2-8f8d-5dfd3f52b41f\",\"dlr_creator_name\":\"Redigert etter "
                        + "publisering\","
                        + "\"dlr_creator_order\":\"0\",\"dlr_creator_time_created\":\"2021-06-08T10:12:37.836Z\"}},"
                        + "{\"features\":{\"dlr_creator_identifier\":\"7c9b3e91-faf1-4eef-9f35-b7ea9f9683ad\","
                        + "\"dlr_creator_name\":\"Anette Olli Siiri\",\"dlr_creator_order\":\"0\","
                        + "\"dlr_creator_time_created\":"
                        + "\"2021-05-05T09:15:42.259Z\"}},{\"features\":{\"dlr_creator_identifier\":"
                        + "\"dece511f-5ee7-48a6-ab48-551aa51122e7\",\"dlr_creator_order\":\"0\","
                        + "\"dlr_creator_time_created\":"
                        + "\"2021-07-02T13:54:50.427Z\"}}],"
                        + "\"contributors\":[{\"features\":{\"dlr_contributor_identifier\":"
                        + "\"af822c16-1825-4aaf-a15d-63dffcb8640b\",\"dlr_contributor_name\":\"Redigert etter "
                        + "publisering\","
                        + "\"dlr_contributor_time_created\":\"2021-06-08T10:12:48.121Z\","
                        + "\"dlr_contributor_type\":\"Producer\"}},"
                        + "{\"features\":{\"dlr_contributor_identifier\":\"2832c74a-7aac-4277-95b2-cc9dd59bd1ff\","
                        + "\"dlr_contributor_name\":\"unit\","
                        + "\"dlr_contributor_time_created\":\"2021-05-05T09:15:40.798Z\",\"dlr_contributor_type\":"
                        + "\"HostingInstitution\"}}],\"accessRead\":[],\"accessWrite\":[\"billyga@ntnu.no\"]}");
        objectArray.add("{\"identifier\":\"3ccd8a0f-f831-485b-ab0c-7fd023fe76ab\","
                        + "\"features\":{\"dlr_access\":\"open\",\"dlr_app\":\"learning\","
                        + "\"dlr_content\":\"https://adressa"
                        + ".no\",\"dlr_content_type\":\"link\",\"dlr_description\":\"Siste nytt innen nyheter, sport, "
                        + "fotball, "
                        + "økonomi, kultur, reise, jobb og mye \\n\\n\\nnfdsfsdfds\\n\\n\\ner fra Norges eldste "
                        + "dagsavis\","
                        + "\"dlr_identifier\":\"3ccd8a0f-f831-485b-ab0c-7fd023fe76ab\","
                        + "\"dlr_licensehelper_can_be_used_commercially\":\"undefined\","
                        + "\"dlr_licensehelper_contains_other_peoples_work\":\"no\","
                        + "\"dlr_licensehelper_others_can_modify_and_build_upon\":\"undefined\","
                        + "\"dlr_licensehelper_resource_restriction\":\"CC BY 4.0\",\"dlr_resource\":\"true\","
                        + "\"dlr_resource_learning\":\"true\",\"dlr_rights_license_name\":\"CC BY 4.0\","
                        + "\"dlr_status_published\":\"true\",\"dlr_storage_id\":\"unit\","
                        + "\"dlr_submitter_email\":\"nr@unit.no\",\"dlr_time_created\":\"2022-02-02T08:45:43.422Z\","
                        + "\"dlr_time_updated\":\"2022-02-02T08:45:43.422Z\","
                        + "\"dlr_time_published\":\"2022-02-02T08:46:16.878Z\",\"dlr_title\":\"Adressa.no\","
                        + "\"dlr_type\":\"Document\"},\"subjects\":[],\"courses\":[],\"tags\":[\"dødsfall\","
                        + "\"eadressa\",\"nyheter\",\"skattelister\",\"trafikk\",\"trondheim\",\"trønder\","
                        + "\"ukeadressa\"],\"types\":[\"learning\"],\"projects\":[],\"funders\":[],"
                        + "\"geographicalCoverages\":[],\"observationalUnits\":[],\"processMethods\":[],"
                        + "\"creators\":[{\"features\":{\"dlr_creator_identifier\":\"cc5b0211-4956-4845-94f2"
                        + "-be2e67d3e503\",\"dlr_creator_name\":\"Nikolai Fikse Raanes\",\"dlr_creator_order\":\"0\","
                        + "\"dlr_creator_time_created\":\"2022-02-02T08:45:47.469Z\"}}],"
                        + "\"contributors\":[{\"features\":{\"dlr_contributor_identifier\":\"48811385-070c-40c5-befe"
                        + "-50501cf50d2f\",\"dlr_contributor_name\":\"Handelshøyskolen BI\","
                        + "\"dlr_contributor_time_created\":\"2022-02-02T08:47:53.056Z\","
                        + "\"dlr_contributor_type\":\"HostingInstitution\"}},"
                        + "{\"features\":{\"dlr_contributor_identifier\":\"3b8d343a-6275-4480-9040-4b0325aca7a9\","
                        + "\"dlr_contributor_name\":\"BIBSYS\",\"dlr_contributor_time_created\":\"2022-02-02T08:48:26"
                        + ".797Z\",\"dlr_contributor_type\":\"HostingInstitution\"}}],\"accessRead\":[],"
                        + "\"accessWrite\":[\"nr@unit.no\"]}");
        objectArray.add("{\"identifier\":\"342cfbae-4844-476d-8516-f112861d8dec\","
                        + "\"features\":{\"dlr_access\":\"open\",\"dlr_app\":\"learning\",\"dlr_content\":\"https://www"
                        + ".facebook.com/NTNUbibliotek/posts/2917760898259307\",\"dlr_content_type\":\"link\","
                        + "\"dlr_description\":\"See posts, photos and more on "
                        + "Fafsldøflsdæfl\\n\\nfsdkølfsdkøflsdkø\\n\\n\\nkfsdølkfscebook.\","
                        + "\"dlr_identifier\":\"342cfbae-4844-476d-8516-f112861d8dec\","
                        + "\"dlr_licensehelper_can_be_used_commercially\":\"undefined\","
                        + "\"dlr_licensehelper_contains_other_peoples_work\":\"no\","
                        + "\"dlr_licensehelper_others_can_modify_and_build_upon\":\"undefined\","
                        + "\"dlr_licensehelper_resource_restriction\":\"CC BY 4.0\",\"dlr_resource\":\"true\","
                        + "\"dlr_resource_learning\":\"true\",\"dlr_rights_license_name\":\"CC BY 4.0\","
                        + "\"dlr_status_published\":"
                        + "\"true\",\"dlr_storage_id\":\"unit\",\"dlr_submitter_email\":\"pcb@unit.no\","
                        + "\"dlr_time_created\":\"2022-02-10T11:19:59.537Z\","
                        + "\"dlr_time_published\":\"2022-02-10T11:20:30"
                        + ".304Z\",\"dlr_time_updated\":\"2022-02-10T11:20:30.304Z\","
                        + "\"dlr_title\":\"Log in or sign up to view\",\"dlr_type\":\"Document\"},\"subjects\":[],"
                        + "\"courses\":[],"
                        + "\"tags\":[],\"types\":[\"learning\"],\"projects\":[],\"funders\":[],"
                        + "\"geographicalCoverages\":[],"
                        + "\"observationalUnits\":[],\"processMethods\":[],"
                        + "\"creators\":[{\"features\":{\"dlr_creator_identifier\""
                        + ":\"a129a11f-dcce-4b34-bcc0-f9f64629e170\",\"dlr_creator_name\":\"Per Christian Bjelke\","
                        + "\"dlr_creator_order\":\"0\",\"dlr_creator_time_created\":\"2022-02-10T11:20:03.925Z\","
                        + "\"dlr_creator_time_updated\":\"2022-02-10T11:20:03.925Z\"}}],\"contributors\":"
                        + "[{\"features\":{\"dlr_contributor_identifier\":\"224742ba-4df0-4501-8601-9a965b445188\","
                        + "\"dlr_contributor_name\":\"UNIT\",\"dlr_contributor_time_created\":\"2022-02-10T11:20:01"
                        + ".244Z\","
                        + "\"dlr_contributor_type\":\"HostingInstitution\"}}],\"accessRead\":[],"
                        + "\"accessWrite\":[\"pcb@unit.no\"]}");
        objectArray.add("{\"identifier\":\"ce2e98d1-4df3-4ce9-a42f-182218beca3e\","
                        + "\"features\":{\"dlr_access\":\"private\",\"dlr_app\":\"learning\",\"dlr_content\":\"pug2"
                        + ".jpeg\",\"dlr_content_type\":\"file\","
                        + "\"dlr_identifier\":\"ce2e98d1-4df3-4ce9-a42f-182218beca3e\","
                        + "\"dlr_licensehelper_contains_other_peoples_work\":\"yes\","
                        + "\"dlr_licensehelper_usage_cleared_with_owner\":\"no_clearance\",\"dlr_resource\":\"true\","
                        + "\"dlr_resource_learning\":\"true\",\"dlr_rights_license_name\":\"CC BY-NC-SA 4.0\","
                        + "\"dlr_status_published\":\"true\",\"dlr_storage_id\":\"unit\","
                        + "\"dlr_submitter_email\":\"ansi@unit.no\",\"dlr_time_created\":\"2022-03-21T09:22:03"
                        + ".509Z\",\"dlr_time_updated\":\"2022-03-21T09:22:03.509Z\","
                        + "\"dlr_time_published\":\"2022-03-22T09:51:08.677Z\",\"dlr_title\":\"pug2\","
                        + "\"dlr_type\":\"Image\"},\"subjects\":[],\"courses\":[],\"tags\":[],"
                        + "\"types\":[\"learning\"],\"projects\":[],\"funders\":[],\"geographicalCoverages\":[],"
                        + "\"observationalUnits\":[],\"processMethods\":[],"
                        + "\"creators\":[{\"features\":{\"dlr_creator_identifier\":\"2ef37cbe-3fda-4d7d-84c2"
                        + "-1442101428f8\",\"dlr_creator_name\":\"Anette Olli Siiri\",\"dlr_creator_order\":\"0\","
                        + "\"dlr_creator_time_created\":\"2022-03-21T09:22:06.950Z\"}}],"
                        + "\"contributors\":[{\"features\":{\"dlr_contributor_identifier\":\"9b4cd8fe-b78f-485d-a0cd"
                        + "-f94edf21daeb\",\"dlr_contributor_name\":\"UNIT\","
                        + "\"dlr_contributor_time_created\":\"2022-03-21T09:22:04.845Z\","
                        + "\"dlr_contributor_type\":\"HostingInstitution\"}}],\"accessRead\":[],"
                        + "\"accessWrite\":[\"ansi@unit.no\"]}");
        responseBodyElement.set("resourcesAsJson", objectArray);
        return responseBodyElement;
    }

    private ObjectNode createRecordResponse() {
        var responseBodyElement = dtoObjectMapper.createObjectNode();
        responseBodyElement.put("identifier", "1234");
        var responseBodyFeaturesObject = dtoObjectMapper.createObjectNode();
        responseBodyFeaturesObject.put("dlr_title", "title");
        responseBodyFeaturesObject.put("dlr_description", "description");
        responseBodyFeaturesObject.put("dlr_rights_license_name", "CC BY 4.0");
        responseBodyFeaturesObject.put("dlr_time_created", "2021-08-09T08:25:22.552Z");
        responseBodyFeaturesObject.put("dlr_time_published", "2021-08-12T08:45:42.154Z");
        responseBodyFeaturesObject.put("dlr_time_updated", "2022-03-12T08:45:42.154Z");
        responseBodyFeaturesObject.put("dlr_identifier_handle", "https://hdl.handle.net/11250.1/1234");
        responseBodyFeaturesObject.put("dlr_identifier_doi", "10.123/SIKT");
        responseBodyElement.set("features", responseBodyFeaturesObject);
        var responseBodyCreatorsArray = dtoObjectMapper.createArrayNode();
        var responseBodyCreatorObject = dtoObjectMapper.createObjectNode();
        var responseBodyCreatorFeaturesObject = dtoObjectMapper.createObjectNode();
        responseBodyCreatorFeaturesObject.put("dlr_creator_name", "Nikoli Fixe");
        responseBodyCreatorObject.set("features", responseBodyCreatorFeaturesObject);
        responseBodyCreatorsArray.add(responseBodyCreatorObject);
        responseBodyElement.set("creators", responseBodyCreatorsArray);
        var responseBodyContributorsObject = dtoObjectMapper.createArrayNode();
        responseBodyElement.set("contributors", responseBodyContributorsObject);
        return responseBodyElement;
    }

    private ObjectNode createDeletedRecordResponse() {
        var responseBodyElement = dtoObjectMapper.createObjectNode();
        responseBodyElement.put("identifier", "1234");
        var responseBodyFeaturesObject = dtoObjectMapper.createObjectNode();
        responseBodyFeaturesObject.put("dlr_title", "title");
        responseBodyFeaturesObject.put("dlr_description", "description");
        responseBodyFeaturesObject.put("dlr_rights_license_name", "CC BY 4.0");
        responseBodyFeaturesObject.put("dlr_time_created", "2021-08-09T08:25:22.552Z");
        responseBodyFeaturesObject.put("dlr_time_published", "2021-08-12T08:45:42.154Z");
        responseBodyFeaturesObject.put("dlr_time_updated", "2022-03-12T08:45:42.154Z");
        responseBodyFeaturesObject.put("dlr_identifier_handle", "https://hdl.handle.net/11250.1/1234");
        responseBodyFeaturesObject.put("dlr_identifier_doi", "10.123/SIKT");
        responseBodyFeaturesObject.put("dlr_status_deleted", "true");
        responseBodyElement.set("features", responseBodyFeaturesObject);
        var responseBodyCreatorsArray = dtoObjectMapper.createArrayNode();
        var responseBodyCreatorObject = dtoObjectMapper.createObjectNode();
        var responseBodyCreatorFeaturesObject = dtoObjectMapper.createObjectNode();
        responseBodyCreatorFeaturesObject.put("dlr_creator_name", "Nikoli Fixe");
        responseBodyCreatorObject.set("features", responseBodyCreatorFeaturesObject);
        responseBodyCreatorsArray.add(responseBodyCreatorObject);
        responseBodyElement.set("creators", responseBodyCreatorsArray);
        var responseBodyContributorsObject = dtoObjectMapper.createArrayNode();
        responseBodyElement.set("contributors", responseBodyContributorsObject);
        return responseBodyElement;
    }
}

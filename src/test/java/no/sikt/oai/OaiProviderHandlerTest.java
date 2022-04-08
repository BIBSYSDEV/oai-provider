package no.sikt.oai;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static no.sikt.oai.MetadataFormat.QDC;
import static no.sikt.oai.RestApiConfig.restServiceObjectMapper;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class OaiProviderHandlerTest {

    public static final String BLANK = " ";
    public static final String UNKNOWN_CLIENT_NAME = "Unknown client name";
    public static final String UNKNOWN_VERB = "UnknownVerb";
    public static final String VALID_IDENTIFIER = "oai:dlr.unit.no:00000000-0000-0000-0000-000000000000";
    private OaiProviderHandler handler;
    private Environment environment;
    private Context context;
    private WireMockServer httpServer;
    private URI serverUriSets;
    private URI serverUriRecord;
    private URI serverUriRecords;

    public void init(String adapter) {
        environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        when(environment.readEnv(OaiConstants.CLIENT_NAME_ENV)).thenReturn(adapter);
        startWiremockServer();
        when(environment.readEnv(OaiConstants.SETS_URI_ENV)).thenReturn(serverUriSets.toString());
        when(environment.readEnv(OaiConstants.RECORD_URI_ENV)).thenReturn(serverUriRecord.toString());
        when(environment.readEnv(OaiConstants.RECORDS_URI_ENV)).thenReturn(serverUriRecords.toString());
        context = mock(Context.class);
        HttpClient httpClient = WiremockHttpClient.create();
        mockSetsResponse();
        mockRecordResponse();
        mockRecordsResponse();
        handler = new OaiProviderHandler(environment, httpClient);
    }

    @AfterEach
    public void tearDown() {
        if(httpServer != null && httpServer.isRunning()) {
            httpServer.stop();
        }
    }

    @Test
    public void handleRequestReturnsIdentifyOaiResponse() throws IOException {
        init("DLR");
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
    public void shouldReturnBadArgumentErrorWithUnknownVerb() throws IOException {
        init("DLR");
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, UNKNOWN_VERB);
        handler.handleRequest(handlerInputStream(queryParameters), output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiConstants.BAD_VERB)));
    }

    @Test
    public void shouldReturnBadArgumentErrorWithMissingVerb() throws IOException {
        init("DLR");
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, BLANK);
        handler.handleRequest(handlerInputStream(queryParameters), output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiConstants.VERB_IS_MISSING)));
    }

    @Test
    public void shouldReturnBadArgumentErrorWhenListRecordsWithoutResumptionToken() throws IOException {
        init("DLR");
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiConstants.METADATA_PREFIX_IS_A_REQUIRED)));
    }

    @Test
    public void shouldBadArgumentErrorWhenGetRecordWithInvalidIdentifier() throws IOException {
        init("DLR");
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.GetRecord.name());
        queryParameters.put(ValidParameterKey.IDENTIFIER.key, "1234");
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiConstants.BAD_ARGUMENT)));
    }

    @Test
    public void shouldReturnCannotDisseminateFormatErrorWhenGetRecordWithoutMetadataPrefix() throws IOException {
        init("DLR");
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.GetRecord.name());
        queryParameters.put(ValidParameterKey.IDENTIFIER.key, VALID_IDENTIFIER);
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiConstants.METADATA_FORMAT_NOT_SUPPORTED)));
    }

    @Test
    public void shouldReturnBadArgumentErrorWhenRequestWithInvalidQueryParam() throws IOException {
        init("DLR");
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.GetRecord.name());
        queryParameters.put(randomString(),  randomString());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiConstants.NOT_A_LEGAL_PARAMETER)));
    }

    @Test
    public void shouldReturnGetRecordResponseWhenAskedForGetRecordWithMetadataPrefixAndIdentifier() throws IOException {
        init("DLR");
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.GetRecord.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        queryParameters.put(ValidParameterKey.SET.key, "sikt");
        queryParameters.put(ValidParameterKey.IDENTIFIER.key, "oai:dlr.unit.no:9a1eae92-38bf-4002-a1a9-d21035242d30");
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(Verb.GetRecord.name())));
    }

    @Test
    public void shouldReturnListMetadataFormatsResponseWhenAskedForListMetadataFormats() throws IOException {
        init("DLR");
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

    @Test
    public void shouldReturnListSetsResponseWhenAskedForListSets() throws IOException {
        init("DLR");
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListSets.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(Verb.ListSets.name())));
    }

    @Test
    public void shouldReturnListIdentifiersResponseWhenAskedForListIdentifiers() throws IOException {
        init("DLR");
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListIdentifiers.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        queryParameters.put(ValidParameterKey.SET.key, "sikt");
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(Verb.ListIdentifiers.name())));
    }

    @Test
    public void shouldReturnBadArgumentErrorWhenAskedForListIdentifiersWithoutMetadataPrefix() throws IOException {
        init("DLR");
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListIdentifiers.name());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiConstants.METADATA_PREFIX_IS_A_REQUIRED)));
    }

    @Test
    public void shouldReturnBadArgumentErrorWhenAskedForListRecordsWithNonExistingMetadataFormat() throws IOException {
        init("DLR");
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.GetRecord.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, randomString());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiConstants.METADATA_FORMAT_NOT_SUPPORTED)));
    }

    @Test
    public void shouldReturnBadArgumentErrorWhenAskedForListRecordsWithNonExistingSetSpec() throws IOException {
        init("DLR");
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, MetadataFormat.OAI_DATACITE.name());
        queryParameters.put(ValidParameterKey.SET.key, randomString());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiConstants.UNKNOWN_SET_NAME)));
    }

    @Test
    public void shouldReturnListRecordsWhenAskedForListRecordsWithExistingSetSpec() throws IOException {
        init("DLR");
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
    public void shouldReturnListRecordsWhenAskedForListRecordsWithExistingNVASetSpec() throws IOException {
        init("NVA");
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, MetadataFormat.OAI_DATACITE.name());
        queryParameters.put(ValidParameterKey.SET.key, "BI");
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(Verb.ListRecords.name())));
    }

    @Test
    public void shouldReturnBadArgumentErrorWhenAskedForListRecordsWithNonExistingNVASetSpec() throws IOException {
        init("NVA");
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, MetadataFormat.OAI_DATACITE.name());
        queryParameters.put(ValidParameterKey.SET.key, randomString());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiConstants.UNKNOWN_SET_NAME)));
    }

    @Test
    public void shouldReturnBadArgumentErrorWhenClientNameFromEnvironmentIsUnknown() {
        environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        when(environment.readEnv(OaiConstants.CLIENT_NAME_ENV)).thenReturn(UNKNOWN_CLIENT_NAME);
        context = mock(Context.class);
        assertThrows(RuntimeException.class, () ->  handler = new OaiProviderHandler(environment));
    }

    @Test
    public void shouldReturnListRecordsResponseWhenAskedForListRecordsWithResumptionToken() throws IOException {
        init("DLR");
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.RESUMPTIONTOKEN.key, "lr~sikt~~~qdc~50");
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(Verb.ListRecords.name())));
    }

    @Test
    public void shouldReturnBadArgumentErrorWhenAskedWithInvalidFromParam() throws IOException {
        init("DLR");
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        queryParameters.put(ValidParameterKey.FROM.key,  randomString());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiConstants.ILLEGAL_DATE_FROM)));
    }

    @Test
    public void shouldReturnBadArgumentErrorWhenAskedWithNullFromParam() throws IOException {
        init("DLR");
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        queryParameters.put(ValidParameterKey.FROM.key, BLANK);
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiConstants.ILLEGAL_DATE_FROM)));
    }

    @Test
    public void shouldReturnBadArgumentErrorWhenAskedWithInvalidUntilParam() throws IOException {
        init("DLR");
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        queryParameters.put(ValidParameterKey.UNTIL.key,  randomString());
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiConstants.ILLEGAL_DATE_UNTIL)));
    }

    @Test
    public void shouldReturnBadArgumentErrorWhenAskedWithDifferentLengthFromUntilParam() throws IOException {
        init("DLR");
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key, QDC.name());
        queryParameters.put(ValidParameterKey.FROM.key, "2006-06-06");
        queryParameters.put(ValidParameterKey.UNTIL.key, "2007-06-06T00:00:00Z");
        var inputStream = handlerInputStream(queryParameters);
        handler.handleRequest(inputStream, output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        var responseBody = gatewayResponse.getBody();
        assertThat(responseBody, is(containsString(OaiConstants.DIFFERENT_DATE_GRANULARITIES)));
    }

    @Test
    public void shouldReturnListRecordsResponseWhenAskedWithSameLengthFromUntilParam() throws IOException {
        init("DLR");
        var output = new ByteArrayOutputStream();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put(ValidParameterKey.VERB.key, Verb.ListRecords.name());
        queryParameters.put(ValidParameterKey.METADATAPREFIX.key,  QDC.name());
        queryParameters.put(ValidParameterKey.FROM.key, "2006-06-06");
        queryParameters.put(ValidParameterKey.UNTIL.key, "2007-06-06");
        var inputStream = handlerInputStream(queryParameters);
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

    private void startWiremockServer() {
        httpServer = new WireMockServer(options().dynamicHttpsPort());
        httpServer.start();
        serverUriSets = URI.create(httpServer.baseUrl() + "/sets");
        serverUriRecord = URI.create(httpServer.baseUrl() + "/record");
        serverUriRecords = URI.create(httpServer.baseUrl() + "/records");
    }

    private void mockSetsResponse() {
        ObjectNode responseBody = createSetsResponse();
        stubFor(get(urlPathMatching("/sets"))
                .willReturn(aResponse().withBody(responseBody
                        .toPrettyString()).withStatus(HttpURLConnection.HTTP_OK)));
    }

    private ObjectNode createSetsResponse() {
        var objectArray = dtoObjectMapper.createArrayNode();
        objectArray.add("bi");
        objectArray.add("diku");
        objectArray.add("ntnu");
        objectArray.add("sikt");
        objectArray.add("uit");
        var responseBodyElement = dtoObjectMapper.createObjectNode();
        responseBodyElement.set("institutions", objectArray);
        return responseBodyElement;
    }

    private void mockRecordsResponse() {
        ObjectNode responseBody = createRecordsResponse();
        stubFor(get(urlPathMatching("/records"))
                .willReturn(aResponse().withBody(responseBody
                        .toPrettyString()).withStatus(HttpURLConnection.HTTP_OK)));
    }

    private void mockRecordResponse() {
        ObjectNode responseBody = createRecordResponse();
        stubFor(get(urlPathMatching("^/[^/]+/[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"))
                .willReturn(aResponse().withBody(responseBody
                        .toPrettyString()).withStatus(HttpURLConnection.HTTP_OK)));
    }

    private ObjectNode createRecordsResponse() {
        var responseBodyElement = dtoObjectMapper.createObjectNode();
        responseBodyElement.put("offset", "0");
        responseBodyElement.put("limit", "50");
        responseBodyElement.put("numFound", 4);
        responseBodyElement.put("queryTime", 0);
        var objectArray = dtoObjectMapper.createArrayNode();
        objectArray.add("{\"identifier\":\"fc2eff7c-5061-47d1-9828-7b3f64c57c67\",\"features\":{\"dlr_access\":\"open\",\"dlr_app\":\"learning\",\"dlr_content\":\"masse_text.txt\",\"dlr_content_type\":\"file\",\"dlr_description\":\"Redigert etter padfasd f\\nlisering\",\"dlr_identifier\":\"fc2eff7c-5061-47d1-9828-7b3f64c57c67\",\"dlr_licensehelper_contains_other_peoples_work\":\"no\",\"dlr_resource\":\"true\",\"dlr_resource_learning\":\"true\",\"dlr_rights_license_name\":\"CC BY 4.0\",\"dlr_status_published\":\"true\",\"dlr_storage_id\":\"unit\",\"dlr_submitter_email\":\"billyga@ntnu.no\",\"dlr_time_created\":\"2021-05-05T09:15:40.028Z\",\"dlr_time_published\":\"2021-05-05T09:16:02.045Z\",\"dlr_title\":\"masse text redigert 3\",\"dlr_type\":\"Presentation\"},\"subjects\":[],\"courses\":[],\"tags\":[\"Nytt emneord\",\"test\"],\"types\":[\"learning\"],\"projects\":[],\"funders\":[],\"geographicalCoverages\":[],\"observationalUnits\":[],\"processMethods\":[],\"creators\":[{\"features\":{\"dlr_creator_identifier\":\"b72c3dad-e671-40c2-8f8d-5dfd3f52b41f\",\"dlr_creator_name\":\"Redigert etter publisering\",\"dlr_creator_order\":\"0\",\"dlr_creator_time_created\":\"2021-06-08T10:12:37.836Z\"}},{\"features\":{\"dlr_creator_identifier\":\"7c9b3e91-faf1-4eef-9f35-b7ea9f9683ad\",\"dlr_creator_name\":\"Anette Olli Siiri\",\"dlr_creator_order\":\"0\",\"dlr_creator_time_created\":\"2021-05-05T09:15:42.259Z\"}},{\"features\":{\"dlr_creator_identifier\":\"dece511f-5ee7-48a6-ab48-551aa51122e7\",\"dlr_creator_order\":\"0\",\"dlr_creator_time_created\":\"2021-07-02T13:54:50.427Z\"}}],\"contributors\":[{\"features\":{\"dlr_contributor_identifier\":\"af822c16-1825-4aaf-a15d-63dffcb8640b\",\"dlr_contributor_name\":\"Redigert etter publisering\",\"dlr_contributor_time_created\":\"2021-06-08T10:12:48.121Z\",\"dlr_contributor_type\":\"Producer\"}},{\"features\":{\"dlr_contributor_identifier\":\"2832c74a-7aac-4277-95b2-cc9dd59bd1ff\",\"dlr_contributor_name\":\"unit\",\"dlr_contributor_time_created\":\"2021-05-05T09:15:40.798Z\",\"dlr_contributor_type\":\"HostingInstitution\"}}],\"accessRead\":[],\"accessWrite\":[\"billyga@ntnu.no\"]}");
        objectArray.add("{\"identifier\":\"3ccd8a0f-f831-485b-ab0c-7fd023fe76ab\",\"features\":{\"dlr_access\":\"open\",\"dlr_app\":\"learning\",\"dlr_content\":\"https://adressa.no\",\"dlr_content_type\":\"link\",\"dlr_description\":\"Siste nytt innen nyheter, sport, fotball, økonomi, kultur, reise, jobb og mye \\n\\n\\nnfdsfsdfds\\n\\n\\ner fra Norges eldste dagsavis\",\"dlr_identifier\":\"3ccd8a0f-f831-485b-ab0c-7fd023fe76ab\",\"dlr_licensehelper_can_be_used_commercially\":\"undefined\",\"dlr_licensehelper_contains_other_peoples_work\":\"no\",\"dlr_licensehelper_others_can_modify_and_build_upon\":\"undefined\",\"dlr_licensehelper_resource_restriction\":\"CC BY 4.0\",\"dlr_resource\":\"true\",\"dlr_resource_learning\":\"true\",\"dlr_rights_license_name\":\"CC BY 4.0\",\"dlr_status_published\":\"true\",\"dlr_storage_id\":\"unit\",\"dlr_submitter_email\":\"nr@unit.no\",\"dlr_time_created\":\"2022-02-02T08:45:43.422Z\",\"dlr_time_published\":\"2022-02-02T08:46:16.878Z\",\"dlr_title\":\"Adressa.no\",\"dlr_type\":\"Document\"},\"subjects\":[],\"courses\":[],\"tags\":[\"dødsfall\",\"eadressa\",\"nyheter\",\"skattelister\",\"trafikk\",\"trondheim\",\"trønder\",\"ukeadressa\"],\"types\":[\"learning\"],\"projects\":[],\"funders\":[],\"geographicalCoverages\":[],\"observationalUnits\":[],\"processMethods\":[],\"creators\":[{\"features\":{\"dlr_creator_identifier\":\"cc5b0211-4956-4845-94f2-be2e67d3e503\",\"dlr_creator_name\":\"Nikolai Fikse Raanes\",\"dlr_creator_order\":\"0\",\"dlr_creator_time_created\":\"2022-02-02T08:45:47.469Z\"}}],\"contributors\":[{\"features\":{\"dlr_contributor_identifier\":\"48811385-070c-40c5-befe-50501cf50d2f\",\"dlr_contributor_name\":\"Handelshøyskolen BI\",\"dlr_contributor_time_created\":\"2022-02-02T08:47:53.056Z\",\"dlr_contributor_type\":\"HostingInstitution\"}},{\"features\":{\"dlr_contributor_identifier\":\"3b8d343a-6275-4480-9040-4b0325aca7a9\",\"dlr_contributor_name\":\"BIBSYS\",\"dlr_contributor_time_created\":\"2022-02-02T08:48:26.797Z\",\"dlr_contributor_type\":\"HostingInstitution\"}}],\"accessRead\":[],\"accessWrite\":[\"nr@unit.no\"]}");
        objectArray.add("{\"identifier\":\"342cfbae-4844-476d-8516-f112861d8dec\",\"features\":{\"dlr_access\":\"open\",\"dlr_app\":\"learning\",\"dlr_content\":\"https://www.facebook.com/NTNUbibliotek/posts/2917760898259307\",\"dlr_content_type\":\"link\",\"dlr_description\":\"See posts, photos and more on Fafsldøflsdæfl\\n\\nfsdkølfsdkøflsdkø\\n\\n\\nkfsdølkfscebook.\",\"dlr_identifier\":\"342cfbae-4844-476d-8516-f112861d8dec\",\"dlr_licensehelper_can_be_used_commercially\":\"undefined\",\"dlr_licensehelper_contains_other_peoples_work\":\"no\",\"dlr_licensehelper_others_can_modify_and_build_upon\":\"undefined\",\"dlr_licensehelper_resource_restriction\":\"CC BY 4.0\",\"dlr_resource\":\"true\",\"dlr_resource_learning\":\"true\",\"dlr_rights_license_name\":\"CC BY 4.0\",\"dlr_status_published\":\"true\",\"dlr_storage_id\":\"unit\",\"dlr_submitter_email\":\"pcb@unit.no\",\"dlr_time_created\":\"2022-02-10T11:19:59.537Z\",\"dlr_time_published\":\"2022-02-10T11:20:30.304Z\",\"dlr_title\":\"Log in or sign up to view\",\"dlr_type\":\"Document\"},\"subjects\":[],\"courses\":[],\"tags\":[],\"types\":[\"learning\"],\"projects\":[],\"funders\":[],\"geographicalCoverages\":[],\"observationalUnits\":[],\"processMethods\":[],\"creators\":[{\"features\":{\"dlr_creator_identifier\":\"a129a11f-dcce-4b34-bcc0-f9f64629e170\",\"dlr_creator_name\":\"Per Christian Bjelke\",\"dlr_creator_order\":\"0\",\"dlr_creator_time_created\":\"2022-02-10T11:20:03.925Z\"}}],\"contributors\":[{\"features\":{\"dlr_contributor_identifier\":\"224742ba-4df0-4501-8601-9a965b445188\",\"dlr_contributor_name\":\"UNIT\",\"dlr_contributor_time_created\":\"2022-02-10T11:20:01.244Z\",\"dlr_contributor_type\":\"HostingInstitution\"}}],\"accessRead\":[],\"accessWrite\":[\"pcb@unit.no\"]}");
        objectArray.add("{\"identifier\":\"ce2e98d1-4df3-4ce9-a42f-182218beca3e\",\"features\":{\"dlr_access\":\"private\",\"dlr_app\":\"learning\",\"dlr_content\":\"pug2.jpeg\",\"dlr_content_type\":\"file\",\"dlr_identifier\":\"ce2e98d1-4df3-4ce9-a42f-182218beca3e\",\"dlr_licensehelper_contains_other_peoples_work\":\"yes\",\"dlr_licensehelper_usage_cleared_with_owner\":\"no_clearance\",\"dlr_resource\":\"true\",\"dlr_resource_learning\":\"true\",\"dlr_rights_license_name\":\"CC BY-NC-SA 4.0\",\"dlr_status_published\":\"true\",\"dlr_storage_id\":\"unit\",\"dlr_submitter_email\":\"ansi@unit.no\",\"dlr_time_created\":\"2022-03-21T09:22:03.509Z\",\"dlr_time_published\":\"2022-03-22T09:51:08.677Z\",\"dlr_title\":\"pug2\",\"dlr_type\":\"Image\"},\"subjects\":[],\"courses\":[],\"tags\":[],\"types\":[\"learning\"],\"projects\":[],\"funders\":[],\"geographicalCoverages\":[],\"observationalUnits\":[],\"processMethods\":[],\"creators\":[{\"features\":{\"dlr_creator_identifier\":\"2ef37cbe-3fda-4d7d-84c2-1442101428f8\",\"dlr_creator_name\":\"Anette Olli Siiri\",\"dlr_creator_order\":\"0\",\"dlr_creator_time_created\":\"2022-03-21T09:22:06.950Z\"}}],\"contributors\":[{\"features\":{\"dlr_contributor_identifier\":\"9b4cd8fe-b78f-485d-a0cd-f94edf21daeb\",\"dlr_contributor_name\":\"UNIT\",\"dlr_contributor_time_created\":\"2022-03-21T09:22:04.845Z\",\"dlr_contributor_type\":\"HostingInstitution\"}}],\"accessRead\":[],\"accessWrite\":[\"ansi@unit.no\"]}");
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

}

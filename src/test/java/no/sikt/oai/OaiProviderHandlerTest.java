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

import static no.sikt.oai.RestApiConfig.restServiceObjectMapper;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OaiProviderHandlerTest {

    private OaiProviderHandler handler;
    private Environment environment;
    private Context context;

    @BeforeEach
    public void init() {
        environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        context = getMockContext();
        handler = new OaiProviderHandler(environment);
    }

    private Context getMockContext() {
        Context context = mock(Context.class);
        when(context.getAwsRequestId()).thenReturn("1234");
        return context;
    }

    @AfterEach
    public void tearDown() {

    }

    @Test
    public void handleRequestReturnsOaiResponse() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(handlerInputStream(), output, context);
        GatewayResponse<String> gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        String responseBody = gatewayResponse.getBody();
        assertEquals("ListRecords", responseBody);
    }

    private InputStream handlerInputStream() throws JsonProcessingException {

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Accept", "application/xml");

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("Verb", "ListRecords");

        return new HandlerRequestBuilder<Void>(restServiceObjectMapper)
                .withHttpMethod("GET")
                .withQueryParameters(queryParameters)
                .withHeaders(requestHeaders)
                .build();
    }

    private GatewayResponse<String> parseSuccessResponse(String output) throws JsonProcessingException {
        JavaType typeRef = restServiceObjectMapper.getTypeFactory()
                .constructParametricType(GatewayResponse.class, String.class);
        return restServiceObjectMapper.readValue(output, typeRef);
    }

}

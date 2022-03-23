package no.sikt.oai;

import com.amazonaws.services.lambda.runtime.Context;
import no.sikt.oai.model.OaiResponse;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;

public class OaiProviderHandler extends ApiGatewayHandler<Void, OaiResponse> {

    @JacocoGenerated
    public OaiProviderHandler() {
        this(new Environment());
    }

    public OaiProviderHandler(Environment environment) {
        super(Void.class, environment);
    }

    @Override
    protected OaiResponse processInput(Void input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {
        final String verb = requestInfo.getQueryParameter("Verb");
        return new OaiResponse(verb);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, OaiResponse output) {
        return HttpURLConnection.HTTP_OK;
    }
}

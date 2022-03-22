package no.sikt.oai;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import no.sikt.oai.model.OaiRequest;
import no.sikt.oai.model.OaiResponse;

public class OaiProviderHandler implements RequestHandler<OaiRequest, OaiResponse> {

    @Override
    public OaiResponse handleRequest(OaiRequest oaiRequest, Context context) {
        return null;
    }
}

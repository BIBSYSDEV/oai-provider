package no.sikt.oai.exception;

import nva.commons.apigateway.exceptions.ApiGatewayException;

import java.net.HttpURLConnection;

public class OaiException extends ApiGatewayException {

    public OaiException(String verb, String errorCode, String errorText) {
        super("<error code=\""+ errorCode + "\">" + errorText + " '" + verb + "'</error>");
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_BAD_REQUEST;
    }

}

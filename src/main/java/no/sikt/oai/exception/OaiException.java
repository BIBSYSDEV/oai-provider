package no.sikt.oai.exception;

import nva.commons.apigateway.exceptions.ApiGatewayException;

import java.net.HttpURLConnection;

public class OaiException extends ApiGatewayException {

    public String errorCode;
    public String verb;
    public String errorText;

    public OaiException(String verb, String errorCode, String errorText) {
        super(verb + " " + errorCode + " " + errorText);
        this.verb = verb;
        this.errorCode = errorCode;
        this.errorText = errorText;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorText() {
        return errorText;
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_BAD_REQUEST;
    }

}

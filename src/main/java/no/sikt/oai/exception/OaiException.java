package no.sikt.oai.exception;

import nva.commons.apigateway.exceptions.ApiGatewayException;

public class OaiException extends ApiGatewayException {

    public String errorCode;
    public String verb;
    public String errorText;

    public OaiException(String verb, String errorCode, String errorText) {
        super(verb + " " + errorCode + " " + errorText);
    }

    @Override
    protected Integer statusCode() {
        return null;
    }

}

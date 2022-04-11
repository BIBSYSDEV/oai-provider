package no.sikt.oai.exception;

import nva.commons.apigateway.exceptions.ApiGatewayException;

public class InternalOaiException extends ApiGatewayException {

    private int statusCode;

    public InternalOaiException(Exception e, int statusCode) {
        super(e);
        this.statusCode = statusCode;
    }

    @Override
    protected Integer statusCode() {
        return statusCode;
    }
}

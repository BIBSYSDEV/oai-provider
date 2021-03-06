package no.sikt.oai.exception;

import nva.commons.apigateway.exceptions.ApiGatewayException;

public class InternalOaiException extends ApiGatewayException {

    private final transient int status;

    public InternalOaiException(Exception e, int statusCode) {
        super(e);
        this.status = statusCode;
    }

    public InternalOaiException(Exception e, String message, int statusCode) {
        super(e, message);
        this.status = statusCode;
    }

    @Override
    protected Integer statusCode() {
        return status;
    }
}

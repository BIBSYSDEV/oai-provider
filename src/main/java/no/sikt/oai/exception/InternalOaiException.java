package no.sikt.oai.exception;

import nva.commons.apigateway.exceptions.ApiGatewayException;

public class InternalOaiException extends ApiGatewayException {

    private transient final int status;

    public InternalOaiException(Exception e, int statusCode) {
        super(e);
        this.status = statusCode;
    }

    @Override
    protected Integer statusCode() {
        return status;
    }
}

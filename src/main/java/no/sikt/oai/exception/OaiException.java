package no.sikt.oai.exception;


public class OaiException extends Exception {

    private final String errorCode;
    private final String errorText;

    public OaiException(String errorCode, String errorText) {
        super(errorCode + " " + errorText);
        this.errorCode = errorCode;
        this.errorText = errorText;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorText() {
        return errorText;
    }

}

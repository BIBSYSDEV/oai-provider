package no.sikt.oai.model;

public class OaiResponse {

    private String responseXml;

    public OaiResponse(String responseXml) {
        this.responseXml = responseXml;
    }

    public String getResponseXml() {
        return responseXml;
    }

    public void setResponseXml(String responseXml) {
        this.responseXml = responseXml;
    }
}

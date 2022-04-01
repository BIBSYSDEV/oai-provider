package no.sikt.oai.service;

import no.sikt.oai.OaiConstants;
import no.sikt.oai.Verb;
import no.sikt.oai.adapter.Adapter;
import no.sikt.oai.exception.OaiException;
import nva.commons.core.JacocoGenerated;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static no.sikt.oai.OaiConstants.NO_SETS_FOUND;
import static no.sikt.oai.OaiConstants.NO_SET_HIERARCHY;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class DataProvider extends RestClient{


    private final HttpClient client;
    private final Adapter adapter;
    private URI institutionUri;

    public DataProvider(HttpClient client, Adapter adapter) {
        this.client = client;
        this.adapter = adapter;
    }

    @JacocoGenerated
    public DataProvider(Adapter adapter) {
        this(HttpClient.newBuilder().build(), adapter);
    }

    public String getInstitutionList() throws OaiException {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(adapter.getInstitutionsUri())
                    .header(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (!responseIsSuccessful(response)) {
                throw new OaiException(Verb.ListSets.name(), NO_SET_HIERARCHY, NO_SETS_FOUND);
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new OaiException(Verb.ListSets.name(), NO_SET_HIERARCHY, NO_SETS_FOUND);
        }

    }

    public void setInstitutionUrl(URI institutionUri) {
        this.institutionUri = institutionUri;
    }

}

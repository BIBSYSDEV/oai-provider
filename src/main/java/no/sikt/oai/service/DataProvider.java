package no.sikt.oai.service;

import no.sikt.oai.OaiConstants;
import no.sikt.oai.adapter.Adapter;
import no.sikt.oai.exception.InternalOaiException;
import no.sikt.oai.exception.OaiException;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static no.sikt.oai.OaiConstants.ID_DOES_NOT_EXIST;
import static no.sikt.oai.OaiConstants.NO_RECORDS_MATCH;
import static no.sikt.oai.OaiConstants.NO_SETS_FOUND;
import static no.sikt.oai.OaiConstants.NO_SET_HIERARCHY;
import static no.sikt.oai.OaiConstants.UNKNOWN_IDENTIFIER;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class DataProvider {


    private final HttpClient client;
    private final Adapter adapter;

    public DataProvider(HttpClient client, Adapter adapter) {
        this.client = client;
        this.adapter = adapter;
    }

    @JacocoGenerated
    public DataProvider(Adapter adapter) {
        this(HttpClient.newBuilder().build(), adapter);
    }

    public String getSetsList() throws OaiException, InternalOaiException {
        HttpResponse<String> response;
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(adapter.getSetsUri())
                    .header(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                    .GET()
                    .build();
            response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new InternalOaiException(e, HttpURLConnection.HTTP_UNAVAILABLE);
        }
        if (!responseIsSuccessful(response)) {
            throw new OaiException(NO_SET_HIERARCHY, NO_SETS_FOUND);
        }
        return response.body();
    }

    public String getRecord(String identifier) throws OaiException, InternalOaiException {
        HttpResponse<String> response;
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(adapter.getRecordUri(identifier))
                    .header(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                    .GET()
                    .build();
            response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new InternalOaiException(e, HttpURLConnection.HTTP_UNAVAILABLE);
        }
        if (!responseIsSuccessful(response)) {
            throw new OaiException(ID_DOES_NOT_EXIST, UNKNOWN_IDENTIFIER);
        }
        return response.body();
    }

    public String getRecordsList(String from, String until, String setSpec, int startPosition)
            throws OaiException, InternalOaiException {
        HttpResponse<String> response;
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(adapter.getRecordsListUri(from, until, setSpec, startPosition))
                    .header(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                    .GET()
                    .build();
            response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new InternalOaiException(e, HttpURLConnection.HTTP_UNAVAILABLE);
        }
        if (!responseIsSuccessful(response)) {
            throw new OaiException(NO_RECORDS_MATCH, OaiConstants.COMBINATION_OF_PARAMS_ERROR);
        }
        return response.body();
    }

    @JacocoGenerated
    protected boolean responseIsSuccessful(HttpResponse<String> response) {
        int status = response.statusCode();
        // status should be in the range [200,300)
        return status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES;
    }

}

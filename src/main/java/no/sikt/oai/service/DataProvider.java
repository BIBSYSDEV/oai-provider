package no.sikt.oai.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.sikt.oai.Verb;
import no.sikt.oai.exception.OaiException;
import nva.commons.core.JacocoGenerated;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class DataProvider extends RestClient{

    private final HttpClient client;
    private ObjectMapper mapper = new ObjectMapper();
    private URI institutionUri;

    public DataProvider(HttpClient client) {
        this.client = client;
    }

    @JacocoGenerated
    public DataProvider() {
        this(HttpClient.newBuilder().build());
    }

    public List<String> getInstitutionList() throws OaiException {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(institutionUri)
                    .header(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (!responseIsSuccessful(response)) {
                throw new OaiException(Verb.ListSets.name(), "noSetHierarchy", "no sets found");
            }
            mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            return mapper.readValue(response.body(), Institutions.class).institutions;
        } catch (IOException | InterruptedException e) {
            throw new OaiException(Verb.ListSets.name(), "noSetHierarchy", "no sets found");
        }

    }

    public void setInstitutionUrl(URI institutionUri) {
        this.institutionUri = institutionUri;
    }

    private static class Institutions {
        @JsonProperty("institutions")
        List<String> institutions;
    }

}

package no.sikt.oai.service;

import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;

public class RestClient {

    protected URI createURI(URI apiUrl, String... path) throws URISyntaxException {
        return new URIBuilder(apiUrl).setPathSegments(path).build();
    }

    protected boolean responseIsSuccessful(HttpResponse<String> response) {
        int status = response.statusCode();
        // status should be in the range [200,300)
        return status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES;
    }
}

package no.sikt.oai.adapter;

import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import no.sikt.oai.OaiConstants;
import no.sikt.oai.data.Record;
import no.sikt.oai.data.RecordsList;
import no.sikt.oai.exception.InternalOaiException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public class NvaAdapter implements Adapter {

    private final transient ObjectMapper mapper = new ObjectMapper();
    private final transient String resourceUri;
    private final transient String resourcesUri;
    private final transient String setsUri; // "https://api.dev.nva.aws.unit.no/customer/";

    public NvaAdapter(Environment environment) {
        setsUri = environment.readEnv(OaiConstants.SETS_URI_ENV);
        resourceUri = environment.readEnv(OaiConstants.RECORD_URI_ENV);
        resourcesUri = environment.readEnv(OaiConstants.RECORDS_URI_ENV);
    }

    @Override
    public boolean isValidIdentifier(String identifier) {
        return identifier.length() == 36;
    }

    @Override
    public String getDescription() {
        return "Repository for NVA resources";
    }

    @Override
    public String getDateGranularity() {
        return "YYYY-MM-DD";
    }

    @Override
    public String getEarliestTimestamp() {
        return "2020-01-31T00:00:01Z";
    }

    @Override
    public String getDeletedRecord() {
        return "yes";
    }

    @Override
    public String getProtocolVersion() {
        return "2.0";
    }

    @Override
    public String getAdminEmail() {
        return "support@unit.no";
    }

    @Override
    public String getRepositoryName() {
        return "NVA Repository";
    }

    @Override
    public String getBaseUrl() {
        return "https://nva.unit.no";
    }

    @Override
    public String getIdentifierPrefix() {
        return "oai:nva.unit.no:";
    }

    @Override
    public List<OaiSet> parseSetsResponse(String json) throws InternalOaiException {
        try {
            List<Customer> customerList = mapper.readValue(json, Customers.class).customerList;
            return customerList.stream()
                .map(customer -> new OaiSet(customer.displayName, customer.id))
                .collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            throw new InternalOaiException(e, HTTP_UNAVAILABLE);
        }
    }

    @Override
    public URI getSetsUri() {
        return UriWrapper
            .fromUri(setsUri)
            .getUri();
    }

    @Override
    public URI getRecordUri(String identifier) {
        return UriWrapper
            .fromUri(resourceUri)
            .addChild(identifier)
            .getUri();
    }

    @Override
    public URI getRecordsListUri(String from, String until, String institution, int startPosition) {
        return UriWrapper
            .fromUri(resourcesUri)
            .getUri();
    }

    @Override
    public Record parseRecordResponse(String json, String metadataPrefix, String setSpec) {
        return new Record("", false, "1234", new Date(), Collections.emptyList());
    }

    @Override
    public RecordsList parseRecordsListResponse(String verb, String json, String metadataPrefix, String setSpec) {
        Record record = new Record("", false, "1234", new Date(), Collections.emptyList());
        RecordsList records = new RecordsList(1);
        records.add(record);
        return records;
    }

    private static class Customers {

        @JsonProperty("@context")
        /* default */ transient String context;
        @JsonProperty("customers")
        /* default */ transient List<Customer> customerList;
        @JsonProperty("id")
        /* default */ transient String id;
    }

    private static class Customer {

        @JsonProperty("createdDate")
        /* default */ transient String createdDate;
        @JsonProperty("displayName")
        /* default */ transient String displayName;
        @JsonProperty("id")
        /* default */ transient String id;
    }
}

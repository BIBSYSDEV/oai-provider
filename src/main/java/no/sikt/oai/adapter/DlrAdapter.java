package no.sikt.oai.adapter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.sikt.oai.Verb;
import no.sikt.oai.data.Record;
import no.sikt.oai.data.RecordsList;
import no.sikt.oai.exception.OaiException;
import nva.commons.core.paths.UriWrapper;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static no.sikt.oai.OaiConstants.NO_SETS_FOUND;
import static no.sikt.oai.OaiConstants.NO_SET_HIERARCHY;

public class DlrAdapter implements Adapter{

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean isValidIdentifier(String identifier) {
        return identifier.length() == 36;
    }

    @Override
    public String getDescription() {
        return "Repository for DLR resources";
    }

    @Override
    public String getDateGranularity() {
        return "YYYY-MM-DD";
    }

    @Override
    public String getEarliestTimestamp() {
        return "1976-01-01T00:00:01Z";
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
        return "dlradmin@unit.no";
    }

    @Override
    public String getRepositoryName() {
        return "DLR Repository";
    }

    @Override
    public String getBaseUrl() {
        return "https://example.com";
    }

    @Override
    public List<String> parseInstitutionResponse(String json) throws OaiException {
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        try {
            return mapper.readValue(json, Institutions.class).institutions;
        } catch (JsonProcessingException e) {
            throw new OaiException(Verb.ListSets.name(), NO_SET_HIERARCHY, NO_SETS_FOUND);
        }
    }

    @Override
    public Record parseRecordResponse(String json) throws OaiException {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            Resource resource = mapper.readValue(json, Resource.class);
            return new Record("", false, "1234", new Date());
        } catch (JsonProcessingException e) {
            throw new OaiException(Verb.GetRecord.name(), NO_SET_HIERARCHY, NO_SETS_FOUND);
        }
    }

    @Override
    public RecordsList parseRecordsListResponse(String verb, String json) throws OaiException {
        //            ResourceSearchResponse resourceSearchResponse = mapper.readValue(json, ResourceSearchResponse.class);
        Record record = new Record("", false, "1234", new Date());
        RecordsList records = new RecordsList(1);
        records.add(record);
        return records;
    }

    @Override
    public URI getInstitutionsUri() {
        return UriWrapper
                .fromUri("https://api-dev.dlr.aws.unit.no/dlr-gui-backend-resources-search/v1/oai/institutions")
                .getUri();
    }

    @Override
    public URI getRecordUri(String identifier) {
        return UriWrapper
                .fromUri("https://api-dev.dlr.aws.unit.no/dlr-gui-backend-resources-search/v1/oai/" +
                        "resources/" + identifier)

                .getUri();
    }

    @Override
    public URI getRecordsListUri(String from, String until, String institution, int startPosition) {
        return UriWrapper
                .fromUri("https://api-dev.dlr.aws.unit.no/dlr-gui-backend-resources-search/v1/oai/" +
                        "resources?filter=facet_institution::"+institution) //"&from=" + from)
                .getUri();
    }

    private static class Institutions {
        @JsonProperty("institutions")
        List<String> institutions;
    }

    private static class Resource {
        @JsonProperty("identifier")
        String identifier;
        @JsonProperty("features")
        Map<String, String> features;
    }

    private static class ResourceSearchResponse {
        String offset;
        String limit;
        long numFound;
        int queryTime;
        List<String> resourcesAsJson;
        List<Map<String, String>> facet_counts;
        List<String> spellcheck_suggestions;
    }

}

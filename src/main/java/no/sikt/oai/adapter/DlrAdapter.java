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
    public Record getRecord(String identifier) {
        return new Record("", false, "1234", new Date());
    }

    @Override
    public RecordsList getRecords(String from, String until, String institution, int startPosition) {
        Record record = new Record("", false, "1234", new Date());
        RecordsList records = new RecordsList(1);
        records.add(record);
        return records;
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
    public URI getInstitutionsUri() {
        return UriWrapper
                .fromUri("https://api-dev.dlr.aws.unit.no/dlr-gui-backend-resources-search/v1/oai/institutions")
                .getUri();
//        try {
//            return new URI("https://api-dev.dlr.aws.unit.no/dlr-gui-backend-resources-search/v1/oai/institutions");
//        } catch (URISyntaxException e) {
//            e.printStackTrace();
//        }
    }

    private static class Institutions {
        @JsonProperty("institutions")
        List<String> institutions;
    }
}

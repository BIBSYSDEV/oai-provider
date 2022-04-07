package no.sikt.oai.adapter;

import no.sikt.oai.OaiConstants;
import no.sikt.oai.data.Record;
import no.sikt.oai.data.RecordsList;
import no.sikt.oai.exception.OaiException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NvaAdapter implements Adapter {

    private String resourcesUri;
    private String setsUri = "https://api.dev.nva.aws.unit.no/customer/";

    public NvaAdapter(Environment environment) {
        setsUri = environment.readEnv(OaiConstants.SETS_URI_ENV);
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
        return "nvaadmin@unit.no";
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
    public List<String> parseInstitutionResponse(String json) throws OaiException {
        List<String> list = new ArrayList<>();
        list.add("ntnu");
        list.add("vid");
        list.add("bi");
        return list;
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
                .fromUri(resourcesUri)
                .getUri();
    }

    @Override
    public URI getRecordsListUri(String from, String until, String institution, int startPosition) {
        return UriWrapper
                .fromUri(resourcesUri)
                .getUri();
    }

    @Override
    public Record parseRecordResponse(String json, String metadataPrefix) throws OaiException {
        return new Record("", false, "1234", new Date());
    }

    @Override
    public RecordsList parseRecordsListResponse(String verb, String json, String metadataPrefix)
            throws OaiException {
        Record record = new Record("", false, "1234", new Date());
        RecordsList records = new RecordsList(1);
        records.add(record);
        return records;
    }

}

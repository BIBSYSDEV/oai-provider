package no.sikt.oai.adapter;

import no.sikt.oai.data.Record;
import no.sikt.oai.data.RecordsList;
import no.sikt.oai.exception.InternalOaiException;

import java.net.URI;
import java.util.List;

public interface Adapter {

    public boolean isValidIdentifier(String identifier);

    public String getDescription();

    public String getDateGranularity();

    public String getEarliestTimestamp();

    public String getDeletedRecord();

    public String getProtocolVersion();

    public String getAdminEmail();

    public String getRepositoryName();

    public String getBaseUrl();

    public String getIdentifierPrefix();

    public List<String> parseInstitutionResponse(String json) throws InternalOaiException;

    public Record parseRecordResponse(String json, String metadataPrefix) throws InternalOaiException;

    public RecordsList parseRecordsListResponse(String verb, String json, String metadataPrefix)
            throws InternalOaiException;

    public URI getSetsUri();

    public URI getRecordUri(String identifier);

    public URI getRecordsListUri(String from, String until, String institution, int startPosition);
}

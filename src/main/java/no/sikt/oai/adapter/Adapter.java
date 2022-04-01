package no.sikt.oai.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.sikt.oai.data.Record;
import no.sikt.oai.data.RecordsList;
import no.sikt.oai.exception.OaiException;

import java.net.URI;
import java.net.URL;
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

    public Record getRecord(String identifier);

    public RecordsList getRecords(String from, String until, String institution, int startPosition);

    public List<String> parseInstitutionResponse(String json) throws OaiException;

    public URI getInstitutionsUri();
}

package no.sikt.oai.adapter;

import no.sikt.oai.data.Record;
import no.sikt.oai.data.RecordsList;
import no.sikt.oai.exception.InternalOaiException;

import java.net.URI;
import java.util.List;

public interface Adapter {

    boolean isValidIdentifier(String identifier);

    String getDescription();

    String getDateGranularity();

    String getEarliestTimestamp();

    String getDeletedRecord();

    String getProtocolVersion();

    String getAdminEmail();

    String getRepositoryName();

    String getBaseUrl();

    String getIdentifierPrefix();

    List<String> parseInstitutionResponse(String json) throws InternalOaiException;

    Record parseRecordResponse(String json, String metadataPrefix) throws InternalOaiException;

    RecordsList parseRecordsListResponse(String verb, String json, String metadataPrefix)
     throws InternalOaiException;

    URI getSetsUri();

    URI getRecordUri(String identifier);

    URI getRecordsListUri(String from, String until, String institution, int startPosition);
}

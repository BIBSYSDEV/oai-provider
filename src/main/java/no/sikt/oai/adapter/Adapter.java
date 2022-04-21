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

    List<OaiSet> parseSetsResponse(String json) throws InternalOaiException;

    Record parseRecordResponse(String json, String metadataPrefix, String setSpec) throws InternalOaiException;

    RecordsList parseRecordsListResponse(String verb, String json, String metadataPrefix, String setSpec)
        throws InternalOaiException;

    URI getSetsUri();

    URI getRecordUri(String identifier);

    URI getRecordsListUri(String from, String until, String institution, int startPosition);

    class OaiSet {

        public String setName;
        public String setSpec;

        public OaiSet(String displayName, String id) {
            setName = displayName;
            setSpec = id;
        }
    }
}

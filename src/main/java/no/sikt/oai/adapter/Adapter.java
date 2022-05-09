package no.sikt.oai.adapter;

import no.sikt.oai.data.Record;
import no.sikt.oai.data.RecordsList;
import no.sikt.oai.exception.InternalOaiException;

import java.util.List;
import no.sikt.oai.exception.OaiException;

public interface Adapter {

    String ALL_SET_NAME = "all";

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

    String getSetsList() throws OaiException, InternalOaiException;

    String getRecord(String identifier) throws OaiException, InternalOaiException;

    String getRecordsList(String from, String until, String setSpec, int startPosition)
            throws OaiException, InternalOaiException;

    class OaiSet {

        public String setName;
        public String setSpec;

        public OaiSet(String displayName, String id) {
            setName = displayName;
            setSpec = id;
        }
    }
}

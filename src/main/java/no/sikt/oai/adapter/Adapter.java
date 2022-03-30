package no.sikt.oai.adapter;

import no.sikt.oai.data.Record;
import no.sikt.oai.data.RecordsList;

import java.net.URL;

public interface Adapter {

    public boolean isValidSetName(String setSpec);

    public boolean isValidIdentifier(String identifier);

    public String getRepositoryName();

    public String getBaseUrl();

    public Record getRecord(String identifier);

    public RecordsList getRecords(String from, String until, String institution, int startPosition);

}

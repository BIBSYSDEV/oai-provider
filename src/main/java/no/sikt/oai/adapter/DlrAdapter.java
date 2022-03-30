package no.sikt.oai.adapter;

import no.sikt.oai.data.Record;
import no.sikt.oai.data.RecordsList;

import java.util.Date;
import java.util.Locale;

public class DlrAdapter implements Adapter{

    @Override
    public boolean isValidSetName(String setSpec) {
        return SetName.isValid(setSpec);
    }

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

    /**
     * TODO! has to be replaced with call to the backend to list all institutions/customers
     */
    enum SetName {
        NTNU,
        VID,
        SIKT;

        public static boolean isValid(String value) {
            for (SetName set : values()) {
                if (set.name().equals(value.toUpperCase(Locale.getDefault()))) {
                    return true;
                }
            }
            return false;
        }
    }
}

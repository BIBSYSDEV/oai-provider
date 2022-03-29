package no.sikt.oai;

public enum Verb {
    Identify,
    GetRecord,
    ListIdentifiers,
    ListMetadataFormats,
    ListRecords,
    ListSets;


    public static boolean isValid(String value) {
        for (Verb verb : values()) {
            if (verb.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}

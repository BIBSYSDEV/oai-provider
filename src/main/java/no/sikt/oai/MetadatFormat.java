package no.sikt.oai;

public enum MetadatFormat {

    /** qualified dublin core */
    QDC,
    /** openAire 4.0 */
    OAI_DATACITE;

    public static boolean isValid(String value) {
        for (MetadatFormat metadatFormat : values()) {
            if (metadatFormat.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}

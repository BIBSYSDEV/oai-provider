package no.sikt.oai;

import java.util.Locale;

public enum MetadataFormat {

    /** OAI DC */
    OAI_DC,
    /** qualified dublin core */
    QDC,
    /** openAire 4.0 */
    OAI_DATACITE;

    public static boolean isValid(String value) {
        for (MetadataFormat metadataFormat : values()) {
            if (metadataFormat.name().equals(value.toUpperCase(Locale.getDefault()))) {
                return true;
            }
        }
        return false;
    }
}

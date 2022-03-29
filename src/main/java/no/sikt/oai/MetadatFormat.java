package no.sikt.oai;

import java.util.Locale;

public enum MetadatFormat {

    /** qualified dublin core */
    QDC,
    /** openAire 4.0 */
    OAI_DATACITE;

    public static boolean isValid(String value) {
        for (MetadatFormat metadatFormat : values()) {
            if (metadatFormat.name().equals(value.toUpperCase(Locale.getDefault()))) {
                return true;
            }
        }
        return false;
    }
}

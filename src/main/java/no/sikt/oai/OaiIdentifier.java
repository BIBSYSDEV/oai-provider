package no.sikt.oai;

import static no.sikt.oai.OaiConstants.BAD_ARGUMENT;
import static no.sikt.oai.OaiConstants.ILLEGAL_IDENTIFIER;
import static no.sikt.oai.OaiConstants.ILLEGAL_IDENTIFIER_PREFIX;
import no.sikt.oai.exception.OaiException;

public class OaiIdentifier {

    private String identifier;

    private String prefix;

    public OaiIdentifier(String identifier, String prefix) throws OaiException {

        try {
            if (!identifier.startsWith(prefix)) {
                throw new OaiException(BAD_ARGUMENT, ILLEGAL_IDENTIFIER);
            }
            this.prefix = prefix;
            setIdentifier(identifier.replace(prefix, ""));
        } catch (final Exception e) {
            throw new OaiException(BAD_ARGUMENT, String.format(ILLEGAL_IDENTIFIER_PREFIX, prefix));
        }
    }

    public String getIdentifier() {
        return identifier;
    }


    private void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String toString() {
        return prefix + getIdentifier();
    }
}

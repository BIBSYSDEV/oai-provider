package no.sikt.oai;

import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class OaiConstants {

    public static final String NO_SET_HIERARCHY = "noSetHierarchy";
    public static final String NO_SETS_FOUND = "no sets found";
    public static final String COULD_NOT_PARSE_RESPONSE = "Could not parse GetRecord response";
    public static final String ILLEGAL_ARGUMENT = "Illegal argument";
    public static final String BAD_ARGUMENT = "badArgument";
    public static final String BAD_VERB = "badVerb";
    public static final String CANNOT_DISSEMINATE_FORMAT = "cannotDisseminateFormat";
    public static final String VERB_IS_MISSING = "'verb' is missing";
    public static final String ID_DOES_NOT_EXIST = "idDoesNotExist";
    public static final String METADATA_PREFIX_IS_A_REQUIRED = "metadataPrefix is a required argument for the verb ";
    public static final String ILLEGAL_IDENTIFIER = "Illegal identifier.";
    public static final String ILLEGAL_IDENTIFIER_PREFIX = "Illegal identifier. Expected prefix '%s'.";
    public static final String NOT_A_LEGAL_PARAMETER = "Not a legal parameter: ";
    public static final String ILLEGAL_DATE_FROM = "Not a legal date FROM, use YYYY-MM-DD";
    public static final String ILLEGAL_DATE_UNTIL = "Not a legal date UNTIL, use YYYY-MM-DD";
    public static final String DIFFERENT_DATE_GRANULARITIES = "The request has different granularities for the from " +
            "and until parameters.";
    public static final String METADATA_FORMAT_NOT_SUPPORTED = "The metadata format identified by the value given " +
            "for the metadataPrefix argument is not supported by the item or by the repository.";
    public static final String UNKNOWN_SET_NAME = "unknown set name: ";

    public static final String SETS_URI_ENV = "SETS_URI";
    public static final String RECORD_URI_ENV = "RECORD_URI";
    public static final String RECORDS_URI_ENV = "RECORDS_URI";
    public static final String CLIENT_NAME_ENV = "OAI_CLIENT_NAME";
    public static final String CLIENT_TYPE_DLR = "DLR";
    public static final String CLIENT_TYPE_NVA = "NVA";
}

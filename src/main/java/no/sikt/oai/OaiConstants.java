package no.sikt.oai;

import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class OaiConstants {

    public static final String NO_SET_HIERARCHY = "noSetHierarchy";
    public static final String NO_SETS_FOUND = "no sets found";
    public static final String UNKNOWN_IDENTIFIER = "The value of the identifier argument is unknown or illegal in "
                                                    + "this repository.";
    public static final String ILLEGAL_ARGUMENT = "Illegal argument";
    public static final String BAD_ARGUMENT = "badArgument";
    public static final String BAD_VERB = "badVerb";
    public static final String CANNOT_DISSEMINATE_FORMAT = "cannotDisseminateFormat";
    public static final String VERB_IS_MISSING = "'verb' is missing";
    public static final String ID_DOES_NOT_EXIST = "idDoesNotExist";
    public static final String NO_RECORDS_MATCH = "noRecordsMatch";
    public static final String METADATA_PREFIX_IS_A_REQUIRED = "metadataPrefix is a required argument for the verb ";
    public static final String ILLEGAL_IDENTIFIER = "Illegal identifier.";
    public static final String ILLEGAL_IDENTIFIER_PREFIX = "Illegal identifier. Expected prefix '%s'.";
    public static final String NOT_A_LEGAL_PARAMETER = "Not a legal parameter: ";
    public static final String ILLEGAL_DATE_FROM = "Not a legal date FROM, use YYYY-MM-DD";
    public static final String ILLEGAL_DATE_UNTIL = "Not a legal date UNTIL, use YYYY-MM-DD";
    public static final String DIFFERENT_DATE_GRANULARITIES = "The request has different granularities for the from "
                                                              + "and until parameters.";
    public static final String METADATA_FORMAT_NOT_SUPPORTED = "The metadata format identified by the value given "
                                                               + "for the metadataPrefix argument is not supported by "
                                                               + "the item or by the repository.";
    public static final String UNKNOWN_SET_NAME = "unknown set name: ";
    public static final String COMBINATION_OF_PARAMS_ERROR = "The combination of the values of the from, until, set "
                                                             + "and metadataPrefix arguments results in an empty list.";

    public static final String SETS_URI_ENV = "SETS_URI";
    public static final String RECORD_URI_ENV = "RECORD_URI";
    public static final String RECORDS_URI_ENV = "RECORDS_URI";
    public static final String COGNITO_URI_ENV = "COGNITO_URI";
    public static final String BACKEND_SECRET_ID_ENV = "BACKEND_SECRET_ID";
    public static final String BACKEND_CLIENT_ID_ENV = "BACKEND_CLIENT_ID";
    public static final String CLIENT_NAME_ENV = "OAI_CLIENT_NAME";
    public static final String CLIENT_TYPE_DLR = "DLR";
    public static final String CLIENT_TYPE_NVA = "NVA";

    public static final String OAI_DC_HEADER =
            "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n"
            + " xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + " xmlns:doc=\"http://www.lyncode.com/xoai\"\n"
            + " xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/"
            + " http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n";


    public static final String OAI_DATACITE_HEADER =
            "<oaire:resource xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + " xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
            + " xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
            + " xmlns:datacite=\"http://datacite.org/schema/kernel-4\"\n"
            + " xmlns:vc=\"http://www.w3.org/2007/XMLSchema-versioning\"\n"
            + " xmlns:oaire=\"http://namespace.openaire.eu/schema/oaire/\"\n"
            + " xsi:schemaLocation=\"http://namespace.openaire.eu/schema/oaire/"
            + " https://www.openaire.eu/schema/repo-lit/4.0/openaire.xsd\">\n";


    public static final String QDC_HEADER = "<qdc:qualifieddc xmlns:doc=\"http://www.lyncode.com/xoai\"\n"
            + " xmlns:dcterms=\"http://purl.org/dc/terms/\"\n"
            + " xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
            + " xmlns:qdc=\"http://dspace.org/qualifieddc/\"\n"
            + " xsi:schemaLocation=\"http://purl.org/dc/elements/1.1/"
            + " http://dublincore.org/schemas/xmls/qdc/2006/01/06/dc.xsd"
            + " http://purl.org/dc/terms/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dcterms.xsd"
            + " http://dspace.org/qualifieddc/http://www.ukoln.ac.uk/metadata/dcmi/xmlschema/qualifieddc.xsd\">\n";

}

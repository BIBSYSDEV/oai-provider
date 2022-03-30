package no.sikt.oai;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import no.sikt.oai.adapter.Adapter;
import no.sikt.oai.adapter.DlrAdapter;
import no.sikt.oai.adapter.NvaAdapter;
import no.sikt.oai.data.Record;
import no.sikt.oai.data.RecordsList;
import no.sikt.oai.exception.OaiException;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import static com.google.common.net.MediaType.APPLICATION_XML_UTF_8;

public class OaiProviderHandler extends ApiGatewayHandler<Void, String> {

    public static final String ILLEGAL_ARGUMENT = "Illegal argument";
    public static final String BAD_ARGUMENT = "badArgument";
    public static final String VERB_IS_MISSING = "'verb' is missing";
    public static final String ID_DOES_NOT_EXIST = "idDoesNotExist";
    public static final String METADATA_PREFIX_IS_A_REQUIRED = "metadataPrefix is a required argument for the verb ";
    public static final String EMPTY_STRING = "";
    public static final String NOT_A_LEGAL_PARAMETER = "Not a legal parameter: ";
    public static final String ILLEGAL_DATE_FROM = "Not a legal date FROM, use YYYY-MM-DD";
    public static final String ILLEGAL_DATE_UNTIL = "Not a legal date UNTIL, use YYYY-MM-DD";
    public static final String DIFFERENT_DATE_GRANULARITIES = "The request has different granularities for the from " +
            "and until parameters.";
    public static final String METADATA_FORMAT_NOT_SUPPORTED = "The metadata format identified by the value given " +
            "for the metadataPrefix argument is not supported by the item or by the repository.";
    public static final String UNKNOWN_SET_NAME = "unknown set name: ";
    public static final String UNKNOWN_CLIENT_NAME = "Could not find clientName %s to initiate adapter.";
    public static final String NO_MATCHING_IDENTIFIER = "No matching identifier in: ";

    public static final String CLIENT_NAME_ENV = "OAI_CLIENT_NAME";

    private Adapter adapter;

    @JacocoGenerated
    public OaiProviderHandler() {
        this(new Environment());
    }

    public OaiProviderHandler(Environment environment) {
        super(Void.class, environment);
        initAdapter();
    }

    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {

        String verb = requestInfo.getQueryParameter(ValidParameterKey.VERB.key);
        String resumptionToken = requestInfo.getQueryParameterOpt(ValidParameterKey.RESUMPTIONTOKEN.key)
                .orElse(EMPTY_STRING);
        String metadataPrefix = requestInfo.getQueryParameterOpt(ValidParameterKey.METADATAPREFIX.key)
                .orElse(EMPTY_STRING);
        String setSpec = requestInfo.getQueryParameterOpt(ValidParameterKey.SET.key)
                .orElse(EMPTY_STRING);
        String from = requestInfo.getQueryParameterOpt(ValidParameterKey.FROM.key)
                .orElse(EMPTY_STRING);
        String until = requestInfo.getQueryParameterOpt(ValidParameterKey.UNTIL.key)
                .orElse(EMPTY_STRING);
        String identifier = requestInfo.getQueryParameterOpt(ValidParameterKey.IDENTIFIER.key)
                .orElse(EMPTY_STRING);

        validateAllParameters(requestInfo.getQueryParameters(), verb);
        validateVerb(verb);

        long startTime = System.currentTimeMillis();

        RecordsList recordsList;
        String response;

        switch (Verb.valueOf(verb)) {
            case GetRecord:
                validateMetadataPrefix(verb, metadataPrefix);
                validateIdentifier(verb, identifier, adapter.getRepositoryName());
                Record record = adapter.getRecord(identifier);
                response = OaiResponse.GetRecord(record, identifier, metadataPrefix, setSpec, adapter.getBaseUrl(),
                        startTime);
                break;
            case ListRecords:
                validateRequiredParameters(verb, resumptionToken, metadataPrefix);
                validateFromAndUntilParameters(verb, from, until);
                validateSet(verb, setSpec);
                recordsList = adapter.getRecords(from, until, setSpec, 0);
                response = OaiResponse.ListRecords(from, until, resumptionToken, metadataPrefix,
                        adapter.getBaseUrl(), 0, setSpec, recordsList, startTime);
                break;
            case ListIdentifiers:
                validateRequiredParameters(verb, resumptionToken, metadataPrefix);
                validateFromAndUntilParameters(verb, from, until);
                validateSet(verb, setSpec);
                recordsList = adapter.getRecords(from, until, setSpec, 0);
                response = OaiResponse.ListIdentifiers(from, until, metadataPrefix, resumptionToken,
                        adapter.getBaseUrl(), setSpec, 0, recordsList, startTime);
                break;
            case ListMetadataFormats:
                response = OaiResponse.ListMetadataFormats(adapter.getBaseUrl(), metadataPrefix, null, null,
                        startTime);
                break;
            case ListSets:
                response = OaiResponse.ListSets(adapter.getBaseUrl(), setSpec, null, startTime);
                break;
            case Identify:
            default:
                response = OaiResponse.Identify(adapter.getRepositoryName(), adapter.getBaseUrl(), null, null,
                        null, null, null, null, startTime);
                break;
        }
        return response;
    }

    private void initAdapter() {
        String clientName = environment.readEnv(CLIENT_NAME_ENV);
        switch (clientName) {
            case "DLR":
                this.adapter = new DlrAdapter();
                break;
            case "NVA":
                this.adapter = new NvaAdapter();
                break;
            default:
                throw new RuntimeException(String.format(UNKNOWN_CLIENT_NAME, clientName));
        }
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return List.of(APPLICATION_XML_UTF_8);
    }

    protected void validateAllParameters(Map<String, String> queryParameters, String verb) throws OaiException {
        for (String paramKey : queryParameters.keySet()) {
            if (!ValidParameterKey.isValidParameterkey(paramKey)) {
                throw new OaiException(verb, BAD_ARGUMENT, NOT_A_LEGAL_PARAMETER + paramKey);
            }
        }
    }

    protected void validateVerb(String verb)
            throws OaiException {
        if (verb.trim().isEmpty()) {
            throw new OaiException(verb, BAD_ARGUMENT, VERB_IS_MISSING);
        }
        if (!Verb.isValid(verb)) {
            throw new OaiException(verb, BAD_ARGUMENT, ILLEGAL_ARGUMENT);
        }
    }

    protected void validateRequiredParameters(String verb, String resumptionToken, String metadataPrefix)
            throws OaiException {
        if (EMPTY_STRING.equals(resumptionToken) && EMPTY_STRING.equals(metadataPrefix)) {
            throw new OaiException(verb, BAD_ARGUMENT, METADATA_PREFIX_IS_A_REQUIRED + verb);
        }
    }

    protected void validateFromAndUntilParameters(String verb, String from, String until) throws OaiException {
        if (from.length() > 0 && !TimeUtils.verifyUTCdate(from)) {
            throw new OaiException(verb, BAD_ARGUMENT, ILLEGAL_DATE_FROM);
        }
        if (until.length() > 0 && !TimeUtils.verifyUTCdate(until)) {
            throw new OaiException(verb, BAD_ARGUMENT, ILLEGAL_DATE_UNTIL);
        }
        if (from.length() != until.length()) {
            throw new OaiException(verb, BAD_ARGUMENT, DIFFERENT_DATE_GRANULARITIES);
        }
    }

    protected void validateSet(String verb, String setSpec)
            throws OaiException {
        if (setSpec.length() > 0 && !adapter.isValidSetName(setSpec)) {
            throw new OaiException(verb, BAD_ARGUMENT, UNKNOWN_SET_NAME + setSpec);
        }
    }

    protected void validateMetadataPrefix(String verb, String metadataPrefix)
            throws OaiException {
        if (!MetadatFormat.isValid(metadataPrefix)) {
            throw new OaiException(verb, BAD_ARGUMENT, METADATA_FORMAT_NOT_SUPPORTED);
        }
    }

    protected void validateIdentifier(String verb, String identifier, String repositoryName)
            throws OaiException {
        if (!adapter.isValidIdentifier(identifier)) {
            throw new OaiException(verb, ID_DOES_NOT_EXIST, NO_MATCHING_IDENTIFIER + repositoryName);
        }
    }
}

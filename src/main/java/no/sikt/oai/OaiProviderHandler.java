package no.sikt.oai;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import no.sikt.oai.adapter.Adapter;
import no.sikt.oai.adapter.DlrAdapter;
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
    private Adapter adapter;

    @JacocoGenerated
    public OaiProviderHandler() {
        this(new Environment());
        this.adapter = new DlrAdapter();
    }

    public OaiProviderHandler(Environment environment) {
        super(Void.class, environment);
        this.adapter = new DlrAdapter();
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

        validateAllParameters(requestInfo.getQueryParameters(), verb);
        validateVerb(verb);

        String response;
        switch (Verb.valueOf(verb)) {
            case GetRecord:
                validateRequiredParameters(verb, resumptionToken, metadataPrefix);
                validateMetadataPrefix(verb, metadataPrefix);
                response = verb;
                break;
            case ListRecords:
            case ListIdentifiers:
                validateRequiredParameters(verb, resumptionToken, metadataPrefix);
                validateFromAndUntilParameters(verb, from, until);
                validateSet(verb, setSpec);
                response = verb;
                break;
            case Identify:
            case ListMetadataFormats:
            case ListSets:
            default:
                response = verb;
                break;
        }
        return response;
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
}

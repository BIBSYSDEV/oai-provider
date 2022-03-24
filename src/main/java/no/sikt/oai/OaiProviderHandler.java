package no.sikt.oai;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
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
import static no.sikt.oai.Verb.Identify;

public class OaiProviderHandler extends ApiGatewayHandler<Void, String> {

    public static final String ILLEGAL_ARGUMENT = "Illegal argument";
    public static final String BAD_ARGUMENT = "badArgument";
    public static final String VERB_IS_MISSING = "'verb' is missing";

    @JacocoGenerated
    public OaiProviderHandler() {
        this(new Environment());
    }

    public OaiProviderHandler(Environment environment) {
        super(Void.class, environment);
    }

    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {

        String verb = requestInfo.getQueryParameter("verb");
        String resumptionToken = requestInfo.getQueryParameterOpt("resumptionToken").orElse("");
        String metadataPrefix = requestInfo.getQueryParameterOpt("metadataPrefix").orElse("");

        validateAllParameters(requestInfo.getQueryParameters(), verb);
        validateVerbAndRequiredParameters(verb, resumptionToken, metadataPrefix);
//        validateFromAndUntilParameters(verb, from, until);

        switch (Verb.valueOf(verb)) {
            case Identify:
            case GetRecord:
            case ListIdentifiers:
            case ListMetadataFormats:
            case ListRecords:
            case ListSets:
            default:
                return verb;
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
                throw new OaiException(verb, BAD_ARGUMENT, "Not a legal parameter: " + paramKey);
            }
        }
    }

    protected void validateVerbAndRequiredParameters(String verb, String resumptionToken, String metadataPrefix) throws OaiException {
        if (verb.trim().isEmpty()) {
            throw new OaiException(verb, BAD_ARGUMENT, VERB_IS_MISSING);
        }
        if (verb.equalsIgnoreCase("listrecords") || verb.equalsIgnoreCase("getrecord")) {
            if ("".equals(resumptionToken) && "".equals(metadataPrefix)) {
                throw new OaiException(verb, BAD_ARGUMENT, "metadataPrefix is a required argument for the verb " + verb);
            }
        }
        try {
            Verb.valueOf(verb);
        } catch (IllegalArgumentException e) {
            throw new OaiException(verb, BAD_ARGUMENT, ILLEGAL_ARGUMENT);
        }
    }

//    protected void validateFromAndUntilParameters(String verb, String from, String until) throws OaiException {
//        if (from != null && from.length() > 0 && !TimeUtils.verifyUTCdate(from)) {
//            throw new OAIException(verb, "badArgument", "Not a legal date FROM, use YYYY-MM-DD or " + oaiConfig.getDateGranularity());
//        }
//        if (until != null && until.length() > 0 && !TimeUtils.verifyUTCdate(until)) {
//            throw new OAIException(verb, "badArgument", "Not a legal date UNTIL, use YYYY-MM-DD or " + oaiConfig.getDateGranularity());
//        }
//        if (from != null && until != null && from.length() > 0 && until.length() > 0) {
//            if (from.length() != until.length()) {
//                throw new OaiException(verb, "badArgument", "The request has different granularities for the from and until parameters.");
//            }
//        }
//    }
//
//    protected void validateSetAndMetadataPrefix(String verb, String setSpec, String metadataPrefix) throws OaiException {
//        if (!metadataFormatValidator.isValid(metadataPrefix)) {
//            throw new OaiException(verb, "cannotDisseminateFormat",
//                    "--The metadata format identified by the value given for the \nmetadataPrefix argument is not supported by the item or by the repository.");
//        }
//        if (setSpec != null && setSpec.length() > 0 && !oaiConfig.isValidSetName(setSpec)) {
//            throw new OaiException(verb, "badArgument", "unknown set name: " + setSpec);
//        }
//    }
}

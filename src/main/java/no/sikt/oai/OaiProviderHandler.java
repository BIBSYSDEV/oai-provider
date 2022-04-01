package no.sikt.oai;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import no.sikt.oai.adapter.Adapter;
import no.sikt.oai.adapter.DlrAdapter;
import no.sikt.oai.adapter.NvaAdapter;
import no.sikt.oai.data.Record;
import no.sikt.oai.data.RecordsList;
import no.sikt.oai.exception.OaiException;
import no.sikt.oai.service.DataProvider;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.google.common.net.MediaType.APPLICATION_XML_UTF_8;

public class OaiProviderHandler extends ApiGatewayHandler<Void, String> {

    public static final String EMPTY_STRING = "";
    public static final String UNKNOWN_CLIENT_NAME = "Could not find clientName %s to initiate adapter.";
    public static final String NO_MATCHING_IDENTIFIER = "No matching identifier in: ";

    public static final String CLIENT_NAME_ENV = "OAI_CLIENT_NAME";

    private Adapter adapter;
    private DataProvider dataProvider;

    @JacocoGenerated
    public OaiProviderHandler() {
        this(new Environment());
    }

    public OaiProviderHandler(Environment environment) {
        super(Void.class, environment);
        initAdapter();
        this.dataProvider = new DataProvider(adapter);
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
                .orElse(EMPTY_STRING).toLowerCase(Locale.getDefault());
        String from = requestInfo.getQueryParameterOpt(ValidParameterKey.FROM.key)
                .orElse(EMPTY_STRING);
        String until = requestInfo.getQueryParameterOpt(ValidParameterKey.UNTIL.key)
                .orElse(EMPTY_STRING);
        String identifier = requestInfo.getQueryParameterOpt(ValidParameterKey.IDENTIFIER.key)
                .orElse(EMPTY_STRING);

        String response;

        try {
            validateAllParameters(requestInfo.getQueryParameters(), verb);
            validateVerb(verb);

            long startTime = System.currentTimeMillis();

            RecordsList recordsList;
            switch (Verb.valueOf(verb)) {
                case GetRecord:
                    validateMetadataPrefix(verb, metadataPrefix);
                    validateIdentifier(verb, identifier, adapter.getRepositoryName());
                    Record record = adapter.getRecord(identifier);
                    response = OaiResponse.getRecord(record, identifier, metadataPrefix, setSpec, adapter.getBaseUrl(),
                            startTime);
                    break;
                case ListRecords:
                    validateRequiredParameters(verb, resumptionToken, metadataPrefix);
                    validateMetadataPrefix(verb, metadataPrefix);
                    validateFromAndUntilParameters(verb, from, until);
                    validateSet(verb, setSpec);
                    validateResumptionToken(verb, resumptionToken);
                    recordsList = adapter.getRecords(from, until, setSpec, 0);
                    response = OaiResponse.listRecords(from, until, resumptionToken, metadataPrefix,
                            adapter.getBaseUrl(), 0, setSpec, recordsList, startTime);
                    break;
                case ListIdentifiers:
                    validateRequiredParameters(verb, resumptionToken, metadataPrefix);
                    validateMetadataPrefix(verb, metadataPrefix);
                    validateFromAndUntilParameters(verb, from, until);
                    validateSet(verb, setSpec);
                    validateResumptionToken(verb, resumptionToken);
                    recordsList = adapter.getRecords(from, until, setSpec, 0);
                    response = OaiResponse.listIdentifiers(from, until, metadataPrefix, resumptionToken,
                            adapter.getBaseUrl(), setSpec, 0, recordsList, startTime);
                    break;
                case ListMetadataFormats:
                    response = OaiResponse.listMetadataFormats(adapter.getBaseUrl(), metadataPrefix, null, null,
                            startTime);
                    break;
                case ListSets:
                    try {
                        List<String> institutionList = this.getInstitutionList();
                        response = OaiResponse.listSets(adapter.getBaseUrl(), institutionList, startTime);
                    } catch (OaiException e) {
                        response = OaiResponse.oaiError(adapter.getBaseUrl(), e.getErrorCode(), e.getErrorText());
                    }
                    break;
                case Identify:
                default:
                    response = OaiResponse.identify(adapter, startTime);
                    break;
            }
        } catch (OaiException e) {
            response = OaiResponse.oaiError(adapter.getBaseUrl(), e.getErrorCode(), e.getErrorText());
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
                throw new OaiException(verb, OaiConstants.BAD_ARGUMENT, OaiConstants.NOT_A_LEGAL_PARAMETER + paramKey);
            }
        }
    }

    protected void validateVerb(String verb)
            throws OaiException {
        if (verb.trim().isEmpty()) {
            throw new OaiException(verb, OaiConstants.BAD_VERB, OaiConstants.VERB_IS_MISSING);
        }
        if (!Verb.isValid(verb)) {
            throw new OaiException(verb, OaiConstants.BAD_VERB, OaiConstants.ILLEGAL_ARGUMENT);
        }
    }

    protected void validateRequiredParameters(String verb, String resumptionToken, String metadataPrefix)
            throws OaiException {
        if (EMPTY_STRING.equals(resumptionToken) && EMPTY_STRING.equals(metadataPrefix)) {
            throw new OaiException(verb, OaiConstants.BAD_ARGUMENT, OaiConstants.METADATA_PREFIX_IS_A_REQUIRED + verb);
        }
    }

    protected void validateFromAndUntilParameters(String verb, String from, String until) throws OaiException {
        if (from.length() > 0 && !TimeUtils.verifyUTCdate(from)) {
            throw new OaiException(verb, OaiConstants.BAD_ARGUMENT, OaiConstants.ILLEGAL_DATE_FROM);
        }
        if (until.length() > 0 && !TimeUtils.verifyUTCdate(until)) {
            throw new OaiException(verb, OaiConstants.BAD_ARGUMENT, OaiConstants.ILLEGAL_DATE_UNTIL);
        }
        if (from.length() != until.length()) {
            throw new OaiException(verb, OaiConstants.BAD_ARGUMENT, OaiConstants.DIFFERENT_DATE_GRANULARITIES);
        }
    }

    protected void validateSet(String verb, String setSpec) throws OaiException {
        if (setSpec.length() > 0 && !getInstitutionList().contains(setSpec)) {
            throw new OaiException(verb, OaiConstants.BAD_ARGUMENT, OaiConstants.UNKNOWN_SET_NAME + setSpec);
        }
    }

    private List<String> getInstitutionList() throws OaiException {
        String json = dataProvider.getInstitutionList();
        return adapter.parseInstitutionResponse(json);
    }

    protected void validateResumptionToken(String verb, String resumptionToken) throws OaiException {
        validateSet(verb, new ResumptionToken(resumptionToken).setSpec);
    }

    protected void validateMetadataPrefix(String verb, String metadataPrefix)
            throws OaiException {
        if (!MetadatFormat.isValid(metadataPrefix)) {
            throw new OaiException(verb, OaiConstants.CANNOT_DISSEMINATE_FORMAT, OaiConstants.METADATA_FORMAT_NOT_SUPPORTED);
        }
    }

    protected void validateIdentifier(String verb, String identifier, String repositoryName)
            throws OaiException {
        if (!adapter.isValidIdentifier(identifier)) {
            throw new OaiException(verb, OaiConstants.ID_DOES_NOT_EXIST, NO_MATCHING_IDENTIFIER + repositoryName);
        }
    }
}

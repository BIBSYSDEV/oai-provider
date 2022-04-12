package no.sikt.oai;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import no.sikt.oai.adapter.Adapter;
import no.sikt.oai.adapter.DlrAdapter;
import no.sikt.oai.adapter.NvaAdapter;
import no.sikt.oai.data.Record;
import no.sikt.oai.data.RecordsList;
import no.sikt.oai.exception.InternalOaiException;
import no.sikt.oai.exception.OaiException;
import no.sikt.oai.service.DataProvider;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.google.common.net.MediaType.APPLICATION_XML_UTF_8;

public class OaiProviderHandler extends ApiGatewayHandler<Void, String> {

    public static final String EMPTY_STRING = "";
    public static final String UNKNOWN_CLIENT_NAME = "Could not find clientName %s to initiate adapter.";
    public static final String NO_MATCHING_IDENTIFIER = "No matching identifier in: ";

    private Adapter adapter;
    private final DataProvider dataProvider;

    @JacocoGenerated
    public OaiProviderHandler() {
        this(new Environment(), HttpClient.newBuilder().build());
    }

    public OaiProviderHandler(Environment environment, HttpClient client) {
        super(Void.class, environment);
        initAdapter();
        this.dataProvider = new DataProvider(client, adapter);
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
                    OaiIdentifier oaiIdentifier = new OaiIdentifier(identifier, adapter.getIdentifierPrefix());
                    validateIdentifier(verb, oaiIdentifier.getIdentifier(), adapter.getRepositoryName());
                    Record record = getRecord(oaiIdentifier.getIdentifier(), metadataPrefix);
                    response = OaiResponse.getRecord(record, oaiIdentifier.toString(), metadataPrefix, setSpec,
                            adapter.getBaseUrl(), startTime);
                    break;
                case ListRecords:
                    validateRequiredParameters(verb, resumptionToken, metadataPrefix);
                    if (resumptionToken.length() > 0) {
                        validateResumptionToken(verb, resumptionToken);
                    } else {
                        validateMetadataPrefix(verb, metadataPrefix);
                        validateFromAndUntilParameters(verb, from, until);
                        validateSet(verb, setSpec);
                    }
                    recordsList = getRecordsList(verb, from, until, setSpec, metadataPrefix, resumptionToken, 0);
                    response = OaiResponse.listRecords(from, until, resumptionToken, metadataPrefix,
                            adapter.getBaseUrl(), 0, setSpec, recordsList, startTime);
                    break;
                case ListIdentifiers:
                    validateRequiredParameters(verb, resumptionToken, metadataPrefix);
                    if (resumptionToken.length() > 0) {
                        validateResumptionToken(verb, resumptionToken);
                    } else {
                        validateMetadataPrefix(verb, metadataPrefix);
                        validateFromAndUntilParameters(verb, from, until);
                        validateSet(verb, setSpec);
                    }
                    recordsList = getRecordsList(verb, from, until, setSpec, metadataPrefix, resumptionToken, 0);
                    response = OaiResponse.listIdentifiers(from, until, metadataPrefix, resumptionToken,
                            adapter.getBaseUrl(), setSpec, 0, recordsList, startTime);
                    break;
                case ListMetadataFormats:
                    response = OaiResponse.listMetadataFormats(adapter.getBaseUrl(), metadataPrefix, null, null,
                            startTime);
                    break;
                case ListSets:
                    List<String> institutionList = this.getInstitutionList();
                    response = OaiResponse.listSets(adapter.getBaseUrl(), institutionList, startTime);
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
        String clientName = environment.readEnv(OaiConstants.CLIENT_NAME_ENV);
        switch (clientName) {
            case OaiConstants.CLIENT_TYPE_DLR:
                this.adapter = new DlrAdapter(environment);
                break;
            case OaiConstants.CLIENT_TYPE_NVA:
                this.adapter = new NvaAdapter(environment);
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
                throw new OaiException(OaiConstants.BAD_ARGUMENT, OaiConstants.NOT_A_LEGAL_PARAMETER + paramKey);
            }
        }
    }

    protected void validateVerb(String verb)
            throws OaiException {
        if (verb.trim().isEmpty()) {
            throw new OaiException(OaiConstants.BAD_VERB, OaiConstants.VERB_IS_MISSING);
        }
        if (!Verb.isValid(verb)) {
            throw new OaiException(OaiConstants.BAD_VERB, OaiConstants.ILLEGAL_ARGUMENT);
        }
    }

    protected void validateRequiredParameters(String verb, String resumptionToken, String metadataPrefix)
            throws OaiException {
        if (EMPTY_STRING.equals(resumptionToken) && EMPTY_STRING.equals(metadataPrefix)) {
            throw new OaiException(OaiConstants.BAD_ARGUMENT, OaiConstants.METADATA_PREFIX_IS_A_REQUIRED + verb);
        }
    }

    protected void validateFromAndUntilParameters(String verb, String from, String until) throws OaiException {
        if (from.length() > 0 && !TimeUtils.isUTCdate(from)) {
            throw new OaiException(OaiConstants.BAD_ARGUMENT, OaiConstants.ILLEGAL_DATE_FROM);
        }
        if (until.length() > 0 && !TimeUtils.isUTCdate(until)) {
            throw new OaiException(OaiConstants.BAD_ARGUMENT, OaiConstants.ILLEGAL_DATE_UNTIL);
        }
        if (from.length() > 0 && until.length() > 0) {
            if (from.length() != until.length()) {
                throw new OaiException(OaiConstants.BAD_ARGUMENT, OaiConstants.DIFFERENT_DATE_GRANULARITIES);
            }
        }
    }

    protected void validateSet(String verb, String setSpec) throws OaiException, InternalOaiException {
        if (setSpec.length() > 0 && !getInstitutionList().contains(setSpec)) {
            throw new OaiException(OaiConstants.BAD_ARGUMENT, OaiConstants.UNKNOWN_SET_NAME + setSpec);
        }
    }

    private List<String> getInstitutionList() throws OaiException, InternalOaiException {
        String json = dataProvider.getSetsList();
        return adapter.parseInstitutionResponse(json);
    }

    private Record getRecord(String identifier, String metadataPrefix) throws InternalOaiException, OaiException {
        String json = dataProvider.getRecord(identifier);
        return adapter.parseRecordResponse(json, metadataPrefix);
    }

    private RecordsList getRecordsList(String verb, String from, String until, String setSpec, String metadataPrefix,
                                       String resumptionToken, int startPosition)
            throws OaiException, InternalOaiException {
        String json;
        if (resumptionToken.length() > 0) {
            ResumptionToken token = new ResumptionToken(resumptionToken);
            json = dataProvider.getRecordsList(token.from, token.until, token.setSpec,
                    Integer.parseInt(token.startPosition));
        } else {
            json = dataProvider.getRecordsList(from, until, setSpec, startPosition);
        }
        return adapter.parseRecordsListResponse(verb, json, metadataPrefix);
    }

    protected void validateResumptionToken(String verb, String resumptionToken)
            throws OaiException, InternalOaiException {
        validateSet(verb, new ResumptionToken(resumptionToken).setSpec);
    }

    protected void validateMetadataPrefix(String verb, String metadataPrefix)
            throws OaiException {
        if (!MetadataFormat.isValid(metadataPrefix)) {
            throw new OaiException(OaiConstants.CANNOT_DISSEMINATE_FORMAT,
                    OaiConstants.METADATA_FORMAT_NOT_SUPPORTED);
        }
    }

    protected void validateIdentifier(String verb, String identifier, String repositoryName)
            throws OaiException {
        if (!adapter.isValidIdentifier(identifier)) {
            throw new OaiException(OaiConstants.ID_DOES_NOT_EXIST, NO_MATCHING_IDENTIFIER + repositoryName);
        }
    }
}

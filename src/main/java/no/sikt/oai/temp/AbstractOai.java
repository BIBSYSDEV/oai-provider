package no.sikt.oai.temp;

import no.sikt.oai.temp.data.Record;
import no.sikt.oai.temp.data.RecordsList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractOai implements Oai {

	private static final Logger logger = LoggerFactory.getLogger(AbstractOai.class);
	
	protected final String prefix;
	protected final OAIConfig oaiConfig;
	protected final MetadataFormatValidator metadataFormatValidator;
	protected final ResumptionTokenValidator resumptionTokenValidator;

	
	public AbstractOai(OAIConfig oaiConfig, String prefix) {
		this.oaiConfig = oaiConfig;
		this.prefix = prefix;
		this.metadataFormatValidator = new MetadataFormatValidator(oaiConfig);
		this.resumptionTokenValidator = new ResumptionTokenValidator(oaiConfig);
	}
	
    @Override
	public String Identify(String verb, String baseUrl, long startTime) {
		StringBuilder response = new StringBuilder();

        makeHeader(response, false);
        makeHeaderRequest(verb, baseUrl, response);
        makeVerbStart(verb, response);
        makeIdentify(baseUrl, response);
        makeVerbEnd(verb, response);
        makeFooter(response);
        makeTimeUsed(verb,startTime, response);
        
		return response.toString();
	}
    
    @Override
    public String ListMetadataFormats(String verb, String baseUrl, long startTime) {
		StringBuilder response = new StringBuilder();

        makeHeader(response, false);
        makeHeaderRequest(verb, baseUrl, response);
        makeVerbStart(verb, response);
        makeListMetadataFormats(response);
        makeVerbEnd(verb, response);
        makeFooter(response);
        makeTimeUsed(verb,startTime, response);
        
		return response.toString();
	}
    
    @Override
	public String GetRecord(Record record, String verb, String identifier, String metadataPrefix, String baseUrl,
			long startTime) throws OAIException {
		OAIIdentifier oaiIdentifier = new OAIIdentifier(identifier, prefix);

		StringBuilder response = new StringBuilder(1000);
		makeHeader(response, true);
		makeHeaderRequestGetRecord(verb, metadataPrefix, oaiIdentifier, baseUrl, response);
		makeVerbStart(verb, response);
		makeRecord(record.isDeleted, record.identifier, record.lastUpdateDate, record.content, "", response);
		makeVerbEnd(verb, response);
		makeFooter(response);
		makeTimeUsed(verb,startTime, response);
		
		return response.toString();
	}
    
    @Override
	public String ListIdentifiers(String verb, String from, String until, String metadataPrefix,
			Optional<String> resumptionToken, String baseUrl, long startTime, int startPosition,
			OAIDatasetDefinition datasetDef, RecordsList records) {
		
        StringBuilder response = new StringBuilder();
		
		makeHeader(response, true);
		makeHeaderRequestListRecordsIdentifiers(verb, resumptionToken, from, until, metadataPrefix, baseUrl, response);
		makeVerbStart(verb, response);

		for (Record record : records) {
		    try {
		        makeRecordHeader(record.isDeleted, record.identifier, record.lastUpdateDate, datasetDef.getSetSpec(), response);
		    } catch (Exception e) {
		        logger.error(verb + " Exception: " + record.identifier + " " + e.getMessage());
		    }
		}

		String newResumptionToken = "";
		long recordsRemaining = records.numFound() - startPosition + records.size();

		if (recordsRemaining > 0) {
		    ResumptionToken nyTok = new ResumptionToken("lr", System.currentTimeMillis(), datasetDef.getSetSpec(), ((from == null) ? "" : from), ((until == null) ? "" : until), metadataPrefix, startPosition + records.size() + "");
		    newResumptionToken = nyTok.asString();
		}

		makeFooterListIdentifiers(records.numFound() + "", newResumptionToken, response);
		makeVerbEnd(verb, response);
		makeFooter(response);
		makeTimeUsed(verb,startTime, response);
		
		return response.toString();
	}
    
    @Override
	public String ListRecords(String verb, String from, String until, Optional<String> resumptionToken,
			String metadataPrefix, String baseUrl, long startTime, int startPosition,
			OAIDatasetDefinition datasetDef, RecordsList records) {
    	StringBuilder response = new StringBuilder();
		String newResumptionToken = "";

		logger.debug(verb+ " ResumptionToken: " + newResumptionToken);

		makeHeader(response, true);
		makeHeaderRequestListRecordsIdentifiers(verb, resumptionToken, from, until, metadataPrefix, baseUrl, response);

		makeVerbStart(verb, response);
		logger.debug(verb + "Itererer over poster, foer while loekke");

		for (Record record : records) {
		    try {
		        makeRecord(record.isDeleted, record.identifier, record.lastUpdateDate, record.content, datasetDef.getSetSpec(), response);
		    } catch (Exception e) {
		        logger.error(verb + " Exception: " + record.identifier + " " + e.getMessage());
		    }
		}

		long recordsRemaining = records.numFound() - (startPosition + records.size());

		if (recordsRemaining > 0) {
		    ResumptionToken nyTok = new ResumptionToken("lr", System.currentTimeMillis(), datasetDef.getSetSpec(), ((from == null) ? "" : from), ((until == null) ? "" : until), metadataPrefix, startPosition + records.size() + "");
		    newResumptionToken = nyTok.asString();
		}

		logger.debug(verb + " nytt resumptionToken= " + newResumptionToken);

		makeFooterListRecords(records.numFound() + "", newResumptionToken, startPosition + records.size() + "", response);

		makeVerbEnd(verb, response);
		makeFooter(response);
		makeTimeUsed(verb,startTime, response);

		// TODO Oppdater statistikk for innhøsting
		oaiConfig.updateRecordCount(datasetDef.getSetSpec(), records.numFound());
		oaiConfig.updateTimeStamp(datasetDef.getSetSpec(), System.currentTimeMillis());
		
		return response.toString();
	}
    
    @Override
	public String ListSets(String verb, String baseUrl, long startTime) {
		StringBuilder response = new StringBuilder(1000);
        makeHeader(response, false);
        makeHeaderRequest(verb, baseUrl, response);
        makeVerbStart(verb, response);
        makeListSets(response);
        makeVerbEnd(verb, response);
        makeFooter(response);
        makeTimeUsed(verb,startTime, response);
        
		return response.toString();
	}
    
    // OAI Helpers
	
	protected void makeHeader(StringBuilder buffer, boolean withMarcxchange) {
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + "\n");
        buffer.append("<OAI-PMH  xmlns=\"http://www.openarchives.org/OAI/2.0/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        if(withMarcxchange) {
            buffer.append("xmlns:marc=\"info:lc/xmlns/marcxchange-v1\" ");
        }
        buffer.append("xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd");
        if(withMarcxchange){
            buffer.append(" info:lc/xmlns/marcxchange-v1 http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd");
        }
        buffer.append("\">\n");
        buffer.append("    <responseDate>" + TimeUtils.getResponseTime() + "</responseDate>" + "\n");
    }

	protected void makeFooter(StringBuilder buffer) {
        buffer.append("</OAI-PMH>\n");
    }

    public void makeVerbStart(String verb, StringBuilder buffer) {
        buffer.append("    <").append(verb).append(">\n");
    }

    public void makeVerbEnd(String verb, StringBuilder buffer) {
        buffer.append("    </").append(verb).append(">\n");
    }

    protected void makeHeaderRequest(String verb, String baseUrl, StringBuilder response) {
        response.append("    <request verb=\"" + verb + "\">").append(baseUrl).append("</request>\n");
    }

    protected void makeRecord(boolean isDeleted, String identifier, Date lastUpdateDate, String xmlContent, String set, StringBuilder buffer) throws OAIException {
        buffer.append("        <record>\n");
        if (isDeleted) {
            buffer.append("            <header status=\"deleted\">\n");
        } else {
            buffer.append("            <header>\n");
        }
        buffer.append("                <identifier>").append(new OAIIdentifier(prefix+identifier, prefix)).append("</identifier>\n");
        buffer.append("                <datestamp>").append(TimeUtils.Date2String(lastUpdateDate, TimeUtils.FORMAT_ZULU_LONG)).append("</datestamp>\n");
        if (set != null && set.length() > 0 && !set.equalsIgnoreCase("default")) {
            buffer.append("                <setSpec>").append(set).append("</setSpec>\n");
        }
        buffer.append("            </header>\n");
            buffer.append("            <metadata>\n");

            // Kun for å få riktig innrykk...
            String[] recordXml = xmlContent.split("\\r?\\n");
            for (String recordXmlPart : recordXml){
                buffer.append("                " + recordXmlPart + "\n");
            }

            buffer.append("            </metadata>\n");
        buffer.append("        </record>\n");
    }

    protected void makeRecordHeader(boolean isDeleted, String identifier, Date lastUpdateDate, String set, StringBuilder buffer) throws OAIException {
        if (isDeleted) {
            buffer.append("        <header status=\"deleted\">\n");
        } else {
            buffer.append("        <header>\n");
        }
        buffer.append("            <identifier>").append(new OAIIdentifier(prefix+identifier, prefix)).append("</identifier>\n");
        buffer.append("            <datestamp>").append(TimeUtils.Date2String(lastUpdateDate, TimeUtils.FORMAT_ZULU_LONG)).append("</datestamp>\n");
        if (set != null && set.length() > 0 && !set.equalsIgnoreCase("default")) {
            buffer.append("            <setSpec>").append(set).append("</setSpec>\n");
        }
        buffer.append("        </header>\n");
    }

    protected void makeTimeUsed(String verb, long startTime, StringBuilder buffer){
        long timeUsed = System.currentTimeMillis() - startTime;
        buffer.append("\n<!-- Time used "+verb+" " + timeUsed + " ms. -->");
    }


    // OAI helpers: GetRecord

    protected void makeHeaderRequestGetRecord(String verb, String metadataPrefix, OAIIdentifier identifier, String baseUrl, StringBuilder buffer) {
        buffer.append("    <request verb=\"" + verb + "\" identifier=\"" + identifier + "\" metadataPrefix=\"" + metadataPrefix + "\">" + baseUrl + "</request>\n");
    }


    // OAI helpers: ListRecords

    protected void makeFooterListRecords(String listSize, String newToken, String cursor, StringBuilder buffer) {
        if (newToken != null && newToken.length() > 0) {
            buffer.append("        <resumptionToken completeListSize=\"" + listSize + "\"  cursor=\"" + cursor + "\">" + newToken + "</resumptionToken>\n");
        }
    }


    // OAI helpers: ListIdentifiers

    protected void makeFooterListIdentifiers(String listSize, String newToken, StringBuilder buffer) {
        if (newToken != null && newToken.length() > 0) {
            buffer.append("        <resumptionToken completeListSize=\"").append(listSize).append("\">").append(newToken).append("</resumptionToken>\n");
        }
    }


    // OAI helpers: ListRecords & ListItentifiers

    protected void makeHeaderRequestListRecordsIdentifiers(String verb, Optional<String> oldResumptionToken, String from, String until, String metadataPrefix, String baseUrl, StringBuilder buffer) {

        boolean writeParams = true;

        buffer.append("    <request verb=\"").append(verb).append("\" ");
        if (oldResumptionToken.isPresent() && oldResumptionToken.get().length() > 0) {
            writeParams = false;
            buffer.append(" resumptionToken=\"").append(oldResumptionToken.get()).append("\" ");
        }
        if (from != null && (from.length() > 9) && writeParams) {
            buffer.append(" from=\"").append(from).append("\"");
        }
        if (until != null && (until.length() > 9) && writeParams) {
            buffer.append(" until=\"").append(until).append("\"");
        }
        if (writeParams) {
            buffer.append(" metadataPrefix=\"").append(metadataPrefix).append("\"");
        }
        buffer.append(">").append(baseUrl).append("</request>\n");
    }

    // OAI helpers: ListSets

    protected void makeListSets(StringBuilder response) {

        ArrayList<OAIDatasetDefinition> c = new ArrayList<>(oaiConfig.getDatasets());
        Collections.sort(c);

        for (OAIDatasetDefinition setDef : c) {
            if (!setDef.getSetSpec().equalsIgnoreCase("default")) {
                response.append("        <set>\n");
                response.append("            <setSpec>").append(setDef.getSetSpec()).append("</setSpec>\n");
                response.append("            <setName>").append(setDef.getSettNavn()).append("</setName>\n");
                response.append("        </set>\n");
            }
        }
    }


    // OAI helpers: Error

    protected String createErrorResponse(String serverName, String requestURI, OAIException e) {
        StringBuilder response = new StringBuilder();
        makeHeader(response, false);
        response.append("    <request>" + serverName + requestURI + "</request>\n");
        response.append("    <error code=\"" + e.errorCode + "\">" + e.errorText + "</error> \n");
        makeFooter(response);
        return response.toString();
    }


    // OAI helpers: Identify

    protected void makeIdentify(String baseUrl, StringBuilder buffer) {
        buffer.append("        <repositoryName>").append(oaiConfig.getRepositoryName()).append("</repositoryName>\n");
        buffer.append("        <baseURL>").append(baseUrl).append("</baseURL>\n");
        buffer.append("        <protocolVersion>").append(oaiConfig.getProtocolVersion()).append("</protocolVersion>\n");
        buffer.append("        <adminEmail>").append(oaiConfig.getAdminEmail()).append("</adminEmail>\n");
        buffer.append("        <earliestDatestamp>").append(oaiConfig.getEarliestTimestamp()).append("</earliestDatestamp>\n");
        buffer.append("        <deletedRecord>").append(oaiConfig.getDeletedRecord()).append("</deletedRecord>\n");
        buffer.append("        <granularity>").append(oaiConfig.getDateGranularity()).append("</granularity>\n");
        buffer.append("        <description>").append("Repository contains authority metadata. Dublin Core not suitable.").append("</description>\n");
    }


    // OAI helpers: ListMetadataFormats

    
    protected void makeListMetadataFormats(StringBuilder buffer) {
        buffer.append("        <metadataFormat>\n");
        buffer.append("            <metadataPrefix>marcxchange</metadataPrefix>\n");
        buffer.append("            <schema>http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd</schema>\n");
        buffer.append("            <metadataNamespace>info:lc/xmlns/marcxchange-v1</metadataNamespace>\n");
        buffer.append("        </metadataFormat>\n");
    }


    // Input Validation

    protected void validateAllParameters(Map<String, String[]> paramMap, String verb) throws OAIException {
        for (String paramKey : paramMap.keySet()) {
            if (!ValidParameterKey.isValidParameterkey(paramKey)) {
                throw new OAIException(verb, "badArgument", "Not a legal parameter: " + paramKey);
            }
        }
    }

    protected void validateVerbAndRequiredParameters(String verb, Optional<String> resumptionToken, String metadataPrefix) throws OAIException {
        if (verb == null || verb.trim().isEmpty()) {
            throw new OAIException(verb, "badArgument", "'verb' is missing");
        }
        
        if (verb.equalsIgnoreCase("listrecords") || verb.equalsIgnoreCase("getrecord")) {
            if ("".equals(resumptionToken.orElse("")) && "".equals(metadataPrefix)) {
                throw new OAIException(verb, "badArgument", "metadataPrefix is a required argument for the verb " + verb);
            }
        }
    }

    protected void validateFromAndUntilParameters(String verb, String from, String until) throws OAIException {
        if (from != null && from.length() > 0 && !TimeUtils.verifyUTCdate(from)) {
            throw new OAIException(verb, "badArgument", "Not a legal date FROM, use YYYY-MM-DD or " + oaiConfig.getDateGranularity());
        }
        if (until != null && until.length() > 0 && !TimeUtils.verifyUTCdate(until)) {
            throw new OAIException(verb, "badArgument", "Not a legal date UNTIL, use YYYY-MM-DD or " + oaiConfig.getDateGranularity());
        }
        if (from != null && until != null && from.length() > 0 && until.length() > 0) {
            if (from.length() != until.length()) {
                throw new OAIException(verb, "badArgument", "The request has different granularities for the from and until parameters.");
            }
        }
    }

    protected void validateSetAndMetadataPrefix(String verb, String setSpec, String metadataPrefix) throws OAIException {
        if (!metadataFormatValidator.isValid(metadataPrefix)) {
            throw new OAIException(verb, "cannotDisseminateFormat",
                    "--The metadata format identified by the value given for the \nmetadataPrefix argument is not supported by the item or by the repository.");
        }
        if (setSpec != null && setSpec.length() > 0 && !oaiConfig.isValidSetName(setSpec)) {
            throw new OAIException(verb, "badArgument", "unknown set name: " + setSpec);
        }
    }	
}

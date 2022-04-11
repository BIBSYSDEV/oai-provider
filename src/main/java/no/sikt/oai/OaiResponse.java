package no.sikt.oai;

import no.sikt.oai.adapter.Adapter;
import no.sikt.oai.data.Record;
import no.sikt.oai.data.RecordsList;
import nva.commons.core.JacocoGenerated;

import java.util.Date;
import java.util.List;

import static no.sikt.oai.TimeUtils.Date2String;
import static no.sikt.oai.TimeUtils.FORMAT_ZULU_LONG;
import static no.sikt.oai.Verb.GetRecord;
import static no.sikt.oai.Verb.Identify;
import static no.sikt.oai.Verb.ListIdentifiers;
import static no.sikt.oai.Verb.ListMetadataFormats;
import static no.sikt.oai.Verb.ListRecords;
import static no.sikt.oai.Verb.ListSets;

public class OaiResponse {

    @JacocoGenerated
    public OaiResponse() {
    }

    public static String identify(Adapter adapter, long startTime) {
        StringBuilder buffer = new StringBuilder();
        makeHeader(buffer);
        makeHeaderRequest(Identify.name(), adapter.getBaseUrl(), buffer);
        makeVerbStart(Identify.name(), buffer);
        makeIdentify(adapter, buffer);
        makeVerbEnd(Identify.name(), buffer);
        makeFooter(buffer);
        makeTimeUsed(Identify.name(), startTime, buffer);
        return buffer.toString();
    }

    public static String listMetadataFormats(String baseUrl, String metadataPrefix, String schema,
                                             String metadataNamespace, long startTime) {
        StringBuilder buffer = new StringBuilder();
        makeHeader(buffer);
        makeHeaderRequest(ListMetadataFormats.name(), baseUrl, buffer);
        makeVerbStart(ListMetadataFormats.name(), buffer);
        makeListMetadataFormats(metadataPrefix, schema, metadataNamespace, buffer);
        makeVerbEnd(ListMetadataFormats.name(), buffer);
        makeFooter(buffer);
        makeTimeUsed(ListMetadataFormats.name(), startTime, buffer);
        return buffer.toString();
    }

    public static String getRecord(Record record, String identifier, String metadataPrefix, String setSpec,
                                   String baseUrl, long startTime) {
        StringBuilder buffer = new StringBuilder(1000);
        makeHeader(buffer);
        makeHeaderRequestGetRecord(GetRecord.name(), metadataPrefix, identifier, baseUrl, buffer);
        makeVerbStart(GetRecord.name(), buffer);
        makeRecord(record.isDeleted, record.identifier, record.lastUpdateDate, record.content, setSpec, buffer);
        makeVerbEnd(GetRecord.name(), buffer);
        makeFooter(buffer);
        makeTimeUsed(GetRecord.name(), startTime, buffer);
        return buffer.toString();
    }

    public static String listIdentifiers(String from, String until, String metadataPrefix,
                                         String resumptionToken, String baseUrl, String setSpec,
                                         int startPosition, RecordsList records, long startTime) {
        StringBuilder buffer = new StringBuilder();
        makeHeader(buffer);
        makeHeaderRequestListRecordsIdentifiers(ListIdentifiers.name(), resumptionToken, from, until, metadataPrefix,
                baseUrl, buffer);
        makeVerbStart(ListIdentifiers.name(), buffer);

        for (Record record : records) {
            makeRecordHeader(record.isDeleted, record.identifier, record.lastUpdateDate, setSpec, buffer);
        }

        String newResumptionToken = "";
        long recordsRemaining = records.numFound() - startPosition + records.size();

        if (recordsRemaining > 0) {
            ResumptionToken nyTok = new ResumptionToken("lr", System.currentTimeMillis(), setSpec,
                    ((from == null) ? "" : from), ((until == null) ? "" : until), metadataPrefix,
                    startPosition + records.size() + "");
            newResumptionToken = nyTok.asString();
        }
        makeFooterListIdentifiers(records.numFound() + "", newResumptionToken, buffer);
        makeVerbEnd(ListIdentifiers.name(), buffer);
        makeFooter(buffer);
        makeTimeUsed(ListIdentifiers.name(), startTime, buffer);
        return buffer.toString();
    }

    public static String listRecords(String from, String until, String resumptionToken, String metadataPrefix,
                                     String baseUrl, int startPosition, String setSpec, RecordsList records,
                                     long startTime) {
        StringBuilder buffer = new StringBuilder();
        String newResumptionToken = "";
        makeHeader(buffer);
        makeHeaderRequestListRecordsIdentifiers(ListRecords.name(), resumptionToken, from, until, metadataPrefix,
                baseUrl, buffer);
        makeVerbStart(ListRecords.name(), buffer);

        for (Record record : records) {
            makeRecord(record.isDeleted, record.identifier, record.lastUpdateDate, record.content, setSpec, buffer);
        }

        long recordsRemaining = records.numFound() - (startPosition + records.size());

        if (recordsRemaining > 0) {
            ResumptionToken nyTok = new ResumptionToken("lr", System.currentTimeMillis(), setSpec,
                    ((from == null) ? "" : from), ((until == null) ? "" : until), metadataPrefix, startPosition + records.size() + "");
            newResumptionToken = nyTok.asString();
        }
        makeFooterListRecords(records.numFound() + "", newResumptionToken, startPosition + records.size()
                + "", buffer);
        makeVerbEnd(ListRecords.name(), buffer);
        makeFooter(buffer);
        makeTimeUsed(ListRecords.name(), startTime, buffer);
        return buffer.toString();
    }

    public static String listSets(String baseUrl, List<String> setList, long startTime) {
        StringBuilder buffer = new StringBuilder(1000);
        makeHeader(buffer);
        makeHeaderRequest(ListSets.name(), baseUrl, buffer);
        makeVerbStart(ListSets.name(), buffer);
        for (String set : setList) {
            makeListSets(set, set, buffer);
        }
        makeVerbEnd(ListSets.name(), buffer);
        makeFooter(buffer);
        makeTimeUsed(ListSets.name(), startTime, buffer);
        return buffer.toString();
    }

    public static String oaiError(String baseUrl, String errorCode, String errorMessage) {
        StringBuilder stringBuilder = new StringBuilder();
        makeHeader(stringBuilder);
        makeHeaderRequest(baseUrl, stringBuilder);
        makeError(errorCode, errorMessage, stringBuilder);
        makeFooter(stringBuilder);
        return stringBuilder.toString();
    }

    // OAI Helpers

    protected static void makeHeader(StringBuilder buffer) {
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + "\n");
        buffer.append("<OAI-PMH  xmlns=\"http://www.openarchives.org/OAI/2.0/\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        buffer.append("xsi:schemaLocation=" +
                "\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd");
        buffer.append("\">\n");
        buffer.append("    <responseDate>").append(TimeUtils.getResponseTime()).append("</responseDate>").append("\n");
    }

    protected static void makeFooter(StringBuilder buffer) {
        buffer.append("</OAI-PMH>\n");
    }

    public static void makeVerbStart(String verb, StringBuilder buffer) {
        buffer.append("    <").append(verb).append(">\n");
    }

    public static void makeVerbEnd(String verb, StringBuilder buffer) {
        buffer.append("    </").append(verb).append(">\n");
    }

    protected static void makeHeaderRequest(String verb, String baseUrl, StringBuilder buffer) {
        buffer.append("    <request verb=\"").append(verb).append("\">").append(baseUrl).append("</request>\n");
    }

    protected static void makeHeaderRequest(String baseUrl, StringBuilder stringBuilder) {
        stringBuilder.append("    <request>").append(baseUrl).append("</request>\n");
    }

    protected static void makeError(String errorCode, String errorMessage, StringBuilder stringBuilder) {
        stringBuilder.append("    <error code=\"").append(errorCode).append("\">").append(errorMessage)
                .append("</error>\n");
    }

    protected static void makeRecord(boolean isDeleted, String identifier, Date lastUpdateDate, String xmlContent,
                                     String setSpec, StringBuilder buffer) {
        buffer.append("        <record>\n");
        if (isDeleted) {
            buffer.append("            <header status=\"deleted\">\n");
        } else {
            buffer.append("            <header>\n");
        }
        buffer.append("                <identifier>").append(identifier).append("</identifier>\n");
        buffer.append("                <datestamp>").append(Date2String(lastUpdateDate, FORMAT_ZULU_LONG))
                .append("</datestamp>\n");
        if (setSpec.length() > 0) {
            buffer.append("                <setSpec>").append(setSpec).append("</setSpec>\n");
        }
        buffer.append("            </header>\n");
        buffer.append("            <metadata>\n");

        // Kun for å få riktig innrykk...
        String[] recordXml = xmlContent.split("\\r?\\n");
        for (String recordXmlPart : recordXml) {
            buffer.append("                ").append(recordXmlPart).append("\n");
        }
        buffer.append("            </metadata>\n");
        buffer.append("        </record>\n");
    }

    protected static void makeRecordHeader(boolean isDeleted, String identifier, Date lastUpdateDate, String setSpec,
                                           StringBuilder buffer) {
        if (isDeleted) {
            buffer.append("        <header status=\"deleted\">\n");
        } else {
            buffer.append("        <header>\n");
        }
        buffer.append("            <identifier>").append(identifier).append("</identifier>\n");
        buffer.append("            <datestamp>").append(Date2String(lastUpdateDate, FORMAT_ZULU_LONG))
                .append("</datestamp>\n");
        if (setSpec.length() > 0) {
            buffer.append("            <setSpec>").append(setSpec).append("</setSpec>\n");
        }
        buffer.append("        </header>\n");
    }

    protected static void makeTimeUsed(String verb, long startTime, StringBuilder buffer) {
        long timeUsed = System.currentTimeMillis() - startTime;
        buffer.append("\n<!-- Time used ").append(verb).append(" ").append(timeUsed).append(" ms. -->");
    }


    // OAI helpers: GetRecord

    protected static void makeHeaderRequestGetRecord(String verb, String metadataPrefix, String identifier,
                                                     String baseUrl, StringBuilder buffer) {
        buffer.append("    <request verb=\"").append(verb).append("\" identifier=\"").append(identifier)
                .append("\" metadataPrefix=\"").append(metadataPrefix).append("\">").append(baseUrl)
                .append("</request>\n");
    }


    // OAI helpers: ListRecords

    protected static void makeFooterListRecords(String listSize, String newToken, String cursor, StringBuilder buffer) {
        if (newToken.length() > 0) {
            buffer.append("        <resumptionToken completeListSize=\"").append(listSize).append("\"  cursor=\"")
                    .append(cursor).append("\">").append(newToken).append("</resumptionToken>\n");
        }
    }


    // OAI helpers: ListIdentifiers

    protected static void makeFooterListIdentifiers(String listSize, String newToken, StringBuilder buffer) {
        if (newToken.length() > 0) {
            buffer.append("        <resumptionToken completeListSize=\"").append(listSize).append("\">")
                    .append(newToken).append("</resumptionToken>\n");
        }
    }


    // OAI helpers: ListRecords & ListItentifiers

    protected static void makeHeaderRequestListRecordsIdentifiers(String verb, String oldResumptionToken, String from,
                                                                  String until, String metadataPrefix, String baseUrl,
                                                                  StringBuilder buffer) {
        boolean writeParams = true;

        buffer.append("    <request verb=\"").append(verb).append("\" ");
        if (oldResumptionToken != null) {
            writeParams = false;
            buffer.append(" resumptionToken=\"").append(oldResumptionToken).append("\" ");
        }
        if ((from.length() > 9) && writeParams) {
            buffer.append(" from=\"").append(from).append("\"");
        }
        if ((until.length() > 9) && writeParams) {
            buffer.append(" until=\"").append(until).append("\"");
        }
        if (writeParams) {
            buffer.append(" metadataPrefix=\"").append(metadataPrefix).append("\"");
        }
        buffer.append(">").append(baseUrl).append("</request>\n");
    }

    // OAI helpers: ListSets

    protected static void makeListSets(String setSpec, String setName, StringBuilder buffer) {
        buffer.append("        <set>\n");
        buffer.append("            <setSpec>").append(setSpec).append("</setSpec>\n");
        buffer.append("            <setName>").append(setName).append("</setName>\n");
        buffer.append("        </set>\n");
    }


    // OAI helpers: Identify

    protected static void makeIdentify(Adapter adapter, StringBuilder buffer) {
        buffer.append("        <repositoryName>").append(adapter.getRepositoryName()).append("</repositoryName>\n");
        buffer.append("        <baseURL>").append(adapter.getBaseUrl()).append("</baseURL>\n");
        buffer.append("        <protocolVersion>").append(adapter.getProtocolVersion()).append("</protocolVersion>\n");
        buffer.append("        <adminEmail>").append(adapter.getAdminEmail()).append("</adminEmail>\n");
        buffer.append("        <earliestDatestamp>").append(adapter.getEarliestTimestamp())
                .append("</earliestDatestamp>\n");
        buffer.append("        <deletedRecord>").append(adapter.getDeletedRecord()).append("</deletedRecord>\n");
        buffer.append("        <granularity>").append(adapter.getDateGranularity()).append("</granularity>\n");
        buffer.append("        <description>").append(adapter.getDescription()).append("</description>\n");
    }


    // OAI helpers: ListMetadataFormats

    protected static void makeListMetadataFormats(String metadataPrefix, String schema, String metadataNamespace,
                                                  StringBuilder buffer) {
        buffer.append("        <metadataFormat>\n");
        buffer.append("            <metadataPrefix>)").append(metadataPrefix).append("</metadataPrefix>\n");
        buffer.append("            <schema>").append(schema).append("</schema>\n");
        buffer.append("            <metadataNamespace>").append(metadataNamespace).append("</metadataNamespace>\n");
        buffer.append("        </metadataFormat>\n");
    }

}

package no.sikt.oai;

import no.sikt.oai.data.Record;
import no.sikt.oai.data.RecordsList;

import java.util.Date;
import java.util.Optional;

import static no.sikt.oai.Verb.GetRecord;
import static no.sikt.oai.Verb.Identify;
import static no.sikt.oai.Verb.ListIdentifiers;
import static no.sikt.oai.Verb.ListMetadataFormats;
import static no.sikt.oai.Verb.ListRecords;
import static no.sikt.oai.Verb.ListSets;

public class OaiResponse {

    public static String Identify(String repositoryName, String baseUrl, String protocolVersion, String adminEmail, String earliestTimestamp, String deletedRecord, String granularity, String description, long startTime) {
        StringBuilder buffer = new StringBuilder();

        makeHeader(buffer, false);
        makeHeaderRequest(Identify.name(), baseUrl, buffer);
        makeVerbStart(Identify.name(), buffer);
        makeIdentify(repositoryName, baseUrl, protocolVersion, adminEmail, earliestTimestamp, deletedRecord, granularity, description, buffer);
        makeVerbEnd(Identify.name(), buffer);
        makeFooter(buffer);
        makeTimeUsed(Identify.name(), startTime, buffer);

        return buffer.toString();
    }

    public static String ListMetadataFormats(String baseUrl, String metadataPrefix, String schema,
                                             String metadataNamespace, long startTime) {
        StringBuilder buffer = new StringBuilder();

        makeHeader(buffer, false);
        makeHeaderRequest(ListMetadataFormats.name(), baseUrl, buffer);
        makeVerbStart(ListMetadataFormats.name(), buffer);
        makeListMetadataFormats(metadataPrefix, schema, metadataNamespace, buffer);
        makeVerbEnd(ListMetadataFormats.name(), buffer);
        makeFooter(buffer);
        makeTimeUsed(ListMetadataFormats.name(), startTime, buffer);
        return buffer.toString();
    }

    public static String GetRecord(Record record, String identifier, String metadataPrefix, String baseUrl, long startTime) {
        StringBuilder buffer = new StringBuilder(1000);

        makeHeader(buffer, true);
        makeHeaderRequestGetRecord(GetRecord.name(), metadataPrefix, identifier, baseUrl, buffer);
        makeVerbStart(GetRecord.name(), buffer);
        makeRecord(record.isDeleted, record.identifier, record.lastUpdateDate, record.content, "", buffer);
        makeVerbEnd(GetRecord.name(), buffer);
        makeFooter(buffer);
        makeTimeUsed(GetRecord.name(), startTime, buffer);

        return buffer.toString();
    }

    public static String ListIdentifiers(String from, String until, String metadataPrefix,
                                         String resumptionToken, String baseUrl, String setSpec,
                                         int startPosition, RecordsList records, long startTime) {
        StringBuilder buffer = new StringBuilder();

        makeHeader(buffer, true);
        makeHeaderRequestListRecordsIdentifiers(ListIdentifiers.name(), resumptionToken, from, until, metadataPrefix, baseUrl, buffer);
        makeVerbStart(ListIdentifiers.name(), buffer);

        for (Record record : records) {
            makeRecordHeader(record.isDeleted, record.identifier, record.lastUpdateDate, setSpec, buffer);
        }

        String newResumptionToken = "";
        long recordsRemaining = records.numFound() - startPosition + records.size();

        if (recordsRemaining > 0) {
            ResumptionToken nyTok = new ResumptionToken("lr", System.currentTimeMillis(), setSpec, ((from == null) ? "" : from), ((until == null) ? "" : until), metadataPrefix, startPosition + records.size() + "");
            newResumptionToken = nyTok.asString();
        }

        makeFooterListIdentifiers(records.numFound() + "", newResumptionToken, buffer);
        makeVerbEnd(ListIdentifiers.name(), buffer);
        makeFooter(buffer);
        makeTimeUsed(ListIdentifiers.name(), startTime, buffer);

        return buffer.toString();
    }

    public static String ListRecords(String from, String until, String resumptionToken, String metadataPrefix,
                                     String baseUrl, int startPosition, String setSpec, RecordsList records,
                                     long startTime) {
        StringBuilder buffer = new StringBuilder();
        String newResumptionToken = "";

        makeHeader(buffer, true);
        makeHeaderRequestListRecordsIdentifiers(ListRecords.name(), resumptionToken, from, until, metadataPrefix, baseUrl, buffer);

        makeVerbStart(ListRecords.name(), buffer);

        for (Record record : records) {
            makeRecord(record.isDeleted, record.identifier, record.lastUpdateDate, record.content, setSpec, buffer);
        }

        long recordsRemaining = records.numFound() - (startPosition + records.size());

        if (recordsRemaining > 0) {
            ResumptionToken nyTok = new ResumptionToken("lr", System.currentTimeMillis(), setSpec, ((from == null) ? "" : from), ((until == null) ? "" : until), metadataPrefix, startPosition + records.size() + "");
            newResumptionToken = nyTok.asString();
        }

        makeFooterListRecords(records.numFound() + "", newResumptionToken, startPosition + records.size() + "", buffer);

        makeVerbEnd(ListRecords.name(), buffer);
        makeFooter(buffer);
        makeTimeUsed(ListRecords.name(), startTime, buffer);

        return buffer.toString();
    }

    public static String ListSets(String baseUrl, String setSpec, String setName, long startTime) {
        StringBuilder buffer = new StringBuilder(1000);

        makeHeader(buffer, false);
        makeHeaderRequest(ListSets.name(), baseUrl, buffer);
        makeVerbStart(ListSets.name(), buffer);
        makeListSets(setSpec, setName, buffer);
        makeVerbEnd(ListSets.name(), buffer);
        makeFooter(buffer);
        makeTimeUsed(ListSets.name(), startTime, buffer);

        return buffer.toString();
    }

    // OAI Helpers

    protected static void makeHeader(StringBuilder buffer, boolean withMarcxchange) {
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + "\n");
        buffer.append("<OAI-PMH  xmlns=\"http://www.openarchives.org/OAI/2.0/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        if (withMarcxchange) {
            buffer.append("xmlns:marc=\"info:lc/xmlns/marcxchange-v1\" ");
        }
        buffer.append("xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd");
        if (withMarcxchange) {
            buffer.append(" info:lc/xmlns/marcxchange-v1 http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd");
        }
        buffer.append("\">\n");
        buffer.append("    <responseDate>" + no.sikt.oai.TimeUtils.getResponseTime() + "</responseDate>" + "\n");
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
        buffer.append("    <request verb=\"" + verb + "\">").append(baseUrl).append("</request>\n");
    }

    protected static void makeRecord(boolean isDeleted, String identifier, Date lastUpdateDate, String xmlContent, String set, StringBuilder buffer) {
        buffer.append("        <record>\n");
        if (isDeleted) {
            buffer.append("            <header status=\"deleted\">\n");
        } else {
            buffer.append("            <header>\n");
        }
        buffer.append("                <identifier>").append(identifier).append("</identifier>\n");
        buffer.append("                <datestamp>").append(no.sikt.oai.temp.TimeUtils.Date2String(lastUpdateDate, no.sikt.oai.temp.TimeUtils.FORMAT_ZULU_LONG)).append("</datestamp>\n");
        if (set != null && set.length() > 0 && !set.equalsIgnoreCase("default")) {
            buffer.append("                <setSpec>").append(set).append("</setSpec>\n");
        }
        buffer.append("            </header>\n");
        buffer.append("            <metadata>\n");

        // Kun for å få riktig innrykk...
        String[] recordXml = xmlContent.split("\\r?\\n");
        for (String recordXmlPart : recordXml) {
            buffer.append("                " + recordXmlPart + "\n");
        }

        buffer.append("            </metadata>\n");
        buffer.append("        </record>\n");
    }

    protected static void makeRecordHeader(boolean isDeleted, String identifier, Date lastUpdateDate, String set, StringBuilder buffer) {
        if (isDeleted) {
            buffer.append("        <header status=\"deleted\">\n");
        } else {
            buffer.append("        <header>\n");
        }
        buffer.append("            <identifier>").append(identifier).append("</identifier>\n");
        buffer.append("            <datestamp>").append(no.sikt.oai.TimeUtils.Date2String(lastUpdateDate, no.sikt.oai.temp.TimeUtils.FORMAT_ZULU_LONG)).append("</datestamp>\n");
        if (set != null && set.length() > 0 && !set.equalsIgnoreCase("default")) {
            buffer.append("            <setSpec>").append(set).append("</setSpec>\n");
        }
        buffer.append("        </header>\n");
    }

    protected static void makeTimeUsed(String verb, long startTime, StringBuilder buffer) {
        long timeUsed = System.currentTimeMillis() - startTime;
        buffer.append("\n<!-- Time used " + verb + " " + timeUsed + " ms. -->");
    }


    // OAI helpers: GetRecord

    protected static void makeHeaderRequestGetRecord(String verb, String metadataPrefix, String identifier, String baseUrl, StringBuilder buffer) {
        buffer.append("    <request verb=\"" + verb + "\" identifier=\"" + identifier + "\" metadataPrefix=\"" + metadataPrefix + "\">" + baseUrl + "</request>\n");
    }


    // OAI helpers: ListRecords

    protected static void makeFooterListRecords(String listSize, String newToken, String cursor, StringBuilder buffer) {
        if (newToken != null && newToken.length() > 0) {
            buffer.append("        <resumptionToken completeListSize=\"" + listSize + "\"  cursor=\"" + cursor + "\">" + newToken + "</resumptionToken>\n");
        }
    }


    // OAI helpers: ListIdentifiers

    protected static void makeFooterListIdentifiers(String listSize, String newToken, StringBuilder buffer) {
        if (newToken != null && newToken.length() > 0) {
            buffer.append("        <resumptionToken completeListSize=\"").append(listSize).append("\">").append(newToken).append("</resumptionToken>\n");
        }
    }


    // OAI helpers: ListRecords & ListItentifiers

    protected static void makeHeaderRequestListRecordsIdentifiers(String verb, String oldResumptionToken, String from, String until, String metadataPrefix, String baseUrl, StringBuilder buffer) {

        boolean writeParams = true;

        buffer.append("    <request verb=\"").append(verb).append("\" ");
        if (oldResumptionToken != null) {
            writeParams = false;
            buffer.append(" resumptionToken=\"").append(oldResumptionToken).append("\" ");
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

    protected static void makeListSets(String setSpec, String setName, StringBuilder buffer) {
        buffer.append("        <set>\n");
        buffer.append("            <setSpec>").append(setSpec).append("</setSpec>\n");
        buffer.append("            <setName>").append(setName).append("</setName>\n");
        buffer.append("        </set>\n");
    }


    // OAI helpers: Identify

    protected static void makeIdentify(String repositoryName, String baseUrl, String protocolVersion, String adminEmail, String earliestTimestamp, String deletedRecord, String granularity, String description, StringBuilder buffer) {
        buffer.append("        <repositoryName>").append(repositoryName).append("</repositoryName>\n");
        buffer.append("        <baseURL>").append(baseUrl).append("</baseURL>\n");
        buffer.append("        <protocolVersion>").append(protocolVersion).append("</protocolVersion>\n");
        buffer.append("        <adminEmail>").append(adminEmail).append("</adminEmail>\n");
        buffer.append("        <earliestDatestamp>").append(earliestTimestamp).append("</earliestDatestamp>\n");
        buffer.append("        <deletedRecord>").append(deletedRecord).append("</deletedRecord>\n");
        buffer.append("        <granularity>").append(granularity).append("</granularity>\n");
        buffer.append("        <description>").append(description).append("</description>\n");
    }


    // OAI helpers: ListMetadataFormats


    protected static void makeListMetadataFormats(String metadataPrefix, String schema, String metadataNamespace, StringBuilder buffer) {
        buffer.append("        <metadataFormat>\n");
        buffer.append("            <metadataPrefix>)").append(metadataPrefix).append("</metadataPrefix>\n");
        buffer.append("            <schema>").append(schema).append("</schema>\n");
        buffer.append("            <metadataNamespace>").append(metadataNamespace).append("</metadataNamespace>\n");
        buffer.append("        </metadataFormat>\n");
    }

}

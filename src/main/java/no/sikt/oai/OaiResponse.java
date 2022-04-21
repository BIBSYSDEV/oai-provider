package no.sikt.oai;

import static no.sikt.oai.TimeUtils.FORMAT_ZULU_LONG;
import static no.sikt.oai.TimeUtils.date2String;
import static no.sikt.oai.Verb.GetRecord;
import static no.sikt.oai.Verb.Identify;
import static no.sikt.oai.Verb.ListIdentifiers;
import static no.sikt.oai.Verb.ListMetadataFormats;
import static no.sikt.oai.Verb.ListRecords;
import static no.sikt.oai.Verb.ListSets;
import java.util.List;
import no.sikt.oai.adapter.Adapter;
import no.sikt.oai.adapter.Adapter.OaiSet;
import no.sikt.oai.data.Record;
import no.sikt.oai.data.RecordsList;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals"})
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

    public static String listMetadataFormats(String baseUrl, long startTime) {
        StringBuilder buffer = new StringBuilder();
        makeHeader(buffer);
        makeHeaderRequest(ListMetadataFormats.name(), baseUrl, buffer);
        makeVerbStart(ListMetadataFormats.name(), buffer);
        makeListMetadataFormats(buffer);
        makeVerbEnd(ListMetadataFormats.name(), buffer);
        makeFooter(buffer);
        makeTimeUsed(ListMetadataFormats.name(), startTime, buffer);
        return buffer.toString();
    }

    public static String getRecord(Record record, String identifier, String metadataPrefix, String baseUrl,
                                   long startTime) {
        StringBuilder buffer = new StringBuilder(1000);
        makeHeader(buffer);
        makeHeaderRequestGetRecord(GetRecord.name(), metadataPrefix, identifier, baseUrl, buffer);
        makeVerbStart(GetRecord.name(), buffer);
        makeRecord(record, buffer, true);
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
            makeRecord(record, buffer, false);
        }

        long recordsRemaining = records.getNumFound() - (startPosition + records.size());

        String newResumptionToken = createNewResumptionToken(from, until, resumptionToken, metadataPrefix,
                startPosition, setSpec, records, recordsRemaining);
        makeFooterListIdentifiers(records.getNumFound(), newResumptionToken, buffer);
        makeVerbEnd(ListIdentifiers.name(), buffer);
        makeFooter(buffer);
        makeTimeUsed(ListIdentifiers.name(), startTime, buffer);
        return buffer.toString();
    }

    public static String listRecords(String from, String until, String resumptionToken, String metadataPrefix,
                                     String baseUrl, int startPosition, String setSpec, RecordsList records,
                                     long startTime) {
        StringBuilder buffer = new StringBuilder();
        makeHeader(buffer);
        makeHeaderRequestListRecordsIdentifiers(ListRecords.name(), resumptionToken, from, until, metadataPrefix,
                                                baseUrl, buffer);
        makeVerbStart(ListRecords.name(), buffer);

        for (Record record : records) {
            makeRecord(record, buffer, true);
        }

        long recordsRemaining = records.getNumFound() - (startPosition + records.size());

        String newResumptionToken = createNewResumptionToken(from, until, resumptionToken, metadataPrefix,
                startPosition, setSpec, records, recordsRemaining);

        makeFooterListRecords(records.getNumFound(), newResumptionToken, startPosition + records.size(), buffer);
        makeVerbEnd(ListRecords.name(), buffer);
        makeFooter(buffer);
        makeTimeUsed(ListRecords.name(), startTime, buffer);
        return buffer.toString();
    }

    private static String createNewResumptionToken(String from, String until, String resumptionToken,
                                                   String metadataPrefix, int startPosition, String setSpec,
                                                   RecordsList records, long recordsRemaining) {
        if (recordsRemaining > 50) {
            ResumptionToken newToken;
            if (resumptionToken.length() > 0) {
                newToken = new ResumptionToken(resumptionToken);
                newToken.timestamp = System.currentTimeMillis();
                newToken.startPosition = Integer.toString(startPosition + records.size());
            } else {
                newToken = new ResumptionToken("lr", System.currentTimeMillis(), setSpec,
                        from == null ? "" : from, until == null ? "" : until,
                        metadataPrefix,
                        Integer.toString(startPosition + records.size()));
            }
            return newToken.asString();
        }
        return "";
    }

    public static String listSets(String baseUrl, List<OaiSet> setList, long startTime) {
        StringBuilder buffer = new StringBuilder(1000);
        makeHeader(buffer);
        makeHeaderRequest(ListSets.name(), baseUrl, buffer);
        makeVerbStart(ListSets.name(), buffer);
        for (OaiSet set : setList) {
            makeListSets(set.setSpec, set.setName, buffer);
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

    @SuppressWarnings({"PMD.ConsecutiveLiteralAppends"})
    protected static void makeHeader(StringBuilder buffer) {
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + "\n")
            .append("<OAI-PMH  xmlns=\"http://www.openarchives.org/OAI/2.0/\" ")
            .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
            .append("xsi:schemaLocation=")
            .append("\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd")
            .append("\">\n")
            .append("    <responseDate>").append(TimeUtils.getResponseTime()).append("</responseDate>")
            .append('\n');
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

    @SuppressWarnings({"PMD.ConsecutiveLiteralAppends"})
    protected static void makeRecord(Record record, StringBuilder buffer, boolean showMetadata) {
        buffer.append("        <record>\n");
        if (record.isDeleted()) {
            buffer.append("            <header status=\"deleted\">\n");
        } else {
            buffer.append("            <header>\n");
        }
        buffer.append("                <identifier>").append(record.getIdentifier()).append("</identifier>\n")
            .append("                <datestamp>").append(date2String(record.getLastUpdateDate(), FORMAT_ZULU_LONG))
            .append("</datestamp>\n");
        for (String setSpec : record.getSetSpecs()) {
            buffer.append("                <setSpec>").append(setSpec).append("</setSpec>\n");
        }
        buffer.append("            </header>\n");
        if (!record.isDeleted() && showMetadata) {
            buffer.append("            <metadata>\n");

            // Kun for å få riktig innrykk...
            String[] recordXml = record.getContent().split("\\r?\\n");
            for (String recordXmlPart : recordXml) {
                buffer.append("                ").append(recordXmlPart).append('\n');
            }
            buffer.append("            </metadata>\n");
        }
        buffer.append("        </record>\n");
    }

    protected static void makeTimeUsed(String verb, long startTime, StringBuilder buffer) {
        long timeUsed = System.currentTimeMillis() - startTime;
        buffer.append("\n<!-- Time used ").append(verb).append(' ').append(timeUsed).append(" ms. -->");
    }

    // OAI helpers: GetRecord

    protected static void makeHeaderRequestGetRecord(String verb, String metadataPrefix, String identifier,
                                                     String baseUrl, StringBuilder buffer) {
        buffer.append("    <request verb=\"").append(verb).append("\" identifier=\"").append(identifier)
            .append("\" metadataPrefix=\"").append(metadataPrefix).append("\">").append(baseUrl)
            .append("</request>\n");
    }

    // OAI helpers: ListRecords

    protected static void makeFooterListRecords(long listSize, String newToken, int cursor, StringBuilder buffer) {
        if (newToken.length() > 0) {
            buffer.append("        <resumptionToken completeListSize=\"").append(listSize).append("\"  cursor=\"")
                .append(cursor).append("\">").append(newToken).append("</resumptionToken>\n");
        }
    }

    // OAI helpers: ListIdentifiers

    protected static void makeFooterListIdentifiers(long listSize, String newToken, StringBuilder buffer) {
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
        if (from.length() > 9 && writeParams) {
            buffer.append(" from=\"").append(from).append('"');
        }
        if (until.length() > 9 && writeParams) {
            buffer.append(" until=\"").append(until).append('"');
        }
        if (writeParams) {
            buffer.append(" metadataPrefix=\"").append(metadataPrefix).append('"');
        }
        buffer.append('>').append(baseUrl).append("</request>\n");
    }

    // OAI helpers: ListSets

    @SuppressWarnings({"PMD.ConsecutiveLiteralAppends"})
    protected static void makeListSets(String setSpec, String setName, StringBuilder buffer) {
        buffer.append("        <set>\n")
            .append("            <setSpec>").append(setSpec).append("</setSpec>\n")
            .append("            <setName>").append(setName).append("</setName>\n")
            .append("        </set>\n");
    }

    // OAI helpers: Identify

    @SuppressWarnings({"PMD.ConsecutiveLiteralAppends"})
    protected static void makeIdentify(Adapter adapter, StringBuilder buffer) {
        buffer.append("        <repositoryName>").append(adapter.getRepositoryName()).append("</repositoryName>\n")
            .append("        <baseURL>").append(adapter.getBaseUrl()).append("</baseURL>\n")
            .append("        <protocolVersion>").append(adapter.getProtocolVersion()).append("</protocolVersion>\n")
            .append("        <adminEmail>").append(adapter.getAdminEmail()).append("</adminEmail>\n")
            .append("        <earliestDatestamp>").append(adapter.getEarliestTimestamp())
            .append("</earliestDatestamp>\n")
            .append("        <deletedRecord>").append(adapter.getDeletedRecord()).append("</deletedRecord>\n")
            .append("        <granularity>").append(adapter.getDateGranularity()).append("</granularity>\n")
            .append("        <description>").append(adapter.getDescription()).append("</description>\n");
    }

    // OAI helpers: ListMetadataFormats

    @SuppressWarnings({"PMD.ConsecutiveLiteralAppends"})
    protected static void makeListMetadataFormats(StringBuilder buffer) {
        buffer.append("        <metadataFormat>\n")
            .append("            <metadataPrefix>").append("qdc").append("</metadataPrefix>\n")
            .append("            <schema>").append("http://dublincore.org/schemas/xmls/qdc/2006/01/06/dcterms.xsd").append("</schema>\n")
            .append("            <metadataNamespace>").append("http://purl.org/dc/terms/").append("</metadataNamespace>\n")
            .append("        </metadataFormat>\n")
            .append("        <metadataFormat>\n")
            .append("            <metadataPrefix>").append("oai_dc").append("</metadataPrefix>\n")
            .append("            <schema>").append("http://www.openarchives.org/OAI/2.0/oai_dc.xsd").append("</schema>\n")
            .append("            <metadataNamespace>").append("http://www.openarchives.org/OAI/2.0/oai_dc/").append("</metadataNamespace>\n")
            .append("        </metadataFormat>\n")
            .append("        <metadataFormat>\n")
            .append("            <metadataPrefix>").append("oai_datacite").append("</metadataPrefix>\n")
            .append("            <schema>").append("http://namespace.openaire.eu/schema/oaire/ https://www.openaire.eu/schema/repo-lit/4.0/openaire.xsd").append("</schema>\n")
            .append("            <metadataNamespace>").append("http://namespace.openaire.eu/schema/oaire/ https://www.openaire.eu/schema/repo-lit/4.0/openaire.xsd").append("</metadataNamespace>\n")
            .append("        </metadataFormat>\n");
    }
}

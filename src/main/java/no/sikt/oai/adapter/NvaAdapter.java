package no.sikt.oai.adapter;

import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static no.sikt.oai.OaiConstants.OAI_DATACITE_HEADER;
import static no.sikt.oai.OaiConstants.OAI_DC_HEADER;
import static no.sikt.oai.OaiConstants.QDC_HEADER;
import static no.sikt.oai.OaiProviderHandler.EMPTY_STRING;
import static no.sikt.oai.adapter.DlrAdapter.ALL_SET_NAME;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.sikt.oai.MetadataFormat;
import no.sikt.oai.OaiConstants;
import no.sikt.oai.TimeUtils;
import no.sikt.oai.data.Record;
import no.sikt.oai.data.RecordsList;
import no.sikt.oai.exception.InternalOaiException;
import no.unit.nva.model.Publication;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class NvaAdapter implements Adapter {

    private final transient ObjectMapper mapper = new ObjectMapper();
    private final transient String resourceUri;
    private final transient String resourcesUri;
    private final transient String setsUri; // "https://api.dev.nva.aws.unit.no/customer/";

    public NvaAdapter(Environment environment) {
        setsUri = environment.readEnv(OaiConstants.SETS_URI_ENV);
        resourceUri = environment.readEnv(OaiConstants.RECORD_URI_ENV);
        resourcesUri = environment.readEnv(OaiConstants.RECORDS_URI_ENV);
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public boolean isValidIdentifier(String identifier) {
        return identifier.length() == 36;
    }

    @Override
    public String getDescription() {
        return "Repository for NVA resources";
    }

    @Override
    public String getDateGranularity() {
        return "YYYY-MM-DD";
    }

    @Override
    public String getEarliestTimestamp() {
        return "2020-01-31T00:00:01Z";
    }

    @Override
    public String getDeletedRecord() {
        return "yes";
    }

    @Override
    public String getProtocolVersion() {
        return "2.0";
    }

    @Override
    public String getAdminEmail() {
        return "support@unit.no";
    }

    @Override
    public String getRepositoryName() {
        return "NVA Repository";
    }

    @Override
    public String getBaseUrl() {
        return "https://nva.unit.no";
    }

    @Override
    public String getIdentifierPrefix() {
        return "oai:nva.unit.no:";
    }

    @Override
    public URI getSetsUri() {
        return UriWrapper
            .fromUri(setsUri)
            .getUri();
    }

    @Override
    public URI getRecordUri(String identifier) {
        return UriWrapper
            .fromUri(resourceUri)
            .addChild(identifier)
            .getUri();
    }

    @Override
    public URI getRecordsListUri(String from, String until, String institution, int startPosition) {
        return UriWrapper
            .fromUri(resourcesUri)
            .getUri();
    }

    @Override
    public List<OaiSet> parseSetsResponse(String json) throws InternalOaiException {
        try {
            List<Customer> customerList = mapper.readValue(json, Customers.class).customerList;
            return customerList.stream()
                    .map(customer -> new OaiSet(customer.displayName, customer.id))
                    .collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            throw new InternalOaiException(e, HTTP_UNAVAILABLE);
        }
    }

    @Override
    public Record parseRecordResponse(String json, String metadataPrefix, String setSpec) throws InternalOaiException {
        try {
            return createRecordFromPublication(mapper.readValue(json, Publication.class), metadataPrefix);
        }  catch (JsonProcessingException e) {
            throw new InternalOaiException(e, HTTP_UNAVAILABLE);
        }
    }

    @Override
    public RecordsList parseRecordsListResponse(String verb, String json, String metadataPrefix, String setSpec) {
        Record record = new Record("", false, "1234", new Date(), Collections.emptyList());
        RecordsList records = new RecordsList(1);
        records.add(record);
        return records;
    }

    private Record createRecordFromPublication(Publication publication, String metadataPrefix) {
        List<String> setSpecs = new ArrayList<>();
        setSpecs.add(ALL_SET_NAME);
        setSpecs.add(publication.getPublisher().getId().toString());
        return new Record(
                createRecordContent(publication, metadataPrefix),
                false,
                getIdentifierPrefix() + publication.getIdentifier(),
                Date.from(publication.getModifiedDate()),
                setSpecs);
    }

    private String createRecordContent(Publication publication, String metadataPrefix) {
        if (metadataPrefix.equalsIgnoreCase(MetadataFormat.QDC.name())) {
            return createRecordContentQdc(publication);
        } else if (metadataPrefix.equalsIgnoreCase(MetadataFormat.OAI_DATACITE.name())) {
            return createRecordContentOaiDatacite(publication);
        } else if (metadataPrefix.equalsIgnoreCase(MetadataFormat.OAI_DC.name())) {
            return createRecordContentOaiDc(publication);
        }
        return EMPTY_STRING;
    }

    @SuppressWarnings({"PMD.ConsecutiveLiteralAppends", "PMD.InsufficientStringBufferDeclaration"})
    private String createRecordContentOaiDc(Publication publication) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(OAI_DC_HEADER)
            .append("    <dc:title>").append(publication.getEntityDescription().getMainTitle())
            .append("</dc:title>\n")
            .append("    <dc:description>")
            .append(publication.getEntityDescription().getDescription())
            .append("</dc:description>\n")
            .append("    <dc:rights>").append(EMPTY_STRING)
            .append("</dc:rights>\n")
            .append("    <dc:type>").append(EMPTY_STRING).append("</dc:type>\n")
            .append("    <dc:publisher>").append(publication.getPublisher().getId())
            .append("</dc:publisher>\n")
            .append("    <dc:date>").append(TimeUtils.date2String(Date.from(publication.getCreatedDate()),
                    TimeUtils.FORMAT_ZULU_SHORT)).append("</dc:date>\n")
            .append("    <dc:date>").append(TimeUtils.date2String(Date.from(publication.getPublishedDate()),
                    TimeUtils.FORMAT_ZULU_SHORT)).append("</dc:date>\n")
            .append("    <dc:date>").append(TimeUtils.date2String(Date.from(publication.getModifiedDate()),
                    TimeUtils.FORMAT_ZULU_SHORT)).append("</dc:date>\n")
            .append("    <dc:identifier>").append(publication.getIdentifier().toString())
            .append("</dc:identifier>\n");
        appendCreatorsDc(publication, buffer);
        buffer.append("</oai_dc:dc>\n");
        return buffer.toString();
    }

    @SuppressWarnings({"PMD.ConsecutiveLiteralAppends", "PMD.InsufficientStringBufferDeclaration"})
    private String createRecordContentQdc(Publication publication) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(QDC_HEADER)
            .append("    <dc:title>").append(publication.getEntityDescription().getMainTitle())
            .append("</dc:title>\n")
            .append("    <dc:description>")
            .append(publication.getEntityDescription().getDescription())
            .append("</dc:description>\n")
            .append("    <dc:rights>").append(EMPTY_STRING)
            .append("</dc:rights>\n")
            .append("    <dcterms:accessRights>").append(EMPTY_STRING)
            .append("</dcterms:accessRights>\n")
            .append("    <dc:publisher>").append(publication.getPublisher().getId())
            .append("</dc:publisher>\n")
            .append("    <dc:type>").append(EMPTY_STRING).append("</dc:type>\n")
            .append("    <dcterms:created>").append(TimeUtils.date2String(Date.from(publication.getCreatedDate()),
                    TimeUtils.FORMAT_ZULU_SHORT))
            .append("</dcterms:created>\n")
            .append("    <dcterms:modified>").append(TimeUtils.date2String(Date.from(publication.getModifiedDate()),
                    TimeUtils.FORMAT_ZULU_SHORT))
            .append("</dcterms:modified>\n")
            .append("    <dcterms:issued>").append(TimeUtils.date2String(Date.from(publication.getPublishedDate()),
                    TimeUtils.FORMAT_ZULU_SHORT))
            .append("</dcterms:issued>\n")
            .append("    <dcterms:identifier xsi:type=\"dcterms:URI\">")
            .append(publication.getIdentifier().toString()).append("</dcterms:identifier>\n")
            .append("    <dcterms:identifier>").append(publication.getIdentifier().toString())
            .append("</dcterms:identifier>\n");
        appendCreatorsDc(publication, buffer);
        appendContributorsDc(publication, buffer);
        buffer.append("</qdc:qualifieddc>\n");
        return buffer.toString();
    }

    @SuppressWarnings({"PMD.ConsecutiveLiteralAppends", "PMD.InsufficientStringBufferDeclaration"})
    private String createRecordContentOaiDatacite(Publication publication) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(OAI_DATACITE_HEADER)
            .append("    <datacite:titles>\n")
            .append("        <datacite:title>").append(publication.getEntityDescription().getMainTitle())
            .append("</datacite:title>\n")
            .append("    </datacite:titles>\n")
            .append("    <dc:description>")
            .append(publication.getEntityDescription().getDescription())
            .append("</dc:description>\n")
            .append("    <dc:publisher>").append(publication.getPublisher().getId())
            .append("</dc:publisher>\n")
            .append("    <datacite:dates>\n")
            .append("        <datacite:date dateType=\"Issued\">")
            .append(TimeUtils.date2String(Date.from(publication.getPublishedDate()), TimeUtils.FORMAT_ZULU_SHORT))
            .append("</datacite:date>\n")
            .append("    </datacite:dates>\n");
        appendCreatorsDatacite(publication, buffer);
        buffer.append("</oaire:resource>\n");
        return buffer.toString();
    }

    private void appendCreatorsDc(Publication publication, StringBuilder buffer) {
        publication.getEntityDescription().getContributors().stream()
                .filter(contributor -> "creator".equalsIgnoreCase(contributor.getRole().name()))
                .map(contributor -> contributor.getIdentity().getId().toString())
                .forEach(contributorName -> {
                    buffer.append("    <dc:creator>").append(contributorName).append("</dc:creator>\n");
                });
    }

    @SuppressWarnings("PMD.ConsecutiveLiteralAppends")
    private void appendCreatorsDatacite(Publication publication, StringBuilder buffer) {
        publication.getEntityDescription().getContributors().stream()
                .filter(contributor -> "creator".equalsIgnoreCase(contributor.getRole().name()))
                .map(contributor -> contributor.getIdentity().getId().toString())
                .forEach(contributorName -> {
                    buffer.append("    <datacite:creators>\n")
                        .append("        <datacite:creator>\n")
                        .append("            <datacite:creatorName>")
                        .append(contributorName)
                        .append("</datacite:creatorName>\n")
                        .append("        </datacite:creator>\n")
                        .append("    </datacite:creators>\n");
                });
    }

    private void appendContributorsDc(Publication publication, StringBuilder buffer) {
        publication.getEntityDescription().getContributors().stream()
                .filter(contributor -> !"creator".equalsIgnoreCase(contributor.getRole().name()))
                .map(contributor -> contributor.getIdentity().getId().toString())
                .forEach(contributorName -> {
                    buffer.append("    <dc:contributor>").append(contributorName).append("</dc:contributor>\n");
                });
    }

    private static class Customers {

        @JsonProperty("@context")
        /* default */ transient String context;
        @JsonProperty("customers")
        /* default */ transient List<Customer> customerList;
        @JsonProperty("id")
        /* default */ transient String id;
    }

    private static class Customer {

        @JsonProperty("createdDate")
        /* default */ transient String createdDate;
        @JsonProperty("displayName")
        /* default */ transient String displayName;
        @JsonProperty("id")
        /* default */ transient String id;
    }
}

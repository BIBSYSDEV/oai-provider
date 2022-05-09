package no.sikt.oai.adapter;

import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static no.sikt.oai.OaiConstants.ID_DOES_NOT_EXIST;
import static no.sikt.oai.OaiConstants.NO_RECORDS_MATCH;
import static no.sikt.oai.OaiConstants.NO_SETS_FOUND;
import static no.sikt.oai.OaiConstants.NO_SET_HIERARCHY;
import static no.sikt.oai.OaiConstants.OAI_DATACITE_HEADER;
import static no.sikt.oai.OaiConstants.OAI_DC_HEADER;
import static no.sikt.oai.OaiConstants.QDC_HEADER;
import static no.sikt.oai.OaiConstants.UNKNOWN_IDENTIFIER;
import static no.sikt.oai.OaiProviderHandler.EMPTY_STRING;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import no.sikt.oai.MetadataFormat;
import no.sikt.oai.OaiConstants;
import no.sikt.oai.TimeUtils;
import no.sikt.oai.data.Record;
import no.sikt.oai.data.RecordsList;
import no.sikt.oai.exception.InternalOaiException;
import no.sikt.oai.exception.OaiException;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.file.model.File;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.PublicationInstance;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;
import org.apache.http.HttpStatus;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.GodClass"})
public class NvaAdapter implements Adapter {

    private static final String SLASH = "/";
    private static final String BLANK = " ";
    private static final String DASH = "-";
    public static final String ACCESS_RIGHT_OPEN = "open";
    public static final String ERROR_UNEXPECTED_RESPONSE_FROM_DATA_SOURCE = "Unexpected response from data source";
    private final transient ObjectMapper mapper = new ObjectMapper();
    private final transient String resourceUri;
    private final transient String resourcesUri;
    private final transient String setsUri;
    private final transient AuthorizedBackendClient client;

    @JacocoGenerated
    public NvaAdapter(Environment environment) {
        setsUri = environment.readEnv(OaiConstants.SETS_URI_ENV);
        resourceUri = environment.readEnv(OaiConstants.RECORD_URI_ENV);
        resourcesUri = environment.readEnv(OaiConstants.RECORDS_URI_ENV);
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.client = AuthorizedBackendClient.prepareWithBackendCredentials();
    }

    public NvaAdapter(Environment environment, AuthorizedBackendClient client) {
        setsUri = environment.readEnv(OaiConstants.SETS_URI_ENV);
        resourceUri = environment.readEnv(OaiConstants.RECORD_URI_ENV);
        resourcesUri = environment.readEnv(OaiConstants.RECORDS_URI_ENV);
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.client = client;
    }

    @Override
    public boolean isValidIdentifier(String identifier) {
        return identifier.length() >= 36;
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
        return "2020-06-18T13:08:48.472596Z";
    }

    @Override
    public String getDeletedRecord() {
        return "no";
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

    private URI getSetsUri() {
        return UriWrapper
            .fromUri(setsUri)
            .getUri();
    }

    private URI getRecordUri(String identifier) {
        return UriWrapper
            .fromUri(resourceUri)
            .addChild(identifier)
            .getUri();
    }

    private URI getRecordsListUri(String from, String until, String setSpec, int startPosition) {
        UriWrapper uriWrapper = UriWrapper.fromUri(resourcesUri);
        StringBuilder query = new StringBuilder();
        if (StringUtils.isNotEmpty(setSpec)) {
            if (!ALL_SET_NAME.equalsIgnoreCase(setSpec)) {
                query.append("publisher = ").append(setSpec)
                    .append(" & ");
            }
            query.append("modifiedDate > ").append(from)
                .append(" & modifiedDate < ").append(until);
        }
        uriWrapper = uriWrapper.addQueryParameter("query", query.toString());
        if (startPosition != 0) {
            uriWrapper = uriWrapper.addQueryParameter("from", String.valueOf(startPosition));
        }
        uriWrapper = uriWrapper.addQueryParameter("results", "50");
        return uriWrapper.getUri();
    }

    @Override
    public String getSetsList() throws OaiException, InternalOaiException {
        HttpResponse<String> response;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(getSetsUri())
                .header(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                .GET();
            response = client.send(builder, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new InternalOaiException(e, HTTP_UNAVAILABLE);
        }
        if (!responseIsSuccessful(response)) {
            throw new OaiException(NO_SET_HIERARCHY, NO_SETS_FOUND);
        }
        return response.body();
    }

    @Override
    public String getRecord(String identifier) throws OaiException, InternalOaiException {
        HttpResponse<String> response;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(getRecordUri(identifier))
                .header(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                .GET();
            response = client.send(builder, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new InternalOaiException(e, HTTP_UNAVAILABLE);
        }
        if (!responseIsSuccessful(response)) {
            throw new OaiException(ID_DOES_NOT_EXIST, UNKNOWN_IDENTIFIER);
        }
        return response.body();
    }

    @Override
    public String getRecordsList(String from, String until, String setSpec, int startPosition)
        throws OaiException, InternalOaiException {
        HttpResponse<String> response;
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(getRecordsListUri(from, until, setSpec, startPosition))
                .header(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                .GET();
            response = client.send(builder, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new InternalOaiException(e, HTTP_UNAVAILABLE);
        }
        if (!responseIsSuccessful(response)) {
            throw new OaiException(NO_RECORDS_MATCH, OaiConstants.COMBINATION_OF_PARAMS_ERROR);
        }
        return response.body();
    }

    @JacocoGenerated
    protected boolean responseIsSuccessful(HttpResponse<String> response) {
        int status = response.statusCode();
        // status should be in the range [200,300)
        return status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES;
    }

    @Override
    public List<OaiSet> parseSetsResponse(String json) throws InternalOaiException {
        try {
            List<Customer> customerList = mapper.readValue(json, Customers.class).customerList;
            List<OaiSet> oaiSetList = new ArrayList<>();
            oaiSetList.add(new OaiSet(ALL_SET_NAME, ALL_SET_NAME));
            oaiSetList.addAll(customerList.stream()
                .map(customer -> new OaiSet(customer.displayName, extractIdentifier(customer.id)))
                .collect(Collectors.toList()));
            return oaiSetList;
        } catch (JsonProcessingException e) {
            throw new InternalOaiException(e, HTTP_UNAVAILABLE);
        }
    }

    private String extractIdentifier(String customerUri) {
        if (customerUri.contains(SLASH)) {
            int lastIndexOfSlash = customerUri.lastIndexOf(SLASH);
            return customerUri.substring(lastIndexOfSlash + 1);
        }
        return customerUri;
    }

    @Override
    public Record parseRecordResponse(String json, String metadataPrefix, String setSpec) throws InternalOaiException {
        try {
            return createRecordFromPublication(mapper.readValue(json, Publication.class), metadataPrefix);
        } catch (JsonProcessingException e) {
            throw new InternalOaiException(e, HTTP_UNAVAILABLE);
        }
    }

    @Override
    public RecordsList parseRecordsListResponse(String verb, String json, String metadataPrefix, String setSpec)
        throws InternalOaiException {
        try {
            PublicationSearchResponse publicationSearchResponse =
                mapper.readValue(json, PublicationSearchResponse.class);
            RecordsList records = new RecordsList(publicationSearchResponse.total);
            for (Publication publication : publicationSearchResponse.hits) {
                records.add(createRecordFromPublication(publication, metadataPrefix));
            }
            return records;
        } catch (JsonProcessingException e) {
            throw new InternalOaiException(e, ERROR_UNEXPECTED_RESPONSE_FROM_DATA_SOURCE, HTTP_UNAVAILABLE);
        }
    }

    private Record createRecordFromPublication(Publication publication, String metadataPrefix) {
        List<String> setSpecs = new ArrayList<>();
        setSpecs.add(ALL_SET_NAME);
        setSpecs.add(UriWrapper.fromUri(publication.getPublisher().getId()).getLastPathElement());
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
            .append("</dc:title>\n");
        Optional.ofNullable(publication.getEntityDescription().getDescription()).ifPresent(description -> {
            buffer.append("    <dc:description>")
                    .append(description)
                    .append("</dc:description>\n");
        });
        Optional.ofNullable(publication.getEntityDescription().getAbstract()).ifPresent(abstractTag -> {
            buffer.append("    <dc:description>")
                .append(abstractTag)
                .append("</dc:description>\n");
        });
        buffer.append("    <dc:rights>").append(getLicenseAsText(publication))
            .append("</dc:rights>\n")
            .append("    <dc:rights>").append(getLicenseAsUri(publication))
            .append("</dc:rights>\n")
            .append("    <dc:type>").append(publication.getEntityDescription().getReference().getPublicationInstance()
                                                .getInstanceType()).append("</dc:type>\n");
        if (publication.getEntityDescription().getReference().getPublicationInstance().isPeerReviewed()) {
            buffer.append("    <dc:type>Peer reviewed</dc:type>\n");
        }
        buffer.append("    <dc:publisher>").append(publication.getPublisher().getId())
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
            .append("</dc:title>\n");
        Optional.ofNullable(publication.getEntityDescription().getDescription()).ifPresent(description -> {
            buffer.append("    <dc:description>")
                .append(description)
                .append("</dc:description>\n");
        });
        Optional.ofNullable(publication.getEntityDescription().getAbstract()).ifPresent(abstractTag -> {
            buffer.append("    <dcterms:abstract>")
                .append(abstractTag)
                .append("</dcterms:abstract>\n");
        });
        buffer.append(extractLanguageDcTag(publication))
            .append("    <dc:rights>").append(getLicenseAsText(publication))
            .append("</dc:rights>\n")
            .append("    <dc:rights xsi:type=\"dcterms:URI\">").append(getLicenseAsUri(publication))
            .append("</dc:rights>\n")
            .append("    <dcterms:accessRights>").append(ACCESS_RIGHT_OPEN)
            .append("</dcterms:accessRights>\n")
            .append("    <dc:publisher>").append(publication.getPublisher().getId())
            .append("</dc:publisher>\n")
            .append("    <dc:type>").append(publication.getEntityDescription().getReference().getPublicationInstance()
                                                .getInstanceType()).append("</dc:type>\n");
        if (publication.getEntityDescription().getReference().getPublicationInstance().isPeerReviewed()) {
            buffer.append("    <dc:type>Peer reviewed</dc:type>\n");
        }
        buffer.append("    <dcterms:created>").append(TimeUtils.date2String(Date.from(publication.getCreatedDate()),
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
            .append("        <datacite:title>")
            .append(publication.getEntityDescription().getMainTitle())
            .append("</datacite:title>\n")
            .append("    </datacite:titles>\n");
        extractDescriptionOaiDatacite(publication, buffer);
        buffer.append(extractLanguageDcTag(publication))
            .append("    <dc:publisher>").append(publication.getPublisher().getId())
            .append("</dc:publisher>\n")
            .append("    <datacite:dates>\n")
            .append("        <datacite:date dateType=\"Issued\">")
            .append(TimeUtils.date2String(Date.from(publication.getPublishedDate()), TimeUtils.FORMAT_ZULU_SHORT))
            .append("</datacite:date>\n")
            .append("    </datacite:dates>\n");
        appendCreatorsDatacite(publication, buffer);
        appendResourceTypeDatacite(publication, buffer);
        buffer.append("</oaire:resource>\n");
        return buffer.toString();
    }

    private void extractDescriptionOaiDatacite(Publication publication, StringBuilder buffer) {
        if (Optional.ofNullable(publication.getEntityDescription().getDescription()).isPresent()
                || Optional.ofNullable(publication.getEntityDescription().getAbstract()).isPresent()) {
            buffer.append("    <datacite:descriptions>\n ");
            Optional.ofNullable(publication.getEntityDescription().getDescription()).ifPresent(description -> {
                buffer.append("    <datacite:description descriptionType=\"Other\">")
                    .append(description)
                    .append("</datacite:description>\n");
            });
            Optional.ofNullable(publication.getEntityDescription().getAbstract()).ifPresent(abstractTag -> {
                buffer.append("    <datacite:description descriptionType=\"Abstract\">")
                    .append(abstractTag)
                    .append("</datacite:description>\n");
            });
            buffer.append("</datacite:descriptions>\n");
        }
    }

    @SuppressWarnings({"PMD.InsufficientStringBufferDeclaration"})
    private String extractLanguageDcTag(Publication publication) {
        StringBuilder str = new StringBuilder();
        if (publication.getEntityDescription().getLanguage() != null) {
            UriWrapper languageUri = UriWrapper.fromUri(publication.getEntityDescription().getLanguage());
            String isoCodePathlet = languageUri.getParent().get().getLastPathElement();
            str.append("    <dc:language xsi:type=\"dcterms:").append(isoCodePathlet).append("\">")
                .append(languageUri.getLastPathElement())
                .append("</dc:language>\n");
        }
        return str.toString();
    }

    private String getLicenseAsText(Publication publication) {
        File file = publication.getFileSet()
            .getFiles()
            .stream()
            .findFirst()
            .get();
        return file.getLicense().getIdentifier();
    }

    private String getLicenseAsUri(Publication publication) {
        String licenseAsText = getLicenseAsText(publication);
        String licensePathlet = licenseAsText.toLowerCase(Locale.getDefault());
        licensePathlet = licensePathlet.replaceAll(BLANK, DASH);
        return "http://creativecommons.org/licenses/" + licensePathlet + "/4.0/deed.no";
    }

    private void appendResourceTypeDatacite(Publication publication, StringBuilder buffer) {
        String instanceType = Optional.of(publication).map(Publication::getEntityDescription)
            .map(EntityDescription::getReference)
            .map(Reference::getPublicationInstance)
            .map(PublicationInstance::getInstanceType).orElse(EMPTY_STRING);
        buffer.append("    <datacite:resourceType resourceTypeGeneral=\"")
            .append(instanceType.isEmpty() ? "Other" : "Text")
            .append("\">")
            .append(instanceType)
            .append("</datacite:resourceType>\n");
    }

    private void appendCreatorsDc(Publication publication, StringBuilder buffer) {
        publication.getEntityDescription().getContributors().stream()
            .filter(contributor -> "creator".equalsIgnoreCase(contributor.getRole().name()))
            .map(contributor -> contributor.getIdentity().getName())
            .forEach(contributorName -> {
                buffer.append("    <dc:creator>").append(contributorName).append("</dc:creator>\n");
            });
    }

    @SuppressWarnings("PMD.ConsecutiveLiteralAppends")
    private void appendCreatorsDatacite(Publication publication, StringBuilder buffer) {
        buffer.append("    <datacite:creators>\n");
        publication.getEntityDescription().getContributors().stream()
            .filter(contributor -> "creator".equalsIgnoreCase(contributor.getRole().name()))
            .forEach(contributor -> {
                extractCreator(buffer, contributor);
            });
        buffer.append("    </datacite:creators>\n");
    }

    private void extractCreator(StringBuilder buffer, Contributor contributor) {
        buffer.append("        <datacite:creator>\n            <datacite:creatorName>")
            .append(contributor.getIdentity().getName())
            .append("</datacite:creatorName>\n");
        if (contributor.getIdentity().getOrcId() != null) {
            buffer.append("<datacite:nameIdentifier nameIdentifierScheme=\"ORCID\" schemeURI=\"https://orcid.org\">")
                .append(UriWrapper.fromUri(contributor.getIdentity().getOrcId()).getLastPathElement())
                .append("</datacite:nameIdentifier>");
        }
        buffer.append("        </datacite:creator>\n");
    }

    private void appendContributorsDc(Publication publication, StringBuilder buffer) {
        publication.getEntityDescription().getContributors().stream()
            .filter(contributor -> !"creator".equalsIgnoreCase(contributor.getRole().name()))
            .map(contributor -> contributor.getIdentity().getName())
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

    private static class PublicationSearchResponse {

        @JsonProperty("@context")
        /* default */ transient String context;
        @JsonProperty("hits")
        /* default */ transient List<Publication> hits;
        @JsonProperty("size")
        /* default */ transient int size;
        @JsonProperty("total")
        /* default */ transient int total;
    }
}

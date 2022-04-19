package no.sikt.oai.adapter;

import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static no.sikt.oai.OaiConstants.RECORDS_URI_ENV;
import static no.sikt.oai.OaiConstants.RECORD_URI_ENV;
import static no.sikt.oai.OaiConstants.SETS_URI_ENV;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.sikt.oai.MetadataFormat;
import no.sikt.oai.TimeUtils;
import no.sikt.oai.data.Record;
import no.sikt.oai.data.RecordsList;
import no.sikt.oai.exception.InternalOaiException;
import nva.commons.core.Environment;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;

public class DlrAdapter implements Adapter {

    private transient final ObjectMapper mapper = new ObjectMapper();
    private transient final String recordsUri; // "https://api-dev.dlr.aws.unit.no/dlr-gui-backend-resources-search/v1/oai/resources";
    private transient final String recordUri; // "https://api-dev.dlr.aws.unit.no/dlr-gui-backend-resources-search/v1/oai/resource";
    private transient final String setsUri; // "https://api-dev.dlr.aws.unit.no/dlr-gui-backend-resources-search/v1/oai/institutions";

    public DlrAdapter(Environment environment) {
        setsUri = environment.readEnv(SETS_URI_ENV);
        recordUri = environment.readEnv(RECORD_URI_ENV);
        recordsUri = environment.readEnv(RECORDS_URI_ENV);
    }

    @Override
    public boolean isValidIdentifier(String identifier) {
        return identifier.length() == 36;
    }

    @Override
    public String getDescription() {
        return "Repository for DLR resources";
    }

    @Override
    public String getDateGranularity() {
        return "YYYY-MM-DD";
    }

    @Override
    public String getEarliestTimestamp() {
        return "2014-08-19T15:47:16Z";
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
        return "dlradmin@unit.no";
    }

    @Override
    public String getRepositoryName() {
        return "DLR Repository";
    }

    @Override
    public String getBaseUrl() {
        return "https://dlr.unit.no";
    }

    @Override
    public String getIdentifierPrefix() {
        return "oai:dlr.unit.no:";
    }

    @Override
    public List<OaiSet> parseSetsResponse(String json) throws InternalOaiException {
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        try {
            List<String> institutionList = mapper.readValue(json, Institutions.class).institutionList;
            return institutionList.stream().map(inst -> new OaiSet(inst, inst)).collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            throw new InternalOaiException(e, HTTP_UNAVAILABLE);
        }
    }

    @Override
    public Record parseRecordResponse(String json, String metadataPrefix) throws InternalOaiException {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            return createRecordFromResource(mapper.readValue(json, Resource.class), metadataPrefix);
        } catch (JsonProcessingException e) {
            throw new InternalOaiException(e, HTTP_UNAVAILABLE);
        }
    }

    @Override
    public RecordsList parseRecordsListResponse(String verb, String json, String metadataPrefix)
            throws InternalOaiException {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            ResourceSearchResponse resourceSearchResponse = mapper.readValue(json, ResourceSearchResponse.class);
            RecordsList records = new RecordsList(resourceSearchResponse.numFound);
            for (String resourceString : resourceSearchResponse.resourcesAsJson) {
                records.add(createRecordFromResource(mapper.readValue(resourceString, Resource.class), metadataPrefix));
            }
            return records;
        } catch (JsonProcessingException e) {
            throw new InternalOaiException(e, HTTP_UNAVAILABLE);
        }
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
                .fromUri(recordUri)
                .addChild(identifier)
                .getUri();
    }

    @Override
    public URI getRecordsListUri(String from, String until, String institution, int startPosition) {
        UriWrapper uriWrapper = UriWrapper.fromUri(recordsUri);
        if (StringUtils.isNotEmpty(institution)) {
            uriWrapper = uriWrapper.addQueryParameter("filter", "facet_institution::" + institution);
        }
        if (StringUtils.isNotEmpty(from)) {
            uriWrapper = uriWrapper.addQueryParameter("from", from);
        }
        if (StringUtils.isNotEmpty(until)) {
            uriWrapper = uriWrapper.addQueryParameter("until", until);
        }
        if (startPosition != 0) {
            uriWrapper = uriWrapper.addQueryParameter("offset", String.valueOf(startPosition));
        }
        return uriWrapper.getUri();
    }

    private Record createRecordFromResource(Resource resource, String metadataPrefix) {
        boolean deleted = Boolean.parseBoolean(resource.features.get("dlr_status_deleted"));
        return new Record(
                createRecordContent(resource, metadataPrefix),
                deleted,
                getIdentifierPrefix() + resource.identifier,
                TimeUtils.string2Date(resource.features.get("dlr_time_updated"), TimeUtils.FORMAT_ZULU_SHORT));
    }

    private String createRecordContent(Resource resource, String metadataPrefix) {
        if (metadataPrefix.equalsIgnoreCase(MetadataFormat.QDC.name())) {
            return createRecordContentQdc(resource);
        } else if (metadataPrefix.equalsIgnoreCase(MetadataFormat.OAI_DATACITE.name())) {
            return createRecordContentOaiDatacite(resource);
        } else if (metadataPrefix.equalsIgnoreCase(MetadataFormat.OAI_DC.name())) {
            return createRecordContentOaiDc(resource);
        }
        return "";
    }

    @SuppressWarnings({"PMD.ConsecutiveLiteralAppends", "PMD.InsufficientStringBufferDeclaration"})
    private String createRecordContentOaiDc(Resource resource) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n")
                .append(" xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n")
                .append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n")
                .append(" xmlns:doc=\"http://www.lyncode.com/xoai\"\n")
                .append(" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/")
                .append(" http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n")
                .append("    <dc:title>").append(resource.features.get("dlr_title")).append("</dc:title>\n")
                .append("    <dc:description>").append(resource.features.get("dlr_description"))
                .append("</dc:description>\n");
        for (ResourceCreator creator : resource.creators) {
            buffer.append("    <dc:creator>").append(creator.features.get("dlr_creator_name"))
                    .append("</dc:creator>\n");
        }
        for (ResourceContributor contributor : resource.contributors) {
            buffer.append("    <dc:contributor>").append(contributor.features.get("dlr_contributor_name"))
                    .append("</dc:contributor>\n");
        }
        buffer.append("    <dc:rights>").append(resource.features.get("dlr_rights_license_name"))
                .append("</dc:rights>\n")
                .append("    <dc:type>").append(resource.features.get("dlr_type")).append("</dc:type>\n")
                .append("    <dc:date>").append(resource.features.get("dlr_time_created")).append("</dc:date>\n")
                .append("    <dc:date>").append(resource.features.get("dlr_time_published")).append("</dc:date>\n")
                .append("    <dc:identifier>").append(resource.features.get("dlr_identifier_handle"))
                .append("</dc:identifier>\n")
                .append("    <dc:identifier>").append(resource.identifier).append("</dc:identifier>\n")
                .append("</oai_dc:dc>\n");
        return buffer.toString();
    }

    @SuppressWarnings({"PMD.ConsecutiveLiteralAppends", "PMD.InsufficientStringBufferDeclaration"})
    private String createRecordContentQdc(Resource resource) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<qdc:qualifieddc xmlns:doc=\"http://www.lyncode.com/xoai\"\n")
                .append(" xmlns:dcterms=\"http://purl.org/dc/terms/\"\n")
                .append(" xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n ")
                .append(" xmlns:qdc=\"http://dspace.org/qualifieddc/\"\n")
                .append(" xsi:schemaLocation=\"http://purl.org/dc/elements/1.1/")
                .append(" http://dublincore.org/schemas/xmls/qdc/2006/01/06/dc.xsd")
                .append(" http://purl.org/dc/terms/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dcterms.xsd")
                .append(" http://dspace.org/qualifieddc/")
                .append("http://www.ukoln.ac.uk/metadata/dcmi/xmlschema/qualifieddc.xsd\">\n")
                .append("    <dc:title>").append(resource.features.get("dlr_title")).append("</dc:title>\n")
                .append("    <dc:description>").append(resource.features.get("dlr_description"))
                .append("</dc:description>\n");
        for (ResourceCreator creator : resource.creators) {
            buffer.append("    <dc:creator>")
                    .append(creator.features.get("dlr_creator_name"))
                    .append("</dc:creator>\n");
        }
        for (ResourceContributor contributor : resource.contributors) {
            buffer.append("    <dc:contributor>")
                    .append(contributor.features.get("dlr_contributor_name"))
                    .append("</dc:contributor>\n");
        }
        buffer.append("    <dc:rights>").append(resource.features.get("dlr_rights_license_name"))
                .append("</dc:rights>\n")
                .append("    <dc:type>").append(resource.features.get("dlr_type")).append("</dc:type>\n")
                .append("    <dcterms:created>").append(resource.features.get("dlr_time_created"))
                .append("</dcterms:created>\n")
                .append("    <dcterms:identifier xsi:type=\"dcterms:URI\">")
                .append(resource.features.get("dlr_identifier_handle")).append("</dcterms:identifier>\n")
                .append("    <dcterms:identifier>").append(resource.identifier).append("</dcterms:identifier>\n")
                .append("</qdc:qualifieddc>\n");
        return buffer.toString();
    }

    @SuppressWarnings({"PMD.ConsecutiveLiteralAppends", "PMD.InsufficientStringBufferDeclaration"})
    private String createRecordContentOaiDatacite(Resource resource) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<oaire:resource xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n")
                .append(" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n")
                .append(" xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n")
                .append(" xmlns:datacite=\"http://datacite.org/schema/kernel-4\"\n")
                .append(" xmlns:vc=\"http://www.w3.org/2007/XMLSchema-versioning\"\n")
                .append(" xmlns:oaire=\"http://namespace.openaire.eu/schema/oaire/\"\n")
                .append(" xsi:schemaLocation=\"http://namespace.openaire.eu/schema/oaire/")
                .append(" https://www.openaire.eu/schema/repo-lit/4.0/openaire.xsd\">\n")
                .append("    <datacite:titles>\n")
                .append("        <datacite:title>").append(resource.features.get("dlr_title"))
                .append("</datacite:title>\n")
                .append("    </datacite:titles>\n")
                .append("    <dc:description>").append(resource.features.get("dlr_description"))
                .append("</dc:description>\n");
        for (ResourceCreator creator : resource.creators) {
            buffer.append("    <datacite:creators>\n")
                    .append("        <datacite:creator>\n")
                    .append("            <datacite:creatorName>").append(creator.features.get("dlr_creator_name"))
                    .append("</datacite:creatorName>\n")
                    .append("        </datacite:creator>\n")
                    .append("    </datacite:creators>\n");
        }
        buffer.append("    <datacite:dates>\n")
                .append("        <datacite:date dateType=\"Issued\">")
                .append(resource.features.get("dlr_time_published"))
                .append("</datacite:date>\n")
                .append("    </datacite:dates>\n")
                .append("</oaire:resource>\n");
        return buffer.toString();
    }

    private static class Institutions {
        @JsonProperty("institutions")
        /* default */ transient List<String> institutionList;
    }

    private static class Resource {
        @JsonProperty("identifier")
        /* default */ transient String identifier;
        @JsonProperty("features")
        /* default */ transient Map<String, String> features;
        @JsonProperty("creators")
        /* default */ transient List<ResourceCreator> creators;
        @JsonProperty("contributors")
        /* default */ transient List<ResourceContributor> contributors;
    }

    private static class ResourceCreator {
        @JsonProperty("features")
        /* default */ transient Map<String, String> features;
    }

    private static class ResourceContributor {
        @JsonProperty("features")
        /* default */ transient Map<String, String> features;
    }

    private static class ResourceSearchResponse {
        @JsonProperty("offset")
        /* default */ transient String offset;
        @JsonProperty("limit")
        /* default */ transient String limit;
        @JsonProperty("numFound")
        /* default */ transient long numFound;
        @JsonProperty("queryTime")
        /* default */ transient int queryTime;
        @JsonProperty("resourcesAsJson")
        /* default */ transient List<String> resourcesAsJson;
        @JsonProperty("facet_counts")
        /* default */ transient List<Map<String, String>> facet_counts;
        @JsonProperty("spellcheck_suggestions")
        /* default */ transient List<String> spellcheck_suggestions;
    }

}

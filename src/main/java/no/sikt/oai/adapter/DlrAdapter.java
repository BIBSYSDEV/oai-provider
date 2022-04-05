package no.sikt.oai.adapter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.sikt.oai.TimeUtils;
import no.sikt.oai.Verb;
import no.sikt.oai.data.Record;
import no.sikt.oai.data.RecordsList;
import no.sikt.oai.exception.OaiException;
import nva.commons.core.paths.UriWrapper;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static no.sikt.oai.OaiConstants.NO_SETS_FOUND;
import static no.sikt.oai.OaiConstants.NO_SET_HIERARCHY;

public class DlrAdapter implements Adapter{

    private ObjectMapper mapper = new ObjectMapper();

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
        return "1976-01-01T00:00:01Z";
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
        return "https://example.com";
    }

    @Override
    public List<String> parseInstitutionResponse(String json) throws OaiException {
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        try {
            return mapper.readValue(json, Institutions.class).institutions;
        } catch (JsonProcessingException e) {
            throw new OaiException(Verb.ListSets.name(), NO_SET_HIERARCHY, NO_SETS_FOUND);
        }
    }

    @Override
    public Record parseRecordResponse(String json) throws OaiException {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            return createRecordFromResource(mapper.readValue(json, Resource.class));
        } catch (JsonProcessingException e) {
            throw new OaiException(Verb.GetRecord.name(), NO_SET_HIERARCHY, NO_SETS_FOUND);
        }
    }

    @Override
    public RecordsList parseRecordsListResponse(String verb, String json) throws OaiException {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            ResourceSearchResponse resourceSearchResponse = mapper.readValue(json, ResourceSearchResponse.class);
            RecordsList records = new RecordsList(resourceSearchResponse.numFound);
            for (String resourceString : resourceSearchResponse.resourcesAsJson) {
                records.add(createRecordFromResource(mapper.readValue(resourceString, Resource.class)));
            }
            return records;
        } catch (JsonProcessingException e) {
            throw new OaiException(verb, NO_SET_HIERARCHY, NO_SETS_FOUND);
        }
    }

    @Override
    public URI getInstitutionsUri() {
        return UriWrapper
                .fromUri("https://api-dev.dlr.aws.unit.no/dlr-gui-backend-resources-search/v1/oai/institutions")
                .getUri();
    }

    @Override
    public URI getRecordUri(String identifier) {
        return UriWrapper
                .fromUri("https://api-dev.dlr.aws.unit.no/dlr-gui-backend-resources-search/v1/oai/resources")
                .addChild(identifier)
                .getUri();
    }

    @Override
    public URI getRecordsListUri(String from, String until, String institution, int startPosition) {
        UriWrapper uriWrapper = UriWrapper.fromUri("https://api-dev.dlr.aws.unit.no/dlr-gui-backend-resources-search/v1/oai/resources");
        if (institution != null) {
            uriWrapper = uriWrapper.addQueryParameter("filter", "facet_institution::" + institution);
        }
        if (from != null) {
            uriWrapper = uriWrapper.addQueryParameter("from", from);
        }
        if (until != null) {
            uriWrapper = uriWrapper.addQueryParameter("until", until);
        }
        return uriWrapper.getUri();
    }

    private Record createRecordFromResource(Resource resource) {
        return new Record(
                createRecordXmlContent(resource),
                false,
                resource.identifier,
                TimeUtils.String2Date(resource.features.get("dlr_time_published"), TimeUtils.FORMAT_ZULU_SHORT));
    }

    private String createRecordXmlContent(Resource resource) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("                <oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" +
                      "                           xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
                      "                           xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:doc=\"http://www.lyncode.com/xoai\"\n" +
                      "                           xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n");
        buffer.append("                    <dc:title>").append(resource.features.get("dlr_title")).append("</dc:title>\n");
        buffer.append("                    <dc:description>").append(resource.features.get("dlr_description")).append("</dc:description>\n");
        for (ResourceCreator creator : resource.creators) {
            buffer.append("                    <dc:creator>").append(creator.features.get("dlr_creator_name")).append("</dc:creator>\n");
        }
        for (ResourceContributor contributor : resource.contributors) {
            buffer.append("                    <dc:contributor>").append(contributor.features.get("dlr_contributor_name")).append("</dc:contributor>\n");
        }
        buffer.append("                    <dc:rights>").append(resource.features.get("dlr_rights_license_name")).append("</dc:rights>\n");
        buffer.append("                    <dc:type>").append(resource.features.get("dlr_type")).append("</dc:type>\n");
        buffer.append("                </oai_dc:dc>\n");
        return buffer.toString();
    }

    private static class Institutions {
        @JsonProperty("institutions")
        List<String> institutions;
    }

    private static class Resource {
        @JsonProperty("identifier")
        String identifier;
        @JsonProperty("features")
        Map<String, String> features;
        @JsonProperty("creators")
        List<ResourceCreator> creators;
        @JsonProperty("contributors")
        List<ResourceContributor> contributors;
    }

    private static class ResourceCreator {
        @JsonProperty("features")
        Map<String, String> features;
    }

    private static class ResourceContributor {
        @JsonProperty("features")
        Map<String, String> features;
    }

    private static class ResourceSearchResponse {
        @JsonProperty("offset")
        String offset;
        @JsonProperty("limit")
        String limit;
        @JsonProperty("numFound")
        long numFound;
        @JsonProperty("queryTime")
        int queryTime;
        @JsonProperty("resourcesAsJson")
        List<String> resourcesAsJson;
        @JsonProperty("facet_counts")
        List<Map<String, String>> facet_counts;
        @JsonProperty("spellcheck_suggestions")
        List<String> spellcheck_suggestions;
    }

}

//package no.sikt.oai.temp.rest;
//
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//import io.swagger.annotations.ApiParam;
//import io.swagger.annotations.ApiResponse;
//import io.swagger.annotations.ApiResponses;
//import no.bibsys.http.HttpUtil;
//import no.bibsys.oai.AbstractOai;
//import no.bibsys.oai.OAIConfig;
//import no.bibsys.oai.OAIDatasetDefinition;
//import no.bibsys.oai.OAIException;
//import no.bibsys.oai.OAIIdentifier;
//import no.bibsys.oai.ResumptionToken;
//import no.bibsys.oai.data.Record;
//import no.bibsys.oai.data.RecordProvider;
//import no.bibsys.oai.data.RecordsList;
//import no.bibsys.oai.rest.filter.PrettyPrint;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.ws.rs.DefaultValue;
//import javax.ws.rs.GET;
//import javax.ws.rs.Path;
//import javax.ws.rs.Produces;
//import javax.ws.rs.QueryParam;
//import javax.ws.rs.core.Context;
//import javax.ws.rs.core.MediaType;
//import javax.ws.rs.core.Response;
//import java.util.Optional;
//
//@Path("/oai")
//@Api(value = "/oai", description = "OAI")
//public class OaiRest extends AbstractOai {
//
//	transient static final Logger logger = LoggerFactory.getLogger(OaiRest.class);
//
//	private final RecordProvider recordProvider;
//
//	public OaiRest(RecordProvider recordProvider, OAIConfig oaiConfig, String prefix) {
//		super(oaiConfig, prefix);
//
//		this.recordProvider = recordProvider;
//	}
//
//    // http://www.openarchives.org/OAI/openarchivesprotocol.html
//
//    @GET
//    @ApiOperation(position = 0, value = "OAI request", notes = "OAI request")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "Ok"),
//            @ApiResponse(code = 302, message = "Temporary redirect"),
//            @ApiResponse(code = 400, message = "Bad request"),
//            @ApiResponse(code = 404, message = "Not found"),
//            @ApiResponse(code = 503, message = "Service unavailable")
//    })
//    @Produces(MediaType.TEXT_XML)
//    @PrettyPrint
//    public Response get(@ApiParam(value = "Verb", required = true, allowableValues = "Identify,GetRecord,ListIdentifiers,ListMetadataFormats,ListRecords,ListSets") @QueryParam("verb") String verb,
//                        @ApiParam(value = "Identifier", required = false) @QueryParam("identifier") String identifier,
//                        @ApiParam(value = "From", required = false) @QueryParam("from") String from,
//                        @ApiParam(value = "Until", required = false) @QueryParam("until") String until,
//                        @ApiParam(value = "Metadata Prefix", required = false) @QueryParam("metadataPrefix") @DefaultValue( "marcxchange" ) String metadataPrefix,
//                        @ApiParam(value = "Set", required = false) @QueryParam("set") String set,
//                        @ApiParam(value = "Resumption Token", required = false) @QueryParam("resumptionToken") String resumptionToken,
//                        @Context HttpServletRequest httpRequest) {
//        try {
//
//            long startTime = System.currentTimeMillis();
//
//            StringBuilder baseUrlBuffer = new StringBuilder();
//
//            if (httpRequest != null) {
//                baseUrlBuffer.append(HttpUtil.getBasePath(httpRequest));
//                baseUrlBuffer.append(httpRequest.getRequestURI());
//            }
//
//            Optional<String> optionalResumptionToken = Optional.ofNullable(resumptionToken);
//
//            String baseUrl = baseUrlBuffer.toString();
//
//            validateAllParameters(httpRequest.getParameterMap(), verb);
//            validateVerbAndRequiredParameters(verb, optionalResumptionToken, metadataPrefix);
//            validateFromAndUntilParameters(verb, from, until);
//
//            if (verb != null && !verb.trim().isEmpty()) {
//
//                switch (verb) {
//                    case "Identify":
//                        return IdentifyResponse(verb, baseUrl, startTime);
//                    case "GetRecord":
//                        return GetRecordResponse(verb, identifier, metadataPrefix, baseUrl, startTime);
//                    case "ListIdentifiers":
//                        return ListIdentifiersResponse(verb, from, until, metadataPrefix, set, optionalResumptionToken, baseUrl, startTime);
//                    case "ListMetadataFormats":
//                        return ListMetadataFormatsResponse(verb, baseUrl, startTime);
//                    case "ListRecords":
//                        return ListRecordsResponse(verb, from, until, set, optionalResumptionToken, metadataPrefix, baseUrl, startTime);
//                    case "ListSets":
//                        return ListSetsResponse(verb, baseUrl, startTime);
//                    default:
//                        throw new OAIException(verb, "badVerb", "Illegal OAI verb");
//                }
//
//            }
//        } catch (OAIException e) {
//            logger.debug("", e);
//            return Response.status(Response.Status.OK).entity(createErrorResponse(httpRequest.getServerName(), httpRequest.getRequestURI(), e)).type(MediaType.TEXT_XML).build();
//        } catch (Exception e) {
//            logger.warn("", e);
//            //Handled below
//        }
//
//        return Response.status(Response.Status.BAD_REQUEST).entity("Invalid OAI request").type(MediaType.TEXT_XML).build();
//    }
//
//    public Response IdentifyResponse(String verb, String baseUrl, long startTime) {
//
//        String response = Identify(verb, baseUrl, startTime);
//
//        return Response.status(Response.Status.OK).entity(response).type(MediaType.TEXT_XML).build();
//    }
//
//    public Response ListMetadataFormatsResponse(String verb, String baseUrl, long startTime) {
//        // ERROR CODES: noMetadataFormats, idDoesNotExist, badArgument
//
//        String response = ListMetadataFormats(verb, baseUrl, startTime);
//
//        return Response.status(Response.Status.OK).entity(response).type(MediaType.TEXT_XML).build();
//    }
//
//
//
//
//    public Response GetRecordResponse(String verb, String identifier, String metadataPrefix, String baseUrl, long startTime) throws OAIException {
//        // REQUIRED: identifier, metadataPrefix
//        // ERROR CODES: cannotDisseminateFormat, idDoesNotExist, badArgument
//
//        if (identifier != null && !identifier.isEmpty() && metadataPrefix != null && !metadataPrefix.isEmpty()) {
//
//            if (!metadataFormatValidator.isValid(metadataPrefix)) {
//                throw new OAIException(verb, "cannotDisseminateFormat",
//                        "!The metadata format identified by the value given for the \nmetadataPrefix argument is not supported by the item or by the repository.");
//            }
//
//    		OAIIdentifier oaiIdentifier = new OAIIdentifier(identifier, prefix);
//
//    		Optional<Record> record = recordProvider.get(oaiIdentifier);
//
//    		if (!record.isPresent()) {
//    		    throw new OAIException(verb, "idDoesNotExist", "No matching identifier in the repository.");
//    		}
//
//            String response = GetRecord(record.get(), verb, identifier, metadataPrefix, baseUrl, startTime);
//
//            return Response.status(Response.Status.OK).entity(response).type(MediaType.TEXT_XML).build();
//        }
//
//        return Response.status(Response.Status.BAD_REQUEST).entity("Invalid request").type(MediaType.TEXT_XML).build();
//    }
//
//    public Response ListIdentifiersResponse(String verb, String from, String until, String metadataPrefix, String set, Optional<String> resumptionToken, String baseUrl, long startTime) throws OAIException {
//        // REQUIRED: metadataPrefix
//        // EXCLUSIVE: resumptionToken
//        // ERROR CODES: noSetHierarchy, noRecordsMatch, cannotDisseminateFormat, badResumptionToken, badArgument
//
//        int startPosition = 0;
//
//
//        if (resumptionToken.isPresent() && resumptionToken.get().length() > 0) {
//            ResumptionToken token = new ResumptionToken(resumptionToken.get());
//            from = token.from;
//            until = token.until;
//            set = token.setSpec;
//            metadataPrefix = token.metadataPrefix;
//            startPosition = Integer.parseInt(token.startPosition);
//            if (!resumptionTokenValidator.isValid(token)) {
//                throw new OAIException(verb, "badResumptionToken", "--The ResumptionToken should at least contain the setSpec.");
//            }
//        }
//
//        validateSetAndMetadataPrefix(verb, set, metadataPrefix);
//
//        OAIDatasetDefinition datasetDef = oaiConfig.getDataset(set);
//
//        int retryCount = 0;
//        boolean success = false;
//
//        do {
//            try {
//
//                RecordsList records = recordProvider.get(verb, from, until, datasetDef, startPosition);
//
//                if (records.numFound() == 0) {
//                    retryCount--;
//                    throw new OAIException(verb, "noRecordsMatch",
//                            "The combination of the values of the from, until, set and metadataPrefix arguments results in an empty list.");
//                }
//
//                String response = ListIdentifiers(verb, from, until, metadataPrefix, resumptionToken, baseUrl, startTime, startPosition,
//						datasetDef, records);
//                success = true;
//
//                return Response.status(Response.Status.OK).entity(response).type(MediaType.TEXT_XML).build();
//
//            } catch (Exception e) {
//                logger.error("RecordProvider error", e);
//            } finally {
//                retryCount++;
//            }
//        } while (!success && retryCount < oaiConfig.getMaximumRequestRetry());
//        if (!success && retryCount >= oaiConfig.getMaximumRequestRetry()) {
//            // Search timed out, kindly ask harvester to try harvesting later....
//            throw new RuntimeException("search timed out for databaseservice: " + datasetDef.toString());
//        }
//
//        return Response.status(Response.Status.BAD_REQUEST).entity("Invalid request").type(MediaType.TEXT_XML).build();
//    }
//
//    public Response ListRecordsResponse(String verb, String from, String until, String set, Optional<String> resumptionToken, String metadataPrefix, String baseUrl, long startTime) throws OAIException {
//        // REQUIRED: metadataPrefix
//        // EXCLUSIVE: resumptionToken
//        // ERROR CODES: noSetHierarchy, noRecordsMatch, cannotDisseminateFormat, badResumptionToken, badArgument
//
//        if ((metadataPrefix != null && !metadataPrefix.isEmpty()) || resumptionToken.isPresent() && !resumptionToken.get().isEmpty()) {
//
//            ResumptionToken resToken;
//            int startPosition = 0;
//
//            if (resumptionToken.isPresent() && resumptionToken.get().length() > 0) {
//                resToken = new ResumptionToken(resumptionToken.get());
//                from = resToken.from;
//                until = resToken.until;
//                set = resToken.setSpec;
//                metadataPrefix = resToken.metadataPrefix;
//                startPosition = Integer.parseInt(resToken.startPosition);
//                if (!resumptionTokenValidator.isValid(resToken)) {
//                    throw new OAIException(verb, "badResumptionToken", "--The ResumptionToken should at least contain the setSpec.");
//                }
//            }
//
//            validateSetAndMetadataPrefix(verb, set, metadataPrefix);
//
//            OAIDatasetDefinition datasetDef = oaiConfig.getDataset(set);
//
//            int retryCount = 0;
//            boolean success = false;
//
//            do {
//                try {
//
//                	RecordsList records = recordProvider.get(verb, from, until, datasetDef, startPosition);
//
//                    if (records.numFound() == 0) {
//                        retryCount--;
//                        success = true;
//                        throw new OAIException(verb, "noRecordsMatch",
//                                "The combination of the values of the from, until, set and metadataPrefix arguments results in an empty list.");
//                    }
//
//
//                    String response = ListRecords(verb, from, until, resumptionToken, metadataPrefix, baseUrl, startTime,
//							startPosition, datasetDef, records);
//                    success = true;
//
//                    return Response.status(Response.Status.OK).entity(response).type(MediaType.TEXT_XML).build();
//
//                } catch (OAIException e) {
//                	throw e;
//                } catch (Exception e) {
//                    logger.error("RecordProvider error", e);
//                } finally {
//                    retryCount++;
//                }
//            } while (!success && retryCount < oaiConfig.getMaximumRequestRetry());
//            if (!success && retryCount >= oaiConfig.getMaximumRequestRetry()) {
//                // Search timed out, kindly ask harvester to try harvesting later....
//                throw new RuntimeException("search timed out for databaseservice: " + datasetDef.toString());
//            }
//        }
//
//        return Response.status(Response.Status.NOT_FOUND).entity("Invalid request").type(MediaType.TEXT_XML).build();
//    }
//
//
//    public Response ListSetsResponse(String verb, String baseUrl, long startTime) {
//        // EXCLUSIVE: resumptionToken
//        // ERROR CODES: noSetHierarchy, badResumptionToken, badArgument
//
//        String response = ListSets(verb, baseUrl, startTime);
//
//        return Response.status(Response.Status.OK).entity(response).type(MediaType.TEXT_XML).build();
//    }
//
//}

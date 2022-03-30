//package no.sikt.oai.temp;
//
//import no.sikt.oai.temp.data.Record;
//import no.sikt.oai.temp.data.RecordsList;
//
//import java.util.Optional;
//
//public interface Oai {
//
//	public Object Identify(String verb, String baseUrl, long startTime);
//
//	public Object ListMetadataFormats(String verb, String baseUrl, long startTime);
//
//	public Object GetRecord(Record record, String verb, String identifier, String metadataPrefix, String baseUrl,
//			long startTime) throws OAIException;
//
//	public Object ListIdentifiers(String verb, String from, String until, String metadataPrefix,
//			Optional<String> resumptionToken, String baseUrl, long startTime, int startPosition,
//			OAIDatasetDefinition datasetDef, RecordsList records) throws OAIException;
//
//	public Object ListRecords(String verb, String from, String until, Optional<String> resumptionToken,
//			String metadataPrefix, String baseUrl, long startTime, int startPosition, OAIDatasetDefinition datasetDef,
//			RecordsList records) throws OAIException;
//
//	public Object ListSets(String verb, String baseUrl, long startTime);
//
//}
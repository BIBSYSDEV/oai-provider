package no.sikt.oai.temp.data;

import no.sikt.oai.temp.OAIDatasetDefinition;
import no.sikt.oai.temp.OAIIdentifier;

import java.util.Optional;

public interface RecordProvider {

	public Optional<Record> get(OAIIdentifier identifier);
	public RecordsList get(String verb, String from, String until, OAIDatasetDefinition datasetDef, int startPosition) throws RecordProviderException;
	
}

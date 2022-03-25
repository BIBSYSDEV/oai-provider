package no.sikt.oai.temp;

import no.sikt.oai.OaiConfig;

public class ResumptionTokenValidator {
	
	private final OaiConfig oaiConfig;
	
	public ResumptionTokenValidator(OaiConfig oaiConfig) {
		this.oaiConfig = oaiConfig;
	}
	
	public boolean isValid(ResumptionToken resumptionToken) {
		OAIDatasetDefinition datasetDef = oaiConfig.getDataset(resumptionToken.setSpec);
		return datasetDef != null;
	}

}

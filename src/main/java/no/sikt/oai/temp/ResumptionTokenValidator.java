package no.sikt.oai.temp;

public class ResumptionTokenValidator {
	
	private final OAIConfig oaiConfig;
	
	public ResumptionTokenValidator(OAIConfig oaiConfig) {
		this.oaiConfig = oaiConfig;
	}
	
	public boolean isValid(ResumptionToken resumptionToken) {
		OAIDatasetDefinition datasetDef = oaiConfig.getDataset(resumptionToken.setSpec);
		return datasetDef != null;
	}

}

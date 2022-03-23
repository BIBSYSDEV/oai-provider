package no.sikt.oai.temp;

public class MetadataFormatValidator {
	
	private final OAIConfig oaiConfig;
	
	public boolean isValid(String metadataFormat) {
		
		for (final String element : oaiConfig.getMetadataFormats()) {
			if (element.equalsIgnoreCase(metadataFormat)) {
				return true;
			}
		}
		return false;
	}

	public MetadataFormatValidator(OAIConfig oaiConfig) {
		this.oaiConfig = oaiConfig;
	}

}

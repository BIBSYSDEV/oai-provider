package no.sikt.oai.temp;

import no.sikt.oai.OaiConfig;

public class MetadataFormatValidator {
	
	private final OaiConfig oaiConfig;
	
	public boolean isValid(String metadataFormat) {
		
		for (final String element : oaiConfig.getMetadataFormats()) {
			if (element.equalsIgnoreCase(metadataFormat)) {
				return true;
			}
		}
		return false;
	}

	public MetadataFormatValidator(OaiConfig oaiConfig) {
		this.oaiConfig = oaiConfig;
	}

}

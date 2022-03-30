package no.sikt.oai;

import no.sikt.oai.adapter.Adapter;

public class ResumptionTokenValidator {
	
	private final Adapter adapter;
	
	public ResumptionTokenValidator(Adapter adapter) {
		this.adapter = adapter;
	}
	
	public boolean isValid(ResumptionToken resumptionToken) {
		return adapter.isValidSetName(resumptionToken.setSpec);
	}

}

package no.sikt.oai.temp;

public class OAIIdentifier  {

    private String identifier;
    
    private String prefix;

    public OAIIdentifier(String idString, String prefix) throws OAIException {

        try {

            if (!idString.startsWith(prefix)) {
            	throw new OAIException("GetRecord", "badArgument", "Illegal identifier.");
            }
            
            this.prefix = prefix;
            String identifier = idString.replace(prefix, "");
            setIdentifier(identifier);
            
        } catch (final Exception e) {
        	throw new OAIException("GetRecord", "badArgument", "Illegal identifier. Expected prefix '"+prefix+"'");
        }

    }

	public String getIdentifier() {
    	return identifier;
    }


	private void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	@Override
	public String toString() {
		return prefix+ getIdentifier();
	}

}

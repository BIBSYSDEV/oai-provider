package no.sikt.oai.temp;




public class OAIDatasetDefinition implements Comparable<OAIDatasetDefinition> {

	private String settNavn;
	private String setSpec;
	private String queryString;
	private long antallPoster = 0;
	private long tidspunktSisteInnhosting = 0L;

	public OAIDatasetDefinition(String setSpec, String settNavn, String queryString) {
		super();
		this.setSetSpec(setSpec);
		this.setSettNavn(settNavn);
		this.setQueryString(queryString);

	}

	@Override
	public int compareTo(OAIDatasetDefinition o) {
		if (o instanceof OAIDatasetDefinition) {
			return compareToSetDef(o);		
		}
		return 0;
	}

	private int compareToSetDef(OAIDatasetDefinition o) {
		if(o != null && getSettNavn() != null) {
			return getSettNavn().compareTo(o.getSettNavn());
		}
		return 0;

	}

	@Override
	public String toString() {
		return "OAIDatasetDefinition [setSpec=" + getSetSpec() + ", queryString=" + getQueryString() + "]";
	}

	public String getSetSpec() {
		return setSpec;
	}

	public void setSetSpec(String setSpec) {
		this.setSpec = setSpec;
	}

	public String getSettNavn() {
		return settNavn;
	}

	public void setSettNavn(String settNavn) {
		this.settNavn = settNavn;
	}

	public String getQueryString() {
		return queryString;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	public long getAntallPoster() {
		return antallPoster;
	}

	public void setAntallPoster(long antallPoster) {
		this.antallPoster = antallPoster;
	}

	public long getTidspunktSisteInnhosting() {
		return tidspunktSisteInnhosting;
	}

	public void setTidspunktSisteInnhosting(long tidspunktSisteInnhosting) {
		this.tidspunktSisteInnhosting = tidspunktSisteInnhosting;
	}


}

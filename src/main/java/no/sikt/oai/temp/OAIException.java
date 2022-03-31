package no.sikt.oai.temp;


/**
 *
 * Alle OAIExceptioners mor. Noe har  gått feil en plass i utførelsen av en request.
 * OAI definerer et gitt sett med feilmeldinger og responser
 * <p>
 * Endringshistorikk:<br>
 * 2003-08-15 SG: Opprettet<br>
 * <p>
 */
@Deprecated
public class OAIException
extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3723456618505981399L;
	public String errorCode;
	public String verb;
	public String errorText;

	public OAIException() {
	}

	public OAIException(String s) {
		super(s);
	}

	public OAIException(String verb, String errorCode, String errorText) {
		super();
		this.verb = verb;
		this.errorCode = errorCode;
		this.errorText = errorText;
	}

	@Override
	public String getMessage() {
		return verb + " " + errorCode + " " + errorText;
	}

	public String getVerb() {
		if (verb != null && verb.length() > 0) {
			return "verb=\"" + verb + "\"";
		}
		else {
			return "";
		}
	}

	@Override
	public String toString() {
		return verb + " " + errorCode + " " + errorText;
	}

}

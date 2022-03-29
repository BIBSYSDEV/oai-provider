package no.sikt.oai.data;

import java.util.Date;

public class Record {
	public final String content;
	public final boolean isDeleted;
	public final String identifier;
	public final Date lastUpdateDate;
	
	public Record(String content, boolean isDeleted, String identifier, Date lastUpdateDate) {
		this.content = content;
		this.isDeleted = isDeleted;
		this.identifier = identifier;
		this.lastUpdateDate = lastUpdateDate;
	}
}
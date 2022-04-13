package no.sikt.oai.data;

import java.util.Date;

public class Record {

	public transient final String content;
	public transient final boolean isDeleted;
	public transient final String identifier;
	public transient final Date lastUpdateDate;
	
	public Record(String content, boolean isDeleted, String identifier, Date lastUpdateDate) {
		this.content = content;
		this.isDeleted = isDeleted;
		this.identifier = identifier;
		this.lastUpdateDate = lastUpdateDate;
	}
}
package no.sikt.oai.data;

import java.util.ArrayList;

public class RecordsList extends ArrayList<Record> {
	
	private final long numFound;

	public RecordsList(long numFound) {
		this.numFound = numFound;
	}

	public long numFound() {
		return numFound;
	}
}

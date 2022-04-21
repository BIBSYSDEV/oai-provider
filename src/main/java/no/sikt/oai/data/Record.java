package no.sikt.oai.data;

import java.util.Date;
import java.util.List;

public class Record {

    public final transient String content;
    public final transient boolean isDeleted;
    public final transient String identifier;
    public final transient Date lastUpdateDate;

    public final transient List<String> setSpecs;

    public Record(String content, boolean isDeleted, String identifier, Date lastUpdateDate, List<String> setSpecs) {
        this.content = content;
        this.isDeleted = isDeleted;
        this.identifier = identifier;
        this.lastUpdateDate = lastUpdateDate;
        this.setSpecs = setSpecs;
    }
}
package no.sikt.oai.data;

import java.util.Date;
import java.util.List;

public class Record {

    public final transient String content;
    public final transient boolean deleted;
    public final transient String identifier;
    public final transient Date lastUpdateDate;

    public final transient List<String> setSpecs;

    public Record(String content, boolean deleted, String identifier, Date lastUpdateDate, List<String> setSpecs) {
        this.content = content;
        this.deleted = deleted;
        this.identifier = identifier;
        this.lastUpdateDate = lastUpdateDate;
        this.setSpecs = setSpecs;
    }

    public String getContent() {
        return content;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    public List<String> getSetSpecs() {
        return setSpecs;
    }
}
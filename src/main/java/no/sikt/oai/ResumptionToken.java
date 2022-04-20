package no.sikt.oai;

public class ResumptionToken {

    public String command = "";
    public long timestamp;
    public String setSpec = "";
    public String from = "";
    public String until = "";
    public String metadataPrefix = "";
    public String startPosition = "0";

    public static final String TOK_SEP = "~";

    public ResumptionToken(String token) {
        int endidx = token.indexOf(TOK_SEP);
        int startidx = 0;

        if (endidx > 0) {
            command = token.substring(startidx, endidx);
        }

        startidx = endidx + 1;
        endidx = token.indexOf(TOK_SEP, startidx);
        if (startidx < endidx) {
            setSpec = token.substring(startidx, endidx);
        }

        startidx = endidx + 1;
        endidx = token.indexOf(TOK_SEP, startidx);
        if (startidx < endidx) {
            from = token.substring(startidx, endidx);
        }

        startidx = endidx + 1;
        endidx = token.indexOf(TOK_SEP, startidx);
        if (startidx < endidx) {
            until = token.substring(startidx, endidx);
        }

        startidx = endidx + 1;
        endidx = token.indexOf(TOK_SEP, startidx);
        if (startidx < endidx) {
            metadataPrefix = token.substring(startidx, endidx);
        }

        startidx = endidx + 1;
        if (startidx < token.length()) {
            startPosition = token.substring(token.lastIndexOf(TOK_SEP) + 1);
        }
    }

    public ResumptionToken(String command, long timestamp, String setSpec, String from, String until,
                           String metadataPrefix, String startPosition) {

        this.command = command;
        this.timestamp = timestamp;
        this.setSpec = setSpec;
        this.from = from;
        this.until = until;
        this.metadataPrefix = metadataPrefix;
        this.startPosition = startPosition;
    }

    public String asString() {
        final String newToken = command + TOK_SEP + setSpec + TOK_SEP + from + TOK_SEP + until + TOK_SEP
                                + metadataPrefix + TOK_SEP + startPosition;
        return newToken;
    }
}

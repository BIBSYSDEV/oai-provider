package no.sikt.oai;

import java.util.HashMap;
import java.util.Map;

public enum ValidParameterKey {
    
        VERB("verb"),
        IDENTIFIER("identifier"),
        METADATAPREFIX("metadataPrefix"),
        FROM("from"),
        UNTIL("until"),
        SET("set"),
        RESUMPTIONTOKEN("resumptionToken");
        String key = "";
        public static final Map<String, ValidParameterKey> keyMap = new HashMap<String, ValidParameterKey>();

        static {
            for (ValidParameterKey parameterKey : values()) {
                keyMap.put(parameterKey.getKeyName(), parameterKey);
            }
        }

        ValidParameterKey(String key) {
            this.key = key;
        }

        public static boolean isValidParameterkey(String key) {
            return keyMap.containsKey(key);
        }

        public String getKeyName() {
            return key;
        }
}

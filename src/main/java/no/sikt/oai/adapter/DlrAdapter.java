package no.sikt.oai.adapter;


import java.util.Locale;

public class DlrAdapter implements Adapter{



    @Override
    public boolean isValidSetName(String setSpec) {
        return SetName.isValid(setSpec);
    }

    /**
     * TODO! has to be replaced with call to the backend to list all institutions/customers
     */
    enum SetName {
        NTNU,
        VID,
        SIKT;

        public static boolean isValid(String value) {
            for (SetName set : values()) {
                if (set.name().equals(value.toUpperCase(Locale.getDefault()))) {
                    return true;
                }
            }
            return false;
        }
    }
}

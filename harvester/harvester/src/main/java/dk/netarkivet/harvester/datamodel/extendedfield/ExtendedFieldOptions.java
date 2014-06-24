
package dk.netarkivet.harvester.datamodel.extendedfield;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * Class to represent options for Extended Fields. 
 */
public class ExtendedFieldOptions {
    /** Key-Value separator. */
    public static final String KEYVALUESEPARATOR = "=";
    /** Line separator. */
    public static final String NEWLINE = System.getProperty("line.separator");
    /** The option lines. */
    private String lines;
    /** The validity state of the list of extended field options. */        
    private boolean valid = false;
    
    /** Key-Value map containing the options. */
    private Map<String, String> options = new LinkedHashMap<String, String>();
    
    /**
     * Constructor. 
     * @param aLines Options separated by newlines (Null argument allowed)
     */
    public ExtendedFieldOptions(String aLines) {
        lines = aLines;

        parsing();
    }
    
    /**
     * Method that parses the data given to the constructor.
     */
    private void parsing() {
        if (lines == null) {
            return;
        }

        StringTokenizer st = new StringTokenizer(lines.trim(), System
                .getProperty("line.separator"));

        String key = null;
        String value = null;

        while (st.hasMoreElements()) {
            String line = st.nextToken();
            StringTokenizer st2 = new StringTokenizer(line, KEYVALUESEPARATOR);

            try {
                key = st2.nextToken();
                value = st2.nextToken();
                if (key.length() == 0 || value.length() == 0) {
                    continue;
                }
                options.put(key, value);
            } catch (NoSuchElementException e) {
                // invalid line, ignoring
                continue;
            }
        }

        if (!options.isEmpty()) {
            valid = true;
        }
    }

    /**
     * 
     * Is these ExtendedField options valid.
     * @return true, if the options are valid; otherwise false
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * @return the options as a map.
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * @return the options as lines separated by newlines.
     */
    public String getOptionsString() {
        String str = "";

        if (isValid()) {
            for (String key : options.keySet()) {
                str += key + KEYVALUESEPARATOR + options.get(key) 
                    + NEWLINE;
            }
        }

        return str;
    }
    
    /**
     * Check, if the given key is a valid option.
     * @param aKey a given option key.
     * @return true, if the list of options is valid, and 
     * there is an option in the options map with the given key.
     */
    public boolean isKeyValid(String aKey) {
        if (isValid()) {
            if (options.keySet().contains(aKey)) {
                return true;
            }
        }
        return false;
    }
}

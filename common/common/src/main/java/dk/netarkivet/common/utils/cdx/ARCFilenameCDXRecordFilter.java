
package dk.netarkivet.common.utils.cdx;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * A filter to use in CDXReader when finding CDXRecords matching a
 * filename-pattern.
 *
 */

@SuppressWarnings({ "serial"})
public class ARCFilenameCDXRecordFilter extends SimpleCDXRecordFilter {
    private String arcfilenamepattern;
    private Pattern p;

    /**
     * Class constructor.
     * @param arcfilenamepattern The filename pattern to be used by this filter
     * @param filtername The name of this filter
     * @throws ArgumentNotValid If any argument are null or an empty string.
     */
    public ARCFilenameCDXRecordFilter(String arcfilenamepattern,
            String filtername) throws ArgumentNotValid {
        super(filtername);
        ArgumentNotValid.checkNotNullOrEmpty(arcfilenamepattern,
                "arcfilenamepattern");
        this.arcfilenamepattern = arcfilenamepattern;
        this.p = Pattern.compile(arcfilenamepattern);
    }

    /**
     * Get the filename pattern used by this filter.
     * @return the filename pattern used by this filter.
     */
    public String getFilenamePattern() {
         return this.arcfilenamepattern;
    }
    
    /*
     * (non-Javadoc)
     * @see dk.netarkivet.common.utils.cdx.SimpleCDXRecordFilter#process(
     * dk.netarkivet.common.utils.cdx.CDXRecord)
     */
    public boolean process(CDXRecord cdxrec) {
        ArgumentNotValid.checkNotNull(cdxrec, "CDXRecord cdxrec");
        Matcher m = p.matcher(cdxrec.getArcfile());
        if (m.matches()) {
            return true;
        }
        return false;
    }
}

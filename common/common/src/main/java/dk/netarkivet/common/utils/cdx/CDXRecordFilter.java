
package dk.netarkivet.common.utils.cdx;


import java.io.Serializable;

/**
 * Interface defining a filter to use in CDXReader when finding CDXRecords.
 */

public interface CDXRecordFilter extends Serializable {

    /**
     * Process one CDXRecord - return true/false.
     * 
     * @param cdxrec
     *            the CDXRecord to be processed.
     * @return true or false on whether the processed CDXRecord is "valid"
     *         according to this filter implementation.
     *         true means this CDXRecord is valid!
     */
    boolean process(CDXRecord cdxrec);

    /**
     * @return the name of the Filter
     */
    String getFilterName();
}


package dk.netarkivet.common.utils.cdx;

import dk.netarkivet.common.exceptions.ArgumentNotValid;


/**
 * A Simple CDXRecordFilter to be extended.
 * It only implements the filtername method.
 */
@SuppressWarnings({ "serial"})
public abstract class SimpleCDXRecordFilter implements CDXRecordFilter{

    /**
     * Variable holding the filtername.
     */
    private String filtername;

    /**
     *
     * @param filtername - the name of the filter
     * @throws ArgumentNotValid If 'filtername' equals null or the empty string
     */
    public SimpleCDXRecordFilter(String filtername) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filtername, "filtername");
        this.filtername = filtername;
    }

    /**
     *
     * @return the filter name
     */
    public String getFilterName(){
        return this.filtername;
    }

    /*
     * (non-Javadoc)
     * @see dk.netarkivet.common.utils.cdx.CDXRecordFilter#process(
     * dk.netarkivet.common.utils.cdx.CDXRecord)
     */
    public abstract boolean process(CDXRecord cdxrec);
}

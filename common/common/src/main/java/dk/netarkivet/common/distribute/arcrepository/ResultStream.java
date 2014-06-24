package dk.netarkivet.common.distribute.arcrepository;

import java.io.InputStream;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/** Simple helper class to store the fact, whether we have a stream which 
 * contains a header or a stream, which does not.
 */
public class ResultStream {
    /** The inputstream w/ or without a HTTP header. */
    private final InputStream inputstream;

    /** Does the inputstream contains a HTTP header?. */
    private final boolean containsHeader;

    /**
     * Create a ResultStream with the given inputStream and information of
     * whether or not the inputStream contains a header.
     * 
     * @param inputstream
     *            An inputStream w/ the data for a stored URI
     * @param containsHeader
     *            true, if the stream contains a header, otherwise false
     */
    public ResultStream(InputStream inputstream, boolean containsHeader) {
        ArgumentNotValid.checkNotNull(inputstream, "InputStream inputstream");
        this.inputstream = inputstream;
        this.containsHeader = containsHeader;
    }

    /**
     * 
     * @return the inputstream
     */
    public InputStream getInputStream() {
        return this.inputstream;
    }

    /**
     * 
     * @return true, if the resultStream contains a header; otherwise false.
     */
    public boolean containsHeader() {
        return this.containsHeader;
    }

}


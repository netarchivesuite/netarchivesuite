
package dk.netarkivet.harvester.indexserver;

import java.util.regex.Pattern;

import dk.netarkivet.harvester.harvesting.metadata.MetadataFile;

/**
 * A RawDataCache that serves files with CDX data.
 *
 */
public class CDXDataCache extends RawMetadataCache {
    /**
     * Create a new CDXDataCache.  For a given job ID, this will fetch
     * and cache cdx data from metadata files
     * (&lt;ID&gt;-metadata-[0-9]+.arc).
     */
    public CDXDataCache() {
        super("cdxdata",
                Pattern.compile(MetadataFile.CDX_PATTERN),
                Pattern.compile("application/x-cdx"));
    }
}

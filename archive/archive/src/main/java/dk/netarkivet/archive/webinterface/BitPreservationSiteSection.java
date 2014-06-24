
package dk.netarkivet.archive.webinterface;

import dk.netarkivet.archive.Constants;
import dk.netarkivet.archive.arcrepository.bitpreservation.ActiveBitPreservationFactory;
import dk.netarkivet.common.webinterface.SiteSection;

/**
 * Site section that creates the menu for bit preservation.
 */
public class BitPreservationSiteSection extends SiteSection {
    /** The number of pages visible in the menu. 
     *  The visible pages: filestatus, batchoverview 
     */
    private static final int PAGES_VISIBLE_IN_MENU = 2; 
    /**
     * Create a new bit preservation SiteSection object.
     */
    public BitPreservationSiteSection() {
        super("mainname;bitpreservation", "Bitpreservation",
                PAGES_VISIBLE_IN_MENU,
                new String[][]{
                      {"filestatus", "pagetitle;filestatus"},
                      {"batchoverview", "pagetitle;batchjob.overview"},
                      // Pages below is not visible in the menu
                      {"batchjob", "pagetitle;batchjob"},
                      {"batchjob-retrieve", 
                          "pagetitle;batchjob.retrieve.resultfile"},
                      {"batchjob-execute", "pagetitle;batchjob.execute"},
                      {"filestatus-checksum",
                              "pagetitle;filestatus.checksum.errors"},
                      {"filestatus-missing", 
                                  "pagetitle;filestatus.files.missing"},
                      {"filestatus-update", 
                                  "pagetitle;filestatus.update"}
              }, "BitPreservation",
                 Constants.TRANSLATIONS_BUNDLE);
    }

    /** Initialize ActiveBitPreservation singleton.
     *  Should speed up the 1st rendering of the JSP pages. 
     */
    public void initialize() {
        ActiveBitPreservationFactory.getInstance();
    }

    /** No cleanup necessary in this site section. */
    public void close() {
    }
}


package dk.netarkivet.viewerproxy.webinterface;

import javax.servlet.http.HttpServletRequest;

import dk.netarkivet.common.webinterface.SiteSection;
import dk.netarkivet.viewerproxy.Constants;

/**
 * Site section that creates the menu for QA.
 *
 */
public class QASiteSection extends SiteSection {
    /** The amount of pages visible in the QA menu.*/
    private static final int PAGES_VISIBLE_IN_MENU = 1;
    
    /**
     * Create a QA SiteSection object.
     *
     * This initialises the SiteSection object with the pages that exists in
     * QA.
     */
    public QASiteSection() {
        super("sitesection;qa", "QA", PAGES_VISIBLE_IN_MENU,
              new String[][]{
                      {"status", "pagetitle;qa.status"},
                      // Pages below is not visible in the menu
                      {"getreports", "pagetitle;qa.get.reports"},
                      {"getfiles", "pagetitle;qa.get.files"},
                      {"crawlloglines",
                       "pagetitle;qa.crawllog.lines.for.domain"},
                      {"searchcrawllog", 
                           "pagetitle;qa.crawllog.lines.matching.regexp"}
              }, "QA",
                 Constants.TRANSLATIONS_BUNDLE);
    }

    /** Create a return-url for the QA pages that takes one.
     *
     * The current implementation is hokey, but trying to go through URL
     * objects is a mess.
     *
     * @param request The request that we have been called with.
     * @return A URL object that leads to the QA-status page on the same
     * machine as the request came from.
     */
    public static String createQAReturnURL(HttpServletRequest request) {
        return request.getRequestURL().toString().replaceAll(
                "/[^/]*\\.jsp.*$", "/QA-status.jsp");
    }

    /** No initialisation necessary in this site section. */
    public void initialize() {
    }

    /** No cleanup necessary in this site section. */
    public void close() {
    }
}

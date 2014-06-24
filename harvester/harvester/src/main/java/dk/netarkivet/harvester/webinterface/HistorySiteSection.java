
package dk.netarkivet.harvester.webinterface;

import dk.netarkivet.common.webinterface.SiteSection;
import dk.netarkivet.harvester.datamodel.RunningJobsInfoDAO;

/**
 * Site section that creates the menu for harvest history.
 */

public class HistorySiteSection extends SiteSection {
    /**
     * Create a new history SiteSection object.
     */
    public HistorySiteSection() {
        super("sitesection;history", "Harveststatus", 3,
              new String[][]{
                      {"alljobs", "pagetitle;all.jobs"},
                      {"deprecatedperdomain", "pagetitle;all.jobs.per.domain"},
                      {"running", "pagetitle;all.jobs.running"},
                      {"running-jobdetails", "pagetitle;running.job.details"},
                      {"perhd", "pagetitle;all.jobs.per.harvestdefinition"},
                      {"perharvestrun", "pagetitle;all.jobs.per.harvestrun"},
                      {"jobdetails", "pagetitle;details.for.job"},
                      {"perdomain", "pagetitle;all.jobs.per.domain"},
                      {"seeds", "pagetitle;seeds.for.harvestdefinition" }
              }, "History",
                 dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
    }

    /**
     * No initialisation necessary in this site section.
     */
    public void initialize() {
        // Initialize the running jobs tables if necessary
        RunningJobsInfoDAO.getInstance();
    }

    /** No cleanup necessary in this site section. */
    public void close() {
    }
}

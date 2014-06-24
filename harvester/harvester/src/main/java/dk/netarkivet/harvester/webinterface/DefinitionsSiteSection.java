
package dk.netarkivet.harvester.webinterface;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.webinterface.SiteSection;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDAO;
import dk.netarkivet.harvester.datamodel.HarvestDBConnection;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.ScheduleDAO;
import dk.netarkivet.harvester.datamodel.TemplateDAO;
import dk.netarkivet.harvester.harvesting.monitor.HarvestMonitor;
import dk.netarkivet.harvester.tools.HarvestTemplateApplication;

/**
 * Site section that creates the menu for data definitions.
 */
public class DefinitionsSiteSection extends SiteSection {
    /** Logger for this class. */
    private Log log = LogFactory.getLog(getClass().getName());
    /** number of pages visible in the left menu. */
    private static final int PAGES_VISIBLE_IN_MENU = 10;
    
    /**
     * Create a new definition SiteSection object.
     */
    public DefinitionsSiteSection() {
        super("sitesection;definitions", "Definitions", PAGES_VISIBLE_IN_MENU,
              new String[][]{
                      {"selective-harvests", "pagetitle;selective.harvests"},
                      {"snapshot-harvests", "pagetitle;snapshot.harvests"},
                      {"schedules", "pagetitle;schedules"},
                      {"find-domains", "pagetitle;find.domains"},
                      {"create-domain", "pagetitle;create.domain"},
                      {"domain-statistics", "pagetitle;domain.statistics"},
                      {"alias-summary", "pagetitle;alias.summary"},
                      {"edit-harvest-templates", 
                          "pagetitle;edit.harvest.templates"},
                      {"edit-global-crawler-traps",
                          "pagetitle;edit.global.crawler.traps"},
                      {"list-extendedfields",
                      "pagetitle;list-extendedfields"},
                      // The pages listed below are not visible in the left menu
                      {"upload-harvest-template",
                              "pagetitle;upload.template"},
                      {"download-harvest-template",
                              "pagetitle;download.template"},
                      {"edit-snapshot-harvest", "pagetitle;snapshot.harvest"},
                      {"edit-selective-harvest", "pagetitle;selective.harvest"},
                      {"edit-domain", "pagetitle;edit.domain"},
                      {"ingest-domains", "pagetitle;ingest.domains"},
                      {"add-event-seeds", "pagetitle;add.seeds"},
                      {"edit-domain-config", "pagetitle;edit.configuration"},
                      {"edit-domain-seedlist", "pagetitle;edit.seed.list"},
                      {"edit-schedule", "pagetitle;edit.schedule"},                      
                      {"edit-extendedfield", "pagetitle;edit.extendedfield"}
              }, "HarvestDefinition",
                 dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
    }

    /**
     * Initialise the site section.
     *
     * @throws UnknownID If the default order.xml does not exist.
     */
    public void initialize() {
        // Force migration if needed
        TemplateDAO templateDao = TemplateDAO.getInstance();
        // Enforce, that the default harvest-template set by
        // Settings.DOMAIN_DEFAULT_ORDERXML should exist.
        if (!templateDao.exists(Settings.get(
                HarvesterSettings.DOMAIN_DEFAULT_ORDERXML))) {
            String message = "The default order template '"
                    + Settings.get(HarvesterSettings.DOMAIN_DEFAULT_ORDERXML)
                    + "' does not exist in the template DAO. Please use the "
                    + HarvestTemplateApplication.class.getName()
                    + " tool to upload this template before"
                    + " loading the Definitions site section in the"
                    + " GUIApplication";
            log.fatal(message);
            throw new UnknownID(message);
        }

        DomainDAO.getInstance();
        ScheduleDAO.getInstance();
        HarvestDefinitionDAO.getInstance();
        JobDAO.getInstance();
        GlobalCrawlerTrapListDAO.getInstance();
        // Start the harvest monitor sever
        HarvestMonitor.getInstance();
    }
    
    /** Release DB resources. */
    public void close() {
        HarvestDBConnection.cleanup();
    }
}

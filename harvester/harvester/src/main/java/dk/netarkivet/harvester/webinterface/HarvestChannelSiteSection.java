
package dk.netarkivet.harvester.webinterface;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.webinterface.SiteSection;
import dk.netarkivet.harvester.datamodel.HarvestChannelDAO;
import dk.netarkivet.harvester.datamodel.HarvestDBConnection;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;

/**
 * Site section that creates the menu for harvest channel and mappings.
 */
@SuppressWarnings({ "unused"})
public class HarvestChannelSiteSection extends SiteSection {
	/** Logger for this class. */
	private Log log = LogFactory.getLog(getClass().getName());
	/** number of pages visible in the left menu. */
	private static final int PAGES_VISIBLE_IN_MENU = 2;

	/**
	 * Create a new definition SiteSection object.
	 */
	public HarvestChannelSiteSection() {
		super("sitesection;HarvestChannel", "HarvestChannel",
				PAGES_VISIBLE_IN_MENU,
				new String[][] {
				{ "edit-harvest-mappings", "pagetitle;edit.harvest.mappings" },
				{ "edit-harvest-channels", "pagetitle;edit.harvest.channels" }
				// The pages listed below are not visible in the left menu
		}, "HarvestChannel",
		dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
	}

	/**
	 * Initialise the site section.
	 * 
	 * @throws UnknownID
	 *             If the default order.xml does not exist.
	 */
	public void initialize() {
		HarvestDefinitionDAO.getInstance();
		HarvestChannelDAO.getInstance();
	}

	/** Release DB resources. */
	public void close() {
		HarvestDBConnection.cleanup();
	}
}

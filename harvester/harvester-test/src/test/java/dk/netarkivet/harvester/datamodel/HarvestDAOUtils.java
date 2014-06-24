
package dk.netarkivet.harvester.datamodel;

import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDBDAO;

/**
 * Allows resetting of the HarvestDB DAO's between tests..
 */
public class HarvestDAOUtils {

    public static void resetDAOs() {
        ExtendedFieldDBDAO.reset();
        DomainDAOTester.resetDomainDAO();
        TemplateDAOTester.resetTemplateDAO();
        HarvestDefinitionDAOTester.resetDAO();
        ScheduleDAOTester.resetDAO();
        JobDAOTester.resetDAO();
        GlobalCrawlerTrapListDBDAO.reset();
    }
}

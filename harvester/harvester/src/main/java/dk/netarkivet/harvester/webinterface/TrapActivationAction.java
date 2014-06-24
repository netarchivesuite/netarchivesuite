
package dk.netarkivet.harvester.webinterface;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapList;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDAO;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDBDAO;

/**
 * Action class for changing the activation status of a crawler trap list.
 *
 */

public class TrapActivationAction extends TrapAction {

    /**
     * The new activation state for the trap list.
     */
    private boolean newActivationState;

    /**
     * Constructor specifying whether this actions activates or deactivates
     * a trap list.
     * @param newActivationState the new activation state.
     */
    public TrapActivationAction(boolean newActivationState) {
        this.newActivationState = newActivationState;
    }

    @Override
    protected void doAction(PageContext context, I18n i18n) {
        HttpServletRequest request = (HttpServletRequest) context.getRequest();
        int trapId = Integer.parseInt(request.getParameter(Constants.TRAP_ID));
        GlobalCrawlerTrapListDAO dao = GlobalCrawlerTrapListDBDAO.getInstance();
        GlobalCrawlerTrapList trapList = dao.read(trapId);
        trapList.setActive(newActivationState);
        dao.update(trapList);
    }
}

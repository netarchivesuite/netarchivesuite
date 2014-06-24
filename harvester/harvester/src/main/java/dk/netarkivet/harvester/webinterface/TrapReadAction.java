
package dk.netarkivet.harvester.webinterface;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import java.io.IOException;
import java.io.OutputStream;

import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapList;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDAO;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDBDAO;

/**
 * Class to read and return a global crawler trap list to a web request.
 *
 */

public class TrapReadAction extends TrapAction {
    @Override
    protected void doAction(PageContext context, I18n i18n) {
        HttpServletRequest request = (HttpServletRequest) context.getRequest();
        int trapId = Integer.parseInt(request.getParameter(Constants.TRAP_ID));
        String contentType = request.getParameter(Constants.TRAP_CONTENT_TYPE);
        HttpServletResponse response = (HttpServletResponse)
                context.getResponse();
        GlobalCrawlerTrapListDAO dao = GlobalCrawlerTrapListDBDAO.getInstance();
        GlobalCrawlerTrapList trapList = dao.read(trapId);
        response.setHeader("Content-Type", contentType);
        if (contentType.startsWith("binary")) {
            response.setHeader("Content-Disposition", "Attachment; filename="
                                                      +  trapList.getName());
        }
        OutputStream out = null;
        try {
            out = response.getOutputStream();
            for (String trap : trapList.getTraps()) {
                out.write((trap + "\n").getBytes());
            }
            out.close();
        } catch (IOException e) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, e, "");
            throw new ForwardedToErrorPage("Error in retrieving trap list", e);
        }
    }
}

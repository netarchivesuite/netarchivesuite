/* File:        $Id: License.txt,v $
 * Revision:    $Revision: 1.4 $
 * Author:      $Author: csr $
 * Date:        $Date: 2005/04/11 16:29:16 $
 *
 * Copyright Det Kongelige Bibliotek og Statsbiblioteket, Danmark
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package dk.netarkivet.harvester.datamodel;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.DBUtils;

/**
 * A singleton giving access to global crawler traps.
 *
 * @author csr
 * @since Nov 26, 2009
 */

public class GlobalCrawlerTrapListDBDAO extends GlobalCrawlerTrapListDAO {

    /**
     * The logger for this class.
     */
    private static final Log log = LogFactory.
            getLog(GlobalCrawlerTrapListDBDAO.class);

    /**
     * The unique instance of this class.
     */
    private static GlobalCrawlerTrapListDBDAO instance;

    /**
     * version of global_crawler_trap_lists needed by the code.
     */
    private static final int TRAP_LIST_VERSION_NEEDED = 1;

    /**
     * version of global_crawler_trap_expressions needed by the code.
     */
    private static final int EXPRESSION_LIST_VERSION_NEEDED = 1;

    private GlobalCrawlerTrapListDBDAO() {
        int trap_list_version =
                DBUtils.getTableVersion(DBConnect.getDBConnection(),
                                  "global_crawler_trap_lists");
        if (trap_list_version < TRAP_LIST_VERSION_NEEDED) {
            log.info("Migrating table 'global_crawler_traps_list' from"
                     + "version " + trap_list_version + " to " +
                     TRAP_LIST_VERSION_NEEDED );
            DBSpecifics.getInstance().updateTable("global_crawler_trap_lists",
                                                  TRAP_LIST_VERSION_NEEDED);
        }
        int expression_list_version =
                DBUtils.getTableVersion(DBConnect.getDBConnection(),
                                  "global_crawler_trap_expressions");
       if (expression_list_version < EXPRESSION_LIST_VERSION_NEEDED) {
            log.info("Migrating table 'global_crawler_trap_expressions' from"
                     + "version " + expression_list_version + " to " +
                     EXPRESSION_LIST_VERSION_NEEDED );
            DBSpecifics.getInstance()
                    .updateTable("global_crawler_trap_expressions",
                                               EXPRESSION_LIST_VERSION_NEEDED);
        }
    }

    /**
     * Factory method to return the singleton instance of this class
     * @return
     */
    public static synchronized GlobalCrawlerTrapListDBDAO getInstance() {
        if (instance == null) {
            instance = new GlobalCrawlerTrapListDBDAO();
        }
        return instance;
    }

    public List<GlobalCrawlerTrapList> getAllActive() {
        //TODO: implement method
        throw new NotImplementedException("Not yet implemented:"
                                          + "dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDBDAO.getAllActive()");
    }

    public List<GlobalCrawlerTrapList> getAllInActive() {
        //TODO: implement method
        throw new NotImplementedException("Not yet implemented:"
                                          + "dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDBDAO.getAllInActive()");
    }

    public List<String> getAllActiveTrapExpressions() {
        //TODO: implement method
        throw new NotImplementedException("Not yet implemented:"
                                          + "dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDBDAO.getAllActiveTrapExpressions()");
    }

    public int create(GlobalCrawlerTrapList trapList) {
        //TODO: implement method
        throw new NotImplementedException("Not yet implemented:"
                                          + "dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDBDAO.create()");
    }

    public void delete(int id) {
        //TODO: implement method
        throw new NotImplementedException("Not yet implemented:"
                                          + "dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDBDAO.delete()");
    }

    public void update(GlobalCrawlerTrapList trapList) {
        //TODO: implement method
        throw new NotImplementedException("Not yet implemented:"
                                          + "dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDBDAO.update()");
    }

    public GlobalCrawlerTrapList read(int id) {
        //TODO: implement method
        throw new NotImplementedException("Not yet implemented:"
                                          + "dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDBDAO.read()");
    }


}

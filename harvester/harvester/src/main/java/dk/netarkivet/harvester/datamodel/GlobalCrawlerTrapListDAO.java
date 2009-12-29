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

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.UnknownID;

/**
 * A Data Access Object for managing persistent collections of global crawler
 * traps.
 *
 * @author csr
 * @since Nov 25, 2009
 */

public abstract class GlobalCrawlerTrapListDAO {

    /**
     * The singleton instace of this class.
     */
    private static GlobalCrawlerTrapListDAO instance;

    /**
     * Protected constructor called by concrete subclasses of this class.
     */
    protected GlobalCrawlerTrapListDAO() {
    }

    /**
     * Get the singleton instance of this class.
     * @return
     */
    public static synchronized GlobalCrawlerTrapListDAO getInstance() {
        throw new NotImplementedException("Not yet implemented");
    }

    /**
     * Get all active crawler traps.
     * @return
     */
    public abstract List<GlobalCrawlerTrapList> getAllActive();

    /**
     * Get all inactive crawler traps.
     * @return
     */
    public abstract List<GlobalCrawlerTrapList> getAllInActive();

    /**
     * Get a merged list (without duplicates) of all currently-active crawler
     * trap expressions.
     * @return
     */
    public abstract List<String> getAllActiveTrapExpressions();

    // CRUD methods for this DAO.

    /**
     *
     * @param trapList
     * @return
     * @throws ArgumentNotValid if the name of the trapList is already in use.
     */
    public abstract int create(GlobalCrawlerTrapList trapList) throws
                                                               ArgumentNotValid;

    public abstract void delete(int id) throws UnknownID;


    public abstract void update(GlobalCrawlerTrapList trapList) throws UnknownID;

    public abstract GlobalCrawlerTrapList read(int id) throws UnknownID;

}

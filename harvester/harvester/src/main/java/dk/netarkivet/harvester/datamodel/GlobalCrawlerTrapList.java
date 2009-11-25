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

import java.io.File;
import java.util.List;

import dk.netarkivet.common.exceptions.NotImplementedException;

/**
 * Class representing one or more global crawler traps, modelled as a list
 * of regular expressions. 
 *
 * @author csr
 * @since Nov 25, 2009
 */

public class GlobalCrawlerTrapList {

    /**
     * The unique id of this collection of crawler traps.
     */
    int id;

    /**
     * The list of traps. Each item is a regular expression matching url's to
     * be avoided.
     */
    List<String> traps;

    /**
     * A free-text description of the traps in this collection.
     */
    String description;

    /**
     * Whether or not this set of traps is active (in use).
     */
    boolean isActive;

    /**
     * Protected constructor used by the DAO to create instances of this class.
     *
     * @param id
     * @param traps
     * @param description
     * @param active
     */
    protected GlobalCrawlerTrapList(int id, List<String> traps, String description,
                                 boolean active) {
        this.id = id;
        this.traps = traps;
        this.description = description;
        isActive = active;
    }

    /**
     * Construct a new GlobalCrawlerTrapList from a local file consisting of
     * newline-separated regular expressions.
     * @param file
     * @param description
     * @param isActive
     */
    public GlobalCrawlerTrapList(File file, String description, boolean isActive) {
        throw new NotImplementedException("Not yet implemented");
    }

    public int getId() {
        return id;
    }

    public List<String> getTraps() {
        return traps;
    }

    public void setTraps(List<String> traps) {
        this.traps = traps;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}

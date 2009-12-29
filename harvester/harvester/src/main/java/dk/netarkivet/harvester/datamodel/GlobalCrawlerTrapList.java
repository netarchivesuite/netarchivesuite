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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

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
     * be avoided. In the database, (id, trap) is a primary key so we model
     * the traps as a Set to avoid possible duplicates.
     */
    Set<String> traps;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * A unique name by which this list is identified
     */
    String name;

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
     * @parame name
     * @param traps
     * @param description
     * @param active
     * @throws ArgumentNotValid if the name is empty or null.
     */
    protected GlobalCrawlerTrapList(int id, List<String> traps, String name,
                                    String description, boolean active) throws
                                                             ArgumentNotValid {
        this.id = id;
        this.traps = new HashSet<String>(traps.size());
        this.traps.addAll(traps);
        this.description = description;
        isActive = active;
        this.name = name;
    }

    /**
     * Construct a new GlobalCrawlerTrapList from an input stream consisting of
     * newline-separated regular expressions.
     * @param is
     * @param description
     * @param isActive
     * @param name
     * @throws IOFailure if the input stream cannot be found or read.
     * @throws ArgumentNotValid if the file is null or the name is null or
     * empty.
     */
    public GlobalCrawlerTrapList(InputStream is, String name, String description,
                                 boolean isActive) throws IOFailure,
                                                          ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(name, "name");
        ArgumentNotValid.checkNotNull(is, "is");
        this.traps = new HashSet<String>();
        this.isActive = isActive;
        this.name = name;
        if (description == null) {
            this.description = "";
        } else {
            this.description = description;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while  ( (line = reader.readLine()) != null ) {
                  traps.add(line.trim());
            }
        } catch (IOException e) {
            throw new IOFailure("Could not read crawler traps", e);
        }
    }

    public int getId() {
        return id;
    }

    public Set<String> getTraps() {
        return traps;
    }

    public void setTraps(Set<String> traps) {
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

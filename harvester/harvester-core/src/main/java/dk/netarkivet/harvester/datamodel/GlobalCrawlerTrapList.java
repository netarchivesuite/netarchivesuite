/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.harvester.datamodel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.harvester.utils.CrawlertrapsUtils;

/**
 * Class representing one or more global crawler traps, modeled as a set of regular expressions.
 */
public class GlobalCrawlerTrapList {

    /** The class logger. */
    private static final Logger log = LoggerFactory.getLogger(GlobalCrawlerTrapList.class);
    
    /** The unique id of this collection of crawler traps. */
    private int id;

    /**
     * The set of traps. Each item is a regular expression matching url's to be avoided. In the database, (id, trap) is
     * a primary key for the table global_crawler_trap_expressions so we model the traps as a Set to avoid possible
     * duplicates.
     */
    private Set<String> traps;

    /** A unique name by which this list is identified. */
    private String name;

    /** A free-text description of the traps in this collection. */
    private String description;

    /** Whether or not this set of traps is active (in use). */
    private boolean isActive;

    /**
     * Protected constructor used by the DAO to create instances of this class.
     *
     * @param id the id of this list.
     * @param name a name by which this list is known.
     * @param traps the set of trap expressions.
     * @param description A textual description of this list (may be null).
     * @param isActive flag indicating whether this list is isActive.
     * @throws ArgumentNotValid if the name is empty or null
     */
    protected GlobalCrawlerTrapList(int id, List<String> traps, String name, String description, boolean isActive)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(name, "name");
        ArgumentNotValid.checkNotNull(traps, "traps");
        this.id = id;
        this.traps = new HashSet<String>(traps.size());
        this.traps.addAll(traps);
        this.description = description;
        this.isActive = isActive;
        this.name = name;
        log.debug("Constructed the list {} with traps {}", name, traps.size());
    }

    /**
     * Construct a new GlobalCrawlerTrapList from an input stream consisting of newline-separated regular expressions.
     *
     * @param is an input stream from which the list of trap expressions can be read.
     * @param name a name by which this list is known.
     * @param description A textual description of this list.
     * @param isActive flag indicating whether this list is isActive.
     * @throws IOFailure if the input stream cannot be found or read.
     * @throws ArgumentNotValid if the input stream is null, the name is null or empty, or the list contains invalid expressions
     */
    public GlobalCrawlerTrapList(InputStream is, String name, String description, boolean isActive) throws IOFailure,
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
        setTrapsFromInputStream(is, name);
    }

    /**
     * A utility method to read the list of traps from an InputStream, line-by-line.
     *
     * @param is The input stream from which to read.
     * @param listName the name of the list being constructed
     * @throws IOFailure if the input stream cannot be read.
     * @throws ArgumentNotValid if the input stream is null or if any of the specified traps are not valid regular
     * expressions and valid XML
     */
    public void setTrapsFromInputStream(InputStream is, String listName) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(is, "is");
        traps.clear();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        int trapsAdded=0;
        int skippedEmptyLines=0;
        Set<String> errors = new HashSet<String>();
        try {
            while ((line = reader.readLine()) != null) {
                final String trap = line.trim();
                if (trap.isEmpty()) {
                    log.debug("Skipping empty line in input for list '{}'", listName);
                    skippedEmptyLines++;
                    continue; 
                } else {
                    try {
                        Pattern.compile(trap);
                        if (!CrawlertrapsUtils.isCrawlertrapsWellformedXML(trap)) {
                        	errors.add("The trap '" + trap + "' is not wellformed XML.");
                        }
                    } catch (PatternSyntaxException e) {
                    	errors.add("The trap '" + trap + "' is not a valid Java regular expression: " + e + " .");
                    }
                    traps.add(trap);
                    trapsAdded++;
                    log.trace("Added trap #{}: '{}'", trapsAdded, trap);
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Could not read crawler traps", e);
        }
        // See if any errors have been found 
        if (errors.size() > 0) {
        	throw new ArgumentNotValid("The traplist '" + listName + "' contains invalid expressions: " + StringUtils.conjoin(
        			",", errors));
        }
        
        
        log.info("GlobalCrawlertraps list '{}' with {} unique traps (non-unique={}, skipped emptyLines={})", listName, traps.size(), trapsAdded, skippedEmptyLines);
    }

    /**
     * Get the id of this list.
     *
     * @return the id.
     */
    public int getId() {
        return id;
    }

    /**
     * Set the id of this list.
     *
     * @param id the id.
     */
    protected void setId(int id) {
        this.id = id;
    }

    /**
     * Get the name of the list.
     *
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the list.
     *
     * @param name the name.
     */
    public void setName(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "name");
        this.name = name;
    }

    /**
     * Get the trap expressions for this list.
     *
     * @return the trap expressions.
     */
    public Set<String> getTraps() {
        return traps;
    }

    /**
     * Set the trap expressions for this list.
     *
     * @param traps the trap expressions.
     */
    public void setTraps(Set<String> traps) {
        ArgumentNotValid.checkNotNull(traps, "traps");
        this.traps = traps;
    }

    /**
     * Get the description of this list.
     *
     * @return the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description of this list.
     *
     * @param description the description.
     */
    public void setDescription(String description) {
        ArgumentNotValid.checkNotNull(description, "description");
        this.description = description;
    }

    /**
     * Returns true if this list is active.
     *
     * @return the activity state of the list.
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Set the activity state of the list.
     *
     * @param active the activity state.
     */
    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GlobalCrawlerTrapList that = (GlobalCrawlerTrapList) o;

        if (id != that.id) {
            return false;
        }
        if (isActive != that.isActive) {
            return false;
        }
        if (description != null ? !description.equals(that.description) : that.description != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (traps != null ? !traps.equals(that.traps) : that.traps != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (traps != null ? traps.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (isActive ? 1 : 0);
        return result;
    }

}

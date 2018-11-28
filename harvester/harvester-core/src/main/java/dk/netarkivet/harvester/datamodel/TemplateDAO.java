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

import java.util.Iterator;

import dk.netarkivet.common.utils.FilterIterator;

/**
 * DAO methods for reading templates only. Implemented as a Singleton
 */
public abstract class TemplateDAO implements DAO {

    /** The singleton TemplateDAO. */
    private static TemplateDAO instance;

    /**
     * Constructor for TemplateDAO. The real construction is done inside the getInstance method.
     */
    TemplateDAO() {
    }

    /**
     * Gets the TemplateDAO singleton.
     *
     * @return the singleton.
     */
    public static synchronized TemplateDAO getInstance() {
        if (instance == null) {
            instance = new TemplateDBDAO();
        }
        return instance;
    }

    /**
     * Read an orderxml template for the named order XML.
     *
     * @param orderXmlName The name of the order.xml document
     * @return The contents of this order.xml document
     */
    public abstract HeritrixTemplate read(String orderXmlName);

    /**
     * Returns an iterator with all names of order.xml-templates.
     *
     * @return Iterator<String> with all names of templates (without .xml).
     */
    public abstract Iterator<String> getAll();


    /**
     * Returns an iterator with names of either all active or all inactive order.xml-templates.
     *
     * @param active true if active templates are wanted, false otherwise.
     * @return Iterator<String> with all names of templates (without .xml).
     */
    public abstract Iterator<String> getAll(boolean active);

    /**
     * Returns an iterator of all templates. Note that this is not the most efficient way of getting all names of
     * templates, for that just use getAll(). Implements the Iterable interface.
     *
     * @return A list of all current templates.
     */
    public Iterator<HeritrixTemplate> iterator() {
        return new FilterIterator<String, HeritrixTemplate>(getAll()) {
            protected HeritrixTemplate filter(String s) {
                return read(s);
            }
        };
    }

    /**
     * Check, if there exists a orderxml-template with a given name.
     *
     * @param orderXmlName a given orderxml name
     * @return true, if there exists a orderxml-template with this name
     */
    public abstract boolean exists(String orderXmlName);

    /**
     * Create a orderxml-template with a given name.
     *
     * @param orderXmlName the given name
     * @param orderXml the Document containing the contents of this new orderxml-template
     */
    public abstract void create(String orderXmlName, HeritrixTemplate orderXml);

    /**
     * Update a specific orderxml-template to contain the contents of the orderXml argument.
     *
     * @param orderXmlName the name of a specific orderxml-template
     * @param orderXml the new contents of this template
     */
    public abstract void update(String orderXmlName, HeritrixTemplate orderXml);

    /**
     * Resets the singleton. Only for use from tests.
     */
    static void resetSingleton() {
        instance = null;
    }

}

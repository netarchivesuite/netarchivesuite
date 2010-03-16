/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package dk.netarkivet.harvester.datamodel;

import java.util.Iterator;

import dk.netarkivet.common.utils.FilterIterator;

/**
 * DAO methods for reading templates only.
 * Implemented as a Singleton
 *
 */
public abstract class TemplateDAO {
    /**
     * The singleton TemplateDAO.
     */
    private static TemplateDAO instance;

    /**
     * Constructor for TemplateDAO.
     * The real construction is done inside the getInstance method.
     */
    TemplateDAO() {
    }

    /**
     * Gets the TemplateDAO singleton.
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

    /** Returns an iterator of all templates.  Note that this is not the
     * most efficient way of getting all names of templates, for that just use
     * getAll().
     * Implements the Iterable interface.
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
     * @param orderXmlName a given orderxml name
     * @return true, if there exists a orderxml-template with this name
     */
    public abstract boolean exists(String orderXmlName);

    /**
     * Create a orderxml-template with a given name.
     * @param orderXmlName the given name
     * @param orderXml the Document containing the contents of
     * this new orderxml-template
     */
    public abstract void create(String orderXmlName, HeritrixTemplate orderXml);

    /**
     * Describe where a given order template is being used.
     *
     * @param orderXmlName a given order template
     * @return A string describing where the given order template is being used.
     */
    public abstract String describeUsages(String orderXmlName);

    /**
     * Delete a orderxml-template with a given name.
     * @param orderXmlName a given name
     */
    public abstract void delete(String orderXmlName);

    /**
     * Update a specific orderxml-template to contain the contents
     * of the orderXml argument.
     * @param orderXmlName the name of a specific orderxml-template
     * @param orderXml the new contents of this template
     */
    public abstract void update(String orderXmlName, HeritrixTemplate orderXml);

    /**
     * Resets the singleton.  Only for use from tests.
     */
    static void resetSingleton() {
        instance = null;
    }
}

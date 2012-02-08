/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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

import java.io.Reader;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.ExceptionUtils;

/**
 * Implements the TemplateDAO with databases.
 *
 * The statements to create the tables are now in
 * scripts/sql/createfullhddb.sql
 */

public class TemplateDBDAO extends TemplateDAO {
    /** the log.*/
    private final Log log = LogFactory.getLog(getClass());

    /** Default constructor.
     * Only used by TemplateDAO,getInstance().
     */
    TemplateDBDAO() {
        Connection connection = HarvestDBConnection.get();
        try {
            HarvesterDatabaseTables.checkVersion(connection, HarvesterDatabaseTables.ORDERTEMPLATES);
        } finally {
            HarvestDBConnection.release(connection);
        }
    }

    /**
     * Read an XML order file for the named order XML.
     *
     * @param orderXmlName The name of the order.xml document
     * @return The contents of this order.xml document
     */
    public synchronized HeritrixTemplate read(String orderXmlName) {
        ArgumentNotValid.checkNotNullOrEmpty(
                orderXmlName, "String orderXmlName");
        Connection c = HarvestDBConnection.get();
        PreparedStatement s = null;
        try {
            s = c.prepareStatement(
                    "SELECT orderxml FROM ordertemplates WHERE name = ?");
            s.setString(1, orderXmlName);
            ResultSet res = s.executeQuery();
            if (!res.next()) {
                throw new UnknownID("Can't find template " + orderXmlName);
            }
            Reader orderTemplateReader = null;
            if (DBSpecifics.getInstance().supportsClob()) {
                Clob clob = res.getClob(1);
                orderTemplateReader = clob.getCharacterStream();
            } else {
                orderTemplateReader = new StringReader(res.getString(1));
            }
            SAXReader reader = new SAXReader();
            // TODO Check what happens on non-ascii
            Document orderXMLdoc = reader.read(orderTemplateReader);
            return new HeritrixTemplate(orderXMLdoc);
        } catch (SQLException e) {
            final String message = "SQL error finding order.xml for "
                + orderXmlName
                + "\n" + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        } catch (DocumentException e) {
            final String message = "Error parsing order.xml string for "
                + orderXmlName;
            log.warn(message, e);
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Returns an iterator with all names of order.xml-templates.
     *
     * @return Iterator<String> with all names of templates (without .xml).
     */
    public synchronized Iterator<String> getAll() {
        Connection c = HarvestDBConnection.get();
        try {
            List<String> names = DBUtils.selectStringList(
                    c,
                    "SELECT name FROM ordertemplates ORDER BY name");
            return names.iterator();
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /** Return true if the database contains a template with the given name.
     *
     * @param orderXmlName Name of an order.xml template (without .xml).
     * @return True if such a template exists.
     * @throws ArgumentNotValid If the orderXmlName is null or an empty String
     */
    public synchronized boolean exists(String orderXmlName) {
        ArgumentNotValid.checkNotNullOrEmpty(
                orderXmlName, "String orderXmlName");

        Connection c = HarvestDBConnection.get();
        try {
            return exists(c, orderXmlName);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /** Return true if the database contains a template with the given name.
    *
    * @param orderXmlName Name of an order.xml template (without .xml).
    * @return True if such a template exists.
    * @throws ArgumentNotValid If the orderXmlName is null or an empty String
    */
   private synchronized boolean exists(Connection c, String orderXmlName) {
       int count = DBUtils.selectIntValue(
               c,
               "SELECT COUNT(*) FROM ordertemplates WHERE name = ?",
               orderXmlName);
       return count == 1;
   }

    /** Create a template. The template must not already exist.
     *
     * @param orderXmlName Name of the template.
     * @param orderXml XML documents that is a Heritrix order.xml template.
     * @throws ArgumentNotValid If the orderXmlName is null or an empty String,
     * or the orderXml is null.
     */
    public synchronized void create(String orderXmlName,
            HeritrixTemplate orderXml) {
        ArgumentNotValid.checkNotNullOrEmpty(
                orderXmlName, "String orderXmlName");
        ArgumentNotValid.checkNotNull(orderXml, "HeritrixTemplate orderXml");

        Connection c = HarvestDBConnection.get();
        PreparedStatement s = null;
        try {
            if (exists(c, orderXmlName)) {
                throw new PermissionDenied("An order template called "
                        + orderXmlName + " already exists");
            }

            s = c.prepareStatement("INSERT INTO ordertemplates "
                    + "( name, orderxml ) VALUES ( ?, ? )");
            DBUtils.setStringMaxLength(s, 1, orderXmlName,
                    Constants.MAX_NAME_SIZE, orderXmlName, "length");
            DBUtils.setClobMaxLength(s, 2, orderXml.getXML(),
                    Constants.MAX_ORDERXML_SIZE, "size", orderXmlName);
            s.executeUpdate();
        } catch (SQLException e) {
            throw new IOFailure("SQL error creating template " + orderXmlName
                    + "\n" + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /** Update a template. The template must already exist.
     *
     * @param orderXmlName Name of the template.
     * @param orderXml XML document that is a Heritrix order.xml template.
     * @throws PermissionDenied If the template does not exist
     * @throws IOFailure If the template could not be
     * @throws ArgumentNotValid If the orderXmlName is null or an empty String,
     * or the orderXml is null.
     */
    public synchronized void update(String orderXmlName,
            HeritrixTemplate orderXml) {
        ArgumentNotValid.checkNotNullOrEmpty(
                orderXmlName, "String orderXmlName");
        ArgumentNotValid.checkNotNull(orderXml, "HeritrixTemplate orderXml");

        Connection c = HarvestDBConnection.get();
        PreparedStatement s = null;
        try {
            if (!exists(c, orderXmlName)) {
                throw new PermissionDenied("No order template called "
                        + orderXmlName + " exists");
            }

            s = c.prepareStatement("UPDATE ordertemplates "
                    + "SET orderxml = ? "
                    + "WHERE name = ?");
            DBUtils.setClobMaxLength(s, 1, orderXml.getXML(),
                    Constants.MAX_ORDERXML_SIZE, "size", orderXmlName);
            s.setString(2, orderXmlName);
            s.executeUpdate();
        } catch (SQLException e) {
            throw new IOFailure("SQL error updating template " + orderXmlName
                    + "\n" + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            HarvestDBConnection.release(c);
        }
    }
}

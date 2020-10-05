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

import java.io.Reader;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.ExceptionUtils;

/**
 * Implements the TemplateDAO with databases.
 * <p>
 * The statements to create the tables are now in scripts/sql/createfullhddb.sql
 */

public class TemplateDBDAO extends TemplateDAO {

    /** the log. */
    private static final Logger log = LoggerFactory.getLogger(TemplateDBDAO.class);

    /**
     * Default constructor. Only used by TemplateDAO,getInstance().
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
        ArgumentNotValid.checkNotNullOrEmpty(orderXmlName, "String orderXmlName");
        Connection c = HarvestDBConnection.get();
        PreparedStatement s = null;
        log.debug("Reading template {} from database", orderXmlName);
        try {
            s = c.prepareStatement("SELECT template_id, orderxml, isActive FROM ordertemplates WHERE name = ?");
            s.setString(1, orderXmlName);
            ResultSet res = s.executeQuery();
            if (!res.next()) {
                throw new UnknownID("Can't find template " + orderXmlName);
            }
            Reader orderTemplateReader = null;
        	long template_id = res.getLong(1);
            if (DBSpecifics.getInstance().supportsClob()) {
                Clob clob = res.getClob(2);
                orderTemplateReader = clob.getCharacterStream();
            } else {
                String string = res.getString(2);
                orderTemplateReader = new StringReader(string);
            } 
            HeritrixTemplate heritrixTemplate = HeritrixTemplate.read(template_id, orderTemplateReader);
            heritrixTemplate.setIsActive(res.getBoolean(3));
            return heritrixTemplate;
        } catch (SQLException e) {
            final String message = "SQL error finding order.xml for " + orderXmlName + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e);
            log.warn(message, e);
            throw new IOFailure(message, e);
        }
         finally {
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
            List<String> names = DBUtils.selectStringList(c, "SELECT name FROM ordertemplates ORDER BY name");
            return names.iterator();
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    @Override
    public synchronized Iterator<String> getAll(boolean active) {
        Connection c = HarvestDBConnection.get();
        try {
            List<String> names = DBUtils.selectStringList(c, "SELECT name FROM ordertemplates WHERE isActive=? ORDER BY name ", active);
            return names.iterator();
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Return true if the database contains a template with the given name.
     *
     * @param orderXmlName Name of an order.xml template (without .xml).
     * @return True if such a template exists.
     * @throws ArgumentNotValid If the orderXmlName is null or an empty String
     */
    public synchronized boolean exists(String orderXmlName) {
        ArgumentNotValid.checkNotNullOrEmpty(orderXmlName, "String orderXmlName");

        Connection c = HarvestDBConnection.get();
        try {
            return exists(c, orderXmlName);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Return true if the database contains a template with the given name.
     *
     * @param orderXmlName Name of an order.xml template (without .xml).
     * @return True if such a template exists.
     * @throws ArgumentNotValid If the orderXmlName is null or an empty String
     */
    private synchronized boolean exists(Connection c, String orderXmlName) {
        int count = DBUtils.selectIntValue(c, "SELECT COUNT(*) FROM ordertemplates WHERE name = ?", orderXmlName);
        return count == 1;
    }

    /**
     * Create a template. The template must not already exist.
     *
     * @param orderXmlName Name of the template.
     * @param orderXml XML documents that is a Heritrix order.xml template.
     * @throws ArgumentNotValid If the orderXmlName is null or an empty String, or the orderXml is null.
     */
    public synchronized void create(String orderXmlName, HeritrixTemplate orderXml) {
        ArgumentNotValid.checkNotNullOrEmpty(orderXmlName, "String orderXmlName");
        ArgumentNotValid.checkNotNull(orderXml, "HeritrixTemplate orderXml");

        Connection c = HarvestDBConnection.get();
        PreparedStatement s = null;
        try {
            if (exists(c, orderXmlName)) {
                throw new PermissionDenied("An order template called " + orderXmlName + " already exists");
            }

            s = c.prepareStatement("INSERT INTO ordertemplates " + "( name, orderxml, isActive ) VALUES ( ?, ?, ? )");
            DBUtils.setStringMaxLength(s, 1, orderXmlName, Constants.MAX_NAME_SIZE, orderXmlName, "length");
            DBUtils.setClobMaxLength(s, 2, orderXml.getXML(), Constants.MAX_ORDERXML_SIZE, "size", orderXmlName);
            s.setBoolean(3, orderXml.isActive());
            s.executeUpdate();
        } catch (SQLException e) {
            throw new IOFailure("SQL error creating template " + orderXmlName + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    /**
     * Update a template. The template must already exist.
     *
     * @param orderXmlName Name of the template.
     * @param orderXml XML document that is a Heritrix order.xml template.
     * @throws PermissionDenied If the template does not exist
     * @throws IOFailure If the template could not be
     * @throws ArgumentNotValid If the orderXmlName is null or an empty String, or the orderXml is null.
     */
    public synchronized void update(String orderXmlName, HeritrixTemplate orderXml) {
        ArgumentNotValid.checkNotNullOrEmpty(orderXmlName, "String orderXmlName");
        ArgumentNotValid.checkNotNull(orderXml, "HeritrixTemplate orderXml");

        Connection c = HarvestDBConnection.get();
        PreparedStatement s = null;
        try {
            if (!exists(c, orderXmlName)) {
                throw new PermissionDenied("No order template called " + orderXmlName + " exists");
            }

            s = c.prepareStatement("UPDATE ordertemplates SET orderxml = ?, isActive= ? WHERE name = ?");
            DBUtils.setClobMaxLength(s, 1, orderXml.getXML(), Constants.MAX_ORDERXML_SIZE, "size", orderXmlName);
            s.setBoolean(2, orderXml.isActive());
            s.setString(3, orderXmlName);
            s.executeUpdate();
        } catch (SQLException e) {
            throw new IOFailure("SQL error updating template " + orderXmlName + "\n"
                    + ExceptionUtils.getSQLExceptionCause(e), e);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

}

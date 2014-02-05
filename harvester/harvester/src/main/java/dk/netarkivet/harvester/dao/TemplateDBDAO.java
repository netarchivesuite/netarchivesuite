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

package dk.netarkivet.harvester.dao;

import java.io.Reader;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.ResultSetExtractor;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.harvester.dao.spec.DBSpecifics;
import dk.netarkivet.harvester.datamodel.Constants;
import dk.netarkivet.harvester.datamodel.HeritrixTemplate;

/**
 * Implements the TemplateDAO with databases.
 *
 * The statements to create the tables are now in
 * scripts/sql/createfullhddb.sql
 */

public class TemplateDBDAO extends TemplateDAO {

	/**
	 * Read an XML order file for the named order XML.
	 *
	 * @param orderXmlName The name of the order.xml document
	 * @return The contents of this order.xml document
	 */
	public synchronized HeritrixTemplate read(final String orderXmlName) {
		ArgumentNotValid.checkNotNullOrEmpty(
				orderXmlName, "String orderXmlName");
		return query("SELECT orderxml FROM ordertemplates WHERE name=:name",
				new ParameterMap("name", orderXmlName),
				new ResultSetExtractor<HeritrixTemplate>() {
			@Override
			public HeritrixTemplate extractData(ResultSet rs)
					throws SQLException, DataAccessException {
				if (!rs.next()) {
					throw new UnknownID("Can't find template " + orderXmlName);
				}
				Reader orderTemplateReader = null;
				if (DBSpecifics.getInstance().supportsClob()) {
					Clob clob = rs.getClob(1);
					orderTemplateReader = clob.getCharacterStream();
				} else {
					orderTemplateReader = new StringReader(rs.getString(1));
				}
				SAXReader reader = new SAXReader();
				// TODO Check what happens on non-ascii
				Document orderXMLdoc;
				try {
					orderXMLdoc = reader.read(orderTemplateReader);
				} catch (DocumentException e) {
					throw new DataRetrievalFailureException(
							"Failed to parse order XML", e);
				}
				return new HeritrixTemplate(orderXMLdoc);
			}
		});
	}

	/**
	 * Returns an iterator with all names of order.xml-templates.
	 *
	 * @return Iterator<String> with all names of templates (without .xml).
	 */
	public synchronized Iterator<String> getAll() {
		return queryStringList("SELECT name FROM ordertemplates ORDER BY name").iterator();
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
		return 1 == queryIntValue(
				"SELECT COUNT(*) FROM ordertemplates WHERE name=:name",
				new ParameterMap("name", orderXmlName));
	}

	/** Create a template. The template must not already exist.
	 *
	 * @param orderXmlName Name of the template.
	 * @param orderXml XML documents that is a Heritrix order.xml template.
	 * @throws ArgumentNotValid If the orderXmlName is null or an empty String,
	 * or the orderXml is null.
	 */
	public synchronized void create(
			String orderXmlName,
			HeritrixTemplate orderXml) {
		ArgumentNotValid.checkNotNullOrEmpty(
				orderXmlName, "String orderXmlName");
		ArgumentNotValid.checkNotNull(orderXml, "HeritrixTemplate orderXml");

		if (exists(orderXmlName)) {
			throw new PermissionDenied("An order template called "
					+ orderXmlName + " already exists");
		}

		executeTransaction(
				"doCreate", 
				String.class, orderXmlName, 
				HeritrixTemplate.class, orderXml);
	}

	@SuppressWarnings("unused")
	private void doCreate(
			String orderXmlName,
			HeritrixTemplate orderXml) {
		executeUpdate("INSERT INTO ordertemplates (name, orderxml)"
				+ " VALUES (:name, :orderXml)",
				new ParameterMap(
						"name", getMaxLengthStringValue(
								orderXmlName, "name", orderXmlName, Constants.MAX_NAME_SIZE),
						"orderXml", getMaxLengthTextValue(
								orderXml, "orderXml", 
								orderXml.getXML(), Constants.MAX_ORDERXML_SIZE)));
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
	public synchronized void update(
			String orderXmlName,
			HeritrixTemplate orderXml) {
		ArgumentNotValid.checkNotNullOrEmpty(
				orderXmlName, "String orderXmlName");
		ArgumentNotValid.checkNotNull(orderXml, "HeritrixTemplate orderXml");

		if (!exists(orderXmlName)) {
			throw new PermissionDenied("No order template called "
					+ orderXmlName + " exists");
		}
		
		executeTransaction(
				"doUpdate",
				String.class, orderXmlName, 
				HeritrixTemplate.class, orderXml);
	}
	
	@SuppressWarnings("unused")
	private void doUpdate(
			String orderXmlName,
			HeritrixTemplate orderXml) {
		executeUpdate("UPDATE ordertemplates SET orderxml=:orderXMl WHERE name=:name",
				new ParameterMap(
						"name", orderXmlName,
						"orderXml", getMaxLengthTextValue(
								orderXml, "orderXml", 
								orderXml.getXML(), Constants.MAX_ORDERXML_SIZE)));
	}
	
}

/* File:        $Id: ExtendedFieldDBDAO.java 2824 2014-01-17 15:43:55Z aponb $Id$
 * Revision:    $Revision: 2824 $Revision$
 * Author:      $Author: aponb $Author$
 * Date:        $Date: 2014-01-17 16:43:55 +0100 (Fri, 17 Jan 2014) $Date$
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedField;

/**
 * A database-based implementation of the ExtendedFieldDBDAO class.
 */
public class ExtendedFieldDBDAO extends ExtendedFieldDAO {

	/** The logger for this class. */
	private final Log log = LogFactory.getLog(getClass());

	@Override
	protected Collection<HarvesterDatabaseTables> getRequiredTables() {
		ArrayList<HarvesterDatabaseTables> tables = new ArrayList<HarvesterDatabaseTables>();
		tables.add(HarvesterDatabaseTables.EXTENDEDFIELD);
		tables.add(HarvesterDatabaseTables.EXTENDEDFIELDVALUE);
		return tables;
	}

	@Override
	public synchronized void create(ExtendedField extendedField) {
		ArgumentNotValid.checkNotNull(extendedField, "aExtendedField");

		if (extendedField.getExtendedFieldID() != null) {
			log.warn("The extendedFieldID for this extended Field is "
					+ "already set. This should probably never happen.");
		} else {
			extendedField.setExtendedFieldID(generateNextID());
		}

		log.debug("Creating " + extendedField.toString());
		executeTransaction("doCreate", ExtendedField.class, extendedField);
	}

	@SuppressWarnings("unused")
	private void doCreate(ExtendedField extendedField) {
		executeUpdate("INSERT INTO extendedfield"
				+ " (extendedfield_id, extendedfieldtype_id, name, format, defaultvalue,"
				+ " options, datatype, mandatory, sequencenr, maxlen)"
				+ " VALUES (:id, :typeId, :name, :format, :defaultValue, :options"
				+ " , :dataType, :mandatory, :sequenceNr, :maxLen)",
				getParameterMap(extendedField));
	}

	/**
	 * Generates the next id of a extended field. this implementation retrieves
	 * the maximum value of extendedfield_id in the DB, and returns this value +
	 * 1.
	 * 
	 * @return The next available ID
	 */
	private synchronized Long generateNextID() {
		Long maxVal = queryLongValue("SELECT max(extendedfield_id) FROM extendedfield");
		if (maxVal == null) {
			maxVal = 0L;
		}
		return maxVal + 1L;
	}

	/**
	 * Check whether a particular extended Field exists.
	 * 
	 * @param extendedfieldId
	 *            Id of the extended field.
	 * @return true if the extended field exists.
	 */
	public boolean exists(Long extendedfieldId) {
		ArgumentNotValid.checkNotNull(extendedfieldId, "Long extendedfieldId");
		return 1 == queryLongValue(
				"SELECT COUNT(*) FROM extendedfield WHERE extendedfield_id=:id",
				new ParameterMap("id", extendedfieldId));
	}

	@Override
	public synchronized void update(ExtendedField extendedField) {
		ArgumentNotValid.checkNotNull(extendedField, "extendedField");

		final Long extendedfieldId = extendedField.getExtendedFieldID();
		if (!exists(extendedfieldId)) {
			throw new UnknownID("Extended Field id " + extendedfieldId
					+ " is not known in persistent storage");
		}

		executeTransaction("doUpdate", ExtendedField.class, extendedField);
	}

	@SuppressWarnings("unused")
	private void doUpdate(ExtendedField extendedField) {
		executeUpdate(
				"UPDATE extendedfield SET extendedfield_id=:id,"
						+ ", extendedfieldtype_id=:typeId"
						+ ", name=:name"
						+ ", format=:format"
						+ ", defaultvalue=:defaultValue"
						+ ", options=:options"
						+ ", datatype=:dataType"
						+ ", mandatory=:mandatory"
						+ ", sequencenr=:sequenceNr"
						+ ", maxlen=:maxLen"
						+ " WHERE extendedfield_id=:id",
						getParameterMap(extendedField));
	}

	@Override
	public synchronized ExtendedField read(final Long extendedfieldId) {
		ArgumentNotValid.checkNotNull(extendedfieldId, "aExtendedfieldId");
		if (!exists(extendedfieldId)) {
			throw new UnknownID("Extended Field id " + extendedfieldId
					+ " is not known in persistent storage");
		}

		return query("SELECT extendedfieldtype_id, name, format, defaultvalue, options, datatype"
				+ ", mandatory, sequencenr, maxlen"
				+ " FROM extendedfield WHERE extendedfield_id=:id",
				new ParameterMap("id", extendedfieldId),
				new ResultSetExtractor<ExtendedField>() {
					@Override
					public ExtendedField extractData(ResultSet rs)
							throws SQLException, DataAccessException {
						rs.next();
						return new ExtendedField(
								extendedfieldId,
								rs.getLong("extendedfieldtype_id"), 
								rs.getString("name"), 
								rs.getString("format"), 
								rs.getInt("datatype"), 
								rs.getInt("mandatory") != 0,
								rs.getInt("sequencenr"), 
								rs.getString("defaultvalue"), 
								rs.getString("options"), 
								rs.getInt("maxlen"));
					}
					
				});
	}
	
	@Override
	public synchronized List<ExtendedField> getAll(long extendedFieldTypeId) {
		List<Long> idList = queryLongList(
				"SELECT extendedfield_id FROM extendedfield WHERE extendedfieldtype_id=:typeId"
				+ " ORDER BY sequencenr ASC", 
				new ParameterMap("typeId", extendedFieldTypeId));
			
		List<ExtendedField> extendedFields = new LinkedList<ExtendedField>();
		for (Long extendedfieldId : idList) {
			extendedFields.add(read(extendedfieldId));
		}
		return extendedFields;
	}

	@Override
	public void delete(long extendedfieldId) {
		ArgumentNotValid.checkNotNull(extendedfieldId, "aExtendedfieldId");
		executeTransaction("doDelete", Long.class, extendedfieldId);
	}
	
	@SuppressWarnings("unused")
	private synchronized void doDelete(long extendedfieldId) {
		executeUpdate(
				"DELETE FROM extendedfieldvalue WHERE extendedfield_id=:id",
				new ParameterMap("id", extendedfieldId));
	}

	private ParameterMap getParameterMap(ExtendedField extendedField) {
		return new ParameterMap(
				"id", extendedField.getExtendedFieldID(),
				"typeId", extendedField.getExtendedFieldTypeID(),
				"name", extendedField.getName(),
				"format", extendedField.getFormattingPattern(),
				"defaultValue", extendedField.getDefaultValue(),
				"options", extendedField.getOptions(),
				"dataType", extendedField.getDatatype(),
				// the following conversion from boolean to int is necessary, 
				// because the database column 'mandatory' is a integer field 
				// and not a boolean (NAS-2127)
				"mandatory", (extendedField.isMandatory() ? 1 : 0),
				"sequenceNr", extendedField.getSequencenr(),
				"maxLen", extendedField.getMaxlen());

	}
	
}

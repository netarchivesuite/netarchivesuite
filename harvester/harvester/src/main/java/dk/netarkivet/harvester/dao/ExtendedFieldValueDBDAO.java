/* File:        $Id: ExtendedFieldValueDBDAO.java 2505 2012-09-26 14:49:21Z svc $Id$
 * Revision:    $Revision: 2505 $Revision$
 * Author:      $Author: svc $Author$
 * Date:        $Date: 2012-09-26 16:49:21 +0200 (Wed, 26 Sep 2012) $Date$
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValue;

/**
 * Implementation class for the ExtendedFieldValueDAO interface.
 */
public class ExtendedFieldValueDBDAO extends ExtendedFieldValueDAO {
	
    /** The logger. */    
    private final Log log = LogFactory.getLog(getClass());
    
    /**
     * Create a ExtendedFieldValue in persistent storage.
     * @param extendedFieldValue The ExtendedFieldValue to create in 
     *  persistent storage
     * @param commit Should we commit this or not
     */
    public void create(ExtendedFieldValue extendedFieldValue, boolean commit) {
        ArgumentNotValid.checkNotNull(extendedFieldValue, "extendedFieldValue");

        if (extendedFieldValue.getExtendedFieldValueID() != null) {
            log.warn("The extendedFieldValueID for this extendedField Value "
                    + "is already set. This should probably never happen.");
        } else {
            extendedFieldValue.setExtendedFieldValueID(generateNextID());
        }

        log.debug("Creating " + extendedFieldValue.toString());
        
        if (commit) {
        	executeTransaction("doCreate", ExtendedFieldValue.class, extendedFieldValue);
        } else {
        	doCreate(extendedFieldValue);
        }
    }
    
    private synchronized void doCreate(ExtendedFieldValue extendedFieldValue) {
        executeUpdate("INSERT INTO extendedfieldvalue ("
        		+ "extendedfieldvalue_id, extendedfield_id, content, instance_id"
        		+ ") VALUES (:id, :fieldId, :content, :instanceId)",
        		new ParameterMap(
        				"id", extendedFieldValue.getExtendedFieldValueID(),
        				"fieldId", extendedFieldValue.getExtendedFieldID(),
        				"content", extendedFieldValue.getContent(),
        				"instanceId", extendedFieldValue.getInstanceID()));
    }
    
    @Override
    public void create(ExtendedFieldValue extendedFieldValue) {
        create(extendedFieldValue, true);
    }

    /**  
     * @return the ID for next extendedvFieldValue inserted.
     */
    private synchronized Long generateNextID() {
        Long maxVal = queryLongValue(
                "SELECT max(extendedfieldvalue_id) FROM extendedfieldvalue");
        if (maxVal == null) {
            maxVal = 0L;
        }
        return maxVal + 1L;
    }

    @Override
    public void delete(long extendedfieldValueId) {
        ArgumentNotValid.checkNotNull(extendedfieldValueId, "extendedfieldValueId");
        executeTransaction("doDelete", Long.class, extendedfieldValueId);
    }
    
    @SuppressWarnings("unused")
	private void doDelete(final long extendedfieldValueID) {
        executeUpdate("DELETE FROM extendedfieldvalue WHERE extendedfieldvalue_id=:id",
        		new ParameterMap("id", extendedfieldValueID));
    }

    @Override
    public boolean exists(Long extendedFieldValueId) {
        ArgumentNotValid.checkNotNull(extendedFieldValueId,
                "Long extendedFieldValueId");
        return 1 == queryLongValue(
                        "SELECT COUNT(*) FROM extendedfieldvalue "
                        + "WHERE extendedfieldvalue_id=:id",
                        new ParameterMap("id", extendedFieldValueId));
    }

    @Override
    public synchronized ExtendedFieldValue read(
    		final Long extendedFieldId,
    		final Long instanceId) {
        ArgumentNotValid.checkNotNull(extendedFieldId, "extendedFieldId");
        ArgumentNotValid.checkNotNull(instanceId, "instanceId");
        return query("SELECT extendedfieldvalue_id, extendedfield_id, content"
        		+ " FROM extendedfieldvalue WHERE extendedfield_id=:fieldId"
        		+ " AND instance_id=:instId",
        		new ParameterMap(
        				"fieldId", extendedFieldId,
        				"instId", instanceId),
        		new ResultSetExtractor<ExtendedFieldValue>() {
					@Override
					public ExtendedFieldValue extractData(ResultSet rs)
							throws SQLException, DataAccessException {
						if (!rs.next()) {
			                return null;
			            }
			            return new ExtendedFieldValue(
			            		rs.getLong("extendedfieldvalue_id"),
			                    rs.getLong("extendedfield_id"), 
			                    instanceId, 
			                    rs.getString("content"));
					}        			
				});
    }
    
    /**
     * Read a ExtendedFieldValue in persistent storage.
     * @param extendedFieldValue The ExtendedFieldValue to update
     * @param commit Should we commit this or not
     * @throws SQLException In case of database problems.
     */
    public void update(ExtendedFieldValue extendedFieldValue, boolean commit) {
        final Long extendedfieldvalueId = extendedFieldValue.getExtendedFieldID();
        if (!exists(extendedfieldvalueId)) {
            throw new UnknownID("Extended Field Value id "
                    + extendedfieldvalueId
                    + " is not known in persistent storage");
        }
        
        if (commit) {
        	executeTransaction("doUpdate", ExtendedFieldValue.class, extendedFieldValue);
        } else {
        	doUpdate(extendedFieldValue);
        }
    }
    
    private synchronized void doUpdate(ExtendedFieldValue extendedFieldValue) {
        executeUpdate("UPDATE extendedfieldvalue"
        		+ " SET extendedfield_id =:fieldId, instance_id=:instId, content=:content"
        		+ " WHERE extendedfieldvalue_id=:id"
        		+ " AND instance_id=:instId",
        		new ParameterMap(
        				"fieldId", extendedFieldValue.getExtendedFieldID(),
        				"instId", extendedFieldValue.getInstanceID(),
        				"content", extendedFieldValue.getContent(),
        				"id", extendedFieldValue.getExtendedFieldValueID()));
    }

    @Override
    public void update(ExtendedFieldValue extendedFieldValue) {
    	update(extendedFieldValue, true);
    }

}

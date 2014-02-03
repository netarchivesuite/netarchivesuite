/* File:        $Id: ExtendedFieldTypeDBDAO.java 2505 2012-09-26 14:49:21Z svc $Id$
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldType;

/**
 * Implementation of the ExtendedFieldTypeDAO interface
 * for creating and accessing extended fields in persistent storage.
 */
public class ExtendedFieldTypeDBDAO extends ExtendedFieldTypeDAO {
    
    @Override
	protected Collection<HarvesterDatabaseTables> getRequiredTables() {
		ArrayList<HarvesterDatabaseTables> tables = new ArrayList<HarvesterDatabaseTables>();
		tables.add(HarvesterDatabaseTables.EXTENDEDFIELDTYPE);
		tables.add(HarvesterDatabaseTables.EXTENDEDFIELD);
		tables.add(HarvesterDatabaseTables.EXTENDEDFIELDVALUE);
		return tables;
	}

    @Override
    public boolean exists(Long extendedfieldtypeId) {
        ArgumentNotValid.checkNotNull(extendedfieldtypeId,
                "Long aExtendedfieldtypeId");
        return 1 == queryLongValue(
        		"SELECT COUNT(*) FROM extendedfieldtype WHERE extendedfieldtype_id=:id",
        		new ParameterMap("id", extendedfieldtypeId));
    }

    @Override
    public synchronized ExtendedFieldType read(final Long extendedfieldtypeId) {
        ArgumentNotValid.checkNotNull(
        		extendedfieldtypeId,
                "aExtendedfieldtypeId");
        
        if (!exists(extendedfieldtypeId)) {
            throw new UnknownID("Extended FieldType id " + extendedfieldtypeId
                    + " is not known in persistent storage");
        }

        return query(
        		"SELECT name FROM extendedfieldtype WHERE extendedfieldtype_id=:id",
        		new ParameterMap("id", extendedfieldtypeId),
        		new ResultSetExtractor<ExtendedFieldType>() {
					@Override
					public ExtendedFieldType extractData(ResultSet rs)
							throws SQLException, DataAccessException {
						rs.next();
						return new ExtendedFieldType(
								extendedfieldtypeId,
			                    rs.getString("name"));
					}        			
				});
    }

    @Override
    public synchronized List<ExtendedFieldType> getAll() {
        List<Long> idList = queryLongList("SELECT extendedfieldtype_id FROM extendedfieldtype");
        List<ExtendedFieldType> extendedFieldTypes = new LinkedList<ExtendedFieldType>();
        for (Long extendedfieldtypeId : idList) {
        	extendedFieldTypes.add(read(extendedfieldtypeId));
        }
        return extendedFieldTypes;
    }
}

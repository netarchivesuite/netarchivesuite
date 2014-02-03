/* File:    $Id: DerbySpecificsTester.java 2467 2012-08-22 10:59:30Z mss $
 * Version: $Revision: 2467 $
 * Date:    $Date: 2012-08-22 12:59:30 +0200 (Wed, 22 Aug 2012) $
 * Author:  $Author: mss $
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
package dk.netarkivet.harvester.dao.spec;

import java.sql.SQLException;

import org.springframework.dao.DataAccessException;

import dk.netarkivet.harvester.dao.DataModelTestDao;
import dk.netarkivet.harvester.dao.ParameterMap;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;

/**
 *
 * Unit test testing the DerbySpecifics class.
 *
 */
public class DerbySpecificsTester extends DataModelTestCase {
    public DerbySpecificsTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    /**
     * Test added to fool JUnit.
     */
    public void testDummy() {
        
    }



    /**
     * FIXME Broken by connection code refactoring 
     * https://sbforge.org/jira/browse/NAS-1924
     * Prefixed with "failing" to disable test.
     */
    public void failingtestGetTemporaryTable() throws SQLException {
    	DataModelTestDao dao = DataModelTestDao.getInstance();
    	try {
    		dao.testQuery("SELECT config_name, domain_name FROM session.jobconfignames");
    		fail("Should have failed query before table is made");
        } catch (DataAccessException e) {
            // expected
        }

    	try {
    		String tmpTable =
    				DBSpecifics.getInstance().getJobConfigsTmpTable();
    		assertEquals("Should have given expected name for Derby temp table",
    				"session.jobconfignames", tmpTable);
    		dao.executeUpdate("INSERT INTO " + tmpTable + " VALUES (:foo, :bar)",
    				new ParameterMap("foo", "foo", "bar", "bar"));
    		
    		String domain = dao.queryStringValue(
    				"SELECT domain_name FROM " + tmpTable
    				+ " WHERE config_name=:name", 
    				new ParameterMap("name", "bar"));
    		assertEquals("Should get expected domain name", "foo", domain);
    		DBSpecifics.getInstance().dropJobConfigsTmpTable(tmpTable);
    	} catch (DataAccessException e) {
    		fail("Should not have had SQL exception " + e);
    	}
    	
    	String tmpTable = DBSpecifics.getInstance().getJobConfigsTmpTable();
    	assertEquals("Should have given expected name for Derby temp table",
    			"session.jobconfignames", tmpTable);
    	
    	dao.executeUpdate("INSERT INTO " + tmpTable + " VALUES (:foo, :bar)",
				new ParameterMap("foo", "foo", "bar", "bar"));
    	String domain = dao.queryStringValue(
    					"SELECT domain_name FROM "
    							+ tmpTable
    							+ " WHERE config_name=:name",
    							new ParameterMap("name", "bar"));
    	assertEquals("Should get expected domain name", "foo", domain);
    }
}
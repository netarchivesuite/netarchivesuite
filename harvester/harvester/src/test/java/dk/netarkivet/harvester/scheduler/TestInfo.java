/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.harvester.scheduler;

import java.io.File;
import java.sql.SQLException;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.Job;

/**
 * Contains test information about all scheduler test data.
 *
 */
public class TestInfo {

    /**
     * The properties-file containing properties for logging in unit-tests.
     */
    static final File TESTLOGPROP = new File(
            "tests/dk/netarkivet/testlog.prop");

    static final File BASEDIR = new File(
            "tests/dk/netarkivet/harvester/scheduler/data");
    static final File ORIGINALS_DIR = new File(BASEDIR, "originals");
    static final File WORKING_DIR = new File(BASEDIR, "working");

    static File ORDER_FILE = new File(TestInfo.WORKING_DIR, "order.xml");
    public static final File LOG_FILE = new File(new File("tests/testlogs"), 
            "netarkivtest.log");
    
    static final File orderTemplatesOriginalsDir 
        = new File(
                "tests/dk/netarkivet/"
                + "/harvester/data/originals/order_templates/");
    
    public TestInfo() {
    }

    /**
     * Get a simple job.
     * @return Job
     */
    static Job getJob() {
        // This job doesn't get an ID here, because we want to see what happens
        // with an ID-less job, too.
        try {
            DataModelTestCase.addHarvestDefinitionToDatabaseWithId(0L);
        } catch (SQLException e) {
            throw new IOFailure(e.getMessage());
        }
        return Job.createJob(0L, new HarvestChannel("test", false, true, ""),
                DomainDAO.getInstance()
                .read("netarkivet.dk")
                .getConfiguration("Engelsk_netarkiv_et_niveau"), 0);
    }
    
}



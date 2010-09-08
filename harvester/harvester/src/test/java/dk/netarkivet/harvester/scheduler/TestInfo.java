/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

import org.dom4j.Document;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobPriority;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.testutils.ReflectUtils;

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
        return Job.createJob(0L,
                DomainDAO.getInstance()
                .read("netarkivet.dk")
                .getConfiguration("Engelsk_netarkiv_et_niveau"), 0);
    }
    

    /** Get a simple job with low priority
     * @return a simple job with low priority
     */
    static Job getJobLowPriority() {
        try {
            Constructor<Job> c = ReflectUtils.getPrivateConstructor(
                    Job.class, Long.class, Map.class, JobPriority.class, Long.TYPE,
                    Long.TYPE, JobStatus.class, String.class, Document.class,
                    String.class, Integer.TYPE);
            return c.newInstance(42L, Collections.<String, String>emptyMap(),
                                 JobPriority.LOWPRIORITY, -1L, -1L,
                                 JobStatus.NEW, "default_template",
                                 XmlUtils.getXmlDoc(ORDER_FILE),
                                 "www.netarkivet.dk", 1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}



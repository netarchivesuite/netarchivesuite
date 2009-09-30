/*$Id$
* $Revision$
* $Date$
* $Author$
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import org.dom4j.Document;
import org.dom4j.Node;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.DBUtils;

/**
 * Unittests for the class dk.netarkivet.harvester.datamodel.DomainDBDAO.
 * 
 */
public class DomainDBDAOTester extends DataModelTestCase {
    public DomainDBDAOTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    /** Test that a bad update doesn't kill the DB.
     * @throws Exception
     */
    public void testBadUpdate() throws Exception {
        DomainDAO dao = DomainDAO.getInstance();
        Domain d = dao.read("netarkivet.dk");
        d.setEdition(-3L);
        try {
            dao.update(d);
            fail("Should fail with wrong edition");
        } catch (PermissionDenied e) {
            //expected
        }

        // If the savepoint is not released after rollback, this will fail.
        d = dao.read("netarkivet.dk");
        d.setComments("New comment");
        dao.update(d);
    }

    public void testIteratorLocking() {
        DomainDAO dao = DomainDAO.getInstance();
        assertEquals("Should have 4 domains", 4, dao.getCountDomains());
        Iterator<Domain> domains = dao.getAllDomains();

        domains.hasNext();
        Domain d1 = domains.next();
        d1.setComments("Should be updatable");
        dao.update(d1);

        Domain d2 = dao.read("netarkivet.dk");
        d2.setComments("Should also be updatable");
        dao.update(d2);
    }

    /** This stresstests the DB DAOs by running several updates in parallel. */
    public void testMultipleSavepoints() {
        // Enforce possible database migration
        DomainDAO dummy = DomainDAO.getInstance();
        assertNotNull("dummy should not be null", dummy);
        final int maxLoop = 300;
        // Make four threads doing updates in the daos
        final int[] done = new int[1];
        final Throwable[] scheduleException = new Throwable[1];
        Thread schedThread = new Thread() {
            public void run() {
                try {
                    ScheduleDAO dao = ScheduleDAO.getInstance();
                    RepeatingSchedule s = (RepeatingSchedule)Schedule.getInstance(
                            null, 1, new HourlyFrequency(1), "foo", "bar");
                    dao.create(s);
                    for (int i = 0; i < maxLoop; i++) {
                        s.setComments("bar" + i);
                        dao.update(s);
                        Thread.yield();
                    }
                } catch (Throwable e) {
                    scheduleException[0] = e;
                } finally {
                    synchronized(done) {
                        done[0]++;
                    }
                }
            }
        };
        final Throwable[] hdException =  new Throwable[1];
        Thread hdThread = new Thread() {
            public void run() {
                try {
                    HarvestDefinitionDAO dao = HarvestDefinitionDAO.getInstance();
                    DomainDAO ddao = DomainDAO.getInstance();
                    Domain d = ddao.read("netarkivet.dk");
                    d.getDefaultConfiguration();
                    List<DomainConfiguration> dcs = new ArrayList<DomainConfiguration>(1);
                    final Schedule schedule = Schedule.getInstance(
                            null, 1, new HourlyFrequency(1), "arg", "splat");
                    ScheduleDAO sDao = ScheduleDAO.getInstance();
                    sDao.create(schedule);
                    PartialHarvest ph = HarvestDefinition.createPartialHarvest(
                            dcs, schedule, "testme", "here");
                    dao.create(ph);
                    for (int i = 0; i < maxLoop; i++) {
                        ph.setComments("foo" + i);
                        dao.update(ph);
                        Thread.yield();
                    }
                } catch (Throwable e) {
                    hdException[0] = e;
                } finally {
                    synchronized(done) {
                        done[0]++;
                    }
                }
            }
        };
        final Throwable[] domainException = new Throwable[1];
        Thread domainThread = new Thread() {
            public void run() {
                try {
                    DomainDAO dao = DomainDAO.getInstance();
                    Domain d = dao.read("netarkivet.dk");
                    for (int i = 0; i < maxLoop; i++) {
                        d.setComments("qux" + i);
                        dao.update(d);
                        Thread.yield();
                    }
                } catch (Throwable e) {
                    domainException[0] = e;
                } finally {
                    synchronized(done) {
                        done[0]++;
                    }
                }
            }
        };
        hdThread.start();
        schedThread.start();
        domainThread.start();
        while (done[0] < 3) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                // Not significant
            }
        }
        if (scheduleException[0] != null) {
            System.out.println("Schedule: " 
                    + scheduleException[0]
                    + scheduleException[0].getStackTrace());
        }
        if (hdException[0] != null) {
            System.out.println("HarvestDefinition: "
                    + hdException[0]
                    + hdException[0].getStackTrace());
        }
        if (domainException[0] != null) {
            System.out.println("Domain: "
                    + domainException[0] 
                    + domainException[0].getStackTrace());
        }
        
        assertTrue("Should have no exceptions",
                   domainException[0] == null &&
                   hdException[0] == null &&
                   scheduleException[0] == null);
        }

    /** Test that non-ascii chars are correctly stored in clobs. */
    public void testNonAsciiClob() {
        //  Simplest clob: seedlist
        SeedList sl = new SeedList("nonascii", "æåø.dk\n");
        DomainDAO dao = DomainDAO.getInstance();
        Domain d = dao.read(TestInfo.EXISTINGDOMAINNAME);
        d.addSeedList(sl);
        dao.update(d);
        Domain d2 = dao.read(TestInfo.EXISTINGDOMAINNAME);
        assertTrue("Seed lists should be the different objects",
                   sl != d2.getSeedList("nonascii"));
        assertEquals("Seeds in seed lists should be the same",
                     d.getSeedList("nonascii").getSeedsAsString(),
                     d2.getSeedList("nonascii").getSeedsAsString());
        assertEquals("Seeds in seed list should be same as original",
                     sl.getSeedsAsString(),
                     d2.getSeedList("nonascii").getSeedsAsString());

        TemplateDAO tdao = TemplateDAO.getInstance();
        Document template = tdao.read(TestInfo.ORDER_XML_NAME).getTemplate();
        Node filterMapNode
                = template.selectSingleNode(HeritrixTemplate.DECIDERULES_MAP_XPATH);
        filterMapNode.setText("åæø");
        tdao.update(TestInfo.ORDER_XML_NAME, new HeritrixTemplate(template));
        Document template2 = tdao.read(TestInfo.ORDER_XML_NAME).getTemplate();
        assertEquals("Template documents should be the same",
                     template.asXML(), template2.asXML());
    }

    /**
     * Test method for testing DomainDBDAO.getAliases().
     */
    public void testGetAliases() throws IllegalAccessException,
            NoSuchFieldException, InterruptedException {

        // create some domains, and make some alias relations between them.
        dk.netarkivet.harvester.webinterface.DomainDefinition.createDomains(
                "aliasdomainone.dk", "aliasdomaintwo.dk", "aliasdomainthree.dk",
                "aliasdomainfour.dk", "aliasdomainfive.dk");

        dk.netarkivet.harvester.webinterface.DomainDefinition.createDomains(
                "test.dk", "firmafjersyn.dk", "firmanyt.dk", "pligtaflevering.dk",
                "deff.dk");

        DomainDAO dao = DomainDAO.getInstance();
        Domain d1 = dao.read("aliasdomainone.dk");
        d1.updateAlias("test.dk");
        dao.update(d1);

        Thread.sleep(2);

        Domain d3 = dao.read("aliasdomainthree.dk");
        d3.updateAlias("firmanyt.dk");
        dao.update(d3);

        Thread.sleep(2);

        Domain d5 = dao.read("aliasdomainfive.dk");
        d5.updateAlias("deff.dk");
        dao.update(d5);

        Thread.sleep(2);

        Domain d2 = dao.read("aliasdomaintwo.dk");
        d2.updateAlias("test.dk");
        dao.update(d2);

        Thread.sleep(2);

        Domain d4 = dao.read("aliasdomainfour.dk");
        d4.updateAlias("pligtaflevering.dk");
        dao.update(d4);

        Thread.sleep(2);

        d2 = dao.read("aliasdomaintwo.dk");
        d2.updateAlias("test.dk");
        dao.update(d2);

        //  This should return a list with length 2
        List<AliasInfo> aliasInfoList = dao.getAliases("test.dk");
        assertEquals("Too few or too many AliasInfo objects returned",
                2, aliasInfoList.size());

        aliasInfoList = dao.getAllAliases();
        // This should return a list with length 5
        assertEquals("Wrong number of AliasInfo objects returned",
                5, aliasInfoList.size());

        assertEquals("Should get earliest first",
                "aliasdomainone.dk", aliasInfoList.get(0).getDomain());

        assertEquals("Should get right order",
                "aliasdomainthree.dk", aliasInfoList.get(1).getDomain());

        assertEquals("Should get right order",
                "aliasdomainfive.dk", aliasInfoList.get(2).getDomain());

        assertEquals("Should get right order",
                "aliasdomainfour.dk", aliasInfoList.get(3).getDomain());

        assertEquals("Should get earliest first",
                "aliasdomaintwo.dk", aliasInfoList.get(4).getDomain());

        /** Test for bug #746: Don't use DomainDAO.read() to get aliases */
        DomainDAOTester.setDomainDAO(new DomainDBDAO() {
            public Domain read(String name) {
                TestCase.fail("Should not read any domains, but asked for " + name);
                return null;
            }
        });
        dao = DomainDAO.getInstance();
        aliasInfoList = dao.getAliases("test.dk");
        assertEquals("Too few or too many AliasInfo objects returned", 2, aliasInfoList.size());
    }

    public void testGetTLDs() {
        //  create some domains.
        dk.netarkivet.harvester.webinterface.DomainDefinition.createDomains(
                "venstre.dk", "venstre.nu", "sy-jonna.dk", "one.com", "two.com",
                "one.dk", "two.net", "1.2.3.4", "3.63.102.33",  "2.3.4.3");
        DomainDAO dao = DomainDAO.getInstance();
        List<TLDInfo> result = dao.getTLDs();
        assertEquals("Expected 5 TLDs", 5, result.size());
        assertEquals("Should have two subdomains of .com",
                2, result.get(1).getCount());
        assertEquals("Should have three IP subdomains",
                3, result.get(0).getCount());
    }

    public void testGetCountDomains() {
        // create some domains, ignore invalid domains
        dk.netarkivet.harvester.webinterface.DomainDefinition.createDomains(
                "venstre.dk", "todo.dk", "venstre.nu", "sy-jonna.dk", "one.com",
                "two.com", "one.net", "two.net");
        DomainDAO dao = DomainDAO.getInstance();
        List<TLDInfo> result = dao.getTLDs();
        assertEquals("Too few or too many TLDs found", 4, result.size());
        assertEquals("Found too few or too many .dk domains",
                7, dao.getCountDomains("*.dk"));
        assertEquals("Found too few or too many .nu domains",
                1, dao.getCountDomains("*.nu"));
        assertEquals("Found too few or too many .net domains",
                2, dao.getCountDomains("*.net"));
        assertEquals("Found too few or too many .com domains",
                2, dao.getCountDomains("*.com"));
    }
    
    /**
     *  Unittest for method getDomainJobInfo.
     */
    public void testGetDomainJobInfo() {
        DomainDAO dao = DomainDAO.getInstance();
        HarvestDefinitionDAO hdDao = HarvestDefinitionDAO.getInstance();
        HarvestDefinition hd = hdDao.read(new Long(42));
        hd.createJobs();
        JobDAO jdao = JobDBDAO.getInstance();
        Job j = jdao.getAll().next();
        Map<String,String> dcMap  = j.getDomainConfigurationMap();
        String theDomainName = "netarkivet.dk";
        String configName = dcMap.get(theDomainName);
        HarvestInfo hi = dao.getDomainJobInfo(j, theDomainName, configName);
        // Before "running" the job a null value should be returned.
        assertNull("The HarvestInfo value should be null, "
                + "when job has not been completed", hi);
        // Fake that the job has been run by inserting a historyInfo
        // entry in the database for this job_id, config_id, harvest_id combination.
        HarvestInfo hi1 = new HarvestInfo(j.getOrigHarvestDefinitionID(),
                j.getJobID(),
                theDomainName,
                configName,
                new Date(),
                10000L,
                64L,
                StopReason.OBJECT_LIMIT);
        long domainId = DBUtils.selectLongValue(
                DBConnect.getDBConnection(),
                "SELECT domain_id FROM domains WHERE name=?", theDomainName);
        long configId = DBUtils.selectLongValue(
                DBConnect.getDBConnection(),
                "SELECT config_id FROM configurations WHERE name = ? AND domain_id=?",
                configName, domainId);
        
        insertHarvestInfo(hi1, configId);
        HarvestInfo hi2 = dao.getDomainJobInfo(j, theDomainName, configName); 
        assertTrue("The HarvestInfo value should not be null, "
                + "when job has been completed", hi2 != null);
        
        assertEquals("StopReason is wrong", hi1.getStopReason(), hi2.getStopReason());
        assertEquals("Bytes harvested is wrong", hi1.getSizeDataRetrieved(), 
                hi2.getSizeDataRetrieved());
        assertEquals("Objects fetched is wrong", hi1.getCountObjectRetrieved(), 
                hi2.getCountObjectRetrieved());
    }
    
    // Copied from DomainDBDAO for local testing
    private void insertHarvestInfo(HarvestInfo harvestInfo, long configId) {
        Connection c = DBConnect.getDBConnection();
        PreparedStatement s = null;
        try {
            // Note that the config_id is grabbed from the configurations table.
            s = c.prepareStatement("INSERT INTO historyinfo "
                    + "( stopreason, objectcount, bytecount, config_id, job_id, "
                    + "harvest_id, harvest_time ) "
                    + "VALUES ( ?, ?, ?, ?, ?, ?, ? )",
                    Statement.RETURN_GENERATED_KEYS);
            s.setInt(1, harvestInfo.getStopReason().ordinal());
            s.setLong(2, harvestInfo.getCountObjectRetrieved());
            s.setLong(3, harvestInfo.getSizeDataRetrieved());
            s.setLong(4, configId);
            if (harvestInfo.getJobID() != null) {
                s.setLong(5, harvestInfo.getJobID());
            } else {
                s.setNull(5, Types.BIGINT);
            }
            s.setLong(6, harvestInfo.getHarvestID());
            s.setTimestamp(7, new Timestamp(harvestInfo.getDate().getTime()));
            s.executeUpdate();
            harvestInfo.setID(DBUtils.getGeneratedID(s));
        } catch (SQLException e) {
            throw new IOFailure("SQL error while inserting harvest info", e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }
}
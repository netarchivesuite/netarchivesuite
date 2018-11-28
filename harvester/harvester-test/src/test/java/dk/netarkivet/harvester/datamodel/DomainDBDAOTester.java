/*
 * #%L
 * Netarchivesuite - harvester - test
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.SlowTest;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedField;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDBDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldTypes;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValue;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValueDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValueDBDAO;
import dk.netarkivet.harvester.scheduler.jobgen.DefaultJobGenerator;
import dk.netarkivet.harvester.webinterface.ExtendedFieldConstants;

/**
 * Unit tests for the class dk.netarkivet.harvester.datamodel.DomainDBDAO.
 */
public class DomainDBDAOTester extends DataModelTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Unittest for extended Fields.
     */
    @Category(SlowTest.class)
    @Test
    public void testExtendedFields() {
        ExtendedFieldDAO extDAO = ExtendedFieldDBDAO.getInstance();
        ExtendedField extField = new ExtendedField(null, (long) ExtendedFieldTypes.DOMAIN, "Test", "12345", 1, true, 1,
                "defaultvalue", "", ExtendedFieldConstants.MAXLEN_EXTF_NAME);
        extDAO.create(extField);

        ExtendedFieldDAO extDAO2 = ExtendedFieldDBDAO.getInstance();
        extField = extDAO2.read(Long.valueOf(1));

        assertEquals(1, extField.getExtendedFieldID().longValue());

        Domain d = Domain.getDefaultDomain(TestInfo.DOMAIN_NAME);

        DomainDAO dao = DomainDAO.getInstance();
        dao.create(d);

        assertEquals("defaultvalue", d.getExtendedFieldValue(Long.valueOf(1)).getContent());

        ExtendedFieldValueDAO efvDAO = ExtendedFieldValueDBDAO.getInstance();
        ExtendedFieldValue efv = efvDAO.read(extField.getExtendedFieldID(), d.getID());

        assertEquals(Long.valueOf(1), efv.getExtendedFieldValueID());
        assertEquals("defaultvalue", efv.getContent());
        assertEquals(Long.valueOf(1), efv.getExtendedFieldID());

        extField = new ExtendedField(null, (long) ExtendedFieldTypes.DOMAIN, "Test2", "12345", 1, true, 2,
                "defaultvalue2", "", ExtendedFieldConstants.MAXLEN_EXTF_BOOLEAN);
        extDAO.create(extField);

        d = dao.read(TestInfo.DOMAIN_NAME);
        List<ExtendedFieldValue> list = d.getExtendedFieldValues();

        assertEquals(2, list.size());
        efv = list.get(0);
        assertEquals(Long.valueOf(1), efv.getExtendedFieldID());
        assertEquals("defaultvalue", efv.getContent());

        efv = list.get(1);
        assertEquals(Long.valueOf(2), efv.getExtendedFieldID());
        assertEquals("defaultvalue2", efv.getContent());
    }

    /**
     * Test that a bad update doesn't kill the DB.
     *
     * @throws Exception
     */
    @Category(SlowTest.class)
    @Test
    public void testBadUpdate() throws Exception {
        DomainDAO dao = DomainDAO.getInstance();
        Domain d = dao.read("netarkivet.dk");
        d.setEdition(-3L);
        try {
            dao.update(d);
            fail("Should fail with wrong edition");
        } catch (PermissionDenied e) {}

        // If the savepoint is not released after rollback, this will fail.
        d = dao.read("netarkivet.dk");
        d.setComments("New comment");
        dao.update(d);
    }

    @Category(SlowTest.class)
    @Test
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
    // Failing: Causes all subsequent test using the database to fail.
    @Category(SlowTest.class)
    @Test
    @Ignore("Cause all subsequent tests using the database to fail")
    public void failingTestMultipleSavepoints() {
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
                    RepeatingSchedule s = (RepeatingSchedule) Schedule.getInstance(null, 1, new HourlyFrequency(1),
                            "foo", "bar");
                    dao.create(s);
                    for (int i = 0; i < maxLoop; i++) {
                        s.setComments("bar" + i);
                        dao.update(s);
                        Thread.yield();
                    }
                } catch (Throwable e) {
                    scheduleException[0] = e;
                } finally {
                    synchronized (done) {
                        done[0]++;
                    }
                }
            }
        };
        final Throwable[] hdException = new Throwable[1];
        Thread hdThread = new Thread() {
            public void run() {
                try {
                    HarvestDefinitionDAO dao = HarvestDefinitionDAO.getInstance();
                    DomainDAO ddao = DomainDAO.getInstance();
                    Domain d = ddao.read("netarkivet.dk");
                    d.getDefaultConfiguration();
                    List<DomainConfiguration> dcs = new ArrayList<DomainConfiguration>(1);
                    final Schedule schedule = Schedule.getInstance(null, 1, new HourlyFrequency(1), "arg", "splat");
                    ScheduleDAO sDao = ScheduleDAO.getInstance();
                    sDao.create(schedule);
                    PartialHarvest ph = HarvestDefinition.createPartialHarvest(dcs, schedule, "testme", "here",
                            "Everybody");
                    dao.create(ph);
                    for (int i = 0; i < maxLoop; i++) {
                        ph.setComments("foo" + i);
                        dao.update(ph);
                        Thread.yield();
                    }
                } catch (Throwable e) {
                    hdException[0] = e;
                } finally {
                    synchronized (done) {
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
                    synchronized (done) {
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
            } catch (InterruptedException e) {}
        }
        if (scheduleException[0] != null) {
            System.out.println("Schedule: " + scheduleException[0] + scheduleException[0].getStackTrace());
        }
        if (hdException[0] != null) {
            System.out.println("HarvestDefinition: " + hdException[0] + hdException[0].getStackTrace());
        }
        if (domainException[0] != null) {
            System.out.println("Domain: " + domainException[0] + domainException[0].getStackTrace());
        }

        assertTrue("Should have no exceptions", domainException[0] == null && hdException[0] == null
                && scheduleException[0] == null);
    }

    /** Test that non-ascii chars are correctly stored in clobs. */
    @Category(SlowTest.class)
    @Test
    public void testNonAsciiClob() {
        // Simplest clob: seedlist
        SeedList sl = new SeedList("nonascii", "æåø.dk\n");
        DomainDAO dao = DomainDAO.getInstance();
        Domain d = dao.read(TestInfo.EXISTINGDOMAINNAME);
        d.addSeedList(sl);
        dao.update(d);
        Domain d2 = dao.read(TestInfo.EXISTINGDOMAINNAME);
        assertTrue("Seed lists should be the different objects", sl != d2.getSeedList("nonascii"));
        assertEquals("Seeds in seed lists should be the same", d.getSeedList("nonascii").getSeedsAsString(), d2
                .getSeedList("nonascii").getSeedsAsString());
        assertEquals("Seeds in seed list should be same as original", sl.getSeedsAsString(), d2.getSeedList("nonascii")
                .getSeedsAsString());

        TemplateDAO tdao = TemplateDAO.getInstance();
        /*
         FIXME Probably the easiest to support this is to  make a dummy HeritrixTemplate that tests this
         issue
         
        Document template = tdao.read(TestInfo.ORDER_XML_NAME).getTemplate();
        Node filterMapNode = template.selectSingleNode(H1HeritrixTemplate.DECIDERULES_MAP_XPATH);
        filterMapNode.setText("åæø");
        tdao.update(TestInfo.ORDER_XML_NAME, new H1HeritrixTemplate(template));
        Document template2 = tdao.read(TestInfo.ORDER_XML_NAME).getTemplate();
        assertEquals("Template documents should be the same", template.asXML(), template2.asXML());
        */
    }

    /**
     * Test method for testing DomainDBDAO.getAliases().
     */
    @Category(SlowTest.class)
    @Test
    public void testGetAliases() throws IllegalAccessException, NoSuchFieldException, InterruptedException {

        // create some domains, and make some alias relations between them.
        dk.netarkivet.harvester.webinterface.DomainDefinition.createDomains("aliasdomainone.dk", "aliasdomaintwo.dk",
                "aliasdomainthree.dk", "aliasdomainfour.dk", "aliasdomainfive.dk");

        dk.netarkivet.harvester.webinterface.DomainDefinition.createDomains("test.dk", "firmafjersyn.dk",
                "firmanyt.dk", "pligtaflevering.dk", "deff.dk");

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

        // This should return a list with length 2
        List<AliasInfo> aliasInfoList = dao.getAliases("test.dk");
        assertEquals("Too few or too many AliasInfo objects returned", 2, aliasInfoList.size());

        aliasInfoList = dao.getAllAliases();
        // This should return a list with length 5
        assertEquals("Wrong number of AliasInfo objects returned", 5, aliasInfoList.size());

        assertEquals("Should get earliest first", "aliasdomainone.dk", aliasInfoList.get(0).getDomain());

        assertEquals("Should get right order", "aliasdomainthree.dk", aliasInfoList.get(1).getDomain());

        assertEquals("Should get right order", "aliasdomainfive.dk", aliasInfoList.get(2).getDomain());

        assertEquals("Should get right order", "aliasdomainfour.dk", aliasInfoList.get(3).getDomain());

        assertEquals("Should get earliest first", "aliasdomaintwo.dk", aliasInfoList.get(4).getDomain());

        /** Test for bug #746: Don't use DomainDAO.read() to get aliases */
        DomainDAOTester.setDomainDAO(new DomainDBDAO() {
            public Domain read(String name) {
                fail("Should not read any domains, but asked for " + name);
                return null; // to placate Eclipse static analysis.
            }
        });
        dao = DomainDAO.getInstance();
        aliasInfoList = dao.getAliases("test.dk");
        assertEquals("Too few or too many AliasInfo objects returned", 2, aliasInfoList.size());
    }

    @Category(SlowTest.class)
    @Test
    public void testGetTLDs() {
        // create some domains.
        dk.netarkivet.harvester.webinterface.DomainDefinition.createDomains("venstre.dk", "venstre.nu", "sy-jonna.dk",
                "one.com", "two.com", "one.dk", "two.net", "1.2.3.4", "3.63.102.33", "2.3.4.3");
        DomainDAO dao = DomainDAO.getInstance();
        List<TLDInfo> result = dao.getTLDs(1);
        assertEquals("Expected 5 TLDs", 5, result.size());
        assertEquals("Should have two subdomains of .com", 2, result.get(1).getCount());
        assertEquals("Should have three IP subdomains", 3, result.get(0).getCount());
    }

    @Category(SlowTest.class)
    @Test
    public void testGetMultiLevelTLD() {
        // create some domains.
        dk.netarkivet.harvester.webinterface.DomainDefinition.createDomains("bidon.fr", "bidon1.gouv.fr",
                "bidon2.gouv.fr", "venstre.dk", "venstre.nu", "sy-jonna.dk", "one.com", "two.com", "one.dk", "two.net",
                "1.2.3.4", "3.63.102.33", "2.3.4.3");
        DomainDAO dao = DomainDAO.getInstance();
        List<TLDInfo> result = dao.getTLDs(1);
        assertEquals("test level 1: Expected 6 TLDs", 6, result.size());
        assertEquals("test level 1: Should have two subdomains of .com", 2, result.get(1).getCount());
        assertEquals("test level 1: Should have three IP subdomains", 3, result.get(0).getCount());

        List<TLDInfo> result2 = dao.getTLDs(2);
        assertEquals("test level 2: Expected 7 TLDs", 7, result2.size());
        assertEquals("test level 2: Should have two subdomains of .com", 2, result2.get(1).getCount());

        assertEquals("test level 2: Should have three subdomains of .fr", 3, result2.get(3).getCount());

        assertEquals("test level 2: Should have two subdomains of gouv.fr", 2, result2.get(4).getCount());

        assertEquals("test level 2: Should have three IP subdomains", 3, result2.get(0).getCount());
    }

    @Ignore ("can't connect to the database error")
    @Test
    public void testGetCountDomains() {
        // create some domains, ignore invalid domains
        dk.netarkivet.harvester.webinterface.DomainDefinition.createDomains("venstre.dk", "todo.dk", "venstre.nu",
                "sy-jonna.dk", "one.com", "two.com", "one.net", "two.net");
        DomainDAO dao = DomainDAO.getInstance();
        List<TLDInfo> result = dao.getTLDs(1);
        assertEquals("Too few or too many TLDs found", 4, result.size());
    }

    /**
     * Unittest for method getDomainJobInfo.
     */
    @Category(SlowTest.class)
    @Test
    public void testGetDomainJobInfo() {
        HarvestDefinitionDAO hdDao = HarvestDefinitionDAO.getInstance();
        HarvestDefinition hd = hdDao.read(Long.valueOf(42));
        DefaultJobGenerator jobGen = new DefaultJobGenerator();
        jobGen.generateJobs(hd);
        JobDAO jdao = JobDBDAO.getInstance();
        Job j = jdao.getAll().next();
        Map<String, String> dcMap = j.getDomainConfigurationMap();
        String theDomainName = "netarkivet.dk";
        String configName = dcMap.get(theDomainName);

        // Fake that the job has been run by inserting a historyInfo
        // entry in the database for this job_id, config_id, harvest_id combination.
        HarvestInfo hi1 = new HarvestInfo(j.getOrigHarvestDefinitionID(), j.getJobID(), theDomainName, configName,
                new Date(), 10000L, 64L, StopReason.OBJECT_LIMIT);

        Connection c = HarvestDBConnection.get();
        try {
            long domainId = DBUtils.selectLongValue(c, "SELECT domain_id FROM domains WHERE name=?", theDomainName);
            long configId = DBUtils.selectLongValue(c,
                    "SELECT config_id FROM configurations WHERE name = ? AND domain_id=?", configName, domainId);

            insertHarvestInfo(c, hi1, configId);
        } finally {
            HarvestDBConnection.release(c);
        }
    }

    @Category(SlowTest.class)
    @Test
    public void testGetDomainHarvestInfo() {
        Job job1 = JobDAOTester.createDefaultJobInDB(0);
        Job job2 = JobDAOTester.createDefaultJobInDB(1);
        Map<String, String> dcMap = job1.getDomainConfigurationMap();
        Map.Entry<String, String> domainConfMapping = job1.getDomainConfigurationMap().entrySet().iterator().next();
        String theDomainName = domainConfMapping.getKey();
        String configName = domainConfMapping.getValue();
        final Date NOW = new Date();
        final Date ONE_DAY_AGO = new Date(System.currentTimeMillis()-3600*24*1000);
        job1.setActualStart(NOW);
        job2.setActualStart(ONE_DAY_AGO);
        JobDAO.getInstance().update(job1);
        JobDAO.getInstance().update(job2);
        // Fake that the job has been run by inserting a historyInfo
        // entry in the database for this job_id, config_id, harvest_id combination.
        HarvestInfo hi1 = new HarvestInfo(job1.getOrigHarvestDefinitionID(), job1.getJobID(), theDomainName, configName,
                new Date(), 10000L, 64L, StopReason.OBJECT_LIMIT);
        HarvestInfo hi2 = new HarvestInfo(job2.getOrigHarvestDefinitionID(), job2.getJobID(), theDomainName, configName,
                ONE_DAY_AGO, 10000L, 64L, StopReason.OBJECT_LIMIT);

        Connection c = HarvestDBConnection.get();
        try {
            long domainId = DBUtils.selectLongValue(c, "SELECT domain_id FROM domains WHERE name=?", theDomainName);
            long configId = DBUtils.selectLongValue(c,
                    "SELECT config_id FROM configurations WHERE name = ? AND domain_id=?", configName, domainId);
            insertHarvestInfo(c, hi1, configId);
            insertHarvestInfo(c, hi2, configId);
        } finally {
            HarvestDBConnection.release(c);
        }
        DomainDAO dao = DomainDAO.getInstance();
        List<DomainHarvestInfo> hinfos = dao.listDomainHarvestInfo(theDomainName, "startdate", false);
        Date d0 = hinfos.get(0).getStartDate();
        Date d1 = hinfos.get(1).getStartDate();

        assertTrue("Should have dates in inverse start order not '" + d0 + "," + d1 + "'", d0.after(d1));

        hinfos = dao.listDomainHarvestInfo(theDomainName, "startdate", true);
        d0 = hinfos.get(0).getStartDate();
        d1 = hinfos.get(1).getStartDate();

        assertTrue("Should have dates in start order not '" + d0 + "," + d1 + "'", d1.after(d0));

    }

    // Copied from DomainDBDAO for local testing
    private void insertHarvestInfo(Connection c, HarvestInfo harvestInfo, long configId) {
        PreparedStatement s = null;
        try {
            // Note that the config_id is grabbed from the configurations table.
            s = c.prepareStatement("INSERT INTO historyinfo "
                    + "( stopreason, objectcount, bytecount, config_id, job_id, " + "harvest_id, harvest_time ) "
                    + "VALUES ( ?, ?, ?, ?, ?, ?, ? )", Statement.RETURN_GENERATED_KEYS);
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
        }
    }
}

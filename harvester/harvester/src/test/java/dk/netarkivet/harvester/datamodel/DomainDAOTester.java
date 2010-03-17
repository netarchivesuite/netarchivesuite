/*$Id$
* $Revision$
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
package dk.netarkivet.harvester.datamodel;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.IteratorUtils;
import dk.netarkivet.testutils.CollectionAsserts;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.StringAsserts;


/**
 * Test the persistence framework for Domain.
 */
public class DomainDAOTester extends DataModelTestCase {
    private static final int NUM_DOMAINS = 4;

    Connection c;

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * @param sTestName
     */
    public DomainDAOTester(String sTestName) {
        super(sTestName);
    }

    /**
     * Check that creation of a new Domain instance succeeds.
     */
    public void testCreateAndRead() {
        DomainDAO dao = DomainDAO.getInstance();
        Domain wd = TestInfo.getDefaultNewDomain();
        DomainConfiguration cfg1 = TestInfo.getDefaultConfig(wd);
        wd.addConfiguration(cfg1);

        String domainName = wd.getName();
        dao.create(wd);

        // check that it is possible to retrieve the domain information again
        DomainDAO dao2 = DomainDAO.getInstance();
        Domain wd2 = dao2.read(domainName);

        cfg1 = wd.getConfiguration(TestInfo.DEFAULTCFGNAME);

        DomainConfiguration cfg2 = wd2.getConfiguration(TestInfo.DEFAULTCFGNAME);

        /* Verify that saved and loaded data identical */
        assertEquals("Retrieved data must match stored data", wd.getName(),
                     wd2.getName());
        assertEquals("Retrieved data must match stored data",
                     cfg1.getMaxObjects(), cfg2.getMaxObjects());
        assertEquals("Retrieved data must match stored data",
                     cfg1.getMaxRequestRate(), cfg2.getMaxRequestRate());
        CollectionAsserts.assertIteratorEquals("Retrieved data must match stored data",
                                               cfg1.getSeedLists(), cfg2.getSeedLists());
        assertEquals("Retrieved data must match stored data",
                     cfg1.getOrderXmlName(), cfg2.getOrderXmlName());
        CollectionAsserts.assertIteratorEquals("Retrieved data must match stored data",
                                               cfg1.getPasswords(), cfg2.getPasswords());

        SeedList seedlist1 = wd.getSeedList(TestInfo.SEEDLISTNAME);
        SeedList seedlist2 = wd2.getSeedList(TestInfo.SEEDLISTNAME);
        assertEquals("Retrieved data must match stored data",
                     seedlist1.getSeedsAsString(), seedlist2.getSeedsAsString());

        // Test that we can't create it again.
        try {
            dao.create(wd);
            fail("Should not be able to create an already existing domain " + wd);
        } catch (PermissionDenied expected) {
            // Expected case
        }
    }

    /** Test deletion of WebDomains. */
    public void testDelete() {
        // create domain to delete
        DomainDAO dao = DomainDAO.getInstance();
        Domain wd = TestInfo.getDefaultNewDomain();
        DomainConfiguration cfg1 = TestInfo.getDefaultConfig(wd);
        wd.addConfiguration(cfg1);

        int original_count = dao.getCountDomains();

        dao.create(wd);

        // create new dao just to check that it works from different dao's
        DomainDAO dao2 = DomainDAO.getInstance();

        // First check invalid argument scenarios
        try {
            dao2.delete(null);
            fail("null not an allowed argument");
        } catch (ArgumentNotValid e) {
            //expected
        }

        try {
            dao2.delete("");
            fail("Empty string not an allowed argument");
        } catch (ArgumentNotValid e) {
            //expected
        }

        try {
            dao2.delete("UnknownId");
            fail("No domain exists with the requested id");
        } catch (UnknownID e) {
            //expected
        }

        assertTrue("The domain should exist before deletion",
                   dao2.exists(wd.getName()));
        assertTrue("The domain should be deletable",
                   dao2.mayDelete(wd));
        // perform the deletion
        dao2.delete(TestInfo.DEFAULTNEWDOMAINNAME);

        // verify no domain left
        assertFalse("The deleted domain should not exist",
                    dao2.exists(wd.getName()));
        assertEquals("Should have the original number of domains after deletion",
                     original_count, dao2.getCountDomains());
    }

    /** Check check updating of an existing entry. */
    public void testUpdate() {
        DomainDAO dao = DomainDAO.getInstance();

        /* Create domain to update */
        Domain wd = TestInfo.getDefaultNewDomain();
        DomainConfiguration cfg1 = TestInfo.getDefaultConfig(wd);
        wd.addConfiguration(cfg1);

        String domainName = wd.getName();
        dao.create(wd);

        try {
            dao.update(null);
            fail("Null not a valid argument");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            Domain wdnew = Domain.getDefaultDomain("UnknownId.com");
            dao.update(wdnew);
            fail("Unknown ID");
        } catch (UnknownID e) {
            // expected
        }

        // modify the domain definition and update
        wd.addSeedList(TestInfo.seedlist2);
        dao.update(wd);

        // check that the modified domain can be retrieved
        DomainDAO dao2 = DomainDAO.getInstance();
        Domain wd2 = dao2.read(domainName);
        wd2.getSeedList(TestInfo.SEEDLISTNAME2);
    }


    /** Check that updating an entry that has already been modified
     *  results in an IOFailure.
     *  */
    public void testOptimisticLocking() {
        DomainDAO dao = DomainDAO.getInstance();

        /* Create domain to update */
        Domain wd = TestInfo.getDefaultNewDomain();
        DomainConfiguration cfg1 = TestInfo.getDefaultConfig(wd);
        wd.addConfiguration(cfg1);

        dao.create(wd);

        // Load the domain into 2 different instances
        Domain inst1 = dao.read(wd.getName());
        Domain inst2 = dao.read(wd.getName());

        assertEquals("The instances should have the same edition number",
                     inst2.getEdition(), inst1.getEdition());

        // updating the first instance should succeed
        // notice that an update of an unmodified instance still
        // is expected to increment the edition counter
        dao.update(inst1);

        try {
          dao.update(inst2);
          fail("The previous update of inst1 should result in an inst2 update error");
        } catch (PermissionDenied e) {
          //expected
        }

    }


    /** Test retrieval of all domains. */
    public void testGetAllDomains() {
        DomainDAO dao = DomainDAO.getInstance();

        // count domains already present
        Iterator<Domain> dwl = dao.getAllDomains();

        List<Domain> domains = new ArrayList<Domain>();
        while (dwl.hasNext()) {
            Domain wd = (Domain) dwl.next();
            domains.add(wd);
        }

        // Add a whole bunch of new domains
        for (int i = 0; i < NUM_DOMAINS; ++i) {
            Domain wd = Domain.getDefaultDomain("www" + i + ".com");
            wd.addSeedList(TestInfo.seedlist);
            wd.addConfiguration(TestInfo.getDefaultConfig(wd));
            dao.create(wd);
        }

        // Retrieve the list of created domains
        dwl = dao.getAllDomains();

        List<Domain> newDomains = new ArrayList<Domain>();
        while (dwl.hasNext()) {
            Domain wd = (Domain) dwl.next();
            newDomains.add(wd);
        }

        // not really the most stringent test but probably sufficient
        int added_domains = newDomains.size() - domains.size();
        assertEquals(NUM_DOMAINS + " domains added, "
                     + NUM_DOMAINS + " domains expected.\nOriginal domains: "
                     + domains + "\nNew domains: "
                     + newDomains,
                     NUM_DOMAINS, added_domains);
    }

    /** Test for bug #121: Trying to access a non-existing domain
     * creates part of the domain structure. */
    public void testAccessNonExisting() {
        DomainDAO dao = DomainDAO.getInstance();

        assertFalse("Domain '" + TestInfo.DEFAULTNEWDOMAINNAME + "' should not exist",
                    dao.exists(TestInfo.DEFAULTNEWDOMAINNAME));
        try {
            dao.read(TestInfo.DEFAULTNEWDOMAINNAME);
            fail("Should not be able to read '" + TestInfo.DEFAULTNEWDOMAINNAME + "' before creating it");
        } catch (UnknownID expected) {
            //
        }

        assertFalse("Domain '" + TestInfo.DEFAULTNEWDOMAINNAME + "' should not exist after failed read",
                    dao.exists(TestInfo.DEFAULTNEWDOMAINNAME));

        Domain wd = TestInfo.getDefaultNewDomain();
        DomainConfiguration cfg1 = TestInfo.getDefaultConfig(wd);
        wd.addConfiguration(cfg1);

        dao.create(wd);
        assertTrue("Domain '" + TestInfo.DEFAULTNEWDOMAINNAME + "' should exist",
                   dao.exists(TestInfo.DEFAULTNEWDOMAINNAME));

        dao.read(TestInfo.DEFAULTNEWDOMAINNAME);
    }

    /** Reset the domain DAO. */
    public static void resetDomainDAO() {
        DomainDAO.resetSingleton();
    }

    /** Set the domain DAO to a specific object. */
    public static void setDomainDAO(DomainDAO dao) throws NoSuchFieldException,
            IllegalAccessException {
        Field f = ReflectUtils.getPrivateField(DomainDAO.class, "instance");
        f.set(null, dao);
    }

    /** Test that an existing domain can be read.
     * @throws Exception
     */
    public void testRead() throws Exception {
        DomainDAO dao = DomainDAO.getInstance();
        Domain d = dao.read("dr.dk");
        assertNotNull("Domain should exist", d);
        assertEquals("Domain name should be correct", "dr.dk", d.getName());
        assertEquals("Default configuration should be correct",
                     "fuld_dybde", d.getDefaultConfiguration().getName());
        int count = 0;
        for (Iterator<DomainConfiguration> i = d.getAllConfigurations(); i.hasNext(); i.next(), count++) {
        }
        assertEquals("Number of configurations should be 1", 1, count);
        assertFalse("Number of password entries should be 0",
                    d.getAllPasswords().hasNext());
        assertEquals("Amount of domain owner info should be correct",
                     1, d.getAllDomainOwnerInfo().length);
        count = 0;
        for (Iterator<SeedList> i = d.getAllSeedLists(); i.hasNext(); i.next(), count++) {
        }
        assertEquals("Number of seedlists should be 1", 1, count);
        assertEquals("Amount of owner info should be 1",
                     1, d.getAllDomainOwnerInfo().length);
    }

    /** Test HarvestInfo read and write.
     * @throws Exception
     */
    public void testReadAndWriteHarvestInfo() throws Exception {
        DomainDAO dao = DomainDAO.getInstance();
        Domain domain0 = dao.read("dr.dk");

        //Testinfo
        HarvestInfo [] his = new HarvestInfo []{
            new HarvestInfo(new Long(4), domain0.getName(), "fuld_dybde", new Date(400000L), 40, 1, StopReason.DOWNLOAD_COMPLETE),
            new HarvestInfo(new Long(2), domain0.getName(), "fuld_dybde", new Date(300000L), 30, 2, StopReason.OBJECT_LIMIT),
            new HarvestInfo(new Long(3), domain0.getName(), "fuld_dybde", new Date(200000L), 20, 3, StopReason.OBJECT_LIMIT),
            new HarvestInfo(new Long(1), domain0.getName(), "fuld_dybde", new Date(100000L), 10, 4, StopReason.OBJECT_LIMIT)
        };

        //clear history
        DomainHistory domainHistory0 = domain0.getHistory();
        assertFalse("There should be 0 harvest infos before adding",
                    domainHistory0.getHarvestInfo().hasNext());

        domainHistory0.addHarvestInfo(his[0]);
        domainHistory0.addHarvestInfo(his[1]);
        domainHistory0.addHarvestInfo(his[2]);
        domainHistory0.addHarvestInfo(his[3]);

        dao.update(domain0);

        Domain domain1 = dao.read("dr.dk");
        DomainHistory domainHistory1 = domain1.getHistory();

        List<HarvestInfo> readhislist = new ArrayList<HarvestInfo>();

        for(Iterator<HarvestInfo> i = domainHistory1.getHarvestInfo(); i.hasNext(); ) {
            readhislist.add(i.next());
        }

        HarvestInfo[] readhis
                = readhislist.toArray(new HarvestInfo[0]);

        assertTrue("The info read should be the same as the info stored:"
                   + Arrays.asList(his) + "\n"
                   + "but was: " + Arrays.asList(readhis),
                   Arrays.equals(his,readhis));
    }

    /** Test that we get the right harvestinfo when asking based on an old harvestinfo.
     * @throws Exception
     */
    public void testGetHarvestInfoBasedOnPreviousHarvestDefinition()
            throws Exception {
        DomainDAO dao = DomainDAO.getInstance();
        try {
            dao.getHarvestInfoBasedOnPreviousHarvestDefinition(null);
            fail("Should throw exception on null argument");
        } catch (ArgumentNotValid e) {
            //expected
        }

        HarvestDefinition hd = HarvestDefinition.createFullHarvest("Full Harvest", "Test of full harvest", null, 2000,
                                                                   Constants.DEFAULT_MAX_BYTES);
        hd.setSubmissionDate(new Date());
        HarvestDefinitionDAO.getInstance().create(hd);

        Domain domain0 = dao.read("dr.dk");
        DomainConfiguration config0 = domain0.getDefaultConfiguration();
        Domain domain1 = dao.read("netarkivet.dk");
        DomainConfiguration config1 = domain1.getDefaultConfiguration();
        Domain domain2 = dao.read("statsbiblioteket.dk");
        DomainConfiguration config2 = domain2.getDefaultConfiguration();

        //milliseconds cleared since they disappear in the DAO
        long time = System.currentTimeMillis()/1000*1000;
        //An older harvest info that should NOT be returned
        Date then = new Date(time);
        HarvestInfo old_hi0 = new HarvestInfo(new Long(42L), domain0.getName(), config0.getName(), then, 1L, 1L, StopReason.DOWNLOAD_COMPLETE);
        config0.addHarvestInfo(old_hi0);
        dao.update(domain0);

        //An older harvest info with same harvest definition that should NOT be returned
        //HarvestInfo old_hi1 = new HarvestInfo(hd.getOid(), domain1.getName(), config1.getName(), then);
        //old_hi1.setStopReason(StopReason.SIZE_LIMIT);
        //config1.addHarvestInfo(old_hi1);
        //dao.update(domain1);

        //Three harvest infos, one for each type
        Date now = new Date(time + 1000);
        HarvestInfo hi0 = new HarvestInfo(hd.getOid(), domain0.getName(), config0.getName(), now, 1L, 1L, StopReason.OBJECT_LIMIT);
        config0.addHarvestInfo(hi0);
        dao.update(domain0);

        HarvestInfo hi1 = new HarvestInfo(hd.getOid(), domain1.getName(), config1.getName(), now, 1L, 1L, StopReason.OBJECT_LIMIT);
        config1.addHarvestInfo(hi1);
        dao.update(domain1);

        HarvestInfo hi2 = new HarvestInfo(hd.getOid(), domain2.getName(), config2.getName(), now, 1L, 1L, StopReason.DOWNLOAD_COMPLETE);
        config2.addHarvestInfo(hi2);
        dao.update(domain2);

        //A newer harvest info that should NOT be returned
        Date later = new Date(time + 2000);
        HarvestInfo new_hi0 = new HarvestInfo(new Long(43L), domain0.getName(), config0.getName(), later, 1L, 1L, StopReason.DOWNLOAD_COMPLETE);
        config0.addHarvestInfo(new_hi0);
        dao.update(domain0);

        HarvestDefinitionDAO.getInstance().update(hd);

        // Test that we get exactly the three harvest infos
        Iterator<HarvestInfo> i = dao.getHarvestInfoBasedOnPreviousHarvestDefinition(hd);
        List<HarvestInfo> tmp = new ArrayList<HarvestInfo>(3);
        while (i.hasNext()) {
            tmp.add(i.next());
        }
        HarvestInfo[] hi = tmp.toArray(new HarvestInfo[0]);
        assertEquals("There should be three pieces of HarvestInfo for harvest,"
                     + "but found " + Arrays.asList(hi),
                     3, hi.length);

        assertTrue("One harvestinfo should be hi0" + hi[0] + "," + hi[1] + "," + hi[2],
                   hi[0].equals(hi0) || hi[1].equals(hi0) || hi[2].equals(hi0));

        assertTrue("One harvestinfo should be hi1" + hi[0] + "," + hi[1] + "," + hi[2],
                   hi[0].equals(hi1) || hi[1].equals(hi1) || hi[2].equals(hi1));

        assertTrue("One harvestinfo should be hi2" + hi[0] + "," + hi[1] + "," + hi[2],
                   hi[0].equals(hi2) || hi[1].equals(hi2) || hi[2].equals(hi2));
    }

    public void testGetCountDomains() throws Exception {
        assertEquals("Must have expected number of domains",
                     4, DomainDAO.getInstance().getCountDomains());
    }

    /** Test that crawler traps can be reread from DAO */
    public void testReadWriteCrawlerTraps() {
        //Add some crawler traps
        Domain d = Domain.getDefaultDomain("adomain.dk");
        List<String> definedregexps = new ArrayList<String>();
        definedregexps.add(".*dr\\.dk.*/.*\\.cgi");
        definedregexps.add(".*statsbiblioteket\\.dk/gentofte.*");
        d.setCrawlerTraps(definedregexps);

        //write and read the crawler traps
        DomainDAO.getInstance().create(d);
        Domain d2 = DomainDAO.getInstance().read("adomain.dk");

        //compare
        List<String> foundregexps = d2.getCrawlerTraps();
        assertEquals("Crawler traps should be saved as given",
                     definedregexps, foundregexps);
    }

    public void testGetDomains() throws Exception {
        DomainDAO dao = DomainDAO.getInstance();

        checkDomainGlob(dao, "*", new String[] {
            "dr.dk", "kb.dk",  "netarkivet.dk", "statsbiblioteket.dk"
        });

        checkDomainGlob(dao, "*et.dk", new String[] {
            "netarkivet.dk", "statsbiblioteket.dk"
        });

        checkDomainGlob(dao, "*r*", new String[] {
            "dr.dk", "netarkivet.dk"
        });

        checkDomainGlob(dao, "??.dk", new String[] {
            "dr.dk", "kb.dk"
        });
    }

    private void checkDomainGlob(DomainDAO dao, final String glob,
                                 final String[] domains) {
        List<String> match1 = dao.getDomains(glob);
        List<String> match1res = Arrays.asList(domains );
        assertEquals("Should find " + domains.length + " domains total, only got "
                     + match1, domains.length, match1.size());
        int i = 0;
        for (String s : match1res) {
            assertEquals("Domain " + i + " should be " + s, s, match1.get(i));
            i++;
        }
    }

    public void testGetDomainHarvestInfo() throws Exception {
        DomainDAO dao = DomainDAO.getInstance();
        
        List<DomainHarvestInfo> info = dao.getDomainHarvestInfo("dr.dk");
        assertEquals("Should have no info for unharvested domain", 0, info.size());

        final String domainName = "netarkivet.dk";
        Domain d = dao.read(domainName);
        DomainConfiguration dc = d.getDefaultConfiguration();
        HarvestInfo hi = new HarvestInfo(42L, 1L, domainName, dc.getName(),
                                         new Date(2L), 10000L, 100L, StopReason.DOWNLOAD_COMPLETE);
        dc.addHarvestInfo(hi);
        d.updateConfiguration(dc);
        dao.update(d);

        Domain d2 = dao.read(domainName);
        assertEquals("Domain should have harvest info after load",
                     1, IteratorUtils.toList(d2.getHistory().getHarvestInfo()).size());
        dc = d2.getDefaultConfiguration();
        assertEquals("Default config should have harvest info for job 1",
                     new Long(1), d2.getHistory().getMostRecentHarvestInfo(dc.getName()).getJobID());
        info = dao.getDomainHarvestInfo(domainName);
        assertEquals("Should have 1 info for harvested domain", 1, info.size());
        DomainHarvestInfo hinfo = info.get(0);
        assertEquals("Info should have right job id",
                     1, hinfo.getJobID());
        assertEquals("Info should have harvest name",
                     "Testhøstning", hinfo.getHarvestName());
        assertEquals("Info should have right harvest num",
                     1, hinfo.getJobID());
        assertEquals("Info should have config name",
                     "Dansk_netarkiv_fuld_dybde", hinfo.getConfigName());
        assertEquals("Info should have stopreason",
                     StopReason.DOWNLOAD_COMPLETE, hinfo.getStopReason());
        // No start or end date on this job.
        assertEquals("Info should have correct #bytes",
                     10000L, hinfo.getBytesDownloaded());
        assertEquals("Info should have correct #docs",
                     100L, hinfo.getDocsDownloaded());

        // For bug 570, test if having some history info with no job id
        // generates too many entries.
        hi = new HarvestInfo(42L, null, domainName, dc.getName(), new Date(1L), 10L, 2L, StopReason.DOWNLOAD_COMPLETE);
        dc.addHarvestInfo(hi);
        hi = new HarvestInfo(42L, null, domainName, dc.getName(), new Date(3L), 11L, 3L, StopReason.DOWNLOAD_COMPLETE);
        dc.addHarvestInfo(hi);
        d2.updateConfiguration(dc);
        dao.update(d2);
        d2 = dao.read(domainName);
        assertEquals("Domain should now have two more harvest infos",
                     3, IteratorUtils.toList(d2.getHistory().getHarvestInfo()).size());
        info = dao.getDomainHarvestInfo(domainName);
        assertEquals("Should have one info for each historyinfo", 3, info.size());
    }

    /** Test that we cannot store a domain that drops configs, seedlists
     * or passwords that are in use.
     */
    public void testDeleteSubparts() {
        DomainDAO dao = DomainDAO.getInstance();

        final String domainName = "kb.dk";
        Domain d = dao.read(domainName);

        // passwords
        assertTrue("mayDelete should allow deleting",
                   dao.mayDelete(d.getPassword("alphapassword")));
        d.removePassword("alphapassword");
        dao.update(d);
        // Should not get a failure.
        d = dao.read(domainName);
        assertFalse("Password should be deleted in stored version",
                    d.hasPassword("alphapassword"));

        assertFalse("mayDelete should not allow deleting",
                    dao.mayDelete(d.getPassword("testpassword")));
        try {
            d.removePassword("testpassword");
            dao.update(d);
            fail("Should get exception trying to delete used password");
        } catch (PermissionDenied e) {
            StringAsserts.assertStringContains(
                    "Should mention config in error message",
                    d.getConfiguration("fuld_dybde").getName(), e.getMessage());
        }
        d = dao.read(domainName);
        assertTrue("Undeletable password should still exist in stored version",
                   d.hasPassword("testpassword"));

        // seedlists
        assertTrue("mayDelete should allow deletion",
                   dao.mayDelete(d.getSeedList("deletable")));
        d.removeSeedList("deletable");
        dao.update(d);
        d = dao.read(domainName);
        assertFalse("Seedlist should be deleted in stored version",
                    d.hasSeedList("deletable"));

        assertFalse("mayDelete should not allow deletion",
                    dao.mayDelete(d.getSeedList("default")));
        try {
            d.removeSeedList("default");
            dao.update(d);
            fail("Should get exception trying to delete used seedlist");
        } catch (PermissionDenied e) {
            StringAsserts.assertStringContains(
                    "Should mention config in error message",
                    d.getDefaultConfiguration().getName(), e.getMessage());
        }
        d = dao.read(domainName);
        assertTrue("Seedlist should still exist in stored version",
                   d.hasSeedList("default"));

        // configurations
        final String defaultConfigName = d.getDefaultConfiguration().getName();
        assertFalse("mayDelete should not allow deletion of default config "
                    + defaultConfigName,
                    dao.mayDelete(d.getConfiguration(defaultConfigName)));
        try {
            d.removeConfiguration(defaultConfigName);
            dao.update(d);
            fail("Should get error trying to delete the default config");
        } catch (PermissionDenied e) {
            // expected
        }
        d = dao.read(domainName);
        assertTrue("Default config should still exist in stored version",
                   d.hasConfiguration(defaultConfigName));

        HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        PartialHarvest hd = (PartialHarvest)hddao.read(42L);
        List<DomainConfiguration> configs = new ArrayList<DomainConfiguration>();
        final String nonDefaultConfigName = "fuld_dybde";
        configs.add(d.getConfiguration(nonDefaultConfigName));
        hd.setDomainConfigurations(configs);
        hddao.update(hd);
        assertFalse("mayDelete should not allow deletion",
                    dao.mayDelete(d.getConfiguration(nonDefaultConfigName)));
        try {
            d.removeConfiguration(nonDefaultConfigName);
            dao.update(d);
            fail("Should get error trying to delete used config");
        } catch (PermissionDenied e) {
            // Test that the error gives good output.
            StringAsserts.assertStringContains(
                    "Should mention harvest def in error message",
                    hd.getName(), e.getMessage());
        }
        d = dao.read(domainName);
        assertTrue("Used config should still exist",
                   d.hasConfiguration(nonDefaultConfigName));

        // Now take config out of harvest and see it go away
        assertFalse("mayDelete should not yet allow deletion of "
                    + nonDefaultConfigName,
                    dao.mayDelete(d.getConfiguration(nonDefaultConfigName)));
        configs.clear();
        configs.add(d.getDefaultConfiguration());
        hd.setDomainConfigurations(configs);
        hddao.update(hd);
        assertTrue("mayDelete should allow deletion of "
                   + nonDefaultConfigName,
                   dao.mayDelete(d.getConfiguration(nonDefaultConfigName)));
        d.removeConfiguration(nonDefaultConfigName);
        dao.update(d);
        d = dao.read(domainName);
        assertFalse("Deleted config should be gone from domain",
                    d.hasConfiguration(nonDefaultConfigName));
    }

    /** Test that we can add and retrieve owner info. */
    public void testInsertOwnerInfo() {
        DomainDAO dao = DomainDAO.getInstance();
        String domainName = "kb.dk";
        Domain d = dao.read(domainName);
        assertTrue("Domain should not have ownerinfo before insertion",
                   d.getAllDomainOwnerInfo().length == 0);
        final Date now = new Date();
        d.addOwnerInfo(new DomainOwnerInfo(now, "a string"));
        dao.update(d);
        Domain d2 = dao.read(d.getName());
        DomainOwnerInfo[] info = d2.getAllDomainOwnerInfo();
        assertEquals("Should have one ownerinfo after insertion",
                     1, info.length);
        assertEquals("Should have same date on ownerinfo", now, info[0].getDate());
        assertEquals("Should have same info on ownerinfo", "a string", info[0].getInfo());
        d2.addOwnerInfo(new DomainOwnerInfo(now, "another string"));
        dao.update(d2);

        Domain d3 = dao.read(d.getName());
        info = d3.getAllDomainOwnerInfo();
        assertEquals("Should have two ownerinfo after insertion",
                     2, info.length);
    }

    /** Test that we can add and retrieve owner info. */
    public void testInsertPassword() {
        DomainDAO dao = DomainDAO.getInstance();
        String domainName = "dr.dk";
        Domain d = dao.read(domainName);
        assertEquals("Domain should not have passwords before insertion",
                     0, IteratorUtils.toList(d.getAllPasswords()).size());
        d.addPassword(new Password("foo", "bar", "baz", "qux", "quux", "quuux"));
        dao.update(d);
        Domain d2 = dao.read(d.getName());
        List<Password> info = IteratorUtils.toList(d2.getAllPasswords());
        assertEquals("Should have one password after insertion",
                     1, info.size());
        assertEquals("Should have same name on password", "foo", info.get(0).getName());
        assertEquals("Should have same comment on password", "bar", info.get(0).getComments());
        assertEquals("Should have same url on password", "baz", info.get(0).getPasswordDomain());
        assertEquals("Should have same realm on password", "qux", info.get(0).getRealm());
        assertEquals("Should have same username on password", "quux", info.get(0).getUsername());
        assertEquals("Should have same password on password", "quuux", info.get(0).getPassword());
        d2.addPassword(new Password("foo2", "bar", "baz", "qux", "quux", "quuux"));
        dao.update(d2);

        Domain d3 = dao.read(d.getName());
        info = IteratorUtils.toList(d3.getAllPasswords());
        assertEquals("Should have two passwords after insertion",
                     2, info.size());
    }

    public void testReadSparse() {
        DomainDAO dao = DomainDAO.getInstance();
        try {
            dao.readSparse(null);
            fail("Should throw exception on null");
        } catch (ArgumentNotValid e) {
            //expected
        }

        try {
            dao.readSparse("Fnord");
            fail("Should throw exception on unknown");
        } catch (UnknownID e) {
            //expected
        }

        String domainName = "dr.dk";
        SparseDomain d = dao.readSparse(domainName);
        assertEquals("Name should be right",
                     domainName, d.getName());
        Iterator<String> domainConfigurationNames
                = d.getDomainConfigurationNames().iterator();
        assertTrue("Should have configs",
                   domainConfigurationNames.hasNext());
        assertEquals("Should have right config",
                     "fuld_dybde", domainConfigurationNames.next());
        assertFalse("Should have no more configs",
                    domainConfigurationNames.hasNext());
    }

    /** Test getting all domains from database, but in the order that domains
     * are sorted by
     * - Default configuration template
     * - Default configuration max byte limit
     */
    public void testGetAllDomainsInSnapshotHarvestOrder() {
        //First, make sure we have something interesting to sort...
        DomainDAO dao = DomainDAO.getInstance();
        Domain d1 =dao.read("dr.dk");
        d1.getDefaultConfiguration().setOrderXmlName("FullSite-order");
        d1.getDefaultConfiguration().setMaxBytes(2000000);
        dao.update(d1);
        Domain d2 =dao.read("kb.dk");
        d2.getDefaultConfiguration().setOrderXmlName("Max_20_2-order");
        d2.getDefaultConfiguration().setMaxBytes(1000000);
        dao.update(d2);
        Domain d3 =dao.read("netarkivet.dk");
        d3.getDefaultConfiguration().setOrderXmlName("FullSite-order");
        d3.getDefaultConfiguration().setMaxBytes(1000000);
        dao.update(d3);
        Domain d4 =dao.read("statsbiblioteket.dk");
        d4.getDefaultConfiguration().setOrderXmlName("Max_20_2-order");
        d4.getDefaultConfiguration().setMaxBytes(2000000);
        dao.update(d4);

        Iterator<Domain> i = DomainDAO.getInstance().getAllDomainsInSnapshotHarvestOrder();
        Domain reference = (Domain) i.next();
        while (i.hasNext()) {
            Domain next = (Domain) i.next();
            DomainConfiguration cfg1 = reference.getDefaultConfiguration();
            DomainConfiguration cfg2 = next.getDefaultConfiguration();
            assertTrue("Order should be right, comparing " + cfg1
                       + " and " + cfg2
                       + ":\n("+ cfg1.getOrderXmlName() +  "," + cfg1.getMaxBytes() + "," + cfg1.getDomain().getName() +  ")"
                       + "\n("+ cfg2.getOrderXmlName() +  "," + cfg2.getMaxBytes() + "," + cfg2.getDomain().getName() +  ")",
                       cfg1.getOrderXmlName().compareTo(cfg2.getOrderXmlName())
                       < 0 || (cfg1.getOrderXmlName().compareTo(
                               cfg2.getOrderXmlName()) == 0)
                              && cfg1.getMaxBytes() > cfg2.getMaxBytes());
            reference = next;
        }
    }
    
    /** Check constructor of DomainHarvestInfo(). */
    public void testDomainHarvestInfoConstructor() {
        long jobId = 42L;
        String domain = "netarkivet.dk";
        String harvestName = "TestHarvest";
        long harvestId = 1L;
        int harvestNum = 0;
        String configName = "defaultconfig";
        Date startDate = new Date();
        Date endDate = new Date();
        long bytesDownloaded = 42000L;
        long docsdownloaded = 995L;
        StopReason theReason = StopReason.DOWNLOAD_COMPLETE;
        DomainHarvestInfo dhi = new DomainHarvestInfo(domain, jobId,
                harvestName, harvestId, harvestNum, configName, startDate,
                endDate, bytesDownloaded, docsdownloaded, theReason);
        assertEquals(domain, dhi.getDomain());
        assertEquals(jobId, dhi.getJobID());
        assertEquals(harvestName, dhi.getHarvestName());
        assertEquals(harvestId, dhi.getHarvestID());
        
        assertEquals(harvestNum, dhi.getHarvestNum());
        assertEquals(configName, dhi.getConfigName());
        assertEquals(startDate, dhi.getStartDate());
        assertEquals(endDate, dhi.getEndDate());  
        assertEquals(bytesDownloaded, dhi.getBytesDownloaded());
        assertEquals(docsdownloaded, dhi.getDocsDownloaded());
        assertEquals(theReason, dhi.getStopReason());
    }
    
}


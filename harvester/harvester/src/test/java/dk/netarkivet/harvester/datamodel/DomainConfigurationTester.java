/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;


/**
 * Tests for the DomainConfiguration class.  Also widely tested from other
 * places.
 *
 */

public class DomainConfigurationTester extends DataModelTestCase {
    public DomainConfigurationTester(String sTestName) {
        super(sTestName);
    }

    public void setUp() throws Exception {
        super.setUp();
        Settings.set(Settings.ERRORFACTOR_PERMITTED_BESTGUESS, "2");
        Settings.set(Settings.ERRORFACTOR_PERMITTED_PREVRESULT, "10");
        Settings.set(Settings.JOBS_MIN_ABSOLUTE_SIZE_DIFFERENCE, "100");
        Settings.set(Settings.JOBS_MAX_TOTAL_JOBSIZE, "1000000");
    }

    public void tearDown() throws Exception {
        super.tearDown();
        Settings.reload();
    }

    public void testConstructorAndSomeGetters() {
        Domain wd = Domain.getDefaultDomain(TestInfo.DOMAIN_NAME);
        wd.addSeedList(TestInfo.seedlist);

        DomainConfiguration newcfg
                = new DomainConfiguration(TestInfo.CONFIGURATION_NAME, wd,
             Arrays.asList(new SeedList[]{TestInfo.seedlist}), new ArrayList<Password>());

        wd.addConfiguration(newcfg);
        wd.setDefaultConfiguration(newcfg.getName());

        // Retrieve the configuration in non-trivial way
        DomainConfiguration cfg = wd.getDefaultConfiguration();

        assertEquals("Expecting name that we created the object with",
                TestInfo.CONFIGURATION_NAME, cfg.getName());
        assertEquals("Expecting domain that we created the object with",
                wd, cfg.getDomain());
        Iterator seeds = cfg.getSeedLists();
        assertEquals("Expecting the seedlists that we created the object with",
                TestInfo.seedlist,
                seeds.next());
        assertEquals("should have just one seedlist",
                false,
                seeds.hasNext());
        assertEquals("Expecting empty list of passwords that we created the"
                     + " object with",
                false, cfg.getPasswords().hasNext());
    }

    /** Test that the maxBytes field is stored correctly.
     * This test is added because the maxBytes field was added after
     * the database was put into production.
     */
    public void testMaxBytes() {
        DomainDAO dao = DomainDAO.getInstance();
        Domain wd = dao.read(TestInfo.EXISTINGDOMAINNAME);
        wd.addSeedList(TestInfo.seedlist);

        DomainConfiguration newcfg
                = new DomainConfiguration(TestInfo.DEFAULTCFGNAME, wd,
                        Arrays.asList(new SeedList[]{TestInfo.seedlist}), new ArrayList<Password>());
        // TODO: Make sure a config always has a valid template?
        newcfg.setOrderXmlName(TestInfo.ORDER_XML_NAME);
        assertEquals("maxBytes should start out at default value",
                Constants.DEFAULT_MAX_BYTES,
                newcfg.getMaxBytes());
        final int maxBytes = 201*1024*1024;
        newcfg.setMaxBytes(maxBytes);
        assertEquals("maxBytes should reflect newly set value",
                maxBytes, newcfg.getMaxBytes());

        wd.addConfiguration(newcfg);
        wd.setDefaultConfiguration(newcfg.getName());
        dao.update(wd);
        wd = dao.read(wd.getName());

        // Retrieve the configuration in non-trivial way
        DomainConfiguration cfg = wd.getDefaultConfiguration();
        assertEquals("Expecting same maxBytes as stored",
                maxBytes, cfg.getMaxBytes());

        long maxBytes2 = 202 * 1024 * 1024;
        cfg.setMaxBytes(maxBytes2);
        dao.update(wd);
        wd = dao.read(wd.getName());

        // Retrieve the configuration in non-trivial way
        DomainConfiguration cfg2 = wd.getDefaultConfiguration();
        assertEquals("Expecting same maxBytes as stored",
                maxBytes2, cfg2.getMaxBytes());

    }

    /** Test that we can remove a password.
     * @throws Exception
     */
    public void testRemovePassword() throws Exception {
        Domain d = Domain.getDefaultDomain(TestInfo.DEFAULTNEWDOMAINNAME);
        d.addPassword(TestInfo.password);
        DomainConfiguration conf = d.getDefaultConfiguration();
        conf.addPassword(TestInfo.password);
        assertTrue("Configuration uses password",
                conf.usesPassword(TestInfo.PASSWORD_NAME));
        conf.removePassword(TestInfo.PASSWORD_NAME);
        assertFalse("Configuration should have password removed",
                conf.usesPassword(TestInfo.PASSWORD_NAME));
        try {
            conf.removePassword(TestInfo.PASSWORD_NAME);
            fail("Should not be able to remove unused password");
        } catch (UnknownID e) {
            // Expected
        }
    }

    /** Test that we can ask for the use of a password.
     * @throws Exception
     */
    public void testUsesPassword() throws Exception {
        Domain d = Domain.getDefaultDomain(TestInfo.DEFAULTNEWDOMAINNAME);
        d.addPassword(TestInfo.password);
        DomainConfiguration conf = d.getDefaultConfiguration();
        assertFalse("Configuration should not be using any passwords by default",
                conf.usesPassword(TestInfo.PASSWORD_NAME));
        conf.addPassword(TestInfo.password);
        assertTrue("Configuration uses password",
                conf.usesPassword(TestInfo.PASSWORD_NAME));
    }

    public void testGetExpectedNumberOfObjects() throws Exception {
        DomainConfiguration dc = Domain.getDefaultDomain("testdomain01.dk").getDefaultConfiguration();
        dc.setMaxObjects(4000);
        assertEquals("Unharvested config should expect half the limit of the "
                     + "configuration with no limit", 2000,
                     dc.getExpectedNumberOfObjects(-1L, -1L));
        assertEquals("Unharvested config should expect half the limit given",
                     3000,
                     dc.getExpectedNumberOfObjects(6000, -1L));

        Date d1 = new GregorianCalendar(1970, 01, 01).getTime();
        Date d2 = new GregorianCalendar(1980, 01, 01).getTime();

        addHistoryObject(dc, d1, 100L, 1L, StopReason.DOWNLOAD_COMPLETE);
        dc.setMaxObjects(1100);
        assertEquals("Completed harvest should add 10% of difference", 200L,
                     dc.getExpectedNumberOfObjects(-1L, -1L));

        dc = Domain.getDefaultDomain("testdomain02.dk").getDefaultConfiguration();
        addHistoryObject(dc, d1, 100L, 1L, StopReason.DOWNLOAD_COMPLETE);
        dc.setMaxObjects(1100);
        assertEquals("Completed harvest should add 10% of difference", 300L,
                     dc.getExpectedNumberOfObjects(2100L, -1L));

        dc = Domain.getDefaultDomain("testdomain03.dk").getDefaultConfiguration();
        addHistoryObject(dc, d1, 100L, 1L, StopReason.OBJECT_LIMIT);
        dc.setMaxObjects(1100);
        assertEquals("Unfinished harvest should add 50% of difference", 600L,
                     dc.getExpectedNumberOfObjects(-1L, -1L));

        dc.setMaxObjects(5000);
        assertEquals("Override flag should not affect lower limits", 1150L,
                dc.getExpectedNumberOfObjects(2200L, -1L));

        dc.setMaxObjects(200);
        assertEquals("Override flag should be a maximum of the end result", 200L,
                dc.getExpectedNumberOfObjects(2200L, -1L));

        dc.setMaxObjects(1000);
        assertEquals("Override flag should be a maximum of the end result", 1000L,
                dc.getExpectedNumberOfObjects(2200L, -1L));

        dc = Domain.getDefaultDomain("testdomain02.dk").getDefaultConfiguration();
        addHistoryObject(dc, d1, 100L, 1L, StopReason.SIZE_LIMIT);
        dc.setMaxObjects(1100);
        assertEquals("Unfinished harvest should add 50% of difference", 1100L,
                     dc.getExpectedNumberOfObjects(2100L, -1L));

        dc = Domain.getDefaultDomain("testdomain03.dk").getDefaultConfiguration();
        addHistoryObject(dc, d1, 200L, 1L, StopReason.DOWNLOAD_COMPLETE);
        addHistoryObject(dc, d2, 100L, 1L, StopReason.SIZE_LIMIT);
        assertEquals("Newer smaller unfinished harvest should not affect expectation", 400L,
                     dc.getExpectedNumberOfObjects(2200L, -1L));

        dc = Domain.getDefaultDomain("testdomain04.dk").getDefaultConfiguration();
        addHistoryObject(dc, d1, 200L, 1L, StopReason.DOWNLOAD_COMPLETE);
        addHistoryObject(dc, d2, 300L, 1L, StopReason.SIZE_LIMIT);
        dc.setMaxObjects(2300);
        assertEquals("Newer larger unfinished harvest should define expectation", 1300L,
                     dc.getExpectedNumberOfObjects(-1L, -1L));

        dc = Domain.getDefaultDomain("testdomain04.dk").getDefaultConfiguration();
        addHistoryObject(dc, d1, 300L, 1L, StopReason.SIZE_LIMIT);
        addHistoryObject(dc, d2, 200L, 1L, StopReason.DOWNLOAD_COMPLETE);
        assertEquals("Older larger unfinished harvest should not affect expectation", 400L,
                     dc.getExpectedNumberOfObjects(2200L, -1L));

        //Testing limits using expected object sizes from byte limits
        dc = Domain.getDefaultDomain("testdomain05.dk").getDefaultConfiguration();
        addHistoryObject(dc, d1, 40L, 400L, StopReason.SIZE_LIMIT);
        assertEquals("The expeected object size should be 38000 because only 40 objects were harvested",
                     85L,
                     dc.getExpectedNumberOfObjects(-1L, 5000000L));

        dc = Domain.getDefaultDomain("testdomain06.dk").getDefaultConfiguration();
        addHistoryObject(dc, d1, 100L, 1000L, StopReason.SIZE_LIMIT);
        assertEquals("The expeected object size should be 10 because 100 objects were harvested",
                     250050L,
                     dc.getExpectedNumberOfObjects(-1L, 5000000L));

        dc = Domain.getDefaultDomain("testdomain07.dk").getDefaultConfiguration();
        addHistoryObject(dc, d1, 1L, 1L, StopReason.SIZE_LIMIT);
        assertEquals("When heritrix writes 1/1 we shouldn't expect too many objects next time",
                     66L,
                     dc.getExpectedNumberOfObjects(-1L, 5000000L));

        dc = Domain.getDefaultDomain("testdomain08.dk").getDefaultConfiguration();
        addHistoryObject(dc, d1, 10L, 10000000L, StopReason.SIZE_LIMIT);
        assertEquals("Even on small harvests we should trust large expectations",
                     5L,
                     dc.getExpectedNumberOfObjects(-1L, 5000000L));
    }

    /** Test that adding seedlists to domainconfigs doesn't confuse the domain. */
    public void testAddSeedlist() {
        Domain d = TestInfo.getDefaultDomain();
        SeedList seedlist = d.getAllSeedLists().next();
        SeedList copy = new SeedList(seedlist.getName(), seedlist.getSeeds());
        DomainConfiguration dc = d.getAllConfigurations().next();
        final ArrayList<SeedList> seedlists = new ArrayList<SeedList>();
        seedlists.add(copy);
        dc.setSeedLists(seedlists); // Should work
        assertSame("Should have domains seedlist, not the copy",
                seedlist, dc.getSeedLists().next());
        try {
            copy = new SeedList("badname", seedlist.getSeeds());
            dc.addSeedList(copy);
            fail("Should not accept an unknown seedlist");
        } catch (UnknownID e) {
            //expected
        }
        try {
            final ArrayList<String> seeds = new ArrayList<String>();
            seeds.add("foobarbaz");
            copy = new SeedList(seedlist.getName(), seeds);
            dc.addSeedList(copy);
            fail("Should not accept an empty seedlist");
        } catch (PermissionDenied e) {
            //expected
        }
    }

    /** Test that adding passwords to domainconfigs doesn't confuse the domain. */
    public void testAddPassword() {
        Domain d = TestInfo.getDefaultDomain();
        Password password = new Password("test", "no comment",
                "domain of evil", "realm of hades", "morgoth", "666");
        d.addPassword(password);
        Password copy = new Password(password.getName(), password.getComments(),
                password.getPasswordDomain(), password.getRealm(),
                password.getUsername(), password.getPassword());
        DomainConfiguration dc = d.getAllConfigurations().next();
        final ArrayList<Password> passwords = new ArrayList<Password>();
        passwords.add(copy);
        dc.setPasswords(passwords); // Should work
        assertSame("Should have domains password, not the copy",
                password, dc.getPasswords().next());
        try {
            copy = new Password("badname", password.getComments(),
                    password.getPasswordDomain(), password.getRealm(),
                    password.getUsername(), password.getPassword());
            dc.addPassword(copy);
            fail("Should not accept an unknown password");
        } catch (UnknownID e) {
            //expected
        }
        try {
            final ArrayList<String> seeds = new ArrayList<String>();
            seeds.add("foobarbaz");
            copy = new Password(password.getName(), password.getComments(),
                    password.getPasswordDomain(), password.getRealm(),
                    password.getUsername(), "TrustEveryOne");
            dc.addPassword(copy);
            fail("Should not accept an wrong password");
        } catch (PermissionDenied e) {
            //expected
        }
    }

    private void addHistoryObject(DomainConfiguration dc, Date d1,
                                  final long countObjectRetrieved,
                                  long sizeDataRetrieved,
                                  final StopReason stopReason
    ) {
        dc.addHarvestInfo
                (new HarvestInfo(new Long(1L), dc.getDomain().getName(),
                        dc.getName(), d1, sizeDataRetrieved, countObjectRetrieved, stopReason));
    }

}

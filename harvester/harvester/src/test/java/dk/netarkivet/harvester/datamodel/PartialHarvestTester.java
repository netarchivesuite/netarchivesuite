/*$Id$
* $Revision$
* $Date$
* $Author$
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.IteratorUtils;

/**
 * Test cases specific to the PartialHarvest class.
 */
public class PartialHarvestTester extends DataModelTestCase {
    private PartialHarvest harvest;
    private static final String harvestName = "Event Harvest";
    private static final String order1xml = "default_orderxml";
    private static final String order2xml = "OneLevel-order";

    public PartialHarvestTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        Schedule sched = ScheduleDAO.getInstance().read("DefaultSchedule");
        harvest = new PartialHarvest(new ArrayList<DomainConfiguration>(), sched, harvestName, "");
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test that adding a single seed results in creation of all the
     * appropriate objects.
     */
    public void testAddSeedsSimpleCase() {
        String seedlist = "http://www.mydomain.dk/page1.jsp?aparam=avalue";
        final long maxbytes = 20000L;
        harvest.addSeeds(seedlist, order1xml, maxbytes);
        PartialHarvest updatedHarvest =
                (PartialHarvest) HarvestDefinitionDAO.getInstance().
                getHarvestDefinition(harvestName);
        String expectedName = harvestName +"_" + order1xml + "_" + maxbytes 
            + "Bytes";
        
        Iterator<DomainConfiguration> dci = updatedHarvest.getDomainConfigurations();
        DomainConfiguration dc = dci.next();
        assertEquals("DomainConfiguration should have expected name, ", expectedName, dc.getName());
        assertEquals("Should have expected domain name", "mydomain.dk", dc.getDomain().getName());
        assertEquals("Should have expected byte limits", maxbytes, dc.getMaxBytes());
        Iterator<SeedList> si = dc.getSeedLists();
        SeedList sl = si.next();
        assertEquals("Should have expected seedlist name", expectedName, sl.getName());
        assertEquals("Seedlist should contain specified URL", seedlist, sl.getSeedsAsString().trim());
        //Should be no more domainconfigurations or seedlists
        assertFalse("Should only be one configuration in the harvest", dci.hasNext());
        assertFalse("Should only be one seedlist in the configuration", si.hasNext());
    }

    /**
     * Test that adding a single seed results in creation of all the
     * appropriate objects.
     */
    public void testAddSeedsInvalid() {
        String seedlist = "http://www.x.dk/page1.jsp?aparam=avalue\n"
                          + "http:// /\n"
                          + "www\n"
                          + "www x\n"
                          + "http://a.b//\n"
                          + "http://x.y/ /";
        final long maxbytes = 30000L;
        try {
            harvest.addSeeds(seedlist, order1xml, maxbytes);
            fail("Should fail on wrong seeds");
        } catch (ArgumentNotValid e) {
            assertTrue("Wrong seeds must be in message: " + e,
                       e.getMessage().contains("http:// /"));
            assertTrue("Wrong seeds must be in message: " + e,
                       e.getMessage().contains("http://www x"));
            assertTrue("Wrong seeds must be in message: " + e,
                       e.getMessage().contains("http://x.y/ /"));
        }
        PartialHarvest updatedHarvest =
                (PartialHarvest) HarvestDefinitionDAO.getInstance().
                getHarvestDefinition(harvestName);
        assertNull("No harvest should be generated", updatedHarvest);
    }

    /**
     * Tests
     * a) Non-default values for maxLoad, maxObjects
     * b) That omitting "http://" is not a problem
     */
    public void testAddSeedsNonDefaultValues() {
        String seedlist = "www.mydomain.dk/page1.jsp?aparam=avalue";
        final long maxbytes = -1L; // unlimited
        harvest.addSeeds(seedlist, order1xml, maxbytes);
        PartialHarvest updatedHarvest =
                (PartialHarvest) HarvestDefinitionDAO.getInstance().
                getHarvestDefinition(harvestName);
        String expectedName = harvestName + "_" + order1xml + "_" + "UnlimitedBytes";
        
        Iterator<DomainConfiguration> dci = updatedHarvest.getDomainConfigurations();
        DomainConfiguration dc = dci.next();
        assertEquals("DomainConfiguration should have expected name, ", 
                expectedName, dc.getName());
        assertEquals("Should have expected domain name", "mydomain.dk", 
                dc.getDomain().getName());
        assertEquals("Should have expected byte limits", 
                maxbytes, dc.getMaxBytes());
        Iterator<SeedList> si = dc.getSeedLists();
        SeedList sl = si.next();
        assertEquals("Should have expected seedlist name", 
                expectedName, sl.getName());
        assertEquals("Seedlist should contain specified URL", 
                "http://" + seedlist, sl.getSeedsAsString().trim());
        //Should be no more domainconfigurations or seedlists
        assertFalse("Should only be one configuration in the harvest", dci.hasNext());
        assertFalse("Should only be one seedlist in the configuration", si.hasNext());
    }

    /**
     * Checks that
     * a) parsing of subdomains is ok and
     * b) https is supported
     */
    public void testAddSeedsWithSubdomain() {
        String seedlist = "https://www.asubdomain.mydomain.dk/page1.jsp?aparam=avalue";
        final long maxbytes = 50000L;
        harvest.addSeeds(seedlist, order1xml, maxbytes);
        PartialHarvest updatedHarvest =
                (PartialHarvest) HarvestDefinitionDAO.getInstance().
                getHarvestDefinition(harvestName);
        String expectedName = harvestName + "_" + order1xml + "_"
            + maxbytes + "Bytes";
        //
        Iterator<DomainConfiguration> dci = updatedHarvest.getDomainConfigurations();
        DomainConfiguration dc = dci.next();
        assertEquals("DomainConfiguration should have expected name, ", expectedName, dc.getName());
        assertEquals("Should have expected domain name", "mydomain.dk", dc.getDomain().getName());
        assertEquals("Should have expected byte limits", maxbytes, dc.getMaxBytes());
        Iterator<SeedList> si = dc.getSeedLists();
        SeedList sl = si.next();
        assertEquals("Should have expected seedlist name", expectedName, sl.getName());
        assertEquals("Seedlist should contain specified URL", seedlist, sl.getSeedsAsString().trim());
        //Should be no more domainconfigurations or seedlists
        assertFalse("Should only be one configuration in the harvest", dci.hasNext());
        assertFalse("Should only be one seedlist in the configuration", si.hasNext());
    }

    /**
     * Test that we can correctly process a seedlist with multiple entries.
     */
    public void testAddComplexSeedlist() {
        String seedlist =
                "\thttps://www.asubdomain.mydomain.dk/page1.jsp?aparam=avalue\n"+
                "www.anewdomain.dk/index.html  \n" +
                "www.mydomain.dk/page2.jsp ";
        final long maxbytes = 60000L;
        harvest.addSeeds(seedlist, order1xml, maxbytes);
        PartialHarvest updatedHarvest =
                (PartialHarvest) HarvestDefinitionDAO.getInstance().
                getHarvestDefinition(harvestName);
        String expectedName = harvestName + "_" + order1xml + "_"
            + maxbytes + "Bytes";
        // Should be two configurations
        Iterator<DomainConfiguration> dci = updatedHarvest.getDomainConfigurations();
        DomainConfiguration dc1 = dci.next();
        DomainConfiguration dc2 = dci.next();
        assertFalse("Should be exactly two configurations", dci.hasNext());
        assertEquals("Configuration should have expected name", expectedName, dc1.getName());
        assertEquals("Configuration should have expected name", expectedName, dc2.getName());
        assertEquals("Should have expected byte limits", maxbytes, dc1.getMaxBytes());
        assertEquals("Should have expected byte limits", maxbytes, dc2.getMaxBytes());
        String name1 = dc1.getDomain().getName();
        String name2 = dc2.getDomain().getName();
        boolean order1 = name1.equals("mydomain.dk") && name2.equals("anewdomain.dk");
        boolean order2 = name2.equals("mydomain.dk") && name1.equals("anewdomain.dk");
        assertTrue("The two domains should correspond to the configurations", order1 || order2);
        // Check that mydomain config contains both seeds
        DomainConfiguration myDomainConfig = null;
        DomainConfiguration newDomainConfig = null;
        if (order1) {
            myDomainConfig = dc1;
            newDomainConfig = dc2;
        } else {
            myDomainConfig = dc2;
            newDomainConfig = dc1;
        }
        // Check mydomain seedlist
        Iterator<SeedList> mdsli =  myDomainConfig.getSeedLists();
        SeedList seedList = mdsli.next();
        assertFalse("Should be one seedlist", mdsli.hasNext());
        List<String> seeds = seedList.getSeeds();
        assertTrue("Should contain expected seeds", seeds.contains("https://www.asubdomain.mydomain.dk/page1.jsp?aparam=avalue"));
        assertTrue("Should contain expected seeds", seeds.contains("http://www.mydomain.dk/page2.jsp"));
        assertEquals("Seedlist should have two entries", 2, seeds.size());
        //check newdomain seedlist
        Iterator<SeedList> ndsli =  newDomainConfig.getSeedLists();
        assertFalse("Should be one seedlist", mdsli.hasNext());
        seedList = ndsli.next();
        seeds = seedList.getSeeds();
        assertTrue("Should contain expected seed", seeds.contains("http://www.anewdomain.dk/index.html"));
        assertEquals("Seedlist should have one entry", 1, seeds.size());

    }

    /**
     * test that we can call addSeeds() multiple times and both update existing
     * configurations and add new ones
     */
    public void testAddSeedsMultipleAdds() {
        String list1 = "www.1.dk\nwww.2.dk/index.jsp\nwww.3.dk";
        String list2 = "http://www.1.dk/private\nwww.4.dk\nwww.3.dk/private";
        String list3 = "www.2.dk/images\n www.4.dk/images\nwww.3.dk/images";
        final long maxbytes1 = 70000L;
        final long maxbytes2 = 80000L;
        String name1 = harvestName + "_" + order1xml + "_" + maxbytes1 + "Bytes";
        String name2 = harvestName + "_" + order2xml + "_" + maxbytes2 + "Bytes";
        harvest.addSeeds(list1, order1xml, maxbytes1);
        harvest = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition(harvestName);
        harvest.addSeeds(list2, order2xml, maxbytes2);
        harvest = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition(harvestName);
        harvest.addSeeds(list3, order1xml, maxbytes1);
        //
        // So now we have the following:
        // www.1.dk has two configurations name1 and name2, each with one seed
        // www.2.dk has one configuration, name1, with two seeds
        // www.3.dk has two configurations, name1 with two seeds and name2 with one seed
        // www.4.dk has two configurations with one seed in each
        // Therefore the harvest as a whole has seven configurations, 4 called name1
        // and three called name2
        PartialHarvest updatedHarvest =
                (PartialHarvest) HarvestDefinitionDAO.getInstance().
                getHarvestDefinition(harvestName);
        Iterator<DomainConfiguration> dci = updatedHarvest.getDomainConfigurations();
        Set<DomainConfiguration> dcs = new HashSet<DomainConfiguration>();
        while (dci.hasNext()) {
            dcs.add(dci.next());
        }
        assertEquals("Should have seven configurations", 7, dcs.size());
        int countName1 = 0;
        int countName2 = 0;
        for (DomainConfiguration dc: dcs) {
            if (dc.getName().equals(name1)) {
                assertEquals("Should have right max bytes", maxbytes1, dc.getMaxBytes());
            }
            if (dc.getName().equals(name2)) {
                assertEquals("Should have right max bytes", maxbytes2, dc.getMaxBytes());
            }
            if (dc.getName().equals(name1) && dc.getDomain().getName().equals("1.dk")) {
                countName1++;
                assertEquals("Should have one seed", 1, dc.getSeedLists().next().getSeeds().size());
            }
            if (dc.getName().equals(name2) && dc.getDomain().getName().equals("1.dk")) {
                countName2++;
                assertEquals("Should have one seed", 1, dc.getSeedLists().next().getSeeds().size());
            }
            if (dc.getName().equals(name1) && dc.getDomain().getName().equals("2.dk")) {
                countName1++;
                assertEquals("Should have two seeds", 2, dc.getSeedLists().next().getSeeds().size());
            }
            if (dc.getName().equals(name2) && dc.getDomain().getName().equals("2.dk")) {
                throw new UnknownID("Did not expect configuration with name " + name2 + "for 2.dk");
            }
            if (dc.getName().equals(name1) && dc.getDomain().getName().equals("3.dk")) {
                countName1++;
                assertEquals("Should have two seeds", 2, dc.getSeedLists().next().getSeeds().size());
            }
            if (dc.getName().equals(name2) && dc.getDomain().getName().equals("3.dk")) {
                countName2++;
                assertEquals("Should have one seed", 1, dc.getSeedLists().next().getSeeds().size());
            }
            if (dc.getName().equals(name1) && dc.getDomain().getName().equals("4.dk")) {
                countName1++;
                assertEquals("Should have one seed", 1, dc.getSeedLists().next().getSeeds().size());
            }
            if (dc.getName().equals(name2) && dc.getDomain().getName().equals("4.dk")) {
                countName2++;
                assertEquals("Should have one seed", 1, dc.getSeedLists().next().getSeeds().size());
            }
        }
        assertEquals("Should be 4 configurations with name " + name1, 4, countName1);
        assertEquals("Should be 3 configurations with name " + name2, 3, countName2);
    }

    /** Test that setting domain configurations actually removes dups.
     * @throws Exception
     */
    public void testSetDomainConfigurations() throws Exception {
        List<DomainConfiguration> nodupslist =
                IteratorUtils.toList(harvest.getDomainConfigurations());

        assertEquals("Should have no entries in domain config list from start",
                0, nodupslist.size());

        DomainDAO ddao = DomainDAO.getInstance();
        Domain d = ddao.read("netarkivet.dk");
        DomainConfiguration dc = d.getDefaultConfiguration();
        List<DomainConfiguration> dclist = new ArrayList<DomainConfiguration>();
        dclist.add(dc);
        dclist.add(dc);
        harvest.setDomainConfigurations(dclist);
        nodupslist = IteratorUtils.toList(harvest.getDomainConfigurations());

        assertEquals("Should have 1 entry in domain config list after adding same twice",
                1, nodupslist.size());

        Domain d2 = ddao.read("netarkivet.dk");
        ddao.update(d2);
        d = ddao.read("netarkivet.dk");

        nodupslist.add(d.getDefaultConfiguration());
        nodupslist.add(dc);

        harvest.setDomainConfigurations(nodupslist);

        nodupslist = IteratorUtils.toList(harvest.getDomainConfigurations());

        assertEquals("Should have 1 entry in domain config list after adding"
                + " same after updates",
                1, nodupslist.size());

        List<DomainConfiguration> list = Collections.emptyList();
        harvest.setDomainConfigurations(list);

        nodupslist = IteratorUtils.toList(harvest.getDomainConfigurations());
        assertEquals("Should have no entries after setting to empty list",
                0, nodupslist.size());
    }
        /**
     * test that we can call addSeeds() multiple times and both update existing
     * configurations and add new ones
     */
    public void testAddSeedsMiscNewlines() {
        String list1 = "www.1.dk\n\rwww.2.dk/index.jsp\n\nwww.3.dk\r\n3.dk/test\r"
                       + "3.dk/tyst";
        final long maxbytes = 90000L;
        harvest.addSeeds(list1, order1xml, maxbytes);
        //
        // So now we have the following:
        // www.1.dk has one configuration, with one seed
        // www.2.dk has one configuration, with one seed
        // www.3.dk has one configuration, with three seeds
        PartialHarvest updatedHarvest =
                (PartialHarvest) HarvestDefinitionDAO.getInstance().
                getHarvestDefinition(harvestName);
        Iterator<DomainConfiguration> dcs = updatedHarvest.getDomainConfigurations();
        DomainConfiguration dc;
        int count = 0;
        while (dcs.hasNext()) {
            count++;
            dc = dcs.next();
            if (dc.getDomain().getName().equals("1.dk")) {
                assertEquals("Should have one seed", 1, dc.getSeedLists().next().getSeeds().size());
            }
            if (dc.getDomain().getName().equals("2.dk")) {
                assertEquals("Should have one seed", 1, dc.getSeedLists().next().getSeeds().size());
            }
            if (dc.getDomain().getName().equals("3.dk")) {
                assertEquals("Should have three seeds", 3, dc.getSeedLists().next().getSeeds().size());
            }
            assertEquals("Should have right max bytes", maxbytes, dc.getMaxBytes());
        }
        assertEquals("Three domains shopuld be added", 3, count);
    }
}
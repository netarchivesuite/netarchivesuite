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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.IteratorUtils;
import dk.netarkivet.common.utils.SlowTest;

/**
 * Test cases specific to the PartialHarvest class.
 */
public class PartialHarvestTester extends DataModelTestCase {
    private PartialHarvest harvest;
    private static final String harvestName = "Event Harvest";
    private static final String order1xml = "default_orderxml";
    private static final String order2xml = "OneLevel-order";
    private static final Map<String,String> attributeValues = new HashMap<String,String>();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Schedule sched = ScheduleDAO.getInstance().read("DefaultSchedule");
        harvest = new PartialHarvest(new ArrayList<DomainConfiguration>(), sched, harvestName, "", "Everybody");
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test that adding a single seed results in creation of all the appropriate objects.
     */
    @Category(SlowTest.class)
    @Test
    public void testAddSeedsSimpleCase() {
        Set<String> seedlist = new HashSet<String>();
        seedlist.add("http://www.mydomain.dk/page1.jsp?aparam=avalue");
 
        final long maxbytes = 20000L;
        final int maxobjects = -1;
        harvest.addSeeds(seedlist, order1xml, maxbytes, maxobjects, attributeValues);
        PartialHarvest updatedHarvest = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition(
                harvestName);
        String expectedName = harvestName + "_" + order1xml + "_" + maxbytes + "Bytes" + "_UnlimitedObjects";

        Iterator<DomainConfiguration> dci = updatedHarvest.getDomainConfigurations();
        DomainConfiguration dc = dci.next();
        assertEquals("DomainConfiguration should have expected name, ", expectedName, dc.getName());
        assertEquals("Should have expected domain name", "mydomain.dk", dc.getDomainName());
        assertEquals("Should have expected byte limits", maxbytes, dc.getMaxBytes());
        Iterator<SeedList> si = dc.getSeedLists();
        SeedList sl = si.next();
        assertEquals("Should have expected seedlist name", expectedName, sl.getName());
        assertTrue("Seedlist should contain specified URL", seedlist.contains(sl.getSeedsAsString().trim()));
        // Should be no more domainconfigurations or seedlists
        assertFalse("Should only be one configuration in the harvest", dci.hasNext());
        assertFalse("Should only be one seedlist in the configuration", si.hasNext());
    }

    /**
     * Test names of seedlist with max bytes and max object defined.
     */
    @Category(SlowTest.class)
    @Test
    public void testMixedLimitsSeedlistNames() {
        Set<String> seedlist = new HashSet<String>();
        seedlist.add("http://www.mydomain.dk/page1.jsp?aparam=avalue");
        final long maxbytes = 1024L;
        final int maxobjects = 250;
        harvest.addSeeds(seedlist, order1xml, maxbytes, maxobjects, attributeValues);
        PartialHarvest updatedHarvest = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition(
                harvestName);

        // maxbytes and max objects defined
        String expectedName = harvestName + "_" + order1xml + "_" + maxbytes + "Bytes" + "_" + maxobjects + "Objects";

        Iterator<DomainConfiguration> dci = updatedHarvest.getDomainConfigurations();
        DomainConfiguration dc = dci.next();
        assertEquals("DomainConfiguration should have expected name, ", expectedName, dc.getName());
        assertEquals("Should have expected byte limits", maxbytes, dc.getMaxBytes());
        assertEquals("Should have expected objects limits", maxobjects, dc.getMaxObjects());
    }

    /**
     * Test that adding a single seed results in creation of all the appropriate objects.
     */
    @Category(SlowTest.class)
    @Test
    public void testAddSeedsInvalid() {
        Set<String> seedlist = new HashSet<String>();
        String badSeed1 = "http:// /";
        String badSeed2 = "www x";
        String badSeed3 = "http://x.y/ /";
        seedlist.add("http://www.x.dk/page1.jsp?aparam=avalue");
        seedlist.add(badSeed1);
        seedlist.add("www");
        seedlist.add(badSeed2);
        seedlist.add("http://a.b//");
        seedlist.add(badSeed3);

        final long maxbytes = 30000L;
        final int maxobjects = -1;
        Set<String> illegalSeeds = harvest.addSeeds(seedlist, order1xml, maxbytes, maxobjects, attributeValues);
        assertTrue("Wrong seed '" + badSeed1 + "' must be in illegalSeeds list ", illegalSeeds.contains(badSeed1));
        assertTrue("Wrong seed '" + badSeed2 + "' must be in illegalSeeds list ", illegalSeeds.contains(badSeed2));
        assertTrue("Wrong seed '" + badSeed3 + "' must be in illegalSeeds list ", illegalSeeds.contains(badSeed3));
        
        PartialHarvest updatedHarvest = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition(
                harvestName);
        assertNotNull("No harvest should be generated", updatedHarvest);
    }

    /**
     * Tests a) Non-default values for maxLoad, maxObjects b) That omitting "http://" is not a problem
     */
    @Category(SlowTest.class)
    @Test
    public void testAddSeedsNonDefaultValues() {
        Set<String> seedlist = new HashSet<String>();
        seedlist.add("www.mydomain.dk/page1.jsp?aparam=avalue");
        final long maxbytes = -1L; // unlimited
        final int maxobjects = -1;

        harvest.addSeeds(seedlist, order1xml, maxbytes, maxobjects, attributeValues);
        PartialHarvest updatedHarvest = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition(
                harvestName);
        String expectedName = harvestName + "_" + order1xml + "_" + "UnlimitedBytes" + "_UnlimitedObjects";

        Iterator<DomainConfiguration> dci = updatedHarvest.getDomainConfigurations();
        DomainConfiguration dc = dci.next();
        assertEquals("DomainConfiguration should have expected name, ", expectedName, dc.getName());
        assertEquals("Should have expected domain name", "mydomain.dk", dc.getDomainName());
        assertEquals("Should have expected byte limits", maxbytes, dc.getMaxBytes());
        Iterator<SeedList> si = dc.getSeedLists();
        SeedList sl = si.next();
        assertEquals("Should have expected seedlist name", expectedName, sl.getName());
        assertTrue("Should only contain one seed, but has " + seedlist.size(), seedlist.size() == 1);
        Object[] seedsAsArray = seedlist.toArray();
        assertEquals("Seedlist should contain specified URL", "http://" + seedsAsArray[0], sl.getSeedsAsString().trim());
        // Should be no more domainconfigurations or seedlists
        assertFalse("Should only be one configuration in the harvest", dci.hasNext());
        assertFalse("Should only be one seedlist in the configuration", si.hasNext());
    }

    /**
     * Checks that a) parsing of subdomains is ok and b) https is supported
     */
    @Category(SlowTest.class)
    @Test
    public void testAddSeedsWithSubdomain() {
        Set<String> seedlist = new HashSet<String>();
        seedlist.add("https://www.asubdomain.mydomain.dk/page1.jsp?aparam=avalue");
        final long maxbytes = 50000L;
        final int maxobjects = -1;

        harvest.addSeeds(seedlist, order1xml, maxbytes, maxobjects, attributeValues);
        PartialHarvest updatedHarvest = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition(
                harvestName);
        String expectedName = harvestName + "_" + order1xml + "_" + maxbytes + "Bytes" + "_UnlimitedObjects";
        //
        Iterator<DomainConfiguration> dci = updatedHarvest.getDomainConfigurations();
        DomainConfiguration dc = dci.next();
        assertEquals("DomainConfiguration should have expected name, ", expectedName, dc.getName());
        assertEquals("Should have expected domain name", "mydomain.dk", dc.getDomainName());
        assertEquals("Should have expected byte limits", maxbytes, dc.getMaxBytes());
        Iterator<SeedList> si = dc.getSeedLists();
        SeedList sl = si.next();
        assertEquals("Should have expected seedlist name", expectedName, sl.getName());
        assertTrue("Seedlist should contain specified URL", seedlist.contains(sl.getSeedsAsString().trim()));
        // Should be no more domainconfigurations or seedlists
        assertFalse("Should only be one configuration in the harvest", dci.hasNext());
        assertFalse("Should only be one seedlist in the configuration", si.hasNext());
    }

    /**
     * Test that we can correctly process a seedlist with multiple entries.
     */
    @Category(SlowTest.class)
    @Test
    public void testAddComplexSeedlist() {
        Set<String> seedlist = new HashSet<String>();
        seedlist.add("\thttps://www.asubdomain.mydomain.dk/page1.jsp?aparam=avalue");
        seedlist.add("www.anewdomain.dk/index.html  ");
        seedlist.add("www.mydomain.dk/page2.jsp ");

        final long maxbytes = 60000L;
        final int maxobjects = -1;

        harvest.addSeeds(seedlist, order1xml, maxbytes, maxobjects, attributeValues);
        PartialHarvest updatedHarvest = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition(
                harvestName);
        String expectedName = harvestName + "_" + order1xml + "_" + maxbytes + "Bytes" + "_UnlimitedObjects";
        // Should be two configurations
        Iterator<DomainConfiguration> dci = updatedHarvest.getDomainConfigurations();
        DomainConfiguration dc1 = dci.next();
        DomainConfiguration dc2 = dci.next();
        assertFalse("Should be exactly two configurations", dci.hasNext());
        assertEquals("Configuration should have expected name", expectedName, dc1.getName());
        assertEquals("Configuration should have expected name", expectedName, dc2.getName());
        assertEquals("Should have expected byte limits", maxbytes, dc1.getMaxBytes());
        assertEquals("Should have expected byte limits", maxbytes, dc2.getMaxBytes());
        String name1 = dc1.getDomainName();
        String name2 = dc2.getDomainName();
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
        Iterator<SeedList> mdsli = myDomainConfig.getSeedLists();
        SeedList seedList = mdsli.next();
        assertFalse("Should be one seedlist", mdsli.hasNext());
        List<String> seeds = seedList.getSeeds();
        assertTrue("Should contain expected seeds",
                seeds.contains("https://www.asubdomain.mydomain.dk/page1.jsp?aparam=avalue"));
        assertTrue("Should contain expected seeds", seeds.contains("http://www.mydomain.dk/page2.jsp"));
        assertEquals("Seedlist should have two entries", 2, seeds.size());
        // check newdomain seedlist
        Iterator<SeedList> ndsli = newDomainConfig.getSeedLists();
        assertFalse("Should be one seedlist", mdsli.hasNext());
        seedList = ndsli.next();
        seeds = seedList.getSeeds();
        assertTrue("Should contain expected seed", seeds.contains("http://www.anewdomain.dk/index.html"));
        assertEquals("Seedlist should have one entry", 1, seeds.size());

    }

    /**
     * test that we can call addSeeds() multiple times and both update existing configurations and add new ones
     * FIXME ignored at time of NAS-5.1, as the attribute system makes it unpractical to add seeds multiple times with the same template, max object, max bytes
     * As we should also consider attributes now.
     * And BTW it is a rare occasion that we need this anyway
     */
    @Category(SlowTest.class)
    @Test
    @Ignore
    public void testAddSeedsMultipleAdds() {
        Set<String> list1 = new HashSet<String>();
        list1.add("www.1.dk\n");
        list1.add("www.2.dk/index.jsp\n");
        list1.add("www.1.dk\n");
        list1.add("www.3.dk");

        Set<String> list2 = new HashSet<String>();
        list2.add("http://www.1.dk/private\n");
        list2.add("www.4.dk\n");
        list2.add("www.3.dk/private");

        Set<String> list3 = new HashSet<String>();
        list3.add("www.2.dk/images\n");
        list3.add(" www.4.dk/images\n");
        list3.add("www.3.dk/images");

        final long maxbytes1 = 70000L;
        final long maxbytes2 = 80000L;
        final int maxobjects = -1;

        String name1 = harvestName + "_" + order1xml + "_" + maxbytes1 + "Bytes" + "_UnlimitedObjects";
        String name2 = harvestName + "_" + order2xml + "_" + maxbytes2 + "Bytes" + "_UnlimitedObjects";
        harvest.addSeeds(list1, order1xml, maxbytes1, maxobjects, attributeValues);
        harvest = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition(harvestName);
        harvest.addSeeds(list2, order2xml, maxbytes2, maxobjects, attributeValues);
        harvest = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition(harvestName);
        harvest.addSeeds(list3, order1xml, maxbytes1, maxobjects, attributeValues);
        //
        // So now we have the following:
        // www.1.dk has two configurations name1 and name2, each with one seed
        // www.2.dk has one configuration, name1, with two seeds
        // www.3.dk has two configurations, name1 with two seeds and name2 with one seed
        // www.4.dk has two configurations with one seed in each
        // Therefore the harvest as a whole has seven configurations, 4 called name1
        // and three called name2
        PartialHarvest updatedHarvest = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition(
                harvestName);
        Iterator<DomainConfiguration> dci = updatedHarvest.getDomainConfigurations();
        Set<DomainConfiguration> dcs = new HashSet<DomainConfiguration>();
        while (dci.hasNext()) {
            dcs.add(dci.next());
        }
        
        assertEquals("Should have seven configurations", 7, dcs.size());
        int countName1 = 0;
        int countName2 = 0;
        for (DomainConfiguration dc : dcs) {
            if (dc.getName().equals(name1)) {
                assertEquals("Should have right max bytes", maxbytes1, dc.getMaxBytes());
            }
            if (dc.getName().equals(name2)) {
                assertEquals("Should have right max bytes", maxbytes2, dc.getMaxBytes());
            }
            if (dc.getName().equals(name1) && dc.getDomainName().equals("1.dk")) {
                countName1++;
                assertEquals("Should have one seed", 1, dc.getSeedLists().next().getSeeds().size());
            }
            if (dc.getName().equals(name2) && dc.getDomainName().equals("1.dk")) {
                countName2++;
                assertEquals("Should have one seed", 1, dc.getSeedLists().next().getSeeds().size());
            }
            if (dc.getName().equals(name1) && dc.getDomainName().equals("2.dk")) {
                countName1++;
                assertEquals("Should have two seeds", 2, dc.getSeedLists().next().getSeeds().size());
            }
            if (dc.getName().equals(name2) && dc.getDomainName().equals("2.dk")) {
                throw new UnknownID("Did not expect configuration with name " + name2 + "for 2.dk");
            }
            if (dc.getName().equals(name1) && dc.getDomainName().equals("3.dk")) {
                countName1++;
                assertEquals("Should have two seeds", 2, dc.getSeedLists().next().getSeeds().size());
            }
            if (dc.getName().equals(name2) && dc.getDomainName().equals("3.dk")) {
                countName2++;
                assertEquals("Should have one seed", 1, dc.getSeedLists().next().getSeeds().size());
            }
            if (dc.getName().equals(name1) && dc.getDomainName().equals("4.dk")) {
                countName1++;
                assertEquals("Should have one seed", 1, dc.getSeedLists().next().getSeeds().size());
            }
            if (dc.getName().equals(name2) && dc.getDomainName().equals("4.dk")) {
                countName2++;
                assertEquals("Should have one seed", 1, dc.getSeedLists().next().getSeeds().size());
            }
        }
        assertEquals("Should be 4 configurations with name " + name1, 4, countName1);
        assertEquals("Should be 3 configurations with name " + name2, 3, countName2);
    }

    /**
     * Test that setting domain configurations actually removes duplicates.
     */
    @Category(SlowTest.class)
    @Test
    public void testSetDomainConfigurations() throws Exception {
        List<DomainConfiguration> nodupslist = IteratorUtils.toList(harvest.getDomainConfigurations());

        assertEquals("Should have no entries in domain config list from start", 0, nodupslist.size());

        DomainDAO ddao = DomainDAO.getInstance();
        Domain d = ddao.read("netarkivet.dk");
        DomainConfiguration dc = d.getDefaultConfiguration();
        List<DomainConfiguration> dclist = new ArrayList<DomainConfiguration>();
        dclist.add(dc);
        dclist.add(dc);
        harvest.setDomainConfigurations(dclist);
        nodupslist = IteratorUtils.toList(harvest.getDomainConfigurations());

        assertEquals("Should have 1 entry in domain config list after adding same twice", 1, nodupslist.size());

        Domain d2 = ddao.read("netarkivet.dk");
        ddao.update(d2);
        d = ddao.read("netarkivet.dk");

        nodupslist.add(d.getDefaultConfiguration());
        nodupslist.add(dc);

        harvest.setDomainConfigurations(nodupslist);

        nodupslist = IteratorUtils.toList(harvest.getDomainConfigurations());

        assertEquals("Should have 1 entry in domain config list after adding" + " same after updates", 1,
                nodupslist.size());

        List<DomainConfiguration> list = Collections.emptyList();
        harvest.setDomainConfigurations(list);

        nodupslist = IteratorUtils.toList(harvest.getDomainConfigurations());
        assertEquals("Should have no entries after setting to empty list", 0, nodupslist.size());
    }

    /**
     * test that we can call addSeeds() multiple times and both update existing configurations and add new ones
     */
    @Category(SlowTest.class)
    @Test
    public void testAddSeedsMiscNewlines() {
        Set<String> list1 = new HashSet<String>();
        list1.add("www.1.dk\n\r");
        list1.add("www.2.dk/index.jsp\n\n");
        list1.add("www.3.dk\r\n");
        list1.add("3.dk/test\r");
        list1.add("3.dk/tyst");

        final long maxbytes = 90000L;
        final int maxobjects = -1;

        harvest.addSeeds(list1, order1xml, maxbytes, maxobjects, attributeValues);
        //
        // So now we have the following:
        // www.1.dk has one configuration, with one seed
        // www.2.dk has one configuration, with one seed
        // www.3.dk has one configuration, with three seeds
        PartialHarvest updatedHarvest = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition(
                harvestName);
        Iterator<DomainConfiguration> dcs = updatedHarvest.getDomainConfigurations();
        DomainConfiguration dc;
        int count = 0;
        while (dcs.hasNext()) {
            count++;
            dc = dcs.next();
            if (dc.getDomainName().equals("1.dk")) {
                assertEquals("Should have one seed", 1, dc.getSeedLists().next().getSeeds().size());
            }
            if (dc.getDomainName().equals("2.dk")) {
                assertEquals("Should have one seed", 1, dc.getSeedLists().next().getSeeds().size());
            }
            if (dc.getDomainName().equals("3.dk")) {
                assertEquals("Should have three seeds", 3, dc.getSeedLists().next().getSeeds().size());
            }
            assertEquals("Should have right max bytes", maxbytes, dc.getMaxBytes());
        }
        assertEquals("Three domains shopuld be added", 3, count);
    }

    /**
     * Verify that you can delete only DomainConfiguration from the list of DomainConfigurations associated with a
     * PartialHarvest.
     */
    @Category(SlowTest.class)
    @Test
    public void testRemoveDomainconfiguration() {
        DomainDAO ddao = DomainDAO.getInstance();
        Domain d = ddao.read("netarkivet.dk");
        DomainConfiguration dc = d.getDefaultConfiguration();
        SparseDomainConfiguration dcKey = new SparseDomainConfiguration(dc.getDomainName(), dc.getName());
        List<DomainConfiguration> dclist = new ArrayList<DomainConfiguration>();
        dclist.add(dc);
        harvest.setDomainConfigurations(dclist);
        List<DomainConfiguration> configList = IteratorUtils.toList(harvest.getDomainConfigurations());
        assertTrue("Should have one config now", configList.size() == 1);
        harvest.removeDomainConfiguration(dcKey);
        configList = IteratorUtils.toList(harvest.getDomainConfigurations());
        assertTrue("Should have zero configs now", configList.size() == 0);
    }

}

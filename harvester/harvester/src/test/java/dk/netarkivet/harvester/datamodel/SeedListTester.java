/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SeedListTester extends DataModelTestCase {
    final String HARVESTNAME = "TestHarvest";
    final int TEST = 3;
    final int KURIER = 2;
    final int ORF= 1;
    final int WIKI = 0;
    
    public SeedListTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    private PartialHarvest makeSelectivePartialHarvestInstance(String harvestName, String[] domains, String[][] seeds) {

        List<DomainConfiguration> webDomainConfigs = new ArrayList<DomainConfiguration>();

        for (int i=0; i < domains.length; i++) {
            Domain d = Domain.getDefaultDomain(domains[i]);
            
            String seedStr = "";
            for (int j=0; j < seeds[i].length; j++) {
                seedStr += seeds[i][j] + "\n";
            }

            SeedList sl = new SeedList(HARVESTNAME + "_" + domains[i], seedStr);
            d.addSeedList(sl);
            
            List<SeedList> seedlists = new ArrayList<SeedList>();
            seedlists.add(sl);
            
            DomainConfiguration cfg = new DomainConfiguration("test", d, seedlists, new ArrayList<Password>());
            cfg.setOrderXmlName(TestInfo.ORDER_XML_NAME);
            cfg.setMaxObjects(10);
            cfg.setMaxRequestRate(11);
            webDomainConfigs.add(cfg);

            d.addConfiguration(cfg);
            
            DomainDAO.getInstance().create(d);
        }


        ScheduleDAO scheduledao = ScheduleDAO.getInstance();
        Schedule schedule = scheduledao.read("DefaultSchedule");

        return HarvestDefinition.createPartialHarvest(webDomainConfigs, schedule, harvestName, harvestName);
    }
    
    /**
     * Tests the getHarvestInfo() method.
     */
    public void testGetDomainsForHarvestdefinition() {
        String[] domains = { "wikipedia.org", "orf.at", "kurier.at", "test.at"};
        String[][] seeds = {
                { "http://en.wikipedia.org/wiki/Austrian_National_Library", "http://fr.wikipedia.org/wiki/Biblioth%C3%A8que_nationale_autrichienne", "http://de.wikipedia.org/wiki/%C3%96sterreichische_Nationalbibliothek" },
                { "http://news.orf.at/090603-38954/index.html", "http://sport.orf.at/", "http://euwahl09.orf.at/stories/1603916/", "http://wetter.orf.at/wie/main?tmp=1421"},
                { "http://kurier.at/sportundmotor/", "http://kurier.at/freizeitundgesundheit/", "http://kurier.at/geldundwirtschaft/"},
                { "http://www.test.at", "http://racing.test.at/", "http://racing.test.at/", "http://racing.test.at/"}
        };

        HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        
        HarvestDefinition hd = makeSelectivePartialHarvestInstance(HARVESTNAME, domains, seeds);
        hd.setSubmissionDate(new Date());
        hddao.create(hd);
        
        List<String> domainList = hddao.getListOfDomainsOfHarvestDefinition(HARVESTNAME);
        List<String> seedList = null;
        
        // kurier.at
        assertEquals(domainList.get(0), domains[KURIER]);
        seedList = hddao.getListOfSeedsOfDomainOfHarvestDefinition(HARVESTNAME, domainList.get(0));
        assertEquals(seedList.get(0), seeds[KURIER][1]);
        assertEquals(seedList.get(1), seeds[KURIER][2]);
        assertEquals(seedList.get(2), seeds[KURIER][0]);
        
        // orf.at
        assertEquals(domainList.get(1), domains[ORF]);
        seedList = hddao.getListOfSeedsOfDomainOfHarvestDefinition(HARVESTNAME, domainList.get(1));
        assertEquals(seedList.get(0), seeds[ORF][2]);
        assertEquals(seedList.get(1), seeds[ORF][0]);
        assertEquals(seedList.get(2), seeds[ORF][1]);
        assertEquals(seedList.get(3), seeds[ORF][3]);

        // test.at
        assertEquals(domainList.get(2), domains[TEST]);
        seedList = hddao.getListOfSeedsOfDomainOfHarvestDefinition(HARVESTNAME, domainList.get(2));
        assertEquals(seedList.get(0), seeds[TEST][1]);  // duplicate entries should be removed!
        assertEquals(seedList.get(1), seeds[TEST][0]);
        
        // wikipedia.org
        assertEquals(domainList.get(3), domains[WIKI]);
        seedList = hddao.getListOfSeedsOfDomainOfHarvestDefinition(HARVESTNAME, domainList.get(3));
        assertEquals(seedList.get(0), seeds[WIKI][2]);
        assertEquals(seedList.get(1), seeds[WIKI][0]);
        assertEquals(seedList.get(2), seeds[WIKI][1]);
    }
}

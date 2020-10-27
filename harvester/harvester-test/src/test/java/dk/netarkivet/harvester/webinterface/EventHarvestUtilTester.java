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

package dk.netarkivet.harvester.webinterface;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.PageContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.utils.SlowTest;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.PartialHarvest;
import dk.netarkivet.harvester.datamodel.Schedule;
import dk.netarkivet.harvester.datamodel.ScheduleDAO;
import dk.netarkivet.harvester.datamodel.SeedList;

/**
 * Unit-tests for the webinterface class dk.netarkivet.harvester.webinterface.EventHarvest.
 */
@Category(SlowTest.class)
public class EventHarvestUtilTester extends HarvesterWebinterfaceTestCase {

    private PartialHarvest harvest;
    private static final String harvestName = "Test Event Harvest";
    private static final String order1 = "default_orderxml";

    /**
     * Initialize the unit-tests. This creates a valid PartialHarvest.
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        Schedule sched = ScheduleDAO.getInstance().read("DefaultSchedule");
        harvest = new PartialHarvest(new ArrayList<DomainConfiguration>(), sched, harvestName, "", "Everybody");
        HarvestDefinitionDAO.getInstance().create(harvest);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Tests the simple case of adding a single seed to an empty harvest.
     * <p>
     */
    @Test
    public void testAddConfigurationsSimpleAdd() {
        String seedlist = "http://www.mydomain.dk/page1.jsp?aparam=avalue";
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();
        TestServletRequest request = new TestServletRequest();
        parameterMap.put("seeds", new String[] {seedlist});
        parameterMap.put("orderTemplate", new String[] {"default_orderxml"});
        parameterMap.put("maxRate", new String[] {"3"});
        parameterMap.put("maxObjects", new String[] {"4"});

        request.setParameterMap(parameterMap);
        I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new TestPageContext(request);
        List<String> illegalSeeds = new ArrayList<String>();
        EventHarvestUtil.addConfigurations(pageContext, I18N, harvest.getName(), illegalSeeds);
        String expectedDomainConfigurationName = harvestName + "_" + order1 + "_1000000000Bytes" + "_4Objects";
        // Check that the domain and configuration have been created
        harvest = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition(harvestName);
        Iterator<DomainConfiguration> dci = harvest.getDomainConfigurations();
        DomainConfiguration dc = dci.next();
        assertEquals("DomainConfiguration should have expected name, ", expectedDomainConfigurationName, dc.getName());
        assertEquals("Should have expected domain name", "mydomain.dk", dc.getDomainName());
        Iterator<SeedList> si = dc.getSeedLists();
        SeedList sl = si.next();
        assertEquals("Should have expected seedlist name", expectedDomainConfigurationName, sl.getName());
        assertEquals("Seedlist should contain specified URL", seedlist, sl.getSeedsAsString().trim());
        // Should be no more domainconfigurations or seedlists
        assertFalse("Should only be one configuration in the harvest", dci.hasNext());
        assertFalse("Should only be one seedlist in the configuration", si.hasNext());
    }
}

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

/**
 * Test cases specific to class SparsePartialHarvest
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import dk.netarkivet.common.utils.SlowTest;

public class SparsePartialHarvestTester extends DataModelTestCase {
    private PartialHarvest harvest;
    private static final String harvestName = "Event Harvest";

    // private static final String order1 = "default_orderxml";
    // private static final String order2 = "OneLevel-order";

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
     * Test constructor.
     */
    @Category(SlowTest.class)
    @Test
    public void testConstructor() {
        SparsePartialHarvest sph = new SparsePartialHarvest(harvest.oid, harvest.harvestDefName, harvest.comments,
                harvest.getNumEvents(), harvest.submissionDate, harvest.isActive, harvest.getEdition(), harvest
                        .getSchedule().getName(), harvest.getNextDate(), harvest.getAudience(), harvest.getChannelId());
        assertEquals("Should have same oid", harvest.getOid(), sph.getOid());
    }

    /**
     * Test the method getSparsePartialHarvest
     */
    @Category(SlowTest.class)
    @Test
    public void testGetSparsePartialHarvest() {
        if (harvest.oid == null) {
            harvest.setOid(Long.valueOf(1L));
        }
        SparsePartialHarvest sph = HarvestDefinitionDBDAO.getInstance().getSparsePartialHarvest(harvestName);
        assertFalse("Should be not null", sph == null);

        PartialHarvest sphComplete = (PartialHarvest) HarvestDefinitionDBDAO.getInstance().getHarvestDefinition(
                harvestName);
        assertEquals("the SparsePartialHarvest and PartialHarvest should have same name", sph.getName(),
                sphComplete.getName());
        assertEquals("the SparsePartialHarvest and PartialHarvest should have same named schedule ",
                sph.getScheduleName(), sphComplete.getSchedule().getName());
        assertEquals("the SparsePartialHarvest and PartialHarvest should have same oid", sph.getOid(),
                sphComplete.getOid());
        assertEquals("the SparsePartialHarvest and PartialHarvest should have same SubmissionDate",
                sph.getSubmissionDate(), sphComplete.getSubmissionDate());
        assertEquals("the SparsePartialHarvest and PartialHarvest should have same comments", sph.getComments(),
                sphComplete.getComments());
        assertEquals("the SparsePartialHarvest and PartialHarvest should have same NextDate", sph.getNextDate(),
                sphComplete.getNextDate());
        assertEquals("the SparsePartialHarvest and PartialHarvest should have same edition", sph.getEdition(),
                sphComplete.getEdition());

        assertEquals("the SparsePartialHarvest and PartialHarvest should have same NumEvents", sph.getNumEvents(),
                sphComplete.getNumEvents());
        assertEquals("the SparsePartialHarvest and PartialHarvest should have same active-state", sph.isActive(),
                sphComplete.isActive);
    }
}

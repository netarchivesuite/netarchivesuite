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
package dk.netarkivet.harvester.datamodel;

/**
 * Test cases specific to class SparsePartialHarvest
 */

import java.util.ArrayList;



public class SparsePartialHarvestTester extends DataModelTestCase {
    private PartialHarvest harvest;
    private static final String harvestName = "Event Harvest";
    //private static final String order1 = "default_orderxml";
    //private static final String order2 = "OneLevel-order";

    public SparsePartialHarvestTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        Schedule sched = ScheduleDAO.getInstance().read("DefaultSchedule");
        harvest = new PartialHarvest(new ArrayList<DomainConfiguration>(), sched, harvestName, "");
        HarvestDefinitionDAO.getInstance().create(harvest);
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }


    /**
     * Test constructor.
     */
    public void testConstructor() {
            SparsePartialHarvest sph = new SparsePartialHarvest(
                harvest.oid,
                harvest.harvestDefName,
                harvest.comments,
                harvest.getNumEvents(),
                harvest.submissionDate,
                harvest.isActive,
                harvest.getEdition(),
                harvest.getSchedule().getName(),
                harvest.getNextDate());
            assertEquals("Should have same oid",harvest.getOid(), sph.getOid());
     }

    /**
     * Test the method getSparsePartialHarvest
     *
     */
    public void testGetSparsePartialHarvest() {
        if (harvest.oid == null) {
            harvest.setOid(new Long(1L));
        }
        SparsePartialHarvest sph = HarvestDefinitionDBDAO.getInstance()
        .getSparsePartialHarvest(harvestName);
        assertFalse("Should be not null", sph == null);

        PartialHarvest sphComplete =  (PartialHarvest) HarvestDefinitionDBDAO.getInstance().
        getHarvestDefinition(harvestName);
        assertEquals("the SparsePartialHarvest and PartialHarvest should have same name",
                sph.getName(), sphComplete.getName());
        assertEquals("the SparsePartialHarvest and PartialHarvest should have same named schedule ",
                sph.getScheduleName(), sphComplete.getSchedule().getName());
        assertEquals("the SparsePartialHarvest and PartialHarvest should have same oid",
                sph.getOid(), sphComplete.getOid());
        assertEquals("the SparsePartialHarvest and PartialHarvest should have same SubmissionDate",
                sph.getSubmissionDate(), sphComplete.getSubmissionDate());
        assertEquals("the SparsePartialHarvest and PartialHarvest should have same comments",
                sph.getComments(), sphComplete.getComments());
        assertEquals("the SparsePartialHarvest and PartialHarvest should have same NextDate",
                sph.getNextDate(), sphComplete.getNextDate());
        assertEquals("the SparsePartialHarvest and PartialHarvest should have same edition",
                sph.getEdition(), sphComplete.getEdition());

        assertEquals("the SparsePartialHarvest and PartialHarvest should have same NumEvents",
                sph.getNumEvents(), sphComplete.getNumEvents());
        assertEquals("the SparsePartialHarvest and PartialHarvest should have same active-state",
                sph.isActive(), sphComplete.isActive);
    }
}
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
        harvest = new PartialHarvest(new ArrayList<DomainConfiguration>(), sched, harvestName, "",
                "Everybody");
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
                harvest.getNextDate(),
                harvest.getAudience(),
                harvest.getChannelId());
            assertEquals("Should have same oid",harvest.getOid(), sph.getOid());
     }

    /**
     * Test the method getSparsePartialHarvest
     *
     */
    public void testGetSparsePartialHarvest() {
        if (harvest.oid == null) {
            harvest.setOid(Long.valueOf(1L));
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
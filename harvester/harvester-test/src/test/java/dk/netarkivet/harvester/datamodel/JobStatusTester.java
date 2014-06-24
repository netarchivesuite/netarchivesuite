package dk.netarkivet.harvester.datamodel;

import java.util.Locale;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

import junit.framework.TestCase;

/**
 * Tests of the JobStatus class.
 * Currently, only the static method getLocalizedString
 * is tested here.
 */
public class JobStatusTester extends TestCase {
    public JobStatusTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    /** 
     * Test getLocalizedString.
     */
    public void testGetLocalizedString() {
        Locale en = new Locale("en");
        assertEquals(JobStatus.NEW.getLocalizedString(en), "New");
        assertEquals(JobStatus.DONE.getLocalizedString(en), "Done");
        assertEquals(JobStatus.SUBMITTED.getLocalizedString(en), "Submitted");
        assertEquals(JobStatus.STARTED.getLocalizedString(en), "Started");
        assertEquals(JobStatus.FAILED.getLocalizedString(en), "Failed");
        assertEquals(JobStatus.RESUBMITTED.getLocalizedString(en), "Resubmitted");
        assertEquals(JobStatus.FAILED_REJECTED.getLocalizedString(en), "Failed (Rejected for Resubmission)");
      }

    public void testLegalChange() {
        JobStatus status = JobStatus.FAILED_REJECTED;
        assertTrue("Should be legal to change JobStatus from FAILED_REJECTED "
                   + "back to FAILED", status.legalChange(JobStatus.FAILED));
    }
    
    public void testFromOrdinal() {
        assertEquals(JobStatus.NEW, JobStatus.fromOrdinal(0));
        assertEquals(JobStatus.SUBMITTED, JobStatus.fromOrdinal(1));
        assertEquals(JobStatus.STARTED, JobStatus.fromOrdinal(2));
        assertEquals(JobStatus.DONE, JobStatus.fromOrdinal(3));
        assertEquals(JobStatus.FAILED, JobStatus.fromOrdinal(4));
        assertEquals(JobStatus.RESUBMITTED, JobStatus.fromOrdinal(5));
        assertEquals(JobStatus.FAILED_REJECTED, JobStatus.fromOrdinal(6));
        try {
            JobStatus.fromOrdinal(7);
            fail("Should throw ArgumentNotValid on invalid status, but didn't");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }
    

}

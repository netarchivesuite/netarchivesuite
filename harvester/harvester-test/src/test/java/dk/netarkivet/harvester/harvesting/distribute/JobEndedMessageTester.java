package dk.netarkivet.harvester.harvesting.distribute;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.JobStatus;
import junit.framework.TestCase;

public class JobEndedMessageTester extends TestCase {
    
    public void testJobEndedConstructor() {
        JobEndedMessage msg = new JobEndedMessage(42L, JobStatus.DONE);
        assertEquals(JobStatus.DONE, msg.getJobStatus());
        msg = new JobEndedMessage(42L, JobStatus.FAILED);
        assertEquals(42L, msg.getJobId());
        assertEquals(JobStatus.FAILED, msg.getJobStatus());
        try {
            new JobEndedMessage(42L, JobStatus.STARTED);
            fail("Should throw ArgumentNotValid given states "
                    + "other than DONE and FAILED");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
    }
}

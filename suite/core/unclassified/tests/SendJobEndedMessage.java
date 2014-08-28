
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.harvesting.distribute.JobEndedMessage;

/**
 * Send JobEndedMessage to the HarvestMonitor
 * with a given JobID, and a FAILED/DONE 
 */
public class SendJobEndedMessage {

    /**
     * Main program that sends a JobEndedMessage to harvestMonitorServer.
     * @param args commandline arguments (should have length 2, 
     * e.g. the jobId, and FAILED/DONE).
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("No JobID and FAILED/DONE given");
            System.exit(1);
        }
        Long jobId = null;
        try {
            jobId = Long.parseLong(args[0]);
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid jobId argument (" + args[0] + ")");
            System.exit(1);
        }
        if (!args[1].equals("FAILED") && !args[1].equals("DONE")) {
            System.out.println("Invalid FAILED/DONE argument (" + args[1] + ")");
            System.exit(1);
        }
        
        JobStatus state = null;
        if (args[1].equals("FAILED")) {
            state = JobStatus.FAILED;   
        } else {
            state = JobStatus.DONE;
        }
        JobEndedMessage msg = new JobEndedMessage(jobId, state);
        JMSConnection con = JMSConnectionFactory.getInstance();
        con.send(msg);
        System.out.println("jobEndedMessage sent for jobid " + jobId);
        con.cleanup();
    }

}

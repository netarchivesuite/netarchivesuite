package dk.netarkivet.systemtest.performance;

import static org.testng.Assert.fail;

import org.jaccept.TestEventManager;

/**
 * This class represents a generic long-running job with a lifecycle as follows:
 *
 * Start the job (i)
 * Wait for startup time (a)
 * Check that job has started (ii)
 * while (job isn't finished (iii) && job has been running < maximum allowed time (c) )
 *    Report progress (iv)
 *    Wait for an interval (b)
 * Report success, or failure if job didn't finish
 *
 * The four numbered steps correspond to the four abstract methods to be implemented.
 * The time intervals a,b,c are the three arguments to the constructor.
 */
public abstract class LongRunningJob {

    String name;
    Long startUpTime;
    Long maxTime;
    Long waitingInterval;

    /**
     *
     * @param startUpTime  The amount of time allowed between the action that starts up the job and the startJob() method
     * being expected to return "true".
     * @param waitingInterval The time to wait between checking for progress.
     * @param maxTime The maximum time the job may run for.
     * @param name The name of the job.
     */
    protected LongRunningJob(Long startUpTime, Long waitingInterval, Long maxTime, String name) {
        this.startUpTime = startUpTime;
        this.maxTime = maxTime;
        this.waitingInterval = waitingInterval;
        this.name = name;
    }

    /**
     * Run the job.
     * @return True if the job completed successfully.
     * @throws InterruptedException
     */
    protected boolean run() throws InterruptedException {
        Long startTime = System.currentTimeMillis();
        startJob();
        Thread.sleep(startUpTime);
        if (!isStarted()) {
            fail("Job " + name + " failed to start.");
            return false;
        } else {
            System.out.println("Job " + name + " started successfully.");
        }
        while (!isFinished()) {
            Long runningTime = System.currentTimeMillis() - startTime;
            if (runningTime > maxTime) {
                fail("Job " + name + " overran the expected time limit " + timeToString(runningTime) + ".");
                return false;
            } else {
                System.out.println("Job progress for " + name + ": " + getProgress() + " after " + timeToString(runningTime) + ".");
            }
            Thread.sleep(waitingInterval);
        }
        Long runningTime = System.currentTimeMillis() - startTime;
        System.out.println("Job " + name + " finished successfully after " + timeToString(runningTime));
        TestEventManager.getInstance().addResult("Job " + name + " finished successfully after " + timeToString(runningTime) + " with result " + getProgress());
        return true;
    }

    abstract void startJob();

    abstract boolean isStarted();

    abstract boolean isFinished();

    /**
     * Describes the progress of the job in human readable form. The implementation has no effect on the running
     * of the job (unless it is coded with side-effects, of course).
     * @return The progress of the job.
     */
    abstract String getProgress();

    /**
     * Convert time in ms into a more-human readable form. The default implementation just converts to
     * seconds, but other implementations may use H:m:s or other formats.
     * @param time a time interval in ms.
     * @return a human-readable time interval.
     */
    public String timeToString(Long time) {
        return time/1000 + "s";
    }
}

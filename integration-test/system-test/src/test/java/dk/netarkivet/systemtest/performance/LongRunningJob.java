package dk.netarkivet.systemtest.performance;

import static org.testng.Assert.fail;

import org.jaccept.TestEventManager;

import dk.netarkivet.systemtest.TestLogger;

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
 *
 * In addition, it is also possible to specify a _minimum_ time for the job to complete as a sanity test.
 */
public abstract class LongRunningJob {

    protected final TestLogger log = new TestLogger(getClass());

    String name;
    Long startUpTime;
    Long maxTime;
    Long waitingInterval;
    Long minTime;

    /**
     * Times are in milliseconds:
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
        this.minTime = 0L;
    }

    /**
     * Run the job.
     * @return True if the job completed successfully.
     */
    protected boolean run() {
        Long startTime = System.currentTimeMillis();
        startJob();
        startWait();
        if (!isStarted()) {
            fail("Job " + name + " failed to start.");
            return false;
        } else {
            log.debug("Job {} started successfully.", name);
        }
        while (!isFinished()) {
            Long runningTime = System.currentTimeMillis() - startTime;
            if (runningTime > maxTime) {
                fail("Job " + name + " overran the expected time limit " + timeToString(runningTime) + ".");
                return false;
            } else {
                log.debug("Job progress for {}: {} after {}.", name, getProgress(), timeToString(runningTime));
            }
            sleepWait();
        }
        Long runningTime = System.currentTimeMillis() - startTime;
        if (runningTime < minTime) {
            fail("Job " + name + " ended after less than the specified minimum time " + timeToString(runningTime) + " (" + timeToString(minTime) + ").");
            return false;
        }
        log.debug("Job {} finished successfully after {}.", name, timeToString(runningTime));
        TestEventManager.getInstance().addResult(
                "Job " + name + " finished successfully after " + timeToString(runningTime) + " with result "
                        + getProgress());
        return true;
    }

    private void startWait() {
            try {
                Thread.sleep(startUpTime);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    private void sleepWait() {
        try {
            Thread.sleep(waitingInterval);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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

    /**
     * Set the minimum time (in milliseconds) that this job must run. This provides a sanity test that the job is
     * actually functioning as expected. The default is 0.
     * @param minTime the minimum time.
     */
    public void setMinTime(Long minTime) {
        this.minTime = minTime;
    }
}

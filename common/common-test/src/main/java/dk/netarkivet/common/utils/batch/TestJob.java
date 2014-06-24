package dk.netarkivet.common.utils.batch;


@SuppressWarnings({ "serial"})
public class TestJob extends ChecksumJob {
    private String testId;
    public TestJob(String in_testId) {
        testId = in_testId;
    }

    public String getTestId() {
        return testId;
    }

    public void setBatchTimeout(long timeout) {
        batchJobTimeout = timeout;
    }
}

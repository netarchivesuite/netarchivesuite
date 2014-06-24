package dk.netarkivet.harvester.harvesting.frontier;


final class FrontierTestUtils {

    /** Outputs a frontier line as a string. */
    public static String toString(FrontierReportLine l) {
        return l.getDomainName()
            + " " + l.getCurrentSize()
            + " " + getDisplayValue(l.getTotalEnqueues())
            + " " + getDisplayValue(l.getSessionBalance())
            + " " + getDisplayValue(l.getLastCost())
            + "(" + getDisplayValue(l.getAverageCost()) + ")"
            + " " + l.getLastDequeueTime()
            + " " + l.getWakeTime()
            + " " + getDisplayValue(l.getTotalSpend())
            + "/" + getDisplayValue(l.getTotalBudget())
            + " " + getDisplayValue(l.getErrorCount())
            + " " + l.getLastPeekUri()
            + " " + l.getLastQueuedUri();
    }

    private static String getDisplayValue(long val) {
        return Long.MIN_VALUE == val ?
                FrontierReportLine.EMPTY_VALUE_TOKEN : "" + val;
    }

    private static String getDisplayValue(double val) {
        if (Double.MIN_VALUE == val) {
            return FrontierReportLine.EMPTY_VALUE_TOKEN;
        }
        return (Math.rint(val) == val ?
                Integer.toString((int) val) : "" + Double.toString(val));
    }

}

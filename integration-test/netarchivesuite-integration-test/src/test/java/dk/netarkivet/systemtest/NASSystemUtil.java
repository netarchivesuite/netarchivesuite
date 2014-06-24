package dk.netarkivet.systemtest;

public class NASSystemUtil {
    public void startSystem() {
    }
    /**
     * Defines the standard application setup in the DK test system.
     */
    public static Application[] getApplications() {
        return new Application[] {
                new Application("KB-TEST-BAR-015", "BitarchiveServer", null,
                        null, "KBN"),
                new Application("KB-TEST-BAR-014", "BitarchiveServer",
                        "BitApp_1", null, "KBN"),
                new Application("KB-TEST-BAR-014", "BitarchiveServer",
                        "BitApp_2", null, "KBN"),
                new Application("KB-TEST-BAR-014", "BitarchiveServer",
                        "BitApp_3", null, "KBN"),
                new Application("kb-test-acs-001", "ChecksumFileServer", null,
                        null, "CSN"),
                new Application("kb-test-acs-001", "IndexServer", null, null,
                        "KBN"),
                new Application("kb-test-acs-001", "ViewerProxy", null, null,
                        "KBN"),
                new Application("kb-test-adm-001", "ArcRepository", null, null,
                        "KBN"),
                new Application("kb-test-adm-001", "BitarchiveMonitorServer",
                        "KBBM", null, "KBN"),
                new Application("kb-test-adm-001", "BitarchiveMonitorServer",
                        "SBBM", null, "SBN"),
                new Application("kb-test-adm-001",
                        "HarvestJobManagerApplication", null, null, "KBN"),
                new Application("kb-test-adm-001", "GUIWebServer", null, null,
                        "KBN"),
                new Application("kb-test-har-003", "HarvestControllerServer",
                        "kblow001", "LOWPRIORITY", "KBN"),
                new Application("kb-test-har-004", "HarvestControllerServer",
                        "kbhigh", "HIGHPRIORITY", "KBN"),
                new Application("kb-test-har-004", "HarvestControllerServer",
                        "kblow002", "LOWPRIORITY", "KBN"),
                new Application("sb-test-bar-001", "BitarchiveServer", null,
                        null, "SBN"),
                new Application("sb-test-har-001", "HarvestControllerServer",
                        "sbhigh", "HIGHPRIORITY", "SBN") };
    }
}

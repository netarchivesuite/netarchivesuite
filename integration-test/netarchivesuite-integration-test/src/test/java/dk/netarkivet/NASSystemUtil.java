package dk.netarkivet;

public class NASSystemUtil {
	public void startSystem() {
	}

	public static Application[] getApplications() {
		return new Application[] {
				new Application("KB-TEST-BAR-013", "BitarchiveServer", null),
				new Application("KB-TEST-BAR-014", "BitarchiveServer", null),
				new Application("kb-test-acs-001", "ChecksumFileServer", null),
				new Application("kb-test-acs-001", "IndexServer", null),
				new Application("kb-test-acs-001", "ViewerProxy", null),
				new Application("kb-test-adm-001", "ArcRepository", null),
				new Application("kb-test-adm-001", "BitarchiveMonitorServer", null),
				new Application("kb-test-adm-001", "HarvestMonitorServer", null),
				new Application("kb-test-adm-001", "HarvestJobManagerApplication", null),
				new Application("kb-test-adm-001", "GUIWebServer", null),
				new Application("kb-test-har-001", "HarvestControllerServer", "LOWPRIORITY"),
				new Application("kb-test-har-001", "HarvestControllerServer", "HIGHPRIORITY"),
				new Application("kb-test-har-002", "HarvestControllerServer", "LOWPRIORITY"),
				new Application("kb-test-har-002", "HarvestControllerServer","HIGHPRIORITY"),
				new Application("sb-test-acs-001", "ViewerProxy", null),
				new Application("sb-test-bar-001", "BitarchiveServer", null)
		};
	}
}

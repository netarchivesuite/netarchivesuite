package dk.netarkivet;

public class NASSystemUtil {
	public void startSystem() {
	}

	public static Application[] getApplications() {
		return new Application[] {
				new Application("KB-TEST-BAR-013", "BitarchiveServer", 
						null, null, "KBN"),
				new Application("KB-TEST-BAR-014", "BitarchiveServer", 
						"BitApp_1", null, "KBN"),
				new Application("KB-TEST-BAR-014", "BitarchiveServer", 
						"BitApp_2",null, "KBN"),
				new Application("KB-TEST-BAR-014", "BitarchiveServer", 
						"BitApp_3",null, "KBN"),
				new Application("kb-test-acs-001", "ChecksumFileServer", 
						null, null, "CSN"),
				new Application("kb-test-acs-001", "IndexServer", 
						null, null, "KBN"),
				new Application("kb-test-acs-001", "ViewerProxy", 
						null, null, "KBN"),
				new Application("kb-test-adm-001", "ArcRepository", 
						null, null, "KBN"),
				new Application("kb-test-adm-001", "BitarchiveMonitorServer", 
						"KBBM", null, "KBN"),
				new Application("kb-test-adm-001", "BitarchiveMonitorServer", 
						"SBBM", null, "SBN"),
				new Application("kb-test-adm-001", "HarvestMonitorServer", 
						null, null, "KBN"),
				new Application("kb-test-adm-001", "HarvestJobManagerApplication", 
						null, null, "KBN"),
				new Application("kb-test-adm-001", "GUIWebServer", 
						null, null, "KBN"),
				new Application("kb-test-har-001", "HarvestControllerServer", 
						null, "LOWPRIORITY", "KBN"),
				new Application("kb-test-har-002", "HarvestControllerServer", 
						"high", "HIGHPRIORITY", "KBN"),
				new Application("kb-test-har-002", "HarvestControllerServer", 
						"low", "LOWPRIORITY", "KBN"),
				new Application("sb-test-acs-001", "ViewerProxy", 
						null, null, "SBN"),
				new Application("sb-test-bar-001", "BitarchiveServer", 
						null, null, "SBN"),
				new Application("sb-test-har-001", "HarvestControllerServer",
						null, "HIGHPRIORITY", "SBN")
		};
	}
}

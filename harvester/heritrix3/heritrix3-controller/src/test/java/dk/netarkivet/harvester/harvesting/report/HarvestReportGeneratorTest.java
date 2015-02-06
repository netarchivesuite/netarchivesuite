package dk.netarkivet.harvester.harvesting.report;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.harvester.harvesting.Heritrix3Files;
import dk.netarkivet.harvester.harvesting.PersistentJobData;

public class HarvestReportGeneratorTest {

	@Before
	public void setUp() throws Exception {

	}

	@Test
	public void testReportGenerator() {
		File crawldir = new File("/home/svc/devel/crawldir/");
		PersistentJobData pjd = new PersistentJobData(crawldir); 
		Heritrix3Files files = Heritrix3Files.getH3HeritrixFiles(crawldir, 
				pjd);
		HarvestReportGenerator hrg = new HarvestReportGenerator(files);
		DomainStatsReport dsr = new DomainStatsReport(hrg.getDomainStatsMap(), 
				hrg.getDefaultStopReason()); 
		HarvestReport hr = HarvestReportFactory.generateHarvestReport(dsr);
	}

}

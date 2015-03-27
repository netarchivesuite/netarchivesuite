package dk.netarkivet.harvester.harvesting.report;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.datamodel.StopReason;
import dk.netarkivet.harvester.harvesting.PersistentJobData;
import dk.netarkivet.harvester.heritrix3.Heritrix3Files;
import dk.netarkivet.harvester.heritrix3.Heritrix3Settings;
import dk.netarkivet.harvester.heritrix3.report.HarvestReportFactory;
import dk.netarkivet.harvester.heritrix3.report.HarvestReportGenerator;

public class HarvestReportGeneratorTest {

	@Test
	public void testReportGenerator() throws IOException {
		File crawldir = new File("src/test/resources/crawldir");
		PersistentJobData pjd = new PersistentJobData(crawldir);
		File h3Bundle = File.createTempFile("fake-path-to-h3-bundle", "");
		File certificat = File.createTempFile("fake-path-to-h3-certificat", "");
		Settings.set(Heritrix3Settings.HERITRIX3_BUNDLE, h3Bundle.getAbsolutePath());
		Settings.set(Heritrix3Settings.HERITRIX3_CERTIFICATE, certificat.getAbsolutePath());
		Heritrix3Files files = Heritrix3Files.getH3HeritrixFiles(crawldir, 
				pjd);
		HarvestReportGenerator hrg = new HarvestReportGenerator(files);
		DomainStatsReport dsr = new DomainStatsReport(hrg.getDomainStatsMap(), 
				hrg.getDefaultStopReason()); 
		HarvestReport hr = HarvestReportFactory.generateHarvestReport(dsr);
		assertEquals(hr.getDefaultStopReason(), StopReason.DOWNLOAD_UNFINISHED);
	}

}

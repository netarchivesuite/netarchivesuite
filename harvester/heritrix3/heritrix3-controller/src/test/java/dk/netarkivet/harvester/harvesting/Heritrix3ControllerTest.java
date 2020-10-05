package dk.netarkivet.harvester.harvesting;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.HeritrixTemplate;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.Password;
import dk.netarkivet.harvester.datamodel.SeedList;
import dk.netarkivet.harvester.heritrix3.Heritrix3Files;
import dk.netarkivet.harvester.heritrix3.controller.HeritrixController;

public class Heritrix3ControllerTest {
	public static void main(String args[]) {
		File crawlDir = null;
		HarvestChannel hc = new HarvestChannel("aChannel", false, false, "aChannel");
		Domain d = Domain.getDefaultDomain("netarkivet.dk");
		List<Password> passwords = new ArrayList<Password>();
		List<SeedList> seedlists = new ArrayList<SeedList>();
		DomainConfiguration cfg = new DomainConfiguration("config1", d, seedlists, passwords);
		HeritrixTemplate orderXMLdoc =  null;
		Job j = new Job(1L, cfg, orderXMLdoc, hc,-1L,-1L, 0L, 1);
		j.setJobID(1L);
		Heritrix3Files files = Heritrix3Files.getH3HeritrixFiles(crawlDir, j);
        String jobName = j.getJobID() + "_" + System.currentTimeMillis();
		HeritrixController bhc = new HeritrixController(files, jobName);
		Assert.assertNotNull(bhc);
	}
}

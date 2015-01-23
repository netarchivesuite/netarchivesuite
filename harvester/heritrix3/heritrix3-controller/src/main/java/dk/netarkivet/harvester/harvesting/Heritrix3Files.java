package dk.netarkivet.harvester.harvesting;

import java.io.File;
import java.io.InputStream;

import dk.netarkivet.harvester.datamodel.HeritrixTemplate;
import dk.netarkivet.harvester.datamodel.Job;

public class Heritrix3Files {

	public static Heritrix3Files getH3HeritrixFiles(File crawldir, PersistentJobData harvestInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Heritrix3Files getH3HeritrixFiles(File crawldir, Job job) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	public File getCrawlDir() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean writeRecoverBackupfile(InputStream data) {
		// TODO Auto-generated method stub
		return false;
	}

	public File getRecoverBackupGzFile() {
		// TODO Auto-generated method stub
		return null;
	}

	public void writeSeedsTxt(String seedListAsString) {
		// TODO Auto-generated method stub
		
	}

	public void setIndexDir(File fetchDeduplicateIndex) {
		// TODO Auto-generated method stub
		
	}

	public void writeOrderXml(HeritrixTemplate orderXMLdoc) {
		// TODO Auto-generated method stub
		
	}

	public File getProgressStatisticsLog() {
		// TODO Auto-generated method stub
		return null;
	}

	public Long getJobID() {
		// TODO Auto-generated method stub
		return null;
	}

	public File getOrderXmlFile() {
		// TODO Auto-generated method stub
		return null;
	}

	public File getSeedsTxtFile() {
		// TODO Auto-generated method stub
		return null;
	}

	public Long getHarvestID() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getArchiveFilePrefix() {
		// TODO Auto-generated method stub
		return null;
	}


	public void deleteFinalLogs() {
		// TODO Auto-generated method stub
		
	}

	public void cleanUpAfterHarvest(File file) {
		// TODO Auto-generated method stub
		
	}

}

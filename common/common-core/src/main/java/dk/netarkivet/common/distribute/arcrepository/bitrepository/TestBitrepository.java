package dk.netarkivet.common.distribute.arcrepository.bitrepository;

import java.io.File;
import java.util.List;

import dk.netarkivet.common.distribute.arcrepository.bitrepository.Bitrepository;

/**
 * 
 *
 *
 */
public class TestBitrepository {
	
	public static void main(String[] args) {
		//runnerOne();
		runnerTwo();
	}
	public static void runnerTwo(){
		File configDir = new File("/home/svc/devel/bitrepository-quickstart-netarchivesuite/commandline/conf");
		String bitmagKeyfilename = "dummy-certificate.pem";
		//Bitrepository bitrep = new Bitrepository(configDir, null, 1, "sbdisk1");
		Bitrepository bitrep = new Bitrepository(configDir, bitmagKeyfilename, 1, "file1-pillar");
		int i=0;
		for (String col: bitrep.getKnownCollections()) {
			 i++;
			 System.out.println("col " + i + ": " + col);
		 }
		 i=0;
		 for ( String pillar: bitrep.getCollectionPillars("books")) {
			 i++; 
			 System.out.println("pillar " + i + ": " + pillar);
		 }
		 
		 if (bitrep.existsInCollection("47-metadata-1.warc", "books")){
			 System.out.println("47-metadata-1.warc found in collection books");
		 } else {
			 System.out.println("47-metadata-1.warc NOT found in collection books");
		 } 
		 System.out.println("books-ids:");
		 for (String id: bitrep.getFileIds("books")) {
			 System.out.println(id);
		 }
		 
		 
		 
		 bitrep.shutdown();
	}
	
	/**
	 * TEST That integrates with our bitrepository integration system.  
	 * netarkiv-pillars:
   		checksum2
   		sbdisk1
	 */
	public static void runnerOne(){
		 File configDir = new File("/home/svc/bitmag-releasetest-conf");
		 String bitmagKeyfilename = "client-certificate.pem"; 
		 Bitrepository bitrep = new Bitrepository(configDir, bitmagKeyfilename, 1, "sbdisk1");
		 for (String col: bitrep.getKnownCollections()) {
			 System.out.println(col);
		 }
		 
		 for ( String pillar: bitrep.getCollectionPillars("netarkiv")) {
			 System.out.println(pillar);
		 }
		 System.out.println();
		 System.out.println("netarkiv-pillars:");
		 for (String col: bitrep.getCollectionPillars("netarkiv")) {
			 System.out.println(col);
		 }
		 
		 System.out.println("netarkiv-ids:");
		 for (String id: bitrep.getFileIds("netarkiv")) {
			 System.out.println(id);
		 }
		 
		 
		 //bitrep.shutdown();
		 
	/*
		 File configDir = new File("/home/svc/bitrepository-quickstart/commandline/conf");
		 //File bitmagKeyfile = new File(configDir, "client-certificate.pem");
		 Bitrepository bitrep = new Bitrepository(configDir, null, 1, "kbpillar-test-linux");
		 bitrep.shutdown();
	*/ 
		 //BitmagArcRepositoryClient client = new BitmagArcRepositoryClient();
		 
		 
		 bitrep.shutdown();
		 
		}
	
	public static void runnerThree() {
		File configDir = new File("/home/svc/bitmag-releasetest-conf");
		String bitmagKeyfilename = "client-certificate.pem";
		Bitrepository bitrep = new Bitrepository(configDir, bitmagKeyfilename, 1, "kbpillar-test-linux");
		bitrep.shutdown();  
	}
	public static void runnerFour() {
	
	//File configDir = new File("/home/svc/bitmag-releasetest-conf");
	 //File bitmagKeyfile = new File(configDir, "client-certificate.pem");
	 //Bitrepository bitrep = new Bitrepository(configDir, bitmagKeyfile);
	 //bitrep.shutdown();
	 
	 //File configDir = new File("/home/svc/bitrepository-quickstart/commandline/conf");
	 //File bitmagKeyfile = new File(configDir, "client-certificate.pem");
	 //Bitrepository bitrep = new Bitrepository(configDir, null, 1);
	 //bitrep.shutdown();

    BitmagArcRepositoryClient client = new BitmagArcRepositoryClient();
	 Bitrepository bitrep = client.getBitrepository();
	 boolean succes = bitrep.existsInCollection("packageId", "dvds");
	 System.out.println(succes);
	 List<String> pillarList  = bitrep.getCollectionPillars("dvds");
	 for (String id: pillarList) {
		 System.out.println(id);
	 }

	 List<String> fileList = bitrep.getFileIds("dvds");
	
	 if (fileList  != null) {
		 System.out.println("Found '" + fileList.size() + "'  ids");
		 for (String id: fileList) {
			 System.out.println(id);
		 }
	 } else {
		 System.out.println("No fileList returned");
	 }
	 bitrep.shutdown();
	 
	}
	
	public static void runnerFive() {

	 //File configDir = new File("/home/svc/bitmag-releasetest-conf");
	 //File bitmagKeyfile = new File(configDir, "client-certificate.pem");
	 //Bitrepository bitrep = new Bitrepository(configDir, bitmagKeyfile);
	 //bitrep.shutdown();
	 
	 File configDir = new File("/home/svc/bitrepository-quickstart/commandline/conf");
	 //File bitmagKeyfile = new File(configDir, "client-certificate.pem");
	 Bitrepository bitrep = new Bitrepository(configDir, null, 1, "kbpillar-test-linux");
	 bitrep.shutdown();
	 
	 
	 
	 BitmagArcRepositoryClient client = new BitmagArcRepositoryClient();
	 
	 
	}
	
}



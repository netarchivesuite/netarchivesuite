package dk.netarkivet.common.distribute.arcrepository.bitrepository;

import java.io.File;
import dk.netarkivet.common.distribute.arcrepository.bitrepository.Bitrepository;

/**
 * 
 * netarkiv-pillars:
   checksum2
   sbdisk1
 *
 */


public class TestBitrepository {
	
	public static void main(String[] args) {

			 File configDir = new File("/home/svc/bitmag-releasetest-conf");
			 File bitmagKeyfile = new File(configDir, "client-certificate.pem");
			 Bitrepository bitrep = new Bitrepository(configDir, bitmagKeyfile, 1, "sbdisk1");
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

	}

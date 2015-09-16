package dk.netarkivet.common.distribute.arcrepository.bitrepository;

import java.io.File;
import java.util.List;

import dk.netarkivet.common.distribute.arcrepository.bitrepository.Bitrepository;

public class BitrepositoryTest3 {

	public static void main(String[] args) {

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
}
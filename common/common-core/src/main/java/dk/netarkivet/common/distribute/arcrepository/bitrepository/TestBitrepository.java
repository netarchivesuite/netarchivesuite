package dk.netarkivet.common.distribute.arcrepository.bitrepository;

import java.io.File;
import dk.netarkivet.common.distribute.arcrepository.bitrepository.Bitrepository;

public class TestBitrepository {

	// FIXME kan kun køres hvis xmlParserAPIs-2.6.2.jar (fra 2003) ikke findes i classpath (skal bruge xercesImpl-2.9.1.jar istedet)
	// Jf. http://stackoverflow.com/questions/20473689/org-xml-sax-saxnotrecognizedexception-feature-http-javax-xml-xmlconstants-fe
	
	public static void main(String[] args) {

			 //File configDir = new File("/home/svc/bitmag-releasetest-conf");
			 //File bitmagKeyfile = new File(configDir, "client-certificate.pem");
			 //Bitrepository bitrep = new Bitrepository(configDir, bitmagKeyfile);
			 //bitrep.shutdown();
			 
			 File configDir = new File("/home/svc/bitrepository-quickstart/commandline/conf");
			 //File bitmagKeyfile = new File(configDir, "client-certificate.pem");
			 Bitrepository bitrep = new Bitrepository(configDir, null);
			 bitrep.shutdown();
			 
			 
			}

	}

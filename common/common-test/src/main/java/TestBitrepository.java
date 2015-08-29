import java.io.File;

import dk.netarkivet.common.distribute.arcrepository.bitrepository.Bitrepository;

public class TestBitrepository {

	public static void main(String[] args) {
	 File configDir = new File("/home/svc/bitmag-releasetest-conf");
	 File bitmagKeyfile = new File(configDir, "client-certificate.pem");
	 Bitrepository bitrep = new Bitrepository(configDir, bitmagKeyfile);
	 bitrep.shutdown();  

	}

}

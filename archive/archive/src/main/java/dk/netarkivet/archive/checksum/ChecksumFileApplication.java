package dk.netarkivet.archive.checksum;

import dk.netarkivet.archive.checksum.distribute.ChecksumFileServer;
import dk.netarkivet.common.utils.ApplicationUtils;

public class ChecksumFileApplication {
    /**
     * Runs the Checksum File Application.
     * 
     * @param args No arguments required - thus an empty array.
     * @see ChecksumFileServer
     */
    public static void main(String[] args) {
	ApplicationUtils.startApp(ChecksumFileServer.class, args);
    }
}

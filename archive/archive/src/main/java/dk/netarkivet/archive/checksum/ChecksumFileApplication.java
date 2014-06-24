package dk.netarkivet.archive.checksum;

import dk.netarkivet.archive.checksum.distribute.ChecksumFileServer;
import dk.netarkivet.common.utils.ApplicationUtils;

/**
 * This class is used to start the checksum file application.
 */
public final class ChecksumFileApplication {
    /**
     * Private constructor. Prevents instantiation of this class.
     */
    private ChecksumFileApplication() {}
    
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

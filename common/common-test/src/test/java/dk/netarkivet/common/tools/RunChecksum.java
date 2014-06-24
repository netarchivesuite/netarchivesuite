
package dk.netarkivet.common.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.PreservationArcRepositoryClient;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.ChecksumJob;

/**
 * A tool that runs a checksum job on a bitarchive, outputting the results.
 *
 */

public class RunChecksum {
    public static void main(String[] argv) throws IOException {
        if (argv.length > 1) {
            System.out.println("Too many arguments");
            dieWithUsage();
        }
        String bitarchive;
        if (argv.length == 1) {
            bitarchive = argv[0];
            boolean found = false;
            final String[] locations = Settings.getAll(
                    CommonSettings.REPLICA_IDS);
            for (String location : locations) {
                if (bitarchive.equals(location)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.out.println("Bitarchive '" + bitarchive + "' not found " +
                        "among known bitarchives " + Arrays.asList(locations));
                dieWithUsage();
            }
        } else {
            bitarchive = Settings.get(CommonSettings.USE_REPLICA_ID);
        }

        PreservationArcRepositoryClient arcrep = ArcRepositoryClientFactory.getPreservationInstance();
        BatchStatus lbs = arcrep.batch(new ChecksumJob(), bitarchive);
        RemoteFile result = lbs.getResultFile();
        File localFile = File.createTempFile("checksum_tool", "results");
        localFile.deleteOnExit();
        result.copyTo(localFile);
        result.cleanup();
        System.out.print("*** Checksum done on '" + bitarchive + "', "
                + lbs.getNoOfFilesProcessed() + " files processed");
        if (lbs.getFilesFailed().size() != 0) {
                System.out.println(", failed files: " + lbs.getFilesFailed());
        }
        System.out.println(" ***");
        BufferedReader reader = new BufferedReader(new FileReader(localFile));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        System.out.println("***");
        reader.close();
        arcrep.close();
        JMSConnectionFactory.getInstance().cleanup();
    }

    private static void dieWithUsage() {
        System.out.println("Usage: java " + RunChecksum.class.getName()
                + " [bitarchive]");
        System.out.println("Bitarchive names are defined in settings.xml");
        System.exit(1);
    }
}

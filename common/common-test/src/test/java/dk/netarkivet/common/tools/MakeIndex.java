
package dk.netarkivet.common.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.exceptions.NetarkivetException;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.cdx.ExtractCDXJob;

/**
 * A tool to create index.cdx files.
 *
 * Usage: java dk.netarkivet.common.tools.MakeIndex [filename]
 *
 */

public class MakeIndex {
    public static void main(String[] argv) {
        File indexfile = new File("index.cdx");
        if (argv.length > 1) {
            System.err.println("Too many arguments.");
            dieWithUsage();
        }
        if (argv.length != 0) {
            indexfile = new File(argv[0]);
        }
        ViewerArcRepositoryClient arcrep = null;
        try {
            // Set to one below the number used by the hacos to avoid them
            // eating our reply.
            // Yes, it's a kludge.  Anyone care to add me a channel just for this?
            Settings.set(CommonSettings.HTTP_PORT_NUMBER, "" + (Settings.getInt(CommonSettings.HTTP_PORT_NUMBER) - 1));
            System.out.println("Connecting to ArcRepository");
            arcrep = ArcRepositoryClientFactory.getViewerInstance();
            System.out.println("Creating index file '" + indexfile + "'");
            ExtractCDXJob cdxjob = new ExtractCDXJob(false);
            // Do index on the first bitarchive found.
            //String baName = wc.getBitarchiveNames()[0];
            BatchStatus cdxstatus =
                    arcrep.batch(cdxjob, Settings.get(
                            CommonSettings.USE_REPLICA_ID));
            cdxstatus.getResultFile().copyTo(indexfile);
            cdxstatus.getResultFile().cleanup();
            final List<File> filesFailed = new ArrayList<File>(cdxstatus.getFilesFailed());
            if (filesFailed != null && filesFailed.size() != 0) {
                System.out.println("Some files failed to be indexed: " + filesFailed);
            } else {
                System.out.println("Indexed " + cdxstatus.getNoOfFilesProcessed() + " files");
            }
        } catch (NetarkivetException e) {
            System.out.println("Error while making index: " + e);
            e.printStackTrace();
        } finally {
            if (arcrep != null) {
                arcrep.close();
            }
            JMSConnectionFactory.getInstance().cleanup();
        }
    }

    private static void dieWithUsage() {
        System.out.println("Usage: java " + MakeIndex.class.getName()
                + " [indexfile]");
        System.exit(1);
    }
}

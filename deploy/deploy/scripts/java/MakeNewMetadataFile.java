//package dk.netarkivet.harvester.tools;

import java.io.File;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.HarvestDocumentation;
import dk.netarkivet.harvester.harvesting.HeritrixFiles;
import dk.netarkivet.harvester.harvesting.metadata.PersistentJobData;

/**
 * Make a metadata arc or warc file in case this process failed during the postprocessing
 * step.
 * Only argument: full path to the current location of the harvest-dir.
 * Need reference to settings.xml as used by the harvester
 * anf of course lib/dk.netarkivet.harvester.jar in the classpath 
 */
public class MakeNewMetadataFile {

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Missing argument: /full/path/to/crawldir");
            System.exit(1);            
        }
        Settings.set(HarvesterSettings.METADATA_FORMAT, "warc");
        
        File crawlDir = new File(args[0]);
        if (!PersistentJobData.existsIn(crawlDir)) {
            throw new IOFailure("No harvestInfo found in directory: "
                    + crawlDir.getAbsolutePath());
        }
        PersistentJobData harvestInfo = new PersistentJobData(crawlDir);
        Long jobID = harvestInfo.getJobID();
        HeritrixFiles files =
                new HeritrixFiles(
                        crawlDir,
                        harvestInfo);
        File tmpMeta = new File(crawlDir, "tmp-meta");
        if (tmpMeta.list().length > 0) {
            System.err.println("Please remove the old uncompleted " + jobID 
                    + "-metadata-1.warc file and the temporary cdx directory if any");
            System.exit(1);
        }
        
        File arcFilesReport = new File(crawlDir, "arcfiles-report.txt");
        if (arcFilesReport.exists()) {
            System.err.println("Please remove the old arcfiles-report.txt");
            System.exit(1);
        }
        
        
        
        
        HarvestDocumentation.documentHarvest(crawlDir, jobID, files.getHarvestID());
    }

}

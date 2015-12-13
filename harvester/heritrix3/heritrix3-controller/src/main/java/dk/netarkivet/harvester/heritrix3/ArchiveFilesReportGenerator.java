package dk.netarkivet.harvester.heritrix3;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;


/**This class generate a report that lists ARC/WARC files (depending on the configured archive format) along with the
* opening date, closing date (if file was properly closed), and size in bytes.
* <p>
* Here is a sample of such a file:
* <p>
* [ARCHIVEFILE] [Closed] [Size]<p> 
* 5-1-20100720161253-00000-bnf_test.arc.gz  "2010-07-20 16:14:31.792" 162928
* <p>
* The file is named "archivefiles-report.txt"
*/
class ArchiveFilesReportGenerator {

    private static final SimpleDateFormat ISO_8601_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    /**
     * The name of the report file. It will be generated in the crawl directory.
     */
    public static final String REPORT_FILE_NAME = Settings.get(Heritrix3Settings.METADATA_ARCHIVE_FILES_REPORT_NAME);

    /**
     * The header line of the report file.
     */
    public static final String REPORT_FILE_HEADER = Settings.get(Heritrix3Settings.METADATA_ARCHIVE_FILES_REPORT_HEADER);

    private IngestableFiles ingestablefiles;

    /**
     * Builds a ARC files report generator, given the Ingestable files object.
     *
     * @param ingestableFiles files belonging to a Heritrix harvest
     */

    public ArchiveFilesReportGenerator(IngestableFiles ingestableFiles) {
        this.ingestablefiles = ingestableFiles;
    }   

    /**
     * Parses heritrix.out and generates the ARC/WARC files report.
     *
     * @return the generated report file.
     */
    protected File generateReport() {

        File reportFile = new File(ingestablefiles.getCrawlDir(), REPORT_FILE_NAME);

        try {
            boolean created = reportFile.createNewFile();
            if (!created) {
                throw new IOException("Unable to create '" + reportFile.getAbsolutePath() + "'.");
            }
            PrintWriter out = new PrintWriter(reportFile);

            out.println(REPORT_FILE_HEADER);

            for (File arcfile : ingestablefiles.getArcFiles()) {
                out.println(arcfile.getName() + " " + ISO_8601_DATE_FORMAT.format(new Date(arcfile.lastModified())) + " " + arcfile.length());
            }
            for (File warcfile : ingestablefiles.getWarcFiles()) {
                out.println(warcfile.getName() + " " + ISO_8601_DATE_FORMAT.format(new Date(warcfile.lastModified())) + " " + warcfile.length());
            }

            out.close();
        } catch (IOException e) {
            throw new IOFailure("Failed to create " + reportFile.getName(), e);
        }

        return reportFile;
    }

}
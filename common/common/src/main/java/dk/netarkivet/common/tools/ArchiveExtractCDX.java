package dk.netarkivet.common.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.common.utils.cdx.ExtractCDXJob;

/**
 *
 *
 * Command line tool for extracting CDX information from given ARC/WARC files.
 *
 * Usage:
 * java dk.netarkivet.common.tools.ExtractCDX file1.ext [file2.ext ...]
 *      > myindex.cdx
 *
 * "ext" can be arc, arc.gz, warc or warc.gz
 *
 * Note: Does not depend on logging - communicates failures on stderr.
 */
public class ArchiveExtractCDX {

    /**
     * Main method. Extracts CDX from all given files and outputs the index
     * on stdout.
     * @param argv A list of (absolute paths to) files to index.
     */
    public static void main(String[] argv) {
        if (argv.length == 0) {
            System.err.println("Missing parameter: "
                    + "Must supply one or more ARC/WARC file(s) to be indexed");
            dieWithUsage();
        }
        List<File> arcFiles = new ArrayList<File>();
        for (String arg : argv) {
           File f = toArcFile(arg);
            arcFiles.add(f);
        }
        File[] arcFileArray = arcFiles.toArray(new File[]{});
        BatchLocalFiles batchRunner = new BatchLocalFiles(arcFileArray);
        batchRunner.run(new ExtractCDXJob(), System.out);
    }

    /**
     * Verifies that the filename (absolute path) points to
     * an existing file and that it is an arc or warc file.
     * @param filename The filename to verify.
     * @return The arc or warc file, as a File.
     */
    private static File toArcFile(String filename) {
        File f;
        try {
            f = FileUtils.makeValidFileFromExisting(filename).getAbsoluteFile();
            if (!FileUtils.WARCS_ARCS_FILTER.accept(f.getParentFile(), f.getName())) {
                dieWithError("Could not accept " + filename
                        + ": was not an arc or warc file");
            }
            return f;
        } catch (IOFailure e) {
           dieWithError("Could not accept " + filename + ":" + e);
           return null; //Compiler does not recognize System.exit()
        }
    }

    /**
     * Prints out a message on stderr and exits with an error code.
     * @param msg The message to print.
     */
    private static void dieWithError(String msg) {
        System.err.println(msg);
        System.err.println("Exiting - output is not OK");
        System.exit(1);
    }

    /**
     * Prints out proper usage of this tool on stderr and exits
     * with an error code.
     */
    private static void dieWithUsage() {
        System.err.println("Usage: java " + ExtractCDX.class.getName()
                + " file1.arc [file2.arc ...]");
        System.exit(1);
    }

}

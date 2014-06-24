package dk.netarkivet.wayback.indexer;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.wayback.WaybackSettings;


public class WaybackIndexerTester extends IndexerTestCase {

    File originals = new File("tests/dk/netarkivet/wayback/indexer/data/originals");
    File working = new File("tests/dk/netarkivet/wayback/indexer/data/working");

     public void setUp() {
         super.setUp();
        FileUtils.removeRecursively(working);
        TestFileUtils.copyDirectoryNonCVS(originals, working);
      }


    public void tearDown() {
        super.tearDown();
        FileUtils.removeRecursively(working);
    }

    /**
     * ingestInitialFiles should return without doing anything if the
     * specified file is an empty string.
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
     public void testIngestInitialFilesBlankSetting()
            throws NoSuchMethodException, InvocationTargetException,
                   IllegalAccessException {
        System.setProperty(WaybackSettings.WAYBACK_INDEXER_INITIAL_FILES, "");
        Method ingestMethod = WaybackIndexer.class.getDeclaredMethod("ingestInitialFiles");
        ingestMethod.setAccessible(true);
        ingestMethod.invoke(null);
    }

    public void testIngestInitialFiles()
            throws NoSuchMethodException, InvocationTargetException,
                   IllegalAccessException {
        String file = (new File(working, "initialfiles"))
                .getAbsolutePath();
        System.setProperty(WaybackSettings.WAYBACK_INDEXER_INITIAL_FILES, file);
        Method ingestMethod = WaybackIndexer.class.getDeclaredMethod("ingestInitialFiles");
        ingestMethod.setAccessible(true);
        ingestMethod.invoke(null);
        ArchiveFileDAO dao = new ArchiveFileDAO();
        assertTrue("Three file should have been ingested", dao.exists("1.arc") &&
                    dao.exists("2.arc") && dao.exists("3.arc"));
        assertTrue("Should be no files awaiting indexing",
                   dao.getFilesAwaitingIndexing().isEmpty());

    }
}

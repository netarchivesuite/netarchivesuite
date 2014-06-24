package dk.netarkivet.wayback.indexer;

import java.io.File;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.wayback.WaybackSettings;

public class ArchiveFileTester extends IndexerTestCase {
    private final File destDir = Settings.getFile(WaybackSettings.WAYBACK_BATCH_OUTPUTDIR);


    @Override
    public void setUp() {
        super.setUp();
        FileUtils.removeRecursively(destDir);
    }

    @Override
    public void tearDown() {
        super.tearDown();
        FileUtils.removeRecursively(destDir);
    }

    /**
     * Test indexing on an archive arcfile
     */
    public void testIndexerArc() {
        ArchiveFile file = new ArchiveFile();
        file.setFilename("arcfile_withredirects.arc");
        (new ArchiveFileDAO()).create(file);
        file.index();
        File outputFile = new File(destDir,
                                   file.getOriginalIndexFileName());
        assertTrue("Should have a resonable numer of lines in output file",
                   FileUtils.countLines(outputFile)>5);
    }

    /**
     * Test indexing on a metadata arcfile
     */
    public void testIndexerMetadata() {
        ArchiveFile file = new ArchiveFile();
        file.setFilename("duplicate.metadata.arc");
        (new ArchiveFileDAO()).create(file);
        file.index();
        File outputFile = new File(destDir,
                                   file.getOriginalIndexFileName());
        assertTrue("Should have a resonable numer of lines in output file",
                   FileUtils.countLines(outputFile) == 15);
    }


}

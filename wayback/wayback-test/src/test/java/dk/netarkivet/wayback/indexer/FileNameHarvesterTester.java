package dk.netarkivet.wayback.indexer;

import java.util.List;

@SuppressWarnings({ "unchecked"})
public class FileNameHarvesterTester extends IndexerTestCase {


    public void testHarvest() {
        FileNameHarvester.harvestAllFilenames();
        ArchiveFileDAO dao = new ArchiveFileDAO();
        List<ArchiveFile> files = dao.getSession().createQuery("from ArchiveFile").list();
        assertEquals("There should be four files", 6, files.size());
        FileNameHarvester.harvestAllFilenames();
        assertEquals("There should still be four files", 6, files.size());      
    }

}

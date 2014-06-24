package dk.netarkivet.wayback.indexer;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.wayback.TestInfo;

@SuppressWarnings({ "unchecked"})
public class IndexerQueueTester extends IndexerTestCase {

    @Override
    public void setUp() {
        super.setUp();
        IndexerQueue.resestSingleton();
    }

    @Override
    public void tearDown() {
        super.tearDown();
        IndexerQueue.resestSingleton();
    }

    public void testProduce()
            throws NoSuchFieldException, IllegalAccessException {
        FileNameHarvester.harvestAllFilenames();
        IndexerQueue.getInstance().populate();
        Field queueField = ReflectUtils.getPrivateField(IndexerQueue.class,
                                                        "queue");
        LinkedBlockingQueue<ArchiveFile> queue =
                (LinkedBlockingQueue<ArchiveFile>) queueField.get(null);
        assertEquals("Queue should have four objects in it", 6, queue.size());
        IndexerQueue.getInstance().populate();
        assertEquals("Queue should still have four objects in it", 6, queue.size());
    }

    public void testProduceRecent()
            throws NoSuchFieldException, IllegalAccessException {

        File dir = TestInfo.FILE_DIR;
        int i = 0;
        for (File file: dir.listFiles()) {
            if (i < 2) {
                file.setLastModified(new Date().getTime() - 7*24*3600*1000L);
                i++;
            }
        }
        FileNameHarvester.harvestRecentFilenames();
        IndexerQueue.getInstance().populate();
        Field queueField = ReflectUtils.getPrivateField(IndexerQueue.class,
                                                        "queue");
        LinkedBlockingQueue<ArchiveFile> queue =
                (LinkedBlockingQueue<ArchiveFile>) queueField.get(null);
        assertEquals("Queue should have four objects in it", 4, queue.size());
        IndexerQueue.getInstance().populate();
        assertEquals("Queue should still have four objects in it", 4, queue.size());
    }

    /**
     * testConsume has been removed from unittestersuite, as it fails.
     */
    /*  
    public void testConsume()
            throws NoSuchFieldException, IllegalAccessException,
                   InterruptedException {
        FileNameHarvester.harvest();
        IndexerQueue.getInstance().populate();
       Runnable consumerRunnable = new Runnable() {

            public void run() {
                IndexerQueue.getInstance().consume();
            }
        };
        (new Thread(consumerRunnable)).start();
        Thread.sleep(100000L);
        Field queueField = ReflectUtils.getPrivateField(IndexerQueue.class,
                                                        "queue");
        LinkedBlockingQueue<ArchiveFile> queue =
                (LinkedBlockingQueue<ArchiveFile>) queueField.get(null);
        assertEquals("DAO should have four indexed files", 4, (new ArchiveFileDAO()).findByCriteria(
                Restrictions.eq("indexed", true)).size());
        assertTrue("Queue should be empty now", queue.isEmpty());
        assertEquals("Should have four files", 4, tempdir.listFiles().length);
    } */

}

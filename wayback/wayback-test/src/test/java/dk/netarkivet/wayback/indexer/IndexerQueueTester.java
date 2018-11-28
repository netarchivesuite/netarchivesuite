/*
 * #%L
 * Netarchivesuite - wayback - test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.wayback.indexer;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.wayback.TestInfo;

@SuppressWarnings({"unchecked"})
public class IndexerQueueTester extends IndexerTestCase {

    @Override
    @Before
    public void setUp() {
        super.setUp();
        IndexerQueue.resestSingleton();
    }

    @Override
    @After
    public void tearDown() {
        super.tearDown();
        IndexerQueue.resestSingleton();
    }

    @Test
    public void testProduce() throws NoSuchFieldException, IllegalAccessException {
        FileNameHarvester.harvestAllFilenames();
        IndexerQueue.getInstance().populate();
        Field queueField = ReflectUtils.getPrivateField(IndexerQueue.class, "queue");
        LinkedBlockingQueue<ArchiveFile> queue = (LinkedBlockingQueue<ArchiveFile>) queueField.get(null);
        assertEquals("Queue should have four objects in it", 6, queue.size());
        IndexerQueue.getInstance().populate();
        assertEquals("Queue should still have four objects in it", 6, queue.size());
    }

    @Test
    public void testProduceRecent() throws NoSuchFieldException, IllegalAccessException {

        File dir = TestInfo.FILE_DIR;
        int i = 0;
        for (File file : dir.listFiles()) {
            if (i < 2) {
                file.setLastModified(new Date().getTime() - 7 * 24 * 3600 * 1000L);
                i++;
            }
        }
        FileNameHarvester.harvestRecentFilenames();
        IndexerQueue.getInstance().populate();
        Field queueField = ReflectUtils.getPrivateField(IndexerQueue.class, "queue");
        LinkedBlockingQueue<ArchiveFile> queue = (LinkedBlockingQueue<ArchiveFile>) queueField.get(null);
        assertEquals("Queue should have four objects in it", 4, queue.size());
        IndexerQueue.getInstance().populate();
        assertEquals("Queue should still have four objects in it", 4, queue.size());
    }

    /**
     * testConsume has been removed from unittestersuite, as it fails.
     */
    /*
     * public void testConsume() throws NoSuchFieldException, IllegalAccessException, InterruptedException {
     * FileNameHarvester.harvest(); IndexerQueue.getInstance().populate(); Runnable consumerRunnable = new Runnable() {
     * 
     * public void run() { IndexerQueue.getInstance().consume(); } }; (new Thread(consumerRunnable)).start();
     * Thread.sleep(100000L); Field queueField = ReflectUtils.getPrivateField(IndexerQueue.class, "queue");
     * LinkedBlockingQueue<ArchiveFile> queue = (LinkedBlockingQueue<ArchiveFile>) queueField.get(null);
     * assertEquals("DAO should have four indexed files", 4, (new ArchiveFileDAO()).findByCriteria(
     * Restrictions.eq("indexed", true)).size()); assertTrue("Queue should be empty now", queue.isEmpty());
     * assertEquals("Should have four files", 4, tempdir.listFiles().length); }
     */

}

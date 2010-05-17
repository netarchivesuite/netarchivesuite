/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package dk.netarkivet.wayback.indexer;

import java.lang.reflect.Field;
import java.util.concurrent.LinkedBlockingQueue;

import dk.netarkivet.testutils.ReflectUtils;

import junit.framework.TestCase;
import org.hibernate.criterion.Restrictions;

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
        FileNameHarvester.harvest();
        IndexerQueue.getInstance().populate();
        Field queueField = ReflectUtils.getPrivateField(IndexerQueue.class,
                                                        "queue");
        LinkedBlockingQueue<ArchiveFile> queue =
                (LinkedBlockingQueue<ArchiveFile>) queueField.get(null);
        assertEquals("Queue should have four objects in it", 4, queue.size());
        IndexerQueue.getInstance().populate();
        assertEquals("Queue should still have four objects in it", 4, queue.size());
    }

   /* public void testConsume()
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
    }*/

}

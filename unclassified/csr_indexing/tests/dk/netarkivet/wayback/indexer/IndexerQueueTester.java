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

import java.util.concurrent.LinkedBlockingQueue;

import junit.framework.TestCase;

public class IndexerQueueTester extends IndexerTestCase {

    public void testProduce()
            throws NoSuchFieldException, IllegalAccessException {
        IndexerQueue.getInstance().populate();
        LinkedBlockingQueue<ArchiveFile> queue =
                (LinkedBlockingQueue<ArchiveFile>)
                        IndexerQueue.class.getField("queue").get(null);
        assertEquals("Queue should have two objects in it", 2, queue.size());
        IndexerQueue.getInstance().populate();
        assertEquals("Queue should still have two objects in it", 2, queue.size());
    }

}

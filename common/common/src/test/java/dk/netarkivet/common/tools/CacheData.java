/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package dk.netarkivet.common.tools;

import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import dk.netarkivet.archive.indexserver.CDXIndexCache;
import dk.netarkivet.archive.indexserver.DedupCrawlLogIndexCache;
import dk.netarkivet.archive.indexserver.FullCrawlLogIndexCache;
import dk.netarkivet.archive.indexserver.MultiFileBasedCache;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.UnknownID;

/**
 * An interface to the various caches in the indexserver.
 *
 */

public class CacheData {
    /** Don't have the time to implement the full "Simple"CmdlineTool iface. */
    public static void main(String[] argv) throws NoSuchFieldException,
            IllegalAccessException {
        if (argv.length < 2) {
            System.out.println("Usage: CacheData <cache> <jobID>+");
            System.exit(1);
        }
        JMSConnectionDummy.setUp();
        String cacheName = argv[0];
        System.arraycopy(argv, 1, argv, 0, argv.length - 1);
        MultiFileBasedCache<Long> cache = null;

        if (cacheName.equals("fullcrawllog")) {
            cache = new FullCrawlLogIndexCache();
        } else if (cacheName.equals("dedupcrawllog")) {
            cache = new DedupCrawlLogIndexCache();
        } else if (cacheName.equals("cdx")) {
            cache = new CDXIndexCache();
        } else {
            throw new ArgumentNotValid("Can't find cache " + cacheName
                    + ", try one of fullcrawllog, dedupcrawllog, cdx");
        }
        Set<Long> IDs = new HashSet<Long>();
        for (String s : argv) {
            IDs.add(Long.parseLong(s));
        }
        long t1 = System.currentTimeMillis();
        System.out.println("Starting to cache at " + t1);
        Set<Long> cached = cache.cache(IDs);
        System.out.println(cached);
        long t2 = System.currentTimeMillis();
        System.out.println("Done caching at " + t2 + " total time " + (t2 - t1));
        if (cached.equals(IDs)) {
            System.out.println("Cached in " + cache.getIndex(cached));
        } else {
            System.out.println("Asked for\n" + IDs + "\n but got \n" + cached);
        }
    }

    public static class JMSConnectionDummy extends JMSConnection {

        public static JMSConnection getInstance() throws UnknownID, IOFailure {
            return new JMSConnectionDummy();
        }

        protected QueueConnectionFactory getQueueConnectionFactory()
                throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        protected TopicConnectionFactory getTopicConnectionFactory()
                throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        protected Queue getQueue(String queueName) throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        protected Topic getTopic(String topicName) throws JMSException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public static void setUp() throws NoSuchFieldException,
                IllegalAccessException {
            Field f = JMSConnection.class.getDeclaredField("instance");
            f.setAccessible(true);
            f.set(null, new JMSConnectionDummy());
        }

        public void setListener(ChannelID mq, MessageListener ml) throws IOFailure {
        }

        public void sendMessage(NetarkivetMessage msg, ChannelID to) {
            throw new IllegalState("Attempt to use JMS, should fail");
        }
    }
}

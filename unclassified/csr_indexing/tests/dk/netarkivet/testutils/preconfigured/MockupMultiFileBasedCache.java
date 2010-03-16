/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.testutils.preconfigured;

import dk.netarkivet.archive.indexserver.MultiFileBasedCache;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

/**
 * JobIndexCache mockup that either returns null, throws exception, waits,
 * or returns a file with the given jobids.
 */
public class MockupMultiFileBasedCache
        extends MultiFileBasedCache<Long>
        implements TestConfigurationIF {
    private Object o;
    private static final int TIMEOUT = 2000;

    /**
     * Constructor for a MultiFileBasedCache
     */
    public MockupMultiFileBasedCache() {
        super("TEST");
    }

    public enum Mode{SILENT, REPLYING, REPLYING_DIR, FAILING, WAITING};

    private Mode mode = Mode.SILENT;

    public int cacheCalled = 0;
    public Set<Long> cacheParameter;
    public boolean woken = false;

    public void setUp() {
        cacheCalled = 0;
        mode = Mode.SILENT;
        cacheParameter = null;
        o = new Object();
        woken = false;
    }

    public void tearDown() {
        for (File f : getCacheDir().listFiles()) {
            FileUtils.removeRecursively(f);
        }
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    protected Set<Long> cacheData(Set<Long> jobIDs) {
        cacheCalled++;
        cacheParameter = jobIDs;
        switch(mode) {
            case SILENT:
                return null;
            case WAITING:
                try {
                    long before = System.currentTimeMillis();
                    synchronized(o) {
                        o.notifyAll();
                        o.wait(TIMEOUT);
                        o.notifyAll();
                    }
                    woken |= System.currentTimeMillis() - before < TIMEOUT;

                } catch (InterruptedException e) {
                    return null;
                }
                return null;
            case REPLYING:
                try {
                    File temp = getCacheFile(jobIDs);
                    temp.deleteOnExit();
                    FileOutputStream fos = new FileOutputStream(temp);
                    for (Long job : jobIDs) {
                        fos.write(job.intValue());
                    }
                    fos.close();
                    return jobIDs;
                } catch (IOException e) {
                    System.out.println("Error in mock-up: " + e);
                    e.printStackTrace();
                    return null;
                }
            case REPLYING_DIR:
                try {
                    File tempDir = getCacheFile(jobIDs);
                    tempDir.deleteOnExit();
                    tempDir.mkdir();
                    OutputStream fos = new FileOutputStream(new File(tempDir, "foo"));
                    /*
                    for (Long job : jobIDs) {
                        FileOutputStream fos = new FileOutputStream(
                                new File(tempDir, job.toString()));
                        fos.write(job.intValue());
                        fos.close();
                    }*/
                    fos.close();
                    return jobIDs;
                } catch (IOException e) {
                    System.out.println("Error in mock-up: " + e);
                    e.printStackTrace();
                    return null;
                }
            case FAILING:
                throw new IOFailure("This is a failing testhandler!");
            default:
                return null;
        }
    }
}

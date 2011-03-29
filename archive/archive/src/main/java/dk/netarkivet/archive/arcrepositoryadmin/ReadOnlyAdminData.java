/* File:     $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */

package dk.netarkivet.archive.arcrepositoryadmin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.IllegalState;

/**
 * A version of AdminData that cannot be changed, but which allows
 * synchronization through a file.
 *
 * To avoid excessive reading of the admin data file and constant stat() calls,
 * users of this are required call synchronize() before major chunks of use to
 * ensure that the data are up to date.
 *
 * Implementation note: Two alternative synch strategies are<p>
 * 1) Recreate ReadOnlyAdminData before every use -- this requires reading the
 *    entire file again (millions of lines).<br>
 * 2) Synchronize at every entry point (hasEntry, getState etc) -- this requires
 *    an expensive stat() call before every action, costly when iterating.
 *    
 * @deprecated This class is only used by the deprecated class
 * FileBasedActiveBitPreservation.
 */
@Deprecated 
public class ReadOnlyAdminData extends AdminData {
    /** The log.*/
    private Log log = LogFactory.getLog(getClass().getName());
    
    /** The time the underlying file (adminDataFile) was last read in.
     * If 0, we have never read admin data (the file doesn't exist).
     */
    protected long lastModified = 0;

    /**
     * @see AdminData#AdminData()
     */
    public ReadOnlyAdminData() {
        super();
    }

    /**
     * Returns _an_ instance if admin data. This is _not_ a singleton.
     * @return An instance of ReadOnlyAdminData
     */
    public static ReadOnlyAdminData getInstance() {
        return new ReadOnlyAdminData();
    }

    /** Read admin data. This should not be used, use synchronize instead, which
     * only rereads when necessary.
     */
    protected void read() {
        lastModified = adminDataFile.lastModified();
        super.read();
    }

    /**
     * Make sure that the internal admin data set is synchronized to the file.
     */
    public void synchronize() {
        if (adminDataFile.lastModified() > lastModified) {
            storeEntries.clear();
            knownBitArchives.clear();
            read();
        }
        if (lastModified == 0) {
            String msg = "Admin data (file: " + adminDataFile.getAbsolutePath() 
                + ") not created in time for reading.";
            log.warn(msg);
            throw new IllegalState(msg);
        }
    }
}

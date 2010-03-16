/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.archive.arcrepository;

import java.io.File;

import dk.netarkivet.archive.arcrepositoryadmin.Admin;
import dk.netarkivet.archive.arcrepositoryadmin.AdminFactory;
import dk.netarkivet.archive.arcrepositoryadmin.UpdateableAdminData;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Utility class to wait for some file to be uploaded to the bitarchive.
 */
public class UploadWaiting {

    public static final long SHORT_TIME = 10;

    /**
     * Assuming that someone asked the ArcRepository to store the given file,
     * waits (but not forever) until all bitarchives are recorded as having
     * completed (and verified) their upload.
     *
     * @param file the file being uploaded
     * @param o    an object to synchronize with (for .wait()).
     * @return Waited time in milliseconds
     * @throws IOFailure if the upload is not completed in less than
     *                   Settings.SHORT_TIMEOUT
     */
    public static long waitForUpload(File file, Object o) throws IOFailure {
        long timeout = TestInfo.SHORT_TIMEOUT;
        final long short_time = SHORT_TIME;
        int time_waited = 0;
        while (time_waited < timeout && !hasAllCompleted(file.getName())) {
            synchronized (o) {
                try {
                    o.wait(short_time);
                    time_waited += short_time;
                } catch (InterruptedException e) {
                    //Interrupted? OK, let's go on.
                }
            }
        }
        if (!hasAllCompleted(file.getName())) {
            throw new IOFailure("Upload of '" + file.getName() + "' timed out");
        }
        return time_waited;
    }

    private static boolean hasAllCompleted(String arcFile) {
        Admin ad = AdminFactory.getInstance();

        if (ad.hasReplyInfo(arcFile)) {
            return false;
        }
        
        for(Replica rep : Replica.getKnown()) {
            if(!ad.hasState(arcFile, rep.getIdentificationChannel().getName())) {
        	return false;
            }
            if(ad.getState(arcFile, rep.getIdentificationChannel().getName()) 
        	    != ReplicaStoreState.UPLOAD_COMPLETED) {
        	return false;
            }
        }
        return true;
    }
}

/*
 * #%L
 * Netarchivesuite - archive - test
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
package dk.netarkivet.archive.arcrepository;

import java.io.File;

import dk.netarkivet.archive.arcrepositoryadmin.Admin;
import dk.netarkivet.archive.arcrepositoryadmin.AdminFactory;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Utility class to wait for some file to be uploaded to the bitarchive.
 */
public class UploadWaiting {

    public static final long SHORT_TIME = 10;

    /**
     * Assuming that someone asked the ArcRepository to store the given file, waits (but not forever) until all
     * bitarchives are recorded as having completed (and verified) their upload.
     *
     * @param file the file being uploaded
     * @param o an object to synchronize with (for .wait()).
     * @return Waited time in milliseconds
     * @throws IOFailure if the upload is not completed in less than Settings.SHORT_TIMEOUT
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
                    // Interrupted? OK, let's go on.
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

        for (Replica rep : Replica.getKnown()) {
            if (!ad.hasState(arcFile, rep.getIdentificationChannel().getName())) {
                return false;
            }
            if (ad.getState(arcFile, rep.getIdentificationChannel().getName()) != ReplicaStoreState.UPLOAD_COMPLETED) {
                return false;
            }
        }
        return true;
    }
}

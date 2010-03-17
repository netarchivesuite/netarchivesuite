/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.common.distribute;

import java.io.File;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.SettingsFactory;

/**
 * Factory for creating remote files.
 */
public class RemoteFileFactory extends SettingsFactory<RemoteFile> {
    /**
     * Create a remote file that handles the transport of the remote file data.
     * This method is used by the sender to prepare the transport over JMS.
     * @param file The File object to make accessable on another machine
     * @param useChecksums Whether transfers should be doublechecked with
     * checksums. Added value is access to checksum of objects.
     * @param fileDeletable If true, the local file will be deleted when it is
     * no longer needed.
     * @param multipleDownloads Whether this file should be allowed to be
     * transferred more than once. 
     * @return A RemoteFile instance encapsulating the file argument.
     */
    public static RemoteFile getInstance(File file, boolean useChecksums,
                                         boolean fileDeletable,
                                         boolean multipleDownloads) {
        ArgumentNotValid.checkNotNull(file, "File file");
        return SettingsFactory.getInstance(
                CommonSettings.REMOTE_FILE_CLASS, file, useChecksums, fileDeletable,
                multipleDownloads);
    }

    /** Same as getInstance(file, false, true, false).
     * @param file The file to move to another computer.
     */
    public static RemoteFile getMovefileInstance(File file) {
        return getInstance(file, false, true, false);   
    }

    /** Same as getInstance(file, false, false, false).
     * @param file The file to copy to another computer.
     */
    public static RemoteFile getCopyfileInstance(File file) {
        return getInstance(file, false, false, false);
    }

    /** Same as getInstance(file, false, false, false).
     * @param file The file to copy to another computer.
     */
    public static RemoteFile getDistributefileInstance(File file) {
        return getInstance(file, true, false, true);
    }
}

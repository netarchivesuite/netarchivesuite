/*
 * #%L
 * Netarchivesuite - common
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
package dk.netarkivet.common.distribute;

import java.io.File;

import org.archive.io.ArchiveRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SettingsFactory;

/**
 * Factory for creating remote files.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class RemoteFileFactory extends SettingsFactory<RemoteFile> {

    /** A named logger for this class. */
    private static final transient Logger log = LoggerFactory.getLogger(RemoteFileFactory.class);

    /**
     * Create a remote file that handles the transport of the remote file data. This method is used by the sender to
     * prepare the transport over JMS.
     *
     * @param file The File object to make accessable on another machine
     * @param useChecksums Whether transfers should be doublechecked with checksums. Added value is access to checksum
     * of objects.
     * @param fileDeletable If true, the local file will be deleted when it is no longer needed.
     * @param multipleDownloads Whether this file should be allowed to be transferred more than once.
     * @return A RemoteFile instance encapsulating the file argument.
     */
    public static RemoteFile getInstance(File file, boolean useChecksums, boolean fileDeletable,
            boolean multipleDownloads, RemoteFileSettings connectionParams) {
        ArgumentNotValid.checkNotNull(file, "File file");
        return SettingsFactory.getInstance(CommonSettings.REMOTE_FILE_CLASS, file, useChecksums, fileDeletable,
                multipleDownloads, connectionParams);
    }

    /* Same as the above method, but without the required RemoteFileSettings. */
    public static RemoteFile getInstance(File file, boolean useChecksums, boolean fileDeletable,
            boolean multipleDownloads) {
        ArgumentNotValid.checkNotNull(file, "File file");
        return SettingsFactory.getInstance(CommonSettings.REMOTE_FILE_CLASS, file, useChecksums, fileDeletable,
                multipleDownloads);
    }

    /**
     * Get an instance connected to an ArchiveRecord. Records are not deletable so there is no concept of a "movefile"
     * instance.
     *
     * @param record
     * @return the file to be copied.
     */
    public static RemoteFile getExtendedInstance(ArchiveRecord record) {
        return SettingsFactory.getInstance(CommonSettings.REMOTE_FILE_CLASS, record);
    }

    /**
     * Returns true iff the defined RemoteFile class has a factory method with signature public static RemoteFile
     * getInstance(ArchiveRecord record)
     *
     * @return true if using an extended remote file.
     */
    public static boolean isExtendedRemoteFile() {
        String remoteFileClass = Settings.get(CommonSettings.REMOTE_FILE_CLASS);
        try {
            Class theClass = Class.forName(remoteFileClass);
            try {
                theClass.getMethod("getInstance", ArchiveRecord.class);
                return true;
            } catch (NoSuchMethodException e) {
                return false;
            }
        } catch (ClassNotFoundException e) {
            log.error("Unknown RemoteFile class :{}", remoteFileClass);
            throw new ArgumentNotValid("Unknown RemoteFile class :" + remoteFileClass);
        }
    }

    /**
     * Same as getInstance(file, false, true, false).
     *
     * @param file The file to move to another computer.
     */
    public static RemoteFile getMovefileInstance(File file) {
        return getInstance(file, false, true, false);
    }

    /**
     * Same as getInstance(file, false, false, false, null).
     *
     * @param file The file to copy to another computer.
     */
    public static RemoteFile getCopyfileInstance(File file) {
        return getInstance(file, false, false, false);
    }

    /**
     * Same as getInstance(file, false, false, false, connectionParams).
     *
     * @param file The file to copy to another computer.
     */
    public static RemoteFile getCopyfileInstance(File file, RemoteFileSettings connectionParams) {
        if (connectionParams != null) {
            return getInstance(file, false, false, false, connectionParams);
        } else {
            return getInstance(file, false, false, false);
        }
    }

    /**
     * Same as getInstance(file, false, false, false).
     *
     * @param file The file to copy to another computer.
     */
    public static RemoteFile getDistributefileInstance(File file) {
        return getInstance(file, true, false, true);
    }

}

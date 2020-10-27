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
package dk.netarkivet.common.distribute.arcrepository;

/**
 * This class encapsulates the different upload states, while storing a file in the archive of a replica . Used by the
 * classes ArcRepository, AdminData, and ArcRepositoryEntry.
 * <p>
 * TODO Needs localisation.
 *
 * @see dk.netarkivet.archive.arcrepository.ArcRepository
 * @see dk.netarkivet.archive.arcrepositoryadmin.AdminData
 * @see dk.netarkivet.archive.arcrepositoryadmin.ArcRepositoryEntry
 */
public enum ReplicaStoreState {

    /** Upload to a replica archive has started. */
    UPLOAD_STARTED,
    /** Data has been successfully uploaded to a replica archive. */
    DATA_UPLOADED,
    /**
     * Upload to replica archive completed, which means that it has been verified by a checksumJob.
     */
    UPLOAD_COMPLETED,
    /** Upload to the replica archive has failed. */
    UPLOAD_FAILED,
    /**
     * If it is unknown whether a file has been successfully uploaded to a replica or not. Used in the database.
     */
    UNKNOWN_UPLOAD_STATE;

    public static ReplicaStoreState fromOrdinal(int ordinal) {
        switch (ordinal) {
        case 0:
            return UPLOAD_STARTED;
        case 1:
            return DATA_UPLOADED;
        case 2:
            return UPLOAD_COMPLETED;
        case 3:
            return UPLOAD_FAILED;
        default:
            // anything else is unknown.
            return UNKNOWN_UPLOAD_STATE;
        }
    }

}

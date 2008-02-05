/* File:        $Id$
 * Revision:    $Revision$
 * Date:        $Date$
 * Author:      $Author$
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
package dk.netarkivet.archive.arcrepository.bitpreservation;

import java.util.Date;

import dk.netarkivet.common.distribute.arcrepository.Location;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;

/**
 * Active bitpreservation is assumed to have access to admin data and bitarchives.
 * Operations may request informations from the bitarchive by sending batch jobs,
 * reading admin data directly, or reading from cached information from either.
 */
public interface ActiveBitPreservation {
    // General status

    /**
     * Get details of the status of one file in the bitarchives
     *
     * @return The details of one file.
     */
    FilePreservationStatus getFilePreservationStatus(String filename);

    // Check status for bitarchives

    /**
     * Return a list of files marked as missing on this location.
     * A file is considered missing if it does not exist compared to
     * admin data. Guaranteed not to recheck the archive, simply returns the
     * number returned by the last test.
     *
     * @param location The location to get missing files from.
     *
     * @return A list of missing files.
     *
     * @throws IOFailure if the list cannot be generated.
     */
    Iterable<String> getMissingFiles(Location location);

    /**
     * Return a list of files with changed checksums on this location.
     * A file is considered changed if checksum does not compare to
     * admin data. Guaranteed not to recheck the archive, simply returns the
     * number returned by the last test.
     *
     * @param location The location to get list of changed files from.
     *
     * @return A list of fiels with changed checksums.
     *
     * @throws IOFailure if the list cannot be generated.
     */
    Iterable<String> getChangedFiles(Location location);

    /** Update the list of files in a given bitarchive. This will be used for
     * the next call to getMissingFiles.
     *
     * @param bitarchive The bitarchive to update list of files for.
     */
    void findMissingFiles(Location bitarchive);

    /** Update the list of checksums in a given bitarchive. This will be used
     * for the next call to getChangedFiles.
     *
     * @param bitarchive The bitarchive to update list of files for.
     */
    void findChangedFiles(Location bitarchive);

    /**
     * Return the number of missing files for location. Guaranteed not to
     * recheck the archive, simply returns the number returned by the last
     * test.
     *
     * @param location The location to get number of missing files from.
     *
     * @return The number of missing files.
     */
    long getNumberOfMissingFiles(Location location);

    /**
     * Return the number of changed files for location. Guaranteed not to
     * recheck the archive, simply returns the number returned by the last
     * test.
     *
     * @param location The location to get number of changed files from.
     *
     * @return The number of changed files.
     */
    long getNumberOfChangedFiles(Location location);

    /**
     * Return the total number of files for location. Guaranteed not to
     * recheck the archive, simply returns the number returned by the last
     * update.
     *
     * @param location The location to get number of files from.
     *
     * @return The number of files.
     */
    long getNumberOfFiles(Location location);

    /**
     * Return the date for last check of missing files for location. Guaranteed
     * not to recheck the archive, simply returns the number returned by the
     * last test.
     *
     * @param location The location to get date for changed files from.
     *
     * @return The date for last check of missing files.
     */
    Date getDateForMissingFiles(Location location);

    /**
     * Return the date for last check of changed files for location. Guaranteed
     * not to recheck the archive, simply returns the number returned by the
     * last test.
     *
     * @param location The location to get date for changed files from.
     *
     * @return The date for last check of changed files.
     */
    Date getDateForChangedFiles(Location location);

    // Update files in bitarchives

    /**
     * Check that files are indeed missing, and present in admin data and
     * reference location. If so, upload missing files from reference location
     * to this location.
     *
     * @param location The location to restore files to
     * @param filename The names of the files.
     *
     * @throws IOFailure if the file cannot be reestablished
     * @throws PermissionDenied if the file is not in correct state
     */
    void reuploadMissingFiles(Location location, String... filename);

    /**
     * Check that file checksum is indeed different to admin data and reference
     * location. If so, remove missing file and upload it from reference
     * location to this location.
     *
     * @param location The location to restore file to
     * @param filename The name of the file.
     * @param checksum
     * @throws IOFailure if the file cannot be reestablished
     * @throws PermissionDenied if the file is not in correct state
     */
    void replaceChangedFile(Location location, String filename,
                            String credentails, String checksum);

    // Check status for admin data

    /**
     * Return a list of files present in bitarchive but missing in AdminData.
     *
     * @return A list of missing files.
     *
     * @throws IOFailure if the list cannot be generated.
     */
    Iterable getMissingFilesForAdminData();

    /**
     * Return a list of files with wrong checksum or status in admin data.
     *
     * @return A list of files with wrong checksum or status.
     *
     * @throws IOFailure if the list cannot be generated.
     */
    Iterable getChangedFilesForAdminData();

    // Update admin data

    /**
     * Reestablish admin data to match bitarchive states for files.
     *
     * @param filename The files to reestablish state for.
     *
     * @throws PermissionDenied if the file is not in correct state
     */
    void addMissingFilesToAdminData(String... filename);

    /**
     * Reestablish admin data to match bitarchive states for file.
     *
     * @param filename The file to reestablish state for.
     *
     * @throws PermissionDenied if the file is not in correct state
     */
    void changeStatusForAdminData(String filename);
}
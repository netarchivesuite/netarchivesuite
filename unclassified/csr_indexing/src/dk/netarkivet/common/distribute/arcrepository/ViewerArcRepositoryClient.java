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
package dk.netarkivet.common.distribute.arcrepository;

import java.io.File;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.batch.FileBatchJob;


/**
 * Implements the Facade pattern to shield off the methods in
 * JMSArcRepositoryClient not to be used by the bit preservation system.
 */
public interface ViewerArcRepositoryClient  {
    /** Call on shutdown to release external resources. */
    void close();

    /**
     * Gets a single ARC record out of the ArcRepository.
     *
     * @param arcfile The name of a file containing the desired record.
     * @param index   The offset of the desired record in the file
     * @return a BitarchiveRecord-object, or null if request times out or object
     * is not found.
     * @exception ArgumentNotValid If the get operation failed.
     */
    BitarchiveRecord get(String arcfile, long index) throws ArgumentNotValid;

    /**
     * Retrieves a file from an ArcRepository and places it in a local file.

     * @param arcfilename Name of the arcfile to retrieve.
     * @param replica The bitarchive to retrieve the data from.
     * @param toFile Filename of a place where the file fetched can be put.
     * @throws IOFailure if there are problems getting a reply or the file
     * could not be found.
     */
    void getFile(String arcfilename, Replica replica, File toFile);

    /**
     * Runs a batch batch job on each file in the ArcRepository.
     *
     * @param job An object that implements the FileBatchJob interface. The
     *  initialize() method will be called before processing and the finish()
     *  method will be called afterwards. The process() method will be called
     *  with each File entry.
     *
     * @param replicaId The archive to execute the job on.
     * @return The status of the batch job after it ended.
     */
    BatchStatus batch(FileBatchJob job, String replicaId);
}

/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.common.distribute.arcrepository;

import java.io.File;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Implements the Facade pattern to shield off the methods in
 * JMSArcRepositoryClient not to be used by the harvest system.
 */
public interface HarvesterArcRepositoryClient {
    /** Call on shutdown to release external resources. */
    void close();

    /**
     * Store the given file in the ArcRepository.  After storing, the file is
     * deleted.
     *
     * @param file A file to be stored. Must exist.
     * @throws IOFailure thrown if store is unsuccesful, or failed to clean
     * up files after the store operation.
     * @throws ArgumentNotValid if file parameter is null or file is not an
     *                          existing file.
     */
    void store(File file) throws IOFailure, ArgumentNotValid;
}

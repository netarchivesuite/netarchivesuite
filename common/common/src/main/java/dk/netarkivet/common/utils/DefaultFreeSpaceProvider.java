/* File:        $Id$
 * Revision:    $Rev$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.common.utils;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Default Free Space Provider of the number of bytes free on the file system.
 */
public class DefaultFreeSpaceProvider implements FreeSpaceProvider {
    
    /** The error logger we notify about error messages on. */
    private Log log = LogFactory.getLog(getClass());

    /**
     * Returns the number of bytes free on the file system that the given file
     * resides on. Will return 0 on non-existing files.
     *
     * @param f a given file
     * @return the number of bytes free on the file system where file f resides.
     * 0 if the file cannot be found.
     */
    public long getBytesFree(File f) {
        ArgumentNotValid.checkNotNull(f, "File f");
        if (!f.exists()) {
            log.warn("The file '" +  f.getAbsolutePath()
                    + "' does not exist. The value 0 returned.");
            return 0;
        }
        return f.getUsableSpace();
    }
}

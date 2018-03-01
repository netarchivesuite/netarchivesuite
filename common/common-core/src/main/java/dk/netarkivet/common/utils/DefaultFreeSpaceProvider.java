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

package dk.netarkivet.common.utils;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Default Free Space Provider of the number of bytes free on the file system.
 */
public class DefaultFreeSpaceProvider implements FreeSpaceProvider {

    /** The error logger we notify about error messages on. */
    private static final Logger log = LoggerFactory.getLogger(DefaultFreeSpaceProvider.class);

    /**
     * Returns the number of bytes free on the file system that the given file resides on. Will return 0 on non-existing
     * files.
     *
     * @param f a given file
     * @return the number of bytes free on the file system where file f resides. 0 if the file cannot be found.
     */
    public long getBytesFree(File f) {
        ArgumentNotValid.checkNotNull(f, "File f");
        if (!f.exists()) {
            log.warn("The file '{}' does not exist. The value 0 returned.", f.getAbsolutePath());
            return 0;
        }
        return f.getUsableSpace();
    }

}

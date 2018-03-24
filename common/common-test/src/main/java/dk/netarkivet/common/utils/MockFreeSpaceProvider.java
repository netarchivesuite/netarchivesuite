/*
 * #%L
 * Netarchivesuite - common - test
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

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Mock Free Space Provider of the number of bytes free on the file system.
 */
public class MockFreeSpaceProvider implements FreeSpaceProvider {

    /**
     * 1 TB in bytes.
     */
    public static final long ONETB = 1024 * 1024 * 1024 * 1024;

    /**
     * Returns 1 TB of bytes as the number of bytes free on the file system that the given file resides on.
     *
     * @param f a given file
     * @return 1 TB of bytes free
     */
    public long getBytesFree(File f) {
        ArgumentNotValid.checkNotNull(f, "File f");

        return ONETB;
    }
}

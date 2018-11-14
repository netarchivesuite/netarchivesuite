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
package dk.netarkivet.archive.bitarchive;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import dk.netarkivet.common.utils.batch.FileBatchJob;

/** This class throws an exception. */
@SuppressWarnings({"serial"})
public class ExceptionBatchInit extends FileBatchJob {
    public void initialize(OutputStream os) {
        throw new SecurityException("Security exception in initialize");
    }

    public boolean processFile(File file, OutputStream os) {
        String name = file.getName();
        try {
            os.write(name.getBytes());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void finish(OutputStream os) {
    }
}

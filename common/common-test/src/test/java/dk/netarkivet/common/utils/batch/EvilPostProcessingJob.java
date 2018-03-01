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
package dk.netarkivet.common.utils.batch;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.utils.FileUtils;

@SuppressWarnings({"serial"})
public class EvilPostProcessingJob extends FileBatchJob {

    @Override
    public void finish(OutputStream os) {
        // TODO Auto-generated method stub
    }

    @Override
    public void initialize(OutputStream os) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean processFile(File file, OutputStream os) {
        try {
            os.write((file.getName() + "\n").getBytes());
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    @Override
    public boolean postProcess(InputStream input, OutputStream output) {
        Log log = LogFactory.getLog(this.getClass());
        try {
            File[] files = FileUtils.getTempDir().listFiles();

            log.info("directory batch contains " + files.length + " files.");

            for (File fil : files) {
                log.warn("deleting: " + fil.getName());
                fil.delete();
            }

            return true;
        } catch (Exception e) {
            log.warn(e.getMessage());
            return false;
        }
    }
}

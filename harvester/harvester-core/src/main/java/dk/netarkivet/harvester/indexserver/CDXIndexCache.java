/*
 * #%L
 * Netarchivesuite - harvester
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

package dk.netarkivet.harvester.indexserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import dk.netarkivet.common.distribute.indexserver.JobIndexCache;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.FileUtils;

/**
 * A cache that serves CDX index files for job IDs.
 * <p>
 * Notice that since data for some IDs may not be available, the actual cached file might not correspond in its content
 * to what was asked for. For instance, if asking for data for IDs 2, 3, and 4, and 3 fails, a cached file for IDs 2 and
 * 4 will be returned. There is currently no way to tell if you got everything you asked for.
 * <p>
 * This cache uses the Unix sort(1) command as an external process call, as that one is optimized for handling large,
 * disk-based sorts.
 */
public class CDXIndexCache extends CombiningMultiFileBasedCache<Long> implements JobIndexCache {

    /** A suffix used by the sortFile method in the sorting process. */
    private static final String WORK_SUFFIX = ".unsorted";

    /**
     * Creates a new cache for CDX index files.
     */
    public CDXIndexCache() {
        super("cdxindex", new CDXDataCache());
    }

    /**
     * Combine parts of an index into one big index.
     *
     * @param filesFound A map of IDs and the files caching their content.
     */
    protected void combine(Map<Long, File> filesFound) {
        File resultFile = getCacheFile(filesFound.keySet());
        concatenateFiles(filesFound.values(), resultFile);
        File workFile = new File(resultFile.getAbsolutePath() + WORK_SUFFIX);
        workFile.deleteOnExit();
        try {
            FileUtils.sortCDX(resultFile, workFile);
            workFile.renameTo(resultFile);
        } finally {
            FileUtils.remove(workFile);
        }
    }

    /**
     * Concatenate a set of files into a single file.
     *
     * @param files The files to concatenate.
     * @param resultFile The file where the files are concatenated into.
     */
    private static void concatenateFiles(Collection<File> files, File resultFile) {
        try {
            BufferedWriter out = null;
            try {
                out = new BufferedWriter(new FileWriter(resultFile));
                for (File f : files) {
                    BufferedReader in = null;
                    try {
                        in = new BufferedReader(new FileReader(f));
                        String s;
                        while ((s = in.readLine()) != null) {
                            out.write(s);
                            out.newLine();
                        }
                    } finally {
                        if (in != null) {
                            in.close();
                        }
                    }
                }
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Couldn't combine indexes for " + files.size() + " jobs into " + resultFile, e);
        }
    }

    @Override
    public void requestIndex(Set<Long> jobSet, Long harvestId) {
        throw new NotImplementedException("This feature is not implemented for this type of cache");
    }

}

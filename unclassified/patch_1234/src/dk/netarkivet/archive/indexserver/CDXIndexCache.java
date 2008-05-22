/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.archive.indexserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import dk.netarkivet.common.distribute.indexserver.JobIndexCache;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.ProcessUtils;

/**
 * A cache that serves CDX index files for job IDs.
 *
 * Notice that since data for some IDs may not be available, the actual
 * cached file might not correspond in its content to what was asked for.
 * For instance, if asking for data for IDs 2, 3, and 4, and 3 fails, a
 * cached file for IDs 2 and 4 will be returned.  There is currently no
 * way to tell if you got everything you asked for.
 *
 * This cache uses the Unix sort(1) command as an external process call,
 * as that one is optimized for handling large, disk-based sorts.
 *
 */
public class CDXIndexCache extends CombiningMultiFileBasedCache<Long>
        implements JobIndexCache {
    private static final String WORK_SUFFIX = ".unsorted";

    /** Creates a new cache for CDX index files.
     *
     */
    public CDXIndexCache() {
        super("cdxindex", new CDXDataCache());
    }

    /** Combine parts of an index into one big index.
     *
     * @param filesFound A map of IDs and the files caching their content.
     */
    protected void combine(Map<Long, File> filesFound) {
        File resultFile = getCacheFile(filesFound.keySet());
        concatenateFiles(filesFound.values(), resultFile);
        sortFile(resultFile);
    }

    /** Concatenate a set of files into a single file.
     *
     * @param files The files to concatenate.
     * @param resultFile The file where the files are concatenated into.
     */
    private static void concatenateFiles(Collection<File> files,
                                         File resultFile)
    {
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
            throw new IOFailure("Couldn't combine indexes for "
                    + files.size() + " jobs into " + resultFile, e);
        }
    }

    /** Sort a (potentionally huge) CDX index file on disk.
     *
     * This method uses the Unix sort(1) command as an external process call,
     * as that one is optimized for handling large, disk-based sorts.  It
     * doesn't, however, depend on the file being an index file.
     *
     * @param file The file containing an unsorted index.
     */
    private void sortFile(File file) {
        File workFile = new File(file.getAbsolutePath() + WORK_SUFFIX);
        workFile.deleteOnExit();
        try {
            ProcessUtils.runProcess(new String[] { "LANG=C"} ,
                    "sort", file.getAbsolutePath(),
                    "-o", workFile.getAbsolutePath());
            workFile.renameTo(file);
        } finally {
            FileUtils.remove(workFile);
        }
    }
}

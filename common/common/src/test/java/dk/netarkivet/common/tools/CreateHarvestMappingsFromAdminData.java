/* File:        $Id$
 * Revision:    $Revision$
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
package dk.netarkivet.common.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepositoryadmin.AdminData;
import dk.netarkivet.archive.arcrepositoryadmin.ReadOnlyAdminData;
import dk.netarkivet.common.utils.FileUtils.FilenameParser;
import dk.netarkivet.common.utils.Settings;

/** This tool produces a jobid-harvestid.txt from a admin.data file.
 *  The file contains <job-id>,<harvest-id> tuples, sorted after job-id.
 *  TODO: Is this necessary?
 *
 */
public class CreateHarvestMappingsFromAdminData {

    private static File outputFile = new File("jobid-harvestid.txt");

    /**
     * Main function, where the work is done as mentioned above.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: CreateHarvestMappingsFromAdminData <admindata-dir>");
            System.exit(1);
        }
        File admindataDir = new File(args[0]);
        try {
            if (!admindataDir.exists()) {
                throw new Exception("Admindatadir does not exist");
            }
            if (!(new File(admindataDir, "admin.data").exists())) {
                throw new Exception("admin.data does not exist in admindatadir");
            }

            } catch (Exception e) {
                System.err.println("Dir '" + admindataDir.getAbsolutePath() + "' contains no admin.data, or dir does not exist");
                System.exit(1);
        }

        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN, admindataDir.getAbsolutePath());

        ReadOnlyAdminData ad = AdminData.getReadOnlyInstance();
        Set<String> arcFiles = ad.getAllFileNames();
        FilenameParser fp;
        SortedMap<String,String> hm = new TreeMap<String,String>();
        Iterator<String> it = arcFiles.iterator();
        while (it.hasNext()) {
            String nextName = it.next();
            try {
                fp = new FilenameParser(new File(nextName));
                if (!hm.containsKey(fp.getJobID())) {

                    //System.out.println(String.format("Added (job,harvestid) = (%s,%s).", fp.getJobID(), fp.getHarvestID()));
                    hm.put(fp.getJobID(),
                            fp.getHarvestID()
                                    );
                }
            } catch (Exception e) {
                //TODO: Should we act differently here?
                //System.out.println("Ignoring filename (probably metadata-arcfile): " + nextName);
            }
        }
        System.out.println("Writing job,harvestid tuples to file: " + outputFile.getAbsolutePath());
        FileWriter fw = new FileWriter(outputFile);
        for(String key: hm.keySet()) {
            fw.write(String.format("%s,%s\n", key, hm.get(key)));
        }
        fw.flush();
        fw.close();
    }
}


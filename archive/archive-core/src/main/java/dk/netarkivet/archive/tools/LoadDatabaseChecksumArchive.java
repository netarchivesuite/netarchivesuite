/*
 * #%L
 * Netarchivesuite - archive
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
package dk.netarkivet.archive.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

import com.sleepycat.je.DatabaseException;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.checksum.DatabaseChecksumArchive;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.KeyValuePair;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.ChecksumJob;

/**
 * Program for uploading data from the filebased FileChecksumArchive to a DatabaseChecksumArchive. The two arguments are
 * /full/path/to/databaseBaseDirectory and /full/path/to/checksum_CS.md5
 */
public class LoadDatabaseChecksumArchive {
    /**
     * Main program for the LoadDatabaseChecksumArchive class
     *
     * @param args two arguments /full/path/to/databaseBaseDirectory and /full/path/to/checksum_CS.md5
     * @throws IOFailure
     * @throws DatabaseException
     */
    public static void main(String[] args) throws IOFailure, DatabaseException {
        if (args.length != 2) {
            System.err.println("Missing args. Required args:  "
                    + " /full/path/to/databaseBaseDirectory /full/path/to/checksum_CS.md5");
            System.exit(1);
        }
        File databaseBasedir = new File(args[0]);
        File checksumCSFile = new File(args[1]);

        if (!databaseBasedir.isDirectory()) {
            String errMsg = "databaseBaseDirectory '" + databaseBasedir.getAbsolutePath()
                    + "' does not exist or is a file instead";
            throw new IOFailure(errMsg);
        }
        System.out.println("Started loading database at: " + new Date());
        Settings.set(ArchiveSettings.CHECKSUM_BASEDIR, databaseBasedir.getAbsolutePath());
        DatabaseChecksumArchive dca = new DatabaseChecksumArchive();

        BufferedReader in = null;
        int loginterval = 10000;
        int currentLine = 0;
        try {
            try {
                in = new BufferedReader(new FileReader(checksumCSFile));
                String line;
                while ((line = in.readLine()) != null) {
                    currentLine++;
                    if (currentLine % loginterval == 0) {
                        System.out.println("Processing line " + currentLine);
                    }
                    KeyValuePair<String, String> entry = ChecksumJob.parseLine(line);
                    dca.put(entry.getKey(), entry.getValue());
                }
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            String msg = "Could not read data from " + checksumCSFile.getAbsolutePath();
            throw new IOFailure(msg, e);
        }

        System.out.println("Finished importing " + currentLine + " lines into the database at " + new Date());
    }
}

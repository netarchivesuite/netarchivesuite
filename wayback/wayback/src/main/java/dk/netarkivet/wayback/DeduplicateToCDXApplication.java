/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package dk.netarkivet.wayback;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.wayback.batch.DeduplicateToCDXAdapter;

/**
 * A simple command line application to generate cdx files from local
 * crawl-log files.
 *
 */

public class DeduplicateToCDXApplication {

    /**
     * Takes an array of file names (relative or full paths) of crawl.log files
     * from which duplicate records are to be extracted. Writes the concatenated
     * cdx files of all duplicate records in these files to standard out. An
     * exception will
     * be thrown if any of the files cannot be read for any reason or if the
     * argument is null
     * @param localCrawlLogs a list of file names
     * @throws FileNotFoundException if one of the files cannot be found
     */
    public void generateCDX(String[] localCrawlLogs)
            throws IOException {
        ArgumentNotValid.checkNotNull(localCrawlLogs, "localCrawlLogs");
        DeduplicateToCDXAdapter adapter = new DeduplicateToCDXAdapter();
        for (String filename: localCrawlLogs) {
            File file = new File(filename);
            FileInputStream inputStream = new FileInputStream(file);
            adapter.adaptStream(inputStream, System.out);
            inputStream.close();
        }
    }

    /**
     * An application to generate unsorted cdx files from duplicate records
     * present in a crawl.log file. The only parameters are a list of file-paths.
     * Output is written to standard out.
     * @param args the file names (relative or absolute paths)
     * @throws FileNotFoundException if one or more of the files does not exist
     */
    public static void main(String[] args) throws IOException {
          if (args.length == 0) {
              System.err.println("No files specified on command line");
              System.err.println("Usage: java dk.netarkivet.wayback.DeduplicateToCDXApplication <files>");
          } else {
              DeduplicateToCDXApplication app =
                      new DeduplicateToCDXApplication();
              app.generateCDX(args);
          }

    }

}

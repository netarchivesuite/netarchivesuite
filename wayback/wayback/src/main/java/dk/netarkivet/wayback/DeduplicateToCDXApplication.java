/* File:        $Id: License.txt,v $
 * Revision:    $Revision: 1.4 $
 * Author:      $Author: csr $
 * Date:        $Date: 2005/04/11 16:29:16 $
 *
 * Copyright Det Kongelige Bibliotek og Statsbiblioteket, Danmark
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

import dk.netarkivet.wayback.batch.DeduplicateToCDXAdapter;

/**
 * A simple command line application to generate cdx files from local
 * crawl-log files.
 *
 * @author csr
 * @since Sep 1, 2009
 */

public class DeduplicateToCDXApplication {

    /**
     * Takes an array of file names (relative or full paths) of crawl.log files
     * from which duplicate records are to be extracted. Writes the concatenated
     * cdx files of all duplicate records in these files to standard out. An
     * exception will
     * be thrown if any of the files cannot be read for any reason
     * @param local_crawl_logs a list of file names
     * @throws FileNotFoundException if one of the files cannot be found
     */
    public void generateCDX(String[] local_crawl_logs)
            throws FileNotFoundException {
        DeduplicateToCDXAdapter adapter = new DeduplicateToCDXAdapter();
        for (String filename: local_crawl_logs) {
            File file = new File(filename);
            adapter.adaptStream((new FileInputStream(file)), System.out);
        }
    }

    /**
     * An application to generate unsorted cdx files from duplicate records
     * present in a crawl.low file. The only parameters are a list of file-paths.
     * Output is written to standard out.
     * @param args the file names (relative or absolute paths)
     * @throws FileNotFoundException if one or more of the files does not exist
     */
    public static void main(String[] args) throws FileNotFoundException {
          if (args.length == 0) {
              System.err.println("No files specified on command line");
              System.err.println("Usage: java dk.netarkivet.wyaback.DeduplicateToCDXApplication <files>");
          } else {
              DeduplicateToCDXApplication app = new DeduplicateToCDXApplication();
              app.generateCDX(args);
          }

    }

}

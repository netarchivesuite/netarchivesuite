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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * A class with a method for creating large files.
 * Bruce Eckel's solution
 * (http://www.java2s.com/ExampleCode/File-Input-Output/Creatingaverylargefileusingmapping.htm)
 * is slightly slower!
 */
public class WriteBytesToFile {
    /**
     * Writes a large number of bytes to a given file.
     * @param args
     * args[0] is the number of bytes to write
     * args[1] the name of the output file
     * @throws IOException If unable to write to output file
     */
    public static void main(String[] args) throws IOException {

        // A reasonably optimal value for the chunksize
        int byteChunkSize = 10000000;

        long nbytes = 0;
        if (args.length != 2) {
            System.out.println("Usage: java WriteBytesToFile nbytes filename");
            System.exit(1);
        }
        try {
            nbytes = Long.parseLong(args[0]);
        } catch (Exception e) {
            System.out.println("First argument must be a number");
        }
        File outputFile = new File(args[1]);
        byte[] byteArr = new byte[byteChunkSize];
        FileOutputStream os = new FileOutputStream(outputFile);
        FileChannel chan = os.getChannel();
        for (int i = 0; i < nbytes / byteChunkSize; i++) {
            chan.write(ByteBuffer.wrap(byteArr));
        }
        os.close();
        chan.close();
    }
}

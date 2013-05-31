/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

public class ShowIndexStats {

    /**
     * @param args The full path of the index
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        String path = null;
        if (args.length != 1) {
            System.err.println("Missing path to the index");
            System.exit(1);
        } else {
            path = args[0];
        }
        FSDirectory luceneDir = FSDirectory.open(new File(path));
        IndexReader r = IndexReader.open(luceneDir);
        System.out.println("Number of docs in index: " +  r.numDocs());
        //System.out.println("Version: " + r.getVersion());
        luceneDir.close();
        r.close();
    }

}
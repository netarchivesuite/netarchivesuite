/* File:   $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */
package dk.netarkivet.wayback.aggregator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Reference index able to load file, sort them and compare the result to files
 * created by the aggregator. In effect, this class performs the same
 * functionality as the real aggregator, but is written in pure Java and relies
 * on in-memory index handling. This means that the <code>TestIndex</code> class
 * can only handles small index sets.
 */
public class TestIndex {
    private TreeSet<String> indexSet = new TreeSet<String>();

    /**
     * Adds the indexes contained in the indicated file to this objects index
     *
     * @param indexFile Name of the files to add the indexes for
     */
    public void addIndexesFromFile(File indexFile) {
        indexSet.addAll(indexFileToIndexSet(indexFile));
    }

    /**
     * Adds the indexes contained in the indicated files to this objects index.
     * Assumes the index file are located in the AggregatorTestCase#inputDirName
     * directory.
     *
     * @param indexFiles Names of the files to add the indexes for
     */
    public void addIndexesFromFiles(File[] indexFiles) {
        for(int i = 0;i < indexFiles.length; i++) {
            indexSet.addAll(indexFileToIndexSet(indexFiles[i]));
        }
    }

    /**
     * Compares the indicated index to the index maintained in this
     * <code>testIndex</code>. The operation returns null if all indexes are the
     * same, including the sorting of the indexes. If thew indexes are different
     * a string describing the difference is returned
     *
     * @param indexFile The file which should be compared to the testindex
     * @return Null if the indexes are equal, else a difference description.
     */
    public String compareToIndex(File indexFile) {
        String result = null;

        TreeSet<String> fileIndexSet = indexFileToIndexSet(indexFile);
        if (fileIndexSet.size() != indexSet.size()) {
            return "The number of indexes ("+fileIndexSet.size()+") are different "
                   + "from the number("+indexSet.size()+" in the reference index";
        }
        Iterator<String> fileIndexIterator = fileIndexSet.iterator();

        for (String index : indexSet) {
            String fileIndex = fileIndexIterator.next();
            if (!index.equals(fileIndex)) {
                result = "Found index difference \n "+
                    "expected "+index+"\n"+
                    "but found "+fileIndex;
                return result;
            }
        }
        return result;
    }

    /**
     * Loads all the indexes in a files into a sorted TreeSet
     *
     * @param indexFile The file to load
     *
     * @return The sorted set containing the file indexes
     */
    private TreeSet<String> indexFileToIndexSet(File indexFile) {
        TreeSet<String> indexSet = new TreeSet<String>();
        try {
            BufferedReader input = new BufferedReader(new FileReader(indexFile));
            try {
                String line = null;
                while ((line = input.readLine()) != null) {
                    indexSet.add(line);
                    boolean DEBUG = false;
                    if (DEBUG) System.out.println("\nAdding line to set: "+line);
                }
            }
            finally {
                input.close();
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }

        return indexSet;
    }
}

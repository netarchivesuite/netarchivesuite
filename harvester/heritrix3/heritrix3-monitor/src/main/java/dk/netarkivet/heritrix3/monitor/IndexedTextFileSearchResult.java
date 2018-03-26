/*
 * #%L
 * Netarchivesuite - heritrix 3 monitor
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

package dk.netarkivet.heritrix3.monitor;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

/**
 * Class used to perform regex search and cached the result on disk.
 * Supports updating the search if the original file increased after a previous search.
 * Also used the <code>StringIndexFile</code> class to show a paged view of the search results.
 *
 * @author nicl
 */
public class IndexedTextFileSearchResult implements Pageable, Closeable {

	/** Original text file to search in. */
	protected File textFile;

	/** Reusable regex pattern object. */
    protected Pattern p;

    /** Reusable regex matches object. */
    protected Matcher m;

    /** Search result text file. */
    protected File srTextFile;

    /** Search result text random access file. */
    protected RandomAccessFile srTextRaf;

    /** Search result index file. */
    protected File srIdxFile;

    /** Search result index random access file. */
    protected RandomAccessFile srIdxRaf;

    /** Last position in the original text file which has been searched. */
    protected long lastIndexedTextPosition;

    /** Indexed text lines. */
    public long indexedTextLines;

    /**
     * Create a search result object and prepare the stored cache files.
     * @param textFile original text file
     * @param srBaseFile base file(name) to use when creating the search result text and index files
     * @param q regex string
     * @param searchResultNr unique sequential search result number used to store cache files
     * @throws IOException if an I/O exception occurs whule creating cache files
     */
    public IndexedTextFileSearchResult(File textFile, File dir, String srBaseFilename, String q, int searchResultNr) throws IOException {
    	this.textFile = textFile;
        p = Pattern.compile(q, Pattern.CASE_INSENSITIVE);
        // Create a reusable pattern matcher object for use with the reset method.
        m = p.matcher("42");
        srTextFile = new File(dir, srBaseFilename + "-" + searchResultNr + ".log");
        srTextRaf = new RandomAccessFile(srTextFile, "rw");
        srTextRaf.setLength(0);
        srIdxFile = new File(dir, srBaseFilename + "-" + searchResultNr + ".idx");
        srIdxRaf = new RandomAccessFile(srIdxFile, "rw");
        srIdxRaf.setLength(0);
        srIdxRaf.writeLong(0);
        lastIndexedTextPosition = 0;
        indexedTextLines = 0;
    }

    @Override
    public synchronized void close() {
        indexedTextLines = 0;
        IOUtils.closeQuietly(srTextRaf);
        IOUtils.closeQuietly(srIdxRaf);
    }

    @Override
    public long getTextFilesize() {
        return srTextFile.length();
    }

    @Override
    public long getIndexFilesize() {
        return srIdxFile.length();
    }

    @Override
    public long getLastIndexedTextPosition() {
        return lastIndexedTextPosition;
    }

    @Override
    public long getIndexedTextLines() {
        return indexedTextLines;
    }

    @Override
    public synchronized byte[] readPage(long page, long itemsPerPage, boolean descending) throws IOException {
        return IndexedTextFile.readPage(srIdxRaf, srTextRaf, page, itemsPerPage, descending);
    }

    public void addFilesToOldFilesList(List<File> oldFilesList) {
        oldFilesList.add(srTextFile);
        oldFilesList.add(srIdxFile);
    }

    /**
     * Perform an update search on the part of the original text file that has not yet been read.
     * @throws IOException if an I/O exception occurs while updaing the cached search result files
     */
    public synchronized void update() throws IOException {
    	// Check to see if the search result is up to date.
    	if (lastIndexedTextPosition >= textFile.length()) {
    		return;
    	}
        RandomAccessFile textRaf = new RandomAccessFile(textFile, "r");
        textRaf.seek(lastIndexedTextPosition);
        srTextRaf.seek(srTextRaf.length());
        srIdxRaf.seek(srIdxRaf.length());
        FileChannel textChannel = textRaf.getChannel();
        byte[] bytes = new byte[1024*1024];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        String tmpStr;
        //long index = lastIndex;
        long index = srTextRaf.length();
        int pos;
        int to;
        int mark;
        int limit;
        boolean b;
        while (textChannel.read(byteBuffer) != -1) {
            byteBuffer.flip();
            pos = byteBuffer.position();
            mark = pos;
            limit = byteBuffer.limit();
            b = true;
            while (b) {
                if (pos < limit) {
                    if (bytes[pos++] == '\n') {
                        to = pos - 1;
                        if (bytes[to - 1] == '\r') {
                            --to;
                        }
                        tmpStr = new String(bytes, mark, to - mark, "UTF-8");
                        m.reset(tmpStr);
                        if (m.matches()) {
                        	srTextRaf.write(bytes, mark, pos - mark);
                            index += pos - mark;
                            srIdxRaf.writeLong(index);
                        	++indexedTextLines;
                        }
                        lastIndexedTextPosition += pos - mark;
                        // next
                        mark = pos;
                        //index += pos - mark;
                        //lastIndex = index;
                    }
                } else {
                    b = false;
                }
            }
            byteBuffer.position(mark);
            byteBuffer.compact();
        }
        textRaf.close();
    }

}

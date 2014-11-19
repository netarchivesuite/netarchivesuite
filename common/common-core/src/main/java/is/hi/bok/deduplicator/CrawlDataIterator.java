/* CrawlDataIterator
 * 
 * Created on 10.04.2006
 *
 * Copyright (C) 2006 National and University Library of Iceland
 * 
 * This file is part of the DeDuplicator (Heritrix add-on module).
 * 
 * DeDuplicator is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 * 
 * DeDuplicator is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with DeDuplicator; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package is.hi.bok.deduplicator;

import java.io.IOException;

/**
 * An abstract base class for implementations of iterators that iterate over different sets of crawl data (i.e.
 * crawl.log, ARC, WARC etc.)
 *
 * @author Kristinn Sigur&eth;sson
 */
public abstract class CrawlDataIterator {

    String source;

    /**
     * Constructor.
     *
     * @param source The location of the crawl data. The meaning of this value may vary based on the implementation of
     * concrete subclasses. Typically it will refer to a directory or a file.
     */
    public CrawlDataIterator(String source) {
        this.source = source;
    }

    /**
     * Are there more elements?
     *
     * @return true if there are more elements, false otherwise
     * @throws IOException If an error occurs accessing the crawl data.
     */
    public abstract boolean hasNext() throws IOException;

    /**
     * Get the next {@link CrawlDataItem}.
     *
     * @return the next CrawlDataItem. If there are no further elements then null will be returned.
     * @throws IOException If an error occurs accessing the crawl data.
     */
    public abstract CrawlDataItem next() throws IOException;

    /**
     * Close any resources held open to read the crawl data.
     *
     * @throws IOException If an error occurs closing access to crawl data.
     */
    public abstract void close() throws IOException;

    /**
     * A short, human readable, string about what source this iterator uses. I.e.
     * "Iterator for Heritrix style crawl.log" etc.
     *
     * @return A short, human readable, string about what source this iterator uses.
     */
    public abstract String getSourceType();
}

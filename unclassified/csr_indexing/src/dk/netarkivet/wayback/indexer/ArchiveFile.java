/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
 *   USA
 */
package dk.netarkivet.wayback.indexer;

import javax.persistence.Id;
import javax.persistence.Entity;
import java.util.Date;

import dk.netarkivet.common.exceptions.NotImplementedException;


/**
 * This class represents a file in the arcrepository which may be indexed by
 * the indexer.
 */
@Entity
public class ArchiveFile {

    /**
     * The name of the file in the arcrepository.
     */
    private String filename;

    /**
     * Boolean flag indicating whether the file has been indexed.
     */
    private boolean isIndexed;

    /**
     * The name of the unsorted cdx index file created from the archive file.
     */
    private String originalIndexFileName;

    /**
     * The name of the sorted index file to which entries from this file have
     * been added.
     */
    private String finalIndexFileName;

    /**
     * The date on which this file was indexed.
     */
    private Date indexedDate;

    public ArchiveFile() {
        isIndexed = false;
        indexedDate = null;
    }

    public String getOriginalIndexFileName() {
        return originalIndexFileName;
    }

    public void setOriginalIndexFileName(String originalIndexFileName) {
        this.originalIndexFileName = originalIndexFileName;
    }

    public String getFinalIndexFileName() {
        return finalIndexFileName;
    }

    public void setFinalIndexFileName(String finalIndexFileName) {
        this.finalIndexFileName = finalIndexFileName;
    }

    public Date getIndexedDate() {
        return indexedDate;
    }

    public void setIndexedDate(Date indexedDate) {
        this.indexedDate = indexedDate;
    }

    /**
     * The filename is used as a natural key because it is a fundamental property
     * of the arcrepository that filenames are unique.
     * @return
     */
    @Id
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean isIndexed() {
        return isIndexed;
    }

    public void setIndexed(boolean indexed) {
        isIndexed = indexed;
    }

    /**
     * Run a batch job to index this file, storing the result locally.
     * If this method runs successfully, the isIndexed flag will be set to
     * true and the originalIndexFileName field will be set to the (arbitrary)
     * name of the file containing the results. The values are persisted to the
     * datastore.
     */
    public void index() {
          throw new NotImplementedException("Not yet implemented");
    }
}

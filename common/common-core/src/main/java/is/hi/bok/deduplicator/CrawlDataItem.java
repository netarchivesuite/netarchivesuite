/* CrawlDataItem
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

/**
 * A base class for individual items of crawl data that should be added to the index.
 *
 * @author Kristinn Sigur&eth;sson
 */
public class CrawlDataItem {

    /**
     * The proper formating of {@link #setURL(String)} and {@link #getURL()}
     */
    public static final String dateFormat = "yyyyMMddHHmmssSSS";

    protected String URL;
    protected String contentDigest;
    protected String timestamp;
    protected String etag;
    protected String mimetype;
    protected String origin;
    protected boolean duplicate;

    /**
     * Constructor. Creates a new CrawlDataItem with all its data initialized to null.
     */
    public CrawlDataItem() {
        URL = null;
        contentDigest = null;
        timestamp = null;
        etag = null;
        mimetype = null;
        origin = null;
        duplicate = false;
    }

    /**
     * Constructor. Creates a new CrawlDataItem with all its data initialized via the constructor.
     *
     * @param URL The URL for this CrawlDataItem
     * @param contentDigest A content digest of the document found at the URL
     * @param timestamp Date of when the content digest was valid for that URL. Format: yyyyMMddHHmmssSSS
     * @param etag Etag for the URL
     * @param mimetype MIME type of the document found at the URL
     * @param origin The origin of the CrawlDataItem (the exact meaning of the origin is outside the scope of this class
     * and it may be any String value)
     * @param duplicate True if this CrawlDataItem was marked as duplicate
     */
    public CrawlDataItem(String URL, String contentDigest, String timestamp, String etag, String mimetype,
            String origin, boolean duplicate) {
        this.URL = URL;
        this.contentDigest = contentDigest;
        this.timestamp = timestamp;
        this.etag = etag;
        this.mimetype = mimetype;
        this.origin = origin;
        this.duplicate = duplicate;
    }

    /**
     * Returns the URL
     *
     * @return the URL
     */
    public String getURL() {
        return URL;
    }

    /**
     * Set the URL
     *
     * @param URL the new URL
     */
    public void setURL(String URL) {
        this.URL = URL;
    }

    /**
     * Returns the documents content digest
     *
     * @return the documents content digest
     */
    public String getContentDigest() {
        return contentDigest;
    }

    /**
     * Set the content digest
     *
     * @param contentDigest The new value of the content digest
     */
    public void setContentDigest(String contentDigest) {
        this.contentDigest = contentDigest;
    }

    /**
     * Returns a timestamp for when the URL was fetched in the format: yyyyMMddHHmmssSSS
     *
     * @return the time of the URLs fetching
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Set a new timestamp.
     *
     * @param timestamp The new timestamp. It should be in the format: yyyyMMddHHmmssSSS
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the etag that was associated with the document.
     * <p>
     * If etag is unavailable null will be returned.
     *
     * @return the etag.
     */
    public String getEtag() {
        return etag;
    }

    /**
     * Set a new Etag
     *
     * @param etag The new etag
     */
    public void setEtag(String etag) {
        this.etag = etag;
    }

    /**
     * Returns the mimetype that was associated with the document.
     *
     * @return the mimetype.
     */
    public String getMimeType() {
        return mimetype;
    }

    /**
     * Set new MIME type.
     *
     * @param mimetype The new MIME type
     */
    public void setMimeType(String mimetype) {
        this.mimetype = mimetype;
    }

    /**
     * Returns the "origin" that was associated with the document.
     *
     * @return the origin (may be null if none was provided for the document)
     */
    public String getOrigin() {
        return origin;
    }

    /**
     * Set new origin
     *
     * @param origin A new origin.
     */
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    /**
     * Returns whether the CrawlDataItem was marked as duplicate.
     *
     * @return true if duplicate, false otherwise
     */
    public boolean isDuplicate() {
        return duplicate;
    }

    /**
     * Set whether duplicate or not.
     *
     * @param duplicate true if duplicate, false otherwise
     */
    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

}

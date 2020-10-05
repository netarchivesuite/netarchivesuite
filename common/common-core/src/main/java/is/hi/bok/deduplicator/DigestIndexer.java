/* DigestIndexer
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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.archive.util.ArchiveUtils;

import dk.netarkivet.common.Constants;

/**
 * A class for building a de-duplication index.
 * <p>
 * The indexing can be done via the command line options (Run with --help parameter to print usage information) or
 * natively embedded in other applications.
 * <p>
 * This class also defines string constants for the lucene field names.
 *
 * @author Kristinn Sigur&eth;sson
 * @author Søren Vejrup Carlsen
 */
public class DigestIndexer {

    // Lucene index field names
    /** The URL. * */
    public static final String FIELD_URL = "url";
    /** The content digest as String. * */
    public static final String FIELD_DIGEST = "digest";
    /**
     * The URLs timestamp (time of fetch). The exact nature of this time may vary slightly depending on the source (i.e.
     * crawl.log and ARCs contain slightly different times but both indicate roughly when the document was obtained. The
     * time is encoded as a String with the Java date format yyyyMMddHHmmssSSS
     */
    public static final String FIELD_TIMESTAMP = "date";
    /** The document's etag. * */
    public static final String FIELD_ETAG = "etag";
    /** A stripped (normalized) version of the URL. * */
    public static final String FIELD_URL_NORMALIZED = "url-normalized";
    /**
     * A field containing meta-data on where the original version of a document is stored.
     */
    public static final String FIELD_ORIGIN = "origin";

    // Indexing modes (by url, by hash or both)
    /**
     * Index URL enabling lookups by URL. If normalized URLs are included in the index they will also be indexed and
     * searchable. *
     */
    public static final String MODE_URL = "URL";
    /** Index HASH enabling lookups by hash (content digest). * */
    public static final String MODE_HASH = "HASH";
    /** Both URL and hash are indexed. * */
    public static final String MODE_BOTH = "BOTH";

    /** Lucene Storage used by the indexwriter. */
    private Directory luceneDirectory;

    /** The index being manipulated. * */
    private IndexWriter index;

    /**
     * @return the IndexWriter
     */
    public IndexWriter getIndex() {
        return index;
    }

    // The options with default settings
    /** Should etags be included in the index. */
    private boolean etag = false;
    /**
     * Should a normalized version of the URL be added to the index.
     */
    private boolean equivalent = false;
    /** Should a timestamp be included in the index. */
    private boolean timestamp = false;
    /** Should we index the url. */
    private boolean indexURL = true;
    /** Should we index the digest. */
    private boolean indexDigest = true;

    /**
     * Each instance of this class wraps one Lucene index for writing deduplication information to it.
     *
     * @param indexLocation The location of the index (path).
     * @param indexingMode Index {@link #MODE_URL}, {@link #MODE_HASH} or {@link #MODE_BOTH}.
     * @param includeNormalizedURL Should a normalized version of the URL be added to the index. See
     * {@link #stripURL(String)}.
     * @param includeTimestamp Should a timestamp be included in the index.
     * @param includeEtag Should an Etag be included in the index.
     * @param addToExistingIndex Are we opening up an existing index. Setting this to false will cause any index at
     * <code>indexLocation</code> to be overwritten.
     * @throws IOException If an error occurs opening the index.
     */
    public DigestIndexer(String indexLocation, String indexingMode, boolean includeNormalizedURL,
            boolean includeTimestamp, boolean includeEtag, boolean addToExistingIndex) throws IOException {

        this.etag = includeEtag;
        this.equivalent = includeNormalizedURL;
        this.timestamp = includeTimestamp;

        if (indexingMode.equals(MODE_URL)) {
            indexDigest = false;
        } else if (indexingMode.equals(MODE_HASH)) {
            indexURL = false;
        }

        // Set up the index writer
        IndexWriterConfig config = new IndexWriterConfig(Constants.LUCENE_VERSION, new WhitespaceAnalyzer(
                Constants.LUCENE_VERSION));
        // TODO Possibly change the default MergePolicy, see NAS-2119
        if (!addToExistingIndex) {
            config.setOpenMode(OpenMode.CREATE);
        } else {
            config.setOpenMode(OpenMode.CREATE_OR_APPEND);
        }
        luceneDirectory = FSDirectory.open(new File(indexLocation));
        index = new IndexWriter(luceneDirectory, config);
    }

    /**
     * Writes the contents of a {@link CrawlDataIterator} to this index.
     * <p>
     * This method may be invoked multiple times with different CrawlDataIterators until {@link #close} has been called.
     *
     * @param dataIt The CrawlDataIterator that provides the data to index.
     * @param mimefilter A regular expression that is used as a filter on the mimetypes to include in the index.
     * @param blacklist If true then the <code>mimefilter</code> is used as a blacklist for mimetypes. If false then the
     * <code>mimefilter</code> is treated as a whitelist.
     * @param defaultOrigin If an item is missing an origin, this default value will be assigned to it. Can be null if
     * no default origin value should be assigned.
     * @param verbose If true then progress information will be sent to System.out.
     * @return The number of items added to the index.
     * @throws IOException If an error occurs writing the index.
     */
    public long writeToIndex(CrawlDataIterator dataIt, String mimefilter, boolean blacklist, String defaultOrigin,
            boolean verbose) throws IOException {
        return writeToIndex(dataIt, mimefilter, blacklist, defaultOrigin, verbose, false);
    }

    /**
     * Writes the contents of a {@link CrawlDataIterator} to this index.
     * <p>
     * This method may be invoked multiple times with different CrawlDataIterators until {@link #close} has been called.
     *
     * @param dataIt The CrawlDataIterator that provides the data to index.
     * @param mimefilter A regular expression that is used as a filter on the mimetypes to include in the index.
     * @param blacklist If true then the <code>mimefilter</code> is used as a blacklist for mimetypes. If false then the
     * <code>mimefilter</code> is treated as a whitelist.
     * @param defaultOrigin If an item is missing an origin, this default value will be assigned to it. Can be null if
     * no default origin value should be assigned.
     * @param verbose If true then progress information will be sent to System.out.
     * @param skipDuplicates Do not add URLs that are marked as duplicates to the index
     * @return The number of items added to the index.
     * @throws IOException If an error occurs writing the index.
     */
    public long writeToIndex(CrawlDataIterator dataIt, String mimefilter, boolean blacklist, String defaultOrigin,
            boolean verbose, boolean skipDuplicates) throws IOException {
        int count = 0;
        int skipped = 0;
        while (dataIt.hasNext()) {
            CrawlDataItem item = dataIt.next();
            if (!(skipDuplicates && item.duplicate) && item.mimetype.matches(mimefilter) != blacklist) {
                // Ok, we wish to index this URL/Digest
                count++;
                if (verbose && count % 10000 == 0) {
                    System.out.println("Indexed " + count + " - Last URL " + "from " + item.getTimestamp());
                }

                Document doc = createDocument(item, defaultOrigin);
                index.addDocument(doc);
                // needed with new IndexWriter (see line 144)
                // index.commit();
            } else {
                skipped++;
            }
        }
        index.commit();
        if (verbose) {
            System.out.println("Indexed " + count + " items (skipped " + skipped + ")");
        }
        return count;
    }

    /**
     * Create Lucene Document for given CrawlDataItem.
     * @param item A CrawlDataItem
     * @param defaultOrigin
     * @return Lucene Document for the given CrawlDataItem
     */
    private Document createDocument(CrawlDataItem item, String defaultOrigin) {
        Document doc = new Document();

        FieldType storedNotIndexed = new FieldType(StringField.TYPE_STORED);
        storedNotIndexed.setIndexed(false);

        FieldType storedNotAnalyzed = new FieldType(StringField.TYPE_STORED);
        storedNotAnalyzed.setOmitNorms(false);

        // Add URL to index.
        if (indexURL) {
            doc.add(new Field(FIELD_URL, item.getURL(), storedNotAnalyzed));
            if (equivalent) {
                doc.add(new Field(FIELD_URL_NORMALIZED, stripURL(item.getURL()), storedNotAnalyzed));
            }
        } else {
            doc.add(new Field(FIELD_URL, item.getURL(), storedNotIndexed));
            if (equivalent) {
                doc.add(new Field(FIELD_URL_NORMALIZED, stripURL(item.getURL()), storedNotIndexed));
            }
        }

        // Add digest to index
        if (indexDigest) {
            doc.add(new Field(FIELD_DIGEST, item.getContentDigest(), storedNotAnalyzed));
        } else {
            doc.add(new Field(FIELD_DIGEST, item.getContentDigest(), storedNotIndexed));
        }
        // Add timestamp to index
        if (timestamp) {
            doc.add(new Field(FIELD_TIMESTAMP, item.getTimestamp(), storedNotIndexed));
        }
        // Add etag to index
        if (etag && item.getEtag() != null) {
            doc.add(new Field(FIELD_ETAG, item.getEtag(), storedNotIndexed));
        }
        if (defaultOrigin != null) {
            String tmp = item.getOrigin();
            if (tmp == null) {
                tmp = defaultOrigin;
            }
            doc.add(new Field(FIELD_ORIGIN, tmp, storedNotIndexed));
        }
        return doc;
    }

    /**
     * Close the index.
     *
     * @throws IOException If an error occurs while closing the index.
     */
    public void close() throws IOException {
        index.close(true);
        luceneDirectory.close();
    }

    /**
     * An aggressive URL normalizer. This methods removes any www[0-9]. segments from an URL, along with any trailing
     * slashes and all parameters.
     * <p>
     * Example: <code>http://www.bok.hi.is/?lang=ice</code> would become <code>http://bok.hi.is</code>
     *
     * @param url The url to strip
     * @return A normalized URL.
     */
    public static String stripURL(String url) {
        url = url.replaceAll("www[0-9]*\\.", "");
        url = url.replaceAll("\\?.*$", "");
        url = url.replaceAll("/$", "");
        return url;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void main(String[] args) throws Exception {
        CommandLineParser clp = new CommandLineParser(args, new PrintWriter(System.out));
        long start = System.currentTimeMillis();

        // Set default values for all settings.
        boolean etag = false;
        boolean equivalent = false;
        boolean timestamp = false;
        String indexMode = MODE_BOTH;
        boolean addToIndex = false;
        String mimefilter = "^text/.*";
        boolean blacklist = true;
        String iteratorClassName = CrawlLogIterator.class.getName();
        String origin = null;
        boolean skipDuplicates = false;

        // Process the options
        Option[] opts = clp.getCommandLineOptions();
        for (int i = 0; i < opts.length; i++) {
            Option opt = opts[i];
            switch (opt.getId()) {
            case 'w':
                blacklist = false;
                break;
            case 'a':
                addToIndex = true;
                break;
            case 'e':
                etag = true;
                break;
            case 'h':
                clp.usage(0);
                break;
            case 'i':
                iteratorClassName = opt.getValue();
                break;
            case 'm':
                mimefilter = opt.getValue();
                break;
            case 'o':
                indexMode = opt.getValue();
                break;
            case 's':
                equivalent = true;
                break;
            case 't':
                timestamp = true;
                break;
            case 'r':
                origin = opt.getValue();
                break;
            case 'd':
                skipDuplicates = true;
                break;
            default:
                System.err.println("Unhandled option id: " + opt.getId());
            }
        }

        List cargs = clp.getCommandLineArguments();

        if (cargs.size() != 2) {
            // Should be exactly two arguments. Source and target!
            clp.usage(0);
        }

        // Get the CrawlDataIterator
        // Get the iterator classname or load default.
        Class cl = Class.forName(iteratorClassName);
        Constructor co = cl.getConstructor(new Class[] {String.class});
        CrawlDataIterator iterator = (CrawlDataIterator) co.newInstance(new Object[] {(String) cargs.get(0)});

        // Print initial stuff
        System.out.println("Indexing: " + cargs.get(0));
        System.out.println(" - Mode: " + indexMode);
        System.out.println(" - Mime filter: " + mimefilter + " (" + (blacklist ? "blacklist" : "whitelist") + ")");
        System.out.println(" - Includes" + (equivalent ? " <equivalent URL>" : "") + (timestamp ? " <timestamp>" : "")
                + (etag ? " <etag>" : ""));
        System.out.println(" - Skip duplicates: " + (skipDuplicates ? "yes" : "no"));
        System.out.println(" - Iterator: " + iteratorClassName);
        System.out.println("   - " + iterator.getSourceType());
        System.out.println("Target: " + cargs.get(1));
        if (addToIndex) {
            System.out.println(" - Add to existing index (if any)");
        } else {
            System.out.println(" - New index (erases any existing index at " + "that location)");
        }

        DigestIndexer di = new DigestIndexer((String) cargs.get(1), indexMode, equivalent, timestamp, etag, addToIndex);

        // Create the index
        di.writeToIndex(iterator, mimefilter, blacklist, origin, true, skipDuplicates);

        // Clean-up
        di.close();

        System.out.println("Total run time: "
                + ArchiveUtils.formatMillisecondsToConventional(System.currentTimeMillis() - start));
    }
}

/* File:        $Id$
 * Date:        $Date$
 * Revision:    $Revision$
 * Author:      $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.common.utils.cdx;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.arc.ARCKey;

/** This class handles reading CDX files and finding entries in them.
 *  Furthermore it implements the possibillity to do filtering of searchresults
 *
 */
public class CDXReader {
    /** The CDX files that we want to iterate over. */
    private List<File> files = new ArrayList<File>();

    /** Any filters we want to apply. */
    private Map<String, CDXRecordFilter> cdxrecordfilters = new HashMap<String, CDXRecordFilter>();

    /** The regular expression that defines seperation between fields. */
    static final String SEPARATOR_REGEX = "\\s+";
    private Log log = LogFactory.getLog(CDXReader.class.getName());

    /** Create a new CDXReader that reads the given file.
     *
     * @param cdxFile A CDX file to read.
     * @throws IOFailure If the file cannot be found.
     */
    public CDXReader(File cdxFile) {
        addCDXFile(cdxFile);
    }

    /** Create a new CDXReader with no file. */
    public CDXReader() {
    }

    /** Add another CDX file to those being searched.
     *
     * @param cdxFile A CDX file to search.
     * @throws IOFailure If the file cannot be found or read
     */
    public void addCDXFile(File cdxFile) {
        ArgumentNotValid.checkNotNull(cdxFile, "cdxFile");
        if (!cdxFile.exists() || !cdxFile.canRead()) {
            final String message = "Can't find CDX file '"
                                   + cdxFile.getAbsolutePath() + "'";
            log.debug(message);
            throw new IOFailure(message);
        }
        files.add(cdxFile);
    }

    /** Forget about all CDX files.
     */
    public void clearCDXFiles() {
        files.clear();
    }

    /** Add another CDXRecordFilter to the list of filters to use when
     * searching.
     *
     * @param cdxrecfilter A CDXRecordFilter to use when searching.
     * @throws ArgumentNotValid If the filter is invalid or another filter
     * exists with the same name.
     */
    public void addCDXRecordFilter(CDXRecordFilter cdxrecfilter)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(cdxrecfilter,  "cdxrecfilter");
        ArgumentNotValid.checkNotNullOrEmpty(cdxrecfilter.getFilterName(),
                "cdxrecfilter.getFilterName()");

        if (cdxrecordfilters.containsKey(cdxrecfilter.getFilterName())) {
            throw new ArgumentNotValid("The Filtername '"
                    + cdxrecfilter.getFilterName() + "' is already in use !");
        }
        cdxrecordfilters.put(cdxrecfilter.getFilterName(), cdxrecfilter);
    }

    /** Remove all CDXRecordFilters.
     *
     */
    public void removeAllCDXRecordFilters() {
        cdxrecordfilters = new HashMap<String, CDXRecordFilter>();
    }

    /** Get a table of all filters.
     *  @return a Hashtable with all the filters.
     */
    public Map<String, CDXRecordFilter> getFilters() {
        return Collections.unmodifiableMap(cdxrecordfilters);
    }

    /** Get a specific filter by the name of the filter -
     *  if not found return null.
     *  @param filtername The given filtername.
     *  @return the CDXRecordFilter
     */
    public CDXRecordFilter getCDXRecordFilter(String filtername){
        return cdxrecordfilters.get(filtername);
    }

    /** Remove a specific filter by the name of the filter.
     *  @param filtername The given filtername.
     *  @throws UnknownID if there is no filter of that name.
     */
    public void removeCDXRecordFilter(String filtername) {
        if (!cdxrecordfilters.containsKey(filtername)) {
            throw new UnknownID("No filter found named " + filtername);
        }
        cdxrecordfilters.remove(filtername);
    }

    /** Look up an entry in CDX files.  Notice that only full match search is
     * allowed, not prefix search.
     *
     * @param uri A URI to find in the CDX files.
     * @return A key indicating the place where the entry can be found, or
     * null if no such entry was found;
     */
    public ARCKey getKey(String uri) {
        for (File f : files) {
            String firstBrokenLine = null;
            long numBrokenLines = 0;
            try {
                CDXLINES: for (String s : BinSearch.getLinesInFile(f, uri)) {
                    String[] field_parts = s.split(SEPARATOR_REGEX);
                    CDXRecord cdxrec;
                    try {
                        cdxrec = new CDXRecord(field_parts);
                    } catch (RuntimeException e) {
                        // Skip lines with wrong format
                        numBrokenLines++;
                        if (firstBrokenLine == null) {
                            firstBrokenLine = s;
                        }
                        continue CDXLINES;
                    }
                    String cdxuri = cdxrec.getURL();
                    if (CDXRecord.URLsEqual(uri, cdxuri)) {
                        for (CDXRecordFilter cdxrecf :
                                cdxrecordfilters.values()) {
                            if (!cdxrecf.process(cdxrec)) {
                                continue CDXLINES;
                            }
                        }
                        return new ARCKey(cdxrec.getArcfile(),
                                cdxrec.getOffset());
                    }
                }
            } finally {
                if (numBrokenLines > 0) {
                    log.warn("CDX file '" + f + "' contains "
                            + numBrokenLines
                            + " invalid CDX lines, first one is\n"
                            + firstBrokenLine);
                }
            }
        }
        return null;
    }
}

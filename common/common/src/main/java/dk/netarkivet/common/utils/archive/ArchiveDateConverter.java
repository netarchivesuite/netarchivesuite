/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
package dk.netarkivet.common.utils.archive;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class ArchiveDateConverter {

    /** ARC date format string as speficied in the ARC documentation. */
	public static final String ARC_DATE_FORMAT = "yyyyMMddHHmmss";

	/** WARC date format string as specified by the WARC ISO standard. */
	public static final String WARC_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	/** ARC <code>DateFormat</code> as specified in the ARC documentation. */
    private final DateFormat arcDateFormat;

    /** WARC <code>DateFormat</code> as speficied in the WARC ISO standard. */
    private final DateFormat warcDateFormat;

    /** Basic <code>DateFormat</code> is not thread safe. */
    private static final ThreadLocal<ArchiveDateConverter> DateParserTL =
        new ThreadLocal<ArchiveDateConverter>() {
        public ArchiveDateConverter initialValue() {
            return new ArchiveDateConverter();
        }
    };

    /**
     * Creates a new <code>DateParser</code>.
     */
    private ArchiveDateConverter() {
        arcDateFormat = new SimpleDateFormat(ARC_DATE_FORMAT);
        arcDateFormat.setLenient(false);
        arcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        warcDateFormat = new SimpleDateFormat(WARC_DATE_FORMAT);
        warcDateFormat.setLenient(false);
        warcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static DateFormat getArcDateFormat() {
    	return DateParserTL.get().arcDateFormat;
    }

    public static DateFormat getWarcDateFormat() {
    	return DateParserTL.get().warcDateFormat;
    }

}

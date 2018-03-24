/*
 * #%L
 * Netarchivesuite - common
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
package dk.netarkivet.common.utils.archive;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Utility class for dispensing ARC/WARC <code>DateFormat</code> objects. Each object is thread safe as long as it it
 * only used by the same thread. This means no caching of this object for later use unless its by the same thread.
 * <code>ThreadLocal</code> handles automatic instantiation and cleanup of objects.
 *
 * @author nicl
 */
public class ArchiveDateConverter {

    /** ARC date format string as speficied in the ARC documentation. */
    public static final String ARC_DATE_FORMAT = "yyyyMMddHHmmss";

    /** WARC date format string as specified by the WARC ISO standard. */
    public static final String WARC_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /** ARC <code>DateFormat</code> as specified in the ARC documentation. */
    private final DateFormat arcDateFormat;

    /** WARC <code>DateFormat</code> as speficied in the WARC ISO standard. */
    private final DateFormat warcDateFormat;

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

    /**
     * <code>DateFormat</code> is not thread safe, so we wrap its construction inside a <code>ThreadLocal</code> object.
     */
    private static final ThreadLocal<ArchiveDateConverter> DateParserTL = new ThreadLocal<ArchiveDateConverter>() {
        @Override
        public ArchiveDateConverter initialValue() {
            return new ArchiveDateConverter();
        }
    };

    /**
     * Returns a <code>DateFormat</code> object for ARC date conversion.
     *
     * @return a <code>DateFormat</code> object for ARC date conversion
     */
    public static DateFormat getArcDateFormat() {
        return DateParserTL.get().arcDateFormat;
    }

    /**
     * Returns a <code>DateFormat</code> object for WARC date conversion.
     *
     * @return a <code>DateFormat</code> object for WARC date conversion
     */
    public static DateFormat getWarcDateFormat() {
        return DateParserTL.get().warcDateFormat;
    }

}

/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Author:      $Date$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package dk.netarkivet.harvester.datamodel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;

/**
 * Constants used by the datamodel and webinterface packages.
 *
 */

public class Constants {
    /** Pattern not used by anyone, except unittests. */
    private static final Pattern ID_PATTERN =
            Pattern.compile(".*_(\\d+)\\.xml");
    /** Regexp for checking, if URL contains a protocol, 
     * like ftp://, http:// . */
    static final String PROTOCOL_REGEXP = "^[a-zA-Z]+:.*";
    /** Maximum size of name entries in the database. */
    static final int MAX_NAME_SIZE = 300;
    /** Maximum size of comment entries in the database. */
    static final int MAX_COMMENT_SIZE = 30000;
    /** Maximum size of crawlertrap entries in the database. */
    static final int MAX_CRAWLER_TRAP_SIZE = 1000;
    /** Maximum size of password url entries in the database. */
    static final int MAX_URL_SIZE = 300;
    /** Maximum size of password realm entries in the database. */
    static final int MAX_REALM_NAME_SIZE = 300;
    /** Maximum size of password username entries in the database. */
    static final int MAX_USER_NAME_SIZE = 20;
    /** Maximum size of password entries in the database. */
    static final int MAX_PASSWORD_SIZE = 40;
    /** Maximum size of ownerinfo entries in the database. */
    static final int MAX_OWNERINFO_SIZE = 1000;
    /** Maximum size of seedlist entries in the database. */
    static final int MAX_SEED_LIST_SIZE = 8 * 1024 * 1024;
    /** Maximum size of a combined seedlist entry (for a job)
     * in the database. */
    static final int MAX_COMBINED_SEED_LIST_SIZE = 64 * 1024 * 1024;
    /** Maximum size of orderxml entries (stringified XML) in the database. */
     static final int MAX_ORDERXML_SIZE = 64 * 1024 * 1024;
    /** Maximum size of error messages from harvests and uploads. */
    public static final int MAX_ERROR_SIZE = 300;
    /** Maximum size of detailed error messages from harvests and uploads. */
    public static final int MAX_ERROR_DETAIL_SIZE = 10000;
    /** This is the default number set as max request rate. */
    public static final int DEFAULT_MAX_REQUEST_RATE = 60;
    /** Max bytes of -1 means infinity. */
    public static final long HERITRIX_MAXBYTES_INFINITY = -1L;
    /** Max objects of -1 means infinity. */
    public static final long HERITRIX_MAXOBJECTS_INFINITY = -1L;
    /** This is the default number set as max bytes harvested.
     * Set to the max number of bytes we harvest from any domain per harvest,
     * unless explicitly deciding otherwise. */
    public static final long DEFAULT_MAX_BYTES
            = Settings.getLong(HarvesterSettings.DOMAIN_CONFIG_MAXBYTES);
    /** This is the default number set as max harvested objects. Note, that
     * although this is a long it is sometimes used as an int, so don't set
     * this value too high. Default max objects should now be infinity, since
     * we use the byte limit. */
    public static final long DEFAULT_MAX_OBJECTS 
        = Settings.getLong(HarvesterSettings.DOMAIN_CONFIG_MAXOBJECTS);
    /**
     * Default value for alias timeout, 1 year.
     * TODO make this into a setting in HarvesterSettings
     */
    public static final long ALIAS_TIMEOUT_IN_MILLISECONDS = 365 * 24 * 60 * 60
                                                             * 1000L;
    /** Descending sort order. */
    public static final String DESCENDING_SORT_ORDER = "DESC";
    /** Ascending sort order. */
    public static final String ASCENDING_SORT_ORDER = "ASC";

    /** Settings used in JobDBDao after admin machine break down. **/
    public static final String NEXT_JOB_ID =
                "settings.harvester.datamodel.domain.nextJobId";
    
    /** Uncallable constructor. */
    private Constants() { }

    /**
     * Returns a new matcher that matches harvest definition file names and
     * sets group 1 to be the id part.
     *
     * @return A new matcher instance.
     */
    public static Matcher getIdMatcher() {
        return ID_PATTERN.matcher("");
    }
}

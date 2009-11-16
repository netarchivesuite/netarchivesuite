/*$Id$
* $Revision$
* $Author$
* $Date$
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
package dk.netarkivet.common;

import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

/**
 * This class is used for global constants only.
 *
 * If your constant is only to be used in a single package, put it in a
 * Constants-class in that package, and make sure it is package private (no
 * modifiers).
 *
 * If your constant is used in a single class only, put it in that class, and
 * make sure it is private
 *
 * Remember everything placed here MUST be constants.
 *
 * This class is never instantiated, so thread security is not an issue.
 *
 * Date: Feb 15, 2005 Time: 6:04:25 PM
 */
public class Constants {
    /** The pattern for an IP-address key. */
    public static final String IP_REGEX_STRING
            = "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}";
    /** A full string matcher for an IP-address. */
    public static final Pattern IP_KEY_REGEXP
            = Pattern.compile("^" + IP_REGEX_STRING + "$");
    /** A full string matcher for an IPv6-address. */
    public static final Pattern IPv6_KEY_REGEXP
            = Pattern.compile("^([0-9A-F]{1,2}\\:){5}[0-9A-F]{1,2}$");
    /**
     * The suffic of a regexp that matches the metadata files.  Add job IDs to
     * the front as necessary.
     */
    public static final String METADATA_FILE_PATTERN_SUFFIX
            = "-metadata-[0-9]+.arc";
    /** The mimetype for a list of CDX entries. */
    public static final String CDX_MIME_TYPE =
            "application/x-cdx";

    /** Possible states of code. */
    private static enum CodeStatus {
        RELEASE, CODEFREEZE, UNSTABLE
    }

    /** Extension of xml file names. */
    public static final String XML_EXTENSION = ".xml";

    //It is QA's responsibility to update the following parameters on all
    // release and codefreeze actions
    /** Major version number. */
    public static final int MAJORVERSION = 3;
    /** Minor version number. */
    public static final int MINORVERSION = 10;
    /** Patch version number. */
    public static final int PATCHVERSION = 0;
    /** Current status of code. */
    private static final CodeStatus BUILDSTATUS = CodeStatus.CODEFREEZE;

    /** Current version of Heritrix used by netarkivet-code. */
    private static final String HERITRIX_VERSION = "1.14.3";

    /**
     * Read this much data when copying data from a file channel. Note that due
     * to a bug in java, this should never be set larger than Integer.MAX_VALUE,
     * since a call to fileChannel.transferFrom/To fails with an error while
     * calling mmap.
     */
    public static final long IO_CHUNK_SIZE = 65536L;
    /** The dirname of the heritrix directory with arcfiles. */
    public static final String ARCDIRECTORY_NAME = "arcs";
    /**
     * How big a buffer we use for read()/write() operations on InputStream/
     * OutputStream.
     */
    public static final int IO_BUFFER_SIZE = 4096;
    /**
     * The organization behind the harvesting. Only used by the
     * HarvestDocumentation class
     */
    public static final String ORGANIZATION_NAME = "netarkivet.dk";
    /** The date format used for NetarchiveSuite dateformatting. */
    private static final String ISO_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss Z";

    /** Internationalisation resource bundle for common module. */
    public static final String TRANSLATIONS_BUNDLE =
            "dk.netarkivet.common.Translations";

    /**
     * Private constructor that does absolutely nothing. Necessary in order to
     * prevent initialization.
     */
    private Constants() {
        //Not to be initialised
    }

    /**
     * Get a human-readable version string.
     *
     * @return A string telling current version and status of code.
     */
    public static String getVersionString() {
        return "Version: " + MAJORVERSION + "." + MINORVERSION + "."
               + PATCHVERSION + " status " + BUILDSTATUS;
    }

    /**
     * Get the Heritrix version presently in use.
     *
     * @return the Heritrix version presently in use
     */
    public static String getHeritrixVersionString() {
        return HERITRIX_VERSION;
    }


    /**
     * Get a formatter that can read and write a date in ISO format including
     * hours/minutes/seconds and timezone.
     *
     * @return The formatter.
     */
    public static SimpleDateFormat getIsoDateFormatter() {
        return new SimpleDateFormat(ISO_DATE_FORMAT);
    }

    /** One minute in milliseconds. */
    public static final long ONE_MIN_IN_MILLIES = 60 * 1000;

    /** One day in milli seconds. */
    public static final long ONE_DAY_IN_MILLIES = 24 * 60 * ONE_MIN_IN_MILLIES;

    /** Pattern that matches our our CDX mime type. */
    public static String CDX_MIME_PATTERN = "application/x-cdx";

    /** Pattern that matches everything. */
    public static String ALL_PATTERN = ".*";
}

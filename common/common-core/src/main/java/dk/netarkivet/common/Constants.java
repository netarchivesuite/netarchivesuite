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
package dk.netarkivet.common;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used for global constants only.
 * <p>
 * If your constant is only to be used in a single package, put it in a Constants-class in that package, and make sure
 * it is package private (no modifiers).
 * <p>
 * If your constant is used in a single class only, put it in that class, and make sure it is private.
 * <p>
 * Remember everything placed here MUST be constants.
 * <p>
 * This class is never instantiated, so thread security is not an issue.
 */
public final class Constants {

    private static final Logger log = LoggerFactory.getLogger(Constants.class);


    /** The pattern for an IP-address key. */
    public static final String IP_REGEX_STRING = "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}";
    /** A full string matcher for an IP-address. */
    public static final Pattern IP_KEY_REGEXP = Pattern.compile("^" + IP_REGEX_STRING + "$");
    /** A full string matcher for an IPv6-address. */
    public static final Pattern IPv6_KEY_REGEXP = Pattern.compile("^([0-9A-F]{1,2}\\:){5}[0-9A-F]{1,2}$");

    /** The mimetype for a list of CDX entries. */
    public static final String CDX_MIME_TYPE = "application/x-cdx";

    /** Extension of XML file names. */
    public static final String XML_EXTENSION = ".xml";

    // Version string. */
    private static String versionHtml;
    private static String version;

    /** Current version of Heritrix 1 used by netarkivet-code. */
    private static final String HERITRIX_VERSION = "1.14.4";
    
    /**
     * The code will try to read the heritrix version from the pom in the jar Manifest. This
     * constant is only ever read as a fallback.
     * */
    private static final String HERITRIX3_VERSION = "3.4.0-NAS-7.5.1-SNAPSHOT";

    /**
     * Read this much data when copying data from a file channel. Note that due to a bug in java, this should never be
     * set larger than Integer.MAX_VALUE, since a call to fileChannel.transferFrom/To fails with an error while calling
     * mmap.
     */
    public static final long IO_CHUNK_SIZE = 65536L;
    /** The directory name of the heritrix directory with arcfiles. */
    public static final String ARCDIRECTORY_NAME = "arcs";
    /** The directory name of the heritrix directory with warcfiles. */
    public static final String WARCDIRECTORY_NAME = "warcs";
    /**
     * How big a buffer we use for read()/write() operations on InputStream/ OutputStream.
     */
    public static final int IO_BUFFER_SIZE = 4096;

    /** The date format used for NetarchiveSuite dateformatting. */
    private static final String ISO_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss Z";

    /** Internationalisation resource bundle for common module. */
    public static final String TRANSLATIONS_BUNDLE = "dk.netarkivet.common.Translations";

    /**
     * Private constructor that does absolutely nothing. Necessary in order to prevent initialization.
     */
    private Constants() {
        // Not to be initialised
    }

    /**
     * Get a human-readable version string.
     * @param isHtmlFormat if true, return a html format for the human-readable version string.
     * @return A string telling current version and status of code.
     */
    public static String getVersionString(boolean isHtmlFormat) {
        if (version == null || versionHtml == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Version: ");
            sb.append(Constants.class.getPackage().getSpecificationVersion());
            String implementationVersion = Constants.class.getPackage().getImplementationVersion();
            StringBuilder sbHtml = new StringBuilder(sb);
            if (implementationVersion != null && implementationVersion.length() == 40) {
            	sbHtml.append(" (<a href=\"https://github.com/netarchivesuite/netarchivesuite/commit/");
            	sbHtml.append(implementationVersion);
            	sbHtml.append("\">");
            	sbHtml.append(implementationVersion.substring(0, 10));
            	sbHtml.append("</a>)");

            	sb.append(" (https://github.com/netarchivesuite/netarchivesuite/commit/");
            	sb.append(implementationVersion);
            	sb.append(")");
            }
            version = sb.toString();
            versionHtml = sbHtml.toString();
        }
        if(isHtmlFormat) {
        	return versionHtml;
        } else {
        	return version;
        }
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
     * Get a formatter that can read and write a date in ISO format including hours/minutes/seconds and timezone.
     *
     * @return The formatter.
     */
    public static SimpleDateFormat getIsoDateFormatter() {
        return new SimpleDateFormat(ISO_DATE_FORMAT);
    }

    /** One minute in milliseconds. */
    public static final long ONE_MIN_IN_MILLIES = 60 * 1000;

    /** One day in milliseconds. */
    public static final long ONE_DAY_IN_MILLIES = 24 * 60 * ONE_MIN_IN_MILLIES;

    /** Pattern that matches our our CDX mimetype. */
    public static final String CDX_MIME_PATTERN = "application/x-cdx";

    /** Pattern that matches everything. */
    public static final String ALL_PATTERN = ".*";

    /** Lucene version used by this release of NetarchiveSuite. */
    public static final Version LUCENE_VERSION = Version.LUCENE_44;

    /** The current website for the NetarchiveSuite project. */
    public static final String PROJECT_WEBSITE = "https://sbforge.org/display/NAS";

    public static String getHeritrix3VersionString() {
        String h3version = null;
        try {
            Properties p = new Properties();
            InputStream is = Constants.class.getResourceAsStream("/META-INF/maven/org.archive.heritrix/heritrix-commons/pom.properties");
            if (is != null) {
                p.load(is);
                h3version = p.getProperty("version", "");
               log.debug("H3 version read from pom: {}", h3version);
            }
        } catch (Exception e) {
            // ignore
        }
        if (h3version == null) {
            Package aPackage = org.archive.util.UriUtils.class.getPackage();
            if (aPackage != null) {
                h3version = aPackage.getImplementationVersion();
                log.debug("H3 version read from implementation: {}", h3version);
                if (h3version == null) {
                    h3version = aPackage.getSpecificationVersion();
                    log.debug("H3 version read from specification: {}", h3version);
                }
            }
        }
        if (h3version == null) {
            log.warn("Could not determine Heritrix 3 version, falling back to hard-coded"
                    + "value {}", HERITRIX3_VERSION);
            h3version = HERITRIX3_VERSION;
        }
        log.debug("Final H3 version: {}", h3version);
        return h3version;
    }
}

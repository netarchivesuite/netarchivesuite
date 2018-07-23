/*
 * #%L
 * Netarchivesuite - harvester
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

package dk.netarkivet.harvester.datamodel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;

/**
 * Constants used by the datamodel and webinterface packages.
 */
public final class Constants {

    /** Pattern not used by anyone, except unittests. */
    private static final Pattern ID_PATTERN = Pattern.compile(".*_(\\d+)\\.xml");
    /**
     * Regexp for checking, if URL contains a protocol, like ftp://, http:// .
     */
    static final String PROTOCOL_REGEXP = "^[a-zA-Z]+:.*";
    /** Maximum size of name entries in the database. */
    static final int MAX_NAME_SIZE = 300;
    /** Maximum size of comment entries in the database. */
    static final int MAX_COMMENT_SIZE = 30000;
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
    /**
     * Maximum size of a combined seedlist entry (for a job) in the database.
     */
    static final int MAX_COMBINED_SEED_LIST_SIZE = 64 * 1024 * 1024;
    /** Maximum size of orderxml entries (stringified XML) in the database. */
    static final int MAX_ORDERXML_SIZE = 64 * 1024 * 1024;
    /** Maximum size of error messages from harvests and uploads. */
    public static final int MAX_ERROR_SIZE = 300;
    /** Maximum size of detailed error messages from harvests and uploads. */
    public static final int MAX_ERROR_DETAIL_SIZE = 10000;
    /** This is the default number set as max request rate. */
    public static final int DEFAULT_MAX_REQUEST_RATE = 60;
    /**
     * Max bytes of -1 means infinity (i.e other factors will determine when the job ends).
     */
    public static final long HERITRIX_MAXBYTES_INFINITY = -1L;
    /**
     * Max objects of -1 means infinity (i.e other factors will determine when the job ends).
     */
    public static final long HERITRIX_MAXOBJECTS_INFINITY = -1L;

    /**
     * Max job running time of 0 means infinite job running time (i.e other factors will determine when the job ends).
     */
    public static final long HERITRIX_MAXJOBRUNNINGTIME_INFINITY = 0L;

    /**
     * This is the default number set as max bytes harvested. Set to the max number of bytes we harvest from any domain
     * per harvest, unless explicitly deciding otherwise.
     */
    public static final long DEFAULT_MAX_BYTES = Settings.getLong(HarvesterSettings.DOMAIN_CONFIG_MAXBYTES);

    /** This is the default number set as max harvested objects. */
    public static final long DEFAULT_MAX_OBJECTS = Settings.getLong(HarvesterSettings.DOMAIN_CONFIG_MAXOBJECTS);

    /**
     * The default maximum time in seconds available for each harvesting job. Set to unlimited (0) in the default
     * settings. Used to restrict the running time for snapshot harvest jobs.
     */
    public static final long DEFAULT_MAX_JOB_RUNNING_TIME = Settings
            .getLong(HarvesterSettings.JOBS_MAX_TIME_TO_COMPLETE);

    /**
     * The value for alias timeout, in milliseconds.
     */
    public static final long ALIAS_TIMEOUT_IN_MILLISECONDS = Settings.getLong(HarvesterSettings.ALIAS_TIMEOUT) * 1000L;

    /** Settings used in JobDBDao after admin machine break down. * */
    public static final String NEXT_JOB_ID = "settings.harvester.datamodel.domain.nextJobId";

    /**
     * The name used for the element in order.xml which contains global crawler traps.
     */
    public static final String GLOBAL_CRAWLER_TRAPS_ELEMENT_NAME = "dk.netarkivet.global_crawler_traps";

    public static final long BYTES_PER_HERITRIX_BYTELIMIT_UNIT = 1024;

    /** Uncallable constructor. */
    private Constants() {
    }

    /**
     * Returns a new matcher that matches harvest definition file names and sets group 1 to be the id part.
     *
     * @return A new matcher instance.
     */
    public static Matcher getIdMatcher() {
        return ID_PATTERN.matcher("");
    }

}

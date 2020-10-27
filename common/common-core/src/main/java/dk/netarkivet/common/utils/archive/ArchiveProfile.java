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

import java.io.FilenameFilter;
import java.util.regex.Pattern;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.utils.FileUtils;

/**
 * Assemble the constants related to an archive format into profiles. Currently only an ARC and WARC profile.
 */
public class ArchiveProfile {

    /** Archive filename filter. */
    public final FilenameFilter filename_filter;

    /** Archive filename string pattern. */
    public final String filename_pattern;

    /** Archive metadata filename regex pattern. */
    public final Pattern metadataFilenamePattern;

    /** Archive directory. */
    public final String archive_directory;

    /**
     * Construct an archive profile.
     *
     * @param filename_filter archive filename filter
     * @param filename_pattern archive filename string pattern
     * @param metadataFilenamePattern archive metadata filename regex pattern
     * @param archive_directory archive directory
     */
    protected ArchiveProfile(FilenameFilter filename_filter, String filename_pattern, Pattern metadataFilenamePattern,
            String archive_directory) {
        this.filename_filter = filename_filter;
        this.filename_pattern = filename_pattern;
        this.metadataFilenamePattern = metadataFilenamePattern;
        this.archive_directory = archive_directory;
    }

    /** ARC archive profile. */
    public static final ArchiveProfile ARC_PROFILE = new ArchiveProfile(FileUtils.ARCS_FILTER, FileUtils.ARC_PATTERN,
            Pattern.compile("([\\w]+)-metadata-([0-9]+).arc"), Constants.ARCDIRECTORY_NAME);

    /** WARC archive profile. */
    public static final ArchiveProfile WARC_PROFILE = new ArchiveProfile(FileUtils.WARCS_FILTER,
            FileUtils.WARC_PATTERN, Pattern.compile("([\\w]+)-metadata-([0-9]+).warc"), Constants.WARCDIRECTORY_NAME);

}

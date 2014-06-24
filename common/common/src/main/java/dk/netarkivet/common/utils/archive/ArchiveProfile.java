
package dk.netarkivet.common.utils.archive;

import java.io.FilenameFilter;
import java.util.regex.Pattern;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.utils.FileUtils;

/**
 * Assemble the constants related to an archive format into profiles.
 * Currently only an ARC and WARC profile.
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
     * @param filename_filter archive filename filter
     * @param filename_pattern archive filename string pattern
     * @param metadataFilenamePattern archive metadata filename regex pattern
     * @param archive_directory archive directory
     */
    protected ArchiveProfile(FilenameFilter filename_filter,
            String filename_pattern,
            Pattern metadataFilenamePattern,
            String archive_directory) {
        this.filename_filter = filename_filter;
        this.filename_pattern = filename_pattern;
        this.metadataFilenamePattern = metadataFilenamePattern;
        this.archive_directory = archive_directory;
    }

    /** ARC archive profile. */
    public static final ArchiveProfile ARC_PROFILE = new ArchiveProfile(
            FileUtils.ARCS_FILTER,
            FileUtils.ARC_PATTERN,
            Pattern.compile("([0-9]+)-metadata-([0-9]+).arc"),
            Constants.ARCDIRECTORY_NAME
            );

    /** WARC archive profile. */
    public static final ArchiveProfile WARC_PROFILE = new ArchiveProfile(
            FileUtils.WARCS_FILTER,
            FileUtils.WARC_PATTERN,
            Pattern.compile("([0-9]+)-metadata-([0-9]+).warc"),
            Constants.WARCDIRECTORY_NAME
            );

}

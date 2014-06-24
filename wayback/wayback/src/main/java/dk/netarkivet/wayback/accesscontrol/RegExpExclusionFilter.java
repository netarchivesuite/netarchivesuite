package dk.netarkivet.wayback.accesscontrol;

import java.util.Collection;
import java.util.regex.Pattern;

import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.resourceindex.filters.ExclusionFilter;
import org.archive.wayback.util.ObjectFilter;

public class RegExpExclusionFilter extends ExclusionFilter {

    /**
     * The regexps to be used as the exclusion filter.
     */
    Collection<Pattern> regexps;

    /**
     * Creates an exclusion filter which will filter out any search result for
     * which the original url matches any of the specified regular expression.
     * @param regexps The regular expressions to match.
     */
    public RegExpExclusionFilter(Collection<Pattern> regexps) {
        this.regexps = regexps;
    }

    @Override
    public int filterObject(CaptureSearchResult captureSearchResult) {
        // Note that the behaviour of the two calls to methods of the class
        // ExclusionCaptureFilterGroup is not well documented. Omitting them
        // results in the excluded objects being marked as not in the archive.
        // With these calls, they are correctly identified as blocked.
        filterGroup.setSawAdministrative();
        for (Pattern regexp: regexps) {
            if (regexp.matcher(captureSearchResult.getOriginalUrl()).matches()) {
                return ObjectFilter.FILTER_EXCLUDE;
            }
        }
        filterGroup.setPassedAdministrative();
        return ObjectFilter.FILTER_INCLUDE;
    }
}

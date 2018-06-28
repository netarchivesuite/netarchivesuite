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

import java.util.Locale;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.I18n;

/**
 * Class for containing a reason for stopping the harvesting of a domain. There are five possible reasons:<br>
 * 1) We have now harvested the whole domain (DOWNLOAD_COMPLETE) <br>
 * 2) We have now harvested the number of objects allowed from this domain in this iteration (OBJECT_LIMIT) <br>
 * 3) We have now harvested the the number of bytes allowed from this domain in this iteration (SIZE_LIMIT) <br>
 * 4) We stopped harvesting because we hit the per-configuration limit (CONFIG_SIZE_LIMIT) <br>
 * 5) We don't know whether or not the harvesting is completed, because the crawler did not finish in an orderly way
 * (DOWNLOAD_UNFINISHED) <br>
 * <p>
 * Note: This enum is serialized to the database using the order in which these are defined. Thus the order of stop
 * reasons MUST NOT BE CHANGED!
 */
public enum StopReason {

    /**
     * Stop reason is download complete, when all pages within the scope of the harvest template have been downloaded.
     */
    DOWNLOAD_COMPLETE,
    /**
     * Stop reason is object limit reached, when the domain reached the maximum number of objects allowed by the
     * harvest.
     */
    OBJECT_LIMIT,
    /**
     * Stop reason is size limit reached, when the domain reached the maximum number of bytes allowed by the harvest.
     */
    SIZE_LIMIT,
    /**
     * Stop reason is configuration size limit reached, when the domain reached the maximum number of bytes allowed by
     * the configuration.
     */
    CONFIG_SIZE_LIMIT,
    /**
     * Stop reason is download unfinished, when we don't know whether or not the harvesting is completed, because the
     * crawler did not finish in an orderly way.
     */
    DOWNLOAD_UNFINISHED,
    /**
     * Stop reason is configuration object limit reached, when the domain reached the maximum number of objects allowed
     * by the configuration.
     */
    CONFIG_OBJECT_LIMIT,

    /**
     * Stop reason is harvesting time limit reached, when the harvester is not finished with harvesting the domain when
     * the harvester reaches its time-limit.
     */
    TIME_LIMIT;

    /** Internationalisation object. */
    private static final I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);

    /**
     * Get the StopReason corresponding to the given positive integer.
     *
     * @param stopreasonNum a given positive integer
     * @return the StopReason corresponding to the given positive integer
     * @see StopReason#ordinal()
     */
    static StopReason getStopReason(final int stopreasonNum) {
        switch (stopreasonNum) {
        case 0:
            return DOWNLOAD_COMPLETE;
        case 1:
            return OBJECT_LIMIT;
        case 2:
            return SIZE_LIMIT;
        case 3:
            return CONFIG_SIZE_LIMIT;
        case 4:
            return DOWNLOAD_UNFINISHED;
        case 5:
            return CONFIG_OBJECT_LIMIT;
        case 6:
            return TIME_LIMIT;
        default:
            throw new UnknownID("No stop reason assigned to " + stopreasonNum);
        }
    }

    /**
     * Return a localized string describing a stopreason.
     *
     * @param l the locale
     * @return a localized string describing a stopreason.
     */
    public String getLocalizedString(Locale l) {
        ArgumentNotValid.checkNotNull(l, "l");
        switch (this) {
        case DOWNLOAD_COMPLETE:
            return I18N.getString(l, "stopreason.complete");
        case OBJECT_LIMIT:
            return I18N.getString(l, "stopreason.max.objects.limit.reached");
        case CONFIG_OBJECT_LIMIT:
            return I18N.getString(l, "stopreason.max.domainobjects.limit.reached");
        case SIZE_LIMIT:
            return I18N.getString(l, "stopreason.max.bytes.limit.reached");
        case CONFIG_SIZE_LIMIT:
            return I18N.getString(l, "stopreason.max.domainconfig.limit.reached");
        case DOWNLOAD_UNFINISHED:
            return I18N.getString(l, "stopreason.download.unfinished");
        case TIME_LIMIT:
            return I18N.getString(l, "stopreason.timelimit.reached");
        default:
            return I18N.getString(l, "stopreason.unknown.0", this);
        }
    }

}

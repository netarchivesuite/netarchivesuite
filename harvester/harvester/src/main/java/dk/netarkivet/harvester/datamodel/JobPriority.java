/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

import java.util.Locale;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.I18n;

/**
 * Enum of the possible priorities of a job. Jobs with a given priority are sent
 * to the same queue, and each harvester listens to one if them.
*/
public enum JobPriority {
    /**
     * Low priority jobs. Used for snapshot harvests.
     */
    LOWPRIORITY,
    /**
     * High priority jobs. Used for selected and event harvests.
     */
    HIGHPRIORITY;
    /** Internationalisation object. */
    private static final I18n I18N
          = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);


    /** Helper method that gives a proper object from e.g. a DB-stored value.
     *
     * @param status a certain integer
     * @return the JobPrioroty related to a certain integer
     */
    public static JobPriority fromOrdinal(int status) {
        switch (status) {
            case 0: return LOWPRIORITY;
            case 1: return HIGHPRIORITY;
            default: throw new ArgumentNotValid("Invalid job priority "
                                                + status);
        }
    }

    /**
     * Return a localized human-readable string describing a job with
     * this priority.
     *
     * Strings are read from the harvester translation bundle found in
     * Translation.properties in this module.
     *
     * @param l The locale
     * @return A human readable string for that locale.
     * @throws ArgumentNotValid on null locale.
     */
    public String getLocalizedString(Locale l) {
        ArgumentNotValid.checkNotNull(l, "Locale l");
        switch (this) {
            case LOWPRIORITY:
                return I18N.getString(l, "full.harvest");
            case HIGHPRIORITY:
                return I18N.getString(l, "partial.harvest");
            default:
                return I18N.getString(l, "unknown.harvest.type.0",
                                      this.toString());
        }
    }
}

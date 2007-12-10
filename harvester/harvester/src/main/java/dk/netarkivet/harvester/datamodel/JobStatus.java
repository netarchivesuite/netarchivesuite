/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
 * Enum of the possible statuses (alt.: status) a Job can be in.
 *
 */
public enum JobStatus {
    NEW, SUBMITTED, STARTED, DONE, FAILED, RESUBMITTED;

    /** Internationalisation object. */
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);

    /** Helper method that gives a proper object from e.g. a DB-stored value.
     *
     * @param status a certain integer
     * @return the JobStatus related to a certain integer
     * @throws ArgumentNotValid
     */
    public static JobStatus fromOrdinal(int status) {
        switch (status) {
            case 0: return NEW;
            case 1: return SUBMITTED;
            case 2: return STARTED;
            case 3: return DONE;
            case 4: return FAILED;
            case 5: return RESUBMITTED;
            default: throw new ArgumentNotValid("Invalid job status " + status);
        }
    }

    /**
     * Return a localized human-readable string describing this status.
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
            case NEW:
                return I18N.getString(l, "status.job.new");
            case SUBMITTED:
                return I18N.getString(l, "status.job.submitted");
            case STARTED:
                return I18N.getString(l, "status.job.started");
            case DONE:
                return I18N.getString(l, "status.job.done");
            case FAILED:
                return I18N.getString(l, "status.job.failed");
            case RESUBMITTED:
                return I18N.getString(l, "status.job.resubmitted");
            default:
                return I18N.getString(l, "status.job.unknown", this.toString());
        }
    }

    /** True if it is legal to change from this status to a new status.
     *
     * @param newStatus a new JobStatus
     * @return true if it is legal to go from the current status to this new status
     */
    public boolean legalChange(JobStatus newStatus) {
        return newStatus.ordinal() >= ordinal();
    }
}

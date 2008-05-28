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

package dk.netarkivet.viewerproxy;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import dk.netarkivet.common.distribute.indexserver.Index;
import dk.netarkivet.common.distribute.indexserver.JobIndexCache;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.utils.StringUtils;

/**
 * Control of viewer proxy. Delegates URL-methods to a missing URL recorder, and
 * cdx control to a CDXCache instance combined with an ARCArchiveAccess
 * instance.
 *
 */

public class DelegatingController implements Controller {
    /**
     * The missing URL recorder which handles missing URL collection.
     */
    private MissingURIRecorder mur;
    /**
     * The CDX cache which generates a CDX from a list of jobs on changeIndex
     * command.
     */
    private JobIndexCache cc;
    /**
     * The ARCArchiveAccess instance to receive new cdx on changeIndex command.
     */
    private ARCArchiveAccess aaa;
    /**
     * Remembers the last joblist used for setting index for status purposes.
     */
    private Set<Long> jobSet;
    /**
     * Remembers the jobs that were available from the last index job list for
     * status purposes.
     */
    private Set<Long> availableSet;

    /**
     * Remembers the label of the index for status purposes.
     */
    private String indexLabel;
    /**
     * Internationalisation context.
     */
    private final static I18n I18N = new I18n(Constants.TRANSLATIONS_BUNDLE);

    /**
     * Initialise a controller with the relevant instances to control
     *
     * @param mur The missing URL recorder which handles missing URL
     *            collection.
     * @param cc  The CDX cache which generates an index from a list of jobs on
     *            changeIndex command.
     * @param aaa The ARCArchiveAccess instance to receive new cdx on
     *            changeIndex command.
     * @throws ArgumentNotValid if any argument is null.
     */
    public DelegatingController(MissingURIRecorder mur, JobIndexCache cc,
                      ARCArchiveAccess aaa) {
        ArgumentNotValid.checkNotNull(mur, "MissingURIRecorder mur");
        ArgumentNotValid.checkNotNull(cc, "CDXCache cc");
        ArgumentNotValid.checkNotNull(aaa, "ARCArchiveAccess aaa");
        this.mur = mur;
        this.cc = cc;
        this.aaa = aaa;
    }

    /**
     * Start collecting missing URLs.
     */
    public void startRecordingURIs() {
        mur.startRecordingURIs();
    }

    /**
     * Stop collecting missing URLs.
     */
    public void stopRecordingURIs() {
        mur.stopRecordingURIs();
    }

    /**
     * Clear list of missing URLs.
     */
    public void clearRecordedURIs() {
        mur.clearRecordedURIs();
    }

    /**
     * Get list of missing URLs.
     *
     * @return The list of missing URLs.
     */
    public Set<URI> getRecordedURIs() {
        return mur.getRecordedURIs();
    }

    /**
     * Change index to use an index based on a list of jobs. Note: Does not
     * check arguments. This is a task for the mediated classes,
     * ArcArchiveAccess and CDXCache.
     *
     * @param jobSet List of jobs to get an index for.
     * @param label The label this index should be known as
     */
    public void changeIndex(Set<Long> jobSet, String label) {
        Index<Set<Long>> jobindex = cc.getIndex(jobSet);
        aaa.setIndex(jobindex.getIndexFile());
        this.availableSet = jobindex.getIndexSet();
        this.jobSet = jobSet;
        this.indexLabel = label;
    }

    /** Get a human readable status of the viewer proxy.
     *
     * @return A human readable status string.
     * @param locale The locale used to generate the string
     */
    public String getStatus(Locale locale) {
        ArgumentNotValid.checkNotNull(locale, "locale");
        StringBuilder status = new StringBuilder();
        if (mur.isRecordingURIs()) {
            status.append(I18N.getString(locale, "currently.collecting.urls"));
        } else {
            status.append(I18N.getString(locale,
                    "currently.not.collecting.urls"));
        }
        status.append('\n');
        status.append(I18N.getString(locale, "current.list.contains.0.urls",
                mur.getRecordedURIs().size()));
        status.append('\n');
        if (jobSet == null) {
            status.append(I18N.getString(locale, "no.index.set"));
        } else {
            List<Long> availableList = new ArrayList<Long>(availableSet);
            Collections.sort(availableList);
            status.append(I18N.getString(locale, "index.0.built.on.jobs.1",
                                         indexLabel,
                                         StringUtils.conjoin(
                                                 ", ", availableList)));
            if (!availableSet.containsAll(jobSet)) {
                //Generate a status message that lists
                // - what was requested
                // - what is available
                // - what is missing
                Set<Long> missingSet = new HashSet<Long>(jobSet);
                missingSet.removeAll(availableSet);
                List<Long> jobList = new ArrayList<Long>(jobSet);
                Collections.sort(jobList);
                List<Long> missingList = new ArrayList<Long>(missingSet);
                Collections.sort(missingList);
                status.append('\n');
                status.append(I18N.getString(
                        locale,
                        "errormsg;request.was.for.0.but.got.1.missing.2",
                        StringUtils.conjoin(", ", jobList),
                        StringUtils.conjoin(", ", availableList),
                        StringUtils.conjoin(", ", missingList)));
            }
        }
        return status.toString();
    }
}

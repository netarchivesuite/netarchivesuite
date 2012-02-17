/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
package dk.netarkivet.harvester.harvesting.frontier;

import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.exceptions.HarvestingAbort;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.utils.TimeUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.controller.BnfHeritrixController;
import dk.netarkivet.harvester.harvesting.distribute.FrontierReportMessage;
import dk.netarkivet.harvester.harvesting.monitor.HarvestMonitor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Implements the analysis of a full frontier report obtained from Heritrix,
 * as the execution of a sequence of user-defined filters, that each generate
 * a smaller, in-memory frontier report that are sent in a JMS message to the
 * {@link HarvestMonitor}.
 *
 */
public class FrontierReportAnalyzer implements Runnable {

    /** The logger to use.    */
    static final Log LOG = LogFactory.getLog(
            FrontierReportAnalyzer.class);
    /** The controller used to communicate with the Heritrix instance. */
    private final BnfHeritrixController heritrixController;
    /** The last time this Analyzer was executed. */
    private long lastExecTime = System.currentTimeMillis();

    /**
     * Builds an analyzer, given an Heritrix controller instance.
     * @param heritrixController the controller allowing communication with the
     * Heritrix crawler instance.
     */
    public FrontierReportAnalyzer(
            BnfHeritrixController heritrixController) {
        super();
        this.heritrixController = heritrixController;

        // Build list of filters from the settings.

        String[] filterClasses = Settings.getAll(
                HarvesterSettings.FRONTIER_REPORT_FILTER_CLASS);
        String[] filterArgs = Settings.getAll(
                HarvesterSettings.FRONTIER_REPORT_FILTER_ARGS);

        for (int i = 0; i < filterClasses.length; i++) {
            String fClass = filterClasses[i];
            String[] fArgs = filterArgs[i].split(";");

            try {
                FrontierReportFilter filter =
                    (FrontierReportFilter) Class.forName(fClass).newInstance();
                filter.init(fArgs);
                filters.add(filter);
            } catch (InstantiationException e) {
                LOG.error("Failed to instantiate filter of class "
                        + fClass, e);
            } catch (IllegalAccessException e) {
                LOG.error("Failed to instantiate filter of class "
                        + fClass, e);
            } catch (ClassNotFoundException e) {
                LOG.error("Failed to instantiate filter of class "
                        + fClass, e);
            }
        }
    }

    /**
     * The filters to apply to the full report, as defined in the settings.
     *  @see HarvesterSettings#FRONTIER_REPORT_FILTER_CLASS
     *  @see HarvesterSettings#FRONTIER_REPORT_FILTER_ARGS
     */
    private List<FrontierReportFilter> filters =
        new LinkedList<FrontierReportFilter>();

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        long elapsed = startTime - lastExecTime;
        LOG.info("Will generate full Heritrix frontier report, "
                + StringUtils.formatDuration(
                        elapsed / TimeUtils.SECOND_IN_MILLIS)
                + " elapsed since last generation started.");
        FullFrontierReport ffr = null;
        try {
            ffr = heritrixController.getFullFrontierReport();
        } catch (HarvestingAbort e) {
            LOG.debug("Unable to retrieve full frontier-reports from Heritrix", 
                    e);
            return;
        }
        long endTime = System.currentTimeMillis();
        elapsed = endTime - startTime;
        LOG.info("Generated full Heritrix frontier report in "
                + (elapsed < TimeUtils.SECOND_IN_MILLIS ? elapsed + " ms"
                        : StringUtils.formatDuration(
                            elapsed / TimeUtils.SECOND_IN_MILLIS))
                + ".");

        lastExecTime = endTime;

        for (FrontierReportFilter filter : filters) {
            startTime = System.currentTimeMillis();
            InMemoryFrontierReport filtered = filter.process(ffr);
            endTime = System.currentTimeMillis();
            elapsed = endTime - startTime;
            LOG.info("Applied filter " + filter.getClass().getName()
                    + " to full frontier report, this took "
                    + (elapsed < TimeUtils.SECOND_IN_MILLIS ? elapsed + " ms"
                            : StringUtils.formatDuration(
                                elapsed / TimeUtils.SECOND_IN_MILLIS))
                    + ".");

            JMSConnectionFactory.getInstance().send(
                    new FrontierReportMessage(filter, filtered));
        }

        ffr.dispose();
    }

}

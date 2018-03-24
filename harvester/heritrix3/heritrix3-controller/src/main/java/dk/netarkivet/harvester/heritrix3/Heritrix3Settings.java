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
package dk.netarkivet.harvester.heritrix3;

import dk.netarkivet.common.utils.Settings;

/**
 * Settings specific to the heritrix3 harvester module of NetarchiveSuite.
 */
public class Heritrix3Settings {

	/** The default place in classpath where the settings file can be found. */
    private static final String DEFAULT_SETTINGS_CLASSPATH = "dk/netarkivet/harvester/heritrix3/settings.xml";

    /*
     * The static initialiser is called when the class is loaded. It will add default values for all settings defined in
     * this class, by loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(DEFAULT_SETTINGS_CLASSPATH);
    }

    // NOTE: The constants defining setting names below are left non-final on
    // purpose! Otherwise, the static initialiser that loads default values
    // will not run.

//    /**
//     * <b>settings.harvester.harvesting.heritrix.crawlLoopWaitTime</b>:<br>
//     * Time interval in seconds to wait during a crawl loop in the harvest controller. Default value is 20 seconds.
//     * 
//     * TODO Maybe move this from the heritrix settings (settings.harvester.harvesting.heritrix) to 
//     * settings.harvester.harvesting.controller.  
//     * 
//     */
    public static String CRAWL_LOOP_WAIT_TIME = "settings.harvester.harvesting.heritrix.crawlLoopWaitTime";

    //
//    /**
//     * <b>settings.harvester.harvesting.frontier.frontierReportWaitTime</b>:<br>
//     * Time interval in seconds to wait between two requests to generate a full frontier report. Default value is 600
//     * seconds (10 min).
//     */
    public static String FRONTIER_REPORT_WAIT_TIME = "settings.harvester.harvesting.frontier.frontierReportWaitTime";
//
//    /**
//     * <b>settings.harvester.harvesting.frontier.filter.class</b> Defines a filter to apply to the full frontier report.
//     * the default class: {@link TopTotalEnqueuesFilter}
//     */
//    public static String FRONTIER_REPORT_FILTER_CLASS = "settings.harvester.harvesting.frontier.filter.class";
//
//    /**
//     * <b>settings.harvester.harvesting.frontier.filter.args</b> Defines a frontier report filter's arguments. Arguments
//     * should be separated by semicolons.
//     */
//    public static String FRONTIER_REPORT_FILTER_ARGS = "settings.harvester.harvesting.frontier.filter.args";
//
//    /**
//     * <b>settings.harvester.harvesting.heritrix.abortIfConnectionLost</b>:<br>
//     * Boolean flag. If set to true, the harvest controller will abort the current crawl when the JMX connection is
//     * lost. If set to true it will only log a warning, leaving the crawl operator shutting down harvester manually.
//     * Default value is true.
//     *
//     * @see BnfHeritrixController
//     */
//    public static String ABORT_IF_CONNECTION_LOST = "settings.harvester.harvesting.heritrix.abortIfConnectionLost";
//
//    /**
//     * <b>settings.harvester.harvesting.heritrix.waitForReportGenerationTimeout</b>:<br>
//     * Maximum time in seconds to wait for Heritrix to generate report files once crawling is over.
//     */
//    public static String WAIT_FOR_REPORT_GENERATION_TIMEOUT = "settings.harvester.harvesting.heritrix.waitForReportGenerationTimeout";
//
//    /**
//     * <b>settings.harvester.harvesting.heritrix.adminName</b>: <br>
//     * The name used to access the Heritrix GUI.
//     */
   public static String HERITRIX_ADMIN_NAME = "settings.harvester.harvesting.heritrix.adminName";
//
//    /**
//     * <b>settings.harvester.harvesting.heritrix.adminPassword</b>: <br>
//     * The password used to access the Heritrix GUI.
//     */
    public static String HERITRIX_ADMIN_PASSWORD = "settings.harvester.harvesting.heritrix.adminPassword";
//
//    /**
//     * <b>settings.harvester.harvesting.heritrix.guiPort</b>: <br>
//     * Port used to access the Heritrix web user interface. This port must not be used by anything else on the machine.
//     * Note that apart from pausing a job, modifications done directly on Heritrix may cause unexpected breakage.
//     */
   public static String HERITRIX_GUI_PORT = "settings.harvester.harvesting.heritrix.guiPort";

//    /**
//     * <b>settings.harvester.harvesting.heritrix.heapSize</b>: <br>
//     * The heap size to use for the Heritrix sub-process. This should probably be fairly large. It can be specified in
//     * the same way as for the -Xmx argument to Java, e.g. 512M, 2G etc.
//     */
    public static String HERITRIX_HEAP_SIZE = "settings.harvester.harvesting.heritrix.heapSize";
//
//    /**
//     * <b>settings.harvester.harvesting.heritrix.javaOpts</b>: <br>
//     * Additional JVM options for the Heritrix sub-process. By default there is no additional JVM option.
//     */
    public static String HERITRIX_JVM_OPTS = "settings.harvester.harvesting.heritrix.javaOpts";
//
//    /**
//     * <b>settings.harvester.harvesting.heritrixControllerClass</b>:<br/>
//     * The implementation of the HeritrixController interface to be used.
//     */
    public static String HERITRIX_CONTROLLER_CLASS = "settings.harvester.harvesting.heritrixController.class";
//
//    /**
//     * <b>settings.harvester.harvesting.heritrixLauncherClass</b>:<br/>
//     * The implementation of the HeritrixLauncher abstract class to be used.
//     */
    public static String HERITRIX_LAUNCHER_CLASS = "settings.harvester.harvesting.heritrixLauncher.class";
//
//    /**
//     * <b>settings.harvester.harvesting.harvestReport</b>:<br/>
//     * The implementation of {@link HarvestReport} interface to be used.
//     */
    public static String HARVEST_REPORT_CLASS = "settings.harvester.harvesting.harvestReport.class";
//
//    /**
//     * <b>settings.harvester.harvesting.harvestReport.disregardSeedsURLInfo</b>:<br/>
//     * Should we disregard seedURL-information and thus assign the harvested bytes to the domain of the harvested URL
//     * instead of the seed url domain? The default is false;
//     */
    public static String DISREGARD_SEEDURL_INFORMATION_IN_CRAWLLOG = "settings.harvester.harvesting.harvestReport.disregardSeedURLInfo";

    /**
     * <b>settings.harvester.harvesting.metadata.generateArchiveFilesReport</b> This setting is a boolean flag that
     * enables/disables the generation of an ARC/WARC files report. Default value is 'true'.
     *
     * @see HarvestDocumentation#documentHarvest(dk.netarkivet.harvester.harvesting.IngestableFiles)
     */
    public static String METADATA_GENERATE_ARCHIVE_FILES_REPORT = "settings.harvester.harvesting.metadata.archiveFilesReport.generate";

    /**
     * <b>settings.harvester.harvesting.metadata.archiveFilesReportName</b> If
     * {@link #METADATA_GENERATE_ARCHIVE_FILES_REPORT} is set to true, sets the name of the generated report file.
     * Default value is 'archivefiles-report.txt'.
     *
     * @see HarvestDocumentation#documentHarvest(dk.netarkivet.harvester.harvesting.IngestableFiles)
     */
    public static String METADATA_ARCHIVE_FILES_REPORT_NAME = "settings.harvester.harvesting.metadata.archiveFilesReport.fileName";

    /**
     * <b>settings.harvester.harvesting.metadata.archiveFilesReportName</b> If
     * {@link #METADATA_GENERATE_ARCHIVE_FILES_REPORT} is set to true, sets the header of the generated report file.
     * This setting should generally be left to its default value, which is '[ARCHIVEFILE] [Closed] [Size]'.
     *
     * @see HarvestDocumentation#documentHarvest(dk.netarkivet.harvester.harvesting.IngestableFiles)
     */
    public static String METADATA_ARCHIVE_FILES_REPORT_HEADER = "settings.harvester.harvesting.metadata.archiveFilesReport.fileHeader";
}

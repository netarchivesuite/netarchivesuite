/*
 * #%L
 * Netarchivesuite - deploy - test
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
package dk.netarkivet.standalone;

import dk.netarkivet.archive.arcrepository.ArcRepository;
import dk.netarkivet.archive.bitarchive.distribute.BitarchiveMonitorServer;
import dk.netarkivet.archive.bitarchive.distribute.BitarchiveServer;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.utils.ApplicationUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.webinterface.GUIWebServer;

/**
 * yet another test application. This application starts an arcrepository, bitarchive, bitarchivemonitor,
 * harvestdefinitionGUI within the same JVM. TestRemoteFile and TestJMSConnection are used, the application therefore
 * does not need access to a JMSBroker nor an FTPServer.
 * <p>
 * Based on StandaloneApplication Primarily a tool to test the GUI
 */

public class StandaloneApplicationReduced {
    /**
     * Runs the ArcRepository Application. Settings are read from config files
     *
     * @param args an empty array
     */
    public static void main(String[] args) {
        Settings.set(CommonSettings.REMOTE_FILE_CLASS, "dk.netarkivet.common.distribute.TestRemoteFile");
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();

        // The Monitor does not work because it disables the monitorLogHandler
        // in order to avoid recursion.
        // ApplicationUtils.startApp(MonitorServer.class, args);

        ApplicationUtils.startApp(ArcRepository.class, args);
        ApplicationUtils.startApp(GUIWebServer.class, args);
        ApplicationUtils.startApp(BitarchiveServer.class, args);
        ApplicationUtils.startApp(BitarchiveMonitorServer.class, args);
    }

}

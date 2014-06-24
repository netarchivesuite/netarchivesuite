package dk.netarkivet.standalone;

import dk.netarkivet.archive.arcrepository.ArcRepository;
import dk.netarkivet.archive.bitarchive.distribute.BitarchiveMonitorServer;
import dk.netarkivet.archive.bitarchive.distribute.BitarchiveServer;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.utils.ApplicationUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.webinterface.GUIWebServer;
import dk.netarkivet.harvester.harvesting.distribute.HarvestControllerServer;
import dk.netarkivet.harvester.indexserver.IndexServer;
import dk.netarkivet.viewerproxy.ViewerProxy;

/**
 * yet another test application.
This application starts an arcrepository, bitarchive, bitarchivemonitor,
harvestdefinitionGUI and harvestserver within the same JVM.
TestRemoteFile and TestJMSConnection are used,
the application  therefore does not need access
to a JMSBroker nor an FTPServer.
Notice that it is possible to complete exactly one harvest-job,
because the harvestServer kills the application once
it has completed the harvest job.
 *
 */
public class StandaloneApplication {
    /**
     * Runs the ArcRepository Application. Settings are read from
     * config files
     *
     * @param args an empty array
     */
    public static void main(String[] args) {
        Settings.set(CommonSettings.REMOTE_FILE_CLASS,
                     "dk.netarkivet.common.distribute.TestRemoteFile");
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        Settings.set(CommonSettings.REPLICA_IDS, Settings.get(
                                   CommonSettings.USE_REPLICA_ID));
        //???Settings.set(CommonSettings.ENVIRONMENT_USE_REPLICA_ID, Settings.get(
        //                CommonSettings.ENVIRONMENT_THIS_REPLICA_ID));


        ApplicationUtils.startApp(BitarchiveMonitorServer.class, args);
        ApplicationUtils.startApp(GUIWebServer.class, args);
        ApplicationUtils.startApp(BitarchiveServer.class, args);
        ApplicationUtils.startApp(ArcRepository.class, args);
        ApplicationUtils.startApp(IndexServer.class, args);

        Settings.set(CommonSettings.HTTP_PORT_NUMBER, "8081");
        ApplicationUtils.startApp(ViewerProxy.class, args);

        // HarvesterControllerServer started as the last app
        // since it seems to block
        ApplicationUtils.startApp(HarvestControllerServer.class, args);
    }

}

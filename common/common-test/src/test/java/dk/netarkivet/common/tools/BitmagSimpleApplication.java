package dk.netarkivet.common.tools;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import dk.netarkivet.common.distribute.bitrepository.BitmagUtils;
import dk.netarkivet.common.distribute.bitrepository.action.ClientAction;
import dk.netarkivet.common.distribute.bitrepository.action.getfile.GetFileAction;
import dk.netarkivet.common.distribute.bitrepository.action.getfileids.GetFileIDsAction;
import dk.netarkivet.common.distribute.bitrepository.action.putfile.PutFileAction;

/**
 * Simple application for testing bitmag client functionality
 */
public class BitmagSimpleApplication {
    public static void main(String[] args) {
        String collectionID;
        String pillarID;
        String fileID;
        File targetFile;

        BitmagUtils.initialize();
        ClientAction clientAction = null;
        try {
            String action = args[0];
            switch (action) {
            case "getfile":
                collectionID = args[1];
                fileID = args[2];
                targetFile = new File(args[3]);
                clientAction = new GetFileAction(BitmagUtils.getFileClient(), collectionID, fileID, targetFile);
                break;
            case "getfileids":
                collectionID = args[1];
                pillarID = args[2];
                clientAction = new GetFileIDsAction(BitmagUtils.getFileIDsClient(), collectionID, pillarID,
                        new Date(0));
                break;
            case "putfile":
                collectionID = args[1];
                targetFile = new File(args[2]);
                fileID = args[3];
                clientAction = new PutFileAction(BitmagUtils.getPutFileClient(), collectionID, targetFile, fileID);
                break;
            case "help":
                System.out.println("Valid actions:");
                System.out.println("getfile <collectionID> <fileID> <targetFile>");
                System.out.println("getfileids <collectionID> <pillarID>");
                System.out.println("putfile <collectionID> <targetFile> <fileID>");
                break;
            default:
                throw new RuntimeException("Unknown action: '" + action + "'");
            }
            clientAction.performAction();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }

        if (clientAction instanceof GetFileIDsAction) {
            Set<String> res = ((GetFileIDsAction) clientAction).getActionResult();
            res.forEach(System.out::println);
        }
        System.exit(0);
    }
}

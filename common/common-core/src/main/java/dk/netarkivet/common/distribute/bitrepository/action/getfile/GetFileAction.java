package dk.netarkivet.common.distribute.bitrepository.action.getfile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

import org.bitrepository.access.getfile.GetFileClient;
import org.bitrepository.protocol.FileExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.bitrepository.BitmagUtils;
import dk.netarkivet.common.distribute.bitrepository.action.ClientAction;

/**
 * Action class to get files from Bitmag.
 */
public class GetFileAction implements ClientAction {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final File targetFile;
    private final GetFileClient client;
    private final String collectionID;
    private final String fileID;
    private boolean succeeded;
    private String info;

    /**
     * Constructor to instantiate the get-file action
     * @param client The client to perform the action on
     * @param collectionID The ID of a known collection to operate on
     * @param fileID The ID of an existing file in the collection to download
     * @param targetFile A File specifying a path to download the file to
     */
    public GetFileAction(GetFileClient client, String collectionID, String fileID, File targetFile) {
        this.client = client;
        this.collectionID = collectionID;
        this.fileID = fileID;
        this.targetFile = targetFile;
    }

    public boolean actionIsSuccess() {
        return succeeded;
    }

    public String getInfo() {
        return info;
    }

    @Override
    public void performAction() {
        if (targetFile.exists()) {
            log.error("Output file '{}' already exists.", targetFile);
            succeeded = false;
            info = "Output file " + targetFile + " already exists.";
            return;
        }

        FileExchange fileExchange = BitmagUtils.getFileExchange();
        GetFileEventHandler eventHandler = new GetFileEventHandler();
        try {
            URL url = new URL(BitmagUtils.getFileExchangeBaseURL().toExternalForm() + UUID.randomUUID().toString());
            client.getFileFromFastestPillar(collectionID, fileID, null, url, eventHandler,
                    "GetFile from NetarchiveSuite");

            eventHandler.waitForFinish();

            succeeded = !eventHandler.hasFailed();
            if (succeeded) {
                log.info("Retrieving {} from {}.", fileID, url.toExternalForm());
                fileExchange.getFile(targetFile, url.toExternalForm());
                try {
                    log.debug("Deleting {}.", url.toExternalForm());
                    fileExchange.deleteFile(url);
                } catch (IOException | URISyntaxException e) {
                    log.error("Failed cleaning up after file '{}' because of {}.", fileID, e.getMessage());
                }
            } else {
                info = eventHandler.getInfo();
                log.error("Failed to get file '{}'", fileID);
            }
        } catch (InterruptedException e) {
            succeeded = false;
            info = "Got an InterruptedException in GetFileAction.";
            log.error("Got interrupted while waiting for operation to complete.");
        } catch (MalformedURLException e) {
            succeeded = false;
            String message = "Got a malformed URL exception while retrieving " + fileID +
                    e.getMessage();
            info = message;
            log.error(message);
        }
    }
}

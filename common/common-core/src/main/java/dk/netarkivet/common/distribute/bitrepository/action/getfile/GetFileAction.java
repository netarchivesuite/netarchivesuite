package dk.netarkivet.common.distribute.bitrepository.action.getfile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

import org.bitrepository.access.getfile.GetFileClient;
import org.bitrepository.protocol.FileExchange;
import dk.netarkivet.common.distribute.bitrepository.BitmagUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public GetFileAction(GetFileClient client, String collectionID, String fileID, File targetFile) {
        this.client = client;
        this.collectionID = collectionID;
        this.fileID = fileID;
        this.targetFile = targetFile;
    }

    @Override
    public void performAction() {
        if (targetFile.exists()) {
            log.error("Output file '{}' already exists.", targetFile);
            return;
        }

        FileExchange fileExchange = BitmagUtils.getFileExchange();
        GetFileEventHandler eventHandler = new GetFileEventHandler();
        try {
            URL url = new URL(BitmagUtils.getFileExchangeBaseURL().toExternalForm() + UUID.randomUUID().toString());
            client.getFileFromFastestPillar(collectionID, fileID, null, url, eventHandler,
                    "GetFile from NAS");

            eventHandler.waitForFinish();

            boolean actionIsSuccess = !eventHandler.hasFailed();
            if (actionIsSuccess) {
                fileExchange.getFile(targetFile, url.toExternalForm());
                try {
                    fileExchange.deleteFile(url);
                } catch (IOException | URISyntaxException e) {
                    log.error("Failed cleaning up after file '{}'", fileID);
                }
            } else {
                log.error("Failed to get file '{}'", fileID);
            }
        } catch (InterruptedException e) {
            log.error("Got interrupted while waiting for operation to complete");
        } catch (MalformedURLException e) {
            log.error("Got malformed URL while trying to get file '{}'", fileID);
        }
    }
}

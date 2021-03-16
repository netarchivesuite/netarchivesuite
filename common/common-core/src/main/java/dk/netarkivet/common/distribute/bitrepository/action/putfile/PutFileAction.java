package dk.netarkivet.common.distribute.bitrepository.action.putfile;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import org.bitrepository.bitrepositoryelements.ChecksumDataForFileTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumType;
import org.bitrepository.common.utils.SettingsUtils;
import org.bitrepository.modify.putfile.PutFileClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.bitrepository.BitmagUtils;
import dk.netarkivet.common.distribute.bitrepository.action.ClientAction;

/**
 * Action class to put files to Bitmag.
 */
public class PutFileAction implements ClientAction {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String collectionID;
    private final String fileID;
    private final PutFileClient client;
    private final File targetFile;
    private boolean success;

    /**
     * Constructor to instantiate the put-file action
     * @param client The client to perform the action on
     * @param collectionID The ID of a known collection to put the file in
     * @param targetFile The File to put in the bitrepository
     * @param fileID ID for the targetFile to have in the bitrepository once it's been uploaded
     */
    public PutFileAction(PutFileClient client, String collectionID, File targetFile, String fileID) {
        this.client = client;
        this.collectionID = collectionID;
        this.targetFile = targetFile;
        this.fileID = fileID;
    }
    public boolean actionIsSuccess() {
        return success;
    }

    @Override
    public void performAction() {
        try {
            URL url = new URL(BitmagUtils.getFileExchangeBaseURL().toExternalForm() + UUID.randomUUID().toString());
            List<String> pillars = SettingsUtils.getPillarIDsForCollection(collectionID);
            PutFileEventHandler eventHandler = new PutFileEventHandler(pillars, targetFile, url);

            ChecksumDataForFileTYPE checksumData = BitmagUtils.getValidationChecksum(targetFile,
                    BitmagUtils.getChecksumSpec(ChecksumType.MD5));
            client.putFile(collectionID, url, fileID, targetFile.length(), checksumData, null,
                    eventHandler, "PutFile from NAS");
            eventHandler.waitForFinish();

            success = !eventHandler.hasFailed();
            if (success) {
                log.info("Put operation was a success! Put file '{}' to bitmag with id: '{}'.",
                        targetFile.getName(), fileID);
            } else {
                log.warn("Failed put operation for file '{}'.", targetFile.getName());
            }
        } catch (MalformedURLException e) {
            log.error("Got malformed URL while trying to get file '{}'", fileID);
        } catch (InterruptedException e) {
            log.error("Got interrupted while waiting for operation to complete");
        }
    }
}

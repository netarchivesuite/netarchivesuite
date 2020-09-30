package dk.netarkivet.common.distribute.bitrepository.action.putfile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.bitrepository.bitrepositoryelements.ChecksumDataForFileTYPE;
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

    public PutFileAction(PutFileClient client, String collectionID, File targetFile, String fileID) {
        this.collectionID = collectionID;
        this.targetFile = targetFile;
        this.fileID = fileID;
        this.client = client;
    }

    @Override
    public void performAction() {
        try {
            URL url = new URL(BitmagUtils.getFileExchangeBaseURL().toExternalForm() + UUID.randomUUID().toString());
            PutFileEventHandler eventHandler = new PutFileEventHandler(targetFile, url);

            ChecksumDataForFileTYPE checksum = BitmagUtils.getChecksum(generateChecksum(targetFile));

            client.putFile(collectionID, url, fileID, targetFile.length(), checksum, null, eventHandler, "Training client put test");
            eventHandler.waitForFinish();

            boolean actionIsSuccess = !eventHandler.hasFailed();
            if (actionIsSuccess) {
                log.info("Put operation was a success! Put file '{}' to bitmag with id: '{}'.", targetFile.getName(), fileID);
            } else {
                log.warn("Failed put operation for file '{}'.", targetFile.getName());
            }
        } catch (MalformedURLException e) {
            log.error("Got malformed URL while trying to get file '{}'", fileID);
        } catch (InterruptedException e) {
            log.error("Got interrupted while waiting for operation to complete");
        }
    }

    /** TODO seems that Bitmags ChecksumUtils can be used for this - try a small test run
     * Helper method for generating an md5-checksum from a file.
     * @param file The file to generate the checksum for.
     * @return A checksum string.
     */
    private String generateChecksum(File file) {
        String checksum = null;
        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            checksum = DigestUtils.md5Hex(is);
        } catch (IOException e) {
            log.error("Failed generating checksum for file '{}'", file.getAbsolutePath());
        }
        return checksum;
    }
}

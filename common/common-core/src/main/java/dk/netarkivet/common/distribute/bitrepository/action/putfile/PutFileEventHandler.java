package dk.netarkivet.common.distribute.bitrepository.action.putfile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

import org.bitrepository.client.eventhandler.EventHandler;
import org.bitrepository.client.eventhandler.IdentificationCompleteEvent;
import org.bitrepository.client.eventhandler.OperationEvent;
import org.bitrepository.protocol.FileExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.bitrepository.BitmagUtils;

public class PutFileEventHandler implements EventHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final FileExchange fileExchange = BitmagUtils.getFileExchange();
    private final File targetFile;
    private final URL uploadURL;

    private final Object finishLock = new Object();
    private boolean finished = false;
    private boolean failed = false;

    /**
     * Constructor
     * @param targetFile The file to put in the bitrepository
     * @param uploadURL The url where the file is uploaded to if the action is successful
     */
    public PutFileEventHandler(File targetFile, URL uploadURL) {
        this.targetFile = targetFile;
        this.uploadURL = uploadURL;
    }

    @Override
    public void handleEvent(OperationEvent event) {
        log.info("Got event from client: {}", event.getEventType());
        switch (event.getEventType()) {
            case IDENTIFICATION_COMPLETE:
                IdentificationCompleteEvent completeEvent = (IdentificationCompleteEvent) event;
                if (completeEvent.getContributorIDs().size() > 0) {
                    uploadToFileExchange();
                }
                break;
            case COMPLETE:
                log.info("Finished put fileID for file '{}'", event.getFileID());
                cleanUpFileExchange();
                finish();
                break;
            case FAILED:
                log.info("Failed put fileID for file '{}'", event.getFileID());
                failed = true;
                cleanUpFileExchange();
                finish();
                break;
            default:
                break;
        }
    }

    private void uploadToFileExchange() {
        try (InputStream is = new BufferedInputStream(new FileInputStream(targetFile), 16384)) {
            fileExchange.putFile(is, uploadURL);
            log.debug("Finished uploading file '{}' to file exchange", targetFile.getName());
        } catch (IOException e) {
            log.error("Failed to upload file '{}' to file exchange", targetFile.getName());
        }
    }

    private void cleanUpFileExchange() {
        try {
            fileExchange.deleteFile(uploadURL);
            log.debug("Finished cleaning up '{}' at URL: '{}'..", targetFile.getName(), uploadURL.toExternalForm());
        } catch (IOException | URISyntaxException e) {
            log.error("Failed cleaning up '{}' at URL: '{}'..", targetFile.getName(), uploadURL.toExternalForm());
        }
    }

    /**
     * Method to indicate the operation have finished (regardless if is successful or not).
     */
    private void finish() {
        log.trace("Finish method invoked");
        synchronized (finishLock) {
            log.trace("Finish method entered synchronized block");
            finished = true;
            finishLock.notifyAll();
            log.trace("Finish method notified All");
        }
    }

    /**
     * Method to wait for the operation to complete. The method is blocking.
     * @throws InterruptedException if the thread is interrupted
     */
    public void waitForFinish() throws InterruptedException {
        synchronized (finishLock) {
            if(!finished) {
                log.trace("Thread waiting for client to finish");
                finishLock.wait();
            }
            log.trace("Client have indicated it's finished.");
        }
    }

    /**
     * Method to determine if the operation was successful.
     * The method should not be called prior to a call to {@link #waitForFinish()} have returned.
     * @return true if the operation succeeded, otherwise false.
     */
    public boolean hasFailed() {
        return failed;
    }
}

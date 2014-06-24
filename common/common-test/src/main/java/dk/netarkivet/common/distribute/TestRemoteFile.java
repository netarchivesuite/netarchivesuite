package dk.netarkivet.common.distribute;

import java.io.File;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Version of RemoteFile that reads/writes a file to local storage.
 * <pre>
 * Created by IntelliJ IDEA.
 * User: csr
 * Date: Mar 2, 2005
 * Time: 3:09:26 PM
 * </pre>
 */
@SuppressWarnings({ "serial"})
public class TestRemoteFile extends HTTPRemoteFile implements RemoteFile {
    public boolean failsOnCopy;

    public static Map<RemoteFile,String> remainingRemoteFiles
            = new WeakHashMap<RemoteFile,String>();

    public TestRemoteFile(File localFile, boolean useChecksum,
                           boolean fileDeletable, boolean multipleDownloads)
            throws IOFailure {
        super (localFile, useChecksum, fileDeletable, multipleDownloads);
        remainingRemoteFiles.put(this, localFile.getName());
    }

    public static RemoteFile getInstance(File remoteFile,
                                         Boolean useChecksums,
                                         Boolean fileDeletable,
                                         Boolean multipleDownloads)
            throws IOFailure {
        return new TestRemoteFile(remoteFile, useChecksums, fileDeletable,
                                   multipleDownloads);
    }

    public void copyTo(File destFile) {
        if (failsOnCopy) {
            throw new IOFailure("Expected IO error in copying "
                    + "- you told me so!");
        }
        super.copyTo(destFile);
    }

    public void appendTo(OutputStream out) {
        if (failsOnCopy) {
            throw new IOFailure("Expected IO error in copying "
                    + "- you told me so!");
        }
        super.appendTo(out);
    }

    public void cleanup() {
        remainingRemoteFiles.remove(this);
        super.cleanup();
    }

    public boolean isDeleted() {
        return !remainingRemoteFiles.containsKey(this);
    }

    public String toString() {
        return "TestRemoteFile: '" + file.getPath() + "'";
    }

    /** Remove any remote files that may have been left over. */
    public static void removeRemainingFiles() {
        //Must copy keyset to avoid concurrent modificaion of set
        for (RemoteFile rf :
                new HashSet<RemoteFile>(remainingRemoteFiles.keySet())) {
            rf.cleanup();
        }
        remainingRemoteFiles.clear();
    }

    /** Give the set of remaining remote files.
     * @return the Set of remaining files
     */
    public static Set<RemoteFile> remainingFiles() {
        return remainingRemoteFiles.keySet();
    }

    public File getFile() {
        return file;
    }

    protected boolean isLocal() {
        return true;
    }
}

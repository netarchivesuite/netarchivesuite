package dk.netarkivet.common.distribute;

import dk.netarkivet.common.exceptions.NotImplementedException;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This is an implementation of RemoteFile which does nothing and can
 * therefore be used in batch jobs for which no output is required.
 *
 */
@SuppressWarnings({ "serial"})
public class NullRemoteFile implements RemoteFile {

    /**
     * @see RemoteFileFactory#getInstance(File, boolean, boolean, boolean)
     */
    public static RemoteFile getInstance(File f,
                                         Boolean useChecksums,
                                         Boolean fileDeletable,
                                         Boolean multipleDownloads) {
          return new NullRemoteFile();
    }

    /**
     * @see RemoteFile#copyTo(File)
     */
    public void copyTo(File destFile) {
    }

    /**
     * @see RemoteFile#appendTo(OutputStream)
     */
    public void appendTo(OutputStream out) {
    }

    public InputStream getInputStream() {
        return null;
    }

    /**
     * @see RemoteFile#cleanup()
     */
    public void cleanup() {
    }

    /**
     * @see RemoteFile#getSize()
     */
    public long getSize() {
        return 0;
    }

    /**
     * Return the file name.
     * @return the file name
     * @see RemoteFile#getName()
     */
    public String getName() {
        return null;
    }

    /**
     * Returns a MD5 Checksum on the file.
     * @return MD5 checksum
     * @see RemoteFile#getChecksum()
     * @throws NotImplementedException Because it is not implemented
     */
    public String getChecksum() throws NotImplementedException {
        throw new NotImplementedException("Not implemented!");
    }
}

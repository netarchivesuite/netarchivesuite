package dk.netarkivet.archive.bitarchive;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/** This class attempts to do illegal actions.
 */
public class EvilBatch extends FileBatchJob {
    String location;
    public void initialize(OutputStream os) {
        try {
            os.write("Legal\n".getBytes());
        } catch (IOException e) {
            throw new IOFailure("Failed to write location", e);
        }
    }
    public boolean processFile(File file, OutputStream os) {
        try {
            FileReader reader = new FileReader(file);
            reader.close();
            // Must fail on one file.
            if (file.getName().equals("Upload3.ARC")) {
                FileUtils.readFile(new File("conf/settings.xml"));
            }
            if (file.getName().equals("fyensdk.arc")) {
                FileUtils.writeBinaryFile(file, "smash!".getBytes());
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }
    public void finish(OutputStream os) {
        // Here's where we do something very illegal.
        try {
            FileWriter writer = new FileWriter("evil");
            writer.write("There is no evil\n");
            writer.close();
        } catch (IOException e) {
            throw new IOFailure("Failed to write evil file", e);
        }
    }
}

package dk.netarkivet.testutils;

import dk.netarkivet.common.exceptions.IOFailure;

import org.archive.io.arc.ARCRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ARCTestUtils {
    /**
     * Reads the content of the given record.
     * Does not close the record - that causes trouble.
     * @param ar An ARCRecord to be read
     * @return The content of the record, as a String.
     */
    public static String readARCRecord(ARCRecord ar) {
        StringBuffer sb = new StringBuffer();
        BufferedReader br = new BufferedReader(new InputStreamReader(ar));
        try {
            int i = -1;
            while ((i = br.read()) != -1) {
                sb.append((char) i);
                //ARCRecords dislike being closed
            }
        } catch(IOException e) {
            throw new IOFailure("Failure reading ARCRecord",e);
        }
        return sb.toString();
    }
}

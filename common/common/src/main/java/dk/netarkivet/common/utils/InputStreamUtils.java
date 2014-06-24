package dk.netarkivet.common.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class provides various utilities for inputstreams.
 */
public class InputStreamUtils {
    
    /** Read a line of bytes from an InputStream.  Useful when an InputStream
     * may contain both text and binary data.
     * @param inputStream A source of data
     * @return A line of text read from inputStream, with terminating
     * \r\n or \n removed, or null if no data is available.
     * @throws IOException on trouble reading from input stream
     */
    public static String readLine(InputStream inputStream) throws IOException {
        byte[] rawdata = readRawLine(inputStream);
        if (rawdata == null) {
            return null;
        }
        int len = rawdata.length;
        if (len > 0) {
            if (rawdata[len - 1] == '\n') {
                len--;
                if (len > 0) {
                    if (rawdata[len - 1] == '\r') {
                        len--;
                    }
                }
            }
        }
        return new String(rawdata, 0, len);
    }

    /** Reads a raw line from an InputStream, up till \n.
     * Since HTTP allows \r\n and \n as terminators, this gets the whole line.
     * This code is adapted from org.apache.commons.httpclient.HttpParser
     *
     * @param inputStream A stream to read from.
     * @return Array of bytes read or null if none are available.
     * @throws IOException if the underlying reads fail
     */
    public  static byte[] readRawLine(InputStream inputStream)
        throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int ch;
        while ((ch = inputStream.read()) >= 0) {
            buf.write(ch);
            if (ch == '\n') {
                break;
            }
        }
        if (buf.size() == 0) {
            return null;
        }
        return buf.toByteArray();
    }

}

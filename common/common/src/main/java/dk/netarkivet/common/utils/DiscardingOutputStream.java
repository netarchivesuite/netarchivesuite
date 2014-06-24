package dk.netarkivet.common.utils;

import java.io.OutputStream;

/** An OutputStream implementation that simply discards everything it gets.
 *  It overrides all the write methods so that they all execute in constant
 *  time. */
class DiscardingOutputStream extends OutputStream {
    /** Discard a single byte of data.
     * @see OutputStream#write(int)
     */
    public void write(int i) {
    }

    /** Discard many bytes of data, efficiently.
     * @see OutputStream#write(byte[], int, int)
     */
    public void write(byte[] buffer, int offset, int amount) {
    }

    /** Discard all the data we can, efficiently.
     * @see OutputStream#write(byte[])
     */
    public void write(byte[] buffer) {
    }
}

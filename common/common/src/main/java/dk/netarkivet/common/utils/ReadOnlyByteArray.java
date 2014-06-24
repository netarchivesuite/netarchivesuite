
package dk.netarkivet.common.utils;

/**
 * Implements access to an array in a read-only fashion.
 */

public class ReadOnlyByteArray {
    private byte[] array;
    /** Creates a new instance based on the given array.
     *
     * @param array Array to provide read-only access to.  The array will
     * not be copied by this class.
     */
    public ReadOnlyByteArray(byte[] array) {
        this.array = array;
    }

    /** Returns the length of the array.
     *
     * @return The length of the array.  Always >= 0.
     */
    public int length() {
        return array.length;
    }

    /** Gets the element at the given index.
     *
     * @param index The index to get the element at.
     * @return The byte at the given index.
     * @throws IndexOutOfBoundsException if the index is < 0 or > length()
     */
    public byte get(int index) {
        return array[index];
    }
}

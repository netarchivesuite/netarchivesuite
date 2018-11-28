/*
 * #%L
 * Netarchivesuite - heritrix 3 monitor
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.heritrix3.monitor;

import java.io.IOException;
import java.io.RandomAccessFile;

public class StringIndexFile {

    public static byte[] readPage(RandomAccessFile idxRaf, RandomAccessFile logRaf, long page, long itemsPerPage, boolean descending) throws IOException {
        byte[] bytes = null;;
        if (page < 1) {
            throw new IllegalArgumentException();
        }
        if (itemsPerPage < 25) {
            throw new IllegalArgumentException();
        }
        long length = idxRaf.length();
        if (length > 8) {
            if (!descending) {
                // Forwards.
                long fromIdx = (page - 1) * (itemsPerPage * 8);
                long toIdx = fromIdx + (itemsPerPage * 8);
                if (toIdx > length) {
                    toIdx = length;
                }
                idxRaf.seek(fromIdx);
                fromIdx = idxRaf.readLong();
                idxRaf.seek(toIdx);
                toIdx = idxRaf.readLong();
                logRaf.seek(fromIdx);
                bytes = new byte[(int)(toIdx - fromIdx)];
                logRaf.readFully(bytes, 0, (int)(toIdx - fromIdx));
            } else {
                // Backwards.
                long toIdx = length - ((page - 1) * itemsPerPage * 8);
                long fromIdx = toIdx - (itemsPerPage * 8) - 8;
                if (fromIdx < 0) {
                    fromIdx = 0;
                }
                // Read line indexes for page.
                int pageIdxArrLen = (int)(toIdx - fromIdx);
                byte[] pageIdxArr = new byte[pageIdxArrLen];
                idxRaf.seek(fromIdx);
                int pos = 0;
                int limit = pageIdxArrLen;
                int read = 0;
                while (limit > 0 && read != -1) {
                    read = idxRaf.read(pageIdxArr, pos, limit);
                    if (read != -1) {
                        pos += read;
                        limit -= read;
                    }
                }
                // Convert line indexes for page.
                limit = pos;
                pos = 0;
                long[] idxArr = new long[limit / 8];
                long l;
                int dstIdx = 0;
                while (pos < limit) {
                    l = (pageIdxArr[pos++] & 255) << 56 | (pageIdxArr[pos++] & 255) << 48 | (pageIdxArr[pos++] & 255) << 40 | (pageIdxArr[pos++] & 255) << 32
                            | (pageIdxArr[pos++] & 255) << 24 | (pageIdxArr[pos++] & 255) << 16 | (pageIdxArr[pos++] & 255) << 8 | (pageIdxArr[pos++] & 255);
                    idxArr[dstIdx++] = l;
                }
                // Load the crawllog lines for page.
                pos = 0;
                limit /= 8;
                fromIdx = idxArr[pos];
                toIdx = idxArr[limit - 1];
                logRaf.seek(fromIdx);
                byte[] tmpBytes = new byte[(int)(toIdx - fromIdx)];
                logRaf.readFully(tmpBytes, 0, (int)(toIdx - fromIdx));
                // Reverse crawllog lines for page.
                bytes = new byte[tmpBytes.length];
                long base = idxArr[pos++];
                fromIdx = base;
                int len;
                dstIdx = bytes.length;
                while (pos < limit) {
                    toIdx = idxArr[pos++];
                    len = (int)(toIdx - fromIdx);
                    dstIdx -= len;
                    System.arraycopy(tmpBytes, (int)(fromIdx - base), bytes, dstIdx, len);
                    fromIdx = toIdx;
                }
            }
        }
        return bytes;
    }

}

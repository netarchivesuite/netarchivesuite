/*
 * #%L
 * Netarchivesuite - common - test
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
package dk.netarkivet.testutils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.archive.io.arc.ARCRecord;

import dk.netarkivet.common.exceptions.IOFailure;

public class ARCTestUtils {
    /**
     * Reads the content of the given record. Does not close the record - that causes trouble.
     *
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
                // ARCRecords dislike being closed
            }
        } catch (IOException e) {
            throw new IOFailure("Failure reading ARCRecord", e);
        }
        return sb.toString();
    }
}

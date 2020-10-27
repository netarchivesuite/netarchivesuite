/*
 * #%L
 * Netarchivesuite - common
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
package dk.netarkivet.common.distribute.arcrepository;

import java.io.InputStream;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Simple helper class to store the fact, whether we have a stream which contains a header or a stream, which does not.
 */
public class ResultStream {

    /** The inputstream w/ or without a HTTP header. */
    private final InputStream inputstream;

    /** Does the inputstream contains a HTTP header?. */
    private final boolean containsHeader;

    /**
     * Create a ResultStream with the given inputStream and information of whether or not the inputStream contains a
     * header.
     *
     * @param inputstream An inputStream w/ the data for a stored URI
     * @param containsHeader true, if the stream contains a header, otherwise false
     */
    public ResultStream(InputStream inputstream, boolean containsHeader) {
        ArgumentNotValid.checkNotNull(inputstream, "InputStream inputstream");
        this.inputstream = inputstream;
        this.containsHeader = containsHeader;
    }

    /**
     * @return the inputstream
     */
    public InputStream getInputStream() {
        return this.inputstream;
    }

    /**
     * @return true, if the resultStream contains a header; otherwise false.
     */
    public boolean containsHeader() {
        return this.containsHeader;
    }

}

/*
 * #%L
 * Netarchivesuite - harvester
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
package dk.netarkivet.viewerproxy;

import java.io.OutputStream;

/**
 * The Response interface is a very minimal version of a HTTP response. We use this to decouple the main parts of the
 * proxy server from a given implementation.
 */
public interface Response {

    /**
     * Return outputstream response should be written to.
     *
     * @return the outputstream response should be written to
     */
    OutputStream getOutputStream();

    /**
     * Set status code.
     *
     * @param statusCode should be valid http status ie. 200, 404,
     */
    void setStatus(int statusCode);

    /**
     * Set status code. and explanatory text string describing the status.
     *
     * @param statusCode should be valid http status ie. 200, 404,
     * @param reason text string explaining status ie. OK, not found,
     */
    void setStatus(int statusCode, String reason);

    /**
     * Add an HTTP header to the response.
     *
     * @param name Name of the header, e.g. Last-Modified-Date
     * @param value The value of the header
     */
    void addHeaderField(String name, String value);

    /**
     * Get the status code from this response.
     *
     * @return The statuscode.
     */
    int getStatus();

}

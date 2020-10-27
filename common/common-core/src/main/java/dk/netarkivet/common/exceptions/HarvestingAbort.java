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
package dk.netarkivet.common.exceptions;

/**
 * This exception is used to signal that harvest is aborted.
 */
@SuppressWarnings({"serial"})
public class HarvestingAbort extends NetarkivetException {

    /**
     * Create a new HarvestAbort exception based on an old exception.
     *
     * @param message Explanatory message
     */
    public HarvestingAbort(String message) {
        super(message);
    }

    /**
     * Create a new HarvestAbort exception based on an old exception.
     *
     * @param message Explanatory message
     * @param cause The exception that prompted the exception
     */
    public HarvestingAbort(String message, Throwable cause) {
        super(message, cause);
    }
}

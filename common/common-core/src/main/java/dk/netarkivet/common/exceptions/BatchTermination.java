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
 * Exception to tell running batchjobs to terminate.
 */
@SuppressWarnings({"serial"})
public class BatchTermination extends NetarkivetException {
    /**
     * Constructs new BatchTermination exception with the given message.
     *
     * @param message The exception message.
     */
    public BatchTermination(String message) {
        super(message);
    }

    /**
     * Constructs new BatchTermination exception with the given message and cause.
     *
     * @param message The exception message.
     * @param cause The cause of the exception.
     */
    public BatchTermination(String message, Throwable cause) {
        super(message, cause);
    }
}

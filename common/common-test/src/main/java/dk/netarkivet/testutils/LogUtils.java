/*
 * #%L
 * Netarchivesuite - common - test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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

import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * Utilities pertaining to testing of logs.
 */
public class LogUtils {

    /**
     * Flush all logs for the given class.
     * 
     * @param className Class to flush logs for.
     */
    public static void flushLogs(final String className) {
        // Make sure that all logs are flushed before testing.
        Logger controllerLogger = Logger.getLogger(className);
        Handler[] handlers = controllerLogger.getHandlers();
        for (int i = 0; i < handlers.length; i++) {
            handlers[i].flush();
        }
    }

}

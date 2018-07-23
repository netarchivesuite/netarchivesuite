/*
 * #%L
 * NetarchiveSuite System test
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
package dk.netarkivet.systemtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*dk.netarkivet.systemtestger should be used by all the test code to enable separation of test
 * logs from the applications logs.
 */
public class TestLogger {
    private final Logger log;

    public TestLogger(Class<?> logHandle) {
        log = LoggerFactory.getLogger(logHandle);
    }

    public void error(String msg) {
        log.error(msg);
    }

    public void debug(String string) {
        log.debug(string);
    }

    public void warn(String msg) {
        log.warn(msg);
    }

    public void warn(java.lang.String s, java.lang.Object... objects){
        log.warn(s, objects);
    }

    public void info(String msg) {
        log.info(msg);
    }

    public void debug(StringBuffer sb) {
        log.debug(sb + "");
    }

    public void debug(java.lang.String s, java.lang.Object... objects){
        log.debug(s, objects);
    }

    public void info(java.lang.String s, java.lang.Object... objects){
        log.info(s, objects);
    }
}

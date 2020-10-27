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
package dk.netarkivet.testutils.preconfigured;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import dk.netarkivet.common.exceptions.PermissionDenied;

public class PreserveStdStreams implements TestConfigurationIF {
    private InputStream origStdIn;
    private PrintStream origStdOut;
    private PrintStream origStdErr;
    private ByteArrayOutputStream myOut;
    private ByteArrayOutputStream myErr;
    private boolean overwrite;

    public PreserveStdStreams(boolean andOverwrite) {
        overwrite = andOverwrite;
    }

    public PreserveStdStreams() {
        this(false);
    }

    public void setUp() {
        origStdIn = System.in;
        origStdOut = System.out;
        origStdErr = System.err;
        if (overwrite) {
            myOut = new ByteArrayOutputStream();
            System.setOut(new PrintStream(myOut));
        }
        if (overwrite) {
            myErr = new ByteArrayOutputStream();
            System.setErr(new PrintStream(myErr));
        }
    }

    public void tearDown() {
        System.setIn(origStdIn);
        System.setOut(origStdOut);
        System.setErr(origStdErr);
    }

    public String getOut() {
        if (overwrite) {
            return myOut.toString();
        }
        throw new PermissionDenied("Set overwrite to true to use this facility");
    }

    public String getErr() {
        if (overwrite) {
            return myErr.toString();
        }
        throw new PermissionDenied("Set overwrite to true to use this facility");
    }
}

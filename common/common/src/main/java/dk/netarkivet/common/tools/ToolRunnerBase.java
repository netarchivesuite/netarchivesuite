/* $Id$
 * $Date$
 * $Revision$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package dk.netarkivet.common.tools;

import dk.netarkivet.common.utils.ExceptionUtils;

/**
 * A simple class that manages and runs an implementation of SimpleCmdlineTool.
 * The class contains an abstract factory method, which will create the actual
 * implementation when specialized. This could also have been done with
 * generics, but this is the traditional implementation.
 *
 *
 */
public abstract class ToolRunnerBase {

    /**
     * Factory method. Creates and returns the intended specific implementation
     * of a command line tool.
     *
     * @return An implementation of the SimpleCmdlineTool interface.
     */
    protected abstract SimpleCmdlineTool makeMyTool();

    /**
     * Consolidates that behavior on error is System.exit(1)
     * (exit with failure).
     */
    private void exitWithFailure() {
        System.exit(1);
    }

    /**
     * A template method implementing default behaviour for showing a message
     * (send to stderr). Can be overridden to ensure logging.
     * @param msg The message to display
     */
    protected void showMessage(String msg) {
        System.err.println(msg);
    }

    /**
     * Passes (command line) parameters to the tool. If an error occured
     * (internalRunTool returned false), exit with failure.
     *
     * @param args Usually a straight passing of the command line parameters
     * from a "main" method.
     */
    public void runTheTool(String... args) {
        if (!internalRunTheTool(args)) {
            exitWithFailure();
        }
        System.exit(0);
    }

    // Private methods follow for doing setup, run and teardown with rudimentary
    // handling of exceptions. Split up in several methods for clarity.

    private boolean runIt(SimpleCmdlineTool tool, String... args) {
        try {
            tool.run(args);
        } catch (Exception e) {
            exceptionMessage("An error occurred during processing.", e);
            return false;
        }
        return true;
    }
    private boolean setupAndRunIt(SimpleCmdlineTool tool, String... args) {
        try {
            tool.setUp(args);
            runIt(tool, args);
            return true;
        } catch (Exception e) {
            exceptionMessage("An error occurred during setup of tool.", e);
            return false;
        } finally {
            tool.tearDown();
        }
    }
    private boolean checkArgsSetupAndRun(SimpleCmdlineTool tool,
            String... args) {
        try {
            if (!tool.checkArgs(args)) {
                usage(tool);
                return false;
            }
            return setupAndRunIt(tool, args);
        } catch (Exception e) {
            exceptionMessage("Couldn't check parameters for tool.", e);
            return false;
        }
    }
    private boolean internalRunTheTool(String... args) {
        try {
            SimpleCmdlineTool tool = makeMyTool();
            return checkArgsSetupAndRun(tool, args);
        } catch (Exception e) {
            exceptionMessage("Couldn't create the tool to run!", e);
            return false;
        }
    }

    /**
     * Prints usage, delegating the actual parameter description to the tool.
     *
     * @param tool The tool that we have specialized this class to. Know how
     * to run and how to describe itself.
     */
    private void usage(SimpleCmdlineTool tool) {
        showMessage("Usage: java " + this.getClass().getName() + " "
                + tool.listParameters());
        exitWithFailure();
    }

    /**
     * Centralized, rudimentary exception handling. Only option is to print a
     * message, and a stack trace.
     *
     * @param msg A message from the tool manager (this class), notifying the
     * user about where the exception occurred.
     * @param e The exception that occurred.
     */
    private void exceptionMessage(String msg, Exception e) {
        showMessage(msg);
        showMessage("Output (if any) is not OK");
        showMessage("Exception message is:");
        showMessage(e.getMessage());
        showMessage("Stack trace:");
        showMessage(ExceptionUtils.getStackTrace(e));
        exitWithFailure();
    }

}

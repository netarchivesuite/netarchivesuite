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
package dk.netarkivet.systemtest.environment;

import org.jaccept.TestEventManager;

import dk.netarkivet.systemtest.TestLogger;
import dk.netarkivet.systemtest.page.PageHelper;

public class TestGUIController {
    protected final TestLogger log = new TestLogger(getClass());
    private final TestEnvironmentController testController;

    public TestGUIController(TestEnvironmentController testController) {
        this.testController = testController;
    }

    public void restartGUI() {
        try {
            log.info("Restarting GUI");
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, "./conf/kill_GUIApplication.sh; sleep 10");
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, "./conf/start_GUIApplication.sh");
            waitForGUIToStart(120);
        } catch (Exception e) {
            throw new RuntimeException("Failed to redeploy GUI", e);
        }
    }

    public void redeployGUI() {
        try {
            log.info("Redeploying GUI");
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, "rm -r tmpdircommon/*");
            testController.runCommandWithoutQuotes("prepare_test.sh");
            testController.runCommandWithoutQuotes("scp -r release_software_dist/$TESTX/lib/* "
                    + TestEnvironment.JOB_ADMIN_SERVER + ":~/$TESTX/lib");
            testController.runCommandWithoutQuotes("scp -r release_software_dist/$TESTX/webpages/* "
                    + TestEnvironment.JOB_ADMIN_SERVER + ":~/$TESTX/webpages");
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, "./conf/kill_GUIApplication.sh");
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, "./conf/start_GUIApplication.sh");
        } catch (Exception e) {
            throw new RuntimeException("Failed to redeploy GUI", e);
        }
    }

    public void waitForGUIToStart(int maxNumberOfSecondsToWait) {
        int numberOfSecondsToWaiting = 0;
        TestEventManager.getInstance().addStimuli("Waiting for GUI to start.");
        while (numberOfSecondsToWaiting++ < maxNumberOfSecondsToWait) {
            PageHelper.reloadSubPage("HarvestDefinition");
            if (PageHelper.getWebDriver().getPageSource().contains("Menu")) {
                System.out.println();
                return;
            } else {
                System.out.print(".");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
            }
        }
    }
}

/* File:        $Id$
* Revision:    $Revision$
* Author:      $Author$
* Date:        $Date$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

package dk.netarkivet.harvester.sidekick;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import junit.framework.TestCase;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.ProcessUtils;

/**
 * csr forgot to comment this!
 *
 */

public class SideKickTester extends TestCase {

    public static final String path = "./tests/dk/netarkivet/harvester/sidekick/data/";
    public static final String originalpath = path+"originals/";
    public static final String workingpath = path+"working/";
    public static final String linuxScript  = path+"linux.sh";
    public static final String windowsScript  = path+"windows.bat";

    public static final String tempFile = workingpath+"tempfile.txt";
    public static final String linuxStarterScript = path+"linuxstarter.sh";
    public static final String windowsStarterScript = path+"windowsstarter.bat";

    public static boolean isFinished;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        FileUtils.createDir(new File(workingpath));
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        FileUtils.removeRecursively(new File(workingpath));
    }

    /**
     * Test that the script we are using actually causes the expected deadlock
     * if its output is not consumed
     * @throws InterruptedException
     */
    public void testProcessDeadlocks() throws InterruptedException {
        String os = System.getProperty("os.name");
        Thread t = new Thread() {
            public void run() {
                try {
                    String os = System.getProperty("os.name");
                    Process p = null;
                    if (os.equals("Linux")) {
                        p = Runtime.getRuntime().exec(linuxScript);
                    } else {
                        p = Runtime.getRuntime().exec(windowsScript);
                    }
                    try {
                        p.waitFor();
                        isFinished = true;
                        fail("Should have deadlocked, but returned " +
                                readProcessOutput(p.getInputStream()) + " " +
                                readProcessOutput(p.getErrorStream()) );

                    } catch (InterruptedException e) {
                        fail("Should not throw interrupted exception");
                    }
                } catch (IOException e) {
                    throw new IOFailure("Threw IOException", e);
                }
            }
        };
        isFinished = false;
        t.start();
        Thread.sleep(200);
        assertFalse("Thread should have deadlocked", isFinished);

    }

    /**
     * Tests that readProcessOutput consumes the script output and allows it to
     * complete
     */
    public void testReadProcessOutput() throws InterruptedException {
        String os = System.getProperty("os.name");
        int delay = 200;
        if(os!="Linux") delay = 1000;
        Thread t = new Thread() {
            public void run() {
                try {
                    String os = System.getProperty("os.name");
                    Process p = null;
                    if (os.equals("Linux")) {
                        p = Runtime.getRuntime().exec(linuxScript);
                    } else {
                        p = Runtime.getRuntime().exec(windowsScript);
                    }
                    ProcessUtils.discardProcessOutput(p.getInputStream());
                    ProcessUtils.discardProcessOutput(p.getErrorStream());
                    try {
                        p.waitFor();
                        isFinished = true;
                    } catch (InterruptedException e) {
                        fail("Should not throw interrupted exception");
                    }
                } catch (IOException e) {
                    throw new IOFailure("Threw IOException", e);
                }
            }
        };
        isFinished = false;
        t.start();
        Thread.sleep(delay);
        assertTrue("Should have exited script", isFinished);
    }


    /**
     * This method is used in error handling in this class to see what output a
     * script actually returned
     * @param inputStream
     * @return
     */
    private static String readProcessOutput(final InputStream inputStream) {
        try {
            InputStream reader = new BufferedInputStream(inputStream);
            StringBuffer buffer = new StringBuffer();
            int c;
            while ((c = reader.read()) != -1) {
                buffer.append((char) c);
            }
            reader.close();
            return buffer.toString();
        } catch (IOException e) {
            throw new IOFailure("Couldn't read output from process.");
        }
    }

    /**
     * This tests that the SideKick will try to start the application if it
     * has stopped running.
     *
     * @throws Exception
     */
    public void testRun() throws Exception {
        String os = System.getProperty("os.name");
        String script = windowsStarterScript;
        if (os.equals("Linux")) script = linuxStarterScript;

        File file = new File(tempFile);
        assertFalse("Test file should not have been copied yet", file.exists());

        SideKick sk = new SideKick("dk.netarkivet.harvester.sidekick.SideKickTester$TestMonitorHook", script);
        Thread skThread = new Thread(sk, "SideKickThread");
        skThread.start();

        Field f1 = SideKick.class.getDeclaredField("mh");
        f1.setAccessible(true);

        Field f2 = SideKick.class.getDeclaredField("seenRunning");
        f2.setAccessible(true);

        assertFalse("SideKick should not have seen application running yet", f2.getBoolean(sk));

        ((TestMonitorHook)f1.get(sk)).running = true;
        skThread.interrupt();
        for (int attempts = 0; attempts < 100 && !f2.getBoolean(sk); attempts++) {
            Thread.sleep(10);
        }

        assertTrue("SideKick should have seen application running by now", f2.getBoolean(sk));

        ((TestMonitorHook)f1.get(sk)).running = false;
        skThread.interrupt();
        for (int attempts = 0; attempts < 100 && !file.exists(); attempts++) {
            Thread.sleep(10);
        }

        if (!file.exists()){
            System.out.println("The file does not exist: " + file.getAbsolutePath());
            if (!(file.getParentFile().exists()) ){
                System.out.println("The parent-file does not exist either: "
                        + file.getParentFile().getAbsolutePath());
            }
            fail("the file does not exist:" + file.getAbsolutePath());
        }

        Thread.sleep(100);
        String fileContent = FileUtils.readFile(file);

        assertEquals("There should be a file containing the correct content", "Dummy file!", fileContent);
    }

    /**
     * Checks the getMonitorHook() loads the correct class.
     *
     * @throws Exception
     */
    public void testGetMonitorHook() throws Exception {
        Class[] params = {String.class};
        SideKick sk = new SideKick("dk.netarkivet.harvester.sidekick.SideKickTester$TestMonitorHook", "");
        Field f = SideKick.class.getDeclaredField("mh");
        f.setAccessible(true);
        assertEquals("Field should be of correct type", f.get(sk).getClass().getName(), "dk.netarkivet.harvester.sidekick.SideKickTester$TestMonitorHook");
    }

    public void testHarvestControllerServerMonitorHook() {
        HarvestControllerServerMonitorHook h = new HarvestControllerServerMonitorHook();
        // Test toString method
        assertEquals("Tostring method returns unexpected value",
                "HarvestControllerServer",
                h.toString());

        // Test getReport() method
        try {
            h.getReport();
            fail("NotImplementedExcepted expected");
        } catch (NotImplementedException e) {
            // Expected
        }

        // Test isRunning() method
        assertEquals("IsRunning returns wrong result: ",
                new File(Settings.get(Settings.HARVEST_CONTROLLER_ISRUNNING_FILE)).exists(),
                h.isRunning());
    }


    /**
     * Helper class.
         */
    public static class TestMonitorHook extends DefaultMonitorHook {

        public boolean running = false;

        public long getMemory() {
            return super.getMemory();
        }

        public boolean isRunning() {
            return running;
        }

        public String getReport() {
            return null;
        }
    }
}

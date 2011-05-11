/* File:            $Id$
 * Revision:        $Revision$
 * Author:          $Author$
 * Date:            $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.testng.annotations.BeforeTest;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * Stand-alone test requires a custom setup of the test system, and can
 * therefore not be run together with other tests after the test system has been
 * deploy.
 */
public abstract class StandaloneTest extends SystemTest {

    @BeforeTest
    public void startTestSystem() throws Exception {
        if (System.getProperty("systemtest.redeploy", "true").equals("true")) {
            runCommandWithEnvironment(getStartupScript());
        }
    }

    /**
     * Defines the test system startup script to run. May be overridden by 
     * concrete classes.
     * @return The startup script to run
     */
    public String getStartupScript() {
        return "all_test_db.sh";
    }

    /**
     * Identifies the test on the test system. More concrete this value will be
     * used for the test environment variable.
     * 
     * @return
     */
    public String getTestX() {
        return "SystemTest";
    }

    /**
     * Uses ssh to run the indicated command on the test deployment server (kb-prod-udv-001.kb.dk). The system test 
     * environment variables: 
     * <ul>
     * <li>TIMESTAMP = svn revision
     * <li>PORT = systemtest.port property or 8071 if undefined
     * <li>MAILRECEIVERS = systemtest.mailrecievers property
     * <li>TESTX = SystemTest
     * </ul>
     * are set prior to running the command.
     * @param remoteCommand The command to run on the test server
     * @throws Exception It apparently didn't work.
     */
    private void runCommandWithEnvironment(String remoteCommand)
            throws Exception {
        BufferedReader inReader = null;
        BufferedReader errReader = null;
        JSch jsch = new JSch();

        Session session = jsch.getSession("test", "kb-prod-udv-001.kb.dk");
        session.setPassword("test123");

        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        session.connect();

        String version;
        if (System.getProperty("systemtest.version") != null)
            version = System.getProperty("systemtest.version");
        else
            version = lookupRevisionValue();
        String setTimeStampCommand = "export TIMESTAMP=" + version;
        String setPortCommand = "export PORT=" + getPort();
        String setMailReceiversCommand = "export MAILRECEIVERS="
                + System.getProperty("systemtest.mailrecievers");
        String setTestCommand = "export TESTX=" + getTestX();
        String setPathCommand = "source /etc/bashrc ; source /etc/profile; source ~/.bash_profile";

        String command = setPathCommand + ";" + setTimeStampCommand + ";"
                + setPortCommand + ";" + setMailReceiversCommand + ";"
                + setTestCommand + ";" + remoteCommand;

        long startTime = System.currentTimeMillis();
        log.info("Running JSch command: " + command);

        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);
        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(null);

        InputStream in = channel.getInputStream();
        InputStream err = ((ChannelExec) channel).getErrStream();

        channel.connect(1000);

        while (true) {
            if (channel.isClosed()) {
                log.info("Command finished in "
                        + (System.currentTimeMillis() - startTime) / 1000
                        + " seconds. " + "Exit code was "
                        + channel.getExitStatus());
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
            }
        }

        inReader = new BufferedReader(new InputStreamReader(in));
        errReader = new BufferedReader(new InputStreamReader(err));

        String s;
        StringBuffer sb = new StringBuffer();
        while ((s = errReader.readLine()) != null) {
            sb.append(s).append("\n");
        }
        log.debug(sb);
        while ((s = inReader.readLine()) != null) {
            sb.append(s).append("\n");
        }
        String result = sb.toString();
        log.error(result);
        if (result.contains("ERROR") || result.contains("Exception")) {
            throw new RuntimeException(
                    "Console output from deployment of NetarchiveSuite to the test system "
                            + "indicated a error");
        }
    }

    /**
     * The deployment script on the test server expects the 'TIMESTAMP' variable
     * to be set to the value between the 'NetarchiveSuite-' and '.zip' part of
     * the NetarchiveSuite zip file in the 'target/deploy' directory.
     * 
     * @return
     */
    private String lookupRevisionValue() {
        if (System.getProperty("systemtest.version") != null)
            return lookupRevisionValue();
        String revisionValue = null;
        File dir = new File("target/deploy");

        String[] children = dir.list();
        int testXValueStart = "NetarchiveSuite-".length();
        for (String fileName : children) {
            int zipPrefixPos = fileName.indexOf(".zip");
            if (fileName.contains("NetarchiveSuite-")
                    && zipPrefixPos > testXValueStart) {
                revisionValue = fileName.substring(testXValueStart,
                        zipPrefixPos);
                break;
            }
        }
        return revisionValue;
    }
}

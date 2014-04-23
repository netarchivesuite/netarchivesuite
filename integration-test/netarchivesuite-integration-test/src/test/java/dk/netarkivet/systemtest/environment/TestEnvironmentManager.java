/* File:            $Id$
 * Revision:        $Revision$
 * Author:          $Author$
 * Date:            $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
package dk.netarkivet.systemtest.environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import dk.netarkivet.systemtest.TestLogger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.apache.commons.io.IOUtils;

/**
 * Provides utilites for performing deployment related commands in the test environment.
 */
public class TestEnvironmentManager {
    protected final TestLogger log = new TestLogger(getClass());
    private final String TESTX;
    private final String GUI_HOST;
    private final String GUI_PORT;
    private final String TIMESTAMP;
    private final String MAILRECEIVERS;

    /**
     * The following environment definitions are used <ul>
     * <ul>
     * <li>TIMESTAMP = svn revision
     * <li>GUI_PORT = systemtest.port property or 8071 if undefined
     * <li>MAILRECEIVERS = systemtest.mailreceivers property
     * <li>TESTX = The supplied test name
     * </ul>
     * @param testX Defines the test name this test should be run under in the test system.
     */
    public TestEnvironmentManager(String testX, String host, int port) {
        TESTX = testX;
        GUI_HOST = System.getProperty("systemtest.host", host);
        GUI_PORT = System.getProperty("systemtest.port", Integer.toString(port));
        TIMESTAMP = lookupRevisionValue();
        MAILRECEIVERS = System.getProperty("systemtest.mailrecievers");
    }
    /**
     * @return The host the gui is run on.
     */
    public String getGuiHost() {
        return GUI_HOST;
    }

    /**
     * @return The port the web gui is run on.
     */
    public String getGuiPort() {
        return GUI_PORT;
    }
    
    /**
     * @return The test name.
     */
    public String getTESTX() {
        return TESTX;
    }
    
    /**
     * Runs the a command on the DEPLOYMENT_SERVER with a command timeout of 1000 seconds. Delegates to the 
     * ${link runCommand(String,int).
     * @param remoteCommand The command to run on the test server
     */
    public void runCommand(String remoteCommand) throws Exception {
        runCommand(remoteCommand, 1000);
    }
    
    /**
     * Runs the a command on the DEPLOYMENT_SERVER. Delegates to the 
     * ${link runCommand(String,String,String,int).
     * @param remoteCommand The server to run the command on.
     * @param remoteCommand The command to run on the test server.
     */
    public void runCommand(String server, String remoteCommand) throws Exception {
        runCommand(server, remoteCommand, 1000);
    }
    
    /**
     * Runs the a command with a command timeout of 1000 seconds. Delegates to the 
     * ${link runCommand(String,String,int).
     * @param remoteCommand The command to run on the test server.
     * @param commandTimeout The timeout for the command.
     */
    public void runCommand(String remoteCommand, int commandTimeout) throws Exception {
        runCommand(null, remoteCommand, commandTimeout);
    }
    
    /**
     * Run the command the in the TESTX dir. This is the normal way of separating diffrent test run in parallel. 
     * @param server The server to run the command on.
     * @param remoteCommand The command to run on the remote server.
     */
    public void runTestXCommand(String server, String remoteCommand) throws Exception {
        String testXRemoteCommand = "cd " + getTESTX() + ";" + remoteCommand;
        runCommand(server, testXRemoteCommand, 1000);
    }

    /**
     * Runs a remote command in the test environment via ssh. The system test environment variables:
     * are set prior to running the command.
     * Extends the {@link #runCommand(String)} with the possibility of overriding the default
     * timeout of 1000 seconds. This may be useful in case of prolong operations.
     * @param server The server to run the command on. If this is null the command is
     *  run on the DEPLOYMENT_SERVER. Commands run other server the will command will be executed by 
     *  ssh to the DEPLOYMENT_SERVER and from here ssh to the actual test server. 
     * @param command The command to run on the test server.
     * @param commandTimeout The timeout for the command.
     */
    public void runCommand(String server, String command, int commandTimeout) 
            throws Exception {
        runCommand(server, command, commandTimeout, "\"");
    }
    
    public void runCommandWithoutQuotes(String command) throws Exception {
        runCommand(null, command, 1000, "");
    }
    
    public void runCommandWithoutQuotes(String command, int[] positiveExitCodes) throws Exception {
        runCommand(null, command, 1000, "", positiveExitCodes);
    }

    /**
     * @param quotes the quotes ", ', none or other to use to box the command.
     */
    public void runCommand(String server, String command, int commandTimeout, String quotes) 
            throws Exception {
        runCommand(server, command, commandTimeout, quotes, new int[]{0});
    }
    
    /**
     * @param positiveExitCodes The exit codes to consider the command a success. This will normally be only 0, but in case of
     * f.ex. 'diff' 1 is also ok.
     */
    public void runCommand(String server, String command, int commandTimeout, String quotes, int[] positiveExitCodes) 
            throws Exception {
        RemoteCommand remoteCommand = new RemoteCommand(server, command, quotes);
        log.info("Running JSch command: " + remoteCommand);
        
        
        BufferedReader inReader = null;
        BufferedReader errReader = null;
        JSch jsch = new JSch();
        Session session = jsch.getSession("test", TestEnvironment.DEPLOYMENT_SERVER);
        setupJSchIdentity(jsch);
        session.setConfig("StrictHostKeyChecking", "no");
        
        long startTime = System.currentTimeMillis();
        session.connect();
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(remoteCommand.commandAsString());
        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(null);

        InputStream in = channel.getInputStream();
        InputStream err = ((ChannelExec) channel).getErrStream();

        channel.connect(1000);
        log.debug("Channel connected, command: " + remoteCommand.commandAsString());

        inReader = new BufferedReader(new InputStreamReader(in));
        errReader = new BufferedReader(new InputStreamReader(err));

        int numberOfSecondsWaiting = 0;
        int maxNumberOfSecondsToWait = 60*10;
        while (true) {
            if (channel.isClosed()) {
                log.info("Command finished in "
                        + (System.currentTimeMillis() - startTime) / 1000
                        + " seconds. " + "Exit code was "
                        + channel.getExitStatus());
                boolean errorOccured = true;
                for (int positiveExit:positiveExitCodes) {
                    if (positiveExit == channel.getExitStatus()) {
                        errorOccured = false;
                        break;
                    }
                }
                if (errorOccured || err.available() > 0) { 
                    throw new RuntimeException("Failed to run command, exit code " + channel.getExitStatus());
                }
                break;
            } else if ( numberOfSecondsWaiting > maxNumberOfSecondsToWait) {
                log.info("Command not finished after " + maxNumberOfSecondsToWait + " seconds. " +
                        "Forcing disconnect.");
                channel.disconnect();
                break;
            }
            try {
                Thread.sleep(1000);

                String s;
                while ((s = inReader.readLine()) != null) {
                    if (!s.trim().isEmpty()) log.debug("ssh: " + s);
                }
                while ((s = errReader.readLine()) != null) {
                    if (!s.trim().isEmpty()) log.warn("ssh error: " + s);
                }
            } catch (InterruptedException ie) {
            }
        }
    }


    /**
     * Escape ',', '\' and &.
     * Runs as TestX command
     */
    public void replaceStringInFile(String server, String file, String stringToReplace, String newString)
            throws Exception {
        runTestXCommand(server,
                "sed -i.original 's" + "," +
                        stringToReplace + "," +
                        newString + "," +
                        "g' " +
                        file);
    }

    private class RemoteCommand {
        final String sshTunneling;
        final String environmentSetup;
        final String command;
        final String quotes;
        
        public RemoteCommand(String server, String command, String quotes) {       
            if (server != null) {
                sshTunneling = "ssh " + server + " ";
            }  else {
                sshTunneling = "";
            }
            
            String setTimestampCommand = "export TIMESTAMP=" + TIMESTAMP;
            String setPortCommand = "export GUI_PORT=" + GUI_PORT;
            String setMailReceiversCommand = "export MAILRECEIVERS="+ MAILRECEIVERS;
            String setTestCommand = "export TESTX=" + TESTX;
            String setPathCommand = "source /etc/bashrc;source /etc/profile;source ~/.bash_profile";
            
            environmentSetup =
                    setPathCommand + ";" + setTimestampCommand + ";"
                    + setPortCommand + ";" + setMailReceiversCommand + ";"
                    + setTestCommand + ";";

            this.command = command;
            this.quotes = quotes;
        }

        /**
         * @return the remote command as a bash command string.
         */
        public String commandAsString() {
            return sshTunneling + quotes + environmentSetup + command + quotes;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("RemoteCommand ");
            if (sshTunneling != null && !sshTunneling.equals("")) {
                sb.append("sshTunneling=" + sshTunneling);
            }
            sb.append("\n\t" + environmentSetup
                    + "\n\t" + command);
            return sb.toString();
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
        String revisionValue = null;
        if (System.getProperty("systemtest.version") != null) {
            revisionValue = System.getProperty("systemtest.version");
        } else { 
            File dir = new File("deploy");
            String[] children = dir.list();
            int testXValueStart = "NetarchiveSuite-".length();
            if (children != null) {
                for (String fileName : children) {
                    int zipPrefixPos = fileName.indexOf(".zip");
                    if (fileName.contains("NetarchiveSuite-")
                        && zipPrefixPos > testXValueStart) {
                        revisionValue = fileName.substring(testXValueStart,
                                                           zipPrefixPos);
                    }
                }
            } else {
                log.warn("No revision number found, null timestamp will be used");
            }
        }
        return revisionValue;
    }

    /**
     * Setup public/private key authentication for JSch. The following attributes are used: <ul>
     * 
     * @param jsch The JSch instance to configure.
     */
    private void setupJSchIdentity(JSch jsch) throws Exception {
        String userHome =  System.getProperty("user.home");
        String privateKeyPath = System.getProperty("privateKeyPath", userHome + "/.ssh/id_rsa");
        String publicKeyPath = System.getProperty("publicKeyPath", userHome + "/.ssh/id_rsa.pub");
        String privateKeyPassword = "";
        byte [] privateKey = IOUtils.toByteArray(new FileInputStream(privateKeyPath));
        byte [] publicKey = IOUtils.toByteArray(new FileInputStream(publicKeyPath));
        byte [] passphrase = privateKeyPassword.getBytes(); 
        jsch.addIdentity("test", privateKey, publicKey, passphrase);
    }
}

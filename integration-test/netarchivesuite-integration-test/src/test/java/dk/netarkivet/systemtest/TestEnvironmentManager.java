package dk.netarkivet.systemtest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.IOUtils;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class TestEnvironmentManager {
    protected final TestLogger log = new TestLogger(getClass());
    private final String TESTX;
    private final String PORT;
    private final String TIMESTAMP;
    private final String MAILRECEIVERS;
    private final String DEPLOYMENT_SERVER = "kb-prod-udv-001.kb.dk";

    /**
     * The following environment definitions are used <ul>
     * <ul>
     * <li>TIMESTAMP = svn revision
     * <li>PORT = systemtest.port property or 8071 if undefined
     * <li>MAILRECEIVERS = systemtest.mailreceivers property
     * <li>TESTX = The supplied test name
     * </ul>
     * @param testX Defines the test name this test should be run under in the test system.
     */
    public TestEnvironmentManager(String testX) {
        TESTX = testX;
        PORT = System.getProperty("systemtest.port", "8071");
        TIMESTAMP = lookupRevisionValue();
        MAILRECEIVERS = System.getProperty("systemtest.mailrecievers");
    }

    /**
     * @return The port the web is run on.
     */
    public String getPort() {
        return PORT;
    }
    
    /**
     * Uses ssh to run the indicated command on the DEPLOYMENT_SERVER. The system test 
     * environment variables:
     * are set prior to running the command.
     * @param remoteCommand The command to run on the test server
     * @throws Exception It apparently didn't work.
     */
    public void runCommandWithEnvironment(String remoteCommand) throws Exception {
        runCommandWithEnvironment(remoteCommand, 1000);
    }

    /**
     * Extends the {@link runCommandWithEnvironment(String)} with the possibility of overriding the default
     * timeout of 1000 seconds. This may be useful in case of prolong operations.
     * @param remoteCommand
     */
    public void runCommandWithEnvironment(String remoteCommand, int commandTimeout)
            throws Exception {
        BufferedReader inReader = null;
        BufferedReader errReader = null;
        JSch jsch = new JSch();

        Session session = jsch.getSession("test", DEPLOYMENT_SERVER);
        setupJSchIdentity(jsch);
        session.setConfig("StrictHostKeyChecking", "no");

        session.connect();

        String setTimeStampCommand = "export TIMESTAMP=" + TIMESTAMP;
        String setPortCommand = "export PORT=" + PORT;
        String setMailReceiversCommand = "export MAILRECEIVERS="+ MAILRECEIVERS;
        String setTestCommand = "export TESTX=" + TESTX;
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
        log.debug("Channel connected");

        inReader = new BufferedReader(new InputStreamReader(in));

        int numberOfSecondsWaiting = 0;
        int maxNumberOfSecondsToWait = 60*10;
        while (true) {
            if (channel.isClosed()) {
                log.info("Command finished in "
                        + (System.currentTimeMillis() - startTime) / 1000
                        + " seconds. " + "Exit code was "
                        + channel.getExitStatus());
                if (channel.getExitStatus() != 0 ) { 
                    errReader = new BufferedReader(new InputStreamReader(err));

                    String s;
                    StringBuffer sb = new StringBuffer();
                    while ((s = errReader.readLine()) != null) {
                        sb.append(s).append("\n");
                    }
                    throw new RuntimeException("Failed to run command, exit code " + channel.getExitStatus() + 
                            "\n Problem was: " + sb);

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
                    System.out.println("ssh: " + s);
                }
            } catch (InterruptedException ie) {
            }
        }

        log.info("Finished command");
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
            for (String fileName : children) {
                int zipPrefixPos = fileName.indexOf(".zip");
                if (fileName.contains("NetarchiveSuite-")
                        && zipPrefixPos > testXValueStart) {
                    revisionValue = fileName.substring(testXValueStart,
                            zipPrefixPos);
                }
            }
        }
        return revisionValue;
    }

    /**
     * Setup public/private key authentication for JSch. The following attributes are used: <ul>
     * 
     * @param jsch
     * @throws Exception
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

package dk.netarkivet.common.distribute;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

public class SSHConnectionManager  {

	public static void main(String[] args) {
		String user = "test";
		String password = "test123";
		String host = "dia-prod-udv-01.kb.dk";
		int port = 22;

		String remoteFile="sample.txt";
		String remoteFile1="sample1.txt";
		int retries = 3;
		int datatimeout = 0;
		SSHConnectionManager cm = null;
		try {
		cm = new SSHConnectionManager(user, password, host, port, retries, datatimeout);
		cm.logOn();
		ChannelSftp sftpChannel = cm.getSftpSession();
		if (existDir("sftp", sftpChannel)) {
			sftpChannel.cd("sftp");
		}
			InputStream out= null;
			System.out.println("exists file " + remoteFile + ": " +  existFile(remoteFile, sftpChannel));
			System.out.println("exists file " + remoteFile1 + ": " +  existFile(remoteFile1, sftpChannel));
			if (existFile(remoteFile, sftpChannel)) {
				out= sftpChannel.get(remoteFile);
				BufferedReader br = new BufferedReader(new InputStreamReader(out));
				String line;
				while ((line = br.readLine()) != null) { 
					System.out.println(line);
				}
				br.close();
			}
		} catch(JSchException | SftpException | IOException e) {
			System.out.println(e);
		} finally {
			if (cm != null) {
				cm.logOut();
			}
		}
	}
	
	public static boolean existFile(String name, ChannelSftp channel) throws SftpException {
		try {
			channel.lstat(name);
			return true;
		} catch (SftpException e){
			if(e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE){
				// file doesn't exist
				return false;	
			} else {
				// something else went wrong
				throw e;
			}
		}
	}
	
	public static boolean existDir(String name, ChannelSftp channel) throws SftpException {
		try {
			SftpATTRS attrs = channel.lstat(name);
			return attrs.isDir();
		} catch (SftpException e){
			if(e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE){
				// file doesn't exist
				return false;	
			} else {
				// something else went wrong
				throw e;
			}
		}
	}
	
	private String sshServerName;
	private String sshUserName;
	private String sshUserPassword;
	private int sshServerPort;
	private int retries;
	private int datatimeout;
	private Session currentSession;
	private ChannelSftp sftpChannel;
	
	
	public SSHConnectionManager(String sshUserName, String sshUserPassword,
			String sshServerName, int sshServerPort, int retries, int datatimeout) {
		this.sshServerName = sshServerName;
		this.sshUserName=sshUserName;
		this.sshUserPassword = sshUserPassword;
		this.sshServerPort = sshServerPort;
		this.retries = retries;
		this.datatimeout = datatimeout;
	}

	public void logOn() throws JSchException {
		JSch jsch = new JSch();
		currentSession = jsch.getSession(sshUserName, sshServerName, sshServerPort);
		currentSession.setPassword(sshUserPassword);
		currentSession.setConfig("StrictHostKeyChecking", "no");
		currentSession.connect();
		sftpChannel = (ChannelSftp) currentSession.openChannel("sftp");
		sftpChannel.connect();
		System.out.println("Connection established to " + getSshServer());
	}

	public String getSshServer() {
		return sshUserName + "@" + sshServerName + ":" + sshServerPort;  
	}

	public ChannelSftp getSftpSession() {
		return sftpChannel;
	}

	public void logOut() {
		if (sftpChannel != null) {
			sftpChannel.disconnect();
		}
		if (currentSession != null) {
			currentSession.disconnect();
		}
	}
}

package dk.netarkivet.harvester.harvesting;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.netarchivesuite.heritrix3wrapper.CommandLauncher;
import org.netarchivesuite.heritrix3wrapper.EngineResult;
import org.netarchivesuite.heritrix3wrapper.Heritrix3Wrapper;
import org.netarchivesuite.heritrix3wrapper.Heritrix3Wrapper.CrawlControllerState;
import org.netarchivesuite.heritrix3wrapper.JobResult;
import org.netarchivesuite.heritrix3wrapper.LaunchResultHandlerAbstract;
import org.netarchivesuite.heritrix3wrapper.ResultStatus;
import org.netarchivesuite.heritrix3wrapper.unzip.UnzipUtils;

import dk.netarkivet.common.exceptions.IOFailure;

//TODO Manually create the adhoc.keystore file (in Heritrix's working directory) that 
// Heritrix usually generates automatically. This can be done using Java 8 tools with the following command (
// assumes Java's bin directory is on the path):
//
// $ keytool -keystore adhoc.keystore -storepass password 
//    -keypass password -alias adhoc -genkey -keyalg RSA 
//    -dname "CN=Heritrix Ad-Hoc HTTPS Certificate" -validity 3650
//
// TODO
// This should probably be added to the heritrix script used by NAS
// or a certificate should be available before/during the installation of NAS
//
//http://kris-sigur.blogspot.dk/2014/10/heritrix-java-8-and-sunsecuritytoolskey.html

public class H3LaunchTest {
	
	public static void main(String[] args) throws IOFailure {
		//"192.168.1.101", 6443, null, null, "h3server", "h3server"
		String hostname = "localhost";
		int port = 6443;
		File keystoreFile= null;
		String keyStorePassword = null;
		String userName = "test";
		String password = "test*test";
		File heritrix3Bundle = null;
		File unpackDir= null;
		if (args.length != 2) {
			System.err.println("Missing args: orderxmlpath and/or Seedsfilepath");
			System.exit(1);	
		}
		
		launchHeritrix(heritrix3Bundle, unpackDir, hostname, port);
	
		
		File cxmlFile = new File(args[0]);
		File seedsFile = new File(args[1]);
		
		JobResult jobResult;
		// Assumes H3 is now up and running (HOW TO VERIFY THAT??)			
		Heritrix3Wrapper h3w = Heritrix3Wrapper.getInstance(hostname, port, 
				keystoreFile, keyStorePassword, userName, password);
		
		EngineResult engineResult;
		engineResult = h3w.waitForEngineReady(60, 1000);
		
		if (engineResult != null && engineResult.status != ResultStatus.OK) {
			throw new IOFailure("Heritrix3 instance failed to start ");
		}
		
		// debug
		System.out.println(engineResult.status + " - " + ResultStatus.OK);
		File basedirStr=null;
		File jobsFile = new File(basedirStr, "jobs/");
		if (!jobsFile.exists()) {
			jobsFile.mkdirs();
		 }
		String jobname = Long.toString(System.currentTimeMillis());
		File jobFile = new File(jobsFile, jobname);
		jobFile.mkdirs();
		try {
			Heritrix3Wrapper.copyFile( cxmlFile, jobFile );
			Heritrix3Wrapper.copyFileAs( seedsFile, jobFile, "seeds.txt" ); 
		} catch (IOException e) {
			throw new IOFailure("Problem occurred during the copying of files to heritrix job", e);
		}

		engineResult = h3w.rescanJobDirectory();
		//System.out.println(new String(engineResult.response, "UTF-8"));
		jobResult = h3w.buildJobConfiguration(jobname);
		//System.out.println(new String(jobResult.response, "UTF-8"));
		jobResult = h3w.waitForJobState(jobname, CrawlControllerState.NASCENT, 60, 1000);
		jobResult = h3w.launchJob(jobname);
		//System.out.println(new String(jobResult.response, "UTF-8"));
		jobResult = h3w.waitForJobState(jobname, CrawlControllerState.PAUSED, 60, 1000);
		jobResult = h3w.unpauseJob(jobname);
		
		//System.out.println(new String(jobResult.response, "UTF-8"));
		
		// Job 'jobname' is now running
		
		/*
		boolean bFinished = false;
		while (!bFinished) {
			try {
				Thread.sleep(60 * 1000);
			} catch (InterruptedException e) {
			}
			jobResult = h3w.job(jobname);
			System.out.println(jobResult.job.isRunning);
			if (!jobResult.job.isRunning) {
				System.out.println(new String(jobResult.response, "UTF-8"));
				bFinished = true;
			}
		}
		
		jobResult = h3w.teardownJob(jobname);
		//System.out.println(new String(jobResult.response, "UTF-8"));
		engineResult = h3w.exitJavaProcess(null);
		h3launcher.process.destroy();
		*/
	//} catch (IOException e) {
	//	e.printStackTrace();
	//}
	}

	public static void launchHeritrix(File heritrix3Bundle, File unpackDir, String hostname, int port)  {
		String zipFileStr = "/home/nicl/workspace/heritrix3-wrapper/NetarchiveSuite-heritrix3-bundler-5.0-SNAPSHOT.zip";
		String unpackDirStr = "/home/nicl/heritrix3-wrapper-test/";
		String basedirStr = unpackDirStr + "heritrix-3.2.0/";
		String[] cmd = {
				"./bin/heritrix",
				"-b 192.168.1.101",
				"-p 6443",
				"-a h3server:h3server",
				"-s h3server.jks,h3server,h3server"
		};
		CommandLauncher h3launcher;
		Heritrix3Wrapper h3wrapper;
		EngineResult engineResult;
		JobResult jobResult;
		final PrintWriter outputPrinter; // final required by java 7
		final PrintWriter errorPrinter; // final required by java 7
		try {
			UnzipUtils.unzip(zipFileStr, unpackDirStr);
			File basedir = new File(basedirStr);

			//File h3serverjksFile = getTestResourceFile("h3server.jks");
			//Heritrix3Wrapper.copyFile( h3serverjksFile, basedir );

			h3launcher = CommandLauncher.getInstance();
			outputPrinter = new PrintWriter(new File(basedir, "heritrix3.out"), "UTF-8");
			errorPrinter = new PrintWriter(new File(basedir, "heritrix3.err"), "UTF-8");
			h3launcher.init(basedir, cmd);
			h3launcher.env.put("FOREGROUND", "true");
			h3launcher.start(new LaunchResultHandlerAbstract() {
				@Override
				public void exitValue(int exitValue) {
					// debug
					System.out.println("exitValue=" + exitValue);
				}
				@Override
				public void output(String line) {
					outputPrinter.println(line);
				}
				@Override
				public void closeOutput() {
					outputPrinter.close();
				}
				@Override
				public void error(String line) {
					errorPrinter.println(line);
				}
				@Override
				public void closeError() {
					errorPrinter.close();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
			
		}
	}
}

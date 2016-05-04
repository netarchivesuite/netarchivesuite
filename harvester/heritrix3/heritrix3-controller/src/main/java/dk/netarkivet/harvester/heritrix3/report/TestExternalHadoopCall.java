package dk.netarkivet.harvester.heritrix3.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/** Experiment class to implement wrapper around  the following hadoop command:
 * /home/svc/hadoop-1.2.1/bin/hadoop jar /home/svc/jbs/jbs-fatjar.jar  org.archive.jbs.Parse /tmp/ /home/svc/webdanica-hadoop-test-data/431-35-20160317083714655-00000-sb-test-har-001.statsbiblioteket.dk.warc.gz
 *  
 *  Results in the file /tmp/431-35-20160317083714655-00000-sb-test-har-001.statsbiblioteket.dk.warc.gz
 *  
 *  There is problems running hadoop using the ProcessBuilder. The process fails  with error:
 *  
 *  
 *  Ready to write hadoop-data to /tmp/1462292109062
cmd: /home/svc/hadoop-1.2.1/bin/hadoop jar /home/svc/jbs/jbs-fatjar.jar org.archive.jbs.Parse /tmp/1462292109062 /data1/svc/webdanica-hadoop-test-data/431-35-20160317083714655-00000-sb-test-har-001.statsbiblioteket.dk.warc.gz
command:  . jar /home/svc/jbs/jbs-fatjar.jar org.archive.jbs.Parse /tmp/1462292109062 /data1/svc/webdanica-hadoop-test-data/431-35-20160317083714655-00000-sb-test-har-001.statsbiblioteket.dk.warc.gz
Using java:  /usr/java/jdk1.8.0_20_x64/bin/java
Error: Could not find or load main class .home.svc.jbs.jbs-fatjar.jar

When running the full command in the bash shell, the command as seen by hadoop script is only 'jar', not 'jar ... 431-35-20160317083714655-00000-sb-test-har-001.statsbiblioteket.dk.warc.gz'
 
 *  
 *  
 * */ 
public class TestExternalHadoopCall {    
    
    
    public static void main(String[] args) throws InterruptedException {
        File testWarc = new File("/home/svc/webdanica-hadoop-test-data/431-35-20160317083714655-00000-sb-test-har-001.statsbiblioteket.dk.warc.gz");
        File hadoopPath = new File("/home/svc/hadoop-1.2.1/bin/hadoop");
        File pathToJarfile = new File("/home/svc/jbs/jbs-fatjar.jar");       
        File tmpDir = new File("/tmp/" + System.currentTimeMillis());
        tmpDir.mkdirs();
        
        if (tmpDir.canWrite()){
            System.out.println("Ready to write hadoop-data to " + tmpDir.getAbsolutePath());
        }
        //runHadoopParsedText(testWarc, hadoopPath, pathToJarfile, tmpDir);
        runHadoopParsedTextAlternate(testWarc, hadoopPath, pathToJarfile, tmpDir);
        // ./bin/hadoop jar ../jbs/jbs-fatjar.jar  org.archive.jbs.Parse /tmp/ ../jwat-tools-0.6.2/431-35-20160317083714655-00000-sb-test-har-001.statsbiblioteket.dk.warc.gz
        
    }
    
    public static void runHadoopParsedText(File testWarc, File hadoopPath, File pathToJarfile, File outputdir) {
        try {
            String cmdString =  hadoopPath.getAbsolutePath() +  " jar " + pathToJarfile.getAbsolutePath() + " org.archive.jbs.Parse " + outputdir.getAbsolutePath() + " " + testWarc.getAbsolutePath();
            System.out.println("cmd: " + cmdString);
            List<String> commandList = makeCommandlist( testWarc, hadoopPath, pathToJarfile, outputdir);
            //commandList.add(cmdString);
            ProcessBuilder pb = new ProcessBuilder(commandList);
            pb.redirectErrorStream(true);
            
            Process p = pb.start();
            
            /*
            Runtime rt = Runtime.getRuntime();
            Process  p = rt.exec( cmdString );
            p.waitFor();
            */
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String s;
            boolean f = false;
            String location = null;
            while ((s = r.readLine())!=null) {
                System.out.println(s);
                /*
                /if (s.contains("Location")){
                    foundLocation = true;
                    location = s.split("Location:")[1];
                } */
            }
            r.close();
            
        } catch ( IOException ioe ) { 
            ioe.printStackTrace(); 
        } 
    }
    private static List<String> makeCommandlist(File testWarc, File hadoopPath,
            File pathToJarfile, File outputdir) {
        List<String> commandList = new ArrayList<String>();
        commandList.add(hadoopPath.getAbsolutePath());
        commandList.add("jar " + pathToJarfile.getAbsolutePath() + " org.archive.jbs.Parse " 
          + outputdir.getAbsolutePath() +  " " + testWarc.getAbsolutePath());
        //commandList.add("org.archive.jbs.Parse");
        //commandList.add(outputdir.getAbsolutePath());
        //commandList.add(testWarc.getAbsolutePath()); 
        return commandList;
    }
    
    public static void runHadoopParsedTextAlternate(File testWarc, File hadoopPath, File pathToJarfile, File outputdir) throws InterruptedException {
        //try {
            String cmdString =  hadoopPath.getAbsolutePath() +  " jar " + pathToJarfile.getAbsolutePath() + " org.archive.jbs.Parse " + outputdir.getAbsolutePath() + " " + testWarc.getAbsolutePath();
            System.out.println("cmd: " + cmdString);
            /*
            List<String> commandList = makeCommandlist( testWarc, hadoopPath, pathToJarfile, outputdir);
            //commandList.add(cmdString);
            ProcessBuilder pb = new ProcessBuilder(commandList);
            
            pb.directory(directory); // Set the working directory of this projects
            
            pb.redirectErrorStream(true);
            
            Process p = pb.start();
            */
           
            Runtime rt = Runtime.getRuntime();
            rt.traceInstructions(true);
            rt.traceMethodCalls(true);
            Process p = null;
            try {
	            p = rt.exec( cmdString );
	            while (p.isAlive()) {
	            	Thread.sleep(4000L);
	            	System.out.println("Still alive");
	            }
	            
	            
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
            
            
            
            
            //p.waitFor();
            /*
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String s;
            while ((s = r.readLine())!=null) {
                System.out.println(s);
                /*
                /if (s.contains("Location")){
                    foundLocation = true;
                    location = s.split("Location:")[1];
                } */
          //  }
            //r.close();
      /*      
        } catch ( IOException ioe ) { 
            ioe.printStackTrace(); 
        }
        */
    
    }

}

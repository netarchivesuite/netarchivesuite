package dk.netarkivet.common.utils.hadoop;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.MRConfig;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.cdx.CDXRecord;
import sun.security.krb5.KrbException;

/** Utilities for Hadoop jobs. */
public class HadoopJobUtils {
    private static final Logger log = LoggerFactory.getLogger(HadoopJobUtils.class);

    public static final String DEFAULT_FILESYSTEM = "fs.defaultFS";
    public static final String MAPREDUCE_FRAMEWORK = "mapreduce.framework.name";
    public static final String YARN_RESOURCEMANAGER_ADDRESS = "yarn.resourcemanager.address";

    /** Utility class, do not initialise. */
    private HadoopJobUtils() {
    }

    /**
     * Obtain a logged in UserGroupInformation for running hadoop jobs from the kerberos parameters
     * defined in CommonSettings.
     * @return The UserGroupInformation instance
     * @throws KrbException if the kerberos configuration is invalid
     * @throws IOException if the kerberos login fails
     */
    public static UserGroupInformation getUserGroupInformation() throws KrbException, IOException {
        String principal = Settings.get(CommonSettings.HADOOP_KERBEROS_PRINCIPAL);
        String keytab = Settings.get(CommonSettings.HADOOP_KERBEROS_KEYTAB);
        String krb5_conf = Settings.get(CommonSettings.HADOOP_KERBEROS_CONF);
        System.setProperty("java.security.krb5.conf", krb5_conf);
        sun.security.krb5.Config.refresh();
        return UserGroupInformation.loginUserFromKeytabAndReturnUGI(principal, keytab);
    }

    /**
     * Login to Kerberos from the settings specified in CommonSettings.
     * @throws KrbException if the kerberos configuration is invalid
     * @throws IOException if the kerberos login fails
     */
    public static void doKerberosLogin() throws KrbException, IOException {
        String principal = Settings.get(CommonSettings.HADOOP_KERBEROS_PRINCIPAL);
        String keytab = Settings.get(CommonSettings.HADOOP_KERBEROS_KEYTAB);
        String krb5_conf = Settings.get(CommonSettings.HADOOP_KERBEROS_CONF);
        System.setProperty("java.security.krb5.conf", krb5_conf);
        sun.security.krb5.Config.refresh();
        UserGroupInformation.loginUserFromKeytab(principal, keytab);
    }


    /**
     * Initialize a hadoop configuration. The basic configuration must be in a directory on the classpath. This class
     * additionally sets the path to the uber jar specified in CommonSettings#HADOOP_MAPRED_UBER_JAR
     * @return A new configuration to use for a job.
     */
    public static Configuration getConf() {
        Configuration conf = new JobConf(new YarnConfiguration(new HdfsConfiguration()));
        conf.set("mapreduce.job.am-access-disabled","true");
        String truststorePath = Settings.get(CommonSettings.HADOOP_KERBEROS_TRUSTSTORE_PATH);
        if (truststorePath != null && !"".equals(truststorePath)) {
            File trustStoreFile = new File(truststorePath);
            if (!trustStoreFile.exists()) {
                throw new RuntimeException("No such truststore file: " + trustStoreFile.getAbsolutePath());
            }
            if (!trustStoreFile.isFile()) {
                throw new RuntimeException("Not a file: " + trustStoreFile.getAbsolutePath());
            }
            conf.set("ssl.client.truststore.location", truststorePath);
        }
        final String jarPath = Settings.get(CommonSettings.HADOOP_MAPRED_UBER_JAR);
        if (jarPath == null || !(new File(jarPath)).exists()) {
            log.warn("Specified jar file {} does not exist.", jarPath);
            throw new RuntimeException("Jar file " + jarPath + " does not exist.");
        }
        conf.set("mapreduce.job.jar", jarPath);
        return conf;
    }

    /**
     * Given a list of file paths prepend 'file://' to every entry and write them as newline
     * separated lines to the given input file path.
     * @param files A list of input file paths to operate on
     * @param inputFilePath The path of the file to write the lines to
     * @throws IOException If the input file path cannot be written to
     */
    public static void writeHadoopInputFileLinesToInputFile(List<java.nio.file.Path> files,
            java.nio.file.Path inputFilePath) throws IOException {
        if (files.size() == 0) {
            log.warn("No file paths to add. Input file will be empty.");
            return;
        }
        java.nio.file.Path lastElem = files.get(files.size() - 1);
        for (java.nio.file.Path file : files) {
            String inputLine = "file://" + file.toString() + "\n";
            if (file.equals(lastElem)) {
                // Not writing newline on last line to avoid a mapper being spawned on no input
                inputLine = "file://" + file.toString();
            }
            Files.write(inputFilePath, inputLine.getBytes(), StandardOpenOption.APPEND);
        }
    }

    /**
     * Collects lines from a jobs output files at a specified path.
     * Also deletes the folder once the output has been collected.
     * @param fileSystem The filesystem that the result is collected from.
     * @param outputFolder The output folder to find the job result files in.
     * @return A list of lines collected from all the output files.
     * @throws IOException If the output folder or its contents cannot be read.
     */
    public static List<String> collectOutputLines(FileSystem fileSystem, Path outputFolder) throws IOException {
        List<String> resultLines = new ArrayList<>();
        RemoteIterator<LocatedFileStatus> iterator = fileSystem.listFiles(outputFolder, false);
        while (iterator.hasNext()) {
            Path subPath = iterator.next().getPath();
            if (subPath.getName().startsWith("part-m")) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(new BufferedInputStream(
                        fileSystem.open(subPath))))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        resultLines.add(line);
                    }
                }
            }
        }
        // Clean up once output has been collected
        fileSystem.delete(outputFolder, true);
        return resultLines;
    }

    /**
     * Converts a list of CDX line strings to a list of CDXRecords
     * @param cdxLines The list to convert
     * @return A list of CDXRecords representing the old list
     */
    public static List<CDXRecord> getCDXRecordListFromCDXLines(List<String> cdxLines) {
        List<CDXRecord> recordsForJob = new ArrayList<>();
        for (String line : cdxLines) {
            String[] parts = line.split("\\s+");
            CDXRecord record = new CDXRecord(parts);
            recordsForJob.add(record);
        }
        return recordsForJob;
    }
}

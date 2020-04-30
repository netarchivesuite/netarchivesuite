package dk.netarkivet.common;

import static dk.netarkivet.common.distribute.arcrepository.bitrepository.BitmagArcRepositoryClient.BITREPOSITORY_KEYFILENAME;
import static dk.netarkivet.common.distribute.arcrepository.bitrepository.BitmagArcRepositoryClient.BITREPOSITORY_SETTINGS_DIR;
import static dk.netarkivet.common.distribute.arcrepository.bitrepository.BitmagArcRepositoryClient.BITREPOSITORY_STORE_MAX_PILLAR_FAILURES;
import static dk.netarkivet.common.distribute.arcrepository.bitrepository.BitmagArcRepositoryClient.BITREPOSITORY_USEPILLAR;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import dk.netarkivet.common.distribute.arcrepository.bitrepository.Bitrepository;
import dk.netarkivet.common.distribute.arcrepository.bitrepository.BitrepositoryUtils;
import dk.netarkivet.common.distribute.arcrepository.bitrepository.TestBitrepository;
import dk.netarkivet.common.utils.HadoopUtils;
import dk.netarkivet.common.utils.Settings;

/**
 * Class used for trying out stuff. Probably just delete
 */
public class HadoopUtilsTester {
    public static void main(String[] args) throws IOException {
        System.setProperty("HADOOP_USER_NAME", "vagrant");
        Configuration testConf = HadoopUtils.getConfFromSettings();
        System.out.println(testConf.get("fs.defaultFS")); // Ensure conf is loaded correctly

        File configDir = Settings.getFile(BITREPOSITORY_SETTINGS_DIR);
        String keyfilename = Settings.get(BITREPOSITORY_KEYFILENAME);
        int maxStoreFailures = Settings.getInt(BITREPOSITORY_STORE_MAX_PILLAR_FAILURES);
        String usepillar = Settings.get(BITREPOSITORY_USEPILLAR);

        Bitrepository bitrep = new Bitrepository(configDir, keyfilename, maxStoreFailures, usepillar);
        bitrep.getFileIds("books").forEach(System.out::println);
        if (bitrep.existsInCollection("quickstart.sh", "books")) {
            File file = bitrep.getFile("quickstart.sh", "books", null); // Can throw error so should also be handled
            System.out.println("name is " + file.getAbsoluteFile());

            FileSystem fs = FileSystem.get(testConf);
            Path hadoopInputNameFile = new Path("/user/vagrant/input.txt");
            fs.createNewFile(hadoopInputNameFile);
            FSDataOutputStream fsdos = fs.append(hadoopInputNameFile);
            fsdos.writeBytes("hdfs://node1:8020/user/vagrant/input/" + hadoopInputNameFile.getName());
            /*BufferedReader bis = new BufferedReader(new FileReader(file));
            for (String line = bis.readLine(); line != null; line = bis.readLine()) {
                System.out.println(line);
            }*/
        } else {
            System.out.println("File doesn't exist wat");
        }
        bitrep.shutdown();
        // Let's for now just reuse TestBitrepository
        //TestBitrepository.testGetFile(bitrep);
    }
}

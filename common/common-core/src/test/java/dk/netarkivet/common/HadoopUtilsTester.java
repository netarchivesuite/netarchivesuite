package dk.netarkivet.common;

import static dk.netarkivet.common.distribute.arcrepository.bitrepository.BitmagArcRepositoryClient.BITREPOSITORY_KEYFILENAME;
import static dk.netarkivet.common.distribute.arcrepository.bitrepository.BitmagArcRepositoryClient.BITREPOSITORY_SETTINGS_DIR;
import static dk.netarkivet.common.distribute.arcrepository.bitrepository.BitmagArcRepositoryClient.BITREPOSITORY_STORE_MAX_PILLAR_FAILURES;
import static dk.netarkivet.common.distribute.arcrepository.bitrepository.BitmagArcRepositoryClient.BITREPOSITORY_USEPILLAR;

import java.io.File;

import org.apache.hadoop.conf.Configuration;

import dk.netarkivet.common.distribute.arcrepository.bitrepository.Bitrepository;
import dk.netarkivet.common.distribute.arcrepository.bitrepository.BitrepositoryUtils;
import dk.netarkivet.common.distribute.arcrepository.bitrepository.TestBitrepository;
import dk.netarkivet.common.utils.HadoopUtils;
import dk.netarkivet.common.utils.Settings;

public class HadoopUtilsTester {
    public static void main(String[] args) {
        Configuration testConf = HadoopUtils.getConfFromSettings();
        System.out.println(testConf.get("fs.defaultFS")); // Ensure conf is loaded correctly

        File configDir = Settings.getFile(BITREPOSITORY_SETTINGS_DIR);
        String keyfilename = Settings.get(BITREPOSITORY_KEYFILENAME);
        int maxStoreFailures = Settings.getInt(BITREPOSITORY_STORE_MAX_PILLAR_FAILURES);
        String usepillar = Settings.get(BITREPOSITORY_USEPILLAR);

        Bitrepository bitrep = new Bitrepository(configDir, keyfilename, maxStoreFailures, usepillar);
        // Let's for now just reuse TestBitrepository
        TestBitrepository.testGetFile(bitrep);
    }
}

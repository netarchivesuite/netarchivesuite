package dk.netarkivet.common.distribute.arcrepository.bitrepository;

import static dk.netarkivet.common.distribute.arcrepository.bitrepository.BitmagArcRepositoryClient.BITREPOSITORY_KEYFILENAME;
import static dk.netarkivet.common.distribute.arcrepository.bitrepository.BitmagArcRepositoryClient.BITREPOSITORY_SETTINGS_DIR;
import static dk.netarkivet.common.distribute.arcrepository.bitrepository.BitmagArcRepositoryClient.BITREPOSITORY_STORE_MAX_PILLAR_FAILURES;
import static dk.netarkivet.common.distribute.arcrepository.bitrepository.BitmagArcRepositoryClient.BITREPOSITORY_USEPILLAR;

import java.io.File;

import dk.netarkivet.common.utils.Settings;

/**
 *
 *
 *
 */
public class TestBitrepository {

    public static void main(String[] args) {
        File configDir = Settings.getFile(BITREPOSITORY_SETTINGS_DIR);
        String keyfilename = Settings.get(BITREPOSITORY_KEYFILENAME);
        int maxStoreFailures = Settings.getInt(BITREPOSITORY_STORE_MAX_PILLAR_FAILURES);
        String usepillar = Settings.get(BITREPOSITORY_USEPILLAR);

        // Initialize connection to the bitrepository
        Bitrepository bitrep = Bitrepository.getInstance(configDir, keyfilename, maxStoreFailures, usepillar);


        testGetFileIDs(bitrep);
        testGetFile(bitrep);
    }

    public static void testGetFile(Bitrepository bitrep){
        int i=0;
        for (String col: bitrep.getKnownCollections()) {
            i++;
            System.out.println("col " + i + ": " + col);
        }
        i=0;
        for ( String pillar: BitrepositoryUtils.getCollectionPillars("books")) {
            i++;
            System.out.println("pillar " + i + ": " + pillar);
        }

        if (bitrep.existsInCollection("70-metadata-1.warc", "books")){
            System.out.println("70-metadata-1.warc found in collection books");
            File f = bitrep.getFile("70-metadata-1.warc", "books", null);
            System.out.println("file fetched = " + f.getAbsolutePath());
        } else {
            System.out.println("70-metadata-1.warc NOT found in collection books");
        }

        bitrep.shutdown();
    }

    /**
     * TEST That integrates with our bitrepository integration system.
     * netarkiv-pillars:
     checksum2
     sbdisk1
     */
    public static void testGetFileIDs(Bitrepository bitrep){
        for (String col: bitrep.getKnownCollections()) {
            System.out.println(col);
        }

        for ( String pillar: BitrepositoryUtils.getCollectionPillars("netarkiv")) {
            System.out.println(pillar);
        }
        System.out.println();
        System.out.println("netarkiv-pillars:");
        for (String col: BitrepositoryUtils.getCollectionPillars("netarkiv")) {
            System.out.println(col);
        }

        System.out.println("netarkiv-ids:");
        for (String id: bitrep.getFileIds("netarkiv")) {
            System.out.println(id);
        }

        bitrep.shutdown();

    }
}

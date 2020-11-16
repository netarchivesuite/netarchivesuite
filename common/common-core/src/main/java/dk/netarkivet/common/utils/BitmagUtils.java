package dk.netarkivet.common.utils;

import static dk.netarkivet.common.distribute.bitrepository.BitmagUtils.BITREPOSITORY_KEYFILENAME;
import static dk.netarkivet.common.distribute.bitrepository.BitmagUtils.BITREPOSITORY_SETTINGS_DIR;
import static dk.netarkivet.common.distribute.bitrepository.BitmagUtils.BITREPOSITORY_STORE_MAX_PILLAR_FAILURES;
import static dk.netarkivet.common.distribute.bitrepository.BitmagUtils.BITREPOSITORY_USEPILLAR;

import java.io.File;

import dk.netarkivet.common.distribute.bitrepository.Bitrepository;

/** Utilities for using Bitmag. */
public class BitmagUtils {

    /** Utility class, do not initialise. */
    private BitmagUtils() {
    }

    /**
     * Init a Bitrepository from settings
     * @return Initialised Bitrepository
     */
    public static Bitrepository initBitrep() {
        File configDir = Settings.getFile(BITREPOSITORY_SETTINGS_DIR);
        String keyfilename = Settings.get(BITREPOSITORY_KEYFILENAME);
        int maxStoreFailures = Settings.getInt(BITREPOSITORY_STORE_MAX_PILLAR_FAILURES);
        String usepillar = Settings.get(BITREPOSITORY_USEPILLAR);

        return Bitrepository.getInstance(configDir, keyfilename);
    }
}

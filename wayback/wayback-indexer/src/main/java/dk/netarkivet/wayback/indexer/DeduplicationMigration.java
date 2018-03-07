package dk.netarkivet.wayback.indexer;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.IOFailure;

public class DeduplicationMigration {

    private static final Logger log = LoggerFactory.getLogger(DeduplicationMigration.class);
            
    public static Hashtable<Pair<String, Long>, Long> parseMigrationRecords(File deduplicationMigrationData, String filename) {
        Hashtable<Pair<String, Long>, Long> lookup = new Hashtable<>();
        try {
            final List<String> migrationLines = org.apache.commons.io.FileUtils.readLines(deduplicationMigrationData);
            log.info("{} migration records found in file {}", migrationLines.size(), filename);
            // duplicationmigration lines should look like this: "FILENAME 496812 393343 1282069269000"
            // But only the first 3 entries are used.
            for (String line : migrationLines) {
                String[] splitLine = StringUtils.split(line);
                if (splitLine.length >= 3) { 
                    lookup.put(new Pair<String, Long>(splitLine[0], Long.parseLong(splitLine[1])),
                            Long.parseLong(splitLine[2])); 
                } else {
                    log.warn("Line '{}' has a wrong format. Ignoring line", line);
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Could not read " + deduplicationMigrationData.getAbsolutePath());
        } finally {
            deduplicationMigrationData.delete();
        }
        return lookup;
    }
}

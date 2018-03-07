/*
 * #%L
 * Netarchivesuite - wayback
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.wayback;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.regex.Pattern;
import org.apache.commons.math3.util.Pair;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.archive.GetMetadataArchiveBatchJob;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.wayback.batch.DeduplicateToCDXAdapter;
import dk.netarkivet.wayback.indexer.DeduplicationMigration;

/**
 * A simple command line application to generate cdx files from local metadata-files
 */

public class DeduplicateToCDXApplicationWithMigration {
    
    /**
     * Takes an array of file names (relative or full paths) of metadata files from which duplicate records are to be
     * extracted. Writes the concatenated cdx files of all duplicate records in these files to standard out. An
     * exception will be thrown if any of the files cannot be read for any reason or if the argument is null
     *
     * @param metadatafiles a list of metadata filenames
     * @throws FileNotFoundException if one of the files cannot be found
     */
    public void generateCDX(String[] metadatafiles) throws IOException {
        ArgumentNotValid.checkNotNull(metadatafiles, "metadatafiles");
        DeduplicateToCDXAdapter adapter = new DeduplicateToCDXAdapter();
        for (String filename : metadatafiles) {
            File file = new File(filename);
            // get crawlog and store as temp file
            File crawlLogFile = getCrawllogFile(file);
            File duplicationMigrationData = getDuplicationMigrationFile(file);
            File cdxFile = new File(file.getParentFile(), file.getName() + ".cdx"); 
            FileOutputStream fos = new FileOutputStream(cdxFile);
            if (duplicationMigrationData != null && duplicationMigrationData.length() > 0) {
                System.out.println("Found duplicationMigrationRecord in file '" + filename + "'. Running new adapter");
                // read migrationdata into datastructure
                Hashtable<Pair<String, Long>, Long> lookup = DeduplicationMigration.parseMigrationRecords(duplicationMigrationData, filename);
                FileInputStream inputStream = new FileInputStream(crawlLogFile);
                adapter.setLookup(lookup);
                adapter.adaptStream(inputStream, fos);
                fos.close();
            } else {
                System.out.println("No duplicationMigrationRecord found in file '" + filename + "'. No migration of duplicate record done");
                FileInputStream inputStream = new FileInputStream(crawlLogFile);
                System.out.println("Size of crawllog: " + crawlLogFile.length());
                adapter.setLookup(null);
                adapter.adaptStream(inputStream, fos);
                fos.close();
            }
            System.out.println("Finished processing file '" + filename + "'");
        }
    }

    private File getCrawllogFile(File file) throws IOException {
        GetMetadataArchiveBatchJob job = new GetMetadataArchiveBatchJob(
                Pattern.compile(Constants.CRAWL_LOG_URL_PATTERN_STRING), Pattern.compile("text/plain"));
        BatchLocalFiles batch = new BatchLocalFiles(new File[]{file});
        File resultFile = File.createTempFile("batch", "crawllog", new File("/tmp"));
        OutputStream os = new FileOutputStream(resultFile);
        batch.run(job, os);
        os.close();
        return resultFile;
    }

    private File getDuplicationMigrationFile(File file) throws IOException {
        GetMetadataArchiveBatchJob job = new GetMetadataArchiveBatchJob(
                Pattern.compile(Constants.DEDUPLICATION_MIGRATION_URL_PATTERN_STRING), Pattern.compile("text/plain"));
        BatchLocalFiles batch = new BatchLocalFiles(new File[]{file});
        File resultFile = File.createTempFile("batch", "dedupmig", new File("/tmp"));
        OutputStream os = new FileOutputStream(resultFile);
        batch.run(job, os);
        os.close();
        return resultFile;
    }

    /**
     * An application to generate unsorted cdx files from duplicate records present in a crawl.log file. The only
     * parameters are a list of file-paths. Output is written to standard out.
     *
     * @param args the file names (relative or absolute paths)
     * @throws FileNotFoundException if one or more of the files does not exist
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("No files specified on command line");
            System.err.println("Usage: java " + DeduplicateToCDXApplicationWithMigration.class.getCanonicalName() + " <files>");
        } else {
            DeduplicateToCDXApplicationWithMigration app = new DeduplicateToCDXApplicationWithMigration();
            app.generateCDX(args);
        }
    }
    
}

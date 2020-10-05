/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
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
package dk.netarkivet.harvester.harvesting.metadata;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.jwat.common.ANVLRecord;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.SlowTest;
import dk.netarkivet.testutils.TestResourceUtils;

@SuppressWarnings({"unchecked", "rawtypes"})
public class MetadataFileWriterTester {

	@Rule
	public TestName test = new TestName();

    private static File logsDir = TestResourceUtils.getFile("crawldir/logs");

    private File WORKING_DIR;

    @Before
    public void initialize() {
    	WORKING_DIR = new File(TestResourceUtils.OUTPUT_DIR, getClass().getSimpleName() + "/" + test.getMethodName());
        FileUtils.removeRecursively(WORKING_DIR);
        FileUtils.createDir(WORKING_DIR);
        if (!logsDir.exists() || !logsDir.isDirectory()) {
        	Assert.fail("Test resource directory missing 'crawldir/logs'!");
        }
    }

    @Test
    public void testMetadataFileWriterArc() throws IOException {
        File metafile = getOutputArcFile("metadata.arc");
        MetadataFileWriter mdfw = MetadataFileWriterArc.createWriter(metafile);

        String uri = "http://www.netarkivet.dk/";
        long ctm = System.currentTimeMillis();

        SecureRandom random = new SecureRandom();
        byte[] payload = new byte[8192];
        random.nextBytes(payload);

        mdfw.write(uri, "application/binary", "127.0.0.1", ctm, payload);
        mdfw.close();
        metafile.deleteOnExit();

        File metadataArcFile = getOutputArcFile("42-metadata-1.arc");
        MetadataFileWriter mfwa = MetadataFileWriterArc.createWriter(metadataArcFile);
        for (File f : logsDir.listFiles()) {
            mfwa.writeFileTo(f, "metadata://netarkivet.dk/crawl/logs/" + f.getName(), "text/plain");
        }
    }

    @Test
    public void testMetadataFileWriterWarc() throws IOException {
        File metafile = getOutputArcFile("metadata.warc");
        MetadataFileWriter mdfw = MetadataFileWriterWarc.createWriter(metafile);

        String uri = "http://www.netarkivet.dk/";
        long ctm = System.currentTimeMillis();

        SecureRandom random = new SecureRandom();
        byte[] payload = new byte[8192];
        random.nextBytes(payload);

        mdfw.write(uri, "application/binary", "127.0.0.1", ctm, payload);
        mdfw.close();

        metafile.deleteOnExit();

        File metadataArcFile = getOutputArcFile("42-metadata-1.warc");
        MetadataFileWriter mfwa = MetadataFileWriterWarc.createWriter(metadataArcFile);
        ((MetadataFileWriterWarc) mfwa).insertInfoRecord(new ANVLRecord());

        for (File f : logsDir.listFiles()) {
            mfwa.writeFileTo(f, "metadata://netarkivet.dk/crawl/logs/" + f.getName(), "text/plain");
        }
    }

    /**
     * This is not run automatically, as this takes a long time to complete (15 seconds).
     */
    @Category(SlowTest.class)
    @Test
    public void notestMetadataFileWriterWarcMassiveLoadTest() throws IOException {
        // TODO verify content of produced warc-file to ensure that all is OK
        File metafile = getOutputArcFile("metadata.warc");
        MetadataFileWriterWarc mdfw = (MetadataFileWriterWarc) MetadataFileWriterWarc.createWriter(metafile);
        mdfw.insertInfoRecord(new ANVLRecord());
        // Create 5000 small files
        String contentPart = "blablabla";
        String someText = StringUtils.repeat(contentPart, 5000);
        List textArray = new ArrayList<String>();
        textArray.add(someText);
        Set<File> files = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            File f = File.createTempFile("metadata", "cdx");
            FileUtils.writeCollectionToFile(f, textArray);
            files.add(f);
        }
        int count = 0;
        for (File f : files) {
            mdfw.writeFileTo(f, "http://netarkivet/ressource-" + count, "text/plain");
            f.delete();
            count++;
        }
    }

    private File getOutputArcFile(String name) {
        File arcfile = new File(WORKING_DIR, name);
        try {
            arcfile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return arcfile;
    }

}

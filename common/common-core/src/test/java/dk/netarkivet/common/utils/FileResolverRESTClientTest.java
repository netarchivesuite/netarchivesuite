package dk.netarkivet.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.service.FileResolver;
import dk.netarkivet.common.utils.service.FileResolverRESTClient;

/**
 * Test suite assumes the existence of a FileResolver with the following files
 * /kbhpillar/collection-netarkivet/1-1-20201015125643056-00000-ciblee_2015_nasharfocus.warc
 * /kbhpillar/collection-netarkivet/1-metadata-1.warc
 * /kbhpillar/collection-netarkivet/10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc.gz
 * /kbhpillar/collection-netarkivet/1234-metadata-1.warc.gz
 * /kbhpillar/collection-netarkivet/5282-metadata-1.warc.gz
 * /kbhpillar/collection-netarkivet/Readme.md
 * /kbhpillar/collection-netarkivet/dkcollection-1-1-metadata-1.warc.gz
 * although not necessarily with these exact paths.
 */
@Ignore
public class FileResolverRESTClientTest {

    FileResolver fileResolver;

    @Before
    public void testFactoryMethod() {
        //Settings.set(CommonSettings.ACCESS_TRUSTSTORE_PATH, "/etc/ssl/certs/java/cacerts");
        Settings.set(CommonSettings.TRUSTSTORE_PASSWORD, "changeit");
        Settings.set(CommonSettings.FILE_RESOLVER_CLASS, "dk.netarkivet.common.utils.service.FileResolverRESTClient");
        Settings.set(CommonSettings.FILE_RESOLVER_BASE_URL, "https://kb-test-netarkivet-bitmag-acs-01.kb.dk/cgi-bin/fileresolver.cgi/");
        //Settings.set(CommonSettings.FILE_RESOLVER_BASE_URL, "https://localhost:10444/cgi-bin/fileresolver.cgi");
        Settings.set(CommonSettings.FILE_RESOLVER_KEYFILE, "/home/colin/https_key.pem");
        fileResolver = SettingsFactory.getInstance(CommonSettings.FILE_RESOLVER_CLASS);
        assertTrue(fileResolver instanceof FileResolverRESTClient);
    }

    @Test
    public void testFileresolver() {
        List<Path> paths = fileResolver.getPaths(Pattern.compile(".*warc.gz"));
        paths.forEach(System.out::println);
    }

    @Test
    public void testFailOnBadUrl() {
        Settings.set(CommonSettings.FILE_RESOLVER_CLASS, "dk.netarkivet.common.utils.service.FileResolverRESTClient");
        Settings.set(CommonSettings.FILE_RESOLVER_BASE_URL, "localhost:10444/cgi-bin/fileresolver.cgi");
        try {
            SettingsFactory.getInstance(CommonSettings.FILE_RESOLVER_CLASS);
            fail("Should have thrown exception before getting here.");
        } catch (Exception e) {
            //expected
        }
    }

    @Test
    public void getPathsMultiple() {
        String byJobNumber = "1-.*.warc.*";
        List<Path> paths = fileResolver.getPaths(Pattern.compile(byJobNumber));
        assertEquals("Expected two files for " + byJobNumber + " not " + paths, paths.size(), 2);
    }

    @Test
    public void getPathsMultipleMetadata() {
        String byJobNumber = "(.*-)?" + 1 + "(-.*)?" + Settings.get(CommonSettings.METADATAFILE_REGEX_SUFFIX);
        List<Path> paths = fileResolver.getPaths(Pattern.compile(byJobNumber));
        assertEquals("Expected two files for " + byJobNumber + " not " + paths, 2, paths.size());
    }

    @Test
    public void getPath() {
        String filename = "10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc.gz";
        Path path = fileResolver.getPath(filename);
        assertTrue("Expected a valid path for the file, not " + path,
                path.toString().contains("/") && path.endsWith(filename));
    }

    @Test
    public void getPathNoSuchFile() {
        String filename = "foobar10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc.gz";
        Path path = fileResolver.getPath(filename);
        assertNull("Expected null, not " + path, path);
    }

    @Test
    public void getPathsEmpty() {
        String byJobNumber = "foobar1-*.warc*";
        List<Path> paths = fileResolver.getPaths(Pattern.compile(byJobNumber));
        assertTrue("Expected 0 results not " + paths, paths.isEmpty());
    }


    @Test
    public void testManyRuns() {
        for (int i = 0; i<40; i++ ) {
            String filename = "10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc.gz";
            Path path = fileResolver.getPath(filename);
            assertTrue("Expected a valid path for the file, not " + path,
                    path.toString().contains("/") && path.endsWith(filename));
            System.out.println("Done: " + i);
        }
    }



}
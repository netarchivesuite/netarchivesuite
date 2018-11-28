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
package dk.netarkivet.harvester.datamodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.regex.Pattern;

import org.dom4j.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.harvester.tools.HarvestTemplateApplication;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Unit tests for the HarvestTemplateApplication tool.
 */
@Ignore("binary derby database not converted to scripts yet")
public class HarvestTemplateApplicationTester {

    PreventSystemExit pse = new PreventSystemExit();
    ReloadSettings rs = new ReloadSettings();

    InputStream origIn;
    PrintStream origOut;
    PrintStream origErr;

    ByteArrayOutputStream newOut = new ByteArrayOutputStream();
    ByteArrayOutputStream newErr = new ByteArrayOutputStream();

    PrintStream outPrintStream = new PrintStream(newOut);
    PrintStream errPrintStream = new PrintStream(newErr);

    @Before
    public void setUp() throws Exception {
        rs.setUp();
        FileUtils.removeRecursively(TestInfo.TEMPDIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.DATADIR, TestInfo.TEMPDIR);
        HarvestDAOUtils.resetDAOs();
        Settings.set(CommonSettings.DB_BASE_URL, "jdbc:derby:" + TestInfo.TEMPDIR.getCanonicalPath() + "/fullhddb");
        DatabaseTestUtils.createHDDB(TestInfo.DBFILE, "fullhddb", TestInfo.TEMPDIR);
        TemplateDAO.getInstance();

        origIn = System.in;
        origOut = System.out;
        origErr = System.err;

        System.setOut(outPrintStream);
        System.setErr(errPrintStream);

        pse.setUp();
    }

    @After
    public void tearDown() throws Exception {
        pse.tearDown();

        System.setIn(origIn);
        System.setOut(origOut);
        System.setErr(origErr);

        newOut.reset();
        newErr.reset();

        DatabaseTestUtils.dropHDDB();
        FileUtils.removeRecursively(TestInfo.TEMPDIR);
        HarvestDAOUtils.resetDAOs();
        rs.tearDown();
    }

    /**
     * Check that the locally setup output and error streams match certain patterns.
     *
     * @param message
     * @param outPattern
     * @param errPattern
     */
    private void assertOutAndErrMatches(String message, String outPattern, String errPattern) {
        outPrintStream.flush();
        errPrintStream.flush();
        String outString = newOut.toString();
        String errString = newErr.toString();
        if (!Pattern.compile(outPattern, Pattern.DOTALL).matcher(outString).matches()) {
            fail(message + " Pattern " + outPattern + " not matching stdout '" + outString + "'");
        }
        if (!Pattern.compile(errPattern, Pattern.DOTALL).matcher(errString).matches()) {
            fail(message + " Pattern " + errPattern + " not matching stderr '" + errString + "'");
        }
    }

    @Test
    public void testMainNoCommand() throws Exception {
        HarvestTemplateApplication.main(new String[0]);
        assertOutAndErrMatches("Should fail on no arguments.", "^$",
                ".*HarvestTemplateApplication.*create.*download.*update.*showall.*");

    }

    @Test
    public void testMainIllegalCommand() {
        HarvestTemplateApplication.main(new String[] {"foo"});
        assertOutAndErrMatches("Should fail on illegal command.", "^$", ".*foo.*not one of the legal commands.*"
                + "create.*download.*update.*showall.*");
    }

    @Test
    public void testCreateNoArgs() throws Exception {
        HarvestTemplateApplication.main(new String[] {"create"});
        assertOutAndErrMatches("Should fail on missing parameter.", "^$",
                ".*create.*Wrong number\\(0\\) of arguments.*" + "download.*update.*");
    }

    @Test
    public void testCreateOneArg() throws Exception {
        HarvestTemplateApplication.main(new String[] {"create", "foo"});
        assertOutAndErrMatches("Should fail on missing parameter.", "^$",
                ".*create.*Wrong number\\(1\\) of arguments.*" + "download.*update.*");
    }

    @Test
    public void testCreateMissingFile() throws Exception {
        HarvestTemplateApplication.main(new String[] {"create", "foo", "missing-file"});
        assertOutAndErrMatches("Should fail on missing file.", "^$", ".*missing-file.*is not readable.*");
    }

    @Test
    public void testCreateIllegalFile() throws Exception {
        HarvestTemplateApplication
                .main(new String[] {"create", "foo", new File(TestInfo.EMPTYDBFILE).getAbsolutePath()});
        assertOutAndErrMatches("Should fail on illegal file.", "^$",
                ".*netarkivtest.log.*is not readable or is not valid xml.*");
    }

    @Test
    public void testCreate() {
        HarvestTemplateApplication
                .main(new String[] {"create", "NewTemplate", TestInfo.ORDERXMLFILE.getAbsolutePath()});
        assertOutAndErrMatches("Should succeed with new file", "^The template 'NewTemplate' has now been created.\n$",
                "^$");
        assertTrue("Should have newly created template in DAO", TemplateDAO.getInstance().exists("NewTemplate"));
        Document doc = XmlUtils.getXmlDoc(TestInfo.ORDERXMLFILE);

        
        // FIXME this test assumes an equals methods for the HeritrixTemplate class.
        /*
        assertEquals("Should have same info in doc as in jobDAO", doc.getText(),
        TemplateDAO.getInstance().read("NewTemplate").getTemplate().getText());
                */
    }

    @Test
    public void testDownloadTemplatesAll() {
        HarvestTemplateApplication.main(new String[] {"download"});
        assertOutAndErrMatches("Should get all templates", "^Downloading template 'FullSite-order'.\n"
                + "Downloading template 'Max_20_2-order'.\n" + "Downloading template 'OneLevel-order'.\n"
                + "Downloading template 'default_orderxml'.\n$", "^$");
        Document doc = XmlUtils.getXmlDoc(new File("OneLevel-order.xml"));
        // FIXME this test assumes an equals methods for the HeritrixTemplate class.
        /* assertEquals("Should have same info in downloaded as in DB", TemplateDAO.getInstance().read("OneLevel-order")
                .getTemplate().getText(), doc.getText());
                */
    }

    @Test
    public void testDownloadTemplatesOne() {
        HarvestTemplateApplication.main(new String[] {"download", "OneLevel-order"});
        assertOutAndErrMatches("Should get one template", "^Downloading template 'OneLevel-order'.\n$", "^$");
    }

    @Test
    public void testDownloadTemplatesTwo() {
        HarvestTemplateApplication.main(new String[] {"download", "OneLevel-order", "NotThere"});
        assertOutAndErrMatches("Should get one template and one error", "^Downloading template 'OneLevel-order'.\n$",
                "^Unable to download template 'NotThere'. It does not exist.\n$");
    }

    @Test
    public void testUpdateNoArgs() {
        HarvestTemplateApplication.main(new String[] {"update"});
        assertOutAndErrMatches("Should fail on missing parameter.", "", ".*update.*Wrong number\\(0\\) of arguments.*"
                + "create.*download.*update.*showall.*");
    }

    @Test
    public void testUpdateOneArg() {
        HarvestTemplateApplication.main(new String[] {"update", "foo"});
        assertOutAndErrMatches("Should fail on missing parameter.", "", ".*update.*Wrong number\\(1\\) of arguments.*"
                + "create.*download.*update.*showall.*");
    }

    @Test
    public void testUpdateNoTemplate() {
        HarvestTemplateApplication.main(new String[] {"update", "foo", "bar"});
        assertOutAndErrMatches("Should fail on missing parameter.", "^$",
                ".*There is no template named 'foo'. Use the create.*");
    }

    @Test
    public void testUpdateNoFile() {
        HarvestTemplateApplication.main(new String[] {"update", "OneLevel-order", "missing-file"});
        assertOutAndErrMatches("Should fail on missing file.", "^$",
                ".*missing-file.*could not be read or is not valid xml.*");
    }

    @Test
    public void testShowAll() {
        HarvestTemplateApplication.main(new String[] {"showall"});
        assertOutAndErrMatches("Should list all templates.", "^FullSite-order\n" + "Max_20_2-order\n"
                + "OneLevel-order\n" + "default_orderxml\n$", "^$");
    }

}

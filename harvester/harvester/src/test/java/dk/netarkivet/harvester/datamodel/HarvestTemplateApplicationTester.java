/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package dk.netarkivet.harvester.datamodel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.regex.Pattern;

import junit.framework.TestCase;
import org.dom4j.Document;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.testutils.DatabaseTestUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.TestUtils;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Unit tests for the HarvestTemplateApplication tool.
 */
public class HarvestTemplateApplicationTester extends TestCase {
    PreventSystemExit pse = new PreventSystemExit();
    ReloadSettings rs = new ReloadSettings();
    
    InputStream origIn;
    PrintStream origOut;
    PrintStream origErr;

    ByteArrayOutputStream newOut = new ByteArrayOutputStream();
    ByteArrayOutputStream newErr = new ByteArrayOutputStream();

    PrintStream outPrintStream = new PrintStream(newOut);
    PrintStream errPrintStream = new PrintStream(newErr);

    public HarvestTemplateApplicationTester(String s) {
        super(s);
    }

    public void setUp() throws SQLException, IllegalAccessException, IOException {
        rs.setUp();
        FileUtils.removeRecursively(TestInfo.TEMPDIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.DATADIR, TestInfo.TEMPDIR);
        TestUtils.resetDAOs();
        Settings.set(CommonSettings.DB_URL,
                "jdbc:derby:" + TestInfo.TEMPDIR.getCanonicalPath() + "/fullhddb");
        DatabaseTestUtils.getHDDB(TestInfo.DBFILE, "fullhddb",
                TestInfo.TEMPDIR);
        TemplateDAO.getInstance();

        origIn = System.in;
        origOut = System.out;
        origErr = System.err;

        System.setOut(outPrintStream);
        System.setErr(errPrintStream);

        pse.setUp();
    }

    public void tearDown() throws Exception {
        pse.tearDown();

        System.setIn(origIn);
        System.setOut(origOut);
        System.setErr(origErr);

        newOut.reset();
        newErr.reset();

        DatabaseTestUtils.dropHDDB();
        FileUtils.removeRecursively(TestInfo.TEMPDIR);
        TestUtils.resetDAOs();
        rs.tearDown();
    }

    /** Check that the locally setup output and error streams match certain
     * patterns.
     *
     * @param message
     * @param outPattern
     * @param errPattern
     */
    private void assertOutAndErrMatches(String message, String outPattern,
                                        String errPattern) {
        outPrintStream.flush();
        errPrintStream.flush();
        String outString = newOut.toString();
        String errString = newErr.toString();
        if (!Pattern.compile(outPattern, Pattern.DOTALL).matcher(outString).matches()) {
            fail(message + " Pattern " + outPattern + " not matching stdout '"
                    + outString + "'");
        }
        if (!Pattern.compile(errPattern, Pattern.DOTALL).matcher(errString).matches()) {
            fail(message + " Pattern " + errPattern + " not matching stderr '"
                    + errString + "'");
        }
    }

    public void testMainNoCommand() throws Exception {
        HarvestTemplateApplication.main(new String[0]);
        assertOutAndErrMatches("Should fail on no arguments.", "^$",
                ".*HarvestTemplateApplication.*create.*download.*update.*showall.*");

    }

    public void testMainIllegalCommand() {
        HarvestTemplateApplication.main(new String[] {"foo"});
        assertOutAndErrMatches("Should fail on illegal command.", "^$",
                ".*foo.*not one of the legal commands.*"
                        + "create.*download.*update.*showall.*");
    }

    public void testCreateNoArgs() throws Exception {
        HarvestTemplateApplication.main(new String[] {"create"});
        assertOutAndErrMatches("Should fail on missing parameter.",
                "^$", ".*create.*Wrong number\\(0\\) of arguments.*"
                + "download.*update.*");
    }

    public void testCreateOneArg() throws Exception {
        HarvestTemplateApplication.main(new String[]{
                "create", "foo"});
        assertOutAndErrMatches("Should fail on missing parameter.",
                "^$", ".*create.*Wrong number\\(1\\) of arguments.*"
                + "download.*update.*");
    }

    public void testCreateMissingFile() throws Exception {
        HarvestTemplateApplication.main(new String[]{
                "create", "foo", "missing-file"});
        assertOutAndErrMatches("Should fail on missing file.",
                "^$",
                ".*missing-file.*is not readable.*");
    }

    public void testCreateIllegalFile() throws Exception {
        HarvestTemplateApplication.main(new String[]{
                "create", "foo", TestInfo.LOG_FILE.getAbsolutePath()});
        assertOutAndErrMatches("Should fail on illegal file.",
                "^$",
                ".*netarkivtest.log.*is not readable or is not valid xml.*");
    }

    public void testCreate() {
        HarvestTemplateApplication.main(new String[] {
            "create", "NewTemplate", TestInfo.ORDERXMLFILE.getAbsolutePath()});
        assertOutAndErrMatches("Should succeed with new file",
                "^The template 'NewTemplate' has now been created.\n$", "^$");
        assertTrue("Should have newly created template in DAO",
                TemplateDAO.getInstance().exists("NewTemplate"));
        Document doc = XmlUtils.getXmlDoc(TestInfo.ORDERXMLFILE);
        assertEquals("Should have same info in doc as in dao",
                doc.getText(),
                TemplateDAO.getInstance().read("NewTemplate")
                    .getTemplate().getText());
    }

    public void testDownloadTemplatesAll() {
        HarvestTemplateApplication.main(new String[] {
                "download"
        });
        assertOutAndErrMatches("Should get all templates",
                "^Downloading template 'FullSite-order'.\n"
                        + "Downloading template 'Max_20_2-order'.\n"
                        + "Downloading template 'OneLevel-order'.\n"
                        + "Downloading template 'default_orderxml'.\n$",
                "^$");
        Document doc = XmlUtils.getXmlDoc(new File("OneLevel-order.xml"));
        assertEquals("Should have same info in downloaded as in DB",
                TemplateDAO.getInstance().read("OneLevel-order")
                .getTemplate().getText(),
                doc.getText());
    }

    public void testDownloadTemplatesOne() {
        HarvestTemplateApplication.main(new String[] {
                "download", "OneLevel-order"
        });
        assertOutAndErrMatches("Should get one template",
                "^Downloading template 'OneLevel-order'.\n$", "^$");
    }

    public void testDownloadTemplatesTwo() {
        HarvestTemplateApplication.main(new String[] {
                "download", "OneLevel-order", "NotThere"
        });
        assertOutAndErrMatches("Should get one template and one error",
                "^Downloading template 'OneLevel-order'.\n$",
                "^Unable to download template 'NotThere'. It does not exist.\n$");
    }

    public void testUpdateNoArgs() {
        HarvestTemplateApplication.main(new String[] {"update" });
        assertOutAndErrMatches("Should fail on missing parameter.",
                "", ".*update.*Wrong number\\(0\\) of arguments.*"
                        + "create.*download.*update.*showall.*");
    }

    public void testUpdateOneArg() {
        HarvestTemplateApplication.main(new String[] {"update", "foo" });
        assertOutAndErrMatches("Should fail on missing parameter.",
                "", ".*update.*Wrong number\\(1\\) of arguments.*"
                + "create.*download.*update.*showall.*");
    }

    public void testUpdateNoTemplate() {
        HarvestTemplateApplication.main(new String[] {"update", "foo", "bar" });
        assertOutAndErrMatches("Should fail on missing parameter.",
                "^$",
                ".*There is no template named 'foo'. Use the create.*");
    }

    public void testUpdateNoFile() {
        HarvestTemplateApplication.main(new String[]
                {"update", "OneLevel-order", "missing-file" });
        assertOutAndErrMatches("Should fail on missing file.",
                "^$",
                ".*missing-file.*could not be read or is not valid xml.*");
    }

    public void testShowAll() {
        HarvestTemplateApplication.main(new String[] {"showall"});
        assertOutAndErrMatches("Should list all templates.",
                "^FullSite-order\n"
                        + "Max_20_2-order\n"
                        + "OneLevel-order\n"
                        + "default_orderxml\n$",
                "^$");
    }
}
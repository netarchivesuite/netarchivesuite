/*
 * #%L
 * Netarchivesuite - archive - test
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
package dk.netarkivet.archive.webinterface;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.servlet.jsp.PageContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.mockobjects.servlet.MockHttpServletRequest;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.webinterface.JspWriterMockup;
import dk.netarkivet.common.webinterface.WebinterfaceTestCase;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class BatchGUITester {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
    ReloadSettings rs = new ReloadSettings();

    @Before
    public void setUp() {
        mtf.setUp();
        rs.setUp();

        Settings.set(CommonSettings.BATCHJOBS_BASEDIR, TestInfo.BATCH_DIR.getAbsolutePath());
    }

    @After
    public void tearDown() {
        rs.tearDown();
        mtf.tearDown();
    }

    @Test
    public void testUtilityConstructor() {
        ReflectUtils.testUtilityConstructor(BatchGUI.class);
    }

    // TODO fix the problem with the 'MockHttpServletRequest', where the elements
    // are removed during validation.
    //
    // public void testNoClass() {
    // String ERROR_FILE_PATH = "ERROR IN CLASS PATH";
    // MockHttpServletRequest request = new MockHttpServletRequest(){
    // @Override
    // public Locale getLocale() {
    // return new Locale("da");
    // }
    // };
    // Locale l = new Locale("da");
    //
    // request.setupAddParameter(Constants.BATCHJOB_PARAMETER,
    // new String[]{ERROR_FILE_PATH});
    // try {
    // BatchGUI.getPageForClass(new WebinterfaceTestCase.TestPageContext(
    // request, null, l));
    // fail("an error in class path is not allowed!");
    // } catch (UnknownID e) {
    // assertTrue("The erroneous file name should be part of the message.",
    // e.getMessage().contains(ERROR_FILE_PATH));
    // }
    // }
    //
    // TODO fix the problem with the 'MockHttpServletRequest', where the elements
    // are removed during validation.
    //
    // public void testClassNotBatchJob() {
    // String ERROR_FILE_PATH = "ERROR IN CLASS PATH";
    // MockHttpServletRequest request = new MockHttpServletRequest(){
    // @Override
    // public Locale getLocale() {
    // return new Locale("en");
    // }
    // };
    // Locale l = new Locale("en");
    //
    // request.setupAddParameter(TestInfo.CONTEXT_CLASS_NAME,
    // new String[]{AdminDataMessage.class.getName()});
    // try {
    // BatchGUI.getPageForClass(new WebinterfaceTestCase.TestPageContext(
    // request, null, l));
    // fail("A class which is not a batchjob is not allowed!");
    // } catch (UnknownID e) {
    // // should hit a problem when looking for the 'arc-file' for the class.
    // assertTrue("A non-batchjob should be menchened by path.",
    // e.getMessage().contains(AdminDataMessage.class.getName()));
    // assertTrue("Should contains: 'Unknown or undefined classpath for batchjob"
    // + "', but was: " + e.getMessage(),
    // e.getMessage().contains("Unknown or undefined classpath for batchjob"));
    // }
    // }
    //
    // TODO fix the problem with the 'MockHttpServletRequest', where the elements
    // are removed during validation.
    //
    // public void testInitializationChecksumJob() {
    // MockHttpServletRequest request = new MockHttpServletRequest(){
    // @Override
    // public Locale getLocale() {
    // return new Locale("en");
    // }
    // };
    // Locale l = new Locale("en");
    // JspWriterMockup out = new JspWriterMockup();
    //
    // request.setupAddParameter(TestInfo.CONTEXT_CLASS_NAME,
    // new String[]{ChecksumJob.class.getName()});
    //
    // PageContext context = new WebinterfaceTestCase.TestPageContext(request, out, l);
    //
    // BatchGUI.getPageForClass(context);
    //
    // String content = out.sw.toString();
    //
    // assertTrue("Should contain the name of the batchjob, '" + ChecksumJob.class.getName()
    // + "', but was: \n" + content,
    // content.contains(ChecksumJob.class.getName()));
    // }
    //
    // TODO fix the problem with the 'MockHttpServletRequest', where the elements
    // are removed during validation.
    //
    // public void testInitialization2() {
    // MockHttpServletRequest request = new MockHttpServletRequest(){
    // @Override
    // public Locale getLocale() {
    // return new Locale("en");
    // }
    // };
    // Locale l = new Locale("en");
    // JspWriterMockup out = new JspWriterMockup();
    //
    // request.setupAddParameter(TestInfo.CONTEXT_CLASS_NAME,
    // new String[]{UrlSearch.class.getName()});
    //
    // PageContext context = new WebinterfaceTestCase.TestPageContext(request, out, l);
    //
    // BatchGUI.getPageForClass(context);
    // }

    @Test
    public void testOverviewPage() throws ArgumentNotValid, IOException {

        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override
            public Locale getLocale() {
                return new Locale("en");
            }

            @Override
            public int getRemotePort() {
                return 0; // To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public String getLocalName() {
                return null; // To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public String getLocalAddr() {
                return null; // To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public int getLocalPort() {
                return 0; // To change body of implemented methods use File | Settings | File Templates.
            }
        };
        Locale l = new Locale("en");
        JspWriterMockup out = new JspWriterMockup();

        PageContext context = new WebinterfaceTestCase.TestPageContext(request, out, l);

        BatchGUI.getBatchOverviewPage(context);
        // System.out.println(out.sw.toString());
    }

    /**
     * FIXME Fails in Hudson
     */
    @Test
    @Ignore("FIXME- the arcFile doesn't exist")
    /*java.lang.AssertionError: The given arcFile '/home/svc/devel/netarchivesuite/archive/archive-test/tests/dk/netarkivet/archive/webinterface/data/working/batch/MimeUrlSearch.jar' doesn't exist
	at org.junit.Assert.fail(Assert.java:88)
	at org.junit.Assert.assertTrue(Assert.java:41)
	at dk.netarkivet.archive.webinterface.BatchGUITester.failingTestExecute(BatchGUITester.java:253)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:47)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:44)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:271)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:70)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:50)
	at org.junit.runners.ParentRunner$3.run(ParentRunner.java:238)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:63)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:236)
	at org.junit.runners.ParentRunner.access$000(ParentRunner.java:53)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:229)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:309)
	at org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:50)
	at org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:38)
	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:459)
	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:675)
	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:382)
	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:192)

       */
    public void failingTestExecute() {
        File arcFile = new File(TestInfo.BATCH_DIR, "MimeUrlSearch.jar");
        assertTrue("The given arcFile '" +  arcFile.getAbsolutePath() + "' doesn't exist", arcFile.isFile());

        Settings.set("settings.common.batch.batchjobs.batchjob.class", "dk.netarkivet.common.utils.batch.UrlSearch");
        Settings.set("settings.common.batch.batchjobs.batchjob.jarfile", arcFile.getAbsolutePath());

        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override
            public Locale getLocale() {
                return new Locale("en");
            }

            @Override
            public int getRemotePort() {
                return 0; // To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public String getLocalName() {
                return null; // To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public String getLocalAddr() {
                return null; // To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public int getLocalPort() {
                return 0; // To change body of implemented methods use File | Settings | File Templates.
            }
        };
        request.setupAddParameter(Constants.FILETYPE_PARAMETER, BatchFileType.Metadata.toString());
        request.setupAddParameter(Constants.JOB_ID_PARAMETER, "1234567890");
        request.setupAddParameter(Constants.BATCHJOB_PARAMETER, "dk.netarkivet.common.utils.batch.UrlSearch");
        request.setupAddParameter(Constants.REPLICA_PARAMETER, "BarOne");
        request.setupAddParameter("arg1", "DUMMY-ARG");
        request.setupAddParameter("arg2", "url");
        request.setupAddParameter("arg3", "mimetype");

        Locale l = new Locale("en");
        JspWriterMockup out = new JspWriterMockup();

        PageContext context = new WebinterfaceTestCase.TestPageContext(request, out, l);
        BatchGUI.execute(context);
    }
}

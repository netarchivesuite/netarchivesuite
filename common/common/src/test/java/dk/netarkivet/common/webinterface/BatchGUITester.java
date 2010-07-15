/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.common.webinterface;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.servlet.jsp.PageContext;

import junit.framework.TestCase;

import com.mockobjects.servlet.MockHttpServletRequest;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.webinterface.WebinterfaceTestCase;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class BatchGUITester extends TestCase {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
    ReloadSettings rs = new ReloadSettings();
    
    public void setUp() {
        mtf.setUp();
        rs.setUp();
        
        Settings.set(CommonSettings.BATCHJOBS_BASEDIR, TestInfo.BATCH_DIR.getAbsolutePath());
    }
    
    public void tearDown() {
        rs.tearDown();
        mtf.tearDown();
    }

    public void testUtilityConstructor() {
        ReflectUtils.testUtilityConstructor(BatchGUI.class);
    }

//  TODO fix the problem with the 'MockHttpServletRequest', where the elements
//  are removed during validation.
//  
//    public void testNoClass() {
//        String ERROR_FILE_PATH = "ERROR IN CLASS PATH";
//        MockHttpServletRequest request = new MockHttpServletRequest(){
//            @Override
//            public Locale getLocale() {
//                return new Locale("da");
//            }
//        };
//        Locale l = new Locale("da");
//        
//        request.setupAddParameter(Constants.BATCHJOB_PARAMETER,
//                new String[]{ERROR_FILE_PATH});
//        try {
//            BatchGUI.getPageForClass(new WebinterfaceTestCase.TestPageContext(
//                    request, null, l));
//            fail("an error in class path is not allowed!");
//        } catch (UnknownID e) {
//            assertTrue("The erroneous file name should be part of the message.", 
//                    e.getMessage().contains(ERROR_FILE_PATH));
//        }
//    }
//    
//  TODO fix the problem with the 'MockHttpServletRequest', where the elements
//  are removed during validation.
//  
//    public void testClassNotBatchJob() {
//        String ERROR_FILE_PATH = "ERROR IN CLASS PATH";
//        MockHttpServletRequest request = new MockHttpServletRequest(){
//            @Override
//            public Locale getLocale() {
//                return new Locale("en");
//            }
//        };
//        Locale l = new Locale("en");
//        
//        request.setupAddParameter(TestInfo.CONTEXT_CLASS_NAME,
//                new String[]{AdminDataMessage.class.getName()});
//        try {
//            BatchGUI.getPageForClass(new WebinterfaceTestCase.TestPageContext(
//                    request, null, l));
//            fail("A class which is not a batchjob is not allowed!");
//        } catch (UnknownID e) {
//            // should hit a problem when looking for the 'arc-file' for the class.
//            assertTrue("A non-batchjob should be menchened by path.", 
//                    e.getMessage().contains(AdminDataMessage.class.getName()));
//            assertTrue("Should contains: 'Unknown or undefined classpath for batchjob"
//                    + "', but was: " + e.getMessage(), 
//                    e.getMessage().contains("Unknown or undefined classpath for batchjob"));
//        }
//    }
//    
//  TODO fix the problem with the 'MockHttpServletRequest', where the elements
//  are removed during validation.
//  
//    public void testInitializationChecksumJob() {
//        MockHttpServletRequest request = new MockHttpServletRequest(){
//            @Override
//            public Locale getLocale() {
//                return new Locale("en");
//            }
//        };
//        Locale l = new Locale("en");
//        JspWriterMockup out = new JspWriterMockup();
//        
//        request.setupAddParameter(TestInfo.CONTEXT_CLASS_NAME,
//                new String[]{ChecksumJob.class.getName()});
//        
//        PageContext context = new WebinterfaceTestCase.TestPageContext(request, out, l);
//
//        BatchGUI.getPageForClass(context);
//        
//        String content = out.sw.toString();
//        
//        assertTrue("Should contain the name of the batchjob, '" + ChecksumJob.class.getName() 
//                + "', but was: \n" + content, 
//                content.contains(ChecksumJob.class.getName()));
//    }
//    
//    TODO fix the problem with the 'MockHttpServletRequest', where the elements
//    are removed during validation.
//    
//    public void testInitialization2() {
//        MockHttpServletRequest request = new MockHttpServletRequest(){
//            @Override
//            public Locale getLocale() {
//                return new Locale("en");
//            }
//        };
//        Locale l = new Locale("en");
//        JspWriterMockup out = new JspWriterMockup();
//        
//        request.setupAddParameter(TestInfo.CONTEXT_CLASS_NAME,
//                new String[]{UrlSearch.class.getName()});
//        
//        PageContext context = new WebinterfaceTestCase.TestPageContext(request, out, l);
//
//        BatchGUI.getPageForClass(context);
//    }        
    
    public void testOverviewPage() throws ArgumentNotValid, IOException {

      MockHttpServletRequest request = new MockHttpServletRequest(){
          @Override
          public Locale getLocale() {
              return new Locale("en");
          }
      };
      Locale l = new Locale("en");
      JspWriterMockup out = new JspWriterMockup();
      
      PageContext context = new WebinterfaceTestCase.TestPageContext(request, out, l);

      BatchGUI.getBatchOverviewPage(context);
      System.out.println(out.sw.toString());
    }
    
    public void testExecute() {
        File arcFile = new File(TestInfo.BATCH_DIR, "MimeUrlSearch.jar");
        assertTrue(arcFile.isFile());
        
        Settings.set("settings.common.batch.batchjobs.batchjob.class", 
                "dk.netarkivet.common.utils.batch.UrlSearch");
        Settings.set("settings.common.batch.batchjobs.batchjob.jarfile",
                arcFile.getAbsolutePath());

        MockHttpServletRequest request = new MockHttpServletRequest(){
           @Override
           public Locale getLocale() {
               return new Locale("en");
           }
       };
       request.setupAddParameter(Constants.FILETYPE_PARAMETER, BatchFileType.Metadata.toString());
       request.setupAddParameter(Constants.JOB_ID_PARAMETER, "1234567890");
       request.setupAddParameter(Constants.BATCHJOB_PARAMETER, 
               "dk.netarkivet.common.utils.batch.UrlSearch");
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

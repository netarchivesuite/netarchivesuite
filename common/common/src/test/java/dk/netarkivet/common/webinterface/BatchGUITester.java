package dk.netarkivet.common.webinterface;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

import junit.framework.TestCase;

import com.mockobjects.servlet.MockHttpServletRequest;

import dk.netarkivet.archive.arcrepository.bitpreservation.AdminDataMessage;
import dk.netarkivet.archive.arcrepository.bitpreservation.ChecksumJob;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.UrlSearch;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.harvester.webinterface.WebinterfaceTestCase;
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
    }
}

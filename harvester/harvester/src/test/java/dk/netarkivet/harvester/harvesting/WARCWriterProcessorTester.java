package dk.netarkivet.harvester.harvesting;

import org.dom4j.Document;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.testutils.TestFileUtils;
import junit.framework.TestCase;

public class WARCWriterProcessorTester extends TestCase {

    private static final String DISK_PATH_XPATH =
            "//crawl-order/controller"
            + "/string[@name='disk-path']";
    
    @Override
    public void setUp(){
        TestFileUtils.copyDirectoryNonCVS(TestInfo.WARCPROCESSORFILES_DIR,
                TestInfo.WORKING_DIR);
    }
    
    @Override
    public void tearDown(){
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
    }
    
    
    public void testWriteWarcInfo() {
        // Change disk-path of order.xml to WORKING_DIR
        Document doc = XmlUtils.getXmlDoc(TestInfo.ORDER_FOR_TESTING_WARCINFO);
        
        XmlUtils.setNode(doc, DISK_PATH_XPATH,
                TestInfo.ORDER_FOR_TESTING_WARCINFO.getParentFile().getAbsolutePath()
                );
        XmlUtils.writeXmlToFile(doc, TestInfo.ORDER_FOR_TESTING_WARCINFO);

        WARCWriterProcessor p = new WARCWriterProcessor("testing");
        String output = p.getFirstrecordBody(TestInfo.ORDER_FOR_TESTING_WARCINFO);
        System.out.println(output);
    }
}

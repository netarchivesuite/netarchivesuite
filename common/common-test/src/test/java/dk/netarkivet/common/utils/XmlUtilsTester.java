package dk.netarkivet.common.utils;

import junit.framework.TestCase;
import org.dom4j.Document;
import org.dom4j.Node;

import dk.netarkivet.testutils.preconfigured.MoveTestFiles;


/**
 * Unit tests for the class XmlUtils.
 */
public class XmlUtilsTester extends TestCase {
    private final MoveTestFiles mtf = new MoveTestFiles(TestInfo.DATADIR,
            TestInfo.TEMPDIR);

    public XmlUtilsTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        mtf.setUp();
    }

    public void tearDown() throws Exception {
        mtf.tearDown();
        super.tearDown();
    }

    public void testSetNode() throws Exception {
        Document doc = XmlUtils.getXmlDoc(TestInfo.XML_FILE_1);
        Node node = doc.selectSingleNode(TestInfo.XML_FILE_1_XPATH_1);
        assertEquals("Should have original value at start",
                "Should go away", node.getText());
        XmlUtils.setNode(doc, TestInfo.XML_FILE_1_XPATH_1, "newValue");
        assertEquals("Should have new value after setting it",
                "newValue", node.getText());
    }
}
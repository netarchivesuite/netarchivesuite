package dk.netarkivet.common.tools;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class OrderXmlBuilder {
    private final Document orderxmlDoc;
    private static DocumentBuilder builder;

    public OrderXmlBuilder() {
        orderxmlDoc = getParser().newDocument();
    }

    public org.dom4j.Document getOrderXml() {
        org.dom4j.io.DOMReader reader = new org.dom4j.io.DOMReader();
        return reader.read(orderxmlDoc);
    }

    public static OrderXmlBuilder create() {
        return new OrderXmlBuilder();
    }

    public OrderXmlBuilder enableDeduplication() {
        Node deduplicationNode = NodeTraverser.create(orderxmlDoc)
                .getChildNode("crawl-order", null)
                .getChildNode("controller", null)
                .getChildNode("map", "write-processors")
                .getChildNode("newObject", "DeDuplicator")
                .getChildNode("boolean", "enabled")
                .getNode();
        deduplicationNode.setTextContent("true");
        return this;
    }

    private static synchronized DocumentBuilder getParser() {
        if(builder == null) {
            try {
                builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            }
        } return builder;
    }
}

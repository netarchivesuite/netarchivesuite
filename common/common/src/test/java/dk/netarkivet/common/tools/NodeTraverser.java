package dk.netarkivet.common.tools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NodeTraverser {
    private final Document doc;
    private Node currentNode;

    public NodeTraverser(Document doc) {
        this.doc = doc;
        currentNode = doc;
    }

    public Node getNode() {
        return currentNode;
    }

    public static NodeTraverser create(Document doc) {
        return new NodeTraverser(doc);
    }

    public NodeTraverser getChildNode(String element, String name) {
        Node childNode = null;
        NodeList nodes = currentNode.getChildNodes();
        if (nodes != null) {
            for (int i = 0 ; i < nodes.getLength() ; i++) {
                Node node = nodes.item(i);
                if ((name == null || name.equals(node.getNodeName())) &&
                        element.equals(node.getAttributes().getNamedItem("name"))) {
                    childNode = node;
                    break;
                }
            }
        }
        if (childNode == null) {
            Element newNode = doc.createElement(element);
            if (name != null) {
                newNode.setAttribute("name", name);
            }
            currentNode.appendChild(newNode);
            childNode = newNode;

        }
        currentNode = childNode;

        return this;
    }
}

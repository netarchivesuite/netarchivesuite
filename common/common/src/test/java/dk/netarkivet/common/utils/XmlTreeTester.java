/* $Id$
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
package dk.netarkivet.common.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import org.dom4j.Document;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.testutils.CollectionAsserts;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

/**
 * Unit tests for the class XmlTree.
 */
public class XmlTreeTester extends TestCase {
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.DATADIR,
                                                  TestInfo.TEMPDIR);

    public XmlTreeTester(String s) {
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

    public void testGetStringTree() {
        StringTree<String> tree1 = getTree();
        assertNotNull("Should get non-null tree", tree1);
        //<dk> 
        //  <netarkivet> 
        //    <test>
        //      <list1>item1</list1>
        //      <list1>item2</list1>
        //      <list1>item3</list1>
        //      <q> what is the question </q>
        //    </test> ...
        assertEquals("Should have node from backing XML",
                     "what is the question",
                     tree1.getSubTree("dk").getSubTrees("netarkivet")
                             .get(0).getSubTree("test")
                             .getSubTree("q").getValue());

        try {
            XmlTree.getStringTree(null);
            fail("Should die on null argument");
        } catch (ArgumentNotValid e) {
            // expected
        }
    }

    public void testGetSubTree() {
        StringTree<String> tree1 = getTree();

        tree1 = tree1.getSubTree("dk");
        assertNotNull("Should have non-null direct subtree", tree1);
        tree1 = tree1.getSubTrees("netarkivet").get(1).getSubTree("answer");
        assertNotNull("Should have non-null direct subtree", tree1);

        tree1 = getTree();
        // Test dotted paths
        assertNotNull("Should have non-null dotted subtree",
                      tree1.getSubTree("dk.heroes.hero"));
        // Test it can find the right way to the subtree
        assertNotNull("Should have non-null dotted subtree",
                      tree1.getSubTree("dk.netarkivet.test"));


        tree1 = tree1.getSubTree("dk");
        try {
            tree1.getSubTree("netarkivet");
            fail("Should die on multiple subtrees");
        } catch (IllegalState e) {
            // expected
        }
        try {
            tree1.getSubTree("netarkvet");
            fail("Should die on no subtrees");
        } catch (IllegalState e) {
            // expected
        }
    }

    public void testGetValue() {
        StringTree<String> tree1 = getTree();
        tree1 = tree1.getSubTree("dk").getSubTrees("netarkivet").get(0)
                .getSubTree("answer");
        assertEquals("Should have value in leaf node",
                     "42", tree1.getValue());

        tree1 = getTree();
        try {
            tree1.getValue();
            fail("Should throw IllegalState when getValue()ing a root");
        } catch (IllegalState e) {
            // expected
        }
        tree1 = tree1.getSubTree("dk");
        try {
            tree1.getValue();
            fail("Should throw IllegalState when getValue()ing a node");
        } catch (IllegalState e) {
            // expected
        }

        tree1 = getTree();
        try {
            tree1.getValue("dk");
            fail("Should die when the named node is the root");
        } catch (IllegalState e) {
            // Expected
        }

        // Test dotted paths
        assertEquals("Should get value in unique leaf node",
                     "Batman", tree1.getValue("dk.heroes.hero"));

        try {
            tree1.getValue("dk.netarkivet.answer");
            fail("Should fail due to multiple subtrees");
        } catch (IllegalState e) {
            // Expected
        }

        try {
            tree1.getValue("dk.heroes");
            fail("Should fail due to non-leafness");
        } catch (IllegalState e) {
            // Expected
        }

        try {
            tree1.getValue("dk.heroes.bystander");
            fail("Should fail due to missing leaf");
        } catch (IllegalState e) {
            // Expected
        }

        tree1 = tree1.getSubTree("dk");

        tree1 = tree1.getSubTrees("netarkivet").get(0);
        assertEquals("Should be possible to get value of subnode",
                     "42", tree1.getValue("answer"));
        try {
            tree1.getValue("test");
            fail("Should die when the named node is not a leaf");
        } catch (IllegalState e) {
            // Expected
        }
        try {
            tree1.getValue("foobar");
            fail("Should die when the named node does not exist");
        } catch (IllegalState e) {
            // Expected
        }
        try {
            tree1.getSubTree("test").getValue("list1");
            fail("Should die when the named node has multiple candidates");
        } catch (IllegalState e) {
            // Expected
        }


    }

    /** Get the default tree from the test.xml file */
    private StringTree<String> getTree() {
        Document doc = XmlUtils.getXmlDoc(TestInfo.TESTXML);
        StringTree<String> tree1 = XmlTree.getStringTree(doc);
        return tree1;
    }

    public void testIsLeaf() {
        StringTree<String> tree1 = getTree();
        tree1 = tree1.getSubTree("dk").getSubTrees("netarkivet").get(0)
                .getSubTree("answer");
        assertTrue("Should have true on leaf node",
                   tree1.isLeaf());

        tree1 = getTree();
        assertFalse("Should have false on root node",
                    tree1.isLeaf());
        assertFalse("Should have false on non-leaf node",
                    tree1.getSubTree("dk").isLeaf());
    }

    public void testGetSubTrees() {
        StringTree<String> tree1 = getTree();
        List<StringTree<String>> rootTrees = tree1.getSubTrees("dk");
        assertEquals("Should get subtrees on root",
                     1, rootTrees.size());
        List<StringTree<String>> subTrees = rootTrees.get(0)
                .getSubTrees("netarkivet");
        assertEquals("Should have found two netarkivet subtrees",
                     2, subTrees.size());
        assertEquals("Tree one should have a 42 answer",
                     "42", subTrees.get(0).getSubTree("answer").getValue());
        assertEquals("Tree two should have a 43 answer",
                     "43", subTrees.get(1).getSubTree("answer").getValue());
        List<StringTree<String>> nonTrees = tree1.getSubTrees("fop");
        assertEquals("Should have found no fop subtrees", 0, nonTrees.size());
        try {
            tree1.getSubTree("dk").getSubTrees("netarkivet").get(0)
                    .getSubTree("answer").getSubTrees("foo");
            fail("Should throw IllegalState on asking for subtree in leaf");
        } catch (IllegalState e) {
            // expected
        }

        assertEquals("Should find 2 netarkivet subtrees with dotted path",
                     2, tree1.getSubTrees("dk.netarkivet").size());
        subTrees = tree1.getSubTrees(
                "dk.netarkivet.answer");
        assertEquals("Should find 2 answer subtrees with dotted path",
                     2, subTrees.size());
        assertEquals("The first answer should be 42", "42",
                     subTrees.get(0).getValue());
        assertEquals("The second answer should be 43", "43",
                     subTrees.get(1).getValue());

        // This one taken from HTMLUtils
        Document doc = XmlUtils.getXmlDoc(TestInfo.SETTINGS_FILE);
        tree1 = XmlTree.getStringTree(doc);
        StringTree<String> subTree = tree1.getSubTree("settings");
        assertNotNull("Should find settings subtree", subTree);
        assertNotNull("Should find common subtree of settings in multimap",
                      subTree.getChildMap().get("common"));
        assertNotNull("Should find common subtree of settings",
                      subTree.getSubTree("common"));
        assertNotNull("Should find webinterface subtree of common",
                      subTree.getSubTree("common").getSubTrees("webinterface"));

        List<StringTree<String>> languages
                = tree1.getSubTree("settings.common.webinterface").
                getSubTrees("language");
        assertEquals("Should have found two language objects",
                     2, languages.size());
    }

    public void testGetChildMultimap() {
        StringTree<String> tree1 = getTree();
        final Map<String, List<StringTree<String>>> rootChildren = tree1
                .getChildMultimap();
        assertEquals("Should be able to get child map of the root element",
                     1, rootChildren.size());
        tree1 = tree1.getSubTree("dk");
        Map<String, List<StringTree<String>>> children =
                tree1.getChildMultimap();
        assertEquals("Multimap should have one entry",
                     2, children.size());
        assertEquals("Netarkivet child should have two entries",
                     2, children.get("netarkivet").size());
        StringTree<String> tree2 = children.get("netarkivet").get(0);
        tree2 = tree2.getSubTree("test");
        children = tree2.getChildMultimap();
        assertEquals("Multimap should have two entries",
                     2, children.size());
        assertTrue("Should have list1 key",
                   children.containsKey("list1"));
        assertTrue("Should have q key",
                   children.containsKey("q"));
        try {
            children.get("q").get(0).getChildMultimap();
            fail("Should have thrown error on leaf");
        } catch (IllegalState e) {
            // expected
        }
    }

    public void testGetLeafMultimap() {
        StringTree<String> tree1 = getTree();
        try {
            tree1.getLeafMultimap();
            fail("Should get error when no children are leafs");
        } catch (IllegalState e) {
            // Expected
        }
        tree1 = tree1.getSubTree("dk").getSubTrees("netarkivet").get(0);
        try {
            tree1.getLeafMultimap();
            fail("Should get error when some children are not leafs");
        } catch (IllegalState e) {
            // Expected
        }
        tree1 = tree1.getSubTree("test");
        Map<String, List<String>> leaves = tree1.getLeafMultimap();
        assertEquals("Should have two leaves",
                     2, leaves.size());
        assertTrue("Should have list1 key", leaves.containsKey("list1"));
        assertTrue("Should have q key", leaves.containsKey("q"));
        assertEquals("list1 should have three elements",
                     3, leaves.get("list1").size());
        CollectionAsserts.assertListEquals("list1 should have right leaves",
                                           leaves.get("list1"),
                                           "item1", "item2", "item3");
        CollectionAsserts.assertListEquals("q should have right leaf",
                                           leaves.get("q"),
                                           "what is the question");
        tree1 = tree1.getSubTree("q");
        try {
            tree1.getLeafMultimap();
            fail("Should fail when there are no children");
        } catch (IllegalState e) {
            // Expected
        }
    }

    public void testGetChildMap() {
        StringTree<String> tree1 = getTree();
        final Map<String, StringTree<String>> rootChildren = tree1
                .getChildMap();
        assertEquals("Should be able to get child map of the root element",
                     1, rootChildren.size());
        tree1 = tree1.getSubTree("dk");
        try {
            tree1.getChildMap();
            fail("Should not be allowed to get childmap of node with "
                 + "several of the same subnode.");
        } catch (IllegalState e) {
            // Expected
        }
        StringTree<String> tree2 = tree1.getSubTrees("netarkivet").get(0);
        Map<String, StringTree<String>> children = tree2.getChildMap();
        assertEquals("Map should have two entries",
                     2, children.size());
        assertTrue("Should have test key",
                   children.containsKey("test"));
        assertTrue("Should have answer key",
                   children.containsKey("answer"));
        try {
            children.get("answer").getChildMap();
            fail("Should have thrown error on leaf");
        } catch (IllegalState e) {
            // expected
        }
    }

    public void testGetLeafMap() {
        StringTree<String> tree1 = getTree();
        try {
            tree1.getLeafMap();
            fail("Should get error when no children are leafs");
        } catch (IllegalState e) {
            // Expected
        }
        tree1 = tree1.getSubTree("dk").getSubTrees("netarkivet").get(0);
        try {
            tree1.getLeafMap();
            fail("Should get error when some children are not leafs");
        } catch (IllegalState e) {
            // Expected
        }

        tree1 = getTree();
        tree1 = tree1.getSubTree("dk").getSubTrees("netarkivet").get(1);
        Map<String, String> leaves = tree1.getLeafMap();
        assertEquals("Should have one leaf",
                     1, leaves.size());
        assertTrue("Should have answer key", leaves.containsKey("answer"));
        assertEquals("Should have right answer",
                     "43", leaves.get("answer"));
        tree1 = tree1.getSubTree("answer");
        try {
            tree1.getLeafMap();
            fail("Should fail when there are no children");
        } catch (IllegalState e) {
            // Expected
        }
        tree1 = getTree();
        tree1 = tree1.getSubTree("dk").getSubTree("heroes");
        leaves = tree1.getLeafMap();
        assertEquals("Should have three leaves",
                     3, leaves.size());
        assertEquals("Should have right hero",
                     "Batman", leaves.get("hero"));
        assertEquals("Should have right sidekick",
                     "Robin", leaves.get("sidekick"));
        assertEquals("Should have right villain",
                     "Dr. Evil", leaves.get("villain"));

    }

    public void testSelectSingleNode() throws Exception {
        Method selectSingleNode = ReflectUtils.getPrivateMethod(XmlTree.class,
                                                              "selectSingleNode",
                                                              String.class);
        StringTree<String> tree1 = getTree();
        assertNotNull("Should be able to get root node",
                      selectSingleNode.invoke(tree1, "dk"));
        assertNotNull("Should be able to get two levels of nodes",
                      selectSingleNode.invoke(tree1, "dk.heroes"));
        try {
            selectSingleNode.invoke(tree1, "dk/heroes");
            fail("Should fail on invalid syntax");
        } catch (InvocationTargetException e) {
            // Expected
        }
        try {
            selectSingleNode.invoke(tree1, "dk.netarkivet");
            fail("Should fail on multiple possibilities");
        } catch (InvocationTargetException e) {
            // Expected
        }
        try {
            selectSingleNode.invoke(tree1, "");
            fail("Should fail on empty parameter");
        } catch (InvocationTargetException e) {
            // Expected
        }
    }
}

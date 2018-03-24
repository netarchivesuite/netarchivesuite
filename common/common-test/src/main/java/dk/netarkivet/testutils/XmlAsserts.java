/*
 * #%L
 * Netarchivesuite - common - test
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

package dk.netarkivet.testutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.dom4j.Document;
import org.dom4j.Node;

/**
 * Helper methods for asserts in Xml documents.
 */
public class XmlAsserts {
    public static void assertNodeWithXpath(Document doc, String xpath) {
        Node theNode = doc.selectSingleNode(xpath);
        if (theNode == null) {
            fail("the Node with xpath '" + xpath + "' should be present");
        }
    }

    public static void assertNodeTextInXpath(String message, Document doc, String xpath, String expected) {
        Node dedup_index_node = doc.selectSingleNode(xpath);
        assertEquals(message, expected, dedup_index_node.getText().trim());
    }
}

/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Utilities for handling XML-files.
 *
 */
public class XmlUtils {

    private static Log log = LogFactory.getLog(XmlUtils.class.getName());

   /** Read and parse an XML-file, and return
     * a Document object representing this object.
     * @param f a given xml file
     * @return a Document representing the xml-file
     * @throws IOFailure if unable to read the xml-file
     *          or unable to parse the file as XML
     */
    public static Document getXmlDoc(File f) throws IOFailure {
        SAXReader reader = new SAXReader();
        if (!f.canRead()) {
            log.debug("Could not read file: " + f);
            throw new IOFailure("Could not read file:" + f);
        }

        try {
            return reader.read(f);
        } catch (DocumentException e) {
            log.warn("Could not parse file as XML: " + f);
            throw new IOFailure("Could not parse file as XML:" + f);
        }
   }

    /**
     * Set a XmlNode defined by the given XPath to the given value.
     *
     * @param doc   the Document, which is being modified
     * @param xpath the given XPath
     * @param value the given value
     */
    public static void setNode(Document doc, String xpath, String value) {
        Node xpathNode = doc.selectSingleNode(xpath);
        if (xpathNode == null) {
            throw new IOFailure("Element " + xpath
                                + "could not be found in the document '"
                                + doc.getRootElement().getName() + "'!");
        }
        xpathNode.setText(value);
    }
}

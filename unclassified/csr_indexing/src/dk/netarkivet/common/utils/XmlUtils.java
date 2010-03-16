/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
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
     * @return a Document representing the xml file
     * @throws IOFailure if unable to read the xml file
     *          or unable to parse the file as XML
     */
    public static Document getXmlDoc(File f) throws IOFailure {
        ArgumentNotValid.checkNotNull(f, "File f");
        SAXReader reader = new SAXReader();
        if (!f.canRead()) {
            log.debug("Could not read file: '" + f + "'");
            throw new IOFailure("Could not read file: '" + f + "'");
        }

        try {
            return reader.read(f);
        } catch (DocumentException e) {
            log.warn("Could not parse the file as XML: '" + f + "'", e);
            throw new IOFailure(
                    "Could not parse the file as XML: '" + f + "'", e);
        }
   }

    /** Read and parse an XML stream, and return
     * a Document object representing this object.
     * @param resourceAsStream a given xml document
     * @return a Document representing the xml document
     * @throws IOFailure if unable to read the xml document
     *          or unable to parse the document as XML
     */
    public static Document getXmlDoc(InputStream resourceAsStream) {
        ArgumentNotValid.checkNotNull(resourceAsStream,
                "InputStream resourceAsStream");
        SAXReader reader = new SAXReader();
        try {
            return reader.read(resourceAsStream);
        } catch (DocumentException e) {
            log.warn("Could not parse inputstream as XML: "
                    + resourceAsStream, e);
            throw new IOFailure(
                    "Could not parse inputstream as XML:"
                                + resourceAsStream, e);
        }
    }

    /**
     * Set a XmlNode defined by the given XPath to the given value.
     *
     * @param doc   the Document, which is being modified
     * @param xpath the given XPath
     * @param value the given value
     * @throws IOFailure If the given XPath was not found in the document
     */
    public static void setNode(Document doc, String xpath, String value) {
        ArgumentNotValid.checkNotNull(doc, "Document doc");
        ArgumentNotValid.checkNotNullOrEmpty(xpath, "String xpath");
        ArgumentNotValid.checkNotNull(value, "String value");
        
        
        Node xpathNode = doc.selectSingleNode(xpath);
        if (xpathNode == null) {
            throw new IOFailure("Element '" + xpath
                                + "' could not be found in the document '"
                                + doc.getRootElement().getName() + "'!");
        }
        xpathNode.setText(value);
    }
    
    /**
     * Validate that the settings xml files conforms to the XSD.
     *
     * @param xsdFile Schema to check settings against.
     * @throws ArgumentNotValid if unable to validate the settings files
     * @throws IOFailure If unable to read the settings files and/or 
     * the xsd file.
     */
    public static void validateWithXSD(File xsdFile) {
        ArgumentNotValid.checkNotNull(xsdFile, "File xsdFile");
        List<File> settingsFiles = Settings.getSettingsFiles();
        for (File settingsFile : settingsFiles) {
            try {
                DocumentBuilderFactory builderFactory
                        = DocumentBuilderFactory.newInstance();
                builderFactory.setNamespaceAware(true);
                DocumentBuilder parser = builderFactory
                        .newDocumentBuilder();
                org.w3c.dom.Document document = parser.parse(settingsFile);

                // create a SchemaFactory capable of understanding WXS schemas
                SchemaFactory factory = SchemaFactory
                        .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

                // load a WXS schema, represented by a Schema instance
                Source schemaFile = new StreamSource(xsdFile);
                Schema schema = factory.newSchema(schemaFile);

                // create a Validator instance, which can be used to validate an
                // instance document
                Validator validator = schema.newValidator();

                // validate the DOM tree
                try {
                    validator.validate(new DOMSource(document));
                } catch (SAXException e) {
                    // instance document is invalid!
                    final String msg = "Settings file '" + settingsFile
                            + "' does not validate using '" + xsdFile + "'";
                    log.warn(msg, e);
                    throw new ArgumentNotValid(msg, e);
                }
            } catch (IOException e) {
                throw new IOFailure("Error while validating: ", e);
            } catch (ParserConfigurationException e) {
                final String msg = "Error validating settings file '"
                        + settingsFile + "'";
                log.warn(msg, e);
                throw new ArgumentNotValid(msg, e);
            } catch (SAXException e) {
                final String msg = "Error validating settings file '"
                        + settingsFile + "'";
                log.warn(msg, e);
                throw new ArgumentNotValid(msg, e);
            }
        }
    }

    /**
     * Write document tree to file.
     *
     * @param doc the document tree to save.
     * @param f the file to write the document to.
     * @throws IOFailure On trouble writing XML file to disk.
     */
    public static void writeXmlToFile(Document doc, File f) throws IOFailure {
        FileOutputStream fos = null;
        try {
            try {
                fos = new FileOutputStream(f);
                StreamUtils.writeXmlToStream(doc, fos);
            } finally {
                if (fos != null) {
                    fos.close();
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Unable to write XML to file '"
                                + f.getAbsolutePath() + "'", e);
        }
    }
}

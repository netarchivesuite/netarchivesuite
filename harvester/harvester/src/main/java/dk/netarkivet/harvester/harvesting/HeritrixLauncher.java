/* File:     $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
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

package dk.netarkivet.harvester.harvesting;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Node;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.HeritrixTemplate;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;

/**
 * A HeritrixLauncher object wraps around an instance of the web crawler
 * Heritrix. The object is constructed with the necessary information to do a
 * crawl. The crawl is performed when doOneCrawl() is called. doOneCrawl()
 * monitors progress and returns when the crawl is finished or must be stopped
 * because it has stalled.
 */
public abstract class HeritrixLauncher {
    /** Class encapsulating placement of various files. */
    private HeritrixFiles files;

    /** the arguments passed to the HeritricController constructor. */
    private Object[] args;

    /**
     * The period to wait in seconds before checking if Heritrix has done
     * anything.
     */
    protected static final int CRAWL_CONTROL_WAIT_PERIOD 
        = Settings.getInt(HarvesterSettings.CRAWL_LOOP_WAIT_TIME);

    //Attributes regarding deduplication.

    /** Xpath for the deduplicator index directory node in order.xml 
     * documents. */
    static final String DEDUPLICATOR_INDEX_LOCATION_XPATH
            = HeritrixTemplate.DEDUPLICATOR_XPATH
              + "/string[@name='index-location']";

    /**
     * Xpath for the boolean telling if the deduplicator is enabled in order.xml
     * documents.
     */
    static final String DEDUPLICATOR_ENABLED
            = HeritrixTemplate.DEDUPLICATOR_XPATH + "/boolean[@name='enabled']";

    /** The class logger. */
    final Log log = LogFactory.getLog(getClass());
    
    /**
     * The JMS connection used to send {@link CrawlProgressMessage}s.
     */
    private final JMSConnection jmsConnection = 
        JMSConnectionFactory.getInstance();

    /** Xpath for the 'disk-path' in the order.xml . */
    private static final String DISK_PATH_XPATH =
            "//crawl-order/controller"
            + "/string[@name='disk-path']";
    /** Xpath for the arcfile 'prefix' in the order.xml . */
    private static final String ARCFILE_PREFIX_XPATH =
            "//crawl-order/controller"
            + "/map[@name='write-processors']"
            + "/newObject/string[@name='prefix']";
    /** Xpath for the ARCs dir in the order.xml. */
    private static final String ARCSDIR_XPATH =
            "//crawl-order/controller"
            + "/map[@name='write-processors']"
            + "/newObject/stringList[@name='path']/string";
    /** Xpath for the 'seedsfile' in the order.xml. */
    private static final String SEEDS_FILE_XPATH =
            "//crawl-order/controller"
            + "/newObject[@name='scope']"
            + "/string[@name='seedsfile']";

    /**
     * Private HeritrixLaucher constructor. Sets up the HeritrixLauncher from
     * the given order file and seedsfile.
     *
     * @param files Object encapsulating location of Heritrix crawldir and
     *              configuration files.
     *
     * @throws ArgumentNotValid If either seedsfile or orderfile does not
     *                          exist.
     */
    protected HeritrixLauncher(HeritrixFiles files) throws ArgumentNotValid {
        if (!files.getOrderXmlFile().isFile()) {
            throw new ArgumentNotValid(
                    "File '" + files.getOrderXmlFile().getName()
                    + "' must exist in order for Heritrix to run. "
                    + "This filepath does not refer to existing file: "
                    + files.getOrderXmlFile().getAbsolutePath());
        }
        if (!files.getSeedsTxtFile().isFile()) {
            throw new ArgumentNotValid(
                    "File '" + files.getSeedsTxtFile().getName()
                    + "' must exist in order for Heritrix to run. "
                    + "This filepath does not refer to existing file: "
                    + files.getSeedsTxtFile().getAbsolutePath());
        }
        this.files = files;
        this.args = new Object[]{files};
    }

    /**
     * Generic constructor to allow HeritrixLauncher to use any implementation
     * of HeritrixController.
     *
     * @param args the arguments to be passed to the constructor or non-static
     *             factory method of the HeritrixController class specified in
     *             settings
     */
    public HeritrixLauncher(Object... args) {
        this.args = args;
    }

    /**
     * Launches the crawl and monitors its progress.
     * @throws IOFailure
     */
    public abstract void doCrawl() throws IOFailure;

    /**
     * This method prepares the orderfile used by the Heritrix crawler. </p> 1.
     * alters the orderfile in the following-way: (overriding whatever is in the
     * orderfile)</br> <ol> <li>sets the disk-path to the outputdir specified in
     * HeritrixFiles.</li> <li>sets the seedsfile to the seedsfile specified in
     * HeritrixFiles.</li> <li>sets the prefix of the arcfiles to unique prefix
     * defined in HeritrixFiles</li> <li>checks that the arcs-file dir is 'arcs'
     * - to ensure that we know where the arc-files are when crawl
     * finishes</li>
     *
     * <li>if deduplication is enabled, sets the node pointing to index
     * directory for deduplication (see step 3)</li> </ol> 2. saves the
     * orderfile back to disk</p>
     *
     * 3. if deduplication is enabled in the order.xml, it writes the absolute
     * path of the lucene index used by the deduplication processor.
     *
     * @throws IOFailure - When the orderfile could not be saved to disk When a
     *                   specific node is not found in the XML-document When the
     *                   SAXReader cannot parse the XML
     */
    public void setupOrderfile() throws IOFailure {
        Document doc = XmlUtils.getXmlDoc(files.getOrderXmlFile());
        XmlUtils.setNode(doc, DISK_PATH_XPATH,
                         files.getCrawlDir().getAbsolutePath());

        XmlUtils.setNode(doc, ARCFILE_PREFIX_XPATH, files.getArcFilePrefix());

        XmlUtils.setNode(doc, SEEDS_FILE_XPATH,
                         files.getSeedsTxtFile().getAbsolutePath());
        XmlUtils.setNode(doc, ARCSDIR_XPATH, Constants.ARCDIRECTORY_NAME);

        if (isDeduplicationEnabledInTemplate(doc)) {
            XmlUtils.setNode(doc, DEDUPLICATOR_INDEX_LOCATION_XPATH,
                             files.getIndexDir().getAbsolutePath());
        }

        files.writeOrderXml(doc);
    }

    /**
     * Return true if the given order.xml file has deduplication enabled.
     *
     * @param doc An order.xml document
     *
     * @return True if Deduplicator is enabled.
     */
    public static boolean isDeduplicationEnabledInTemplate(Document doc) {
        ArgumentNotValid.checkNotNull(doc, "Document doc");
        Node xpathNode = doc.selectSingleNode(DEDUPLICATOR_ENABLED);
        return xpathNode != null
               && xpathNode.getText().trim().equals("true");

    }
    
    /**
     * @return an instance of the wrapper class for Heritrix files.
     */
    protected HeritrixFiles getHeritrixFiles() {
        return files;
    }
    
    /**
     * @return the optional arguments used to initialize 
     * the chosen Heritrix controller implementation.
     */
    protected Object[] getControllerArguments() {
        return args;
    }

    /**
     * @return the JMS connection used to send messages.
     */
    public JMSConnection getJMSConnection() {
        return jmsConnection;
    }

}

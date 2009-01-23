package dk.netarkivet.deploy2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.StreamUtils;

/**
 * Class for combining the different setting files into a 
 * complete settings file.
 */
public class BuildCompleteSettings {
    /**
     * Run the program.
     * This loads and merges all the setting files into a single file.
     * 
     * @param args Unused arguments.
     * @throws IOException For input/output errors.
     */
    public static void main(String[] args) throws IOException {
        XmlStructure settings = null;
        for (String path: Constants.BUILD_SETTING_FILES) {
/*            InputStream is = getSettingsFileAsStream(path);
            File tmpFile = File.createTempFile("tmp", "tmp");
            StreamUtils.copyInputStreamToOutputStream(is, 
                    new FileOutputStream(tmpFile));
*/
            File tmpFile = FileUtils.getResourceFileFromClassPath(path);
            if (settings == null) {
                settings = new XmlStructure(tmpFile);
            } else {
                Element elem = retrieveXmlSettingsTree(tmpFile);
                if (elem != null) {
                    settings.overWrite(elem);
                } else {
                    System.out.println("Cannot overwrite!!");
                }
            }
        }

        // make settings file.
        File completeSettings = new File(
                Constants.BUILD_COMPLETE_SETTINGS_FILE_PATH);
        FileWriter fw = new FileWriter(completeSettings);
        // write settings to file.
        fw.append(settings.getXML());
        fw.append("\n");
        fw.close();
    }
    
    /**
     * Retrieves the main element from the file.
     * 
     * @param settingFile The file to load into an Element.
     * @return The root of the XML structure of the settings file. 
     */
    private static Element retrieveXmlSettingsTree(File settingFile) {
        try {
            Document doc;
            SAXReader reader = new SAXReader();
            if (settingFile.canRead()) {
                doc =  reader.read(settingFile);
                return doc.getRootElement();
            } else {
                System.out.println("Cannot read file: " 
                        + settingFile.getAbsolutePath());
            }
        } catch (DocumentException e) {
            System.err.println("Problems with file: " 
                    + settingFile.getAbsolutePath());
        }
        return null;
    }
}

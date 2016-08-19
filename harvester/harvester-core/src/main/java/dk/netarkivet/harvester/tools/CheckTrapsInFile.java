package dk.netarkivet.harvester.tools;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.netarchivesuite.heritrix3wrapper.xmlutils.XmlEntityResolver;
import org.netarchivesuite.heritrix3wrapper.xmlutils.XmlErrorHandler;
import org.netarchivesuite.heritrix3wrapper.xmlutils.XmlValidationResult;
import org.netarchivesuite.heritrix3wrapper.xmlutils.XmlValidator;

public class CheckTrapsInFile {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Missing trapsfile argument");
            System.exit(1);
        }
        File trapsFile = new File(args[0]);
        if (!trapsFile.isFile()) {
            System.err.println("trapsfile argument '" +  trapsFile + "' does not exist");
            System.exit(1);
        }

        BufferedReader fr = null;
        String line=null;
        String trimmedLine=null;

        try {
            fr = new BufferedReader(new FileReader(trapsFile));
            while ((line = fr.readLine()) != null) {
                trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) {
                    continue;
                }
                if (isCrawlertrapsWelleformedXML(trimmedLine)) {
                    System.out.println("OK: crawlertrap '" + trimmedLine + "' is wellformed");
                }  else {
                    System.out.println("BAD: crawlertrap '" + trimmedLine + "' is not wellformed");
                }
            }
        } finally {
            IOUtils.closeQuietly(fr);
        }
    }

    private static boolean isCrawlertrapsWelleformedXML(String... lines ) {
        String prefix = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><values>";
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        for (String trimmedLine: lines) {
            sb.append("<value>" + trimmedLine + "</value>");
        }
        
        String end = "</values>";
        
        sb.append(end);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(sb.toString().getBytes());
        try {
            XmlValidator xmlValidator = new XmlValidator();
            XmlEntityResolver entityResolver = null;
            XmlErrorHandler errorHandler = new XmlErrorHandler();
            XmlValidationResult result = new XmlValidationResult();
            return xmlValidator.testStructuralValidity(bais, entityResolver, errorHandler, result);
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
        
    }

}

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ResumptionTokenTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        String sample = "<resumptionToken cursor=\"100\" completeListSize=\"421653\">oai_dc/421653/56199935/200/0/447/x/x/x</resumptionToken>";
        String pattern = "(?i)<resumptionToken\\s*cursor=\"[0-9]+\"\\s*completeListSize=\"[0-9]+\">\\s*(.*)</resumptionToken>";
        Matcher m = Pattern.compile(pattern).matcher(sample);
        if (m.find()) {
            System.out.println("PURE pattern found" + m.group(1) );
            
        }
    }

}

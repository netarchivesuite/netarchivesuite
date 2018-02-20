package dk.netarkivet.heritrix3.monitor;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import dk.netarkivet.heritrix3.monitor.AcceptLanguageParser.AcceptLanguage;

public class TestAcceptLanguageHeader extends Mockito {

    @Test
    public void test_parse() {
        HttpServletRequest request;
        List<AcceptLanguage> acceptLanguages;

        Object[][] cases = new Object[][] {
            {null, new Object[][] {
            }},
            {"", new Object[][] {
            }},
            {"prick!", new Object[][] {
            }},
            {"da", new Object[][] {
                {"da", null, "da", 1.0f}
            }},
            {"da;", new Object[][] {
                {"da", null, "da", 1.0f}
            }},
            {" da ", new Object[][] {
                {"da", null, "da", 1.0f}
            }},
            {"da ;", new Object[][] {
                {"da", null, "da", 1.0f}
            }},
            {"da ;,", new Object[][] {
                {"da", null, "da", 1.0f}
            }},
            {"da ; ,", new Object[][] {
                {"da", null, "da", 1.0f}
            }},
            {"da, ", new Object[][] {
                {"da", null, "da", 1.0f}
            }},
            {"da,en-gb", new Object[][] {
                {"da", null, "da", 1.0f},
                {"en", "gb", "en-gb", 1.0f}
            }},
            {"da , en-gb ", new Object[][] {
                {"da", null, "da", 1.0f},
                {"en", "gb", "en-gb", 1.0f}
            }},
            {"da-", new Object[][] {
                {"da", null, "da", 1.0f}
            }},
            {"da- ", new Object[][] {
                {"da", null, "da", 1.0f}
            }},
            {"da-;", new Object[][] {
                {"da", null, "da", 1.0f}
            }},
            {"da-,", new Object[][] {
                {"da", null, "da", 1.0f}
            }},
            {"DA-DK", new Object[][] {
                {"da", "dk", "da-dk", 1.0f}
            }},
            {"da, en-gb;q=0.8, en;q=0.7", new Object[][] {
                {"da", null, "da", 1.0f},
                {"en", "gb", "en-gb", 0.8f},
                {"en", null, "en", 0.7f},
            }},
            {"en;q=0.7, en-gb;q=0.8, da", new Object[][] {
                {"da", null, "da", 1.0f},
                {"en", "gb", "en-gb", 0.8f},
                {"en", null, "en", 0.7f},
            }},
            {"da;q=0", new Object[][] {
                {"da", null, "da", 0.0f}
            }},
            {"da;q=1", new Object[][] {
                {"da", null, "da", 1.0f}
            }},
        };

        Object[][] languages;
        AcceptLanguage acceptLanguage;
        for (int i=0; i<cases.length; ++i) {
            request = mock(HttpServletRequest.class);
            when(request.getHeader("Accept-Language")).thenReturn((String)cases[i][0]);
            acceptLanguages = AcceptLanguageParser.parseHeader(request);
            languages = (Object[][])cases[i][1];
            Assert.assertEquals(languages.length, acceptLanguages.size());
            for (int j=0; j<languages.length; ++j) {
                acceptLanguage = acceptLanguages.get(j);
                Assert.assertEquals(languages[j][0], acceptLanguage.language);
                Assert.assertEquals(languages[j][1], acceptLanguage.country);
                Assert.assertEquals(languages[j][2], acceptLanguage.locale);
                Assert.assertEquals(languages[j][3], acceptLanguage.qvalue);
            }
        }
    }

}

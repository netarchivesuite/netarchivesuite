package dk.netarkivet.systemtest.page;

import org.jaccept.TestEventManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class DomainConfigurationPageHelper {
    public static final String NEW_CONFIGURATION = "New configuration";

    public static final String DEFAULT_DOMAIN_NAME = "defaultconfig";
    public static final String MAX_OBJECTS_FIELD = "maxObjects";
    public static final String MAX_BYTES_FIELD = "maxBytes";
    public static final String COMMENTS = "comments";

    public static void createConfiguration(String domainName, String configurationName) {
        TestEventManager.getInstance().addStimuli("Creating configuration" + configurationName);
        WebDriver driver = PageHelper.getWebDriver();
        DomainWebTestHelper.editDomain(domainName);

        driver.findElement(By.linkText(NEW_CONFIGURATION)).click();
        driver.findElement(By.name("configName")).sendKeys(configurationName);
        driver.findElement(By.cssSelector("input[type=\"submit\"]")).click();
    }

    /**
     * Goto the edit page for the indicated domains default configuration.
     */
    public static void gotoDefaultConfigurationPage(String domainName) {
        gotoConfigurationPage(domainName, DEFAULT_DOMAIN_NAME);
    }

    public static void gotoConfigurationPage(String domainName, String configurationName) {
        TestEventManager.getInstance().addStimuli("Updating configuration" + configurationName);
        DomainWebTestHelper.editDomain(domainName);

        WebElement table = PageHelper.getWebDriver().findElement(By.className("selection_table"));
        List<WebElement> tr_collection = table.findElements(By.tagName("tr"));
        for (WebElement webElement:tr_collection) {
            List<WebElement> rowCells = webElement.findElements(By.xpath("td"));
            if (rowCells.size() > 0 && //none header
                rowCells.get(0).getText().contains(configurationName)) {
                webElement.findElement(By.linkText("Edit")).click();
                break;
            }
        }
        PageHelper.getWebDriver().findElement(By.name(MAX_OBJECTS_FIELD)); // Ensure page is loaded.
    }

    public static void setMaxObjects(int value) {
        WebElement maxObjectsField = PageHelper.getWebDriver().findElement(By.name(MAX_OBJECTS_FIELD));
        maxObjectsField.clear();
        maxObjectsField.sendKeys(String.valueOf(value));
    }

    public static void submitChanges() {
        PageHelper.getWebDriver().findElement(By.cssSelector("input[type=\"submit\"]")).click();
    }
}

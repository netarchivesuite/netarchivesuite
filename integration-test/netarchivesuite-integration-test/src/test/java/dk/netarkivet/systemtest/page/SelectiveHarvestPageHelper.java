package dk.netarkivet.systemtest.page;

import dk.netarkivet.systemtest.HarvestUtils;
import org.jaccept.TestEventManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Provides functionality for commonly used test access to domain web content.
 *
 * Will also log webpage interactions.
 */
public class SelectiveHarvestPageHelper {
    public static void createSelectiveHarvest(
            String name, String comments, String[] domains) {
        TestEventManager.getInstance().addStimuli("Creating harvest " + name);
        WebDriver driver = PageHelper.getWebDriver();
        PageHelper.gotoPage(PageHelper.MenuPages.SelectiveHarvests);
        driver.findElement(By.linkText("Create new selective harvest definition")).click();

        driver.findElement(By.name("harvestname")).clear();
        driver.findElement(By.name("harvestname")).sendKeys(name);

        driver.findElement(By.name("comments")).clear();
        if (comments != null) {
            driver.findElement(By.name("comments")).sendKeys(comments);
        }

        driver.findElement(By.name("domainlist")).clear();
        for (String domain:domains) {
            driver.findElement(By.name("domainlist")).sendKeys(domain + "\n");
        }

        driver.findElement(By.cssSelector("input[type=\"submit\"]")).click();
        driver.findElement(By.name("save")).click();
    }

    /**
     * Creates a selective harvest without comments for the default domain.
     */
    public static void createSelectiveHarvest(String name) {
        createSelectiveHarvest(name, null, new String[] {HarvestUtils.DEFAULT_DOMAIN});
    }

    public static void activateHarvest(String name) {
        TestEventManager.getInstance().addStimuli("Activating harvest " + name);
        PageHelper.gotoPage(PageHelper.MenuPages.SelectiveHarvests);
        if (PageHelper.getWebDriver().getPageSource().contains("Show inactive harvest definitions")) {
            PageHelper.getWebDriver().findElement(By.linkText("Show inactive harvest definitions")).click();
        }
        WebElement table = PageHelper.getWebDriver().findElement(By.className("selection_table"));
        List<WebElement> tr_collection = table.findElements(By.tagName("tr"));
        for (WebElement webElement:tr_collection) {
            if (webElement.getText().contains(name)) {
                webElement.findElement(By.linkText("Activate")).click();
                break;
            }
        }
    }

    public static void deactivateHarvest(String name) {
        TestEventManager.getInstance().addStimuli("Deactivating harvest " + name);
        PageHelper.gotoPage(PageHelper.MenuPages.SelectiveHarvests);
        WebElement table = PageHelper.getWebDriver().findElement(By.className("selection_table"));
        List<WebElement> tr_collection = table.findElements(By.tagName("tr"));
        for (WebElement webElement:tr_collection) {
            if (webElement.getText().contains(name)) {
                webElement.findElement(By.linkText("Deactivate")).click();
            }
        }
    }

    public static void deactivateAllHarvests() {
        TestEventManager.getInstance().addStimuli("Deactivating all harvests");
        PageHelper.gotoPage(PageHelper.MenuPages.SelectiveHarvests);
        if (PageHelper.getWebDriver().getPageSource().contains("Hide inactive harvest definitions")) {
            PageHelper.getWebDriver().findElement(By.linkText("Hide inactive harvest definitions")).click();
        }
        while (!PageHelper.getWebDriver().getPageSource().contains("No selective harvests defined")) {
            // Ensure page is loaded
            PageHelper.getWebDriver().findElement(By.linkText("Create new selective harvest definition"));
            WebElement table = PageHelper.getWebDriver().findElement(By.className("selection_table"));
            List<WebElement> tr_collection = table.findElements(By.tagName("tr"));
            TestEventManager.getInstance().addStimuli("Deactivating harvest: " +
                    tr_collection.get(1).findElements(By.xpath("td")).get(0).getText());
            tr_collection.get(1).findElement(By.linkText("Deactivate")).click();
        }
    }

}

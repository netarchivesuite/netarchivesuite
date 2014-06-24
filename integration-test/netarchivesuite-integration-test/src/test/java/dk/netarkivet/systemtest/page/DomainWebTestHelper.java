package dk.netarkivet.systemtest.page;

import org.jaccept.TestEventManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.Arrays;

/**
 * Provides functionality for  commonly used test access to domain web content.
 *
 * Will also log webpage interactions.
 */
public class DomainWebTestHelper {
    public static final String SHOW_UNUSED_CONFIGURATIONS_LINK = "Show unused configurations";
    public static final String HIDE_UNUSED_CONFIGURATIONS_LINK = "Hide unused configurations";
    public static final String SHOW_UNUSED_SEED_LISTS_LINK = "Show unused seed lists";
    public static final String HIDE_UNUSED_SEED_LISTS_LINK = "Hide unused seed lists";
    public static final String NEW_SEED_LIST = "New seed list";

    public static void createDomain(String[] domains) {
        TestEventManager.getInstance().addStimuli("Creating domains" + Arrays.asList(domains));
        WebDriver driver = PageHelper.getWebDriver();
        PageHelper.gotoPage(PageHelper.MenuPages.CreateDomain);

        driver.findElement(By.name("domainlist")).clear();
        for (String domain:domains) {
            driver.findElement(By.name("domainlist")).sendKeys(domain + "\n");
        }

        driver.findElement(By.cssSelector("input[type=\"submit\"]")).click();
    }

    public static void editDomain(String domainName) {
        PageHelper.gotoSubPage("HarvestDefinition/Definitions-edit-domain.jsp?name=" + domainName);
    }

    public static void createSeedList(String domainName, String seedListName, String[] seeds) {
        TestEventManager.getInstance().addStimuli("Creating configuration" + seedListName +
                "(" + seeds + ")");
        WebDriver driver = PageHelper.getWebDriver();
        editDomain(domainName);

        driver.findElement(By.linkText(NEW_SEED_LIST)).click();
        driver.findElement(By.name("urlListName")).sendKeys(seedListName);

        driver.findElement(By.name("seedList")).clear();
        for (String seed:seeds) {
            driver.findElement(By.name("seedList")).sendKeys(seed + "\n");
        }

        driver.findElement(By.cssSelector("input[type=\"submit\"]")).click();
    }
}

package dk.netarkivet.systemtest;

import com.gargoylesoftware.htmlunit.BrowserVersion;
//import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
//import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import java.io.File;
import java.util.List;
import java.util.Set;

public class SeleniumSession<U extends WebDriver & JavascriptExecutor> implements AutoCloseable,WebDriver,JavascriptExecutor {

    private final U driver;

    public SeleniumSession() {
        U createdDriver;
/*        try {
            //createdDriver = firefoxDriver();
            createdDriver = chromeDriver();
        } catch (AssertionError error) { //The chromedriver does not exist
            createdDriver = htmlUnitDriver();
        }*/
        createdDriver = htmlUnitDriver();
        driver = createdDriver;
    }

    //Not currently used, but can be commented in easily, if we need to test on a system not using chrome.
    private U firefoxDriver() {
        return (U)new FirefoxDriver();
    }

    private U htmlUnitDriver() {
        HtmlUnitDriver htmlUnitDriver = new HtmlUnitDriver(BrowserVersion.CHROME) {
            {   // Nessesary for the login reload to work
                // https://stackoverflow.com/a/44163031
                this.getWebClient().getCache().setMaxSize(0);
            }
        };
        return  (U) htmlUnitDriver;
    }

/*    private U chromeDriver() {
        String driverString = "/usr/lib/chromium-browser/chromedriver";
        System.setProperty("webdriver.chrome.driver", driverString);
        Assert.assertTrue("Chromedriver '"+driverString+"' does not exist",new File(driverString).exists());
        return  (U) new ChromeDriver();
    }*/


    @Override
    public void close()  {
        driver.close();
    }

    @Override
    public void quit() {
        driver.quit();
    }

    @Override
    public Set<String> getWindowHandles() {
        return driver.getWindowHandles();
    }

    @Override
    public String getWindowHandle() {
        return driver.getWindowHandle();
    }


    @Override
    public TargetLocator switchTo() {
        return driver.switchTo();
    }

    @Override
    public Navigation navigate() {
        return driver.navigate();
    }

    @Override
    public Options manage() {
        return driver.manage();
    }

    @Override
    public void get(String url) {
        driver.get(url);
    }

    @Override
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    @Override
    public String getTitle() {
        return driver.getTitle();
    }

    @Override
    public WebElement findElement(By by) {
        return driver.findElement(by);
    }

    @Override
    public List<WebElement> findElements(By by) {
        return driver.findElements(by);
    }

    @Override
    public String getPageSource() {
        return driver.getPageSource();
    }

    @Override
    public Object executeScript(String s, Object... objects) {
        return driver.executeScript(s, objects);
    }

    @Override
    public Object executeAsyncScript(String s, Object... objects) {
        return driver.executeAsyncScript(s, objects);
    }
}
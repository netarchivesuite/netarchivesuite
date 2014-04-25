package dk.netarkivet.systemtest.functional;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import dk.netarkivet.systemtest.HarvestUtils;
import dk.netarkivet.systemtest.NASAssert;
import dk.netarkivet.systemtest.SeleniumTest;
import dk.netarkivet.systemtest.page.DomainWebTestHelper;
import dk.netarkivet.systemtest.page.PageHelper;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ExtendedFieldTest extends SeleniumTest {
    private String extendedIDForTest;
    private DateFormat dateFomatter = new SimpleDateFormat("HHmmss");

    @BeforeMethod(alwaysRun=true)
    public void setup(Method method) {
        Date startTime = new Date();
        extendedIDForTest = method.getName() + "-" + dateFomatter.format(startTime);
    }

    @Test(groups = {"guitest","functest"})
    public void extendedDomainStringFieldTest() throws Exception {
        addDescription("Tests that String type extended fields works correctly on domains.");
        addStep("Create a new String type field (name:" + extendedIDForTest + ") for domains",
                "");
        PageHelper.gotoPage(PageHelper.MenuPages.ExtendedFields);
        driver.findElement(By.linkText("create Extended Field")).click(); // Todo needs more specific find

        driver.findElement(By.name("extf_name")).clear();
        driver.findElement(By.name("extf_name")).sendKeys(extendedIDForTest);

        Select select = new Select(driver.findElement(By.name("extf_datatype")));
        select.selectByVisibleText("String");

        driver.findElement(By.cssSelector("input[type=\"submit\"]")).click();

        addStep("Edit the default domain (" + HarvestUtils.DEFAULT_DOMAIN + ")",
                "The new extended field should be shown");
        DomainWebTestHelper.editDomain(HarvestUtils.DEFAULT_DOMAIN);
        NASAssert.assertTrue(driver.getPageSource().contains(extendedIDForTest));

        addStep("Fill out the new extended field with a value and save the "
                + "updated domain",
                "");


        addStep("Reopen the domain",
                "The new extended field should contain the newly defined value");
    }
}

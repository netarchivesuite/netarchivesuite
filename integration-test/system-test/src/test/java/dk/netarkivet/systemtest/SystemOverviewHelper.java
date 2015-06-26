package dk.netarkivet.systemtest;

import static org.testng.Assert.assertTrue;
import static org.testng.FileAssert.fail;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import dk.netarkivet.systemtest.page.PageHelper;

public class SystemOverviewHelper {

    public static List<WebElement>  getRowContainingLogString(String logSnippet) {
        int logColumn = 5;
        List<WebElement> rows = PageHelper.getWebDriver().findElements(
                By.xpath("//table[@class='system_state_table']/tbody/tr[position()>1]"));
        for (WebElement rowElement : rows) {
            List<WebElement> rowCells = rowElement.findElements(By.xpath("td"));
            if(rowCells.get(logColumn).getText().contains(logSnippet)) {
                return rowCells;
            }
        }
        throw new AssertionError("Unable to find log containing " + logSnippet);
    }
}

/*
 * #%L
 * NetarchiveSuite System test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.systemtest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jaccept.TestEventManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import dk.netarkivet.systemtest.environment.TestEnvironment;
import dk.netarkivet.systemtest.environment.TestEnvironmentController;
import dk.netarkivet.systemtest.page.DomainConfigurationPageHelper;
import dk.netarkivet.systemtest.page.PageHelper;
import dk.netarkivet.systemtest.page.SelectiveHarvestPageHelper;

public class HarvestUtils {

    protected static final TestLogger log = new TestLogger(HarvestUtils.class);
    public static String DEFAULT_DOMAIN = "pligtaflevering.dk";
    public static final int MAX_MINUTES_TO_WAIT_FOR_HARVEST = 60;

    /**
     * Ensures that the number of selelective harvests have run for the default domain. Existingharvests -
     * requiredNumberOfHarvests are started as part of this method, and the method returns when the harvests are
     * finished.
     *
     * @param requiredNumberOfHarvests The number of harvests which must have run when this method returns.
     */
    public static void ensureNumberOfHarvestsForDefaultDomain(int requiredNumberOfHarvests) {
        int numberOfExtraHarvestsToRun = requiredNumberOfHarvests - getNumberOfHarvestsRun(DEFAULT_DOMAIN);
        if (numberOfExtraHarvestsToRun > 0) {
            runHarvests("EnsureNumberOfHarvests-" + System.currentTimeMillis(), numberOfExtraHarvestsToRun);
        }
    }

    /**
     * Returns number of harvests run for the indicated domain.
     */
    public static int getNumberOfHarvestsRun(String domainName) {
        gotoHarvestHistoryForDomain(domainName);
        return PageHelper.getWebDriver()
                .findElements(By.xpath("//table[@class='selection_table']/tbody/tr[position()>1]")).size();
    }

    public static void gotoHarvestHistoryForDomain(String domainName) {
        PageHelper.gotoSubPage("History/Harveststatus-perdomain.jsp?domainName=" + domainName);
    }

    public static void runHarvests(String name, int count) {
        Set<String> unfinishedHarvests = new HashSet<String>();
        for (int i = 0; i < count; i++) {
            String nameWithCounter = name + i;
            unfinishedHarvests.add(nameWithCounter);
            SelectiveHarvestPageHelper.createSelectiveHarvest(nameWithCounter);
            SelectiveHarvestPageHelper.activateHarvest(nameWithCounter);
        }

        long starttime = System.currentTimeMillis();
        TestEventManager.getInstance().addResult("Waiting for the following harvests to finish: " + unfinishedHarvests);
        int minutesWaitingForHarvest = 0;
        int maxMinutesToWaitForAllHarvests = unfinishedHarvests.size() * MAX_MINUTES_TO_WAIT_FOR_HARVEST;
        System.err.print("Initiating " + unfinishedHarvests.size() + " new harvests, so " + count + " finished "
                + "harvests are available. Will timeout after " + maxMinutesToWaitForAllHarvests + " minutes.\n");
        while (!unfinishedHarvests.isEmpty()) {
            System.err.print(".");
            PageHelper.reloadSubPage("History/Harveststatus-perdomain.jsp?domainName=" + DEFAULT_DOMAIN);
            for (String harvest : unfinishedHarvests.toArray(new String[unfinishedHarvests.size()])) {
                if (PageHelper.getWebDriver().getPageSource().contains(harvest)) {
                    System.err.println("\n" + harvest + " finished");
                    unfinishedHarvests.remove(harvest);
                }
            }
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
            }
            if (++minutesWaitingForHarvest > maxMinutesToWaitForAllHarvests) {
                throw new RuntimeException("The harvests " + unfinishedHarvests + " took to long (more that "
                        + MAX_MINUTES_TO_WAIT_FOR_HARVEST + ") to finish, " + "aborting");
            }
        }
        System.err.println("All harvests finished in " + (System.currentTimeMillis() - starttime) / 1000 + " seconds");
    }

    public static void minimizeDefaultHarvest() {
        DomainConfigurationPageHelper.gotoDefaultConfigurationPage(DEFAULT_DOMAIN);
        DomainConfigurationPageHelper.setMaxObjects(10);
        DomainConfigurationPageHelper.submitChanges();
    }


    public static void waitForJobStatus(String harvestName, JobStatus jobStatus) {
        boolean keepWaiting = true;
        int secondsWaitingForJob = 0;
        int maxSecondsToWaitForAllHarvests = 60;
        while (keepWaiting) {
            System.err.print(".");

            try {
                Thread.sleep(10000);
                secondsWaitingForJob = secondsWaitingForJob + 10;
            } catch (InterruptedException e) {
            }
            if (secondsWaitingForJob > maxSecondsToWaitForAllHarvests) {
                throw new RuntimeException("The job for " + harvestName + " took to long (more that "
                        + maxSecondsToWaitForAllHarvests + "s) to finish, " + "aborting");
            }

            PageHelper.reloadSubPage("History/Harveststatus-alljobs.jsp?" +
                    "JOB_STATUS=" + jobStatus +
                    "&HARVEST_NAME=&START_DATE=&END_DATE=&JOB_ID_ORDER=ASC&PAGE_SIZE=100&START_PAGE_INDEX=1&upload=Show");
            if (PageHelper.getWebDriver().getPageSource().contains(harvestName)) {
                keepWaiting = false;
            }
        }
    }

    public static void waitForJobGeneration(String harvestName) {
        boolean keepWaiting = true;
        int secondsWaitingForJob = 0;
        int maxSecondsToWaitForAllHarvests = 180;
        while (keepWaiting) {
            System.err.print(".");

            try {
                Thread.sleep(10000);
                secondsWaitingForJob = secondsWaitingForJob + 10;
            } catch (InterruptedException e) {
            }
            if (secondsWaitingForJob > maxSecondsToWaitForAllHarvests) {
                throw new RuntimeException("The job for " + harvestName + " took to long (more that "
                        + maxSecondsToWaitForAllHarvests + "s) to finish, " + "aborting");
            }

            PageHelper.reloadSubPage("History/Harveststatus-alljobs.jsp?" +
                    "JOB_STATUS=ALL" +
                    "&HARVEST_NAME=&START_DATE=&END_DATE=&JOB_ID_ORDER=ASC&PAGE_SIZE=100&START_PAGE_INDEX=1&upload=Show");
            if (PageHelper.getWebDriver().getPageSource().contains(harvestName)) {
                keepWaiting = false;
            }
        }
    }

    public static void waitForJobGeneration(String harvestName, TestEnvironmentController testEnvironmentController) {
        boolean keepWaiting = true;
        int secondsWaitingForJob = 0;
        int maxSecondsToWaitForAllHarvests = 360;
        while (keepWaiting) {
            System.err.print(".");

            try {
                Thread.sleep(10000);
                secondsWaitingForJob = secondsWaitingForJob + 10;
            } catch (InterruptedException e) {
            }
            if (secondsWaitingForJob > maxSecondsToWaitForAllHarvests) {
                throw new RuntimeException("The job for " + harvestName + " took to long (more that "
                        + maxSecondsToWaitForAllHarvests + "s) to finish, " + "aborting");
            }
            keepWaiting = !isFinished(harvestName, testEnvironmentController);
        }
    }


    /**
     * Looks for a log statement like "Created 212 jobs for harvest hgj8hy".
     * @return
     */
    static boolean isFinished(String harvestName, TestEnvironmentController testEnvironmentController) {
        try {
            String output =  testEnvironmentController.runCommand(TestEnvironment.JOB_ADMIN_SERVER,
                    "grep 'Created' ${HOME}/" + testEnvironmentController.ENV.getTESTX() + "/log/HarvestJobManager*",
                    new int[] {0, 1});
            final String harvestNamePattern = ".*Created ([0-9]+) jobs([^<>]*)[(]" + harvestName + "[)].*";
            Pattern finished = Pattern.compile(harvestNamePattern,
                    Pattern.DOTALL);
            log.debug("Matching '" + harvestNamePattern + "' against '" + output + "'");
            final Matcher matcher = finished.matcher(output);
            if (matcher.matches()) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String findHarvestingHost() {
        boolean jobIsRunning = false;
        int secondsBetweenCheckForRunningJob = 10;
        int maxSecondsTowaitForRunningJob=60;
        int secondsWaiting = 0;
        for (secondsWaiting = 0 ; secondsWaiting < maxSecondsTowaitForRunningJob ;  secondsWaiting = secondsWaiting + secondsBetweenCheckForRunningJob) {
            List<WebElement> runningJobRow = getRunningJobRow();
            System.err.print(".");
            if (runningJobRow != null) {
                return runningJobRow.get(2).getText();
            }
            try {
                Thread.sleep(secondsBetweenCheckForRunningJob);
            } catch (InterruptedException e) {
            }
        }
        throw new RuntimeException("Running job did't appear for more than "
                + maxSecondsTowaitForRunningJob + "s, " + "aborting");
    }

    public enum JobStatus {
        NEW, SUBMITTED, STARTED, DONE, FAILED, RESUBMITTED, FAILED_REJECTED;
    }

    public static List<WebElement> getRunningJobRow() {
        PageHelper.gotoPage(PageHelper.MenuPages.RunningJobs);
        List<WebElement> rows = PageHelper.getWebDriver().findElements(
                By.xpath("//table[@class='selection_table']/tbody/tr[position()>1]"));
        for (WebElement rowElement : rows) {
            List<WebElement> rowCells = rowElement.findElements(By.xpath("td"));
            if (rowCells.size() > 1) {
                if (rowCells.get(1).findElement(By.xpath("Crawler er igang")) != null) {
                    return rowCells;
                }
            }
        }
        return null;
    }
}

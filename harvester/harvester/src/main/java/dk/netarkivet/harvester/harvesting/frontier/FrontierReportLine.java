/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.harvester.harvesting.frontier;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sleepycat.persist.model.Persistent;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Wraps a line of the frontier report.
 * As of Heritrix 1.14.14, the format of a frontier report line
 * sequentially lists the following tokens, separated by a whitespace :
 *
 * <ol>
 * <li>queue</li>
 * <li>currentSize</li>
 * <li>totalEnqueues</li>
 * <li>sessionBalance</li>
 * <li>lastCost(averageCost)</li>
 * <li>lastDequeueTime</li>
 * <li>wakeTime</li>
 * <li>totalSpend/totalBudget</li>
 * <li>errorCount</li>
 * <li>lastPeekUri</li>
 * <li>lastQueuedUri</li>
 * </ol>
 *
 * This class implements a natural order : comparisons are made :
 * - first by decreasing values of totalEnqueues
 * - secondly by domain name (string natural order)
 *
 * Thanks to Gordon Mohr at Internet Archive for explaining the exact semantics
 * of the frontier report fields.
 *
 */
@Persistent
public class FrontierReportLine
implements Serializable,
           Comparable<FrontierReportLine>,
           FrontierReportLineOrderKey {

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(FrontierReportLine.class);

    /**
     * Expected size of string array when we split the line token
     * across "\\s+".
     */
    private static final int EXPECTED_SPLIT_SEGMENTS = 11;

    /**
     * Token used to signify an empty value.
     */
    static final String EMPTY_VALUE_TOKEN = "-";

    /**
     * The queue name, in our case the domain, as we use per domain queues.
     */
    private String domainName;

    /**  Number of URIs currently in the queue.  */
    private long currentSize;

    /**
     * Count of total times a URI has been enqueued to this queue;
     * a measure of the total number of URI instances ever put on this queue.
     * This can be a larger number than the unique URIs, as some URIs
     * (most notably DNS/robots when refetched, but possibly other things
     * force-requeued under advanced usage) may be enqueued more than once.
     */
    private long totalEnqueues;

    /**
     * When using the 'budget/rotation' functionality (a non-zero URI cost
     * policy), this is the running 'balance' of a queue during its current
     * 'active' session. This balance declines; when it hits zero, another queue
     * (if any are waiting 'inactive') gets a chance to enter active crawling
     * (as fast as politeness allows).
     */
    private long sessionBalance;

    /**
     * The 'cost' of the last URI charged against the queue's budgets. If
     * using a cost policy that makes some URIs more costly than others, this
     * may indicate the queue has reached more-costly URIs. (Such larger-cost
     * URIs will be inserted later in the queue, accelerate the depletion of
     * the session balance, and accelerate progress towards the total queue
     * budget, which could send the queue into 'retirement'. Thus higher-cost
     * URIs mean a queue over time gets less of the crawler's cycles.)
     */
    private double lastCost;

    /** Average cost of a processed URI. */
    private double averageCost;

    /**
     *  Timestamp of when the last URI came off this queue for processing.
     *  May give an indication of how long a queue has been empty/inactive.
     */
    private String lastDequeueTime;

    /**
     * If the queue is in any sort of politeness- or connect-problem-'snooze'
     * delay, this indicates when it will again be eligible to offer URIs to
     * waiting threads. (When it wakes, it gets in line -- so actual wait before
     * next URI is tried may be longer depending on the balance of threads and
     * other active queues.)
     */
    private String wakeTime;

    /**
     * The total of all URI costs charged against this queue.
     */
    private long totalSpend;

    /**
     * The totalBudget above which the queue will be retired (made permanently
     * inactive unless its totalBudget is raised).
     */
    private long totalBudget;

    /**
     * The number of URIs from this queue that reached 'finished' status with
     * an error code (non-retryable errors, or exhausted retries, or other
     * errors). When nonzero and rising there may be special problems with the
     * site(s) related to this queue.
     */
    private long errorCount;

    /**
     * The last URI peeked/dequeued from the head of this queue.
     */
    private String lastPeekUri;

    /**
     * The last URI enqueued to anywhere in this queue.
     */
    private String lastQueuedUri;

    /**
     * Default empty constructor.
     */
    public FrontierReportLine() {

    }

    /**
     * Builds a cloned line.
     * @param original the line to clone
     */
    protected FrontierReportLine(FrontierReportLine original) {
        this.averageCost = original.averageCost;
        this.currentSize = original.currentSize;
        this.domainName = new String(original.domainName);
        this.errorCount = original.errorCount;
        this.lastCost = original.lastCost;
        this.lastDequeueTime = new String(original.lastDequeueTime);
        this.lastPeekUri = new String(original.lastPeekUri);
        this.lastQueuedUri = new String(original.lastQueuedUri);
        this.sessionBalance = original.sessionBalance;
        this.totalBudget = original.totalBudget;
        this.totalEnqueues = original.totalEnqueues;
        this.totalSpend = original.totalSpend;
        this.wakeTime = new String(original.wakeTime);
    }

    /**
     * Parses the given string.
     * @param lineToken the string to parse.
     */
    FrontierReportLine(String lineToken) {

        String[] split = lineToken.split("\\s+");

        if (split.length != EXPECTED_SPLIT_SEGMENTS) {
            throw new ArgumentNotValid("Format of line token '"
                    + lineToken + "' is not a valid frontier report line!");
        }


        this.domainName = split[0];
        try  {
        this.currentSize = parseLong(split[1]);
        } catch (NumberFormatException e) {

        }
        this.totalEnqueues = parseLong(split[2]);
        this.sessionBalance = parseLong(split[3]);

        // Cost token is lastCost(averageCost)
        String costToken = split[4];
        int leftParenIdx = costToken.indexOf("(");
        this.lastCost = parseDouble(costToken.substring(0, leftParenIdx));
        this.averageCost = parseDouble(
                costToken.substring(leftParenIdx + 1, costToken.indexOf(")")));

        this.lastDequeueTime = split[5];
        this.wakeTime = split[6];

        // Budget token is totalSpend/totalBudget
        String[] budgetTokens = split[7].split("/");
        if (budgetTokens.length != 2) {
            LOG.warn("Found incorrect budget token '" + split[7]);
        } else {
            this.totalSpend = parseLong(budgetTokens[0]);
            this.totalBudget = parseLong(budgetTokens[1]);
        }

        this.errorCount = parseLong(split[8]);

        this.lastPeekUri = split[9];
        this.lastQueuedUri = split[10];

    }

    /**
     * @return the domainName
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * @param domainName the domainName to set
     */
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    /**
     * @return the currentSize
     */
    public long getCurrentSize() {
        return currentSize;
    }

    /**
     * @param currentSize the currentSize to set
     */
    public void setCurrentSize(long currentSize) {
        this.currentSize = currentSize;
    }

    /**
     * @return the totalEnqueues
     */
    public long getTotalEnqueues() {
        return totalEnqueues;
    }

    /**
     * @param totalEnqueues the totalEnqueues to set
     */
    public void setTotalEnqueues(long totalEnqueues) {
        this.totalEnqueues = totalEnqueues;
    }

    /**
     * @return the sessionBalance
     */
    public long getSessionBalance() {
        return sessionBalance;
    }

    /**
     * @param sessionBalance the sessionBalance to set
     */
    public void setSessionBalance(long sessionBalance) {
        this.sessionBalance = sessionBalance;
    }

    /**
     * @return the lastCost
     */
    public double getLastCost() {
        return lastCost;
    }

    /**
     * @param lastCost the lastCost to set
     */
    public void setLastCost(double lastCost) {
        this.lastCost = lastCost;
    }

    /**
     * @return the averageCost
     */
    public double getAverageCost() {
        return averageCost;
    }

    /**
     * @param averageCost the averageCost to set
     */
    public void setAverageCost(double averageCost) {
        this.averageCost = averageCost;
    }

    /**
     * @return the lastDequeueTime
     */
    public String getLastDequeueTime() {
        return lastDequeueTime;
    }

    /**
     * @param lastDequeueTime the lastDequeueTime to set
     */
    public void setLastDequeueTime(String lastDequeueTime) {
        this.lastDequeueTime = lastDequeueTime;
    }

    /**
     * @return the wakeTime
     */
    public String getWakeTime() {
        return wakeTime;
    }

    /**
     * @param wakeTime the wakeTime to set
     */
    public void setWakeTime(String wakeTime) {
        this.wakeTime = wakeTime;
    }

    /**
     * @return the totalSpend
     */
    public long getTotalSpend() {
        return totalSpend;
    }

    /**
     * @param totalSpend the totalSpend to set
     */
    public void setTotalSpend(long totalSpend) {
        this.totalSpend = totalSpend;
    }

    /**
     * @return the totalBudget
     */
    public long getTotalBudget() {
        return totalBudget;
    }

    /**
     * @param totalBudget the totalBudget to set
     */
    public void setTotalBudget(long totalBudget) {
        this.totalBudget = totalBudget;
    }

    /**
     * @return the errorCount
     */
    public long getErrorCount() {
        return errorCount;
    }

    /**
     * @param errorCount the errorCount to set
     */
    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    /**
     * @return the lastPeekUri
     */
    public String getLastPeekUri() {
        return lastPeekUri;
    }

    /**
     * @param lastPeekUri the lastPeekUri to set
     */
    public void setLastPeekUri(String lastPeekUri) {
        this.lastPeekUri = lastPeekUri;
    }

    /**
     * @return the lastQueuedUri
     */
    public String getLastQueuedUri() {
        return lastQueuedUri;
    }

    /**
     * @param lastQueuedUri the lastQueuedUri to set
     */
    public void setLastQueuedUri(String lastQueuedUri) {
        this.lastQueuedUri = lastQueuedUri;
    }

    /**
     * Default order relation is descending size of the queue (totalEnqueues).
     */
    @Override
    public int compareTo(FrontierReportLine l) {
        return FrontierReportLineNaturalOrder.getInstance().compare(this, l);
    }

    /**
     * There is one queue per domain, so equality is based on the domain name.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FrontierReportLine) {
            return domainName.equals(
                    ((FrontierReportLine) obj).getDomainName());
        }
        return false;
    }

    /**
     * There is one queue per domain, so hashcode is based on the domain name.
     */
    @Override
    public int hashCode() {
        return domainName.hashCode();
    }

    public String getQueueId() {
        return domainName;
    }

    public long getQueueSize() {
        return totalEnqueues;
    }

    /**
     * Parses the token.
     * @param longToken token to parse.
     * @return parsed value or default value if value is empty or unparsable.
     */
    private static long parseLong(String longToken) {
        if (EMPTY_VALUE_TOKEN.equals(longToken)) {
            return Long.MIN_VALUE;
        }
        try {
            return Long.parseLong(longToken);
        } catch (NumberFormatException e) {
            LOG.warn(e);
            return Long.MIN_VALUE;
        }
    }

    /**
     * Parses the token.
     * @param dblToken token to parse.
     * @return parsed value or default value if value is empty or unparsable.
     */
    private static double parseDouble(String dblToken) {
        if (EMPTY_VALUE_TOKEN.equals(dblToken)) {
            return Double.MIN_VALUE;
        }
        try {
            return Double.parseDouble(dblToken);
        } catch (NumberFormatException e) {
            LOG.warn(e);
            return Double.MIN_VALUE;
        }
    }

}

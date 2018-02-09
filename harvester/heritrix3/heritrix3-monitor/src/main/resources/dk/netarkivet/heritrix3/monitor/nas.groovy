logfilePrefix = "scripting_events"   // A logfile will be created with this prefix + ".log"
//initials = "ABC"   // Curator initials to be added to log-messages

// To use, just remove the initial "//" from any one of these lines.
//
//killToeThread  1       //Kill a toe thread by number
//listFrontier '.*stats.*'    //List uris in the frontier matching a given regexp
//deleteFromFrontier '.*foobar.*'    //Remove uris matching a given regexp from the frontier
//printCrawlLog '.*'          //View already crawled lines uris matching a given regexp

import com.sleepycat.je.DatabaseEntry
import com.sleepycat.je.OperationStatus
import com.sleepycat.je.CursorConfig
import com.sleepycat.je.LockMode

import java.nio.file.Files
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.regex.Matcher
import java.util.regex.Pattern

void killToeThread(int thread) {
    job.crawlController.requestCrawlPause()
    job.crawlController.killThread(thread, true)
    logEvent("Killed Toe Thread number " + thread + ".")
    rawOut.println "WARNING: This job and heritrix may now need to be manually terminated when it is finished harvesting."
    rawOut.println "REMINDER: This job is now in a Paused state."
}

/**
 * Utility method to find and return the logger for a given log-prefix, or initialise it if it doesn't already exist.
 * @return logger
 */
Logger getLogger() {
    for (Map.Entry<Logger,FileHandler> entry: job.crawlController.loggerModule.fileHandlers ) {
        if (entry.key.name.contains(logfilePrefix)) {
            return entry.key
        }
    }
    return job.crawlController.loggerModule.setupSimpleLog(logfilePrefix)
}

void logEvent(String e) {
    try {
        getLogger().info("Action from user " + initials + ": " +e)
    } catch(groovy.lang.MissingPropertyException e1) {
        getLogger().info("Action from user: " +e)
    }
}

void deleteFromFrontier(String regex) {
    //job.crawlController.requestCrawlPause()
    if (job.crawlController.isPaused()) {
	    count = job.crawlController.frontier.deleteURIs(".*", regex)
	    //rawOut.println "REMINDER: This job is now in a Paused state."
	    logEvent("Deleted " + count + " URIs from frontier matching regex '" + regex + "'")
	    rawOut.println count + " URIs were deleted from the frontier."
	    rawOut.println("This action has been logged in " + logfilePrefix + ".log")
    }
    else {
	    rawOut.println "This job is not in a Paused state. Wait until job is pause and try again."
    }
}

void getNumberOfUrlsInFrontier() {
    //type  org.archive.crawler.frontier.BdbMultipleWorkQueues
    pendingUris = job.crawlController.frontier.pendingUris
    pendingUrisCount = pendingUris.pendingUrisDB.count()
    htmlOut.println "URLs in the frontierqueue: " + pendingUrisCount
}

void getNumberOfMatchedUrlsInFrontier(String regex) {
    matchingCount = 0
    pattern = ~regex
    //type  org.archive.crawler.frontier.BdbMultipleWorkQueues
    pendingUris = job.crawlController.frontier.pendingUris
    config = new CursorConfig()
    config.setReadUncommitted(true)
    //iterates over the raw underlying instance of com.sleepycat.je.Database
    cursor = pendingUris.pendingUrisDB.openCursor(null, config)
    key = new DatabaseEntry()
    value = new DatabaseEntry()
    try {
        while (cursor.getNext(key, value, null) == OperationStatus.SUCCESS) {
            if (value.getData().length == 0) {
                continue
            }
            curi = pendingUris.crawlUriBinding.entryToObject(value)
            if (pattern.matcher(curi.toString())) {
                ++matchingCount
            }
        }
    } finally {
        cursor.close()
    }
    rawOut.println matchingCount
}

/**
 * Find the links that are in the frontier list fulfilling the regular expression and the links on the actual page
 * @param regex Combined pagenumber and searchstring
 * @param itemsPerPage items displayed per page
 * @return the total number of pages combined with the frontier links that are on the page that fulfills the search string
 */
void listFrontier(String regex, long itemsPerPage, long page) {
    //style = 'overflow: auto; word-wrap: normal; white-space: pre; width:1200px; height:500px'
    //htmlOut.println '<pre style="' + style +'">'
    pattern = ~regex
    //type  org.archive.crawler.frontier.BdbMultipleWorkQueues
    pendingUris = job.crawlController.frontier.pendingUris
    totalCachedLines = pendingUris.pendingUrisDB.count()
    totalCachedSize = getPages(totalCachedLines, itemsPerPage)
    if (page > totalCachedSize) {
        page = totalCachedSize
    }
    matchingCount = 0
    index = 0

    def sb = StringBuilder.newInstance()

    config = new CursorConfig()
    config.setReadUncommitted(true)
    //iterates over the raw underlying instance of com.sleepycat.je.Database
    cursor = pendingUris.pendingUrisDB.openCursor(null, config)
    key = new DatabaseEntry()
    value = new DatabaseEntry()
    try {
        fIdx = (long)(page * itemsPerPage)
        tIdx = (long)(fIdx + itemsPerPage)
        if ("".compareTo(regex) == 0) {
            /*
            if (fIdx > 0) {
                index = skipNext(fIdx - 1, key, value, LockMode.READ_UNCOMMITTED)
                if (index > 0) {
                    ++index
                }
            }
            */
            matchingCount = totalCachedLines
            while (cursor.getNext(key, value, null) == OperationStatus.SUCCESS && index < tIdx) {
                if (value.getData().length == 0) {
                    continue
                }
                if (index >= fIdx) {
                    curi = pendingUris.crawlUriBinding.entryToObject(value)
                    sb << curi << '\n'
                }
                ++index
            }
        }
        else {
            while (cursor.getNext(key, value, null) == OperationStatus.SUCCESS && index < tIdx) {
                if (value.getData().length == 0) {
                    continue
                }
                curi = pendingUris.crawlUriBinding.entryToObject(value)
                if (pattern.matcher(curi.toString())) {
                    if (((long)index) >= ((long)(page * itemsPerPage)) && ((long)index) < ((long)((page + 1) * itemsPerPage))) {
                        sb << curi << '\n'
                    }
                    ++index
                    ++matchingCount
                }
            }
        }
    } finally {
        cursor.close()
    }
    content = matchingCount + '</p>' + '<pre>' + sb.toString() + '</pre>'

    htmlOut.println content
}

/**
 * Calculate the total number of pages.
 * @param items total number of items
 * @param itemsPerPage items displayed per page
 * @return the total number of pages
 */
long getPages(long items, long itemsPerPage) {
    long pages = (items + itemsPerPage - 1) / itemsPerPage
    if (pages == 0) {
        pages = 1
    }
    return pages
}

void pageFrontier(long skip, int items) {
    htmlOut.println '<pre>'
    //pattern = ~regex
    //type  org.archive.crawler.frontier.BdbMultipleWorkQueues
    pendingUris = job.crawlController.frontier.pendingUris
    //iterates over the raw underlying instance of com.sleepycat.je.Database
    cursor = pendingUris.pendingUrisDB.openCursor(null, null)
    key = new DatabaseEntry()
    value = new DatabaseEntry()
    cursor.skipNext(skip, key, value, null)
    matchingCount = 0
    try {
        while (cursor.getNext(key, value, null) == OperationStatus.SUCCESS) {
            if (value.getData().length == 0) {
                continue
            }
            curi = pendingUris.crawlUriBinding.entryToObject(value)
            htmlOut.println '<span style="font-size:small;">' + curi + '</span>'
            /*
            if (pattern.matcher(curi.toString())) {
                htmlOut.println '<span style="font-size:small;">' + curi + '</span>'
                matchingCount++
            }
            */
        }
    } finally {
        cursor.close()
    }
    htmlOut.println '</pre>'
    htmlOut.println '<p>'+ matchingCount + " matching uris found </p>"
}

class patternMatchingPredicate implements Predicate<String> {
    private java.util.regex.Pattern p

    patternMatchingPredicate(java.util.regex.Pattern p) {this.p=p }
    boolean test(String s) {return s.matches(p) }
}

class PrintConsumer implements Consumer<String> {
    private PrintWriter out

    PrintConsumer(PrintWriter out){this.out=out }
    void accept(String s) {out.println("<span style=\"font-size:small\">" + s + "</span>") }
}

void printCrawlLog(String regex) {
    style = 'overflow: auto; word-wrap: normal; white-space: pre; width:1200px; height:500px'
    htmlOut.println '<pre style="' + style +'">'
    namePredicate = new patternMatchingPredicate(~regex)
    crawlLogFile = job.crawlController.frontier.loggerModule.crawlLogPath.file
    matchingCount =  Files.lines(crawlLogFile.toPath()).filter(namePredicate).peek(new PrintConsumer(htmlOut)).count()
    htmlOut.println '</pre>'
    htmlOut.println '<p>'+ matchingCount + " matching lines found </p>"
}

void showModBudgets() {
    def modQueues = job.jobContext.data.get("manually-added-queues")
    if(modQueues.size() > 0) {
        htmlOut.println('<p style="margin-top: 50px;">Budgets of following domains/hosts have been changed in the current job :</p>')
    }
    htmlOut.println('<ul>')
    modQueues.each { key, value ->
        htmlOut.println('<li>'+key)
        htmlOut.println('<input type="hidden" name="queueName" value="'+key+'"/>')
        htmlOut.println('<input type="text" name="'+key+'-budget" style="width:100px" value="'+value+'"/></li>')
	}
	htmlOut.println('</ul>')
}

void changeBudget(String key, int value) {
	isQuotaEnforcer = false
	try { 
	   quotaEnforcerBean = appCtx.getBean("quotaenforcer")
	   if(quotaEnforcerBean != null) {
	   		if(appCtx.getBean("frontier").queueTotalBudget == -1) {
	   			isQuotaEnforcer = true
	   		}
	   }
	} catch(Exception e1) {
	   //Catch block 
	}
	//case quotaenforcer
	if(isQuotaEnforcer == true) {
		propertyName = "quotaenforcer.groupMaxAllKb"
	}
	//case frontier.queueTotalBudget
	else {
		propertyName = "frontier.queueTotalBudget"
	}

	surtDomain = ""
	for(str in key.split('\\.')) {
		surtDomain = str+","+surtDomain
	}
	surtDomain = "http://("+surtDomain
	
	mgr = appCtx.getBean("sheetOverlaysManager")
	newSheetName = "budget-"+value
	//get existing sheet for value
	sheet = mgr.sheetsByName.get(newSheetName)
	if(sheet == null) {
		mgr.putSheetOverlay(newSheetName, propertyName, value)
	}
	mgr.addSurtAssociation(surtDomain, newSheetName)
	
	//if frontier related settings have changed (for instance, budget), this can bring queues out of retirement
	appCtx.getBean("frontier").reconsiderRetiredQueues()

	//to store our manually added budget changes, we have to put them in a map
	def modQueues = job.jobContext.data.get("manually-added-queues")
    if(modQueues == null) {
		modQueues = [:]
	}
	oldValue = modQueues.get(key)	
	modQueues.put(key, value)
	job.jobContext.data.put("manually-added-queues", modQueues)
	
	if(oldValue == null || (oldValue != null && oldValue != value)) {
		logEvent("Changed budget for "+ key + " -> "+value+" URIs")
	}
}

void getQueueTotalBudget() {
	htmlOut.println appCtx.getBean("frontier").queueTotalBudget
}

void showFilters() {
	def originalIndexSize = job.jobContext.data.get("original-filters-size")
	regexRuleObj = appCtx.getBean("scope").rules.find{ it.class == org.archive.modules.deciderules.MatchesListRegexDecideRule }
	if(originalIndexSize != null && originalIndexSize < regexRuleObj.regexList.size()) {
		htmlOut.println('<ul>')
		for (i = originalIndexSize; i < regexRuleObj.regexList.size(); i++) {
			htmlOut.println('<li>')
   		    htmlOut.println('<input type="checkbox" name="removeIndex" value="'+i+'" />&nbsp;')
			htmlOut.println(regexRuleObj.regexList.get(i).pattern()+'</li>')
		}
		htmlOut.println('</ul>')
	}
}

void addFilter(String pat) {
	if(pat.length() > 0) {
		Pattern myRegex = Pattern.compile(pat)
		regexRuleObj = appCtx.getBean("scope").rules.find{ it.class == org.archive.modules.deciderules.MatchesListRegexDecideRule }
		//to store our manually added filters, we have to put them in a map
		def originalIndexSize = job.jobContext.data.get("original-filters-size")
		if(originalIndexSize == null) {
			job.jobContext.data.put("original-filters-size", regexRuleObj.regexList.size())
		}
		regexRuleObj.regexList.add(myRegex)
		logEvent("Added a RejectDecideRule matching regex '"+ pat + "'")
	}
}

void removeFilters(indexesOFiltersToRemove) {
	indexesOFiltersToRemove = indexesOFiltersToRemove.sort().reverse()
	regexRuleObj = appCtx.getBean("scope").rules.find{ it.class == org.archive.modules.deciderules.MatchesListRegexDecideRule }
	indexesOFiltersToRemove.each ({ num ->
		logEvent("Removed a RejectDecideRule matching regex '"+ regexRuleObj.regexList[num] + "'")
		regexRuleObj.regexList.remove(num)
	})
}


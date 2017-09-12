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

import java.nio.file.Files
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.logging.FileHandler
import java.util.logging.Logger
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
    job.crawlController.requestCrawlPause()
    count = job.crawlController.frontier.deleteURIs(".*", regex)
    rawOut.println "REMINDER: This job is now in a Paused state."
    logEvent("Deleted " + count + " URIs from frontier matching regex '" + regex + "'")
    rawOut.println count + " URIs were deleted from the frontier."
    rawOut.println("This action has been logged in " + logfilePrefix + ".log")
}

void listFrontier(String regex, long limit, String pageStr) {
    //style = 'overflow: auto; word-wrap: normal; white-space: pre; width:1200px; height:500px'
    //htmlOut.println '<pre style="' + style +'">'

    pattern = ~regex
    //type  org.archive.crawler.frontier.BdbMultipleWorkQueues
    pendingUris = job.crawlController.frontier.pendingUris
    htmlOut.println '<p>Limit: ' + 100 + '\n<br/>'
    htmlOut.println 'Total queued URIs: ' + pendingUris.pendingUrisDB.count() + '\n<br/>'
    htmlOut.println '-'
    htmlOut.println '---'
    htmlOut.println '-----'
    htmlOut.println 'Total cached size: ' + getPages(pendingUris.pendingUrisDB.count(), limit) + '\n<br/>'
    htmlOut.println '-------'
    page = 2
    htmlOut.println 'Page: ' + page + '\n<br/>'
    htmlOut.println '---------'
    totalCachedLines = pendingUris.pendingUrisDB.count();
    htmlOut.println '-----------'
    totalCachedSize = getPages(pendingUris.pendingUrisDB.count(), limit)
    htmlOut.println '-------------'
    content = totalCachedLines
    content = content + '<pre>'
    //iterates over the raw underlying instance of com.sleepycat.je.Database
    cursor = pendingUris.pendingUrisDB.openCursor(null, null)
    key = new DatabaseEntry()
    value = new DatabaseEntry()
    matchingCount = 0
    index = 0
    try {
        /*
        htmlOut.println lines

        totalCachedLines = pendingUris;
        if (linesPerPage == 0)
            linesPerPage = 100
        totalCachedSize = getPages(pendingUris, linesPerPage)
        */

        //while (cursor.getNext(key, value, null) == OperationStatus.SUCCESS && ((long)index) < ((long)(page * linesPerPage))) {
        while (cursor.getNext(key, value, null) == OperationStatus.SUCCESS && index < 25) {
            index++
            //content = content + index + '\n'
        }

        while (cursor.getNext(key, value, null) == OperationStatus.SUCCESS && limit > 0) {
//        while (cursor.getNext(key, value, null) == OperationStatus.SUCCESS && index < (page + 1) * pagesize - 1) {
/*            if ((index < page * pagesize) || (index > (page + 1) * pagesize)) {
                index++
                continue
            }
*/
            content = content + index + '\n'

            if (value.getData().length == 0) {
                continue
            }
            curi = pendingUris.crawlUriBinding.entryToObject(value)
            if (pattern.matcher(curi.toString())) {
                //htmlOut.println '<span style="font-size:small;">' + curi + '</span>'
                content = content + curi + '\n'
                matchingCount++
                index++
                --limit
            }
        }
    } finally {
        cursor.close()
    }

    content = content + '</pre>'

    if (limit > 0) {
        content = 'Matching URIs(test): '+ matchingCount + '</p>' + content
    } else {
        content = 'First matching URIs(test) (return limit reached): ' + matchingCount + '</p>' + content
    }
    htmlOut.println content
}

/**
 * Calculate the total number of pages.
 * @param items total number of items
 * @param itemsPerPage items displayed per page
 * @return the total number of pages
 */
public static long getPages(long items, long itemsPerPage) {
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




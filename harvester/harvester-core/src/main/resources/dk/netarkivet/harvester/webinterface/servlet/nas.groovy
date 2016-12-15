logfilePrefix = "scripting_events"   // A logfile will be created with this prefix + ".log"
//initials = "ABC"   // Curator initials to be added to log-messages

// To use, just remove the initial "//" from any one of these lines.
//
//killToeThread  1       //Kill a toe thread by number
//listFrontier '.*stats.*'    //List uris in the frontier matching a given regexp
//deleteFromFrontier '.*foobar.*'    //Remove uris matching a given regexp from the frontier
//printCrawlLog '.*'          //View already crawled lines uris matching a given regexp

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.OperationStatus

import java.nio.file.Files
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.regex.Pattern

void killToeThread(int thread) {
    job.crawlController.requestCrawlPause();
    job.crawlController.killThread(thread, true);
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
    return job.crawlController.loggerModule.setupSimpleLog(logfilePrefix);
}

void logEvent(String e) {
    getLogger().info("Action from user " + initials + ": " +e)
}

void deleteFromFrontier(String regex) {
    job.crawlController.requestCrawlPause()
    count = job.crawlController.frontier.deleteURIs(".*", regex)
    rawOut.println "REMINDER: This job is now in a Paused state."
    logEvent("Deleted " + count + " uris matching regex '" + regex + "'")
    rawOut.println count + " uris deleted from frontier."
    rawOut.println("This action has been logged in " + logfilePrefix + ".log")
}

void listFrontier(String regex, long limit) {
    //style = 'overflow: auto; word-wrap: normal; white-space: pre; width:1200px; height:500px'
    //htmlOut.println '<pre style="' + style +'">'
    
    pattern = ~regex
    //type  org.archive.crawler.frontier.BdbMultipleWorkQueues
    pendingUris = job.crawlController.frontier.pendingUris
    htmlOut.println '<p>Total queued URIs: ' + pendingUris.pendingUrisDB.count() + '\n<br/>'
    content = '<pre>'
    //iterates over the raw underlying instance of com.sleepycat.je.Database
    cursor = pendingUris.pendingUrisDB.openCursor(null, null);
    key = new DatabaseEntry();
    value = new DatabaseEntry();
    matchingCount = 0
    try {
        while (cursor.getNext(key, value, null) == OperationStatus.SUCCESS && limit > 0) {
            if (value.getData().length == 0) {
                continue;
            }
            curi = pendingUris.crawlUriBinding.entryToObject(value);
            if (pattern.matcher(curi.toString())) {
                //htmlOut.println '<span style="font-size:small;">' + curi + '</span>'
                content = content + curi + '\n'
                matchingCount++
                --limit;
            }
        }
    } finally {
        cursor.close();
    }
    content = content +  '</pre>'
    if (limit > 0) {
        content = 'Matching URIs: '+ matchingCount + '</p>' + content
    } else {
        content = 'First matching URIs (return limit reached): ' + matchingCount + '</p>' + content
    }
    htmlOut.println content
}

void pageFrontier(long skip, int items) {
    htmlOut.println '<pre>'
    //pattern = ~regex
    //type  org.archive.crawler.frontier.BdbMultipleWorkQueues
    pendingUris = job.crawlController.frontier.pendingUris
    //iterates over the raw underlying instance of com.sleepycat.je.Database
    cursor = pendingUris.pendingUrisDB.openCursor(null, null);
    key = new DatabaseEntry();
    value = new DatabaseEntry();
    cursor.skipNext(skip, key, value, null)
    matchingCount = 0
    try {
        while (cursor.getNext(key, value, null) == OperationStatus.SUCCESS) {
            if (value.getData().length == 0) {
                continue;
            }
            curi = pendingUris.crawlUriBinding.entryToObject(value);
            htmlOut.println '<span style="font-size:small;">' + curi + '</span>'
            /*
            if (pattern.matcher(curi.toString())) {
                htmlOut.println '<span style="font-size:small;">' + curi + '</span>'
                matchingCount++
            }
            */
        }
    } finally {
        cursor.close();
    }
    htmlOut.println '</pre>'
    htmlOut.println '<p>'+ matchingCount + " matching uris found </p>"
}

class patternMatchingPredicate implements Predicate<String> {
    private java.util.regex.Pattern p;
    public patternMatchingPredicate(java.util.regex.Pattern p) {this.p=p;}
    boolean test(String s) {return s.matches(p);}
}

class PrintConsumer implements Consumer<String> {
    private PrintWriter out;
    public PrintConsumer(PrintWriter out){this.out=out;}
    void accept(String s) {out.println("<span style=\"font-size:small\">" + s + "</span>");}
}

void printCrawlLog(String regex) {
    style = 'overflow: auto; word-wrap: normal; white-space: pre; width:1200px; height:500px'
    htmlOut.println '<pre style="' + style +'">'
    namePredicate = new patternMatchingPredicate(~regex);
    crawlLogFile = job.crawlController.frontier.loggerModule.crawlLogPath.file
    matchingCount =  Files.lines(crawlLogFile.toPath()).filter(namePredicate).peek(new PrintConsumer(htmlOut)).count()
    htmlOut.println '</pre>'
    htmlOut.println '<p>'+ matchingCount + " matching lines found </p>"
}

void showModBudgets() {
	def modQueues = job.jobContext.data.get("manually-added-queues");
	if(modQueues.size() > 0) {
		htmlOut.println('<p>Budgets of following domains/hosts have been changed in the current job :</p>')
	}
	htmlOut.println('<ul>')
	modQueues.each { key, value ->
		htmlOut.println('<li>'+key)
		htmlOut.println('<input type="text" name="'+key+'-budget" value="'+value+'"/>')
		htmlOut.println('<button type="submit" name="submitButton" value="'+key+'" class="btn btn-success"><i class="icon-white icon-thumbs-up"></i> Save</button></li>')
	}
	htmlOut.println('</ul>')
}

void changeBudget(String key, int value) {
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
		mgr.putSheetOverlay(newSheetName, "frontier.queueTotalBudget", value)
	}
	mgr.addSurtAssociation(surtDomain, newSheetName)
	
	//if frontier related settings have changed (for instance, budget), this can bring queues out of retirement
	appCtx.getBean("frontier").reconsiderRetiredQueues()

	//to store our manually added budget changes, we have to put them in a map
	def modQueues = job.jobContext.data.get("manually-added-queues");
	if(modQueues == null) {
		modQueues = [:]
	}
	modQueues.put(key, value)
	job.jobContext.data.put("manually-added-queues", modQueues)
	
	logEvent("manual budget change : "+ key + " -> "+value)
}

void getQueueTotalBudget() {
	htmlOut.println appCtx.getBean("frontier").queueTotalBudget
}

void showFilters() {
	def originalIndexSize = job.jobContext.data.get("original-filters-size")
	regexRuleObj = appCtx.getBean("scope").rules.find{ it.class == org.archive.modules.deciderules.MatchesListRegexDecideRule }
	htmlOut.println('<ul>')
	for (i = originalIndexSize; i < regexRuleObj.regexList.size(); i++) {
		htmlOut.println('<li><input type="checkbox" name="removeIndex" value="'+i+'" /> '+regexRuleObj.regexList.get(i).pattern()+'</li>')
	}
	htmlOut.println('</ul>')
	if(originalIndexSize < regexRuleObj.regexList.size()) {
		htmlOut.println('<button type="submit" name="remove-filter" value="1" class="btn btn-success"><i class="icon-white icon-remove"></i> Remove</button>')
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
		logEvent("manual add of a DecideResult.REJECT filter : "+ pat)
	}
}

void removeFilters(def indexesOFiltersToRemove) {
	indexesOFiltersToRemove = indexesOFiltersToRemove.sort().reverse()
	regexRuleObj = appCtx.getBean("scope").rules.find{ it.class == org.archive.modules.deciderules.MatchesListRegexDecideRule }
	def originalIndexSize = job.jobContext.data.get("original-filters-size")
	indexesOFiltersToRemove.eachWithIndex { num, idx ->
		logEvent("removing DecideResult.REJECT filter : "+ regexRuleObj.regexList[num+originalIndexSize])
		regexRuleObj.regexList.remove(num+originalIndexSize)
	}
}




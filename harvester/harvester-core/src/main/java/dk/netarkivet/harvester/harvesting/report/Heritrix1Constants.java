/*
 * #%L
 * Netarchivesuite - harvester
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

package dk.netarkivet.harvester.harvesting.report;
/*
import org.archive.crawler.datamodel.CrawlURI;
import org.archive.crawler.deciderules.DecideRuleSequence;
import org.archive.crawler.deciderules.DecidingScope;
import org.archive.crawler.deciderules.MatchesListRegExpDecideRule;
import org.archive.crawler.framework.CrawlController;
*/
public class Heritrix1Constants {

    /** Prefix associated with annotations made by this processor. */
    public static final String CONTENT_SIZE_ANNOTATION_PREFIX = "content-size:";

    // import org.archive.crawler.datamodel.CrawlURI;
    //public static final int CRAWLURI_S_BLOCKED_BY_QUOTA = CrawlURI.S_BLOCKED_BY_QUOTA;
    public static final int CRAWLURI_S_BLOCKED_BY_QUOTA = -5003;

    // import org.archive.crawler.deciderules.DecideRuleSequence;
    //public static final String DECIDERULESEQUENCE_CLASSNAME = DecideRuleSequence.class.getName();
    public static final String DECIDERULESEQUENCE_CLASSNAME = "org.archive.crawler.deciderules.DecideRuleSequence";

    // import org.archive.crawler.deciderules.DecidingScope;
    //public static final String DECIDINGSCOPE_CLASSNAME = DecidingScope.class.getName();
    public static final String DECIDINGSCOPE_CLASSNAME = "org.archive.crawler.deciderules.DecidingScope";

    // import org.archive.crawler.deciderules.MatchesListRegExpDecideRule;
    //public static final String MATCHESLISTREGEXPDECIDERULE_CLASSNAME = MatchesListRegExpDecideRule.class.getName();
    public static final String MATCHESLISTREGEXPDECIDERULE_CLASSNAME = "org.archive.crawler.deciderules.MatchesListRegExpDecideRule";

    // import org.archive.crawler.framework.CrawlController;
    //public static final Object CRAWLCONTROLLER_FINISHED = CrawlController.FINISHED;
    public static final Object CRAWLCONTROLLER_FINISHED = "FINISHED".intern();

    /*
    public static void main(String[] args) {
        System.out.println(CRAWLURI_S_BLOCKED_BY_QUOTA);
        System.out.println(DECIDERULESEQUENCE_CLASSNAME);
        System.out.println(DECIDINGSCOPE_CLASSNAME);
        System.out.println(MATCHESLISTREGEXPDECIDERULE_CLASSNAME);
        System.out.println(CRAWLCONTROLLER_FINISHED);
    }
    */

}

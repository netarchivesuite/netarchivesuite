filedesc://8-metadata-1.arc 0.0.0.0 20080602075254 text/plain 77
1 0 InternetArchive
URL IP-address Archive-date Content-type Archive-length

metadata://netarkivet.dk/crawl/setup/duplicatereductionjobs?majorversion=1&minorversion=0&harvestid=2&harvestnum=1&jobid=8 127.0.1.1 20080602072228 text/plain 1
6
metadata://netarkivet.dk/crawl/setup/order.xml?heritrixVersion=1.12.1b&harvestid=2&jobid=8 127.0.1.1 20080602072229 text/xml 21722
<?xml version="1.0" encoding="UTF-8"?>

<crawl-order xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="heritrix_settings.xsd">
  <meta>
    <name>default_orderxml</name>
    <description>Default Profile</description>
    <operator>Admin</operator>
    <organization/>
    <audience/>
    <date>20080118111217</date>
  </meta>
  <controller>
    <string name="settings-directory">settings</string>
    <string name="disk-path">/home/kfc/build/netarchivesuite/scripts/simple_harvest/server2/8_1212391347947</string>
    <string name="logs-path">logs</string>
    <string name="checkpoints-path">checkpoints</string>
    <string name="state-path">state</string>
    <string name="scratch-path">scratch</string>
    <long name="max-bytes-download">0</long>
    <long name="max-document-download">0</long>
    <long name="max-time-sec">0</long>
    <integer name="max-toe-threads">50</integer>
    <integer name="recorder-out-buffer-bytes">4096</integer>
    <integer name="recorder-in-buffer-bytes">65536</integer>
    <integer name="bdb-cache-percent">0</integer>
    <!-- DecidingScope migrated from DomainScope -->
    <newObject name="scope" class="org.archive.crawler.deciderules.DecidingScope">
      <boolean name="enabled">true</boolean>
      <string name="seedsfile">/home/kfc/build/netarchivesuite/scripts/simple_harvest/server2/8_1212391347947/seeds.txt</string>
      <boolean name="reread-seeds-on-config">true</boolean>
      <!-- DecideRuleSequence. Multiple DecideRules applied in order with last non-PASS the resulting decision -->
      <newObject name="decide-rules" class="org.archive.crawler.deciderules.DecideRuleSequence">
        <map name="rules">
          <newObject name="rejectByDefault" class="org.archive.crawler.deciderules.RejectDecideRule"/>
          <newObject name="acceptURIFromSeedDomains" class="dk.netarkivet.harvester.harvesting.OnNSDomainsDecideRule">
            <string name="decision">ACCEPT</string>
            <string name="surts-source-file"/>
            <boolean name="seeds-as-surt-prefixes">true</boolean>
            <string name="surts-dump-file"/>
            <boolean name="also-check-via">false</boolean>
            <boolean name="rebuild-on-reconfig">true</boolean>
          </newObject>
          <newObject name="rejectIfTooManyHops" class="org.archive.crawler.deciderules.TooManyHopsDecideRule">
            <integer name="max-hops">25</integer>
          </newObject>
          <newObject name="rejectIfPathological" class="org.archive.crawler.deciderules.PathologicalPathDecideRule">
            <integer name="max-repetitions">3</integer>
          </newObject>
          <newObject name="acceptIfTranscluded" class="org.archive.crawler.deciderules.TransclusionDecideRule">
            <integer name="max-trans-hops">25</integer>
            <integer name="max-speculative-hops">1</integer>
          </newObject>
          <newObject name="pathdepthfilter" class="org.archive.crawler.deciderules.TooManyPathSegmentsDecideRule">
            <integer name="max-path-depth">20</integer>
          </newObject>
          <newObject name="global_crawlertraps" class="org.archive.crawler.deciderules.MatchesListRegExpDecideRule">
            <string name="decision">REJECT</string>
            <string name="list-logic">OR</string>
            <stringList name="regexp-list">
              <string>.*core\.UserAdmin.*core\.UserLogin.*</string>
              <string>.*core\.UserAdmin.*register\.UserSelfRegistration.*</string>
              <string>.*\/w\/index\.php\?title=Speci[ae]l:Recentchanges.*</string>
              <string>.*act=calendar&amp;cal_id=.*</string>
              <string>.*advCalendar_pi.*</string>
              <string>.*cal\.asp\?date=.*</string>
              <string>.*cal\.asp\?view=monthly&amp;date=.*</string>
              <string>.*cal\.asp\?view=weekly&amp;date=.*</string>
              <string>.*cal\.asp\?view=yearly&amp;date=.*</string>
              <string>.*cal\.asp\?view=yearly&amp;year=.*</string>
              <string>.*cal\/cal_day\.php\?op=day&amp;date=.*</string>
              <string>.*cal\/cal_week\.php\?op=week&amp;date=.*</string>
              <string>.*cal\/calendar\.php\?op=cal&amp;month=.*</string>
              <string>.*cal\/yearcal\.php\?op=yearcal&amp;ycyear=.*</string>
              <string>.*calendar\.asp\?calmonth=.*</string>
              <string>.*calendar\.asp\?qMonth=.*</string>
              <string>.*calendar\.php\?sid=.*</string>
              <string>.*calendar\.php\?start=.*</string>
              <string>.*calendar\.php\?Y=.*</string>
              <string>.*calendar\/\?CLmDemo_horizontal=.*</string>
              <string>.*calendar_menu\/calendar\.php\?.*</string>
              <string>.*calendar_scheduler\.php\?d=.*</string>
              <string>.*calendar_year\.asp\?qYear=.*</string>
              <string>.*calendarix\/calendar\.php\?op=.*</string>
              <string>.*calendarix\/yearcal\.php\?op=.*</string>
              <string>.*calender\/default\.asp\?month=.*</string>
              <string>.*Default\.asp\?month=.*</string>
              <string>.*events\.asp\?cat=0&amp;mDate=.*</string>
              <string>.*events\.asp\?cat=1&amp;mDate=.*</string>
              <string>.*events\.asp\?MONTH=.*</string>
              <string>.*events\.asp\?month=.*</string>
              <string>.*index\.php\?iDate=.*</string>
              <string>.*index\.php\?module=PostCalendar&amp;func=view.*</string>
              <string>.*index\.php\?option=com_events&amp;task=view.*</string>
              <string>.*index\.php\?option=com_events&amp;task=view_day&amp;year=.*</string>
              <string>.*index\.php\?option=com_events&amp;task=view_detail&amp;year=.*</string>
              <string>.*index\.php\?option=com_events&amp;task=view_month&amp;year=.*</string>
              <string>.*index\.php\?option=com_events&amp;task=view_week&amp;year=.*</string>
              <string>.*index\.php\?option=com_events&amp;task=view_year&amp;year=.*</string>
              <string>.*index\.php\?option=com_extcalendar&amp;Itemid.*</string>
              <string>.*modules\.php\?name=Calendar&amp;op=modload&amp;file=index.*</string>
              <string>.*modules\.php\?name=vwar&amp;file=calendar&amp;action=list&amp;month=.*</string>
              <string>.*modules\.php\?name=vwar&amp;file=calendar.*</string>
              <string>.*modules\.php\?name=vWar&amp;mod=calendar.*</string>
              <string>.*modules\/piCal\/index\.php\?caldate=.*</string>
              <string>.*modules\/piCal\/index\.php\?cid=.*</string>
              <string>.*option,com_events\/task,view_day\/year.*</string>
              <string>.*option,com_events\/task,view_month\/year.*</string>
              <string>.*option,com_extcalendar\/Itemid.*</string>
              <string>.*task,view_month\/year.*</string>
              <string>.*shopping_cart\.php.*</string>
              <string>.*action.add_product.*</string>
              <string>.*action.remove_product.*</string>
              <string>.*action.buy_now.*</string>
              <string>.*checkout_payment\.php.*</string>
              <string>.*login.*login.*login.*login.*</string>
              <string>.*homepage_calendar\.asp.*</string>
              <string>.*MediaWiki.*Movearticle.*</string>
              <string>.*index\.php.*action=edit.*</string>
              <string>.*comcast\.net.*othastar.*</string>
              <string>.*Login.*Login.*Login.*</string>
              <string>.*redir.*redir.*redir.*</string>
              <string>.*bookingsystemtime\.asp\?dato=.*</string>
              <string>.*bookingsystem\.asp\?date=.*</string>
              <string>.*cart\.asp\?mode=add.*</string>
              <string>.*\/photo.*\/photo.*\/photo.*</string>
              <string>.*\/skins.*\/skins.*\/skins.*</string>
              <string>.*\/scripts.*\/scripts.*\/scripts.*</string>
              <string>.*\/styles.*\/styles.*\/styles.*</string>
              <string>.*\/coppermine\/login\.php\?referer=.*</string>
              <string>.*\/images.*\/images.*\/images.*</string>
              <string>.*\/stories.*\/stories.*\/stories.*</string>
            </stringList>
          </newObject>
        </map>
        <!-- end rules -->
      </newObject>
      <!-- end decide-rules -->
    </newObject>
    <!-- End DecidingScope -->
    <map name="http-headers">
      <string name="user-agent">Mozilla/5.0 (compatible; heritrix/1.12.1 +http://my_website.com/my_infopage.html)</string>
      <string name="from">my_email@my_website.com</string>
    </map>
    <newObject name="robots-honoring-policy" class="org.archive.crawler.datamodel.RobotsHonoringPolicy">
      <string name="type">ignore</string>
      <boolean name="masquerade">false</boolean>
      <text name="custom-robots"/>
      <stringList name="user-agents"/>
    </newObject>
    <newObject name="frontier" class="org.archive.crawler.frontier.BdbFrontier">
      <float name="delay-factor">1.0</float>
      <integer name="max-delay-ms">1000</integer>
      <integer name="min-delay-ms">300</integer>
      <integer name="max-retries">3</integer>
      <long name="retry-delay-seconds">300</long>
      <integer name="preference-embed-hops">1</integer>
      <integer name="total-bandwidth-usage-KB-sec">1500</integer>
      <integer name="max-per-host-bandwidth-usage-KB-sec">500</integer>
      <string name="queue-assignment-policy">dk.netarkivet.harvester.harvesting.DomainnameQueueAssignmentPolicy</string>
      <string name="force-queue-assignment"/>
      <boolean name="pause-at-start">false</boolean>
      <boolean name="pause-at-finish">false</boolean>
      <boolean name="source-tag-seeds">false</boolean>
      <boolean name="recovery-log-enabled">false</boolean>
      <boolean name="hold-queues">true</boolean>
      <integer name="balance-replenish-amount">3000</integer>
      <integer name="error-penalty-amount">100</integer>
      <long name="queue-total-budget">-1</long>
      <string name="cost-policy">org.archive.crawler.frontier.UnitCostAssignmentPolicy</string>
      <long name="snooze-deactivate-ms">300000</long>
      <integer name="target-ready-backlog">50</integer>
      <string name="uri-included-structure">org.archive.crawler.util.BdbUriUniqFilter</string>
    </newObject>
    <map name="uri-canonicalization-rules">
      <newObject name="Lowercase" class="org.archive.crawler.url.canonicalize.LowercaseRule">
        <boolean name="enabled">true</boolean>
      </newObject>
      <newObject name="Userinfo" class="org.archive.crawler.url.canonicalize.StripUserinfoRule">
        <boolean name="enabled">true</boolean>
      </newObject>
      <newObject name="WWW" class="org.archive.crawler.url.canonicalize.StripWWWRule">
        <boolean name="enabled">false</boolean>
      </newObject>
      <newObject name="SessionIDs" class="org.archive.crawler.url.canonicalize.StripSessionIDs">
        <boolean name="enabled">true</boolean>
      </newObject>
      <newObject name="QueryStrPrefix" class="org.archive.crawler.url.canonicalize.FixupQueryStr">
        <boolean name="enabled">true</boolean>
      </newObject>
    </map>
    <!-- Heritrix pre-fetch processors -->
    <map name="pre-fetch-processors">
      <newObject name="QuotaEnforcer" class="org.archive.crawler.prefetch.QuotaEnforcer">
        <boolean name="force-retire">false</boolean>
        <boolean name="enabled">true</boolean>
        <newObject name="QuotaEnforcer#decide-rules" class="org.archive.crawler.deciderules.DecideRuleSequence">
          <map name="rules"/>
        </newObject>
        <long name="server-max-fetch-successes">-1</long>
        <long name="server-max-success-kb">-1</long>
        <long name="server-max-fetch-responses">-1</long>
        <long name="server-max-all-kb">-1</long>
        <long name="host-max-fetch-successes">-1</long>
        <long name="host-max-success-kb">-1</long>
        <long name="host-max-fetch-responses">-1</long>
        <long name="host-max-all-kb">-1</long>
        <long name="group-max-fetch-successes">-1</long>
        <long name="group-max-success-kb">-1</long>
        <long name="group-max-fetch-responses">-1</long>
        <long name="group-max-all-kb">9766</long>
      </newObject>
      <newObject name="Preselector" class="org.archive.crawler.prefetch.Preselector">
        <boolean name="enabled">true</boolean>
        <newObject name="Preselector#decide-rules" class="org.archive.crawler.deciderules.DecideRuleSequence">
          <map name="rules"/>
        </newObject>
        <boolean name="override-logger">false</boolean>
        <boolean name="recheck-scope">true</boolean>
        <boolean name="block-all">false</boolean>
        <string name="block-by-regexp"/>
        <string name="allow-by-regexp"/>
      </newObject>
      <newObject name="Preprocessor" class="org.archive.crawler.prefetch.PreconditionEnforcer">
        <boolean name="enabled">true</boolean>
        <newObject name="Preprocessor#decide-rules" class="org.archive.crawler.deciderules.DecideRuleSequence">
          <map name="rules"/>
        </newObject>
        <integer name="ip-validity-duration-seconds">21600</integer>
        <integer name="robot-validity-duration-seconds">86400</integer>
        <boolean name="calculate-robots-only">false</boolean>
      </newObject>
    </map>
    <!--End of Heritrix pre-fetch processors -->
    <!-- Heritrix fetch processors -->
    <map name="fetch-processors">
      <newObject name="DNS" class="org.archive.crawler.fetcher.FetchDNS">
        <boolean name="enabled">true</boolean>
        <newObject name="DNS#decide-rules" class="org.archive.crawler.deciderules.DecideRuleSequence">
          <map name="rules"/>
        </newObject>
        <boolean name="accept-non-dns-resolves">false</boolean>
        <boolean name="digest-content">true</boolean>
        <string name="digest-algorithm">sha1</string>
      </newObject>
      <newObject name="HTTP" class="org.archive.crawler.fetcher.FetchHTTP">
        <boolean name="enabled">true</boolean>
        <newObject name="HTTP#decide-rules" class="org.archive.crawler.deciderules.DecideRuleSequence">
          <map name="rules"/>
        </newObject>
        <newObject name="midfetch-decide-rules" class="org.archive.crawler.deciderules.DecideRuleSequence">
          <map name="rules"/>
        </newObject>
        <integer name="timeout-seconds">1200</integer>
        <integer name="sotimeout-ms">20000</integer>
        <integer name="fetch-bandwidth">0</integer>
        <long name="max-length-bytes">0</long>
        <boolean name="ignore-cookies">false</boolean>
        <boolean name="use-bdb-for-cookies">true</boolean>
        <string name="load-cookies-from-file"/>
        <string name="save-cookies-to-file"/>
        <string name="trust-level">open</string>
        <stringList name="accept-headers"/>
        <string name="http-proxy-host"/>
        <string name="http-proxy-port"/>
        <string name="default-encoding">ISO-8859-1</string>
        <boolean name="digest-content">true</boolean>
        <string name="digest-algorithm">sha1</string>
        <boolean name="send-if-modified-since">true</boolean>
        <boolean name="send-if-none-match">true</boolean>
        <boolean name="send-connection-close">true</boolean>
        <boolean name="send-referer">true</boolean>
        <boolean name="send-range">false</boolean>
        <string name="bind-address"/>
      </newObject>
    </map>
    <!-- end of Heritrix Fetch processors -->
    <!-- Heritrix extract processors -->
    <map name="extract-processors">
      <newObject name="ExtractorHTTP" class="org.archive.crawler.extractor.ExtractorHTTP">
        <boolean name="enabled">true</boolean>
        <newObject name="ExtractorHTTP#decide-rules" class="org.archive.crawler.deciderules.DecideRuleSequence">
          <map name="rules"/>
        </newObject>
      </newObject>
      <newObject name="ExtractorHTML" class="org.archive.crawler.extractor.ExtractorHTML">
        <boolean name="enabled">true</boolean>
        <newObject name="ExtractorHTML#decide-rules" class="org.archive.crawler.deciderules.DecideRuleSequence">
          <map name="rules"/>
        </newObject>
        <boolean name="extract-javascript">true</boolean>
        <boolean name="treat-frames-as-embed-links">true</boolean>
        <boolean name="ignore-form-action-urls">true</boolean>
        <boolean name="overly-eager-link-detection">true</boolean>
        <boolean name="ignore-unexpected-html">true</boolean>
      </newObject>
      <newObject name="ExtractorCSS" class="org.archive.crawler.extractor.ExtractorCSS">
        <boolean name="enabled">true</boolean>
        <newObject name="ExtractorCSS#decide-rules" class="org.archive.crawler.deciderules.DecideRuleSequence">
          <map name="rules"/>
        </newObject>
      </newObject>
      <newObject name="ExtractorJS" class="org.archive.crawler.extractor.ExtractorJS">
        <boolean name="enabled">true</boolean>
        <newObject name="ExtractorJS#decide-rules" class="org.archive.crawler.deciderules.DecideRuleSequence">
          <map name="rules"/>
        </newObject>
      </newObject>
      <newObject name="ExtractorSWF" class="org.archive.crawler.extractor.ExtractorSWF">
        <boolean name="enabled">true</boolean>
        <newObject name="ExtractorSWF#decide-rules" class="org.archive.crawler.deciderules.DecideRuleSequence">
          <map name="rules"/>
        </newObject>
      </newObject>
    </map>
    <!-- end of Heritrix extract processors -->
    <!-- Heritrix write processors -->
    <map name="write-processors">
      <newObject name="DeDuplicator" class="is.hi.bok.deduplicator.DeDuplicator">
        <boolean name="enabled">true</boolean>
        <map name="filters"/>
        <string name="index-location">/home/kfc/build/netarchivesuite/scripts/simple_harvest/cache/DEDUP_CRAWL_LOG/6-cache</string>
        <string name="matching-method">By URL</string>
        <boolean name="try-equivalent">true</boolean>
        <boolean name="change-content-size">false</boolean>
        <string name="mime-filter">^text/.*</string>
        <string name="filter-mode">Blacklist</string>
        <string name="analysis-mode">Timestamp</string>
        <string name="log-level">SEVERE</string>
        <string name="origin"/>
        <string name="origin-handling">Use index information</string>
        <boolean name="stats-per-host">true</boolean>
      </newObject>
      <newObject name="Archiver" class="org.archive.crawler.writer.ARCWriterProcessor">
        <boolean name="enabled">true</boolean>
        <newObject name="Archiver#decide-rules" class="org.archive.crawler.deciderules.DecideRuleSequence">
          <map name="rules"/>
        </newObject>
        <boolean name="compress">false</boolean>
        <string name="prefix">8-2</string>
        <string name="suffix">${HOSTNAME}</string>
        <integer name="max-size-bytes">100000000</integer>
        <stringList name="path">
          <string>arcs</string>
        </stringList>
        <integer name="pool-max-active">5</integer>
        <integer name="pool-max-wait">300000</integer>
        <long name="total-bytes-to-write">0</long>
        <boolean name="skip-identical-digests">false</boolean>
      </newObject>
    </map>
    <!-- End of Heritrix write processors -->
    <!-- Heritrix post processors -->
    <map name="post-processors">
      <newObject name="Updater" class="org.archive.crawler.postprocessor.CrawlStateUpdater">
        <boolean name="enabled">true</boolean>
        <newObject name="Updater#decide-rules" class="org.archive.crawler.deciderules.DecideRuleSequence">
          <map name="rules"/>
        </newObject>
      </newObject>
      <newObject name="LinksScoper" class="org.archive.crawler.postprocessor.LinksScoper">
        <boolean name="enabled">true</boolean>
        <newObject name="LinksScoper#decide-rules" class="org.archive.crawler.deciderules.DecideRuleSequence">
          <map name="rules"/>
        </newObject>
        <boolean name="override-logger">false</boolean>
        <boolean name="seed-redirects-new-seed">false</boolean>
        <integer name="preference-depth-hops">-1</integer>
        <newObject name="scope-rejected-url-rules" class="org.archive.crawler.deciderules.DecideRuleSequence">
          <map name="rules"/>
        </newObject>
      </newObject>
      <newObject name="Scheduler" class="org.archive.crawler.postprocessor.FrontierScheduler">
        <boolean name="enabled">true</boolean>
        <newObject name="Scheduler#decide-rules" class="org.archive.crawler.deciderules.DecideRuleSequence">
          <map name="rules"/>
        </newObject>
      </newObject>
      <newObject name="ContentSize" class="dk.netarkivet.harvester.harvesting.ContentSizeAnnotationPostProcessor">
        <boolean name="enabled">true</boolean>
        <newObject name="ContentSize#decide-rules" class="org.archive.crawler.deciderules.DecideRuleSequence">
          <map name="rules"/>
        </newObject>
      </newObject>
    </map>
    <!-- end of Heritrix post processors -->
    <map name="loggers">
      <newObject name="crawl-statistics" class="org.archive.crawler.admin.StatisticsTracker">
        <integer name="interval-seconds">20</integer>
      </newObject>
    </map>
    <string name="recover-path"/>
    <boolean name="checkpoint-copy-bdbje-logs">true</boolean>
    <boolean name="recover-retain-failures">false</boolean>
    <newObject name="credential-store" class="org.archive.crawler.datamodel.CredentialStore">
      <map name="credentials"/>
    </newObject>
  </controller>
</crawl-order>

metadata://netarkivet.dk/crawl/setup/harvestInfo.xml?heritrixVersion=1.12.1b&harvestid=2&jobid=8 127.0.1.1 20080602072227 text/xml 379
<?xml version="1.0" encoding="UTF-8"?>

<harvestInfo>
  <version>0.2</version>
  <jobId>8</jobId>
  <priority>HIGHPRIORITY</priority>
  <harvestNum>1</harvestNum>
  <origHarvestDefinitionID>2</origHarvestDefinitionID>
  <maxBytesPerDomain>10000000</maxBytesPerDomain>
  <maxObjectsPerDomain>-1</maxObjectsPerDomain>
  <orderXMLName>default_orderxml</orderXMLName>
</harvestInfo>

metadata://netarkivet.dk/crawl/setup/seeds.txt?heritrixVersion=1.12.1b&harvestid=2&jobid=8 127.0.1.1 20080602072228 text/plain 46
http://www.netarkivet.dk
http://www.kaarefc.dk
metadata://netarkivet.dk/crawl/reports/crawl-report.txt?heritrixVersion=1.12.1b&harvestid=2&jobid=8 127.0.1.1 20080602075245 text/plain 309
Crawl Name: default_orderxml
Crawl Status: Finished - Ended by operator
Duration Time: 1m13s976ms
Total Seeds Crawled: 0
Total Seeds not Crawled: 2
Total Hosts Crawled: -1
Total Documents Crawled: 0
Processed docs/sec: 0
Bandwidth in Kbytes/sec: 0
Total Raw Data Size in Bytes: 0 (0 B) 
Novel Bytes: 0 (0 B) 

metadata://netarkivet.dk/crawl/reports/frontier-report.txt?heritrixVersion=1.12.1b&harvestid=2&jobid=8 127.0.1.1 20080602075245 text/plain 509

 -----===== IN-PROCESS QUEUES =====-----

 -----===== READY QUEUES =====-----
queue currentSize totalEnqueues sessionBalance lastCost (averageCost) lastDequeueTime wakeTime totalSpend/totalBudget errorCount lastPeekUri lastQueuedUri
netarkivet.dk 2 2 2999 1(1) - - 1/-1 0 dns:www.netarkivet.dk dns:www.netarkivet.dk
kaarefc.dk 2 2 2999 1(1) - - 1/-1 0 dns:www.kaarefc.dk dns:www.kaarefc.dk

 -----===== SNOOZED QUEUES =====-----

 -----===== INACTIVE QUEUES =====-----

 -----===== RETIRED QUEUES =====-----

metadata://netarkivet.dk/crawl/reports/hosts-report.txt?heritrixVersion=1.12.1b&harvestid=2&jobid=8 127.0.1.1 20080602075245 text/plain 24
[#urls] [#bytes] [host]

metadata://netarkivet.dk/crawl/reports/mimetype-report.txt?heritrixVersion=1.12.1b&harvestid=2&jobid=8 127.0.1.1 20080602075245 text/plain 30
[#urls] [#bytes] [mime-types]

metadata://netarkivet.dk/crawl/reports/processors-report.txt?heritrixVersion=1.12.1b&harvestid=2&jobid=8 127.0.1.1 20080602075245 text/plain 1776
Processors report - 200806020752
  Job being crawled:    default_orderxml
  Number of Processors: 16
  NOTE: Some processors may not return a report!

Processor: org.archive.crawler.fetcher.FetchHTTP
  Function:          Fetch HTTP URIs
  CrawlURIs handled: 0
  Recovery retries:   0

Processor: org.archive.crawler.extractor.ExtractorHTTP
  Function:          Extracts URIs from HTTP response headers
  CrawlURIs handled: 0
  Links extracted:   0

Processor: org.archive.crawler.extractor.ExtractorHTML
  Function:          Link extraction on HTML documents
  CrawlURIs handled: 0
  Links extracted:   0

Processor: org.archive.crawler.extractor.ExtractorCSS
  Function:          Link extraction on Cascading Style Sheets (.css)
  CrawlURIs handled: 0
  Links extracted:   0

Processor: org.archive.crawler.extractor.ExtractorJS
  Function:          Link extraction on JavaScript code
  CrawlURIs handled: 0
  Links extracted:   0

Processor: org.archive.crawler.extractor.ExtractorSWF
  Function:          Link extraction on Shockwave Flash documents (.swf)
  CrawlURIs handled: 0
  Links extracted:   0

Processor: is.hi.bok.digest.DeDuplicator
  Function:          Abort processing of duplicate records
                     - Lookup by url in use
  Total handled:     0
  Duplicates found:  0 Na%
  Bytes total:       0 (0 B)
  Bytes discarded:   0 (0 B) Na%
  New (no hits):     0
  Exact hits:        0
  Equivalent hits:   0
  Timestamp predicts: (Where exact URL existed in the index)
  Change correctly:  0
  Change falsly:     0
  Non-change correct:0
  Non-change falsly: 0
  Missing timpestamp:0
  [Host] [total] [duplicates] [bytes] [bytes discarded] [new] [exact] [equiv] [change correct] [change falsly] [non-change correct] [non-change falsly] [no timestamp]


metadata://netarkivet.dk/crawl/reports/responsecode-report.txt?heritrixVersion=1.12.1b&harvestid=2&jobid=8 127.0.1.1 20080602075245 text/plain 18
[rescode] [#urls]

metadata://netarkivet.dk/crawl/reports/seeds-report.txt?heritrixVersion=1.12.1b&harvestid=2&jobid=8 127.0.1.1 20080602075245 text/plain 109
[code] [status] [seed] [redirect]
0 NOTCRAWLED http://www.kaarefc.dk/
0 NOTCRAWLED http://www.netarkivet.dk/

metadata://netarkivet.dk/crawl/logs/crawl.log?heritrixVersion=1.12.1b&harvestid=2&jobid=8 127.0.1.1 20080602072231 text/plain 0

metadata://netarkivet.dk/crawl/logs/local-errors.log?heritrixVersion=1.12.1b&harvestid=2&jobid=8 127.0.1.1 20080602072231 text/plain 0

metadata://netarkivet.dk/crawl/logs/progress-statistics.log?heritrixVersion=1.12.1b&harvestid=2&jobid=8 127.0.1.1 20080602075245 text/plain 1334
20080602072232 CRAWL RESUMED - Running
           timestamp  discovered      queued   downloaded       doc/s(avg)  KB/s(avg)   dl-failures   busy-thread   mem-use-KB  heap-size-KB   congestion   max-depth   avg-depth
2008-06-02T07:22:52Z           4           4            0             0(0)       0(0)             0             0        15778         24256            1           2           2
2008-06-02T07:23:12Z           4           4            0             0(0)       0(0)             0             0        16691         24256            1           2           2
2008-06-02T07:23:32Z           4           4            0             0(0)       0(0)             0             0        17700         24256            1           2           2
20080602072346 CRAWL WAITING - Pausing - Waiting for threads to finish
2008-06-02T07:23:46Z           4           4            0             0(0)       0(0)             0             0        12400         25984            1           2           2
20080602072346 CRAWL PAUSED - Paused
20080602075245 CRAWL ENDING - Finished - Ended by operator
2008-06-02T07:52:45Z           4           4            0             0(0)       0(0)             0             0        13256         27648            ∞           2           2
20080602075245 CRAWL ENDED - Finished - Ended by operator

metadata://netarkivet.dk/crawl/logs/runtime-errors.log?heritrixVersion=1.12.1b&harvestid=2&jobid=8 127.0.1.1 20080602072231 text/plain 0

metadata://netarkivet.dk/crawl/logs/uri-errors.log?heritrixVersion=1.12.1b&harvestid=2&jobid=8 127.0.1.1 20080602072231 text/plain 0

metadata://netarkivet.dk/crawl/logs/heritrix.out?heritrixVersion=1.12.1b&harvestid=2&jobid=8 127.0.1.1 20080602075248 text/plain 12421
The Heritrix process is started in the following environment
 (note that some entries will be changed by the starting JVM):
CATALINA_HOME=/home/kfc/Projects/Fedora/Fedora-3.0b1-svn6557/tomcat
CLASSPATH=/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/heritrix-1.12.1b.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/bsh-2.0b4.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/commons-cli-1.0.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/commons-codec-1.3.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/commons-collections-3.1.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/commons-httpclient-3.0.1.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/commons-lang-2.3.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/commons-logging-1.0.4.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/commons-pool-1.3.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/dnsjava-2.0.3.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/fastutil-5.0.3-heritrix-subset-1.0.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/itext-1.2.0.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/jasper-compiler-tomcat-4.1.30.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/jasper-runtime-tomcat-4.1.30.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/javaswf-CVS-SNAPSHOT-1.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/je-3.2.23.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/jericho-html-2.3.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/jets3t-0.5.0.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/jetty-4.2.23.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/junit-3.8.2.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/libidn-0.5.9.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/mg4j-1.0.1.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/poi-2.0-RC1-20031102.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/poi-scratchpad-2.0-RC1-20031102.jar:/home/kfc/build/netarchivesuite/scripts/simple_harvest/lib/heritrix/lib/servlet-tomcat-4.1.30.jar::/home/kfc/build/netarchivesuite/lib/dk.netarkivet.archive.jar:/home/kfc/build/netarchivesuite/lib/dk.netarkivet.viewerproxy.jar:/home/kfc/build/netarchivesuite/lib/dk.netarkivet.harvester.jar:/home/kfc/build/netarchivesuite/lib/dk.netarkivet.monitor.jar
COLORTERM=gnome-terminal
DBUS_SESSION_BUS_ADDRESS=unix:abstract=/tmp/dbus-WJ2auapvxN,guid=dceb6eb2f6f56ea8367f081e48438c19
DESKTOP_SESSION=default
DESKTOP_STARTUP_ID=
DISPLAY=:0.0
FEDORA_HOME=/home/kfc/Projects/Fedora/Fedora-3.0b1-svn6557
FILETRANSFERPORT=8045
GDMSESSION=default
GDM_LANG=en_DK.UTF-8
GDM_XSERVER_LOCATION=local
GNOME_DESKTOP_SESSION_ID=Default
GNOME_KEYRING_PID=6264
GNOME_KEYRING_SOCKET=/tmp/keyring-tE5XPx/socket
GPG_AGENT_INFO=/tmp/seahorse-PQbV5D/S.gpg-agent:6377:1
GTK_RC_FILES=/etc/gtk/gtkrc:/home/kfc/.gtkrc-1.2-gnome2
HERITRIXPORT=8094
HISTCONTROL=ignoreboth
HOME=/home/kfc
HTTPPORT=8076
IMQ=/opt/mq/bin/imqbrokerd
IMQ_JAVAHOME=/usr/lib/jvm/java-1.5.0-sun
JAVA_HOME=/usr/lib/jvm/java-1.5.0-sun
JDK_HOME=/usr/lib/jvm/java-6-sun
JMXPORT=8108
KEEPDATA=1
LANG=en_DK.UTF-8
LD_LIBRARY_PATH=/usr/lib/jvm/java-1.5.0-sun-1.5.0.15/jre/lib/i386/server:/usr/lib/jvm/java-1.5.0-sun-1.5.0.15/jre/lib/i386:/usr/lib/jvm/java-1.5.0-sun-1.5.0.15/jre/../lib/i386
LESSCLOSE=/usr/bin/lesspipe %s %s
LESSOPEN=| /usr/bin/lesspipe %s
LOGNAME=kfc
LS_COLORS=no=00:fi=00:di=01;34:ln=01;36:pi=40;33:so=01;35:do=01;35:bd=40;33;01:cd=40;33;01:or=40;31;01:su=37;41:sg=30;43:tw=30;42:ow=34;42:st=37;44:ex=01;32:*.tar=01;31:*.tgz=01;31:*.svgz=01;31:*.arj=01;31:*.taz=01;31:*.lzh=01;31:*.lzma=01;31:*.zip=01;31:*.z=01;31:*.Z=01;31:*.dz=01;31:*.gz=01;31:*.bz2=01;31:*.bz=01;31:*.tbz2=01;31:*.tz=01;31:*.deb=01;31:*.rpm=01;31:*.jar=01;31:*.rar=01;31:*.ace=01;31:*.zoo=01;31:*.cpio=01;31:*.7z=01;31:*.rz=01;31:*.jpg=01;35:*.jpeg=01;35:*.gif=01;35:*.bmp=01;35:*.pbm=01;35:*.pgm=01;35:*.ppm=01;35:*.tga=01;35:*.xbm=01;35:*.xpm=01;35:*.tif=01;35:*.tiff=01;35:*.png=01;35:*.svg=01;35:*.mng=01;35:*.pcx=01;35:*.mov=01;35:*.mpg=01;35:*.mpeg=01;35:*.m2v=01;35:*.mkv=01;35:*.ogm=01;35:*.mp4=01;35:*.m4v=01;35:*.mp4v=01;35:*.vob=01;35:*.qt=01;35:*.nuv=01;35:*.wmv=01;35:*.asf=01;35:*.rm=01;35:*.rmvb=01;35:*.flc=01;35:*.avi=01;35:*.fli=01;35:*.gl=01;35:*.dl=01;35:*.xcf=01;35:*.xwd=01;35:*.yuv=01;35:*.aac=00;36:*.au=00;36:*.flac=00;36:*.mid=00;36:*.midi=00;36:*.mka=00;36:*.mp3=00;36:*.mpc=00;36:*.ogg=00;36:*.ra=00;36:*.wav=00;36:
NLSPATH=/usr/dt/lib/nls/msg/%L/%N.cat
PATH=/home/kfc/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games
PWD=/home/kfc/build/netarchivesuite/scripts/simple_harvest
SESSION_MANAGER=local/david:/tmp/.ICE-unix/6265
SHELL=/bin/bash
SHLVL=2
SSH_AUTH_SOCK=/tmp/keyring-tE5XPx/ssh
TERM=xterm
USER=kfc
USERNAME=kfc
WINDOWID=77594658
WINDOWPATH=7
WINDOWPOS=7
XAUTHORITY=/home/kfc/.Xauthority
XDG_DATA_DIRS=/usr/local/share/:/usr/share/:/usr/share/gdm/
XDG_SESSION_COOKIE=9fe3ccd37aff8d986122f70046fd01a1-1212386327.498747-1668684687
XFILESEARCHPATH=/usr/dt/app-defaults/%L/Dt
XTERM_LOCALE=en_DK.UTF-8
XTERM_SHELL=/usr/lib/jvm/java-1.5.0-sun/bin/java
XTERM_VERSION=XTerm(229)
_=/usr/bin/xterm
Process properties:
com.sun.management.jmxremote=
dk.netarkivet.monitorsettings.file=/home/kfc/build/netarchivesuite/scripts/simple_harvest/monitor_settings.xml
dk.netarkivet.quickstart.basedir=/home/kfc/build/netarchivesuite
dk.netarkivet.settings.file=/home/kfc/build/netarchivesuite/scripts/simple_harvest/settings.xml
file.encoding=UTF-8
file.encoding.pkg=sun.io
file.separator=/
heritrix.version=1.12.1b
java.awt.graphicsenv=sun.awt.X11GraphicsEnvironment
java.awt.printerjob=sun.print.PSPrinterJob
java.class.path=:/home/kfc/build/netarchivesuite/lib/dk.netarkivet.archive.jar:/home/kfc/build/netarchivesuite/lib/dk.netarkivet.viewerproxy.jar:/home/kfc/build/netarchivesuite/lib/dk.netarkivet.harvester.jar:/home/kfc/build/netarchivesuite/lib/dk.netarkivet.monitor.jar
java.class.version=49.0
java.endorsed.dirs=/usr/lib/jvm/java-1.5.0-sun-1.5.0.15/jre/lib/endorsed
java.ext.dirs=/usr/lib/jvm/java-1.5.0-sun-1.5.0.15/jre/lib/ext
java.home=/usr/lib/jvm/java-1.5.0-sun-1.5.0.15/jre
java.io.tmpdir=/tmp
java.library.path=/usr/lib/jvm/java-1.5.0-sun-1.5.0.15/jre/lib/i386/server:/usr/lib/jvm/java-1.5.0-sun-1.5.0.15/jre/lib/i386:/usr/lib/jvm/java-1.5.0-sun-1.5.0.15/jre/../lib/i386
java.rmi.server.randomIDs=true
java.runtime.name=Java(TM) 2 Runtime Environment, Standard Edition
java.runtime.version=1.5.0_15-b04
java.security.manager=
java.security.policy=/home/kfc/build/netarchivesuite/conf/quickstart.security.policy
java.specification.name=Java Platform API Specification
java.specification.vendor=Sun Microsystems Inc.
java.specification.version=1.5
java.util.logging.config.file=/home/kfc/build/netarchivesuite/scripts/simple_harvest/log/log.prop.HarvestController-8076
java.vendor=Sun Microsystems Inc.
java.vendor.url=http://java.sun.com/
java.vendor.url.bug=http://java.sun.com/cgi-bin/bugreport.cgi
java.version=1.5.0_15
java.vm.info=mixed mode
java.vm.name=Java HotSpot(TM) Server VM
java.vm.specification.name=Java Virtual Machine Specification
java.vm.specification.vendor=Sun Microsystems Inc.
java.vm.specification.version=1.0
java.vm.vendor=Sun Microsystems Inc.
java.vm.version=1.5.0_15-b04
line.separator=

org.archive.crawler.frontier.AbstractFrontier.queue-assignment-policy=org.archive.crawler.frontier.HostnameQueueAssignmentPolicy,org.archive.crawler.frontier.IPQueueAssignmentPolicy,org.archive.crawler.frontier.BucketQueueAssignmentPolicy,org.archive.crawler.frontier.SurtAuthorityQueueAssignmentPolicy,dk.netarkivet.harvester.harvesting.DomainnameQueueAssignmentPolicy
os.arch=i386
os.name=Linux
os.version=2.6.24-17-generic
path.separator=:
settings.common.http.port=8076
settings.common.jmx.passwordFile=/home/kfc/build/netarchivesuite/conf/jmxremote.password
settings.common.jmx.port=8107
settings.common.jmx.rmiPort=8207
settings.harvester.harvesting.heritrix.guiPort=8092
settings.harvester.harvesting.heritrix.jmxPort=8093
settings.harvester.harvesting.isrunningFile=./hcs2Running.tmp
settings.harvester.harvesting.oldjobsDir=oldjobs2
settings.harvester.harvesting.queuePriority=HIGHPRIORITY
settings.harvester.harvesting.serverDir=server2
simple.harvest.indicator=0
sun.arch.data.model=32
sun.boot.class.path=/usr/lib/jvm/java-1.5.0-sun-1.5.0.15/jre/lib/rt.jar:/usr/lib/jvm/java-1.5.0-sun-1.5.0.15/jre/lib/i18n.jar:/usr/lib/jvm/java-1.5.0-sun-1.5.0.15/jre/lib/sunrsasign.jar:/usr/lib/jvm/java-1.5.0-sun-1.5.0.15/jre/lib/jsse.jar:/usr/lib/jvm/java-1.5.0-sun-1.5.0.15/jre/lib/jce.jar:/usr/lib/jvm/java-1.5.0-sun-1.5.0.15/jre/lib/charsets.jar:/usr/lib/jvm/java-1.5.0-sun-1.5.0.15/jre/classes
sun.boot.library.path=/usr/lib/jvm/java-1.5.0-sun-1.5.0.15/jre/lib/i386
sun.cpu.endian=little
sun.cpu.isalist=
sun.desktop=gnome
sun.io.unicode.encoding=UnicodeLittle
sun.java.launcher=SUN_STANDARD
sun.jnu.encoding=UTF-8
sun.management.compiler=HotSpot Server Compiler
sun.os.patch.level=unknown
user.country=DK
user.dir=/home/kfc/build/netarchivesuite/scripts/simple_harvest
user.home=/home/kfc
user.language=en
user.name=kfc
user.timezone=Europe/Paris
Working directory: server2/8_1212391347947
127.0.0.1 - admin [02/Jun/2008:07:23:43 +0000] "POST /j_security_check HTTP/1.1" 302 0 
127.0.0.1 - - [02/Jun/2008:07:23:43 +0000] "GET / HTTP/1.1" 302 0 
127.0.0.1 - admin [02/Jun/2008:07:23:43 +0000] "GET /index.jsp HTTP/1.1" 200 10980 
127.0.0.1 - - [02/Jun/2008:07:23:43 +0000] "GET /js/util.js HTTP/1.1" 200 801 
127.0.0.1 - - [02/Jun/2008:07:23:43 +0000] "GET /css/heritrix.css HTTP/1.1" 200 2101 
127.0.0.1 - - [02/Jun/2008:07:23:43 +0000] "GET /images/h.ico HTTP/1.1" 200 198 
127.0.0.1 - - [02/Jun/2008:07:23:43 +0000] "GET /images/logo.gif HTTP/1.1" 200 889 
127.0.0.1 - admin [02/Jun/2008:07:23:46 +0000] "GET /console/action.jsp?action=pause HTTP/1.1" 302 4 
127.0.0.1 - admin [02/Jun/2008:07:23:46 +0000] "GET /index.jsp HTTP/1.1" 200 11164 
07:22:29.982 EVENT  Starting Jetty/4.2.23
07:22:30.184 EVENT  Started WebApplicationContext[/,Heritrix Console]
07:22:30.271 EVENT  Started SocketListener on 0.0.0.0:8092
07:22:30.271 EVENT  Started org.mortbay.jetty.Server@1aa9f99
06/02/2008 07:22:30 +0000 INFO org.archive.crawler.Heritrix postRegister org.archive.crawler:guiport=8092,host=david,jmxport=8093,name=Heritrix,type=CrawlService registered to MBeanServerId=david_1212391349563, SpecificationVersion=1.2 Maintenance Release, ImplementationVersion=1.5.0_15-b04, SpecificationVendor=Sun Microsystems
Heritrix version: 1.12.1b
06/02/2008 07:22:32 +0000 INFO org.archive.crawler.admin.CrawlJob postRegister org.archive.crawler:host=david,jmxport=8093,mother=Heritrix,name=8-2-20080602072231225,type=CrawlService.Job registered to MBeanServerId=david_1212391349563, SpecificationVersion=1.2 Maintenance Release, ImplementationVersion=1.5.0_15-b04, SpecificationVendor=Sun Microsystems
06/02/2008 07:52:45 +0000 INFO org.archive.crawler.admin.CrawlJob postDeregister org.archive.crawler:host=david,jmxport=8093,mother=Heritrix,name=8-2-20080602072231225,type=CrawlService.Job unregistered from MBeanServerId=david_1212391349563, SpecificationVersion=1.2 Maintenance Release, ImplementationVersion=1.5.0_15-b04, SpecificationVendor=Sun Microsystems
06/02/2008 07:52:48 +0000 INFO org.archive.crawler.Heritrix postDeregister org.archive.crawler:guiport=8092,host=david,jmxport=8093,name=Heritrix,type=CrawlService unregistered from MBeanServerId=david_1212391349563, SpecificationVersion=1.2 Maintenance Release, ImplementationVersion=1.5.0_15-b04, SpecificationVendor=Sun Microsystems
07:52:48.586 EVENT  Stopping Acceptor ServerSocket[addr=0.0.0.0/0.0.0.0,port=0,localport=8092]
07:52:48.587 EVENT  Stopped SocketListener on 0.0.0.0:8092
07:52:48.592 EVENT  Stopped WebApplicationContext[/,Heritrix Console]
07:52:48.592 EVENT  Stopped org.mortbay.http.NCSARequestLog@4a553f
07:52:48.593 EVENT  Stopped org.mortbay.jetty.Server@1aa9f99


filedesc://12-metadata-1.arc 0.0.0.0 20080602165933 text/plain 77
1 0 InternetArchive
URL IP-address Archive-date Content-type Archive-length

metadata://netarkivet.dk/crawl/setup/duplicatereductionjobs?majorversion=1&minorversion=0&harvestid=2&harvestnum=2&jobid=12 127.0.1.1 20080602165804 text/plain 1
8
metadata://netarkivet.dk/crawl/setup/order.xml?heritrixVersion=1.12.1b&harvestid=2&jobid=12 127.0.1.1 20080602165805 text/xml 21725
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
    <string name="disk-path">/home/kfc/build/netarchivesuite/scripts/simple_harvest/server2/12_1212425884349</string>
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
      <string name="seedsfile">/home/kfc/build/netarchivesuite/scripts/simple_harvest/server2/12_1212425884349/seeds.txt</string>
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
        <string name="index-location">/home/kfc/build/netarchivesuite/scripts/simple_harvest/cache/DEDUP_CRAWL_LOG/8-cache</string>
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
        <string name="prefix">12-2</string>
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

metadata://netarkivet.dk/crawl/setup/harvestInfo.xml?heritrixVersion=1.12.1b&harvestid=2&jobid=12 127.0.1.1 20080602165804 text/xml 380
<?xml version="1.0" encoding="UTF-8"?>

<harvestInfo>
  <version>0.2</version>
  <jobId>12</jobId>
  <priority>HIGHPRIORITY</priority>
  <harvestNum>2</harvestNum>
  <origHarvestDefinitionID>2</origHarvestDefinitionID>
  <maxBytesPerDomain>10000000</maxBytesPerDomain>
  <maxObjectsPerDomain>-1</maxObjectsPerDomain>
  <orderXMLName>default_orderxml</orderXMLName>
</harvestInfo>

metadata://netarkivet.dk/crawl/setup/seeds.txt?heritrixVersion=1.12.1b&harvestid=2&jobid=12 127.0.1.1 20080602165804 text/plain 46
http://www.netarkivet.dk
http://www.kaarefc.dk
metadata://netarkivet.dk/crawl/reports/crawl-report.txt?heritrixVersion=1.12.1b&harvestid=2&jobid=12 127.0.1.1 20080602165920 text/plain 313
Crawl Name: default_orderxml
Crawl Status: Finished
Duration Time: 1m12s602ms
Total Seeds Crawled: 2
Total Seeds not Crawled: 0
Total Hosts Crawled: 7
Total Documents Crawled: 251
Processed docs/sec: 3.47
Bandwidth in Kbytes/sec: 174
Total Raw Data Size in Bytes: 12841240 (12 MB) 
Novel Bytes: 12841240 (12 MB) 

metadata://netarkivet.dk/crawl/reports/frontier-report.txt?heritrixVersion=1.12.1b&harvestid=2&jobid=12 127.0.1.1 20080602165920 text/plain 15
frontier empty

metadata://netarkivet.dk/crawl/reports/hosts-report.txt?heritrixVersion=1.12.1b&harvestid=2&jobid=12 127.0.1.1 20080602165920 text/plain 207
[#urls] [#bytes] [host]
120 10546964 netarkivet.dk
40 1808738 www.kaarefc.dk
38 327191 netarchive.dk
37 146784 www.douglasadams.com
7 593 dns:
3 4478 jigsaw.w3.org
3 5470 www.w3.org
2 1022 www.netarkivet.dk

metadata://netarkivet.dk/crawl/reports/mimetype-report.txt?heritrixVersion=1.12.1b&harvestid=2&jobid=12 127.0.1.1 20080602165920 text/plain 342
[#urls] [#bytes] [mime-types]
84 81537 image/gif
84 781765 text/html
22 9258973 application/pdf
20 1217007 audio/x-wav
12 26850 image/png
8 10611 text/plain
7 21585 text/css
7 593 text/dns
2 157712 application/x-gzip
1 723731 application/vnd.ms-powerpoint
1 550399 application/x-java-archive
1 4159 application/x-javascript
1 6318 image/jpeg

metadata://netarkivet.dk/crawl/reports/processors-report.txt?heritrixVersion=1.12.1b&harvestid=2&jobid=12 127.0.1.1 20080602165920 text/plain 2081
Processors report - 200806021659
  Job being crawled:    default_orderxml
  Number of Processors: 16
  NOTE: Some processors may not return a report!

Processor: org.archive.crawler.fetcher.FetchHTTP
  Function:          Fetch HTTP URIs
  CrawlURIs handled: 243
  Recovery retries:   0

Processor: org.archive.crawler.extractor.ExtractorHTTP
  Function:          Extracts URIs from HTTP response headers
  CrawlURIs handled: 243
  Links extracted:   11

Processor: org.archive.crawler.extractor.ExtractorHTML
  Function:          Link extraction on HTML documents
  CrawlURIs handled: 78
  Links extracted:   4694

Processor: org.archive.crawler.extractor.ExtractorCSS
  Function:          Link extraction on Cascading Style Sheets (.css)
  CrawlURIs handled: 7
  Links extracted:   19

Processor: org.archive.crawler.extractor.ExtractorJS
  Function:          Link extraction on JavaScript code
  CrawlURIs handled: 1
  Links extracted:   0

Processor: org.archive.crawler.extractor.ExtractorSWF
  Function:          Link extraction on Shockwave Flash documents (.swf)
  CrawlURIs handled: 0
  Links extracted:   0

Processor: is.hi.bok.digest.DeDuplicator
  Function:          Abort processing of duplicate records
                     - Lookup by url in use
  Total handled:     144
  Duplicates found:  0 0.0%
  Bytes total:       12026686 (11 MB)
  Bytes discarded:   0 (0 B) 0.0%
  New (no hits):     144
  Exact hits:        0
  Equivalent hits:   0
  Timestamp predicts: (Where exact URL existed in the index)
  Change correctly:  0
  Change falsly:     0
  Non-change correct:0
  Non-change falsly: 0
  Missing timpestamp:0
  [Host] [total] [duplicates] [bytes] [bytes discarded] [new] [exact] [equiv] [change correct] [change falsly] [non-change correct] [non-change falsly] [no timestamp]
  www.douglasadams.com 27 0 46454 0 27 0 0 0 0 0 0 0
  www.w3.org 2 0 3914 0 2 0 0 0 0 0 0 0
  jigsaw.w3.org 2 0 3802 0 2 0 0 0 0 0 0 0
  netarchive.dk 12 0 28517 0 12 0 0 0 0 0 0 0
  netarkivet.dk 80 0 10176593 0 80 0 0 0 0 0 0 0
  www.kaarefc.dk 21 0 1767406 0 21 0 0 0 0 0 0 0


metadata://netarkivet.dk/crawl/reports/responsecode-report.txt?heritrixVersion=1.12.1b&harvestid=2&jobid=12 127.0.1.1 20080602165920 text/plain 55
[rescode] [#urls]
200 211
404 22
1 7
302 5
301 4
401 1

metadata://netarkivet.dk/crawl/reports/seeds-report.txt?heritrixVersion=1.12.1b&harvestid=2&jobid=12 127.0.1.1 20080602165920 text/plain 129
[code] [status] [seed] [redirect]
302 CRAWLED http://www.netarkivet.dk/ http://netarkivet.dk/
200 CRAWLED http://www.kaarefc.dk/

metadata://netarkivet.dk/crawl/logs/crawl.log?heritrixVersion=1.12.1b&harvestid=2&jobid=12 127.0.1.1 20080602165920 text/plain 58947
2008-06-02T16:58:08.735Z     1         54 dns:www.kaarefc.dk P http://www.kaarefc.dk/ text/dns #002 20080602165807953+296 sha1:Q3N47QKVZUTSE3CZFPNJBNDMEVBDXDIL - content-size:54
2008-06-02T16:58:08.742Z     1         59 dns:www.netarkivet.dk P http://www.netarkivet.dk/ text/dns #001 20080602165807939+153 sha1:WAMUGVLUXLWFRHIYNA6FIRZ6BHUQ7ITU - content-size:59
2008-06-02T16:58:09.111Z   404        287 http://www.kaarefc.dk/robots.txt P http://www.kaarefc.dk/ text/html #005 20080602165809055+51 sha1:UYPHNRGYSU4RITKMYXDPSQ4O7CM7RL6S - content-size:466
2008-06-02T16:58:09.163Z   302        301 http://www.netarkivet.dk/robots.txt P http://www.netarkivet.dk/ text/html #001 20080602165809055+93 sha1:ESVJLA2FRUB6UDKGUQWRMONKMVJQR325 - content-size:521
2008-06-02T16:58:09.454Z   200       1954 http://www.kaarefc.dk/ - - text/html #004 20080602165809415+12 sha1:Z365FQONKTNU76BP7T4C746UU3O77G2X - content-size:2209,3t
2008-06-02T16:58:09.492Z     1         93 dns:jigsaw.w3.org EP http://jigsaw.w3.org/css-validator/images/vcss text/dns #007 20080602165809462+26 sha1:2WAANFFVCPKGRKVCEVXCAKKVDTAPDRUZ - content-size:93
2008-06-02T16:58:09.536Z   302        291 http://www.netarkivet.dk/ - - text/html #010 20080602165809466+65 sha1:FJZLU5E256LIJ5NFHPXYMTWVX7E5CS3L - content-size:501,3t
2008-06-02T16:58:09.773Z   200        973 http://www.kaarefc.dk/style.css E http://www.kaarefc.dk/ text/css #008 20080602165809758+12 sha1:QQGPCUPJBPZE7VHGVIKCOU5T57BXMNA3 - content-size:1226
2008-06-02T16:58:10.050Z   200        403 http://jigsaw.w3.org/robots.txt EP http://jigsaw.w3.org/css-validator/images/vcss text/plain #011 20080602165809798+250 sha1:NVV3PMRCRI7NFOKUQ6GTKJ6ECUDL73PC - content-size:676
2008-06-02T16:58:10.089Z   401        480 http://www.kaarefc.dk/private/ L http://www.kaarefc.dk/ text/html #005 20080602165810074+10 sha1:LVUNLGRFLR6DL2ULFU3XOBRN2HZB7DB2 - content-size:712
2008-06-02T16:58:10.407Z   301        318 http://www.kaarefc.dk/mindterm L http://www.kaarefc.dk/ text/html #006 20080602165810392+10 sha1:RAYXTVIQUX6NHOKOHHS2EMASMDFLLZYZ - content-size:548
2008-06-02T16:58:10.620Z   200       1547 http://jigsaw.w3.org/css-validator/images/vcss E http://www.kaarefc.dk/ image/gif #004 20080602165810351+251 sha1:GGFHILTXC4O4QGIOOJZZDNZ3N5Z5HKU6 - content-size:1943,3t
2008-06-02T16:58:10.742Z   200       1255 http://www.kaarefc.dk/mindterm/ LR http://www.kaarefc.dk/mindterm text/html #008 20080602165810709+11 sha1:B7XARX4VIXUITARYQG2TFQG7LNVE6HHV - content-size:1510
2008-06-02T16:58:10.919Z     1         56 dns:netarkivet.dk RP http://netarkivet.dk/ text/dns #015 20080602165809846+1070 sha1:WSKKCZ2A25JSBJMADWN2CPCTURIUQF6D - content-size:56
2008-06-02T16:58:10.962Z     1        214 dns:www.w3.org EP http://www.w3.org/Icons/valid-html401 text/dns #011 20080602165810928+31 sha1:DIE66K6W7BNNIKUKUP7ERQKFVHQRAHVK - content-size:214
2008-06-02T16:58:11.058Z   301        313 http://www.kaarefc.dk/wop L http://www.kaarefc.dk/ text/html #005 20080602165811044+10 sha1:MEFRJNP3ZEMIWC374PKTMWAQTWQBETQL - content-size:538
2008-06-02T16:58:11.373Z   200        267 http://www.kaarefc.dk/public/ L http://www.kaarefc.dk/ text/html #010 20080602165811360+10 sha1:5AB2EEY3DGCIRQJ4A2AVKJ33TKTO5LZZ - content-size:521
2008-06-02T16:58:11.516Z   200       1194 http://www.w3.org/robots.txt EP http://www.w3.org/Icons/valid-html401 text/plain #006 20080602165811268+247 sha1:72W7ABDATBBIKOE7GVD5VHLHUZORXWT5 - content-size:1556
2008-06-02T16:58:11.686Z   404        296 http://www.kaarefc.dk/mindterm/kaarefc.dk LRL http://www.kaarefc.dk/mindterm/ text/html #008 20080602165811676+9 sha1:DSAADVFELGW6IBXPD7VR6J3B4DENTOS3 - content-size:475
2008-06-02T16:58:11.989Z   404        290 http://netarkivet.dk/robots.txt RP http://netarkivet.dk/ text/html #011 20080602165811925+62 sha1:SJXA45HOAGK6LVRDJWKTXOQXKU2MJQAC - content-size:471
2008-06-02T16:58:11.999Z   404        327 http://www.kaarefc.dk/mindterm/com.mindbright.application.MindTerm.class LRE http://www.kaarefc.dk/mindterm/ text/html #002 20080602165811989+9 sha1:TQ6YEUXEICG3OLADF7MHK73GHL4BLXT7 - content-size:506
2008-06-02T16:58:12.070Z   200       1542 http://www.w3.org/Icons/valid-html401 E http://www.kaarefc.dk/ image/png #016 20080602165811819+249 sha1:GFOGAERAPGQKFL66QFHDOJVLF3AN7ZUA - content-size:2001,3t
2008-06-02T16:58:12.383Z   302          0 http://netarkivet.dk/ R http://www.netarkivet.dk/ text/html #010 20080602165812292+89 sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ - content-size:241,3t
2008-06-02T16:58:12.584Z   200     550123 http://www.kaarefc.dk/mindterm/mindterm.jar LRE http://www.kaarefc.dk/mindterm/ application/x-java-archive #007 20080602165812301+278 sha1:HRJ2FX23CMF55GAOF5LNKCCZLEJEPQMD - content-size:550399
2008-06-02T16:58:12.717Z   200       1547 http://jigsaw.w3.org/css-validator/images/vcss.gif ER http://jigsaw.w3.org/css-validator/images/vcss image/gif #006 20080602165812372+344 sha1:GGFHILTXC4O4QGIOOJZZDNZ3N5Z5HKU6 - content-size:1859
2008-06-02T16:58:12.931Z   200       5373 http://www.kaarefc.dk/wop/ LL http://www.kaarefc.dk/wop text/html #019 20080602165812886+11 sha1:JOMSWKPDBCAWYBLJDSNLQEE6DCTMOPUO - content-size:5629
2008-06-02T16:58:12.986Z   200      12236 http://netarkivet.dk/index-da.php RR http://netarkivet.dk/ text/html #008 20080602165812685+132 sha1:5F6YRXNZJ2FOW7EKQ3YMDMXKBMFUAC2K - content-size:12431
2008-06-02T16:58:13.195Z     1         61 dns:www.douglasadams.com LLEP http://www.douglasadams.com/creations/0345391829.html text/dns #002 20080602165812933+259 sha1:U5DO5K27MSAALL5JW5WCN4JDLBWO35BH - content-size:61
2008-06-02T16:58:13.345Z   200       1542 http://www.w3.org/Icons/valid-html401.png ER http://www.w3.org/Icons/valid-html401 image/png #016 20080602165813062+282 sha1:GFOGAERAPGQKFL66QFHDOJVLF3AN7ZUA - content-size:1913
2008-06-02T16:58:13.409Z   404        290 http://netarkivet.dk/robots.txt PL http://www.netarkivet.dk/robots.txt text/html #010 20080602165813288+120 sha1:SJXA45HOAGK6LVRDJWKTXOQXKU2MJQAC - content-size:471
2008-06-02T16:58:13.643Z   200         57 http://www.douglasadams.com/robots.txt LLEP http://www.douglasadams.com/creations/0345391829.html text/plain #007 20080602165813500+142 sha1:2YI7X55YGQQCC3CRI7OWONHEDKSYKLWJ - content-size:319
2008-06-02T16:58:13.673Z   200       1557 http://www.kaarefc.dk/wop/wop.css LLE http://www.kaarefc.dk/wop/ text/css #018 20080602165813660+12 sha1:6W3MBG43EDZ7VF47CDMMQFV7Q2I453U3 - content-size:1811
2008-06-02T16:58:13.832Z   200        251 http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_20.gif RRE http://netarkivet.dk/index-da.php image/gif #028 20080602165813712+118 sha1:STTVR3FJILXODO63X5B57XKS3VLCTD54 - content-size:500
2008-06-02T16:58:13.997Z   200       2507 http://www.kaarefc.dk/wop/wop.html LLL http://www.kaarefc.dk/wop/ text/html #005 20080602165813974+13 sha1:BB6II6NDC7MJGMX3UZGDMHL4BWYTFHA5 - content-size:2762
2008-06-02T16:58:14.257Z   200       9634 http://www.douglasadams.com/creations/0345391829.html LLE http://www.kaarefc.dk/wop/ text/html #008 20080602165813945+221 sha1:I5TITMUBKLFTEECARB3YDT5NGZHAXZ7K - content-size:9899,3t
2008-06-02T16:58:14.321Z   200       3985 http://www.kaarefc.dk/wop/submit.html LLL http://www.kaarefc.dk/wop/ text/html #016 20080602165814299+12 sha1:SSAXZEXAA37GRZ57EJ7O7TBN3ZPFLXDS - content-size:4240
2008-06-02T16:58:14.579Z   200      96788 http://netarkivet.dk/publikationer/vejledning.pdf RRL http://netarkivet.dk/index-da.php application/pdf #001 20080602165814133+444 sha1:NFPOH2IMN5O2V3ZKVTC4AARLFCTHLGD2 - content-size:97048
2008-06-02T16:58:14.665Z   200       1397 http://www.douglasadams.com/images/arch_layout_09_02over.gif LLEX http://www.douglasadams.com/creations/0345391829.html image/gif #004 20080602165814559+101 sha1:E3IBMGJ7NUINFDHXRN2YU55JLK5WTWNG - content-size:1646
2008-06-02T16:58:14.666Z   200       4597 http://www.kaarefc.dk/wop/thanks.html LLL http://www.kaarefc.dk/wop/ text/html #007 20080602165814623+10 sha1:EHG5YH347NSULROGZMCJQ3ACY52M3HBI - content-size:4853
2008-06-02T16:58:14.990Z   200       4434 http://www.kaarefc.dk/wop/help.html LLL http://www.kaarefc.dk/wop/ text/html #002 20080602165814968+14 sha1:KJ2VULDQLUXILAJRNUPESAX55Z2XBFII - content-size:4690
2008-06-02T16:58:15.137Z   404      10991 http://www.douglasadams.com/creations/document.Narchlayout_03_02 LLEX http://www.douglasadams.com/creations/0345391829.html text/html #005 20080602165814968+153 sha1:HSIXPXF55APZCV35C7AVYFU344PDFHYS - content-size:11264
2008-06-02T16:58:15.233Z   200      23438 http://netarkivet.dk/faq/index-da.php RRL http://netarkivet.dk/index-da.php text/html #008 20080602165815025+140 sha1:U6VVONEP2ROTEA6MTYSRG55O3BI6EGOL - content-size:23633
2008-06-02T16:58:15.313Z   200       5373 http://www.kaarefc.dk/wop/index.html LLL http://www.kaarefc.dk/wop/ text/html #016 20080602165815292+10 sha1:JOMSWKPDBCAWYBLJDSNLQEE6DCTMOPUO - content-size:5629
2008-06-02T16:58:15.540Z   200        944 http://www.douglasadams.com/images/corner.gif LLEE http://www.douglasadams.com/creations/0345391829.html image/gif #001 20080602165815439+100 sha1:DOEM4OPZXQU2YOC67R33DFDJDLMJRU2U - content-size:1192
2008-06-02T16:58:15.599Z   200        478 http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_03.gif RRE http://netarkivet.dk/index-da.php image/gif #006 20080602165815534+64 sha1:GP6GQ3RSSV7LESACJ5GDRS5SOZSIILS5 - content-size:728
2008-06-02T16:58:15.631Z   200       2268 http://www.kaarefc.dk/wop/contact.html LLL http://www.kaarefc.dk/wop/ text/html #004 20080602165815614+10 sha1:3OL3ZAL7TFD7TVR6WT3OZ7TSLDLFJLMK - content-size:2523
2008-06-02T16:58:15.942Z   200         43 http://www.douglasadams.com/images/arch_layout_00.gif LLEE http://www.douglasadams.com/creations/0345391829.html image/gif #030 20080602165815842+99 sha1:3ACLJFOLTOCLSAD2EW25QX424Z2AATG6 - content-size:289
2008-06-02T16:58:16.030Z   200     179464 http://www.kaarefc.dk/wop/wop.wav LLLL http://www.kaarefc.dk/wop/wop.html audio/x-wav #019 20080602165815933+95 sha1:KE566UQYHCLQPAOGO5VRDC6X3HUDTOVW - content-size:179725
2008-06-02T16:58:16.343Z   200         44 http://www.kaarefc.dk/wop/wavs/23.wav LLLL http://www.kaarefc.dk/wop/thanks.html audio/x-wav #010 20080602165816331+12 sha1:VOQCPDAHKMXMK4UIR53K67U6XVQHCNTQ - content-size:298
2008-06-02T16:58:16.475Z   200       1396 http://www.douglasadams.com/images/arch_layout_06_02.gif LLEE http://www.douglasadams.com/creations/0345391829.html image/gif #016 20080602165816244+230 sha1:DVC3ATGQORFCIKHIEGP42HROSF4EUCZ7 - content-size:1645
2008-06-02T16:58:16.675Z   200      27312 http://www.kaarefc.dk/wop/wavs/7.wav LLLL http://www.kaarefc.dk/wop/thanks.html audio/x-wav #007 20080602165816646+28 sha1:7DKOBTGY2W7YFGRTM5ENSJ3LBIAUXO3V - content-size:27597
2008-06-02T16:58:17.005Z   200      24684 http://www.kaarefc.dk/wop/wavs/9.wav LLLL http://www.kaarefc.dk/wop/thanks.html audio/x-wav #002 20080602165816977+27 sha1:RMZE7JLHMGUFEOITPQHC3WESGRMZ52XO - content-size:24969
2008-06-02T16:58:17.151Z   404      10991 http://www.douglasadams.com/creations/document.Narchlayout_08_02 LLEX http://www.douglasadams.com/creations/0345391829.html text/html #028 20080602165816776+361 sha1:HSIXPXF55APZCV35C7AVYFU344PDFHYS - content-size:11264
2008-06-02T16:58:17.284Z   200    1030376 http://netarkivet.dk/publikationer/DFrevy.pdf RRL http://netarkivet.dk/index-da.php application/pdf #035 20080602165815901+1377 sha1:VBUHWDGKLIDBFQCVZV77MDJDNQTRSRWL - content-size:1030638
2008-06-02T16:58:17.338Z   200      34212 http://www.kaarefc.dk/wop/wavs/22.wav LLLL http://www.kaarefc.dk/wop/thanks.html audio/x-wav #010 20080602165817308+29 sha1:4U742GU25XJ2WIXUHEA7AU27GELFX2LG - content-size:34497
2008-06-02T16:58:17.615Z   200       1100 http://www.douglasadams.com/images/title2.gif LLEE http://www.douglasadams.com/creations/0345391829.html image/gif #001 20080602165817513+101 sha1:GXNQ4E7NHP556JC7ADL2LLBAB7EH37EL - content-size:1349
2008-06-02T16:58:17.670Z   200      32360 http://www.kaarefc.dk/wop/wavs/3.wav LLLL http://www.kaarefc.dk/wop/thanks.html audio/x-wav #018 20080602165817639+29 sha1:CONEC52ZWAQWM2EMDR42HAQEFMSHBHKB - content-size:32645
2008-06-02T16:58:17.987Z   200      12550 http://www.kaarefc.dk/wop/wavs/5.wav LLLL http://www.kaarefc.dk/wop/thanks.html audio/x-wav #002 20080602165817971+14 sha1:2CB6MK27RTBO27VIU3KHN6WQKSZWJSWQ - content-size:12835
2008-06-02T16:58:18.023Z   200       1702 http://www.douglasadams.com/images/arch_layout_02_02over.gif LLEX http://www.douglasadams.com/creations/0345391829.html image/gif #015 20080602165817917+105 sha1:UKZNRYLXVLVVB5O2S5R3LPV4RMXF3YBW - content-size:1951
2008-06-02T16:58:18.363Z   200     116164 http://www.kaarefc.dk/wop/wavs/6.wav LLLL http://www.kaarefc.dk/wop/thanks.html audio/x-wav #010 20080602165818288+73 sha1:6VA6KPV7HDKSAIVIO45CYQBQXJNKYYCW - content-size:116425
2008-06-02T16:58:18.372Z   200        478 http://netarkivet.dk/netarkivet_mouseover/billeder/netarkivet_guidelines_03.gif RRX http://netarkivet.dk/index-da.php image/gif #035 20080602165818285+86 sha1:7E2MALHL3YN34SKRDUYQJILUGMHEXJT5 - content-size:728
2008-06-02T16:58:18.500Z   200       6067 http://www.douglasadams.com/creations/0330267388.jpg LLEE http://www.douglasadams.com/creations/0345391829.html image/jpeg #016 20080602165818323+176 sha1:ZPIBYNHCL33S4RHJJ6WJLZVBPARZJENV - content-size:6318
2008-06-02T16:58:18.701Z   200      45128 http://www.kaarefc.dk/wop/wavs/2.wav LLLL http://www.kaarefc.dk/wop/thanks.html audio/x-wav #018 20080602165818666+34 sha1:GQKETE4FZGDVYXIMGQ4JB3RGHBJNHF7E - content-size:45413
2008-06-02T16:58:18.903Z   200       1338 http://www.douglasadams.com/images/amazon.gif LLEE http://www.douglasadams.com/creations/0345391829.html image/gif #036 20080602165818802+100 sha1:PIWMCRPR56D5L623X7D5ZHMDHBTO57QL - content-size:1587
2008-06-02T16:58:19.037Z   200      45142 http://www.kaarefc.dk/wop/wavs/10.wav LLLL http://www.kaarefc.dk/wop/thanks.html audio/x-wav #015 20080602165819002+34 sha1:WICE7MDD7NBP27FVCDNLIUHYP2HMPWXQ - content-size:45427
2008-06-02T16:58:19.305Z   200        357 http://www.douglasadams.com/images/arch_09_03.gif LLEE http://www.douglasadams.com/creations/0345391829.html image/gif #008 20080602165819205+99 sha1:N74MIUPZAGBW2WEAYRX3PY6MIAOR5ICW - content-size:605
2008-06-02T16:58:19.360Z   200      21904 http://www.kaarefc.dk/wop/wavs/1.wav LLLL http://www.kaarefc.dk/wop/thanks.html audio/x-wav #035 20080602165819339+20 sha1:UUDR6EP4RSHEQQ6LPO5SLZXTPAW4CNRP - content-size:22189
2008-06-02T16:58:19.362Z   200         43 http://netarkivet.dk/netarkivet_alm/billeder/spacer.gif RRE http://netarkivet.dk/index-da.php image/gif #010 20080602165819297+64 sha1:5J67LA4YGEZ3MJYSWXTTX7542ROMKNZW - content-size:291
2008-06-02T16:58:19.714Z   200       1724 http://www.douglasadams.com/images/red_02_02.gif LLEE http://www.douglasadams.com/creations/0345391829.html image/gif #001 20080602165819606+107 sha1:SYEANGUNVMJ3ZOGECQ4Q4SIUZ5LJTPYO - content-size:1973
2008-06-02T16:58:19.752Z   200     148326 http://www.kaarefc.dk/wop/wavs/13.wav LLLL http://www.kaarefc.dk/wop/thanks.html audio/x-wav #007 20080602165819662+89 sha1:QDYSNBZGBBC2DOJL6WOY77NULB6KIQG5 - content-size:148587
2008-06-02T16:58:19.759Z   200        455 http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_06.gif RRE http://netarkivet.dk/index-da.php image/gif #018 20080602165819663+96 sha1:5AXVRC2AOFEGKPA732J67GUEVJGL3FDA - content-size:705
2008-06-02T16:58:20.088Z   200      15972 http://www.kaarefc.dk/wop/wavs/0.wav LLLL http://www.kaarefc.dk/wop/thanks.html audio/x-wav #005 20080602165820053+34 sha1:BHECRSWK5XLK4NGKW34JGYVHCITRHOTP - content-size:16257
2008-06-02T16:58:20.132Z   200        980 http://www.douglasadams.com/images/arch_layout_08_02over.gif LLEX http://www.douglasadams.com/creations/0345391829.html image/gif #019 20080602165820017+114 sha1:IUQABJCXF6HHR6YADAEAGRFPC4GFARUJ - content-size:1228
2008-06-02T16:58:20.198Z   200       8800 http://netarkivet.dk/presse/index-da.php RRL http://netarkivet.dk/index-da.php text/html #028 20080602165820061+98 sha1:P3FGBE22RGRUVJRV6KVTKJ6BFBIJI7TU - content-size:8995
2008-06-02T16:58:20.430Z   200      60952 http://www.kaarefc.dk/wop/wavs/19.wav LLLL http://www.kaarefc.dk/wop/thanks.html audio/x-wav #016 20080602165820391+38 sha1:7XNEDPK3GZEXUPR2UHG4QES2N3BCBY6L - content-size:61237
2008-06-02T16:58:20.542Z   200       1363 http://www.douglasadams.com/images/arch_layout_07_02.gif LLEE http://www.douglasadams.com/creations/0345391829.html image/gif #006 20080602165820434+107 sha1:HYMZ4LQV7QUFXXHEQDWM65ONMXKYFU62 - content-size:1612
2008-06-02T16:58:20.571Z   200        209 http://netarkivet.dk/netarkivet_mouseover/billeder/netarkivet_guidelines_09.gif RRX http://netarkivet.dk/index-da.php image/gif #004 20080602165820500+69 sha1:4DISLWHOARCF3JB3VFWSDQA2OWVHAS7E - content-size:458
2008-06-02T16:58:20.754Z   200      20196 http://www.kaarefc.dk/wop/wavs/15.wav LLLL http://www.kaarefc.dk/wop/thanks.html audio/x-wav #007 20080602165820732+21 sha1:SBB5AQM3YYLYZPMOMEUA7SNXJWNIZMJ2 - content-size:20481
2008-06-02T16:58:20.936Z   200         56 http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_16.gif RRE http://netarkivet.dk/index-da.php image/gif #009 20080602165820872+63 sha1:FOBREXIPW6KAKDLQ65S7QCNWIODJKEDX - content-size:304
2008-06-02T16:58:21.009Z   404      10991 http://www.douglasadams.com/creations/document.Narchlayout_05_02 LLEX http://www.douglasadams.com/creations/0345391829.html text/html #036 20080602165820844+153 sha1:HSIXPXF55APZCV35C7AVYFU344PDFHYS - content-size:11264
2008-06-02T16:58:21.132Z   200     131854 http://www.kaarefc.dk/wop/wavs/12.wav LLLL http://www.kaarefc.dk/wop/thanks.html audio/x-wav #005 20080602165821056+74 sha1:QQWDUAHINWCKFULTXBLUEC2VKNJSCBM4 - content-size:132115
2008-06-02T16:58:21.306Z   200        163 http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_17.gif RRE http://netarkivet.dk/index-da.php image/gif #008 20080602165821238+67 sha1:Q2Z2YBECUJOB4JFCDBGT5SEWKMP4XIOK - content-size:412
2008-06-02T16:58:21.443Z   404        305 http://www.kaarefc.dk/wop/www.spiritual-supply.com LLLL http://www.kaarefc.dk/wop/thanks.html text/html #006 20080602165821433+9 sha1:MJRWU5CENWAR4Q7FL2QHXFGYFDM5NP3V - content-size:484
2008-06-02T16:58:21.482Z   404      10991 http://www.douglasadams.com/creations/document.Narchlayout_02_02 LLEX http://www.douglasadams.com/creations/0345391829.html text/html #035 20080602165821311+159 sha1:HSIXPXF55APZCV35C7AVYFU344PDFHYS - content-size:11264
2008-06-02T16:58:21.671Z   200        301 http://netarkivet.dk/netarkivet_mouseover/billeder/netarkivet_guidelines_04.gif RRX http://netarkivet.dk/index-da.php image/gif #001 20080602165821607+63 sha1:36NXWN45ZWC656ROIH6KJZBMDNQ4DDVM - content-size:551
2008-06-02T16:58:21.789Z   200      62770 http://www.kaarefc.dk/wop/wavs/17.wav LLLL http://www.kaarefc.dk/wop/thanks.html audio/x-wav #007 20080602165821745+43 sha1:B6FTZH3GF7EPPQG6XROHGLVXFZ3OWK3V - content-size:63055
2008-06-02T16:58:21.891Z   200        868 http://www.douglasadams.com/images/arch_12_08.gif LLEE http://www.douglasadams.com/creations/0345391829.html image/gif #012 20080602165821784+106 sha1:GNQXYNK2MAWXZOGD74DJ2Q55LTHMXMFT - content-size:1116
2008-06-02T16:58:22.036Z   200        292 http://netarkivet.dk/netarkivet_mouseover/billeder/netarkivet_guidelines_11.gif RRX http://netarkivet.dk/index-da.php image/gif #002 20080602165821973+62 sha1:OULHGSPHG3MNKIEG5RBH2UFI7GPRMYFO - content-size:542
2008-06-02T16:58:22.106Z   200      12248 http://www.kaarefc.dk/wop/wavs/11.wav LLLL http://www.kaarefc.dk/wop/thanks.html audio/x-wav #005 20080602165822091+14 sha1:BU6V2ENF22K5VQWFUZ3OFBKXXKMWO4CT - content-size:12533
2008-06-02T16:58:22.291Z   200        128 http://www.douglasadams.com/images/arrow.gif LLEE http://www.douglasadams.com/creations/0345391829.html image/gif #028 20080602165822193+97 sha1:E4CITMYOI5MYPNHZDRWHFUCNHJDCPTDE - content-size:375
2008-06-02T16:58:22.441Z   200      40712 http://www.kaarefc.dk/wop/wavs/16.wav LLLL http://www.kaarefc.dk/wop/thanks.html audio/x-wav #016 20080602165822408+32 sha1:5N2AM2HBHWKTZ4BZTEZ6TMP2KMSZQBLA - content-size:40997
2008-06-02T16:58:22.514Z   200      13763 http://netarkivet.dk/kildetekster/index-da.php RRL http://netarkivet.dk/index-da.php text/html #010 20080602165822338+134 sha1:WEOEPS32H6WSZIJCLEZHFYBRG34L4VNY - content-size:13958
2008-06-02T16:58:22.694Z   200        984 http://www.douglasadams.com/images/arch_layout_03_02over.gif LLEX http://www.douglasadams.com/creations/0345391829.html image/gif #001 20080602165822593+100 sha1:QCNY66OQWOEYQCS2MFB4HKVBOHVQO4WK - content-size:1232
2008-06-02T16:58:22.836Z   200     179464 http://www.kaarefc.dk/wop/wavs/14.wav LLLL http://www.kaarefc.dk/wop/thanks.html audio/x-wav #018 20080602165822743+92 sha1:YCZSZSOBGPEH3AFAFOGJQZDOTHKA7CLC - content-size:179725
2008-06-02T16:58:22.901Z   200        455 http://netarkivet.dk/netarkivet_mouseover/billeder/netarkivet_guidelines_06.gif RRX http://netarkivet.dk/index-da.php image/gif #012 20080602165822816+84 sha1:4MZXUQYBPMATHIXH5IODC2IPL62I2IET - content-size:705
2008-06-02T16:58:23.097Z   200       1091 http://www.douglasadams.com/images/arch_layout_04_02over.gif LLEX http://www.douglasadams.com/creations/0345391829.html image/gif #036 20080602165822995+101 sha1:2KOR6KSC7HZ5CCR6AJMTSMM7CODVBNR7 - content-size:1340
2008-06-02T16:58:23.268Z   200       1327 http://netarkivet.dk/netarkivet.css RRE http://netarkivet.dk/index-da.php text/css #008 20080602165823202+65 sha1:5DHNTUXDJU6DXNRPIJHIGF2AAJGA2RIV - content-size:1577
2008-06-02T16:58:23.603Z   404      10991 http://www.douglasadams.com/creations/document.Narchlayout_06_02 LLEX http://www.douglasadams.com/creations/0345391829.html text/html #016 20080602165823398+194 sha1:HSIXPXF55APZCV35C7AVYFU344PDFHYS - content-size:11264
2008-06-02T16:58:23.634Z   200        292 http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_11.gif RRE http://netarkivet.dk/index-da.php image/gif #004 20080602165823570+63 sha1:JTOWYEBGPD5F32ZEEC6PSGG2S7IK6TF5 - content-size:542
2008-06-02T16:58:24.001Z   200       2287 http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_01.gif RRE http://netarkivet.dk/index-da.php image/gif #009 20080602165823935+65 sha1:4VTDVM3CT5OLO6Y7LTPMIMBTESCLPNNN - content-size:2538
2008-06-02T16:58:24.007Z   200       1100 http://www.douglasadams.com/images/arch_layout_04_02.gif LLEE http://www.douglasadams.com/creations/0345391829.html image/gif #020 20080602165823905+101 sha1:GS5EMIGLAZ6W5RZO6VN7MZUPKF4TAIPW - content-size:1349
2008-06-02T16:58:24.368Z   200        249 http://netarkivet.dk/netarkivet_mouseover/billeder/netarkivet_guidelines_05.gif RRX http://netarkivet.dk/index-da.php image/gif #006 20080602165824303+64 sha1:M3NXXI5SZSCK5YSAECJ6DOQQU7TGNMTK - content-size:498
2008-06-02T16:58:24.410Z   200       1402 http://www.douglasadams.com/images/arch_layout_09_02.gif LLEE http://www.douglasadams.com/creations/0345391829.html image/gif #035 20080602165824309+100 sha1:HRTZTDUNCY4JV6MRYS7P35HYCLCAWRJJ - content-size:1651
2008-06-02T16:58:24.735Z   200        209 http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_09.gif RRE http://netarkivet.dk/index-da.php image/gif #001 20080602165824671+63 sha1:LWI43KF4TLYXE5OB4B46CAJGPYKJMTKR - content-size:458
2008-06-02T16:58:24.812Z   200        151 http://www.douglasadams.com/images/arch_13_09.gif LLEE http://www.douglasadams.com/creations/0345391829.html image/gif #007 20080602165824711+100 sha1:KXDWJLGVWW7TT2GN3JUM2ZBYI7JX4PRZ - content-size:398
2008-06-02T16:58:25.102Z   200        279 http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_08.gif RRE http://netarkivet.dk/index-da.php image/gif #002 20080602165825036+65 sha1:VDPOBIKM6JNE6BZW3V3V7NXGF6FWBXIT - content-size:529
2008-06-02T16:58:25.277Z   404      10991 http://www.douglasadams.com/creations/document.Narchlayout_04_02 LLEX http://www.douglasadams.com/creations/0345391829.html text/html #019 20080602165825113+153 sha1:HSIXPXF55APZCV35C7AVYFU344PDFHYS - content-size:11264
2008-06-02T16:58:25.533Z   200       8727 http://netarkivet.dk/organisation/index-da.php RRL http://netarkivet.dk/index-da.php text/html #035 20080602165825403+96 sha1:6H5HQXWNKC5USSA6EPW6T6DV6CQKHWAL - content-size:8922
2008-06-02T16:58:25.681Z   200       1558 http://www.douglasadams.com/images/arch_layout_05_02.gif LLEE http://www.douglasadams.com/creations/0345391829.html image/gif #016 20080602165825579+101 sha1:HGUCSZ4PQ5TQLPI2UJLLZFGJWGFNQIII - content-size:1807
2008-06-02T16:58:25.974Z   200      11377 http://netarkivet.dk/nyheder/index-da.php?highlight=20070704 RRL http://netarkivet.dk/index-da.php text/html #018 20080602165825835+100 sha1:2OQCKST4RWOSC2OHL6Z245FJ2DN5DNOJ - content-size:11572
2008-06-02T16:58:26.066Z     1         56 dns:netarchive.dk RRLEP http://netarchive.dk/suite text/dns #009 20080602165825980+84 sha1:AIQEW3JJGAGPFNHMYKWH5DMBEVJFM6WT - content-size:56
2008-06-02T16:58:26.085Z   200       1352 http://www.douglasadams.com/images/arch_layout_07_02over.gif LLEX http://www.douglasadams.com/creations/0345391829.html image/gif #015 20080602165825983+101 sha1:X5NUUHA73MODG2HP7BAL2ANMRWXANEGW - content-size:1601
2008-06-02T16:58:26.342Z   200       1124 http://netarkivet.dk/nyheder/netarchivesuite.gif RRLE http://netarkivet.dk/nyheder/index-da.php?highlight=20070704 image/gif #019 20080602165826276+65 sha1:V2KUUX4STTMCW55A7LYI6Z2MZ4LNFFWD - content-size:1375
2008-06-02T16:58:26.433Z   404        290 http://netarchive.dk/robots.txt RRLEP http://netarchive.dk/suite text/html #006 20080602165826369+63 sha1:LCJ3EWXG4VC6RSLBXCMPBKBFTZ6DPWHO - content-size:471
2008-06-02T16:58:26.489Z   200       1543 http://www.douglasadams.com/images/arch_layout_05_02over.gif LLEX http://www.douglasadams.com/creations/0345391829.html image/gif #035 20080602165826386+102 sha1:SISIJTQJGBAGVF64D4LVVDOQSLAUEVHP - content-size:1792
2008-06-02T16:58:26.707Z   200        301 http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_04.gif RRE http://netarkivet.dk/index-da.php image/gif #016 20080602165826643+63 sha1:LCJRYXKKVGNMUJOLGXK53WEAVZ552OOX - content-size:551
2008-06-02T16:58:26.955Z   404      10991 http://www.douglasadams.com/creations/document.Narchlayout_09_02 LLEX http://www.douglasadams.com/creations/0345391829.html text/html #007 20080602165826791+153 sha1:HSIXPXF55APZCV35C7AVYFU344PDFHYS - content-size:11264
2008-06-02T16:58:27.082Z   200        407 http://netarkivet.dk/netarkivet_mouseover/billeder/netarkivet_guidelines_07.gif RRX http://netarkivet.dk/index-da.php image/gif #020 20080602165827009+64 sha1:WV7DQCSXLAZGCCZK5IU2HWAY7HD2WCEJ - content-size:657
2008-06-02T16:58:27.083Z   200      15187 http://netarchive.dk/suite RRLE http://netarkivet.dk/nyheder/index-da.php?highlight=20070704 text/html #001 20080602165826733+315 sha1:HFBOPZXZXIAPRFUQP7LXGNAFBNLFOYQM - content-size:15376,3t
2008-06-02T16:58:27.358Z   200       1827 http://www.douglasadams.com/images/creations1.gif LLEE http://www.douglasadams.com/creations/0345391829.html image/gif #008 20080602165827256+101 sha1:FKSRYBKYVIEC2MHUISSNABWNUFF3LF4S - content-size:2076
2008-06-02T16:58:27.523Z   200      11377 http://netarkivet.dk/nyheder/index-da.php?highlight=20041222 RRL http://netarkivet.dk/index-da.php text/html #006 20080602165827384+102 sha1:VNICAUA6MZBTVYG2UH5TTRU26UZSE5L7 - content-size:11572
2008-06-02T16:58:27.827Z   404      10991 http://www.douglasadams.com/creations/document.Narchlayout_07_02 LLEX http://www.douglasadams.com/creations/0345391829.html text/html #016 20080602165827660+153 sha1:HSIXPXF55APZCV35C7AVYFU344PDFHYS - content-size:11264
2008-06-02T16:58:27.896Z   200        234 http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_02.gif RRE http://netarkivet.dk/index-da.php image/gif #012 20080602165827830+65 sha1:NC33QYYCLP3RLQIRTPS5VDZQVIQAJH37 - content-size:483
2008-06-02T16:58:27.901Z   200      10328 http://netarchive.dk/suite/Welcome?action=AttachFile&do=view&target=transparent_logo.png RRLEE http://netarchive.dk/suite text/html #035 20080602165827399+485 sha1:EZM2M7PE4BDXHM6BJGBG5QCTYHX2TCQC - content-size:10516
2008-06-02T16:58:28.229Z   200        962 http://www.douglasadams.com/images/arch_layout_08_02.gif LLEE http://www.douglasadams.com/creations/0345391829.html image/gif #028 20080602165828129+99 sha1:IMGHENZLM3JVVEHUCYKTBVS4QDK6NJVR - content-size:1210
2008-06-02T16:58:28.264Z   200        279 http://netarkivet.dk/netarkivet_mouseover/billeder/netarkivet_guidelines_08.gif RRX http://netarkivet.dk/index-da.php image/gif #019 20080602165828198+65 sha1:X3UMMU2PPK46YHRU235UQPKSTLEA3YAV - content-size:529
2008-06-02T16:58:28.454Z   200        587 http://netarchive.dk/wiki/modern/css/projection.css RRLEE http://netarchive.dk/suite text/css #010 20080602165828388+64 sha1:QC434RA3GD54LGC5AHUDGDNX23DYXWRJ - content-size:837
2008-06-02T16:58:28.638Z   200       1426 http://www.douglasadams.com/images/arch_layout_06_02over.gif LLEX http://www.douglasadams.com/creations/0345391829.html image/gif #004 20080602165828531+106 sha1:VJUJ2ZIHHYFVLA57IYVOHUIPXZEDTCRT - content-size:1675
2008-06-02T16:58:28.740Z   200      17658 http://netarkivet.dk/publikationer/index-da.php RRL http://netarkivet.dk/index-da.php text/html #016 20080602165828565+129 sha1:IKF55UKOBMIFQJGZAH6U4AS76LTYDL6M - content-size:17853
2008-06-02T16:58:29.086Z   200      15187 http://netarchive.dk/suite/Welcome RRLEE http://netarchive.dk/suite text/html #012 20080602165828754+316 sha1:HFBOPZXZXIAPRFUQP7LXGNAFBNLFOYQM - content-size:15376
2008-06-02T16:58:29.090Z   200       5952 http://www.douglasadams.com/images/creations2.gif LLEE http://www.douglasadams.com/creations/0345391829.html image/gif #030 20080602165828940+149 sha1:TT4UEAF5HVB2ACQON5DGHGDV55TZJAGZ - content-size:6202
2008-06-02T16:58:29.105Z   404        313 http://netarkivet.dk/publikationer/statsbiblioteket.dk RRLX http://netarkivet.dk/publikationer/index-da.php text/html #009 20080602165829042+62 sha1:YTYKJPQIPH57A2FPCCOUAZR66QE5JVCL - content-size:494
2008-06-02T16:58:29.472Z   404        313 http://netarkivet.dk/publikationer/statsbiblitoeket.dk RRLX http://netarkivet.dk/publikationer/index-da.php text/html #004 20080602165829406+65 sha1:3I4RIWDFPANG44637NJRED53GHUWWUFF - content-size:494
2008-06-02T16:58:29.497Z   200        987 http://www.douglasadams.com/images/arch_layout_03_02.gif LLEE http://www.douglasadams.com/creations/0345391829.html image/gif #010 20080602165829392+104 sha1:WKTBNC3AUR7LLHQEAQHQLF4DAFENRNHP - content-size:1235
2008-06-02T16:58:29.839Z   301        315 http://netarkivet.dk/faq RRL http://netarkivet.dk/index-da.php text/html #035 20080602165829774+62 sha1:GYQJGQZ33CESIQLST6IN4DZ3SSQTXF5M - content-size:541
2008-06-02T16:58:30.101Z   200      32664 http://netarchive.dk/suite/TitleIndex RRLEE http://netarchive.dk/suite text/html #006 20080602165829403+634 sha1:FM2PGMVJGQVPNXNXWSVYX73NGQ5NEP52 - content-size:32853
2008-06-02T16:58:30.204Z   302          0 http://netarkivet.dk/faq/ RRLR http://netarkivet.dk/faq text/html #028 20080602165830141+62 sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ - content-size:241
2008-06-02T16:58:30.633Z   200       8874 http://netarkivet.dk/presse/index-da.php?highlight=20060705 RRL http://netarkivet.dk/index-da.php text/html #016 20080602165830506+98 sha1:377HRVXDEGDV2HTP2EIDD2EEFQS7KG45 - content-size:9069
2008-06-02T16:58:31.004Z   200       1579 http://netarchive.dk/suite/Welcome?action=raw RRLEE http://netarchive.dk/suite text/plain #032 20080602165830736+267 sha1:JOXWBJMQRYZNYEQDSKMEIP2ANARFY2NR - content-size:1815
2008-06-02T16:58:31.067Z   200      11377 http://netarkivet.dk/nyheder/index-da.php?highlight=20070330 RRL http://netarkivet.dk/index-da.php text/html #007 20080602165830934+101 sha1:YK2DZA6CYPJK7A4HSRV6MT4RA4ERZ2FR - content-size:11572
2008-06-02T16:58:31.375Z   200       3892 http://netarchive.dk/wiki/common/js/common.js RRLEE http://netarchive.dk/suite application/x-javascript #008 20080602165831306+67 sha1:AQX5NKXRI6QYML6TPWZRMQM7X5HU3CVP - content-size:4159
2008-06-02T16:58:31.506Z   200      11303 http://netarkivet.dk/nyheder/index-da.php RRL http://netarkivet.dk/index-da.php text/html #004 20080602165831368+101 sha1:2L3LO3WV3GM5JTY2DRCEF2PPOWDS7QDC - content-size:11498
2008-06-02T16:58:31.874Z   200       1677 http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_13.gif RRE http://netarkivet.dk/index-da.php image/gif #024 20080602165831808+65 sha1:PMKEU2EBIPANJEMZ3XPUE3HZWDMHCEQH - content-size:1928
2008-06-02T16:58:32.242Z   301        319 http://netarkivet.dk/forslag RRL http://netarkivet.dk/index-da.php text/html #028 20080602165832177+63 sha1:H73U5HIFUNXSLG4IAN2DBRG42U72CW2S - content-size:549
2008-06-02T16:58:32.500Z   200      54102 http://netarchive.dk/suite/WordIndex RRLEE http://netarchive.dk/suite text/html #035 20080602165831677+757 sha1:K3QA57LECEDALQQNY6FPROD4H6OMTYRE - content-size:54291
2008-06-02T16:58:32.608Z   302          0 http://netarkivet.dk/forslag/ RRLR http://netarkivet.dk/forslag text/html #016 20080602165832544+63 sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ - content-size:241
2008-06-02T16:58:33.062Z   200      12606 http://netarkivet.dk/forslag/index-da.php RRLRR http://netarkivet.dk/forslag/ text/html #048 20080602165832911+120 sha1:5XD6CLMNHRLM35YNL2LCZ4VUFL5W5KAN - content-size:12801
2008-06-02T16:58:33.427Z   200         70 http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_12.gif RRE http://netarkivet.dk/index-da.php image/gif #008 20080602165833363+63 sha1:4JCFDAGUSWHQI7ZDJPZ7TL376T2RIQBK - content-size:318
2008-06-02T16:58:33.755Z   200      10298 http://netarchive.dk/suite/Welcome?action=AttachFile&do=view&target=netarkivet.gif RRLEE http://netarchive.dk/suite text/html #019 20080602165833257+486 sha1:5YIFBUZZ32AELMSF6QWITP75LCOYF6Y4 - content-size:10486
2008-06-02T16:58:33.792Z   200        249 http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_05.gif RRE http://netarkivet.dk/index-da.php image/gif #024 20080602165833729+62 sha1:EO4DKE5CWEBIAEIZQLAYY2F2FOCMGBE7 - content-size:498
2008-06-02T16:58:34.158Z   200         76 http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_10.gif RRE http://netarkivet.dk/index-da.php image/gif #036 20080602165834094+63 sha1:MWC3WWS3JIMG2DNM6IICL5EYTCV2PSX7 - content-size:324
2008-06-02T16:58:34.308Z   200        775 http://netarchive.dk/wiki/modern/css/print.css RRLEE http://netarchive.dk/suite text/css #028 20080602165834242+65 sha1:5H3BTAI7GIXYJPSLYL2DEE4323EI7Q6W - content-size:1025
2008-06-02T16:58:34.525Z   200        407 http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_07.gif RRE http://netarkivet.dk/index-da.php image/gif #010 20080602165834459+65 sha1:ZZICEZONJLX77I3LDTM2KCW7TV2RJW3Z - content-size:657
2008-06-02T16:58:34.708Z   200       7558 http://netarchive.dk/wiki/modern/css/screen.css RRLEE http://netarchive.dk/suite text/css #016 20080602165834609+95 sha1:2E6RM7GOPS7H2T553PG5MCS5OGDGXPAW - content-size:7809
2008-06-02T16:58:34.977Z   200      11517 http://netarkivet.dk/index-en.php RRL http://netarkivet.dk/index-da.php text/html #041 20080602165834827+101 sha1:RIHA3WYGBB6JEBDUVAA4VPUYXSLFOEDF - content-size:11712
2008-06-02T16:58:35.113Z   200       7048 http://netarchive.dk/wiki/modern/css/common.css RRLEE http://netarchive.dk/suite text/css #048 20080602165835010+94 sha1:RFTTC5ZJ7TS3AJL2QB57TAGXRTUVMNHY - content-size:7300
2008-06-02T16:58:35.342Z   200         43 http://netarkivet.dk/netarchive_alm/billeder/spacer.gif RRLE http://netarkivet.dk/index-en.php image/gif #028 20080602165835279+62 sha1:5J67LA4YGEZ3MJYSWXTTX7542ROMKNZW - content-size:291
2008-06-02T16:58:35.711Z   200       1733 http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_13.gif RRLE http://netarkivet.dk/index-en.php image/gif #016 20080602165835643+67 sha1:KR5IEXNFNB5NT3E26BH4GGN3MTCYOQ6E - content-size:1984
2008-06-02T16:58:35.764Z   200      21967 http://netarchive.dk/suite/HelpOnFormatting RRLEE http://netarchive.dk/suite text/html #008 20080602165835415+326 sha1:25DN5ZP567K6LGY6QRSU4OYSARDM72Y4 - content-size:22156
2008-06-02T16:58:36.078Z   200        428 http://netarkivet.dk/netarchive_mouseover/billeder/netarkivet_guidelines_07.gif RRLX http://netarkivet.dk/index-en.php image/gif #007 20080602165836013+64 sha1:7EXP2PHAMEUNVXX65H44H7JHCK6UB2MN - content-size:678
2008-06-02T16:58:36.361Z   200       1124 http://netarchive.dk/suite/Welcome?action=AttachFile&do=get&target=netarkivet.gif RRLEE http://netarchive.dk/suite image/gif #030 20080602165836092+268 sha1:V2KUUX4STTMCW55A7LYI6Z2MZ4LNFFWD - content-size:1422
2008-06-02T16:58:36.443Z   200        251 http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_20.gif RRLE http://netarkivet.dk/index-en.php image/gif #035 20080602165836379+63 sha1:STTVR3FJILXODO63X5B57XKS3VLCTD54 - content-size:500
2008-06-02T16:58:36.809Z   200        440 http://netarkivet.dk/netarchive_mouseover/billeder/netarkivet_guidelines_06.gif RRLX http://netarkivet.dk/index-en.php image/gif #019 20080602165836744+64 sha1:THHDJULHOM4NY5B5T3JAZFJQDZN4L5OD - content-size:690
2008-06-02T16:58:37.039Z   200      12381 http://netarchive.dk/suite/FindPage RRLEE http://netarchive.dk/suite text/html #016 20080602165836662+364 sha1:XVYC4GYGYKS2S7TTBEOPI3FHCP2U67AG - content-size:12570
2008-06-02T16:58:37.174Z   200        209 http://netarkivet.dk/netarchive_mouseover/billeder/netarkivet_guidelines_09.gif RRLX http://netarkivet.dk/index-en.php image/gif #048 20080602165837111+62 sha1:NEOTOCAV3JFNAHMJXKMHMOEML4BESPA3 - content-size:458
2008-06-02T16:58:37.541Z   200        478 http://netarkivet.dk/netarchive_mouseover/billeder/netarkivet_guidelines_03.gif RRLX http://netarkivet.dk/index-en.php image/gif #004 20080602165837475+65 sha1:7E2MALHL3YN34SKRDUYQJILUGMHEXJT5 - content-size:728
2008-06-02T16:58:37.706Z   200       7370 http://netarchive.dk/suite/Welcome?action=print RRLEE http://netarchive.dk/suite text/html #035 20080602165837403+294 sha1:QQGHRN2NPEAVBEXAEV55CDFAKXBEJA7F - content-size:7559
2008-06-02T16:58:37.907Z   200         70 http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_12.gif RRLE http://netarkivet.dk/index-en.php image/gif #037 20080602165837843+63 sha1:4JCFDAGUSWHQI7ZDJPZ7TL376T2RIQBK - content-size:318
2008-06-02T16:58:38.273Z   200        204 http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_14.gif RRLE http://netarkivet.dk/index-en.php image/gif #028 20080602165838208+64 sha1:7L6F7XZDQMMV4LYYUN5ZNVJRAJU4MJA3 - content-size:453
2008-06-02T16:58:38.298Z   200       9146 http://netarchive.dk/suite/Welcome?action=AttachFile&do=get&target=transparent_logo.png RRLEEE http://netarchive.dk/suite/Welcome?action=AttachFile&do=view&target=transparent_logo.png image/png #016 20080602165838008+289 sha1:V7PDPNYPYHOLD2DK4LZUL5NSEUGU45ZM - content-size:9450
2008-06-02T16:58:38.637Z   200        242 http://netarkivet.dk/netarchive_mouseover/billeder/netarkivet_guidelines_05.gif RRLX http://netarkivet.dk/index-en.php image/gif #035 20080602165838575+61 sha1:RWL4VLCGTJE2UTWMF4OC5VPAYOOUPVX2 - content-size:491
2008-06-02T16:58:38.661Z   200        178 http://netarchive.dk/wiki/modern/img/moin-attach.png RRLEEE http://netarchive.dk/suite/TitleIndex image/png #008 20080602165838599+61 sha1:R74PGPXM3JB6PRICP7NPMQ6ZNS5SDCJL - content-size:428
2008-06-02T16:58:39.004Z   200       2287 http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_01.gif RRLE http://netarkivet.dk/index-en.php image/gif #018 20080602165838939+64 sha1:4VTDVM3CT5OLO6Y7LTPMIMBTESCLPNNN - content-size:2538
2008-06-02T16:58:39.368Z   200        276 http://netarkivet.dk/netarchive_mouseover/billeder/netarkivet_guidelines_11.gif RRLX http://netarkivet.dk/index-en.php image/gif #030 20080602165839306+62 sha1:VD6FZG3TMOENNHSMFKY37JBA54P2QWEU - content-size:526
2008-06-02T16:58:39.639Z   200      24820 http://netarchive.dk/suite/TitleIndex?action=print RRLEEE http://netarchive.dk/suite/TitleIndex text/html #041 20080602165838961+627 sha1:F65NW54F3H4PP4VN472GYZFAZUISHNEM - content-size:25009
2008-06-02T16:58:39.732Z   200         56 http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_16.gif RRLE http://netarkivet.dk/index-en.php image/gif #024 20080602165839670+61 sha1:FOBREXIPW6KAKDLQ65S7QCNWIODJKEDX - content-size:304
2008-06-02T16:58:40.096Z   200        295 http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_19.gif RRLE http://netarkivet.dk/index-en.php image/gif #007 20080602165840034+61 sha1:4Z36L4FC7YK7G2DEGRMMWLTBOQTNSI5C - content-size:545
2008-06-02T16:58:40.460Z   200        440 http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_06.gif RRLE http://netarkivet.dk/index-en.php image/gif #010 20080602165840397+62 sha1:VFRVBY3J4BJHPDICSV7IPXDKNLPE4F7L - content-size:690
2008-06-02T16:58:40.527Z   200        479 http://netarchive.dk/suite/TitleIndex?action=raw RRLEEE http://netarchive.dk/suite/TitleIndex text/plain #028 20080602165840267+259 sha1:WSOXNBWHP5WSOOH2RMGFWLIFAGBRU3PL - content-size:715
2008-06-02T16:58:40.824Z   200        163 http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_17.gif RRLE http://netarkivet.dk/index-en.php image/gif #019 20080602165840761+62 sha1:Q2Z2YBECUJOB4JFCDBGT5SEWKMP4XIOK - content-size:412
2008-06-02T16:58:41.188Z   200        273 http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_08.gif RRLE http://netarkivet.dk/index-en.php image/gif #036 20080602165841126+61 sha1:P3X77U4QSFYEVVYT2SQS2ZUN4CWSANAV - content-size:523
2008-06-02T16:58:41.553Z   200        273 http://netarkivet.dk/netarchive_mouseover/billeder/netarkivet_guidelines_08.gif RRLX http://netarkivet.dk/index-en.php image/gif #028 20080602165841490+62 sha1:3RFZHRVWESNQXUSW6LXUKPCBHAEPW6KE - content-size:523
2008-06-02T16:58:41.638Z   200      46265 http://netarchive.dk/suite/WordIndex?action=print RRLEEE http://netarchive.dk/suite/WordIndex text/html #037 20080602165840828+743 sha1:FDLSVVN6KMW7JLEN4GYHUU4ELW4C5SGY - content-size:46454
2008-06-02T16:58:41.918Z   200        478 http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_03.gif RRLE http://netarkivet.dk/index-en.php image/gif #033 20080602165841855+62 sha1:GP6GQ3RSSV7LESACJ5GDRS5SOZSIILS5 - content-size:728
2008-06-02T16:58:42.282Z   200         76 http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_10.gif RRLE http://netarkivet.dk/index-en.php image/gif #016 20080602165842220+61 sha1:MWC3WWS3JIMG2DNM6IICL5EYTCV2PSX7 - content-size:324
2008-06-02T16:58:42.646Z   200        265 http://netarkivet.dk/netarchive_mouseover/billeder/netarkivet_guidelines_04.gif RRLX http://netarkivet.dk/index-en.php image/gif #037 20080602165842583+63 sha1:XU6CRACCGFXCAODB73EHNAEC4XAJOWBC - content-size:515
2008-06-02T16:58:42.676Z   200        453 http://netarchive.dk/suite/WordIndex?action=raw RRLEEE http://netarchive.dk/suite/WordIndex text/plain #010 20080602165842382+293 sha1:IFMARPUZIHREISVWI5PCN6UCEDNISMYE - content-size:689
2008-06-02T16:58:43.011Z   200        428 http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_07.gif RRLE http://netarkivet.dk/index-en.php image/gif #018 20080602165842949+62 sha1:Q6TITNTYNWCP3BQIS7L5X7GQPP5FBI3F - content-size:678
2008-06-02T16:58:43.045Z   404        308 http://netarchive.dk/wiki/modern/img/tab-wiki.png RRLEEE http://netarchive.dk/wiki/modern/css/screen.css text/html #032 20080602165842977+68 sha1:NKUL6DVQ4LZBYQ6DZF24LULBCRLGB3ZT - content-size:489
2008-06-02T16:58:43.378Z   200        265 http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_04.gif RRLE http://netarkivet.dk/index-en.php image/gif #030 20080602165843313+64 sha1:5LJ5J2NUBNUZOOYJSEM54HZULJCFHO7D - content-size:515
2008-06-02T16:58:43.448Z   200       9717 http://netarchive.dk/wiki/modern/img/draft.png RRLEEE http://netarchive.dk/wiki/modern/css/screen.css image/png #004 20080602165843346+101 sha1:AXH2IFNXC4MUT26SRHRJZHGR3FDAJDNR - content-size:9970
2008-06-02T16:58:43.743Z   200        209 http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_09.gif RRLE http://netarkivet.dk/index-en.php image/gif #024 20080602165843679+63 sha1:IAKPAZSAVLCUNNDLF6KDQ7AO7NHQP2AQ - content-size:458
2008-06-02T16:58:43.812Z   404        312 http://netarchive.dk/wiki/modern/img/tab-selected.png RRLEEE http://netarchive.dk/wiki/modern/css/screen.css text/html #019 20080602165843749+63 sha1:CJSOBF6MY3EIV53BE3YMS2GCAUCFKMAS - content-size:493
2008-06-02T16:58:44.109Z   200        234 http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_02.gif RRLE http://netarkivet.dk/index-en.php image/gif #032 20080602165844045+63 sha1:NC33QYYCLP3RLQIRTPS5VDZQVIQAJH37 - content-size:483
2008-06-02T16:58:44.176Z   404        308 http://netarchive.dk/wiki/modern/img/tab-user.png RRLEEE http://netarchive.dk/wiki/modern/css/screen.css text/html #048 20080602165844114+62 sha1:YEYQUYANFH6UJSNKP2775OYFP3BVVSCN - content-size:489
2008-06-02T16:58:44.475Z   200        276 http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_11.gif RRLE http://netarkivet.dk/index-en.php image/gif #004 20080602165844411+63 sha1:EAE2FFGNOXB2FVTPXOXMY45LWDTXT3J3 - content-size:526
2008-06-02T16:58:44.541Z   200        214 http://netarchive.dk/wiki/modern/img/moin-inter.png RRLEEE http://netarchive.dk/wiki/modern/css/common.css image/png #028 20080602165844477+63 sha1:AZDXPMOFJGWC72MJIK4RNWROTQVMIYK6 - content-size:464
2008-06-02T16:58:44.841Z   200        242 http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_05.gif RRLE http://netarkivet.dk/index-en.php image/gif #019 20080602165844776+64 sha1:LMIW33PQDYKCP74JCOMX5QEPV5BU4YMM - content-size:491
2008-06-02T16:58:44.907Z   200        272 http://netarchive.dk/wiki/modern/img/moin-ftp.png RRLEEE http://netarchive.dk/wiki/modern/css/common.css image/png #022 20080602165844842+64 sha1:EBWHW6FURKB4MEUMLJQFS7NBUMTN3IKS - content-size:523
2008-06-02T16:58:45.208Z   200        204 http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_14.gif RRE http://netarkivet.dk/index-da.php image/gif #048 20080602165845142+64 sha1:7L6F7XZDQMMV4LYYUN5ZNVJRAJU4MJA3 - content-size:453
2008-06-02T16:58:45.272Z   200        189 http://netarchive.dk/wiki/modern/img/moin-news.png RRLEEE http://netarchive.dk/wiki/modern/css/common.css image/png #016 20080602165845208+63 sha1:5HI4RRWYPKRTY5FNNHVIBMAXC3QREDUH - content-size:439
2008-06-02T16:58:45.645Z   200      10350 http://netarkivet.dk/links/index-da.php RRL http://netarkivet.dk/index-da.php text/html #028 20080602165845510+102 sha1:MP7CCE7SP5HNNVUAJBMUIUAVG2PWJNDL - content-size:10545
2008-06-02T16:58:45.647Z   200        159 http://netarchive.dk/wiki/modern/img/moin-email.png RRLEEE http://netarchive.dk/wiki/modern/css/common.css image/png #035 20080602165845573+63 sha1:7E7D7ZAHIBN2B5JHK4AFTY5PZHNSIH4M - content-size:409
2008-06-02T16:58:46.013Z   200        189 http://netarchive.dk/wiki/modern/img/moin-telnet.png RRLEEE http://netarchive.dk/wiki/modern/css/common.css image/png #018 20080602165845949+63 sha1:5HI4RRWYPKRTY5FNNHVIBMAXC3QREDUH - content-size:439
2008-06-02T16:58:46.018Z   200        295 http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_19.gif RRE http://netarkivet.dk/index-da.php image/gif #020 20080602165845949+69 sha1:4Z36L4FC7YK7G2DEGRMMWLTBOQTNSI5C - content-size:545
2008-06-02T16:58:46.377Z   200        164 http://netarchive.dk/wiki/modern/img/attention.png RRLEEE http://netarchive.dk/wiki/modern/css/common.css image/png #030 20080602165846314+62 sha1:IILD75JGR4A6OLP6X2VQIZCSCTB6UDVK - content-size:414
2008-06-02T16:58:46.485Z   200      40935 http://netarkivet.dk/presse/Pressemeddelelse_juli2006.pdf RRLL http://netarkivet.dk/presse/index-da.php application/pdf #004 20080602165846319+165 sha1:KGGKPMZT466QKJHBFTFFA7D53XSOX4HL - content-size:41220
2008-06-02T16:58:46.742Z   200        150 http://netarchive.dk/wiki/modern/img/moin-www.png RRLEEE http://netarchive.dk/wiki/modern/css/common.css image/png #010 20080602165846678+63 sha1:HNHMFVO47VMGOCFP5CEJ6AFXBDTKW2MO - content-size:400
2008-06-02T16:58:46.953Z   200      33967 http://netarkivet.dk/presse/Bryllup-20040706.pdf RRLL http://netarkivet.dk/presse/index-da.php application/pdf #019 20080602165846786+166 sha1:HSQV5MOF3YPVD2SRY653TG4T5O3UV2BQ - content-size:34252
2008-06-02T16:58:47.369Z   200      14067 http://netarchive.dk/suite/HelpOnFormatting?action=print RRLEEE http://netarchive.dk/suite/HelpOnFormatting text/html #015 20080602165847042+318 sha1:AZL32LC2DQUSK3GCJQOWDYK62ZFSRMUY - content-size:14256
2008-06-02T16:58:47.450Z   200      65703 http://netarkivet.dk/presse/nethoestning_KUM_pressemed.pdf RRLL http://netarkivet.dk/presse/index-da.php application/pdf #016 20080602165847255+194 sha1:TQNPWB7EVMGXOUSXFTF3DALATAWFW7OX - content-size:65963
2008-06-02T16:58:47.981Z   200       3333 http://netarchive.dk/suite/HelpOnFormatting?action=raw RRLEEE http://netarchive.dk/suite/HelpOnFormatting text/plain #010 20080602165847689+291 sha1:ODUAFFCXDANCYOUJATLE65674FQVVSBT - content-size:3569
2008-06-02T16:58:48.039Z   200     143857 http://netarkivet.dk/kildetekster/ProxyViewer-0.1.tar.gz RRLL http://netarkivet.dk/kildetekster/index-da.php application/x-gzip #022 20080602165847751+287 sha1:KNZD77DNPYYDSJASBZDKLOO3YXNMKWZN - content-size:144147
2008-06-02T16:58:48.464Z   200      13277 http://netarkivet.dk/kildetekster/JavaArcUtils-0.3.tar.gz RRLL http://netarkivet.dk/kildetekster/index-da.php application/x-gzip #030 20080602165848340+123 sha1:OKCTCILJM3656DJP6LNFPMS26OKWXKXD - content-size:13565
2008-06-02T16:58:48.547Z   200       1036 http://netarchive.dk/suite/FindPage?action=raw RRLEEE http://netarchive.dk/suite/FindPage text/plain #015 20080602165848282+264 sha1:CR7QZ4B4AT3INR7DSB2SR7E6F6LI6LBX - content-size:1272
2008-06-02T16:58:48.933Z   200      17732 http://netarkivet.dk/publikationer/index-da.php?highlight=20060519 RRLL http://netarkivet.dk/nyheder/index-da.php?highlight=20070704 text/html #044 20080602165848767+129 sha1:FCH2JFCJ3I6YGV4FZBJ4JUENQAHJII27 - content-size:17927
2008-06-02T16:58:49.248Z   200       4610 http://netarchive.dk/suite/FindPage?action=print RRLEEE http://netarchive.dk/suite/FindPage text/html #031 20080602165848848+393 sha1:JH4GPTPOCQO5EZA7SSXQ2JYTJFFXVIDZ - content-size:4799
2008-06-02T16:58:50.113Z   200     647007 http://netarkivet.dk/nyheder/Newsletter_Netarchive_dk_march2007.pdf RRLL http://netarkivet.dk/nyheder/index-da.php?highlight=20070704 application/pdf #016 20080602165849236+873 sha1:5WGVAIDNEHC4KMPZKM43EF4345BXRYFE - content-size:647268
2008-06-02T16:58:51.558Z   200     388964 http://netarkivet.dk/nyheder/Newsletter_Netarchive_dk_august2006.pdf RRLL http://netarkivet.dk/nyheder/index-da.php?highlight=20070704 application/pdf #018 20080602165850988+567 sha1:JKOL4334OHMZBPBDQLZNIC4RH7TLKSKU - content-size:389225
2008-06-02T16:58:53.111Z   200     723456 http://netarkivet.dk/publikationer/ECDL2001-bnh-08092001.ppt RRLL http://netarkivet.dk/publikationer/index-da.php application/vnd.ms-powerpoint #036 20080602165852126+981 sha1:O7NLBCYQQ63X23QG2A25RYO6S7TL4LU5 - content-size:723731
2008-06-02T16:58:54.770Z   200     481188 http://netarkivet.dk/publikationer/iwaw05-christensen.pdf RRLL http://netarkivet.dk/publikationer/index-da.php application/pdf #007 20080602165854094+673 sha1:2K6NQUCSN74LJBSGLVQRXN4TRO7QMA5E - content-size:481449
2008-06-02T16:58:56.637Z   200     928844 http://netarkivet.dk/publikationer/webarkivering-webarchiving.pdf RRLL http://netarkivet.dk/publikationer/index-da.php application/pdf #030 20080602165855445+1187 sha1:D3KJJ2KBXKWFPMNZII4DL5SB5DF2AG5L - content-size:929105
2008-06-02T16:58:57.995Z   200     202974 http://netarkivet.dk/publikationer/Etags-2004.pdf RRLL http://netarkivet.dk/publikationer/index-da.php application/pdf #041 20080602165857639+355 sha1:Z3G7EAE47UAQRA5MID6RNCPGYFKX3PBF - content-size:203235
2008-06-02T16:58:59.483Z   200     787861 http://netarkivet.dk/publikationer/webark-final-rapport-2003.pdf RRLL http://netarkivet.dk/publikationer/index-da.php application/pdf #004 20080602165858452+1027 sha1:YMFYNHFAN5KFIGSGOGJSVGHXJZOFXOOT - content-size:788122
2008-06-02T16:59:00.853Z   200     216713 http://netarkivet.dk/publikationer/FormatRepositories-2004.pdf RRLL http://netarkivet.dk/publikationer/index-da.php application/pdf #015 20080602165900485+366 sha1:YSKZCV3U6ZW75EM6NWZOLLDOI2KILDDB - content-size:216974
2008-06-02T16:59:01.733Z   200     340509 http://netarkivet.dk/publikationer/FileFormats-2004.pdf RRLL http://netarkivet.dk/publikationer/index-da.php application/pdf #031 20080602165901220+511 sha1:LK4752I5S5P3VOYTSNIE6IORW4I2YBNN - content-size:340770
2008-06-02T16:59:02.453Z   200      78087 http://netarkivet.dk/publikationer/iwaw06-clausen.pdf RRLL http://netarkivet.dk/publikationer/index-da.php application/pdf #004 20080602165902246+206 sha1:LUIV26WP27BPP5Y7UY3JEWNJKUT4FPCI - content-size:78347
2008-06-02T16:59:03.371Z   200     429874 http://netarkivet.dk/publikationer/Archival_format_requirements-2004.pdf RRLL http://netarkivet.dk/publikationer/index-da.php application/pdf #007 20080602165902755+613 sha1:NUQAFRMVM2PAEYLHFXGW4JHDCT3VCMIH - content-size:430135
2008-06-02T16:59:04.433Z   200     284558 http://netarkivet.dk/publikationer/Kanon-Juli05.pdf RRLL http://netarkivet.dk/publikationer/index-da.php application/pdf #041 20080602165903985+446 sha1:QH4FQRSNU4D3M4EHKZBXTW4OHDRWTWDE - content-size:284819
2008-06-02T16:59:05.140Z   200     120591 http://netarkivet.dk/publikationer/nhc-kb-dk-msst2006.pdf RRLL http://netarkivet.dk/publikationer/index-da.php application/pdf #003 20080602165904881+257 sha1:2XDVCDQ4BFZLS563TYKEWONPMKJZPG3W - content-size:120852
2008-06-02T16:59:05.577Z   200       8415 http://netarkivet.dk/forslag/domtester.php RRLRRL http://netarkivet.dk/forslag/index-da.php text/html #004 20080602165905441+107 sha1:GRZ5ISG62A5FXD6R4KCBFV4QCH6CXXF2 - content-size:8610
2008-06-02T16:59:06.004Z   200       8706 http://netarkivet.dk/organisation/index-en.php RRLL http://netarkivet.dk/index-en.php text/html #038 20080602165905879+96 sha1:QUYCIEWFBDWN5KMF6R5FKSUFN3BC42EL - content-size:8901
2008-06-02T16:59:06.444Z   200      10949 http://netarkivet.dk/nyheder/index-en.php?highlight=20070704 RRLL http://netarkivet.dk/index-en.php text/html #007 20080602165906306+99 sha1:E53KIHBNXQSH4IMFSYRJYJWGEWX5O3TX - content-size:11144
2008-06-02T16:59:06.920Z   200      19499 http://netarkivet.dk/publikationer/index-en.php?highlight=20071102 RRLL http://netarkivet.dk/index-en.php text/html #024 20080602165906746+131 sha1:IB5ELFNY6ZKROG23WG4POZXAF7XU7QXI - content-size:19694
2008-06-02T16:59:07.286Z   404        299 http://netarkivet.dk/publikationer/kb.dk RRLLX http://netarkivet.dk/publikationer/index-en.php?highlight=20071102 text/html #041 20080602165907222+63 sha1:VUBJFRXRSWTYPTVC4XBD7GUX4RI35GY5 - content-size:480
2008-06-02T16:59:07.652Z   404        307 http://netarkivet.dk/publikationer/netarkivet.dk RRLLX http://netarkivet.dk/publikationer/index-en.php?highlight=20071102 text/html #030 20080602165907588+63 sha1:FEC5CQLVSML3PPTEWFJK5KUAA6JHIMFZ - content-size:488
2008-06-02T16:59:08.083Z   200      10875 http://netarkivet.dk/nyheder/index-en.php RRLL http://netarkivet.dk/index-en.php text/html #019 20080602165907954+100 sha1:DXZREEG3B24UAAWLPF43EJBTMZWYDAZR - content-size:11070
2008-06-02T16:59:08.558Z   200      12686 http://netarkivet.dk/kildetekster/index-en.php RRLL http://netarkivet.dk/index-en.php text/html #007 20080602165908385+126 sha1:TDEWOCITPOZF4BTQTSEMO4OF6Z6O25HX - content-size:12881
2008-06-02T16:59:08.986Z   200       8226 http://netarkivet.dk/faq/index-en.php RRLL http://netarkivet.dk/index-en.php text/html #040 20080602165908861+97 sha1:OF5J6IXEI3IY4MBEBAYAEDZOBOIDMYAB - content-size:8421
2008-06-02T16:59:09.455Z   200      19425 http://netarkivet.dk/publikationer/index-en.php RRLL http://netarkivet.dk/index-en.php text/html #041 20080602165909287+129 sha1:Q4HLVKFBZ3GQCGVNE22BFXRLBXFWEMFS - content-size:19620
2008-06-02T16:59:09.886Z   200      10875 http://netarkivet.dk/nyheder/index-en.php?highlight=20070330 RRLL http://netarkivet.dk/index-en.php text/html #015 20080602165909757+100 sha1:DXZREEG3B24UAAWLPF43EJBTMZWYDAZR - content-size:11070
2008-06-02T16:59:10.357Z   200      19499 http://netarkivet.dk/publikationer/index-en.php?highlight=20070918 RRLL http://netarkivet.dk/index-en.php text/html #048 20080602165910188+130 sha1:D5Q5ZO2MKED5E62XX5Y2GHBT6HRMN6G6 - content-size:19694
2008-06-02T16:59:10.785Z   200      10092 http://netarkivet.dk/links/index-en.php RRLL http://netarkivet.dk/index-en.php text/html #008 20080602165910658+99 sha1:6GC7OL5GENN6X2V6IFBKLJGPRBBINMXZ - content-size:10287
2008-06-02T16:59:11.268Z   200      19499 http://netarkivet.dk/publikationer/index-en.php?highlight=20070502 RRLL http://netarkivet.dk/index-en.php text/html #012 20080602165911088+129 sha1:NYX6YPW53NOQMMXN4BBJLBG7BDOK4W7V - content-size:19694
2008-06-02T16:59:12.766Z   200     931885 http://netarkivet.dk/publikationer/DFrevy_english.pdf RRLL http://netarkivet.dk/index-en.php application/pdf #018 20080602165911570+1191 sha1:OM26J2VS6CNOHWG6ILB4OPEGIYREWDGZ - content-size:932146
2008-06-02T16:59:13.892Z   200       8742 http://netarkivet.dk/presse/index-en.php RRLL http://netarkivet.dk/index-en.php text/html #008 20080602165913768+97 sha1:ZXSZEZVHE4DP7GADTGUCXZUEZLGJ4CS7 - content-size:8937
2008-06-02T16:59:14.833Z   200     109213 http://netarkivet.dk/publikationer/InteroperabilityInTheFuture_IFLA2007.pdf RRLLL http://netarkivet.dk/publikationer/index-en.php?highlight=20071102 application/pdf #028 20080602165914588+244 sha1:5GNII75TW22U2LM2PRWDW6PEHGQTDE5F - content-size:109474
2008-06-02T16:59:16.119Z   200     747666 http://netarkivet.dk/publikationer/CollectingTheDanishInternet_2007.pdf RRLLL http://netarkivet.dk/publikationer/index-en.php?highlight=20071102 application/pdf #003 20080602165915135+980 sha1:VKC6Z63W5Q4Z4SYRMJXSZXCMKPQS2FLF - content-size:747927
2008-06-02T16:59:17.745Z   200     454116 http://netarkivet.dk/publikationer/Event-definition-final.pdf RRLLL http://netarkivet.dk/publikationer/index-en.php?highlight=20071102 application/pdf #006 20080602165917101+641 sha1:WM45TXWCP6CXR7IK2HPLDPJ3NWPMEMME - content-size:454377
2008-06-02T16:59:19.474Z   200     835366 http://netarkivet.dk/publikationer/IntegrationOfNonHarvestedData.pdf RRLLL http://netarkivet.dk/publikationer/index-en.php?highlight=20071102 application/pdf #041 20080602165918388+1082 sha1:F3MN4YGS3G54XMDB5JTYQJSL5RTAMRWP - content-size:835627
2008-06-02T16:59:20.476Z -5003          - http://netarkivet.dk/publikationer/CollectingTheDanishInternet_2007_ES.pdf RRLLL http://netarkivet.dk/publikationer/index-en.php?highlight=20071102 no-type #007 - - - Q:group-max-all-kb

metadata://netarkivet.dk/crawl/logs/local-errors.log?heritrixVersion=1.12.1b&harvestid=2&jobid=12 127.0.1.1 20080602165806 text/plain 0

metadata://netarkivet.dk/crawl/logs/progress-statistics.log?heritrixVersion=1.12.1b&harvestid=2&jobid=12 127.0.1.1 20080602165920 text/plain 1008
20080602165807 CRAWL RESUMED - Running
           timestamp  discovered      queued   downloaded       doc/s(avg)  KB/s(avg)   dl-failures   busy-thread   mem-use-KB  heap-size-KB   congestion   max-depth   avg-depth
2008-06-02T16:58:27Z         166          43          124         6.2(6.2)   157(157)             0             0        13831         33408            1          24          14
2008-06-02T16:58:47Z         243          33          211       4.35(5.28)     27(92)             0             2        17768         34048            1          30          16
2008-06-02T16:59:07Z         250          15          236       1.25(3.93)   286(156)             0             0        15825         34304            1          15          15
20080602165920 CRAWL ENDING - Finished
2008-06-02T16:59:20Z         250           0          250       1.17(3.47)   260(174)             1             0        16529         34496            �           0           0
20080602165920 CRAWL ENDED - Finished

metadata://netarkivet.dk/crawl/logs/runtime-errors.log?heritrixVersion=1.12.1b&harvestid=2&jobid=12 127.0.1.1 20080602165806 text/plain 0

metadata://netarkivet.dk/crawl/logs/uri-errors.log?heritrixVersion=1.12.1b&harvestid=2&jobid=12 127.0.1.1 20080602165806 text/plain 0

metadata://netarkivet.dk/crawl/logs/heritrix.out?heritrixVersion=1.12.1b&harvestid=2&jobid=12 127.0.1.1 20080602165928 text/plain 11800
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
WINDOWID=65011746
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
Working directory: server2/12_1212425884349
16:58:05.853 EVENT  Starting Jetty/4.2.23
16:58:06.094 EVENT  Started WebApplicationContext[/,Heritrix Console]
16:58:06.265 EVENT  Started SocketListener on 0.0.0.0:8092
16:58:06.265 EVENT  Started org.mortbay.jetty.Server@1b000e7
06/02/2008 16:58:06 +0000 INFO org.archive.crawler.Heritrix postRegister org.archive.crawler:guiport=8092,host=david,jmxport=8093,name=Heritrix,type=CrawlService registered to MBeanServerId=david_1212425885477, SpecificationVersion=1.2 Maintenance Release, ImplementationVersion=1.5.0_15-b04, SpecificationVendor=Sun Microsystems
Heritrix version: 1.12.1b
06/02/2008 16:58:07 +0000 INFO org.archive.crawler.admin.CrawlJob postRegister org.archive.crawler:host=david,jmxport=8093,mother=Heritrix,name=12-2-20080602165806703,type=CrawlService.Job registered to MBeanServerId=david_1212425885477, SpecificationVersion=1.2 Maintenance Release, ImplementationVersion=1.5.0_15-b04, SpecificationVendor=Sun Microsystems
06/02/2008 16:59:20 +0000 INFO org.archive.crawler.admin.CrawlJob postDeregister org.archive.crawler:host=david,jmxport=8093,mother=Heritrix,name=12-2-20080602165806703,type=CrawlService.Job unregistered from MBeanServerId=david_1212425885477, SpecificationVersion=1.2 Maintenance Release, ImplementationVersion=1.5.0_15-b04, SpecificationVendor=Sun Microsystems
06/02/2008 16:59:28 +0000 INFO org.archive.crawler.Heritrix postDeregister org.archive.crawler:guiport=8092,host=david,jmxport=8093,name=Heritrix,type=CrawlService unregistered from MBeanServerId=david_1212425885477, SpecificationVersion=1.2 Maintenance Release, ImplementationVersion=1.5.0_15-b04, SpecificationVendor=Sun Microsystems
16:59:28.648 EVENT  Stopping Acceptor ServerSocket[addr=0.0.0.0/0.0.0.0,port=0,localport=8092]
16:59:28.649 EVENT  Stopped SocketListener on 0.0.0.0:8092
16:59:28.649 EVENT  Stopped WebApplicationContext[/,Heritrix Console]
16:59:28.650 EVENT  Stopped org.mortbay.http.NCSARequestLog@f6dcc7
16:59:28.650 EVENT  Stopped org.mortbay.jetty.Server@1b000e7
16:59:28.650 EVENT  Stopped WebApplicationContext[/,Heritrix Console]
16:59:28.650 EVENT  Stopped org.mortbay.jetty.Server@1b000e7

metadata://netarkivet.dk/crawl/index/cdx?majorversion=1&minorversion=0&harvestid=2&jobid=12&timestamp=20080602165808&serialno=00000 127.0.1.1 20080602165934 application/x-cdx 43978
dns:www.netarkivet.dk 212.242.40.3 20080602165807 text/dns 59 12-2-20080602165808-00000-david.arc 1352 9a754d4705b3c093206d7e01afacae6c
dns:www.kaarefc.dk 212.242.40.3 20080602165807 text/dns 54 12-2-20080602165808-00000-david.arc 1474 bc4bdef44f201a5bfef131e872c11582
http://www.kaarefc.dk/robots.txt 77.212.246.170 20080602165809 text/html 466 12-2-20080602165808-00000-david.arc 1588 1124a2c5fe1a0d5bae96d29e8f3ea70e
http://www.netarkivet.dk/robots.txt 130.225.27.144 20080602165809 text/html 521 12-2-20080602165808-00000-david.arc 2132 0b8ded4cd2d24c0efae773399b661405
http://www.kaarefc.dk/ 77.212.246.170 20080602165809 text/html 2209 12-2-20080602165808-00000-david.arc 2734 5524d2e6079d5869a77c4027f131d1f9
dns:jigsaw.w3.org 212.242.40.3 20080602165809 text/dns 93 12-2-20080602165808-00000-david.arc 5012 24fa1ea406fd43714ab4f2b082b594d0
http://www.netarkivet.dk/ 130.225.27.144 20080602165809 text/html 501 12-2-20080602165808-00000-david.arc 5164 68fb14836694b159e8be6035eed4ad70
http://www.kaarefc.dk/style.css 77.212.246.170 20080602165809 text/css 1226 12-2-20080602165808-00000-david.arc 5736 9c5423925d6b3366bafbb2f299ecd52c
http://jigsaw.w3.org/robots.txt 128.30.52.36 20080602165809 text/plain 676 12-2-20080602165808-00000-david.arc 7039 86d9e3e4890dd47495b570064a2a16f0
http://www.kaarefc.dk/private/ 77.212.246.170 20080602165810 text/html 712 12-2-20080602165808-00000-david.arc 7791 7457c38d222a86aa0837718afb2bc0cd
http://www.kaarefc.dk/mindterm 77.212.246.170 20080602165810 text/html 548 12-2-20080602165808-00000-david.arc 8579 e885bfd7ef96d486b39576ebb1461ad9
http://jigsaw.w3.org/css-validator/images/vcss 128.30.52.36 20080602165810 image/gif 1943 12-2-20080602165808-00000-david.arc 9203 3cde9b5ee1e8f827dfa86501814a2f45
http://www.kaarefc.dk/mindterm/ 77.212.246.170 20080602165810 text/html 1510 12-2-20080602165808-00000-david.arc 11237 32beccfbb8f4a8e10cdc3ad3023b4349
dns:netarkivet.dk 212.242.40.3 20080602165809 text/dns 56 12-2-20080602165808-00000-david.arc 12825 84822a4add670e0f061150796928d1d4
dns:www.w3.org 212.242.40.3 20080602165810 text/dns 214 12-2-20080602165808-00000-david.arc 12940 558ae80a71d48fab25b4f558fdcb8f9e
http://www.kaarefc.dk/wop 77.212.246.170 20080602165811 text/html 538 12-2-20080602165808-00000-david.arc 13211 1c1d750b3a37c5dfcb3442d74cdfab3b
http://www.kaarefc.dk/public/ 77.212.246.170 20080602165811 text/html 521 12-2-20080602165808-00000-david.arc 13820 94157618050b0f0554951d26ee218f27
http://www.w3.org/robots.txt 128.30.52.51 20080602165811 text/plain 1556 12-2-20080602165808-00000-david.arc 14416 ac1e9d44a00d223c952a9b86aec16fc0
http://www.kaarefc.dk/mindterm/kaarefc.dk 77.212.246.170 20080602165811 text/html 475 12-2-20080602165808-00000-david.arc 16046 78b0f82de3231ead56658ebc37292f32
http://netarkivet.dk/robots.txt 130.225.27.144 20080602165811 text/html 471 12-2-20080602165808-00000-david.arc 16608 91ec20de0ab96ed896714cfea4f82f3c
http://www.kaarefc.dk/mindterm/com.mindbright.application.MindTerm.class 77.212.246.170 20080602165811 text/html 506 12-2-20080602165808-00000-david.arc 17156 9050dcb73715c23eca65ba05bdb956ed
http://www.w3.org/Icons/valid-html401 128.30.52.51 20080602165811 image/png 2001 12-2-20080602165808-00000-david.arc 17780 c57abd58777cc159cc6a40ad68843008
http://netarkivet.dk/ 130.225.27.144 20080602165812 text/html 241 12-2-20080602165808-00000-david.arc 19863 89f6af64b53904ae9f537653180b672d
http://www.kaarefc.dk/mindterm/mindterm.jar 77.212.246.170 20080602165812 application/x-java-archive 550399 12-2-20080602165808-00000-david.arc 20171 a62b2d4bcb1fb6fd6d6f285380115603
http://jigsaw.w3.org/css-validator/images/vcss.gif 128.30.52.36 20080602165812 image/gif 1859 12-2-20080602165808-00000-david.arc 570679 1ef4f89883e0302e737f2b2cc3024201
http://netarkivet.dk/index-da.php 130.225.27.144 20080602165812 text/html 12431 12-2-20080602165808-00000-david.arc 572633 5d5646f5f1bcf5757eebc6e1520c7382
http://www.kaarefc.dk/wop/ 77.212.246.170 20080602165812 text/html 5629 12-2-20080602165808-00000-david.arc 585145 d530522923449b01a770691f25371f79
dns:www.douglasadams.com 212.242.40.3 20080602165812 text/dns 61 12-2-20080602165808-00000-david.arc 590847 c133de71acd2aeeb6adc94f82e28d7f6
http://www.w3.org/Icons/valid-html401.png 128.30.52.51 20080602165813 image/png 1913 12-2-20080602165808-00000-david.arc 590974 afdfa4f4ea656aec13c9e39437ca5454
http://netarkivet.dk/robots.txt 130.225.27.144 20080602165813 text/html 471 12-2-20080602165808-00000-david.arc 592973 f0eacb5688ba2d44a9575cd54dc6c699
http://www.douglasadams.com/robots.txt 89.16.172.251 20080602165813 text/plain 319 12-2-20080602165808-00000-david.arc 593521 ec7499e4440ef62731750f2f0d6ef6ed
http://www.kaarefc.dk/wop/wop.css 77.212.246.170 20080602165813 text/css 1811 12-2-20080602165808-00000-david.arc 593924 42d9143ad660519deb6f09c7c4ece939
http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_20.gif 130.225.27.144 20080602165813 image/gif 500 12-2-20080602165808-00000-david.arc 595814 d8b2a25da5e5cbda1bf5f50f6564742d
http://www.kaarefc.dk/wop/wop.html 77.212.246.170 20080602165813 text/html 2762 12-2-20080602165808-00000-david.arc 596433 11b1f3fd17bf7a1405a61c17511204a7
http://www.douglasadams.com/creations/0345391829.html 89.16.172.251 20080602165813 text/html 9899 12-2-20080602165808-00000-david.arc 599276 6a9a501f86860fbb243538f66c573063
http://www.kaarefc.dk/wop/submit.html 77.212.246.170 20080602165814 text/html 4240 12-2-20080602165808-00000-david.arc 609274 e065c3519dff9445860a59f969f98f39
http://netarkivet.dk/publikationer/vejledning.pdf 130.225.27.144 20080602165814 application/pdf 97048 12-2-20080602165808-00000-david.arc 613598 0b9b241bedfb1cda2605001247704d5b
http://www.kaarefc.dk/wop/thanks.html 77.212.246.170 20080602165814 text/html 4853 12-2-20080602165808-00000-david.arc 710749 7c90580a1440b500978621300f061716
http://www.douglasadams.com/images/arch_layout_09_02over.gif 89.16.172.251 20080602165814 image/gif 1646 12-2-20080602165808-00000-david.arc 715686 3733caf311529a0c6072ecee486af19d
http://www.kaarefc.dk/wop/help.html 77.212.246.170 20080602165814 text/html 4690 12-2-20080602165808-00000-david.arc 717438 50035207fc061ada172122c3a0be0a8d
http://www.douglasadams.com/creations/document.Narchlayout_03_02 89.16.172.251 20080602165814 text/html 11264 12-2-20080602165808-00000-david.arc 722210 0ee5efbed8d62c88e5cf3f01faf1da02
http://netarkivet.dk/faq/index-da.php 130.225.27.144 20080602165815 text/html 23633 12-2-20080602165808-00000-david.arc 733585 2a95be010de63fd803fdcfad4108b9b7
http://www.kaarefc.dk/wop/index.html 77.212.246.170 20080602165815 text/html 5629 12-2-20080602165808-00000-david.arc 757303 39a99541eddb6f1f1fab98252dc518f9
http://www.douglasadams.com/images/corner.gif 89.16.172.251 20080602165815 image/gif 1192 12-2-20080602165808-00000-david.arc 763015 313070973655d9bc47fed0b3cfab36c8
http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_03.gif 130.225.27.144 20080602165815 image/gif 728 12-2-20080602165808-00000-david.arc 764298 796855882e9f7c6c79150956250d9688
http://www.kaarefc.dk/wop/contact.html 77.212.246.170 20080602165815 text/html 2523 12-2-20080602165808-00000-david.arc 765145 b02db05616589732830c56a28ce18da8
http://www.douglasadams.com/images/arch_layout_00.gif 89.16.172.251 20080602165815 image/gif 289 12-2-20080602165808-00000-david.arc 767753 668bb5fde28cbe371872c2ef1812b190
http://www.kaarefc.dk/wop/wop.wav 77.212.246.170 20080602165815 audio/x-wav 179725 12-2-20080602165808-00000-david.arc 768140 ee4176cd64487dcf1c74ca3074b66515
http://www.kaarefc.dk/wop/wavs/23.wav 77.212.246.170 20080602165816 audio/x-wav 298 12-2-20080602165808-00000-david.arc 947949 b835a516deefe87e5606d699725551bf
http://www.douglasadams.com/images/arch_layout_06_02.gif 89.16.172.251 20080602165816 image/gif 1645 12-2-20080602165808-00000-david.arc 948332 4e0d7791e59b7e2b80324646448e259f
http://www.kaarefc.dk/wop/wavs/7.wav 77.212.246.170 20080602165816 audio/x-wav 27597 12-2-20080602165808-00000-david.arc 950079 6000d81dd69734c8175d545571b2c423
http://www.kaarefc.dk/wop/wavs/9.wav 77.212.246.170 20080602165816 audio/x-wav 24969 12-2-20080602165808-00000-david.arc 977762 f05aeb0a3e4df32577c9c5193e748b1a
http://www.douglasadams.com/creations/document.Narchlayout_08_02 89.16.172.251 20080602165816 text/html 11264 12-2-20080602165808-00000-david.arc 1002817 c7f46ab61df75d2b1a62fd4442f97e08
http://netarkivet.dk/publikationer/DFrevy.pdf 130.225.27.144 20080602165815 application/pdf 1030638 12-2-20080602165808-00000-david.arc 1014192 7b069dbe312e6f6762c47b227822fa54
http://www.kaarefc.dk/wop/wavs/22.wav 77.212.246.170 20080602165817 audio/x-wav 34497 12-2-20080602165808-00000-david.arc 2044931 6d1e938a6345f9a83641338f6a77f3f5
http://www.douglasadams.com/images/title2.gif 89.16.172.251 20080602165817 image/gif 1349 12-2-20080602165808-00000-david.arc 2079515 54d8ec9712f3b9d9973b09354513c9a1
http://www.kaarefc.dk/wop/wavs/3.wav 77.212.246.170 20080602165817 audio/x-wav 32645 12-2-20080602165808-00000-david.arc 2080955 41f00400d40b0b0a8caff3c779c00f6f
http://www.kaarefc.dk/wop/wavs/5.wav 77.212.246.170 20080602165817 audio/x-wav 12835 12-2-20080602165808-00000-david.arc 2113686 9f2dbca49d05e9e71ced0482e5f2eeb2
http://www.douglasadams.com/images/arch_layout_02_02over.gif 89.16.172.251 20080602165817 image/gif 1951 12-2-20080602165808-00000-david.arc 2126607 0458a66fb1fcde96732bf7ea22cea7b6
http://www.kaarefc.dk/wop/wavs/6.wav 77.212.246.170 20080602165818 audio/x-wav 116425 12-2-20080602165808-00000-david.arc 2128664 60a355fd48baedfbf102ceb81889ea80
http://netarkivet.dk/netarkivet_mouseover/billeder/netarkivet_guidelines_03.gif 130.225.27.144 20080602165818 image/gif 728 12-2-20080602165808-00000-david.arc 2245176 31b0243e06abb3a9ebef24775765ac6c
http://www.douglasadams.com/creations/0330267388.jpg 89.16.172.251 20080602165818 image/jpeg 6318 12-2-20080602165808-00000-david.arc 2246029 a16d26ae2cc784168ad519ff4b223e05
http://www.kaarefc.dk/wop/wavs/2.wav 77.212.246.170 20080602165818 audio/x-wav 45413 12-2-20080602165808-00000-david.arc 2252446 6d40997bb8fc64f3b471de9c58974de5
http://www.douglasadams.com/images/amazon.gif 89.16.172.251 20080602165818 image/gif 1587 12-2-20080602165808-00000-david.arc 2297945 d87be4301f4f753ec780fceb9e9777ae
http://www.kaarefc.dk/wop/wavs/10.wav 77.212.246.170 20080602165819 audio/x-wav 45427 12-2-20080602165808-00000-david.arc 2299623 827029e68dd8567d47f0786f1fdf9c73
http://www.douglasadams.com/images/arch_09_03.gif 89.16.172.251 20080602165819 image/gif 605 12-2-20080602165808-00000-david.arc 2345137 15c0740ff0389f0c90931b35cb425de8
http://www.kaarefc.dk/wop/wavs/1.wav 77.212.246.170 20080602165819 audio/x-wav 22189 12-2-20080602165808-00000-david.arc 2345836 a762f64184f32ee43372ae61e138f792
http://netarkivet.dk/netarkivet_alm/billeder/spacer.gif 130.225.27.144 20080602165819 image/gif 291 12-2-20080602165808-00000-david.arc 2368111 0186aff500d6ee6ffc85f11f937e3964
http://www.douglasadams.com/images/red_02_02.gif 89.16.172.251 20080602165819 image/gif 1973 12-2-20080602165808-00000-david.arc 2368503 9e799dd5788636518ea844a108dc5ab3
http://www.kaarefc.dk/wop/wavs/13.wav 77.212.246.170 20080602165819 audio/x-wav 148587 12-2-20080602165808-00000-david.arc 2370570 be6ee34637ceb3e723880dad4d1df958
http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_06.gif 130.225.27.144 20080602165819 image/gif 705 12-2-20080602165808-00000-david.arc 2519245 951dc994e246fb8894fef3fce369c6ba
http://www.kaarefc.dk/wop/wavs/0.wav 77.212.246.170 20080602165820 audio/x-wav 16257 12-2-20080602165808-00000-david.arc 2520069 f92d01481aa93719e6a933026ce77f4d
http://www.douglasadams.com/images/arch_layout_08_02over.gif 89.16.172.251 20080602165820 image/gif 1228 12-2-20080602165808-00000-david.arc 2536412 5632f7653932061db8a3176cfbff05ae
http://netarkivet.dk/presse/index-da.php 130.225.27.144 20080602165820 text/html 8995 12-2-20080602165808-00000-david.arc 2537746 e244ab5d4f3c6b9213ecce917e1cc100
http://www.kaarefc.dk/wop/wavs/19.wav 77.212.246.170 20080602165820 audio/x-wav 61237 12-2-20080602165808-00000-david.arc 2546828 92aaecae8395e528fe67c73d9e4bd5e6
http://www.douglasadams.com/images/arch_layout_07_02.gif 89.16.172.251 20080602165820 image/gif 1612 12-2-20080602165808-00000-david.arc 2608152 3ca116f164282a81afffba8e2eebcb6c
http://netarkivet.dk/netarkivet_mouseover/billeder/netarkivet_guidelines_09.gif 130.225.27.144 20080602165820 image/gif 458 12-2-20080602165808-00000-david.arc 2609866 8c22b03cac68cb033becf4af05849a66
http://www.kaarefc.dk/wop/wavs/15.wav 77.212.246.170 20080602165820 audio/x-wav 20481 12-2-20080602165808-00000-david.arc 2610449 f5466c4ade77ca800694cb785a9d78ca
http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_16.gif 130.225.27.144 20080602165820 image/gif 304 12-2-20080602165808-00000-david.arc 2631017 24da67cd626ee5b81a382b9765e433e5
http://www.douglasadams.com/creations/document.Narchlayout_05_02 89.16.172.251 20080602165820 text/html 11264 12-2-20080602165808-00000-david.arc 2631440 9adcbdc113bdb90cf11e97b6c916afc7
http://www.kaarefc.dk/wop/wavs/12.wav 77.212.246.170 20080602165821 audio/x-wav 132115 12-2-20080602165808-00000-david.arc 2642815 b15ea7fb209b5356c0f6f9e3427582f5
http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_17.gif 130.225.27.144 20080602165821 image/gif 412 12-2-20080602165808-00000-david.arc 2775018 7482be013a9f7702018d0bb4e1bf8ce3
http://www.kaarefc.dk/wop/www.spiritual-supply.com 77.212.246.170 20080602165821 text/html 484 12-2-20080602165808-00000-david.arc 2775549 cb8d72164257fc2c9a77ab220ab91b9b
http://www.douglasadams.com/creations/document.Narchlayout_02_02 89.16.172.251 20080602165821 text/html 11264 12-2-20080602165808-00000-david.arc 2776129 ccafcad363ff61a1eaa33bf99cc84afe
http://netarkivet.dk/netarkivet_mouseover/billeder/netarkivet_guidelines_04.gif 130.225.27.144 20080602165821 image/gif 551 12-2-20080602165808-00000-david.arc 2787504 3d1d5c8708bcd2aa3c90ac3083f815c7
http://www.kaarefc.dk/wop/wavs/17.wav 77.212.246.170 20080602165821 audio/x-wav 63055 12-2-20080602165808-00000-david.arc 2788180 7320830c3532ec79a0ba84ecd6f031b7
http://www.douglasadams.com/images/arch_12_08.gif 89.16.172.251 20080602165821 image/gif 1116 12-2-20080602165808-00000-david.arc 2851322 6b8a87e4ddc12b64649cdf766c0a94cc
http://netarkivet.dk/netarkivet_mouseover/billeder/netarkivet_guidelines_11.gif 130.225.27.144 20080602165821 image/gif 542 12-2-20080602165808-00000-david.arc 2852533 b28f97dd230959611f92196244b9f712
http://www.kaarefc.dk/wop/wavs/11.wav 77.212.246.170 20080602165822 audio/x-wav 12533 12-2-20080602165808-00000-david.arc 2853200 8c93b3f050fdc04bb0e78cec7648623e
http://www.douglasadams.com/images/arrow.gif 89.16.172.251 20080602165822 image/gif 375 12-2-20080602165808-00000-david.arc 2865820 707a1161f844c5455b446391314561d1
http://www.kaarefc.dk/wop/wavs/16.wav 77.212.246.170 20080602165822 audio/x-wav 40997 12-2-20080602165808-00000-david.arc 2866284 f905416c19640da42379e2384bd6cdc5
http://netarkivet.dk/kildetekster/index-da.php 130.225.27.144 20080602165822 text/html 13958 12-2-20080602165808-00000-david.arc 2907368 1effde0d1c4e27d73d82ac663474888c
http://www.douglasadams.com/images/arch_layout_03_02over.gif 89.16.172.251 20080602165822 image/gif 1232 12-2-20080602165808-00000-david.arc 2921420 22e9663494a44e15943cb8f32b2d7bd7
http://www.kaarefc.dk/wop/wavs/14.wav 77.212.246.170 20080602165822 audio/x-wav 179725 12-2-20080602165808-00000-david.arc 2922758 929f2ef48c8be1f138665a08b9ef28c2
http://netarkivet.dk/netarkivet_mouseover/billeder/netarkivet_guidelines_06.gif 130.225.27.144 20080602165822 image/gif 705 12-2-20080602165808-00000-david.arc 3102571 d60021087f98a21e64c969ab336a6bff
http://www.douglasadams.com/images/arch_layout_04_02over.gif 89.16.172.251 20080602165822 image/gif 1340 12-2-20080602165808-00000-david.arc 3103401 abf4305cb71fc069b23c7ae171c64ca5
http://netarkivet.dk/netarkivet.css 130.225.27.144 20080602165823 text/css 1577 12-2-20080602165808-00000-david.arc 3104847 e5130d33842f8994f357d030301e8e00
http://www.douglasadams.com/creations/document.Narchlayout_06_02 89.16.172.251 20080602165823 text/html 11264 12-2-20080602165808-00000-david.arc 3106505 2b83597dd99c4ca3e5579f13f4848cbf
http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_11.gif 130.225.27.144 20080602165823 image/gif 542 12-2-20080602165808-00000-david.arc 3117880 03eb9b3c45011bc2e216c6357d8536a8
http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_01.gif 130.225.27.144 20080602165823 image/gif 2538 12-2-20080602165808-00000-david.arc 3118541 64c769d99b3a5d144c878b7113ab09cb
http://www.douglasadams.com/images/arch_layout_04_02.gif 89.16.172.251 20080602165823 image/gif 1349 12-2-20080602165808-00000-david.arc 3121199 6cf982af59ad5b55f8370de025bb7a64
http://netarkivet.dk/netarkivet_mouseover/billeder/netarkivet_guidelines_05.gif 130.225.27.144 20080602165824 image/gif 498 12-2-20080602165808-00000-david.arc 3122650 139fae40f47c4cbd77425c35e3473d68
http://www.douglasadams.com/images/arch_layout_09_02.gif 89.16.172.251 20080602165824 image/gif 1651 12-2-20080602165808-00000-david.arc 3123273 ee198bb34d8f4196a3b4d2bfdaf84dd4
http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_09.gif 130.225.27.144 20080602165824 image/gif 458 12-2-20080602165808-00000-david.arc 3125026 0d294389059a64d534dd7555adff37dc
http://www.douglasadams.com/images/arch_13_09.gif 89.16.172.251 20080602165824 image/gif 398 12-2-20080602165808-00000-david.arc 3125603 1fe7bd28a1dfef3411707abb289cc03e
http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_08.gif 130.225.27.144 20080602165825 image/gif 529 12-2-20080602165808-00000-david.arc 3126095 16b4e9556137bd51c690d0545588a6d5
http://www.douglasadams.com/creations/document.Narchlayout_04_02 89.16.172.251 20080602165825 text/html 11264 12-2-20080602165808-00000-david.arc 3126743 b00f0f1125311de03a28cee3b82b893d
http://netarkivet.dk/organisation/index-da.php 130.225.27.144 20080602165825 text/html 8922 12-2-20080602165808-00000-david.arc 3138118 84b3f98bb5531da339a1a5106bd771ca
http://www.douglasadams.com/images/arch_layout_05_02.gif 89.16.172.251 20080602165825 image/gif 1807 12-2-20080602165808-00000-david.arc 3147133 eb294c7d2771e24802bb3f4c1ff0f75e
http://netarkivet.dk/nyheder/index-da.php?highlight=20070704 130.225.27.144 20080602165825 text/html 11572 12-2-20080602165808-00000-david.arc 3149042 d2d65a3120e3c51f61cb6cedc5657ce5
dns:netarchive.dk 212.242.40.3 20080602165825 text/dns 56 12-2-20080602165808-00000-david.arc 3160722 1eb9558eb50987f1e3209955e5dbde3e
http://www.douglasadams.com/images/arch_layout_07_02over.gif 89.16.172.251 20080602165825 image/gif 1601 12-2-20080602165808-00000-david.arc 3160837 a248ec6a0be2298515997afe3735ceb0
http://netarkivet.dk/nyheder/netarchivesuite.gif 130.225.27.144 20080602165826 image/gif 1375 12-2-20080602165808-00000-david.arc 3162544 d8ab771c7befbdc604129bcc8882d912
http://netarchive.dk/robots.txt 130.225.27.144 20080602165826 text/html 471 12-2-20080602165808-00000-david.arc 3164014 eb1fbca874faa81a22e656c226097d9d
http://www.douglasadams.com/images/arch_layout_05_02over.gif 89.16.172.251 20080602165826 image/gif 1792 12-2-20080602165808-00000-david.arc 3164562 4f0237161e3b36be8299907c2b3b8ee2
http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_04.gif 130.225.27.144 20080602165826 image/gif 551 12-2-20080602165808-00000-david.arc 3166460 fcb3512e643e423a7d7202422e53d497
http://www.douglasadams.com/creations/document.Narchlayout_09_02 89.16.172.251 20080602165826 text/html 11264 12-2-20080602165808-00000-david.arc 3167130 d63952f21c153178489bfce4b7cd1f48
http://netarchive.dk/suite 130.225.27.144 20080602165826 text/html 15376 12-2-20080602165808-00000-david.arc 3178505 9c6d4f56117d68d47f9cef9edfee2ea8
http://netarkivet.dk/netarkivet_mouseover/billeder/netarkivet_guidelines_07.gif 130.225.27.144 20080602165827 image/gif 657 12-2-20080602165808-00000-david.arc 3193955 db4529d21455e67a7ddb88f7693bbc88
http://www.douglasadams.com/images/creations1.gif 89.16.172.251 20080602165827 image/gif 2076 12-2-20080602165808-00000-david.arc 3194737 d95ebc91f0fb6117237f83d301cc72aa
http://netarkivet.dk/nyheder/index-da.php?highlight=20041222 130.225.27.144 20080602165827 text/html 11572 12-2-20080602165808-00000-david.arc 3196908 739d24794c5c62454a10cad2d494257d
http://www.douglasadams.com/creations/document.Narchlayout_07_02 89.16.172.251 20080602165827 text/html 11264 12-2-20080602165808-00000-david.arc 3208588 6e5ce07f84bcaa93043d80e71a7b9234
http://netarchive.dk/suite/Welcome?action=AttachFile&do=view&target=transparent_logo.png 130.225.27.144 20080602165827 text/html 10516 12-2-20080602165808-00000-david.arc 3219963 8f49a3856f22beeb7c0df9691a7758ea
http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_02.gif 130.225.27.144 20080602165827 image/gif 483 12-2-20080602165808-00000-david.arc 3230615 231adc654048a700cfbbb911af8eb201
http://www.douglasadams.com/images/arch_layout_08_02.gif 89.16.172.251 20080602165828 image/gif 1210 12-2-20080602165808-00000-david.arc 3231217 379257b4f8001c91db80f199c2a6d561
http://netarkivet.dk/netarkivet_mouseover/billeder/netarkivet_guidelines_08.gif 130.225.27.144 20080602165828 image/gif 529 12-2-20080602165808-00000-david.arc 3232529 ac83c56eac8af5e5619efe3b210980d4
http://netarchive.dk/wiki/modern/css/projection.css 130.225.27.144 20080602165828 text/css 837 12-2-20080602165808-00000-david.arc 3233183 aa21582634eef1dc64f47789928169d8
http://www.douglasadams.com/images/arch_layout_06_02over.gif 89.16.172.251 20080602165828 image/gif 1675 12-2-20080602165808-00000-david.arc 3234116 2a33a96e922ee530c55fff5e74f58b14
http://netarkivet.dk/publikationer/index-da.php 130.225.27.144 20080602165828 text/html 17853 12-2-20080602165808-00000-david.arc 3235897 bae4b2ebdc213bbb27a15589759ebaf5
http://netarchive.dk/suite/Welcome 130.225.27.144 20080602165828 text/html 15376 12-2-20080602165808-00000-david.arc 3253845 04d2c1d380473d97cac936795f13b250
http://www.douglasadams.com/images/creations2.gif 89.16.172.251 20080602165828 image/gif 6202 12-2-20080602165808-00000-david.arc 3269303 eedb92be96d3e39ec6a2342f0e7cd22d
http://netarkivet.dk/publikationer/statsbiblioteket.dk 130.225.27.144 20080602165829 text/html 494 12-2-20080602165808-00000-david.arc 3275600 29b6d0499373f57c3a08d1d8e4addcc0
http://netarkivet.dk/publikationer/statsbiblitoeket.dk 130.225.27.144 20080602165829 text/html 494 12-2-20080602165808-00000-david.arc 3276194 8543126a6ef758b836e3661c0f072d58
http://www.douglasadams.com/images/arch_layout_03_02.gif 89.16.172.251 20080602165829 image/gif 1235 12-2-20080602165808-00000-david.arc 3276788 26911d83bc566edfafd54e304274fef5
http://netarkivet.dk/faq 130.225.27.144 20080602165829 text/html 541 12-2-20080602165808-00000-david.arc 3278125 1572fc619d15d3d4b9e169fbb577391d
http://netarchive.dk/suite/TitleIndex 130.225.27.144 20080602165829 text/html 32853 12-2-20080602165808-00000-david.arc 3278736 8de28936321851056f74cd1704f4b0b2
http://netarkivet.dk/faq/ 130.225.27.144 20080602165830 text/html 241 12-2-20080602165808-00000-david.arc 3311674 d3833090321c58f48bd95971fe3af2fd
http://netarkivet.dk/presse/index-da.php?highlight=20060705 130.225.27.144 20080602165830 text/html 9069 12-2-20080602165808-00000-david.arc 3311986 51478b1329e4ca430a7a42d3f65145e6
http://netarchive.dk/suite/Welcome?action=raw 130.225.27.144 20080602165830 text/plain 1815 12-2-20080602165808-00000-david.arc 3321161 786a59256bf51255009259cdccd1d6ec
http://netarkivet.dk/nyheder/index-da.php?highlight=20070330 130.225.27.144 20080602165830 text/html 11572 12-2-20080602165808-00000-david.arc 3323069 09cbe306abd480043dee5d25ac655596
http://netarchive.dk/wiki/common/js/common.js 130.225.27.144 20080602165831 application/x-javascript 4159 12-2-20080602165808-00000-david.arc 3334749 050adcdd785143408748142b722bba7e
http://netarkivet.dk/nyheder/index-da.php 130.225.27.144 20080602165831 text/html 11498 12-2-20080602165808-00000-david.arc 3339015 69432fd7b821104ba7f36790cbc9e295
http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_13.gif 130.225.27.144 20080602165831 image/gif 1928 12-2-20080602165808-00000-david.arc 3350602 056b1a4c13e5a4740f3d00a4ba617a35
http://netarkivet.dk/forslag 130.225.27.144 20080602165832 text/html 549 12-2-20080602165808-00000-david.arc 3352650 1cc0b659d434951679c7cd43522b31da
http://netarchive.dk/suite/WordIndex 130.225.27.144 20080602165831 text/html 54291 12-2-20080602165808-00000-david.arc 3353273 b32b934b98701f77f187b0db7a2be773
http://netarkivet.dk/forslag/ 130.225.27.144 20080602165832 text/html 241 12-2-20080602165808-00000-david.arc 3407648 5877a165923c451c5be8c60e29587403
http://netarkivet.dk/forslag/index-da.php 130.225.27.144 20080602165832 text/html 12801 12-2-20080602165808-00000-david.arc 3407964 5f541b7660ac158e642013a9f4e06388
http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_12.gif 130.225.27.144 20080602165833 image/gif 318 12-2-20080602165808-00000-david.arc 3420854 ec36aebaedaaae44bea6c0beb89ae9cd
http://netarchive.dk/suite/Welcome?action=AttachFile&do=view&target=netarkivet.gif 130.225.27.144 20080602165833 text/html 10486 12-2-20080602165808-00000-david.arc 3421291 65f69b63969a84fb0f5d51bac58b44fb
http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_05.gif 130.225.27.144 20080602165833 image/gif 498 12-2-20080602165808-00000-david.arc 3431907 0e98d0c20821c6f94b67e8c5135f9818
http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_10.gif 130.225.27.144 20080602165834 image/gif 324 12-2-20080602165808-00000-david.arc 3432524 8b9ea155e7c1efac3069bc3c6eaf4959
http://netarchive.dk/wiki/modern/css/print.css 130.225.27.144 20080602165834 text/css 1025 12-2-20080602165808-00000-david.arc 3432967 ad5459f902d9db772546e9b768492d47
http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_07.gif 130.225.27.144 20080602165834 image/gif 657 12-2-20080602165808-00000-david.arc 3434084 445e6d5046dcbba90cbcf80c7b1ccc2e
http://netarchive.dk/wiki/modern/css/screen.css 130.225.27.144 20080602165834 text/css 7809 12-2-20080602165808-00000-david.arc 3434860 4bbb2f93a0a193d68f18c792103c3855
http://netarkivet.dk/index-en.php 130.225.27.144 20080602165834 text/html 11712 12-2-20080602165808-00000-david.arc 3442762 7bfd747917008e153bb3895945be8791
http://netarchive.dk/wiki/modern/css/common.css 130.225.27.144 20080602165835 text/css 7300 12-2-20080602165808-00000-david.arc 3454555 786755768cc61a196b7b15ee78e19b47
http://netarkivet.dk/netarchive_alm/billeder/spacer.gif 130.225.27.144 20080602165835 image/gif 291 12-2-20080602165808-00000-david.arc 3461948 d545765d593342c5bc33e2d04e52436e
http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_13.gif 130.225.27.144 20080602165835 image/gif 1984 12-2-20080602165808-00000-david.arc 3462340 bc8bef347e3fad66c381928288a9869c
http://netarchive.dk/suite/HelpOnFormatting 130.225.27.144 20080602165835 text/html 22156 12-2-20080602165808-00000-david.arc 3464444 af1cbdbcb7cc675dce5a7882318c8a5e
http://netarkivet.dk/netarchive_mouseover/billeder/netarkivet_guidelines_07.gif 130.225.27.144 20080602165836 image/gif 678 12-2-20080602165808-00000-david.arc 3486691 7d309742fbfeb9a1e40a437a8a229a5a
http://netarchive.dk/suite/Welcome?action=AttachFile&do=get&target=netarkivet.gif 130.225.27.144 20080602165836 image/gif 1422 12-2-20080602165808-00000-david.arc 3487494 2402a84544b364bb8b6c590961fe6bbd
http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_20.gif 130.225.27.144 20080602165836 image/gif 500 12-2-20080602165808-00000-david.arc 3489044 f66889474679575a4c40be89337ad630
http://netarkivet.dk/netarchive_mouseover/billeder/netarkivet_guidelines_06.gif 130.225.27.144 20080602165836 image/gif 690 12-2-20080602165808-00000-david.arc 3489663 af175434d22281c024da0838e945be6b
http://netarchive.dk/suite/FindPage 130.225.27.144 20080602165836 text/html 12570 12-2-20080602165808-00000-david.arc 3490478 cb3ee761a768b296f320f609ed0aa9e3
http://netarkivet.dk/netarchive_mouseover/billeder/netarkivet_guidelines_09.gif 130.225.27.144 20080602165837 image/gif 458 12-2-20080602165808-00000-david.arc 3503131 c30de2e56fce4605b0f38642de3cc262
http://netarkivet.dk/netarchive_mouseover/billeder/netarkivet_guidelines_03.gif 130.225.27.144 20080602165837 image/gif 728 12-2-20080602165808-00000-david.arc 3503714 038896f57d79b571a380f91f808e5ef6
http://netarchive.dk/suite/Welcome?action=print 130.225.27.144 20080602165837 text/html 7559 12-2-20080602165808-00000-david.arc 3504567 e47f719839d8a17e251caa8acdfff556
http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_12.gif 130.225.27.144 20080602165837 image/gif 318 12-2-20080602165808-00000-david.arc 3512220 6b4ff95c676c401a2453bb03fdddf431
http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_14.gif 130.225.27.144 20080602165838 image/gif 453 12-2-20080602165808-00000-david.arc 3512657 b50ae622df756c4488580cb93d187d23
http://netarchive.dk/suite/Welcome?action=AttachFile&do=get&target=transparent_logo.png 130.225.27.144 20080602165838 image/png 9450 12-2-20080602165808-00000-david.arc 3513229 4c1b03a2b94a4ef431e5bec04ca6486e
http://netarkivet.dk/netarchive_mouseover/billeder/netarkivet_guidelines_05.gif 130.225.27.144 20080602165838 image/gif 491 12-2-20080602165808-00000-david.arc 3522813 96862fdee3280aa47c93deeb7baecab5
http://netarchive.dk/wiki/modern/img/moin-attach.png 130.225.27.144 20080602165838 image/png 428 12-2-20080602165808-00000-david.arc 3523429 303c73b437371bb006d77e2857b5f9e6
http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_01.gif 130.225.27.144 20080602165838 image/gif 2538 12-2-20080602165808-00000-david.arc 3523955 6d002dcd519a450abffe1bfff5566496
http://netarkivet.dk/netarchive_mouseover/billeder/netarkivet_guidelines_11.gif 130.225.27.144 20080602165839 image/gif 526 12-2-20080602165808-00000-david.arc 3526613 ac21b8556b56b20e99c6166ef164eb73
http://netarchive.dk/suite/TitleIndex?action=print 130.225.27.144 20080602165838 text/html 25009 12-2-20080602165808-00000-david.arc 3527264 eb7900f4527695c4e5df81341bc94cd2
http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_16.gif 130.225.27.144 20080602165839 image/gif 304 12-2-20080602165808-00000-david.arc 3552371 099e3755bb0979bc684fa91b76f3363d
http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_19.gif 130.225.27.144 20080602165840 image/gif 545 12-2-20080602165808-00000-david.arc 3552794 a86bf796521b92c4254ceb6e01e26032
http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_06.gif 130.225.27.144 20080602165840 image/gif 690 12-2-20080602165808-00000-david.arc 3553458 159f776b436533d50c2dfe55b0364515
http://netarchive.dk/suite/TitleIndex?action=raw 130.225.27.144 20080602165840 text/plain 715 12-2-20080602165808-00000-david.arc 3554267 686f9b41fc6afc561650ccfc15a3701c
http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_17.gif 130.225.27.144 20080602165840 image/gif 412 12-2-20080602165808-00000-david.arc 3555077 e10996e582fbb6b4b9c30194e854bac0
http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_08.gif 130.225.27.144 20080602165841 image/gif 523 12-2-20080602165808-00000-david.arc 3555608 8776203dca1480ae717931810ea88a35
http://netarkivet.dk/netarchive_mouseover/billeder/netarkivet_guidelines_08.gif 130.225.27.144 20080602165841 image/gif 523 12-2-20080602165808-00000-david.arc 3556250 40577fed9c447fb88c10f0debed44e0d
http://netarchive.dk/suite/WordIndex?action=print 130.225.27.144 20080602165840 text/html 46454 12-2-20080602165808-00000-david.arc 3556898 7c0883a172d467acc69d76628991b4dd
http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_03.gif 130.225.27.144 20080602165841 image/gif 728 12-2-20080602165808-00000-david.arc 3603449 83f368c8a336111f6411d3a3ab0e32d3
http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_10.gif 130.225.27.144 20080602165842 image/gif 324 12-2-20080602165808-00000-david.arc 3604296 24e9741a345c2e6af60535b225e7b629
http://netarkivet.dk/netarchive_mouseover/billeder/netarkivet_guidelines_04.gif 130.225.27.144 20080602165842 image/gif 515 12-2-20080602165808-00000-david.arc 3604739 c740986e615f443447bbe3ad15df69a9
http://netarchive.dk/suite/WordIndex?action=raw 130.225.27.144 20080602165842 text/plain 689 12-2-20080602165808-00000-david.arc 3605379 38755e9e7aa10d9282e8446a55869ed0
http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_07.gif 130.225.27.144 20080602165842 image/gif 678 12-2-20080602165808-00000-david.arc 3606162 a4eca8d798cf1553b999d77b94d9c4ca
http://netarchive.dk/wiki/modern/img/tab-wiki.png 130.225.27.144 20080602165842 text/html 489 12-2-20080602165808-00000-david.arc 3606959 7e1e3778a2de2b86f39de5a78715c101
http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_04.gif 130.225.27.144 20080602165843 image/gif 515 12-2-20080602165808-00000-david.arc 3607543 bacf749b35edd51885c8323ac3ab1f67
http://netarchive.dk/wiki/modern/img/draft.png 130.225.27.144 20080602165843 image/png 9970 12-2-20080602165808-00000-david.arc 3608177 a24ab176e62d0cec48485cb0e40d32b5
http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_09.gif 130.225.27.144 20080602165843 image/gif 458 12-2-20080602165808-00000-david.arc 3618240 b7a99a2715133fca4710c2c853cd6e77
http://netarchive.dk/wiki/modern/img/tab-selected.png 130.225.27.144 20080602165843 text/html 493 12-2-20080602165808-00000-david.arc 3618817 38898fa7b7b65015e28396a4c27591cc
http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_02.gif 130.225.27.144 20080602165844 image/gif 483 12-2-20080602165808-00000-david.arc 3619409 6b8f5a8e4f8b5d17e0db788b18754331
http://netarchive.dk/wiki/modern/img/tab-user.png 130.225.27.144 20080602165844 text/html 489 12-2-20080602165808-00000-david.arc 3620011 bbb45f9a51808aebc9c1b07265cdc702
http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_11.gif 130.225.27.144 20080602165844 image/gif 526 12-2-20080602165808-00000-david.arc 3620595 4a98fcb2a1e1fbc563bdad41cb6f601e
http://netarchive.dk/wiki/modern/img/moin-inter.png 130.225.27.144 20080602165844 image/png 464 12-2-20080602165808-00000-david.arc 3621240 530adf2e34a2af1a0ab0aad05b62208e
http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_05.gif 130.225.27.144 20080602165844 image/gif 491 12-2-20080602165808-00000-david.arc 3621801 d19eb1a35f9d34691a6c05fc4bbebdef
http://netarchive.dk/wiki/modern/img/moin-ftp.png 130.225.27.144 20080602165844 image/png 523 12-2-20080602165808-00000-david.arc 3622411 a3f7c0c46d268e959bee8639c72bd16a
http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_14.gif 130.225.27.144 20080602165845 image/gif 453 12-2-20080602165808-00000-david.arc 3623029 f296e65cb9c1ea2f6d65b6fe4ce8ec19
http://netarchive.dk/wiki/modern/img/moin-news.png 130.225.27.144 20080602165845 image/png 439 12-2-20080602165808-00000-david.arc 3623601 bf4d7936d10b86c45df3ac15708f3301
http://netarkivet.dk/links/index-da.php 130.225.27.144 20080602165845 text/html 10545 12-2-20080602165808-00000-david.arc 3624136 a8ae2580b2180f60017d82167ea5515e
http://netarchive.dk/wiki/modern/img/moin-email.png 130.225.27.144 20080602165845 image/png 409 12-2-20080602165808-00000-david.arc 3634768 a405981cd5ac5562d4b50e4f019b89a0
http://netarchive.dk/wiki/modern/img/moin-telnet.png 130.225.27.144 20080602165845 image/png 439 12-2-20080602165808-00000-david.arc 3635274 94bfdd0e21e4cdaa16879c46f8c37eec
http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_19.gif 130.225.27.144 20080602165845 image/gif 545 12-2-20080602165808-00000-david.arc 3635811 b3edff280ab7aad9490ddfec4a0b2d56
http://netarchive.dk/wiki/modern/img/attention.png 130.225.27.144 20080602165846 image/png 414 12-2-20080602165808-00000-david.arc 3636475 289a70f2d51f5825ba5f93423aa8a79c
http://netarkivet.dk/presse/Pressemeddelelse_juli2006.pdf 130.225.27.144 20080602165846 application/pdf 41220 12-2-20080602165808-00000-david.arc 3636985 64b6df3f8f1b0605ca74801d501ae45c
http://netarchive.dk/wiki/modern/img/moin-www.png 130.225.27.144 20080602165846 image/png 400 12-2-20080602165808-00000-david.arc 3678316 d610c46a8efa7e9d805e9604df32dde9
http://netarkivet.dk/presse/Bryllup-20040706.pdf 130.225.27.144 20080602165846 application/pdf 34252 12-2-20080602165808-00000-david.arc 3678811 64a9258c9c55c23447934a75a2ec5859
http://netarchive.dk/suite/HelpOnFormatting?action=print 130.225.27.144 20080602165847 text/html 14256 12-2-20080602165808-00000-david.arc 3713165 02837f16833e7018fffbe97e22aace50
http://netarkivet.dk/presse/nethoestning_KUM_pressemed.pdf 130.225.27.144 20080602165847 application/pdf 65963 12-2-20080602165808-00000-david.arc 3727525 80b1a51360bf80f9a40e7c422e4cf20d
http://netarchive.dk/suite/HelpOnFormatting?action=raw 130.225.27.144 20080602165847 text/plain 3569 12-2-20080602165808-00000-david.arc 3793600 50e7a090663ea2a983ca419102c07af6
http://netarkivet.dk/kildetekster/ProxyViewer-0.1.tar.gz 130.225.27.144 20080602165847 application/x-gzip 144147 12-2-20080602165808-00000-david.arc 3797271 b0862c9dac5037781db10ae1a6b44320
http://netarkivet.dk/kildetekster/JavaArcUtils-0.3.tar.gz 130.225.27.144 20080602165848 application/x-gzip 13565 12-2-20080602165808-00000-david.arc 3941532 a681e37f3320e251f5d23a4596cdbebf
http://netarchive.dk/suite/FindPage?action=raw 130.225.27.144 20080602165848 text/plain 1272 12-2-20080602165808-00000-david.arc 3955211 72eb13feb844157e3a72c06562b299a7
http://netarkivet.dk/publikationer/index-da.php?highlight=20060519 130.225.27.144 20080602165848 text/html 17927 12-2-20080602165808-00000-david.arc 3956577 e89b3df62415e850fa8c1ecf5aff59d2
http://netarchive.dk/suite/FindPage?action=print 130.225.27.144 20080602165848 text/html 4799 12-2-20080602165808-00000-david.arc 3974618 d0d95d1dbc33fff374efddf379b0f2f5
http://netarkivet.dk/nyheder/Newsletter_Netarchive_dk_march2007.pdf 130.225.27.144 20080602165849 application/pdf 647268 12-2-20080602165808-00000-david.arc 3979512 b41e331b04009c51e18f434601378650
http://netarkivet.dk/nyheder/Newsletter_Netarchive_dk_august2006.pdf 130.225.27.144 20080602165850 application/pdf 389225 12-2-20080602165808-00000-david.arc 4626902 efe96707a88c4a3dcdb8ff98adccf14d
http://netarkivet.dk/publikationer/ECDL2001-bnh-08092001.ppt 130.225.27.144 20080602165852 application/vnd.ms-powerpoint 723731 12-2-20080602165808-00000-david.arc 5016250 731682cf6b6a5a7a562e548b571bc199
http://netarkivet.dk/publikationer/iwaw05-christensen.pdf 130.225.27.144 20080602165854 application/pdf 481449 12-2-20080602165808-00000-david.arc 5740110 b993ef52ff24e768d9d94c38ca87a55b
http://netarkivet.dk/publikationer/webarkivering-webarchiving.pdf 130.225.27.144 20080602165855 application/pdf 929105 12-2-20080602165808-00000-david.arc 6221671 9289e488cbef7f3f46d4eb674436e6db
http://netarkivet.dk/publikationer/Etags-2004.pdf 130.225.27.144 20080602165857 application/pdf 203235 12-2-20080602165808-00000-david.arc 7150896 13128b764ee5020208b0e148624ba359
http://netarkivet.dk/publikationer/webark-final-rapport-2003.pdf 130.225.27.144 20080602165858 application/pdf 788122 12-2-20080602165808-00000-david.arc 7354235 bba24b1e99d33a8e3cce7636f0b269d7
http://netarkivet.dk/publikationer/FormatRepositories-2004.pdf 130.225.27.144 20080602165900 application/pdf 216974 12-2-20080602165808-00000-david.arc 8142476 b9e4b45ae99f710df19fdfc02c5e403f
http://netarkivet.dk/publikationer/FileFormats-2004.pdf 130.225.27.144 20080602165901 application/pdf 340770 12-2-20080602165808-00000-david.arc 8359567 8c0f13693e661be12ac0d4c2ed98f57a
http://netarkivet.dk/publikationer/iwaw06-clausen.pdf 130.225.27.144 20080602165902 application/pdf 78347 12-2-20080602165808-00000-david.arc 8700447 e618d5be3544bd4f2f9c014c4a6ec3ae
http://netarkivet.dk/publikationer/Archival_format_requirements-2004.pdf 130.225.27.144 20080602165902 application/pdf 430135 12-2-20080602165808-00000-david.arc 8778901 1a019d8fd707694a39bbd7874d4e3d02
http://netarkivet.dk/publikationer/Kanon-Juli05.pdf 130.225.27.144 20080602165903 application/pdf 284819 12-2-20080602165808-00000-david.arc 9209163 360bb0fe88c4b0412c677f79aed2f58b
http://netarkivet.dk/publikationer/nhc-kb-dk-msst2006.pdf 130.225.27.144 20080602165904 application/pdf 120852 12-2-20080602165808-00000-david.arc 9494088 8ac0d3774139759565c8e81187ec95cd
http://netarkivet.dk/forslag/domtester.php 130.225.27.144 20080602165905 text/html 8610 12-2-20080602165808-00000-david.arc 9615052 49075a658989a05b648d8168dc72c583
http://netarkivet.dk/organisation/index-en.php 130.225.27.144 20080602165905 text/html 8901 12-2-20080602165808-00000-david.arc 9623751 d19f98ba6d823eb81538cf12506e03ec
http://netarkivet.dk/nyheder/index-en.php?highlight=20070704 130.225.27.144 20080602165906 text/html 11144 12-2-20080602165808-00000-david.arc 9632745 a7b70b6ef8d03073db17f7ed601cc40b
http://netarkivet.dk/publikationer/index-en.php?highlight=20071102 130.225.27.144 20080602165906 text/html 19694 12-2-20080602165808-00000-david.arc 9643997 02b944915bc10b00ec90080920b51db2
http://netarkivet.dk/publikationer/kb.dk 130.225.27.144 20080602165907 text/html 480 12-2-20080602165808-00000-david.arc 9663805 ac56f7e6564759ca40a911f4258b45de
http://netarkivet.dk/publikationer/netarkivet.dk 130.225.27.144 20080602165907 text/html 488 12-2-20080602165808-00000-david.arc 9664371 6f8cdfc1fc3653e791eab43b066d2884
http://netarkivet.dk/nyheder/index-en.php 130.225.27.144 20080602165907 text/html 11070 12-2-20080602165808-00000-david.arc 9664953 839759f410e6a8cf3eecc8859a17cc83
http://netarkivet.dk/kildetekster/index-en.php 130.225.27.144 20080602165908 text/html 12881 12-2-20080602165808-00000-david.arc 9676112 448c1e21b6a47d4c8e48f5fedbca5b37
http://netarkivet.dk/faq/index-en.php 130.225.27.144 20080602165908 text/html 8421 12-2-20080602165808-00000-david.arc 9689087 614501e19304de80c0310f1dbadcafcc
http://netarkivet.dk/publikationer/index-en.php 130.225.27.144 20080602165909 text/html 19620 12-2-20080602165808-00000-david.arc 9697592 67f0eb0e74e31ecb8b581204e12fe343
http://netarkivet.dk/nyheder/index-en.php?highlight=20070330 130.225.27.144 20080602165909 text/html 11070 12-2-20080602165808-00000-david.arc 9717307 2ed156ec54c596d881522138822d73a7
http://netarkivet.dk/publikationer/index-en.php?highlight=20070918 130.225.27.144 20080602165910 text/html 19694 12-2-20080602165808-00000-david.arc 9728485 02389d15886efab8149e95b5da82ed65
http://netarkivet.dk/links/index-en.php 130.225.27.144 20080602165910 text/html 10287 12-2-20080602165808-00000-david.arc 9748293 4a304114a129dcfe6df784727fa26371
http://netarkivet.dk/publikationer/index-en.php?highlight=20070502 130.225.27.144 20080602165911 text/html 19694 12-2-20080602165808-00000-david.arc 9758667 bc909db4d63aa7fa92551b69b4e51f53
http://netarkivet.dk/publikationer/DFrevy_english.pdf 130.225.27.144 20080602165911 application/pdf 932146 12-2-20080602165808-00000-david.arc 9778475 4bc0bffef7d4b41b817b9b92f3d0320f
http://netarkivet.dk/presse/index-en.php 130.225.27.144 20080602165913 text/html 8937 12-2-20080602165808-00000-david.arc 10710729 ae0e9ba547140d2255ae64b87e936b66
http://netarkivet.dk/publikationer/InteroperabilityInTheFuture_IFLA2007.pdf 130.225.27.144 20080602165914 application/pdf 109474 12-2-20080602165808-00000-david.arc 10719753 f679dbfa89692c6783597dca4cf396ba
http://netarkivet.dk/publikationer/CollectingTheDanishInternet_2007.pdf 130.225.27.144 20080602165915 application/pdf 747927 12-2-20080602165808-00000-david.arc 10829357 2609735fc51fea5c86778a50def2ead7
http://netarkivet.dk/publikationer/Event-definition-final.pdf 130.225.27.144 20080602165917 application/pdf 454377 12-2-20080602165808-00000-david.arc 11577410 64964ce13fc64b8b7564e4ddb9b567fe
http://netarkivet.dk/publikationer/IntegrationOfNonHarvestedData.pdf 130.225.27.144 20080602165918 application/pdf 835627 12-2-20080602165808-00000-david.arc 12031903 043994d505d9d8955c498f62ddd61793


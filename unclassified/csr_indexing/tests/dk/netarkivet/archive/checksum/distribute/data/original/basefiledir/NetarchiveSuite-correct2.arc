filedesc://1-metadata-1.arc 0.0.0.0 20090514142608 text/plain 77
1 0 InternetArchive
URL IP-address Archive-date Content-type Archive-length

metadata://netarkivet.dk/crawl/setup/order.xml?heritrixVersion=1.14.3&harvestid=1&jobid=1 130.226.228.8 20090514142057 text/xml 18032
<?xml version="1.0" encoding="UTF-8"?>

<crawl-order xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="heritrix_settings.xsd">
  <meta>
    <name>3levels_orderxml</name>
    <description>default orderxml with max-hops set to 3</description>
    <operator>Admin</operator>
    <organization/>
    <audience>TESTERS</audience>
    <date>20080118111217</date>
  </meta>
  <controller>
    <string name="settings-directory">settings</string>
    <string name="disk-path">/home/test/SPIL/harvester_high/1_1242310856461</string>
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
      <string name="seedsfile">/home/test/SPIL/harvester_high/1_1242310856461/seeds.txt</string>
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
            <integer name="max-hops">3</integer>
          </newObject>
          <newObject name="rejectIfPathological" class="org.archive.crawler.deciderules.PathologicalPathDecideRule">
            <integer name="max-repetitions">3</integer>
          </newObject>
          <newObject name="acceptIfTranscluded" class="org.archive.crawler.deciderules.TransclusionDecideRule">
            <integer name="max-trans-hops">3</integer>
            <integer name="max-speculative-hops">1</integer>
          </newObject>
          <newObject name="pathdepthfilter" class="org.archive.crawler.deciderules.TooManyPathSegmentsDecideRule">
            <integer name="max-path-depth">20</integer>
          </newObject>
          <newObject name="webbyen" class="org.archive.crawler.deciderules.MatchesRegExpDecideRule">
            <string name="decision">REJECT</string>
            <string name="regexp">.*webbyen.*kontakt\.asp.*</string>
          </newObject>
          <newObject name="dr_dk" class="org.archive.crawler.deciderules.MatchesRegExpDecideRule">
            <string name="decision">REJECT</string>
            <string name="regexp">.*dr\.dk.*epg\.asp.*</string>
          </newObject>
          <newObject name="stiften" class="org.archive.crawler.deciderules.MatchesRegExpDecideRule">
            <string name="decision">REJECT</string>
            <string name="regexp">.*stiften.*adstream_mjx\.ads.*</string>
          </newObject>
          <newObject name="halibut_dk" class="org.archive.crawler.deciderules.MatchesRegExpDecideRule">
            <string name="decision">REJECT</string>
            <string name="regexp">.*halibut\.dk\/cgi-bin.*</string>
          </newObject>
          <newObject name="cybercomputer_dk" class="org.archive.crawler.deciderules.MatchesRegExpDecideRule">
            <string name="decision">REJECT</string>
            <string name="regexp">.*cybercomputer\.dk\/putikurv.*</string>
          </newObject>
          <newObject name="tawselovers_dk" class="org.archive.crawler.deciderules.MatchesRegExpDecideRule">
            <string name="decision">REJECT</string>
            <string name="regexp">.*tawselovers.*action=buy_now.*</string>
          </newObject>
        </map>
        <!-- end rules -->
      </newObject>
      <!-- end decide-rules -->
    </newObject>
    <!-- End DecidingScope -->
    <map name="http-headers">
      <string name="user-agent">Mozilla/5.0 (compatible; heritrix/1.5.0-200506132127 +http://netarkivet.dk/website/info.html)</string>
      <string name="from">netarkivet-svar@netarkivet.dk</string>
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
        <long name="group-max-all-kb">97657</long>
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
        <string name="index-location">/home/test/SPIL/cache/DEDUP_CRAWL_LOG/empty-cache</string>
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
        <string name="prefix">1-1</string>
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
        <boolean name="seed-redirects-new-seed">true</boolean>
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

metadata://netarkivet.dk/crawl/setup/harvestInfo.xml?heritrixVersion=1.14.3&harvestid=1&jobid=1 130.226.228.8 20090514142056 text/xml 380
<?xml version="1.0" encoding="UTF-8"?>

<harvestInfo>
  <version>0.2</version>
  <jobId>1</jobId>
  <priority>HIGHPRIORITY</priority>
  <harvestNum>0</harvestNum>
  <origHarvestDefinitionID>1</origHarvestDefinitionID>
  <maxBytesPerDomain>100000000</maxBytesPerDomain>
  <maxObjectsPerDomain>-1</maxObjectsPerDomain>
  <orderXMLName>3levels_orderxml</orderXMLName>
</harvestInfo>

metadata://netarkivet.dk/crawl/setup/seeds.txt?heritrixVersion=1.14.3&harvestid=1&jobid=1 130.226.228.8 20090514142056 text/plain 80
http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm
metadata://netarkivet.dk/crawl/reports/crawl-report.txt?heritrixVersion=1.14.3&harvestid=1&jobid=1 130.226.228.8 20090514142601 text/plain 312
Crawl Name: 3levels_orderxml
Crawl Status: Finished
Duration Time: 5m1s87ms
Total Seeds Crawled: 1
Total Seeds not Crawled: 0
Total Hosts Crawled: 11
Total Documents Crawled: 612
Processed docs/sec: 2.02
Bandwidth in Kbytes/sec: 289
Total Raw Data Size in Bytes: 89284048 (85 MB) 
Novel Bytes: 89284048 (85 MB) 

metadata://netarkivet.dk/crawl/reports/frontier-report.txt?heritrixVersion=1.14.3&harvestid=1&jobid=1 130.226.228.8 20090514142601 text/plain 15
frontier empty

metadata://netarkivet.dk/crawl/reports/hosts-report.txt?heritrixVersion=1.14.3&harvestid=1&jobid=1 130.226.228.8 20090514142601 text/plain 483
[#urls] [#bytes] [host] [#robots] [#remaining]
292 43598631 carlsencards.com 0 0 
282 43711183 www.carlsencards.com 0 0 
12 1366 dns: 0 0 
4 32561 pagead2.googlesyndication.com 0 0 
3 1053 googleads.g.doubleclick.net 0 0 
3 14684 partner.googleadservices.com 0 0 
3 10797 s9.addthis.com 0 0 
2 560 ad.yieldmanager.com 0 0 
2 483 download.macromedia.com 0 0 
2 1884487 fpdownload2.macromedia.com 0 0 
2 27095 www.google-analytics.com 0 0 
2 1148 www.macromedia.com 0 0 
0 0 dns: 0 0 

metadata://netarkivet.dk/crawl/reports/mimetype-report.txt?heritrixVersion=1.14.3&harvestid=1&jobid=1 130.226.228.8 20090514142601 text/plain 317
[#urls] [#bytes] [mime-types]
214 2605825 text/html
148 80729460 application/x-shockwave-flash
130 3775390 image/jpeg
83 160967 image/gif
12 1366 text/dns
11 6832 text/plain
4 51094 application/x-javascript
2 6436 text/css
2 54427 text/javascript
1 1884214 application/x-cab-compressed
1 7825 image/png
1 212 no-type

metadata://netarkivet.dk/crawl/reports/processors-report.txt?heritrixVersion=1.14.3&harvestid=1&jobid=1 130.226.228.8 20090514142601 text/plain 2077
Processors report - 200905141426
  Job being crawled:    3levels_orderxml
  Number of Processors: 16
  NOTE: Some processors may not return a report!

Processor: org.archive.crawler.fetcher.FetchHTTP
  Function:          Fetch HTTP URIs
  CrawlURIs handled: 597
  Recovery retries:   0

Processor: org.archive.crawler.extractor.ExtractorHTTP
  Function:          Extracts URIs from HTTP response headers
  CrawlURIs handled: 597
  Links extracted:   5

Processor: org.archive.crawler.extractor.ExtractorHTML
  Function:          Link extraction on HTML documents
  CrawlURIs handled: 214
  Links extracted:   9677

Processor: org.archive.crawler.extractor.ExtractorCSS
  Function:          Link extraction on Cascading Style Sheets (.css)
  CrawlURIs handled: 2
  Links extracted:   0

Processor: org.archive.crawler.extractor.ExtractorJS
  Function:          Link extraction on JavaScript code
  CrawlURIs handled: 5
  Links extracted:   46

Processor: org.archive.crawler.extractor.ExtractorSWF
  Function:          Link extraction on Shockwave Flash documents (.swf)
  CrawlURIs handled: 148
  Links extracted:   452

Processor: is.hi.bok.digest.DeDuplicator
  Function:          Abort processing of duplicate records
                     - Lookup by url in use
  Total handled:     367
  Duplicates found:  0 0.0%
  Bytes total:       86608950 (83 MB)
  Bytes discarded:   0 (0 B) 0.0%
  New (no hits):     367
  Exact hits:        0
  Equivalent hits:   0
  Timestamp predicts: (Where exact URL existed in the index)
  Change correctly:  0
  Change falsly:     0
  Non-change correct:0
  Non-change falsly: 0
  Missing timpestamp:0
  [Host] [total] [duplicates] [bytes] [bytes discarded] [new] [exact] [equiv] [change correct] [change falsly] [non-change correct] [non-change falsly] [no timestamp]
  s9.addthis.com 1 0 877 0 1 0 0 0 0 0 0 0
  partner.googleadservices.com 1 0 7825 0 1 0 0 0 0 0 0 0
  www.carlsencards.com 182 0 42358017 0 182 0 0 0 0 0 0 0
  fpdownload2.macromedia.com 1 0 1884214 0 1 0 0 0 0 0 0 0
  carlsencards.com 182 0 42358017 0 182 0 0 0 0 0 0 0


metadata://netarkivet.dk/crawl/reports/responsecode-report.txt?heritrixVersion=1.14.3&harvestid=1&jobid=1 130.226.228.8 20090514142601 text/plain 56
[rescode] [#urls]
200 546
404 45
1 12
301 4
302 1
403 1

metadata://netarkivet.dk/crawl/reports/seeds-report.txt?heritrixVersion=1.14.3&harvestid=1&jobid=1 130.226.228.8 20090514142601 text/plain 127
[code] [status] [seed] [redirect]
200 CRAWLED http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm

metadata://netarkivet.dk/crawl/logs/crawl.log?heritrixVersion=1.14.3&harvestid=1&jobid=1 130.226.228.8 20090514142601 text/plain 151647
2009-05-14T14:21:01.026Z     1         58 dns:carlsencards.com P http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm text/dns #048 20090514142100573+135 sha1:LNHVBV2CPX3LEUOU7B2ZEDC7FOVYOWNF - content-size:58
2009-05-14T14:21:01.431Z   200         24 http://carlsencards.com/robots.txt P http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm text/plain #047 20090514142101357+69 sha1:NX6EE77HGWDXEZ4X6R6JQHRPET25ZRQ7 - content-size:315
2009-05-14T14:21:02.044Z   200      13160 http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm - - text/html #048 20090514142101743+29 sha1:FOZSHUKZVNNCGIBD7DYY3NP3NBVBSKZT - content-size:13455,3t
2009-05-14T14:21:02.045Z     1         60 dns:download.macromedia.com EP http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab text/dns #028 20090514142102007+22 sha1:NTT52JOSENFL5N6R5H4NJ3YXRTXL3D4E - content-size:60
2009-05-14T14:21:02.046Z     1        247 dns:www.google-analytics.com EP http://www.google-analytics.com/urchin.js text/dns #049 20090514142101948+47 sha1:UNS4FGRJEL3WTSQ2GCFEJQT3XSQXY7LW - content-size:247
2009-05-14T14:21:02.051Z     1        191 dns:pagead2.googlesyndication.com EP http://pagead2.googlesyndication.com/pagead/show_ads.js text/dns #047 20090514142101972+45 sha1:6RN766SHF34G23FV3RBIBCJZBFPPD4B6 - content-size:191
2009-05-14T14:21:02.428Z   200       3233 http://carlsencards.com/Scripts/AC_RunActiveContent.js E http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm application/x-javascript #001 20090514142102368+27 sha1:FCFAL2UKP7GGPHR2TJ7J4U4QIJS76XEY - content-size:3541
2009-05-14T14:21:02.462Z   200       4043 http://www.google-analytics.com/robots.txt EP http://www.google-analytics.com/urchin.js text/plain #044 20090514142102377+82 sha1:2JRMU32JADPKVYRE42CA4RU4BFEWSK3J - content-size:4222
2009-05-14T14:21:02.471Z   200         26 http://download.macromedia.com/robots.txt EP http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab text/plain #043 20090514142102381+88 sha1:MNSXZO35OCDMK2YM2TS4NGM3W2BWMSDI - content-size:271
2009-05-14T14:21:02.477Z   200         40 http://pagead2.googlesyndication.com/robots.txt EP http://pagead2.googlesyndication.com/pagead/show_ads.js text/plain #003 20090514142102378+96 sha1:MYUG3RTHZEBS7WU5VERK65PR5I3WALPG - content-size:174
2009-05-14T14:21:02.766Z   200        961 http://carlsencards.com/images/bottom.gif E http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm image/gif #028 20090514142102742+22 sha1:65NMJW62UE4EKQTBBFPKGPHJ5IKGHNX4 - content-size:1253
2009-05-14T14:21:02.871Z   302          0 http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab E http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm no-type #048 20090514142102784+81 sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ - content-size:212,3t
2009-05-14T14:21:02.954Z   200      22645 http://www.google-analytics.com/urchin.js E http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm text/javascript #049 20090514142102784+168 sha1:TN7SOU5XSERFFRLV3CWYONDVQLDU2RMZ - content-size:22873,3t
2009-05-14T14:21:03.078Z   200      31179 http://pagead2.googlesyndication.com/pagead/show_ads.js E http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm text/javascript #047 20090514142102783+206 sha1:PDUH7AM7O4U2WNSE3QTTKRFE2JPW6SWB - content-size:31554,3t
2009-05-14T14:21:03.110Z   200        231 http://carlsencards.com/images/line.gif E http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm image/gif #040 20090514142103083+24 sha1:FFV74LRA7H6G3HJ2UGDBHTZK5Y7EUAOH - content-size:522
2009-05-14T14:21:03.127Z     1        203 dns:partner.googleadservices.com EXP http://partner.googleadservices.com/ text/dns #043 20090514142103072+50 sha1:O5YPT2NFNPXV7M5J3BS4M3OM26FCHUOG - content-size:203
2009-05-14T14:21:03.168Z     1        211 dns:googleads.g.doubleclick.net EXP http://googleads.g.doubleclick.net/ text/dns #044 20090514142103060+104 sha1:37BZ3H73LW4V476UX5IOKDH54AYPPNMK - content-size:211
2009-05-14T14:21:03.259Z     1         59 dns:ad.yieldmanager.com EXP http://ad.yieldmanager.com/ text/dns #001 20090514142103064+192 sha1:J43PHALHDIO6BYC7DCJQNW2RK77YMNDC - content-size:59
2009-05-14T14:21:03.417Z     1         61 dns:www.macromedia.com XP http://www.macromedia.com/go/getflashplayer text/dns #038 20090514142103201+213 sha1:FJHBBCPSN6IVVGIR7WL627OHFDT6DFYI - content-size:61
2009-05-14T14:21:03.794Z   200      53181 http://carlsencards.com/images/newCardTemplateBtm_fortTell.jpg E http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm image/jpeg #035 20090514142103422+167 sha1:3LZU5TRSFDGY2FIJE7FQEEW5JYZ7Y7GI - content-size:53477
2009-05-14T14:21:03.853Z   200         40 http://partner.googleadservices.com/robots.txt EXP http://partner.googleadservices.com/ text/plain #026 20090514142103450+343 sha1:MYUG3RTHZEBS7WU5VERK65PR5I3WALPG - content-size:174
2009-05-14T14:21:03.881Z   200         40 http://googleads.g.doubleclick.net/robots.txt EXP http://googleads.g.doubleclick.net/ text/plain #032 20090514142103515+365 sha1:MYUG3RTHZEBS7WU5VERK65PR5I3WALPG - content-size:174
2009-05-14T14:21:03.949Z   404          9 http://ad.yieldmanager.com/robots.txt EXP http://ad.yieldmanager.com/ text/plain #050 20090514142103578+247 sha1:2IC4XVTYGMZKEEWFV2JNOPDXC6GC2LZI - content-size:280
2009-05-14T14:21:03.978Z   301        241 http://pagead2.googlesyndication.com/ EX http://pagead2.googlesyndication.com/pagead/show_ads.js text/html #037 20090514142103392+225 sha1:4PW55ISZQAQ6ZFPG2NBY6HF75WU4JWGN - content-size:443
2009-05-14T14:21:04.155Z   200       1348 http://carlsencards.com/images/top4About_over.gif X http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm image/gif #044 20090514142104131+23 sha1:UAMZ26XMYX6AHTN744UF73CKZTGS6ZRZ - content-size:1641
2009-05-14T14:21:04.208Z   301        239 http://www.macromedia.com/robots.txt XP http://www.macromedia.com/go/getflashplayer text/html #048 20090514142103768+428 sha1:5H5MF2CFFF4ENJS6I7MBTT2HG652XTKN - content-size:522
2009-05-14T14:21:04.211Z   -63          - http://www.adobe.com/robots.txt XPR http://www.macromedia.com/robots.txt no-type #001 - - - -
2009-05-14T14:21:04.354Z   404          9 http://ad.yieldmanager.com/ EX http://pagead2.googlesyndication.com/pagead/show_ads.js text/plain #003 20090514142104262+89 sha1:2IC4XVTYGMZKEEWFV2JNOPDXC6GC2LZI - content-size:280,3t
2009-05-14T14:21:04.380Z   200         21 http://pagead2.googlesyndication.com/pagead/ EXR http://pagead2.googlesyndication.com/ text/html #022 20090514142104292+85 sha1:TZDJ4Y5CEE7SM3CC2CWTROIAG5JGMYG2 - content-size:390
2009-05-14T14:21:04.385Z   301        239 http://googleads.g.doubleclick.net/ EX http://pagead2.googlesyndication.com/pagead/show_ads.js text/html #010 20090514142104262+117 sha1:DQSG5NLY6EBDSMXRZDEIGFMJCIYGVXNG - content-size:439,3t
2009-05-14T14:21:04.452Z   200       6359 http://partner.googleadservices.com/ EX http://pagead2.googlesyndication.com/pagead/show_ads.js text/html #006 20090514142104213+203 sha1:5F4AJOKYCB3PT5F3TFEBIKNWW2U53WYX - content-size:6685,3t
2009-05-14T14:21:04.498Z   200       1474 http://carlsencards.com/images/top5Contact_over.gif X http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm image/gif #013 20090514142104473+23 sha1:3ENF6M47XANHT4S5ZKVJJQB7HBOXQJB2 - content-size:1767
2009-05-14T14:21:04.774Z   200         21 http://googleads.g.doubleclick.net/pagead/ EXR http://googleads.g.doubleclick.net/ text/html #032 20090514142104691+81 sha1:TZDJ4Y5CEE7SM3CC2CWTROIAG5JGMYG2 - content-size:440
2009-05-14T14:21:04.837Z   200       1805 http://carlsencards.com/images/topStore_over.gif X http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm image/gif #050 20090514142104811+25 sha1:SY3SRA7UZSDNUC27PNSNCZQH4JVT4ALD - content-size:2098
2009-05-14T14:21:04.904Z   200       7582 http://partner.googleadservices.com/intl/en_com/images/logo_plain.png EXE http://partner.googleadservices.com/ image/png #049 20090514142104771+131 sha1:XYZQI6PS7RVG4FTUCHHFFIJZ4GZVE4ZS - content-size:7825
2009-05-14T14:21:05.071Z   301        291 http://www.macromedia.com/go/getflashplayer X http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm text/html #028 20090514142104651+414 sha1:7SLZKPNRQSPGM5TSPEL7RLSK7ZH2CBYE - content-size:626,3t
2009-05-14T14:21:05.278Z     1         56 dns:www.adobe.com XRP http://www.adobe.com/shockwave/download/download.cgi?P1_Prod_Version=ShockwaveFlash text/dns #040 20090514142105080+195 sha1:MFNUYF62VZQFQ4W5YZN2LP7ZTSVSUXSV - content-size:56
2009-05-14T14:21:05.612Z   -61          - http://www.adobe.com/shockwave/download/download.cgi?P1_Prod_Version=ShockwaveFlash XR http://www.macromedia.com/go/getflashplayer no-type #035 - - - 2t
2009-05-14T14:21:05.694Z   200     854427 http://carlsencards.com/Ecard38.swf E http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm application/x-shockwave-flash #044 20090514142105151+278 sha1:HRQG67POJUAH5626L6HHZSWTWQOIQ4T2 - content-size:854744
2009-05-14T14:21:05.722Z     1         97 dns:fpdownload2.macromedia.com ERP http://fpdownload2.macromedia.com/get/shockwave/cabs/flash/swflash.cab text/dns #012 20090514142105510+210 sha1:ERDOQADQZ5ZOMMPA65NZHIXMI3XWREY4 - content-size:97
2009-05-14T14:21:06.024Z   200        319 http://carlsencards.com/images/top3.gif E http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm image/gif #037 20090514142106001+21 sha1:VUCO34MR5S7LAOMCD4HUNBRV5UI7EASS - content-size:611
2009-05-14T14:21:06.082Z   200         26 http://fpdownload2.macromedia.com/robots.txt ERP http://fpdownload2.macromedia.com/get/shockwave/cabs/flash/swflash.cab text/plain #028 20090514142106045+36 sha1:MNSXZO35OCDMK2YM2TS4NGM3W2BWMSDI - content-size:273
2009-05-14T14:21:06.854Z   200    1883940 http://fpdownload2.macromedia.com/get/shockwave/cabs/flash/swflash.cab ER http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab application/x-cab-compressed #022 20090514142106391+448 sha1:ZJQ2JJVYW7IWJ2UEMM3DM3NQNJ4URF2T - content-size:1884214,3t
2009-05-14T14:21:07.406Z   200        306 http://carlsencards.com/images/top7.gif E http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm image/gif #010 20090514142107381+24 sha1:2ZA2XXDVHLXYB5WQMBKTS4S5IHKM44PU - content-size:598
2009-05-14T14:21:07.735Z   200        123 http://carlsencards.com/images/mainTile.gif E http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm image/gif #012 20090514142107711+22 sha1:GSKVU6DKSDA4LXXQI5DFR6D5G2NXFRV5 - content-size:414
2009-05-14T14:21:08.065Z   200       2055 http://carlsencards.com/images/top8_micardsa.gif E http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm image/gif #037 20090514142108041+22 sha1:ZQF4BMLTBCSCAQ4YAV54CXXBD7HR2OS4 - content-size:2348
2009-05-14T14:21:08.396Z   200       1340 http://carlsencards.com/images/top4About_up.gif E http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm image/gif #010 20090514142108371+23 sha1:P53ZCQ4T7FIFOJDZ66YGVAWX3MHIBUOW - content-size:1633
2009-05-14T14:21:08.729Z   200       1684 http://carlsencards.com/images/top6Select_over.gif X http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm image/gif #044 20090514142108701+26 sha1:XA5ZKO3FLI774ZWP7NUG357RYAEUIK55 - content-size:1977
2009-05-14T14:21:09.068Z   200       2926 http://carlsencards.com/carlsencards.css E http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm text/css #002 20090514142109041+24 sha1:BVWR2SMZOLX4QO7WZODFHNHWGXQKYYAN - content-size:3218
2009-05-14T14:21:09.404Z   404        212 http://carlsencards.com/outgoing/cafepress X http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm text/html #010 20090514142109381+22 sha1:NLUTP7HPNVNEVWNZS77TC5PGKVW6GMD2 - content-size:413
2009-05-14T14:21:09.740Z   200       9335 http://carlsencards.com/images/top2Logo.gif E http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm image/gif #044 20090514142109711+28 sha1:733OWHC7LSROYZVCICHF6XOBZQHQWQC2 - content-size:9629
2009-05-14T14:21:10.074Z   200        453 http://carlsencards.com/images/top1.gif E http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm image/gif #037 20090514142110051+21 sha1:2RAUWFZ7XQLJN7VV75TU5ZQJZZB4EZCT - content-size:745
2009-05-14T14:21:10.404Z   200       1668 http://carlsencards.com/images/top6Select_up.gif E http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm image/gif #010 20090514142110381+22 sha1:BNL3P6CQTYEZKT6DP74UTA2LPRRHVL2T - content-size:1961
2009-05-14T14:21:10.765Z   200      59871 http://carlsencards.com/images/newCardTemplateBtm_hamster.jpg E http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm image/jpeg #044 20090514142110712+51 sha1:7SKXBOZLD7TRBKBFIY7PJSWJYHEENEZ4 - content-size:60167
2009-05-14T14:21:11.095Z   200       1795 http://carlsencards.com/images/topStore_up.gif E http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm image/gif #037 20090514142111071+23 sha1:TA7ZZTRET2ZUIREMTIW36IM3Z3P56G7G - content-size:2088
2009-05-14T14:21:11.424Z   200       1463 http://carlsencards.com/images/top5Contact_up.gif E http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm image/gif #010 20090514142111401+22 sha1:RIEXVJ3N7KUPRKPQQSWDNE7M3QQE4YWO - content-size:1756
2009-05-14T14:21:11.833Z   200      14919 http://carlsencards.com/about.htm L http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm text/html #012 20090514142111731+37 sha1:N3MINUMWMCR3XPMAC4JMPHL2R7Z2VSZV - content-size:15214
2009-05-14T14:21:12.167Z   404        209 http://carlsencards.com/images/aboutCat LX http://carlsencards.com/about.htm text/html #043 20090514142112142+24 sha1:O7YNBR4WAVNYDQVGEMIOJEEQROGGHAU5 - content-size:410
2009-05-14T14:21:12.539Z   200      32356 http://carlsencards.com/images/aboutCat.swf LE http://carlsencards.com/about.htm application/x-shockwave-flash #006 20090514142112471+46 sha1:JYA2MPNONP6DSGQ6ZCSOLEVH43ZOGUMX - content-size:32671
2009-05-14T14:21:13.377Z   200     116554 http://carlsencards.com/index.htm L http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm text/html #032 20090514142112852+68 sha1:S55ZEYEBJ76BSSWIT3INW3IV7GJXSW3J - content-size:116851
2009-05-14T14:21:13.407Z     1         61 dns:s9.addthis.com LEP http://s9.addthis.com/button1-bm.gif text/dns #048 20090514142113214+190 sha1:KNLFN6LR5RCV6OUT2CO5UODDULU2D6DE - content-size:61
2009-05-14T14:21:13.729Z   200      37845 http://carlsencards.com/images/free-ecard-thumb17.jpg LE http://carlsencards.com/index.htm image/jpeg #026 20090514142113681+47 sha1:B6WJVEOUVXBCZPK7C3MQS7YQA3BP6GTT - content-size:38141
2009-05-14T14:21:13.778Z   200        116 http://s9.addthis.com/robots.txt LEP http://s9.addthis.com/button1-bm.gif text/plain #012 20090514142113725+52 sha1:A6J7I3PWLEWBIYX7K6YSWAVZLTFFTEL5 - content-size:354
2009-05-14T14:21:14.068Z   200       1937 http://carlsencards.com/images/headerWontbecomingtowork.gif LE http://carlsencards.com/index.htm image/gif #002 20090514142114041+26 sha1:C7ZCVGOR2VIOYKC5LHDY5JXCBFFJQBXL - content-size:2230
2009-05-14T14:21:14.140Z   200        637 http://s9.addthis.com/button1-bm.gif LE http://carlsencards.com/index.htm image/gif #037 20090514142114091+48 sha1:GYYKCUJEDNJI3XHWX3AJ2O3R737744F6 - content-size:877,3t
2009-05-14T14:21:14.466Z   200      26806 http://carlsencards.com/images/free-ecard-thumb26.jpg LE http://carlsencards.com/index.htm image/jpeg #048 20090514142114381+83 sha1:KPZW5TXKJND6D2QRK3A3P2SQ6RVR4RRJ - content-size:27102
2009-05-14T14:21:14.556Z   200       9388 http://s9.addthis.com/js/widget.php?v=10 LE http://carlsencards.com/index.htm text/html #006 20090514142114461+93 sha1:KRL37XJFXZ4YZBV643CV7R6IFVL5XSSK - content-size:9566
2009-05-14T14:21:14.815Z   200      12060 http://carlsencards.com/images/seatFillers/twoCats.swf LE http://carlsencards.com/index.htm application/x-shockwave-flash #012 20090514142114772+36 sha1:OYNUMN65YLC77QEVD5Q7GAOQMG7VUH47 - content-size:12375
2009-05-14T14:21:15.180Z   200      37745 http://carlsencards.com/images/newCardTopFrontpgHBFlowers.jpg LE http://carlsencards.com/index.htm image/jpeg #037 20090514142115131+47 sha1:GOTG452VF2YSZ2ZOZF3DCUTJHRG3OGGX - content-size:38041
2009-05-14T14:21:15.530Z   200      22700 http://carlsencards.com/images/newCardTopFrontpg_fortTellR.jpg LE http://carlsencards.com/index.htm image/jpeg #006 20090514142115491+37 sha1:ONPR44DDSR5F7PR3M4VGSM3JAPK23GUZ - content-size:22996
2009-05-14T14:21:15.890Z   200      40297 http://carlsencards.com/images/free-ecard-thumb20.jpg LE http://carlsencards.com/index.htm image/jpeg #022 20090514142115841+47 sha1:IRQZX4BIENZK2CUGDIZGDKVENVBYUEFV - content-size:40593
2009-05-14T14:21:16.256Z   200      23662 http://carlsencards.com/images/free-ecard-thumb33.jpg LE http://carlsencards.com/index.htm image/jpeg #005 20090514142116202+52 sha1:XSPUCFDCCN2YYUZDG5XWCVAJPAWS734S - content-size:23958
2009-05-14T14:21:16.607Z   200      13506 http://carlsencards.com/images/seatFillers/hollaMouse.swf LE http://carlsencards.com/index.htm application/x-shockwave-flash #031 20090514142116561+37 sha1:RRV3LAXCKTV4RCLK2MZT5OMXFYO6IFK4 - content-size:13821
2009-05-14T14:21:16.955Z   200      24342 http://carlsencards.com/images/free-ecard-thumb47.jpg LE http://carlsencards.com/index.htm image/jpeg #050 20090514142116911+42 sha1:CDYO73F6Q5J4ECWKW6YTYZXT5CQC53X6 - content-size:24638
2009-05-14T14:21:17.288Z   200        818 http://carlsencards.com/images/headerILoveYou.gif LE http://carlsencards.com/index.htm image/gif #038 20090514142117262+24 sha1:GGMSV5BJY6MUJ5XILSFL3WN6TAS4QF7D - content-size:1110
2009-05-14T14:21:17.614Z   200       1203 http://carlsencards.com/images/extLinks/ofree_mini_3.gif LE http://carlsencards.com/index.htm image/gif #031 20090514142117592+21 sha1:WNRYCHO72DRW6A56UXYCYEZW7N2ELMU6 - content-size:1496
2009-05-14T14:21:17.960Z   200      25475 http://carlsencards.com/images/free-ecard-thumb02.jpg LE http://carlsencards.com/index.htm image/jpeg #050 20090514142117922+36 sha1:SPSRXBVXYUEJJN4AYVVEDL5CYRWKJLJY - content-size:25771
2009-05-14T14:21:18.295Z   200       3083 http://carlsencards.com/images/extLinks/top20free88x53PD.gif LE http://carlsencards.com/index.htm image/gif #038 20090514142118272+21 sha1:LB76JDQZEMK6IL6PBDRGBVOZXN2YLSOP - content-size:3376
2009-05-14T14:21:18.662Z   200      12291 http://carlsencards.com/images/seatFillers/penguinJoke.swf LE http://carlsencards.com/index.htm application/x-shockwave-flash #031 20090514142118602+55 sha1:QZ4AWBGYNIAXRCPC7ZZ63GM4ZQPYMFIR - content-size:12606
2009-05-14T14:21:18.994Z   200        417 http://carlsencards.com/images/campBoxTile.jpg LE http://carlsencards.com/index.htm image/jpeg #049 20090514142118972+21 sha1:H7X5DA7BB6MN7TJPQSVCPVARL7N6GF3B - content-size:710
2009-05-14T14:21:19.505Z   200      24969 http://carlsencards.com/images/free-ecard-thumb14.jpg LE http://carlsencards.com/index.htm image/jpeg #038 20090514142119302+150 sha1:SXGFUP5GBJC24MPXA6OZWE7KOFW3BOYY - content-size:25265
2009-05-14T14:21:19.865Z   200       1231 http://carlsencards.com/images/headerNoSpecOcc.gif LE http://carlsencards.com/index.htm image/gif #012 20090514142119842+21 sha1:HWF3YHQUOLZWH34RXK6KEERVLIYUXZIC - content-size:1524
2009-05-14T14:21:20.217Z   200      33173 http://carlsencards.com/images/free-ecard-thumb40.jpg LE http://carlsencards.com/index.htm image/jpeg #028 20090514142120171+45 sha1:WSFZ646L3LZDRKHZCHHITHKFGRLTKRVW - content-size:33469
2009-05-14T14:21:20.561Z   200      25944 http://carlsencards.com/images/free-ecard-thumb21.jpg LE http://carlsencards.com/index.htm image/jpeg #038 20090514142120521+38 sha1:FPKUXFT73B3DWSZ3BZ3KGZUEUEHUSX24 - content-size:26240
2009-05-14T14:21:20.918Z   200      39614 http://carlsencards.com/images/free-ecard-thumb56.jpg LE http://carlsencards.com/index.htm image/jpeg #022 20090514142120872+45 sha1:AXVN6CASVGLI5AVGUYCWMD7WFHURAONY - content-size:39910
2009-05-14T14:21:21.269Z   200      39614 http://carlsencards.com/images/free-ecard-thumb44.jpg LE http://carlsencards.com/index.htm image/jpeg #047 20090514142121221+47 sha1:AXVN6CASVGLI5AVGUYCWMD7WFHURAONY - content-size:39910
2009-05-14T14:21:21.622Z   200      23021 http://carlsencards.com/images/free-ecard-thumb30.jpg LE http://carlsencards.com/index.htm image/jpeg #006 20090514142121582+39 sha1:SYVSXQXTZPDXLU7I2AFB7RZRWBKCK2L2 - content-size:23317
2009-05-14T14:21:21.954Z   200        204 http://carlsencards.com/images/campBoxBtm.gif LE http://carlsencards.com/index.htm image/gif #050 20090514142121932+21 sha1:HWKNSCPQDMXGJ2FU5WPUBI2QT6XAMXEV - content-size:495
2009-05-14T14:21:22.303Z   200      23784 http://carlsencards.com/images/free-ecard-thumb13.jpg LE http://carlsencards.com/index.htm image/jpeg #047 20090514142122262+39 sha1:C2GSIZTGQQ73LLNCLQESV64FDESTF5KJ - content-size:24080
2009-05-14T14:21:22.650Z   200      22950 http://carlsencards.com/images/free-ecard-thumb18.jpg LE http://carlsencards.com/index.htm image/jpeg #006 20090514142122612+37 sha1:KQCXKUVEEC3QKAT3S3QUQARB3CDZICYT - content-size:23246
2009-05-14T14:21:23.001Z   200      27136 http://carlsencards.com/images/free-ecard-thumb53.jpg LE http://carlsencards.com/index.htm image/jpeg #049 20090514142122962+37 sha1:2OQ4A3ZQ75DUF6TR6UYZEI5GDGWJCSJ5 - content-size:27432
2009-05-14T14:21:23.361Z   200      41328 http://carlsencards.com/images/free-ecard-thumb15.jpg LE http://carlsencards.com/index.htm image/jpeg #047 20090514142123313+46 sha1:3N5BXYTG65PFA2PZVKPWV4E2YKBJXDJW - content-size:41624
2009-05-14T14:21:23.712Z   200      16067 http://carlsencards.com/images/free-ecard-thumb03.jpg LE http://carlsencards.com/index.htm image/jpeg #013 20090514142123672+38 sha1:V5BGZIG2X7SX4XXMRFQUIFAFEMANJUU6 - content-size:16363
2009-05-14T14:21:24.045Z   200       1254 http://carlsencards.com/images/headerHappyAnniversary.gif LE http://carlsencards.com/index.htm image/gif #002 20090514142124022+21 sha1:6OD4G226YNCRD6XQ6QZF5O54HGCHHCMH - content-size:1547
2009-05-14T14:21:24.723Z   200       3054 http://carlsencards.com/images/extLinks/banner2.gif LE http://carlsencards.com/index.htm image/gif #047 20090514142124352+370 sha1:XE54VJXMGM4ZSITOIFAAARCF5F33SZRN - content-size:3347
2009-05-14T14:21:25.142Z   200      26549 http://carlsencards.com/images/free-ecard-thumb27.jpg LE http://carlsencards.com/index.htm image/jpeg #028 20090514142125102+38 sha1:VLOYZJFAFCKFQHEJKIGBOFIWK2IKWF66 - content-size:26845
2009-05-14T14:21:25.606Z   200     301349 http://carlsencards.com/images/seatFillers/musicRadio.swf LE http://carlsencards.com/index.htm application/x-shockwave-flash #040 20090514142125452+138 sha1:IJF23YOY3E2XLX6YL6H6XB3MZLUUXM77 - content-size:301666
2009-05-14T14:21:25.952Z   200      26443 http://carlsencards.com/images/free-ecard-thumb09.jpg LE http://carlsencards.com/index.htm image/jpeg #012 20090514142125912+38 sha1:4YZ2DW7VY62YJVAFFSK4275P45II6Q7L - content-size:26739
2009-05-14T14:21:26.305Z   200      28710 http://carlsencards.com/images/free-ecard-thumb29.jpg LE http://carlsencards.com/index.htm image/jpeg #037 20090514142126262+42 sha1:IJWX4DVFVPWM2ETM45ZJUVPUJJNI6VYI - content-size:29006
2009-05-14T14:21:26.652Z   200      28357 http://carlsencards.com/images/free-ecard-thumb45.jpg LE http://carlsencards.com/index.htm image/jpeg #038 20090514142126612+39 sha1:UU5QWP573MLBSG25DFJVFUJWUKW7B5O4 - content-size:28653
2009-05-14T14:21:27.010Z   200      29853 http://carlsencards.com/images/free-ecard-thumb11.jpg LE http://carlsencards.com/index.htm image/jpeg #022 20090514142126962+46 sha1:XOI3LG4APLIBMCDLSHBLRCB3I35TP7RN - content-size:30149
2009-05-14T14:21:27.349Z   200       2542 http://carlsencards.com/images/extLinks/small2.gif LE http://carlsencards.com/index.htm image/gif #005 20090514142127322+25 sha1:FVZZW5LFIUJGSCLPMQ7PUDSDDRR7WBYE - content-size:2835
2009-05-14T14:21:27.698Z   200      31067 http://carlsencards.com/images/free-ecard-thumb16.jpg LE http://carlsencards.com/index.htm image/jpeg #038 20090514142127652+45 sha1:SDVAUXMRINUPOX76OIAHDIMUYMCF3DNT - content-size:31363
2009-05-14T14:21:28.048Z   200      31543 http://carlsencards.com/images/free-ecard-thumb23.jpg LE http://carlsencards.com/index.htm image/jpeg #050 20090514142128002+44 sha1:Z6UV5Z2GQ4OM4GU24DG5QCAMEXK6UERE - content-size:31839
2009-05-14T14:21:28.390Z   200      24622 http://carlsencards.com/images/free-ecard-thumb22.jpg LE http://carlsencards.com/index.htm image/jpeg #001 20090514142128352+37 sha1:3ATP2FR6PJ7S564Y3UEP23DN7VBS7FKY - content-size:24918
2009-05-14T14:21:28.784Z   200      28910 http://carlsencards.com/images/free-ecard-thumb35.jpg LE http://carlsencards.com/index.htm image/jpeg #038 20090514142128702+46 sha1:2R72SPMVXEZNJOI2DUFTEFJ5JF473G4T - content-size:29206
2009-05-14T14:21:29.131Z   200      23414 http://carlsencards.com/images/free-ecard-thumb58.jpg LE http://carlsencards.com/index.htm image/jpeg #028 20090514142129092+37 sha1:NHXBTTSPBRC23ZKDVAJ7LIASQJM7Z47O - content-size:23710
2009-05-14T14:21:29.467Z   200        998 http://carlsencards.com/images/headerGetwellsoon.gif LE http://carlsencards.com/index.htm image/gif #032 20090514142129442+23 sha1:LJ24AUYT3CCEKY75LO3Y77MNRDX57NDL - content-size:1290
2009-05-14T14:21:29.795Z   200       1122 http://carlsencards.com/images/headerHoliday.gif LE http://carlsencards.com/index.htm image/gif #013 20090514142129772+22 sha1:O2KKKEOIC5A2G5S4NBXDTXU6MEJGNKY3 - content-size:1415
2009-05-14T14:21:30.224Z   200      23311 http://carlsencards.com/images/free-ecard-thumb41.jpg LE http://carlsencards.com/index.htm image/jpeg #028 20090514142130102+120 sha1:GSVJ7ADE74STHQYLEPEMVAGIPRSB5AXV - content-size:23607
2009-05-14T14:21:30.555Z   200       1456 http://carlsencards.com/images/extLinks/LunarAntics_8031.gif LE http://carlsencards.com/index.htm image/gif #003 20090514142130532+22 sha1:EY56IKKIAS7A5CJV3GAKQW3LOQG5DEBJ - content-size:1749
2009-05-14T14:21:30.906Z   200      30054 http://carlsencards.com/images/free-ecard-thumb36.jpg LE http://carlsencards.com/index.htm image/jpeg #044 20090514142130862+43 sha1:BRTWA6BA3IJVPA2NLK3XTUYMIQI2O2H6 - content-size:30350
2009-05-14T14:21:31.258Z   200        729 http://carlsencards.com/images/headerSorry.gif LE http://carlsencards.com/index.htm image/gif #028 20090514142131212+45 sha1:YHUW3ABL7E54RCRMWZADLIJI6OECK75G - content-size:1021
2009-05-14T14:21:31.602Z   200      21987 http://carlsencards.com/images/free-ecard-thumb01.jpg LE http://carlsencards.com/index.htm image/jpeg #003 20090514142131562+38 sha1:3GLURXCDEWJNT5PT3FJVQC3MBQVISRZI - content-size:22283
2009-05-14T14:21:31.936Z   200       1313 http://carlsencards.com/images/headerLinks2.gif LE http://carlsencards.com/index.htm image/gif #012 20090514142131912+23 sha1:AD2ANLQYT7TMCKVB3TADRNKFUMZJBOJ4 - content-size:1606
2009-05-14T14:21:32.293Z   200      33962 http://carlsencards.com/images/free-ecard-thumb32.jpg LE http://carlsencards.com/index.htm image/jpeg #028 20090514142132242+50 sha1:PFSVOXBCYFKWWJP2IF4RAGEIJNBF336Z - content-size:34258
2009-05-14T14:21:32.660Z   200      27906 http://carlsencards.com/images/free-ecard-thumb39.jpg LE http://carlsencards.com/index.htm image/jpeg #003 20090514142132602+46 sha1:XWEXWJQB6DFQ2GNBFXCWBAXQHFI6ZIYM - content-size:28202
2009-05-14T14:21:33.019Z   200      31311 http://carlsencards.com/images/free-ecard-thumb54.jpg LE http://carlsencards.com/index.htm image/jpeg #022 20090514142132972+46 sha1:CGJXS7GMKMOGKCC326IZMIEVFUAVZZPJ - content-size:31607
2009-05-14T14:21:33.348Z   200       2955 http://carlsencards.com/images/extLinks/allfreethings_88.gif LE http://carlsencards.com/index.htm image/gif #043 20090514142133322+24 sha1:NILIH5Y7VMCTGSDIWKMOICJY54JBQIYX - content-size:3248
2009-05-14T14:21:33.682Z   200      12556 http://carlsencards.com/images/free-ecard-thumb24.jpg LE http://carlsencards.com/index.htm image/jpeg #003 20090514142133652+29 sha1:N4QGENO4VUY4UHVUMXOH2I43IKIDXOAB - content-size:12852
2009-05-14T14:21:34.041Z   200      35078 http://carlsencards.com/images/free-ecard-thumb49.jpg LE http://carlsencards.com/index.htm image/jpeg #022 20090514142133992+48 sha1:BKX66UQRR3PK7X6Y2464XFRHFHIR2ZGN - content-size:35374
2009-05-14T14:21:34.390Z   200      20068 http://carlsencards.com/images/free-ecard-thumb19.jpg LE http://carlsencards.com/index.htm image/jpeg #037 20090514142134352+37 sha1:TIYUXM5QRQNAPN5WIQ6BRWYK5SNMBZWY - content-size:20364
2009-05-14T14:21:34.864Z   200        822 http://carlsencards.com/images/headerIhateYou.gif LE http://carlsencards.com/index.htm image/gif #006 20090514142134703+106 sha1:F2S7TNXU6WSY66VIETXUA72JR7ECMBME - content-size:1114
2009-05-14T14:21:35.241Z   200      21123 http://carlsencards.com/images/free-ecard-thumb48.jpg LE http://carlsencards.com/index.htm image/jpeg #028 20090514142135202+37 sha1:G4MCDVAC6KCKVEAO5SYSP7MPQ5YSX52H - content-size:21419
2009-05-14T14:21:35.576Z   200       1088 http://carlsencards.com/images/headerSpreadtheword.gif LE http://carlsencards.com/index.htm image/gif #010 20090514142135552+23 sha1:BZSGFWL2LNVDNPCSBTZJHXN5VXGIJOVK - content-size:1381
2009-05-14T14:21:35.906Z   200        859 http://carlsencards.com/images/headerThankYou.gif LE http://carlsencards.com/index.htm image/gif #006 20090514142135883+22 sha1:E4A54CBVUA5ANPEKWYI3EAXWGFZF2PM6 - content-size:1151
2009-05-14T14:21:36.257Z   200      31668 http://carlsencards.com/images/free-ecard-thumb08.jpg LE http://carlsencards.com/index.htm image/jpeg #028 20090514142136212+44 sha1:IDLAOEM7MQ3VD7UX3E5QIWH6FJSU3265 - content-size:31964
2009-05-14T14:21:36.608Z   200      30126 http://carlsencards.com/images/free-ecard-thumb51.jpg LE http://carlsencards.com/index.htm image/jpeg #010 20090514142136562+45 sha1:FBKMFLLL4YM3ASA4JT2HO6ZIIZELZENY - content-size:30422
2009-05-14T14:21:36.957Z   200      33974 http://carlsencards.com/images/free-ecard-thumb28.jpg LE http://carlsencards.com/index.htm image/jpeg #006 20090514142136912+43 sha1:QYJPAFLXY25FNTWBENSFVCKKGQMWIIYE - content-size:34270
2009-05-14T14:21:37.288Z   200       2933 http://carlsencards.com/images/extLinks/freesitee.gif LE http://carlsencards.com/index.htm image/gif #028 20090514142137262+24 sha1:CSJXAJC5UBJFAS2Y32GJWIGKS6SKUW3H - content-size:3226
2009-05-14T14:21:37.637Z   200      16240 http://carlsencards.com/images/seatFillers/frontpageCat.swf LE http://carlsencards.com/index.htm application/x-shockwave-flash #010 20090514142137592+37 sha1:HYE6AVOIASRIYJSFOESNUO5S5L6QSD5V - content-size:16555
2009-05-14T14:21:37.982Z   200      24784 http://carlsencards.com/images/free-ecard-thumb05.jpg LE http://carlsencards.com/index.htm image/jpeg #006 20090514142137942+38 sha1:QHJ7KUASBUTYHUCDOC2VIRWBUOAX5523 - content-size:25080
2009-05-14T14:21:38.341Z   200      26743 http://carlsencards.com/images/free-ecard-thumb34.jpg LE http://carlsencards.com/index.htm image/jpeg #043 20090514142138292+48 sha1:PUM2KJ74NOOBIJO5LYHA4K57KAK7Q5T2 - content-size:27039
2009-05-14T14:21:38.693Z   200      23333 http://carlsencards.com/images/free-ecard-thumb52.jpg LE http://carlsencards.com/index.htm image/jpeg #048 20090514142138652+39 sha1:SUID2FW5CBHHAQX5UBP6SIRWFDHIBQRL - content-size:23629
2009-05-14T14:21:39.042Z   200      25439 http://carlsencards.com/images/free-ecard-thumb31.jpg LE http://carlsencards.com/index.htm image/jpeg #012 20090514142139003+38 sha1:VNGVJ5NL5CRDJ4S3E2NVGMGFSLWEURLO - content-size:25735
2009-05-14T14:21:39.401Z   200      40204 http://carlsencards.com/images/free-ecard-thumb46.jpg LE http://carlsencards.com/index.htm image/jpeg #037 20090514142139353+46 sha1:HYBLOQZQI6GKCO34R32GTD2UALRDXHI5 - content-size:40500
2009-05-14T14:21:39.754Z   200      21101 http://carlsencards.com/images/free-ecard-thumb00.jpg LE http://carlsencards.com/index.htm image/jpeg #003 20090514142139713+40 sha1:IC6VS36FXYLICSYUUU7GFY5VW7HP3JBC - content-size:21397
2009-05-14T14:21:40.112Z   200      37291 http://carlsencards.com/images/free-ecard-thumb10.jpg LE http://carlsencards.com/index.htm image/jpeg #022 20090514142140062+49 sha1:QZ3NXPDBKOJO5LOYPAPY3HGOJX6W6U4Y - content-size:37587
2009-05-14T14:21:40.462Z   200      27194 http://carlsencards.com/images/free-ecard-thumb55.jpg LE http://carlsencards.com/index.htm image/jpeg #005 20090514142140423+37 sha1:LDXB4FIQIRVA2QUK5PG2V3A2MS3GRFPG - content-size:27490
2009-05-14T14:21:40.811Z   200      25185 http://carlsencards.com/images/free-ecard-thumb42.jpg LE http://carlsencards.com/index.htm image/jpeg #047 20090514142140772+37 sha1:2SAQVZACY6A2LJLMKO2SHMMAFNT2WF37 - content-size:25481
2009-05-14T14:21:41.162Z   200      20501 http://carlsencards.com/images/free-ecard-thumb37.jpg LE http://carlsencards.com/index.htm image/jpeg #050 20090514142141122+39 sha1:OJDGIV5R6FVLHWBOA6RECAIJOCMWBETD - content-size:20797
2009-05-14T14:21:41.511Z   200      22988 http://carlsencards.com/images/free-ecard-thumb12.jpg LE http://carlsencards.com/index.htm image/jpeg #001 20090514142141473+37 sha1:GLHSP5BYHDXQATRBV6W7YTGV2ZIJITUZ - content-size:23284
2009-05-14T14:21:41.865Z   200      28399 http://carlsencards.com/images/free-ecard-thumb25.jpg LE http://carlsencards.com/index.htm image/jpeg #031 20090514142141822+42 sha1:X4PCYYGIJUMGU3PLOFME3RTCCACWWLFU - content-size:28695
2009-05-14T14:21:42.218Z   200      31008 http://carlsencards.com/images/free-ecard-thumb38.jpg LE http://carlsencards.com/index.htm image/jpeg #049 20090514142142172+44 sha1:5ROP654VEO537ONU6CUIDEMRBV6CATL4 - content-size:31304
2009-05-14T14:21:42.547Z   200       1142 http://carlsencards.com/images/headerCongrats.gif LE http://carlsencards.com/index.htm image/gif #001 20090514142142523+23 sha1:EEQVPQ4TE3PGBVRMQUY5LPZ6N6VBDSHQ - content-size:1435
2009-05-14T14:21:42.899Z   200      29915 http://carlsencards.com/images/free-ecard-thumb59.jpg LE http://carlsencards.com/index.htm image/jpeg #031 20090514142142852+45 sha1:7C62ROF35XW4X5R4BYM4UHEJB4ZMUSCS - content-size:30211
2009-05-14T14:21:43.243Z   200      25561 http://carlsencards.com/images/free-ecard-thumb06.jpg LE http://carlsencards.com/index.htm image/jpeg #049 20090514142143203+39 sha1:FTLBUDBNLKZQWEVREQTLAU7NC7DRALZK - content-size:25857
2009-05-14T14:21:43.626Z   200       1010 http://carlsencards.com/images/headerHappybirthday.gif LE http://carlsencards.com/index.htm image/gif #001 20090514142143553+71 sha1:S6ANFLDKA2326C5GSMHTXFMHYPK52P4J - content-size:1303
2009-05-14T14:21:43.979Z   200      34161 http://carlsencards.com/images/free-ecard-thumb57.jpg LE http://carlsencards.com/index.htm image/jpeg #038 20090514142143933+44 sha1:FWW7JTZ2KTPNHMRFXUQ64F254IP66Z6M - content-size:34457
2009-05-14T14:21:44.323Z   200      18350 http://carlsencards.com/images/free-ecard-thumb04.jpg LE http://carlsencards.com/index.htm image/jpeg #028 20090514142144283+39 sha1:7QUFJE5LX7WFGGYOGQSMD46B3FKGAKC7 - content-size:18646
2009-05-14T14:21:44.658Z   200        203 http://carlsencards.com/images/campBoxTop.gif LE http://carlsencards.com/index.htm image/gif #010 20090514142144633+24 sha1:EXDSKG2GRCS4LEPFLXCNVBGGQIK4PYXM - content-size:494
2009-05-14T14:21:45.008Z   200      31978 http://carlsencards.com/images/free-ecard-thumb43.jpg LE http://carlsencards.com/index.htm image/jpeg #014 20090514142144963+43 sha1:7RA6V6E3ZVVWTEE4KY7SNNLQ2YJ7AJTU - content-size:32274
2009-05-14T14:21:45.359Z   200      41483 http://carlsencards.com/images/free-ecard-thumb50.jpg LE http://carlsencards.com/index.htm image/jpeg #028 20090514142145312+45 sha1:FAB3NB5LKEB5SV67VTHRRRYTU7MWPVMT - content-size:41779
2009-05-14T14:21:45.710Z   200      30307 http://carlsencards.com/images/free-ecard-thumb07.jpg LE http://carlsencards.com/index.htm image/jpeg #048 20090514142145662+47 sha1:6I7QTQQZNIU2N3UHOOZVWWJLI4YJDGMS - content-size:30603
2009-05-14T14:21:46.051Z   200       6890 http://carlsencards.com/images/extLinks/omg_button.gif LE http://carlsencards.com/index.htm image/gif #044 20090514142146023+26 sha1:5NFNEDXIAHQFZVOGZTGCW2TQXFVYXW63 - content-size:7184
2009-05-14T14:21:46.385Z   200       1815 http://carlsencards.com/images/headerNewBaby.gif LE http://carlsencards.com/index.htm image/gif #043 20090514142146363+21 sha1:5NK5R43JIMAISLJAYPAMEE3KSS2RP6F3 - content-size:2108
2009-05-14T14:21:46.754Z   200      12756 http://carlsencards.com/free-ecard-58-congratulations-hamster-on-strike.htm L http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm text/html #048 20090514142146693+29 sha1:UISKUXND75DU2QLB5XSH7OZNIXYNHNWI - content-size:13051
2009-05-14T14:21:47.514Z   200    1107240 http://carlsencards.com/Ecard58.swf LE http://carlsencards.com/free-ecard-58-congratulations-hamster-on-strike.htm application/x-shockwave-flash #006 20090514142147063+383 sha1:UFZ2A6SCTSWXJ5AMMFWFDJ4XD5NJC5FS - content-size:1107559
2009-05-14T14:21:48.679Z   200      20931 http://carlsencards.com/ecards-and-free-stuff-links.htm L http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm text/html #031 20090514142147913+719 sha1:A5RP4VMGI2XCALDS6AAKYDO656SFA5SJ - content-size:21226
2009-05-14T14:21:49.715Z   200       1026 http://carlsencards.com/images/extLinks/aford_promobanner_31x88.gif LE http://carlsencards.com/ecards-and-free-stuff-links.htm image/gif #031 20090514142149683+31 sha1:ODZWNUYQLREA3DJSRRVQAFIV7YKLOX7G - content-size:1319
2009-05-14T14:21:50.092Z   200      13065 http://carlsencards.com/contact.htm L http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm text/html #030 20090514142150023+35 sha1:MJ4LB45MYP2AEVTAF2IXH7W2JW4YQURO - content-size:13360
2009-05-14T14:21:50.597Z   200      13020 http://carlsencards.com/images/contactCat.swf LE http://carlsencards.com/contact.htm application/x-shockwave-flash #043 20090514142150403+115 sha1:62BCBWCUWHBND36NHBIQPPO2WQD233CE - content-size:13335
2009-05-14T14:21:50.956Z   404        211 http://carlsencards.com/images/contactCat LX http://carlsencards.com/contact.htm text/html #013 20090514142150933+21 sha1:FQFAYHEKXIXD5WZBMVRVKGIPG3WLJ4UX - content-size:412
2009-05-14T14:21:51.317Z   200      12601 http://carlsencards.com/disclaimer.htm L http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm text/html #002 20090514142151263+28 sha1:GEZSORQCPGAWMWLWS4ZGUK7XBXBYAK7Q - content-size:12896
2009-05-14T14:21:51.684Z   200      12767 http://carlsencards.com/free-ecard-51-happy-birthday-fortune-teller-rat.htm L http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm text/html #043 20090514142151623+28 sha1:6WWIG35CG34DBV2WHNDAJKF3QNK3D6XY - content-size:13062
2009-05-14T14:21:52.229Z   200     479689 http://carlsencards.com/Ecard51.swf LE http://carlsencards.com/free-ecard-51-happy-birthday-fortune-teller-rat.htm application/x-shockwave-flash #016 20090514142151993+203 sha1:4N5FRQ2J332RV2GLT5X4VTA364ZFCMQX - content-size:480006
2009-05-14T14:21:52.551Z     1         62 dns:www.carlsencards.com LP http://www.carlsencards.com/index.htm text/dns #005 20090514142152539+10 sha1:LYOFHK6QLIRKQ5O667EHNA65FIN5NGZG - content-size:62
2009-05-14T14:21:52.889Z   200         24 http://www.carlsencards.com/robots.txt LP http://www.carlsencards.com/index.htm text/plain #003 20090514142152867+21 sha1:NX6EE77HGWDXEZ4X6R6JQHRPET25ZRQ7 - content-size:315
2009-05-14T14:21:53.638Z   200     116554 http://www.carlsencards.com/index.htm L http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm text/html #016 20090514142153193+79 sha1:S55ZEYEBJ76BSSWIT3INW3IV7GJXSW3J - content-size:116851,3t
2009-05-14T14:21:54.081Z   200      31543 http://www.carlsencards.com/images/free-ecard-thumb23.jpg LE http://www.carlsencards.com/index.htm image/jpeg #013 20090514142153944+136 sha1:Z6UV5Z2GQ4OM4GU24DG5QCAMEXK6UERE - content-size:31839
2009-05-14T14:21:54.431Z   200      28357 http://www.carlsencards.com/images/free-ecard-thumb45.jpg LE http://www.carlsencards.com/index.htm image/jpeg #028 20090514142154393+37 sha1:UU5QWP573MLBSG25DFJVFUJWUKW7B5O4 - content-size:28653
2009-05-14T14:21:54.766Z   200       2926 http://www.carlsencards.com/carlsencards.css LE http://www.carlsencards.com/index.htm text/css #010 20090514142154743+21 sha1:BVWR2SMZOLX4QO7WZODFHNHWGXQKYYAN - content-size:3218
2009-05-14T14:21:55.119Z   200      30054 http://www.carlsencards.com/images/free-ecard-thumb36.jpg LE http://www.carlsencards.com/index.htm image/jpeg #033 20090514142155073+44 sha1:BRTWA6BA3IJVPA2NLK3XTUYMIQI2O2H6 - content-size:30350
2009-05-14T14:21:55.446Z   200       1805 http://www.carlsencards.com/images/topStore_over.gif LX http://www.carlsencards.com/index.htm image/gif #028 20090514142155423+22 sha1:SY3SRA7UZSDNUC27PNSNCZQH4JVT4ALD - content-size:2098
2009-05-14T14:21:55.799Z   200      28399 http://www.carlsencards.com/images/free-ecard-thumb25.jpg LE http://www.carlsencards.com/index.htm image/jpeg #031 20090514142155753+44 sha1:X4PCYYGIJUMGU3PLOFME3RTCCACWWLFU - content-size:28695
2009-05-14T14:21:56.144Z   200      22950 http://www.carlsencards.com/images/free-ecard-thumb18.jpg LE http://www.carlsencards.com/index.htm image/jpeg #014 20090514142156103+40 sha1:KQCXKUVEEC3QKAT3S3QUQARB3CDZICYT - content-size:23246
2009-05-14T14:21:56.477Z   200       1313 http://www.carlsencards.com/images/headerLinks2.gif LE http://www.carlsencards.com/index.htm image/gif #028 20090514142156454+22 sha1:AD2ANLQYT7TMCKVB3TADRNKFUMZJBOJ4 - content-size:1606
2009-05-14T14:21:56.805Z   200        998 http://www.carlsencards.com/images/headerGetwellsoon.gif LE http://www.carlsencards.com/index.htm image/gif #031 20090514142156783+21 sha1:LJ24AUYT3CCEKY75LO3Y77MNRDX57NDL - content-size:1290
2009-05-14T14:21:57.138Z   200       1231 http://www.carlsencards.com/images/headerNoSpecOcc.gif LE http://www.carlsencards.com/index.htm image/gif #013 20090514142157113+24 sha1:HWF3YHQUOLZWH34RXK6KEERVLIYUXZIC - content-size:1524
2009-05-14T14:21:57.474Z   200       6890 http://www.carlsencards.com/images/extLinks/omg_button.gif LE http://www.carlsencards.com/index.htm image/gif #028 20090514142157443+30 sha1:5NFNEDXIAHQFZVOGZTGCW2TQXFVYXW63 - content-size:7184
2009-05-14T14:21:57.825Z   200      27194 http://www.carlsencards.com/images/free-ecard-thumb55.jpg LE http://www.carlsencards.com/index.htm image/jpeg #010 20090514142157783+40 sha1:LDXB4FIQIRVA2QUK5PG2V3A2MS3GRFPG - content-size:27490
2009-05-14T14:21:58.179Z   200      39614 http://www.carlsencards.com/images/free-ecard-thumb56.jpg LE http://www.carlsencards.com/index.htm image/jpeg #033 20090514142158133+44 sha1:AXVN6CASVGLI5AVGUYCWMD7WFHURAONY - content-size:39910
2009-05-14T14:21:58.505Z   200        123 http://www.carlsencards.com/images/mainTile.gif LE http://www.carlsencards.com/index.htm image/gif #028 20090514142158483+21 sha1:GSKVU6DKSDA4LXXQI5DFR6D5G2NXFRV5 - content-size:414
2009-05-14T14:21:58.854Z   200      26743 http://www.carlsencards.com/images/free-ecard-thumb34.jpg LE http://www.carlsencards.com/index.htm image/jpeg #031 20090514142158813+40 sha1:PUM2KJ74NOOBIJO5LYHA4K57KAK7Q5T2 - content-size:27039
2009-05-14T14:21:59.187Z   200       1340 http://www.carlsencards.com/images/top4About_up.gif LE http://www.carlsencards.com/index.htm image/gif #014 20090514142159163+23 sha1:P53ZCQ4T7FIFOJDZ66YGVAWX3MHIBUOW - content-size:1633
2009-05-14T14:21:59.533Z   200      13369 http://www.carlsencards.com/images/seatFillers/sleepingCat.swf LE http://www.carlsencards.com/index.htm application/x-shockwave-flash #028 20090514142159493+37 sha1:2T4KYAEO3IUZUEV2J2SPAC7PHWR4GU2I - content-size:13684
2009-05-14T14:21:59.883Z   200      24969 http://www.carlsencards.com/images/free-ecard-thumb14.jpg LE http://www.carlsencards.com/index.htm image/jpeg #031 20090514142159843+39 sha1:SXGFUP5GBJC24MPXA6OZWE7KOFW3BOYY - content-size:25265
2009-05-14T14:22:00.216Z   200       1684 http://www.carlsencards.com/images/top6Select_over.gif LX http://www.carlsencards.com/index.htm image/gif #030 20090514142200193+22 sha1:XA5ZKO3FLI774ZWP7NUG357RYAEUIK55 - content-size:1977
2009-05-14T14:22:00.548Z   200        306 http://www.carlsencards.com/images/top7.gif LE http://www.carlsencards.com/index.htm image/gif #028 20090514142200523+23 sha1:2ZA2XXDVHLXYB5WQMBKTS4S5IHKM44PU - content-size:598
2009-05-14T14:22:00.916Z   200      37856 http://www.carlsencards.com/images/seatFillers/freeTheFliesGame.swf LE http://www.carlsencards.com/index.htm application/x-shockwave-flash #048 20090514142200865+43 sha1:RUIUNHEAL4AB2C3FGDX7OW7FKP37TN5K - content-size:38171
2009-05-14T14:22:01.248Z   200       1348 http://www.carlsencards.com/images/top4About_over.gif LX http://www.carlsencards.com/index.htm image/gif #012 20090514142201223+24 sha1:UAMZ26XMYX6AHTN744UF73CKZTGS6ZRZ - content-size:1641
2009-05-14T14:22:01.576Z   200       1254 http://www.carlsencards.com/images/headerHappyAnniversary.gif LE http://www.carlsencards.com/index.htm image/gif #037 20090514142201553+22 sha1:6OD4G226YNCRD6XQ6QZF5O54HGCHHCMH - content-size:1547
2009-05-14T14:22:01.931Z   200      31311 http://www.carlsencards.com/images/free-ecard-thumb54.jpg LE http://www.carlsencards.com/index.htm image/jpeg #031 20090514142201883+46 sha1:CGJXS7GMKMOGKCC326IZMIEVFUAVZZPJ - content-size:31607
2009-05-14T14:22:02.258Z   200       1203 http://www.carlsencards.com/images/extLinks/ofree_mini_3.gif LE http://www.carlsencards.com/index.htm image/gif #012 20090514142202233+24 sha1:WNRYCHO72DRW6A56UXYCYEZW7N2ELMU6 - content-size:1496
2009-05-14T14:22:02.586Z   200       1668 http://www.carlsencards.com/images/top6Select_up.gif LE http://www.carlsencards.com/index.htm image/gif #028 20090514142202563+22 sha1:BNL3P6CQTYEZKT6DP74UTA2LPRRHVL2T - content-size:1961
2009-05-14T14:22:02.920Z   200       1815 http://www.carlsencards.com/images/headerNewBaby.gif LE http://www.carlsencards.com/index.htm image/gif #048 20090514142202893+25 sha1:5NK5R43JIMAISLJAYPAMEE3KSS2RP6F3 - content-size:2108
2009-05-14T14:22:03.251Z   200       2955 http://www.carlsencards.com/images/extLinks/allfreethings_88.gif LE http://www.carlsencards.com/index.htm image/gif #030 20090514142203223+27 sha1:NILIH5Y7VMCTGSDIWKMOICJY54JBQIYX - content-size:3248
2009-05-14T14:22:03.613Z   200      27136 http://www.carlsencards.com/images/free-ecard-thumb53.jpg LE http://www.carlsencards.com/index.htm image/jpeg #028 20090514142203563+49 sha1:2OQ4A3ZQ75DUF6TR6UYZEI5GDGWJCSJ5 - content-size:27432
2009-05-14T14:22:03.954Z   200       3233 http://www.carlsencards.com/Scripts/AC_RunActiveContent.js LE http://www.carlsencards.com/index.htm application/x-javascript #031 20090514142203923+27 sha1:FCFAL2UKP7GGPHR2TJ7J4U4QIJS76XEY - content-size:3541
2009-05-14T14:22:04.325Z   200      33974 http://www.carlsencards.com/images/free-ecard-thumb28.jpg LE http://www.carlsencards.com/index.htm image/jpeg #012 20090514142204263+60 sha1:QYJPAFLXY25FNTWBENSFVCKKGQMWIIYE - content-size:34270
2009-05-14T14:22:04.658Z   200        818 http://www.carlsencards.com/images/headerILoveYou.gif LE http://www.carlsencards.com/index.htm image/gif #006 20090514142204634+22 sha1:GGMSV5BJY6MUJ5XILSFL3WN6TAS4QF7D - content-size:1110
2009-05-14T14:22:05.004Z   200      26549 http://www.carlsencards.com/images/free-ecard-thumb27.jpg LE http://www.carlsencards.com/index.htm image/jpeg #040 20090514142204963+40 sha1:VLOYZJFAFCKFQHEJKIGBOFIWK2IKWF66 - content-size:26845
2009-05-14T14:22:05.354Z   200      24622 http://www.carlsencards.com/images/free-ecard-thumb22.jpg LE http://www.carlsencards.com/index.htm image/jpeg #012 20090514142205313+40 sha1:3ATP2FR6PJ7S564Y3UEP23DN7VBS7FKY - content-size:24918
2009-05-14T14:22:05.712Z   200      29915 http://www.carlsencards.com/images/free-ecard-thumb59.jpg LE http://www.carlsencards.com/index.htm image/jpeg #006 20090514142205663+47 sha1:7C62ROF35XW4X5R4BYM4UHEJB4ZMUSCS - content-size:30211
2009-05-14T14:22:06.244Z   200      20068 http://www.carlsencards.com/images/free-ecard-thumb19.jpg LE http://www.carlsencards.com/index.htm image/jpeg #047 20090514142206024+137 sha1:TIYUXM5QRQNAPN5WIQ6BRWYK5SNMBZWY - content-size:20364
2009-05-14T14:22:06.617Z   200      41483 http://www.carlsencards.com/images/free-ecard-thumb50.jpg LE http://www.carlsencards.com/index.htm image/jpeg #037 20090514142206573+43 sha1:FAB3NB5LKEB5SV67VTHRRRYTU7MWPVMT - content-size:41779
2009-05-14T14:22:06.964Z   200      25185 http://www.carlsencards.com/images/free-ecard-thumb42.jpg LE http://www.carlsencards.com/index.htm image/jpeg #048 20090514142206923+39 sha1:2SAQVZACY6A2LJLMKO2SHMMAFNT2WF37 - content-size:25481
2009-05-14T14:22:07.296Z   200       1937 http://www.carlsencards.com/images/headerWontbecomingtowork.gif LE http://www.carlsencards.com/index.htm image/gif #047 20090514142207273+22 sha1:C7ZCVGOR2VIOYKC5LHDY5JXCBFFJQBXL - content-size:2230
2009-05-14T14:22:07.642Z   200      20501 http://www.carlsencards.com/images/free-ecard-thumb37.jpg LE http://www.carlsencards.com/index.htm image/jpeg #037 20090514142207603+37 sha1:OJDGIV5R6FVLHWBOA6RECAIJOCMWBETD - content-size:20797
2009-05-14T14:22:07.992Z   200      27906 http://www.carlsencards.com/images/free-ecard-thumb39.jpg LE http://www.carlsencards.com/index.htm image/jpeg #048 20090514142207953+38 sha1:XWEXWJQB6DFQ2GNBFXCWBAXQHFI6ZIYM - content-size:28202
2009-05-14T14:22:08.330Z   200        319 http://www.carlsencards.com/images/top3.gif LE http://www.carlsencards.com/index.htm image/gif #012 20090514142208304+25 sha1:VUCO34MR5S7LAOMCD4HUNBRV5UI7EASS - content-size:611
2009-05-14T14:22:08.683Z   200      23333 http://www.carlsencards.com/images/free-ecard-thumb52.jpg LE http://www.carlsencards.com/index.htm image/jpeg #037 20090514142208634+47 sha1:SUID2FW5CBHHAQX5UBP6SIRWFDHIBQRL - content-size:23629
2009-05-14T14:22:09.016Z   200       2542 http://www.carlsencards.com/images/extLinks/small2.gif LE http://www.carlsencards.com/index.htm image/gif #031 20090514142208994+21 sha1:FVZZW5LFIUJGSCLPMQ7PUDSDDRR7WBYE - content-size:2835
2009-05-14T14:22:09.354Z   200      12556 http://www.carlsencards.com/images/free-ecard-thumb24.jpg LE http://www.carlsencards.com/index.htm image/jpeg #012 20090514142209324+29 sha1:N4QGENO4VUY4UHVUMXOH2I43IKIDXOAB - content-size:12852
2009-05-14T14:22:09.693Z   200       9335 http://www.carlsencards.com/images/top2Logo.gif LE http://www.carlsencards.com/index.htm image/gif #028 20090514142209664+27 sha1:733OWHC7LSROYZVCICHF6XOBZQHQWQC2 - content-size:9629
2009-05-14T14:22:10.049Z   200      40297 http://www.carlsencards.com/images/free-ecard-thumb20.jpg LE http://www.carlsencards.com/index.htm image/jpeg #048 20090514142210004+43 sha1:IRQZX4BIENZK2CUGDIZGDKVENVBYUEFV - content-size:40593
2009-05-14T14:22:10.391Z   200      24342 http://www.carlsencards.com/images/free-ecard-thumb47.jpg LE http://www.carlsencards.com/index.htm image/jpeg #012 20090514142210353+37 sha1:CDYO73F6Q5J4ECWKW6YTYZXT5CQC53X6 - content-size:24638
2009-05-14T14:22:10.726Z   200        453 http://www.carlsencards.com/images/top1.gif LE http://www.carlsencards.com/index.htm image/gif #037 20090514142210703+22 sha1:2RAUWFZ7XQLJN7VV75TU5ZQJZZB4EZCT - content-size:745
2009-05-14T14:22:11.062Z   200       1456 http://www.carlsencards.com/images/extLinks/LunarAntics_8031.gif LE http://www.carlsencards.com/index.htm image/gif #048 20090514142211033+28 sha1:EY56IKKIAS7A5CJV3GAKQW3LOQG5DEBJ - content-size:1749
2009-05-14T14:22:11.414Z   200      21123 http://www.carlsencards.com/images/free-ecard-thumb48.jpg LE http://www.carlsencards.com/index.htm image/jpeg #012 20090514142211373+40 sha1:G4MCDVAC6KCKVEAO5SYSP7MPQ5YSX52H - content-size:21419
2009-05-14T14:22:11.746Z   200       3054 http://www.carlsencards.com/images/extLinks/banner2.gif LE http://www.carlsencards.com/index.htm image/gif #037 20090514142211723+22 sha1:XE54VJXMGM4ZSITOIFAAARCF5F33SZRN - content-size:3347
2009-05-14T14:22:12.078Z   200        204 http://www.carlsencards.com/images/campBoxBtm.gif LE http://www.carlsencards.com/index.htm image/gif #040 20090514142212053+24 sha1:HWKNSCPQDMXGJ2FU5WPUBI2QT6XAMXEV - content-size:495
2009-05-14T14:22:12.424Z   200      25439 http://www.carlsencards.com/images/free-ecard-thumb31.jpg LE http://www.carlsencards.com/index.htm image/jpeg #012 20090514142212384+39 sha1:VNGVJ5NL5CRDJ4S3E2NVGMGFSLWEURLO - content-size:25735
2009-05-14T14:22:12.776Z   200      23662 http://www.carlsencards.com/images/free-ecard-thumb33.jpg LE http://www.carlsencards.com/index.htm image/jpeg #005 20090514142212734+40 sha1:XSPUCFDCCN2YYUZDG5XWCVAJPAWS734S - content-size:23958
2009-05-14T14:22:13.130Z   200      28710 http://www.carlsencards.com/images/free-ecard-thumb29.jpg LE http://www.carlsencards.com/index.htm image/jpeg #040 20090514142213084+45 sha1:IJWX4DVFVPWM2ETM45ZJUVPUJJNI6VYI - content-size:29006
2009-05-14T14:22:13.456Z   200       1795 http://www.carlsencards.com/images/topStore_up.gif LE http://www.carlsencards.com/index.htm image/gif #022 20090514142213434+21 sha1:TA7ZZTRET2ZUIREMTIW36IM3Z3P56G7G - content-size:2088
2009-05-14T14:22:13.802Z   200      18350 http://www.carlsencards.com/images/free-ecard-thumb04.jpg LE http://www.carlsencards.com/index.htm image/jpeg #037 20090514142213764+36 sha1:7QUFJE5LX7WFGGYOGQSMD46B3FKGAKC7 - content-size:18646
2009-05-14T14:22:14.136Z   200       1010 http://www.carlsencards.com/images/headerHappybirthday.gif LE http://www.carlsencards.com/index.htm image/gif #040 20090514142214114+21 sha1:S6ANFLDKA2326C5GSMHTXFMHYPK52P4J - content-size:1303
2009-05-14T14:22:14.466Z   200        822 http://www.carlsencards.com/images/headerIhateYou.gif LE http://www.carlsencards.com/index.htm image/gif #012 20090514142214444+21 sha1:F2S7TNXU6WSY66VIETXUA72JR7ECMBME - content-size:1114
2009-05-14T14:22:14.797Z   200        859 http://www.carlsencards.com/images/headerThankYou.gif LE http://www.carlsencards.com/index.htm image/gif #005 20090514142214774+22 sha1:E4A54CBVUA5ANPEKWYI3EAXWGFZF2PM6 - content-size:1151
2009-05-14T14:22:15.144Z   200      23021 http://www.carlsencards.com/images/free-ecard-thumb30.jpg LE http://www.carlsencards.com/index.htm image/jpeg #003 20090514142215104+38 sha1:SYVSXQXTZPDXLU7I2AFB7RZRWBKCK2L2 - content-size:23317
2009-05-14T14:22:15.501Z   200      35078 http://www.carlsencards.com/images/free-ecard-thumb49.jpg LE http://www.carlsencards.com/index.htm image/jpeg #012 20090514142215454+45 sha1:BKX66UQRR3PK7X6Y2464XFRHFHIR2ZGN - content-size:35374
2009-05-14T14:22:15.845Z   200      23414 http://www.carlsencards.com/images/free-ecard-thumb58.jpg LE http://www.carlsencards.com/index.htm image/jpeg #006 20090514142215804+39 sha1:NHXBTTSPBRC23ZKDVAJ7LIASQJM7Z47O - content-size:23710
2009-05-14T14:22:16.191Z   200      25561 http://www.carlsencards.com/images/free-ecard-thumb06.jpg LE http://www.carlsencards.com/index.htm image/jpeg #003 20090514142216154+36 sha1:FTLBUDBNLKZQWEVREQTLAU7NC7DRALZK - content-size:25857
2009-05-14T14:22:16.519Z   200        961 http://www.carlsencards.com/images/bottom.gif LE http://www.carlsencards.com/index.htm image/gif #022 20090514142216494+23 sha1:65NMJW62UE4EKQTBBFPKGPHJ5IKGHNX4 - content-size:1253
2009-05-14T14:22:16.868Z   200      16240 http://www.carlsencards.com/images/seatFillers/frontpageCat.swf LE http://www.carlsencards.com/index.htm application/x-shockwave-flash #037 20090514142216824+37 sha1:HYE6AVOIASRIYJSFOESNUO5S5L6QSD5V - content-size:16555
2009-05-14T14:22:17.196Z   200       1088 http://www.carlsencards.com/images/headerSpreadtheword.gif LE http://www.carlsencards.com/index.htm image/gif #003 20090514142217174+21 sha1:BZSGFWL2LNVDNPCSBTZJHXN5VXGIJOVK - content-size:1381
2009-05-14T14:22:17.541Z   200      23311 http://www.carlsencards.com/images/free-ecard-thumb41.jpg LE http://www.carlsencards.com/index.htm image/jpeg #050 20090514142217504+36 sha1:GSVJ7ADE74STHQYLEPEMVAGIPRSB5AXV - content-size:23607
2009-05-14T14:22:17.867Z   200       1474 http://www.carlsencards.com/images/top5Contact_over.gif LX http://www.carlsencards.com/index.htm image/gif #006 20090514142217844+22 sha1:3ENF6M47XANHT4S5ZKVJJQB7HBOXQJB2 - content-size:1767
2009-05-14T14:22:18.219Z   200      31067 http://www.carlsencards.com/images/free-ecard-thumb16.jpg LE http://www.carlsencards.com/index.htm image/jpeg #040 20090514142218174+44 sha1:SDVAUXMRINUPOX76OIAHDIMUYMCF3DNT - content-size:31363
2009-05-14T14:22:18.563Z   200      26806 http://www.carlsencards.com/images/free-ecard-thumb26.jpg LE http://www.carlsencards.com/index.htm image/jpeg #012 20090514142218524+38 sha1:KPZW5TXKJND6D2QRK3A3P2SQ6RVR4RRJ - content-size:27102
2009-05-14T14:22:18.913Z   200      22700 http://www.carlsencards.com/images/newCardTopFrontpg_fortTellR.jpg LE http://www.carlsencards.com/index.htm image/jpeg #006 20090514142218874+38 sha1:ONPR44DDSR5F7PR3M4VGSM3JAPK23GUZ - content-size:22996
2009-05-14T14:22:19.249Z   200       2933 http://www.carlsencards.com/images/extLinks/freesitee.gif LE http://www.carlsencards.com/index.htm image/gif #004 20090514142219224+23 sha1:CSJXAJC5UBJFAS2Y32GJWIGKS6SKUW3H - content-size:3226
2009-05-14T14:22:19.601Z   200      37745 http://www.carlsencards.com/images/newCardTopFrontpgHBFlowers.jpg LE http://www.carlsencards.com/index.htm image/jpeg #012 20090514142219554+45 sha1:GOTG452VF2YSZ2ZOZF3DCUTJHRG3OGGX - content-size:38041
2009-05-14T14:22:19.942Z   200      22988 http://www.carlsencards.com/images/free-ecard-thumb12.jpg LE http://www.carlsencards.com/index.htm image/jpeg #006 20090514142219904+37 sha1:GLHSP5BYHDXQATRBV6W7YTGV2ZIJITUZ - content-size:23284
2009-05-14T14:22:20.303Z   200      37845 http://www.carlsencards.com/images/free-ecard-thumb17.jpg LE http://www.carlsencards.com/index.htm image/jpeg #004 20090514142220254+48 sha1:B6WJVEOUVXBCZPK7C3MQS7YQA3BP6GTT - content-size:38141
2009-05-14T14:22:20.638Z   200       1142 http://www.carlsencards.com/images/headerCongrats.gif LE http://www.carlsencards.com/index.htm image/gif #028 20090514142220614+23 sha1:EEQVPQ4TE3PGBVRMQUY5LPZ6N6VBDSHQ - content-size:1435
2009-05-14T14:22:20.999Z   200      33173 http://www.carlsencards.com/images/free-ecard-thumb40.jpg LE http://www.carlsencards.com/index.htm image/jpeg #006 20090514142220944+54 sha1:WSFZ646L3LZDRKHZCHHITHKFGRLTKRVW - content-size:33469
2009-05-14T14:22:21.326Z   200        203 http://www.carlsencards.com/images/campBoxTop.gif LE http://www.carlsencards.com/index.htm image/gif #004 20090514142221304+21 sha1:EXDSKG2GRCS4LEPFLXCNVBGGQIK4PYXM - content-size:494
2009-05-14T14:22:21.775Z   200        231 http://www.carlsencards.com/images/line.gif LE http://www.carlsencards.com/index.htm image/gif #028 20090514142221634+90 sha1:FFV74LRA7H6G3HJ2UGDBHTZK5Y7EUAOH - content-size:522
2009-05-14T14:22:22.142Z   200      16067 http://www.carlsencards.com/images/free-ecard-thumb03.jpg LE http://www.carlsencards.com/index.htm image/jpeg #031 20090514142222104+36 sha1:V5BGZIG2X7SX4XXMRFQUIFAFEMANJUU6 - content-size:16363
2009-05-14T14:22:22.466Z   200       3083 http://www.carlsencards.com/images/extLinks/top20free88x53PD.gif LE http://www.carlsencards.com/index.htm image/gif #022 20090514142222444+21 sha1:LB76JDQZEMK6IL6PBDRGBVOZXN2YLSOP - content-size:3376
2009-05-14T14:22:22.812Z   200      25944 http://www.carlsencards.com/images/free-ecard-thumb21.jpg LE http://www.carlsencards.com/index.htm image/jpeg #028 20090514142222774+36 sha1:FPKUXFT73B3DWSZ3BZ3KGZUEUEHUSX24 - content-size:26240
2009-05-14T14:22:23.169Z   200      41328 http://www.carlsencards.com/images/free-ecard-thumb15.jpg LE http://www.carlsencards.com/index.htm image/jpeg #031 20090514142223124+44 sha1:3N5BXYTG65PFA2PZVKPWV4E2YKBJXDJW - content-size:41624
2009-05-14T14:22:23.520Z   200      28910 http://www.carlsencards.com/images/free-ecard-thumb35.jpg LE http://www.carlsencards.com/index.htm image/jpeg #050 20090514142223474+45 sha1:2R72SPMVXEZNJOI2DUFTEFJ5JF473G4T - content-size:29206
2009-05-14T14:22:24.634Z   200      30126 http://www.carlsencards.com/images/free-ecard-thumb51.jpg LE http://www.carlsencards.com/index.htm image/jpeg #005 20090514142223824+809 sha1:FBKMFLLL4YM3ASA4JT2HO6ZIIZELZENY - content-size:30422
2009-05-14T14:22:25.503Z   200      26443 http://www.carlsencards.com/images/free-ecard-thumb09.jpg LE http://www.carlsencards.com/index.htm image/jpeg #047 20090514142225454+48 sha1:4YZ2DW7VY62YJVAFFSK4275P45II6Q7L - content-size:26739
2009-05-14T14:22:25.860Z   200      29853 http://www.carlsencards.com/images/free-ecard-thumb11.jpg LE http://www.carlsencards.com/index.htm image/jpeg #028 20090514142225814+45 sha1:XOI3LG4APLIBMCDLSHBLRCB3I35TP7RN - content-size:30149
2009-05-14T14:22:26.212Z   200      30307 http://www.carlsencards.com/images/free-ecard-thumb07.jpg LE http://www.carlsencards.com/index.htm image/jpeg #031 20090514142226164+46 sha1:6I7QTQQZNIU2N3UHOOZVWWJLI4YJDGMS - content-size:30603
2009-05-14T14:22:26.625Z   200      37291 http://www.carlsencards.com/images/free-ecard-thumb10.jpg LE http://www.carlsencards.com/index.htm image/jpeg #050 20090514142226524+100 sha1:QZ3NXPDBKOJO5LOYPAPY3HGOJX6W6U4Y - content-size:37587
2009-05-14T14:22:26.980Z   200      31668 http://www.carlsencards.com/images/free-ecard-thumb08.jpg LE http://www.carlsencards.com/index.htm image/jpeg #037 20090514142226934+45 sha1:IDLAOEM7MQ3VD7UX3E5QIWH6FJSU3265 - content-size:31964
2009-05-14T14:22:27.307Z   200        417 http://www.carlsencards.com/images/campBoxTile.jpg LE http://www.carlsencards.com/index.htm image/jpeg #040 20090514142227284+22 sha1:H7X5DA7BB6MN7TJPQSVCPVARL7N6GF3B - content-size:710
2009-05-14T14:22:27.651Z   200      12060 http://www.carlsencards.com/images/seatFillers/twoCats.swf LE http://www.carlsencards.com/index.htm application/x-shockwave-flash #050 20090514142227614+32 sha1:OYNUMN65YLC77QEVD5Q7GAOQMG7VUH47 - content-size:12375
2009-05-14T14:22:28.000Z   200      31978 http://www.carlsencards.com/images/free-ecard-thumb43.jpg LE http://www.carlsencards.com/index.htm image/jpeg #016 20090514142227954+44 sha1:7RA6V6E3ZVVWTEE4KY7SNNLQ2YJ7AJTU - content-size:32274
2009-05-14T14:22:28.453Z   200      39614 http://www.carlsencards.com/images/free-ecard-thumb44.jpg LE http://www.carlsencards.com/index.htm image/jpeg #003 20090514142228304+148 sha1:AXVN6CASVGLI5AVGUYCWMD7WFHURAONY - content-size:39910
2009-05-14T14:22:29.170Z   200      27027 http://www.carlsencards.com/images/seatFillers/xmasPenguins.swf LE http://www.carlsencards.com/index.htm application/x-shockwave-flash #028 20090514142228764+396 sha1:H6SXDCVMBVEV4NHSWIK6NKTPDACPJRN4 - content-size:27342
2009-05-14T14:22:29.625Z   200      34161 http://www.carlsencards.com/images/free-ecard-thumb57.jpg LE http://www.carlsencards.com/index.htm image/jpeg #050 20090514142229574+50 sha1:FWW7JTZ2KTPNHMRFXUQ64F254IP66Z6M - content-size:34457
2009-05-14T14:22:29.973Z   200      21101 http://www.carlsencards.com/images/free-ecard-thumb00.jpg LE http://www.carlsencards.com/index.htm image/jpeg #037 20090514142229934+38 sha1:IC6VS36FXYLICSYUUU7GFY5VW7HP3JBC - content-size:21397
2009-05-14T14:22:30.360Z   200      31008 http://www.carlsencards.com/images/free-ecard-thumb38.jpg LE http://www.carlsencards.com/index.htm image/jpeg #040 20090514142230284+75 sha1:5ROP654VEO537ONU6CUIDEMRBV6CATL4 - content-size:31304
2009-05-14T14:22:30.691Z   200       1122 http://www.carlsencards.com/images/headerHoliday.gif LE http://www.carlsencards.com/index.htm image/gif #005 20090514142230664+26 sha1:O2KKKEOIC5A2G5S4NBXDTXU6MEJGNKY3 - content-size:1415
2009-05-14T14:22:31.039Z   200      40204 http://www.carlsencards.com/images/free-ecard-thumb46.jpg LE http://www.carlsencards.com/index.htm image/jpeg #016 20090514142230994+44 sha1:HYBLOQZQI6GKCO34R32GTD2UALRDXHI5 - content-size:40500
2009-05-14T14:22:31.391Z   200      33962 http://www.carlsencards.com/images/free-ecard-thumb32.jpg LE http://www.carlsencards.com/index.htm image/jpeg #040 20090514142231345+44 sha1:PFSVOXBCYFKWWJP2IF4RAGEIJNBF336Z - content-size:34258
2009-05-14T14:22:31.748Z   200      21987 http://www.carlsencards.com/images/free-ecard-thumb01.jpg LE http://www.carlsencards.com/index.htm image/jpeg #002 20090514142231694+52 sha1:3GLURXCDEWJNT5PT3FJVQC3MBQVISRZI - content-size:22283
2009-05-14T14:22:32.085Z   404        212 http://www.carlsencards.com/outgoing/cafepress LX http://www.carlsencards.com/index.htm text/html #043 20090514142232055+29 sha1:NLUTP7HPNVNEVWNZS77TC5PGKVW6GMD2 - content-size:413
2009-05-14T14:22:32.419Z   200       2055 http://www.carlsencards.com/images/top8_micardsa.gif LE http://www.carlsencards.com/index.htm image/gif #004 20090514142232394+24 sha1:ZQF4BMLTBCSCAQ4YAV54CXXBD7HR2OS4 - content-size:2348
2009-05-14T14:22:32.778Z   200      59871 http://www.carlsencards.com/images/newCardTemplateBtm_hamster.jpg LE http://www.carlsencards.com/index.htm image/jpeg #049 20090514142232724+53 sha1:7SKXBOZLD7TRBKBFIY7PJSWJYHEENEZ4 - content-size:60167
2009-05-14T14:22:33.123Z   200      23784 http://www.carlsencards.com/images/free-ecard-thumb13.jpg LE http://www.carlsencards.com/index.htm image/jpeg #032 20090514142233084+38 sha1:C2GSIZTGQQ73LLNCLQESV64FDESTF5KJ - content-size:24080
2009-05-14T14:22:33.577Z   200       1463 http://www.carlsencards.com/images/top5Contact_up.gif LE http://www.carlsencards.com/index.htm image/gif #025 20090514142233435+141 sha1:RIEXVJ3N7KUPRKPQQSWDNE7M3QQE4YWO - content-size:1756
2009-05-14T14:22:33.906Z   200        729 http://www.carlsencards.com/images/headerSorry.gif LE http://www.carlsencards.com/index.htm image/gif #037 20090514142233884+21 sha1:YHUW3ABL7E54RCRMWZADLIJI6OECK75G - content-size:1021
2009-05-14T14:22:34.255Z   200      24784 http://www.carlsencards.com/images/free-ecard-thumb05.jpg LE http://www.carlsencards.com/index.htm image/jpeg #028 20090514142234215+38 sha1:QHJ7KUASBUTYHUCDOC2VIRWBUOAX5523 - content-size:25080
2009-05-14T14:22:34.623Z   200      53181 http://www.carlsencards.com/images/newCardTemplateBtm_fortTell.jpg LE http://www.carlsencards.com/index.htm image/jpeg #022 20090514142234565+56 sha1:3LZU5TRSFDGY2FIJE7FQEEW5JYZ7Y7GI - content-size:53477
2009-05-14T14:22:34.976Z   200      25475 http://www.carlsencards.com/images/free-ecard-thumb02.jpg LE http://www.carlsencards.com/index.htm image/jpeg #016 20090514142234935+40 sha1:SPSRXBVXYUEJJN4AYVVEDL5CYRWKJLJY - content-size:25771
2009-05-14T14:22:35.365Z   200      15971 http://carlsencards.com/sitemap.htm L http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm text/html #048 20090514142235284+38 sha1:G4K4GCX55X3QAHYG62NQETKUGV5E52YZ - content-size:16266
2009-05-14T14:22:35.698Z   404        223 http://carlsencards.com/application/x-shockwave-flash EX http://carlsencards.com/Scripts/AC_RunActiveContent.js text/html #050 20090514142235674+23 sha1:B77W4WVFI2JIIWG76POARQ7BKKO7B5JT - content-size:424
2009-05-14T14:22:36.027Z   404        207 http://carlsencards.com/pagead/atf.js EX http://pagead2.googlesyndication.com/pagead/show_ads.js text/html #043 20090514142236004+22 sha1:HXBM4EYP5GTNJBRNE4TMWHGUJTIAWBMS - content-size:408
2009-05-14T14:22:36.442Z   404        199 http://carlsencards.com/1.8.1 EX http://pagead2.googlesyndication.com/pagead/show_ads.js text/html #048 20090514142236334+107 sha1:C3OKZQBIJ7UXFOEMN6DUOGKTEPTP7O7I - content-size:400
2009-05-14T14:22:36.779Z   404        223 http://carlsencards.com/ShockwaveFlash.ShockwaveFlash EX http://pagead2.googlesyndication.com/pagead/show_ads.js text/html #012 20090514142236755+23 sha1:C7U7YSJVHPBRKXHHE6JDWHVVNZEJ6H22 - content-size:424
2009-05-14T14:22:37.156Z   404        210 http://carlsencards.com/gampad/cookie.js?callback=_GA_googleCookieHelper.setCookieInfo EX http://pagead2.googlesyndication.com/pagead/show_ads.js text/html #006 20090514142237085+44 sha1:QMHSYODGRF7EFY6ERFESQY6GJHPRRBA5 - content-size:411
2009-05-14T14:22:37.644Z   404        225 http://carlsencards.com/ShockwaveFlash.ShockwaveFlash.7 EX http://pagead2.googlesyndication.com/pagead/show_ads.js text/html #041 20090514142237485+107 sha1:BMZFJYGWOJ5VNNOXRCMO3XAXYF7IAEQK - content-size:426
2009-05-14T14:22:37.981Z   404        199 http://carlsencards.com/0.005 EX http://pagead2.googlesyndication.com/pagead/show_ads.js text/html #016 20090514142237955+25 sha1:RHQ4PMITVWI4WDTM4HK7ERNACWMEKD7U - content-size:400
2009-05-14T14:22:38.307Z   404        207 http://carlsencards.com/JavaScript1.1 EX http://pagead2.googlesyndication.com/pagead/show_ads.js text/html #028 20090514142238285+21 sha1:6IZN56JAFBSYYLM244LJVXUGJV4K3YNM - content-size:408
2009-05-14T14:22:38.637Z   404        198 http://carlsencards.com/0.01 EX http://pagead2.googlesyndication.com/pagead/show_ads.js text/html #025 20090514142238615+21 sha1:474BKJST5VPCLBW24QWPSE43ZYN5I5EQ - content-size:399
2009-05-14T14:22:38.968Z   404        197 http://carlsencards.com/1.9 EX http://pagead2.googlesyndication.com/pagead/show_ads.js text/html #037 20090514142238945+22 sha1:KBR76YEOKK5V5IHAY3NHVAA7B3NGISWA - content-size:398
2009-05-14T14:22:39.297Z   404        198 http://carlsencards.com/9.50 EX http://pagead2.googlesyndication.com/pagead/show_ads.js text/html #028 20090514142239275+21 sha1:YQOJE5AYOIGD3GBMKSPGQTHLWF5GYJYF - content-size:399
2009-05-14T14:22:39.627Z   404        225 http://carlsencards.com/ShockwaveFlash.ShockwaveFlash.6 EX http://pagead2.googlesyndication.com/pagead/show_ads.js text/html #030 20090514142239605+21 sha1:2UWEUTPGXHDZOP5ERWRB6C6Q7FYDEYIH - content-size:426
2009-05-14T14:22:39.957Z   404        215 http://carlsencards.com/pagead/test_domain.js EX http://pagead2.googlesyndication.com/pagead/show_ads.js text/html #037 20090514142239935+21 sha1:VNSKOJFRTRHOD3FDWXV7H63SRO4UH34V - content-size:416
2009-05-14T14:22:40.288Z   404        205 http://carlsencards.com/_root.card_ EX http://carlsencards.com/Ecard38.swf text/html #028 20090514142240264+23 sha1:NI6VE4CBCLMKK2K7MKPZ64ORE7XTCGTW - content-size:406
2009-05-14T14:22:40.633Z   200          6 http://carlsencards.com/notify2.php EX http://carlsencards.com/Ecard38.swf text/html #044 20090514142240595+36 sha1:33JIARWYMOT56TH5SVI4RR7DRN5DWK3E - content-size:205
2009-05-14T14:22:40.977Z   403        205 http://www.carlsencards.com/dBText/ EX http://carlsencards.com/Ecard38.swf text/html #037 20090514142240945+31 sha1:XLDJ7NOHTVIY3N3JQGTM2P3MWB24G26C - content-size:406
2009-05-14T14:22:41.378Z   200     153651 http://carlsencards.com/EcardFooter.swf EX http://carlsencards.com/Ecard38.swf application/x-shockwave-flash #028 20090514142241285+83 sha1:UOY4YJMYPCB55BW3PKISM4PSVSWNZO5L - content-size:153968
2009-05-14T14:22:41.761Z   200      21678 http://carlsencards.com/cat-free-ecards-congratulations.htm LL http://carlsencards.com/index.htm text/html #022 20090514142241685+36 sha1:FPCYRRGCYMQQB5I53I5GCPIYAUGJMW55 - content-size:21973
2009-05-14T14:22:42.119Z   200      12848 http://carlsencards.com/free-ecard-49-no-special-occasion-sleep-e-card.htm LL http://carlsencards.com/index.htm text/html #043 20090514142242065+27 sha1:JBZN3KGSIYQDVME74VPO3A73J4NKSZHI - content-size:13143
2009-05-14T14:22:42.708Z   200     652154 http://carlsencards.com/Ecard49.swf LLE http://carlsencards.com/free-ecard-49-no-special-occasion-sleep-e-card.htm application/x-shockwave-flash #040 20090514142242425+238 sha1:2EAE6VSOUMOEA4YB36W3JWSDPZA37WNQ - content-size:652471
2009-05-14T14:22:43.071Z   200      12753 http://carlsencards.com/free-ecard-04-im-sorry-dog-interactive.htm LL http://carlsencards.com/index.htm text/html #016 20090514142243015+31 sha1:ZHZIA5QP6QI6RV5GJLXSUEB6XYH5KDOO - content-size:13048
2009-05-14T14:22:44.073Z   200      21042 http://carlsencards.com/cat-free-ecards-happy-birthday.htm LL http://carlsencards.com/index.htm text/html #037 20090514142243995+36 sha1:2CZ6XZ26PZKAG3ESVIEK37JCSFFVYIDG - content-size:21337
2009-05-14T14:22:44.456Z   200      13058 http://carlsencards.com/free-ecard-43-happy-fathers-day-mixer-board.htm LL http://carlsencards.com/index.htm text/html #031 20090514142244386+29 sha1:YTZJLSNMKKU32WIABLYYTJUKJVCVWF33 - content-size:13353
2009-05-14T14:22:44.819Z   200      12961 http://carlsencards.com/free-ecard-35-happy-valentines-day-jungle-love-lemurs.htm LL http://carlsencards.com/index.htm text/html #050 20090514142244765+28 sha1:KJ5VXNFTSFL5CBBUDDSSNEPXKFE2DMBC - content-size:13256
2009-05-14T14:22:45.191Z   200      12791 http://carlsencards.com/free-ecard-22-i-hate-you-cat-latin-te-odio.htm LL http://carlsencards.com/index.htm text/html #043 20090514142245125+27 sha1:HPBOCJQPYHRGSUDKSSSFEAU4Y5NKTTFN - content-size:13086
2009-05-14T14:22:45.568Z   200      20700 http://carlsencards.com/cat-free-ecards-no-special-occasion.htm LL http://carlsencards.com/index.htm text/html #048 20090514142245495+37 sha1:7BBSD3JNKWNEJA6DPNMPJT6NX34YXTW3 - content-size:20995
2009-05-14T14:22:45.932Z   200      12977 http://carlsencards.com/free-ecard-33-happy-valentines-day-funny-cats.htm LL http://carlsencards.com/index.htm text/html #002 20090514142245875+31 sha1:HOVVQTAAN6VKMXVGANW3D6L3IBNNS3FW - content-size:13272
2009-05-14T14:22:46.286Z   200      12759 http://carlsencards.com/free-ecard-05-happy-birthday-postcard-coffee.htm LL http://carlsencards.com/index.htm text/html #001 20090514142246235+26 sha1:LMIFRHO7UZSSZZXTJHZQ3FRU4DPZVG56 - content-size:13054
2009-05-14T14:22:46.845Z   200     560226 http://carlsencards.com/Ecard5.swf LLE http://carlsencards.com/free-ecard-05-happy-birthday-postcard-coffee.htm application/x-shockwave-flash #034 20090514142246595+233 sha1:LVYDDXAXH4HNI37MZXYSTIF5SRWP5KFS - content-size:560543
2009-05-14T14:22:47.207Z   200      12825 http://carlsencards.com/free-ecard-37-congratulations-bored-funny-cats.htm LL http://carlsencards.com/index.htm text/html #043 20090514142247155+28 sha1:QJB7TEE5YQ7Z2YHY352PWKF6OJJERWIS - content-size:13120
2009-05-14T14:22:48.254Z   200     668732 http://carlsencards.com/Ecard37.swf LLE http://carlsencards.com/free-ecard-37-congratulations-bored-funny-cats.htm application/x-shockwave-flash #016 20090514142247955+273 sha1:SHJBUHF2KOA3OCI2ZHHYVGM5WE56T5ZG - content-size:669049
2009-05-14T14:22:48.605Z   200      13369 http://carlsencards.com/images/seatFillers/sleepingCat.swf LL http://carlsencards.com/index.htm application/x-shockwave-flash #004 20090514142248565+37 sha1:2T4KYAEO3IUZUEV2J2SPAC7PHWR4GU2I - content-size:13684
2009-05-14T14:22:49.640Z   200      17328 http://carlsencards.com/cat-free-ecards-i-love-you.htm LL http://carlsencards.com/index.htm text/html #048 20090514142249575+36 sha1:MK2X4UOC6NWY4SNG4RWLWMBBKJA6IEO4 - content-size:17623
2009-05-14T14:22:49.997Z   200      12826 http://carlsencards.com/free-ecard-12-im-sorry-little-bear-big-words.htm LL http://carlsencards.com/index.htm text/html #002 20090514142249945+29 sha1:R667YUVYSI54CO5ZTKX6HYOU6ZRNOTEM - content-size:13121
2009-05-14T14:22:50.539Z   200     564357 http://carlsencards.com/Ecard12.swf LLE http://carlsencards.com/free-ecard-12-im-sorry-little-bear-big-words.htm application/x-shockwave-flash #001 20090514142250305+218 sha1:QGANSZLD6RYBAA72BPPYKZ4G53NZMOYR - content-size:564674
2009-05-14T14:22:50.896Z   200      12832 http://carlsencards.com/free-ecard-27-happy-thanksgiving-funny-cats.htm LL http://carlsencards.com/index.htm text/html #050 20090514142250845+27 sha1:VUXUO4AORC3LCOSRUTS66FUW2LGUJN4L - content-size:13127
2009-05-14T14:22:51.917Z   200     658126 http://carlsencards.com/Ecard27.swf LLE http://carlsencards.com/free-ecard-27-happy-thanksgiving-funny-cats.htm application/x-shockwave-flash #013 20090514142251655+249 sha1:P5JLZNJUDNFZKIXL4J22KKLPSE7KFSF7 - content-size:658443
2009-05-14T14:22:52.289Z   200      15807 http://carlsencards.com/cat-free-ecards-i-wont-be-coming-to-work-today.htm LL http://carlsencards.com/index.htm text/html #043 20090514142252225+38 sha1:QUJYJDHQWBWPYAFBAR7BPI6UX6YJY6JS - content-size:16102
2009-05-14T14:22:53.303Z   200      12905 http://carlsencards.com/free-ecard-34-happy-valentines-day-romantic-mice.htm LL http://carlsencards.com/index.htm text/html #043 20090514142253215+62 sha1:Z6AHHHZ4ET6YNCMIDC7WHANCYOLLTXNL - content-size:13200
2009-05-14T14:22:53.690Z   200      12769 http://carlsencards.com/free-ecard-13-spread-the-word-make-a-difference-cat-mouse.htm LL http://carlsencards.com/index.htm text/html #020 20090514142253616+29 sha1:RSYMWFXZFGKESBY5QJZNK46JRUBVR6DP - content-size:13064
2009-05-14T14:22:54.048Z   200      12821 http://carlsencards.com/free-ecard-14-get-well-cat-flower-medicine.htm LL http://carlsencards.com/index.htm text/html #002 20090514142253995+30 sha1:RHSYS5RRIJDBPITTOLH2N6UB66BJLFO4 - content-size:13116
2009-05-14T14:22:54.578Z   200     517114 http://carlsencards.com/Ecard14.swf LLE http://carlsencards.com/free-ecard-14-get-well-cat-flower-medicine.htm application/x-shockwave-flash #010 20090514142254355+207 sha1:K7BWB7ELELLUIYOJ56AL4HR27AO7P3NV - content-size:517431
2009-05-14T14:22:54.938Z   200      12841 http://carlsencards.com/free-ecard-07-happy-birthday-devil-angel-directors-cut.htm LL http://carlsencards.com/index.htm text/html #005 20090514142254885+29 sha1:BF76XOLKMHBYF5QBD5VXZYRID73VI7F7 - content-size:13136
2009-05-14T14:22:55.889Z   200     772909 http://carlsencards.com/Ecard7.swf LLE http://carlsencards.com/free-ecard-07-happy-birthday-devil-angel-directors-cut.htm application/x-shockwave-flash #015 20090514142255595+276 sha1:HUCZAEHSVCO53EUFWYVQW4YHSQ4LZNJI - content-size:773226
2009-05-14T14:22:56.248Z   200      12851 http://carlsencards.com/free-ecard-03-wont-come-to-work-mona-lisa.htm LL http://carlsencards.com/index.htm text/html #037 20090514142256195+28 sha1:ZPSJXL2TW6VIYTQM4IZG6CQPCCWQ6VZI - content-size:13146
2009-05-14T14:22:57.677Z   200     574587 http://carlsencards.com/Ecard3.swf LLE http://carlsencards.com/free-ecard-03-wont-come-to-work-mona-lisa.htm application/x-shockwave-flash #028 20090514142257415+232 sha1:WLIZ2VVUYST4XLLUNJT5QSAXWAYIAIUB - content-size:574904
2009-05-14T14:22:58.037Z   200      12770 http://carlsencards.com/free-ecard-19-merry-christmas-penguins-writing-snow.htm LL http://carlsencards.com/index.htm text/html #049 20090514142257985+28 sha1:KOMECRDOGVZX5C3KYMHHD4DFSGPQDWRJ - content-size:13065
2009-05-14T14:22:58.882Z   200      12790 http://carlsencards.com/free-ecard-20-happy-easter-eggs-jurassic.htm LL http://carlsencards.com/index.htm text/html #047 20090514142258816+29 sha1:6HN7WEIGJ4BIIED2MZZ56FZ3Z3ECE3K5 - content-size:13085
2009-05-14T14:22:59.236Z   200      12832 http://carlsencards.com/free-ecard-55-happy-anniversary-lemurs.htm LL http://carlsencards.com/index.htm text/html #037 20090514142259185+29 sha1:MSIK7DDMIIBBJDZO33OJQAURCR5SH6TW - content-size:13127
2009-05-14T14:22:59.837Z   200     647852 http://carlsencards.com/Ecard55.swf LLE http://carlsencards.com/free-ecard-55-happy-anniversary-lemurs.htm application/x-shockwave-flash #031 20090514142259545+265 sha1:YAPCWUGDDP5O4BUS2V6VW7KE2WJ4MNKH - content-size:648169
2009-05-14T14:23:00.197Z   200      37856 http://carlsencards.com/images/seatFillers/freeTheFliesGame.swf LL http://carlsencards.com/index.htm application/x-shockwave-flash #037 20090514142300145+46 sha1:RUIUNHEAL4AB2C3FGDX7OW7FKP37TN5K - content-size:38171
2009-05-14T14:23:01.165Z   200      12845 http://carlsencards.com/free-ecard-28-merry-christmas-easter-bunny-santa.htm LL http://carlsencards.com/index.htm text/html #037 20090514142301115+27 sha1:G2DPX42HEG5R4CCEVA5LJDGIPUL45ENX - content-size:13140
2009-05-14T14:23:04.775Z   200     528092 http://carlsencards.com/Ecard28.swf LLE http://carlsencards.com/free-ecard-28-merry-christmas-easter-bunny-santa.htm application/x-shockwave-flash #010 20090514142301476+3269 sha1:55A56E5OSQQVICYGQO63E6Q3PHJQOQKV - content-size:528409
2009-05-14T14:23:05.838Z   200      12929 http://carlsencards.com/free-ecard-57-no-special-occasion-FAPSS.htm LL http://carlsencards.com/index.htm text/html #048 20090514142305786+29 sha1:VQ5AE7JROXRL6HAVF6MN5LQXBHYO62AT - content-size:13224
2009-05-14T14:23:06.371Z   200     437898 http://carlsencards.com/Ecard57.swf LLE http://carlsencards.com/free-ecard-57-no-special-occasion-FAPSS.htm application/x-shockwave-flash #002 20090514142306146+201 sha1:HYOIKRMTKIRNDUGB3KUWTA4T77H5JEA7 - le:IOException@ExtractorSWF,content-size:438215
2009-05-14T14:23:06.726Z   200      12865 http://carlsencards.com/free-ecard-24-no-special-occasion-hi-mouse-pixar.htm LL http://carlsencards.com/index.htm text/html #001 20090514142306676+27 sha1:QP6DSVZ7OFBXLB73KBU6UVAV4TMPVR6W - content-size:13160
2009-05-14T14:23:07.287Z   200      12935 http://carlsencards.com/free-ecard-00-spread-the-word-tell-your-friends.htm LL http://carlsencards.com/index.htm text/html #002 20090514142307236+29 sha1:POGNYUZ4W4F25ZN4IZXYG7LU5IWCSYR2 - content-size:13230
2009-05-14T14:23:07.933Z   200     901909 http://carlsencards.com/Ecard0.swf LLE http://carlsencards.com/free-ecard-00-spread-the-word-tell-your-friends.htm application/x-shockwave-flash #001 20090514142307596+318 sha1:7XQKIMFXQTSDWUGH74X2VRH27FE5QKIT - content-size:902226
2009-05-14T14:23:08.322Z   -61          - http://www.adobe.com/images/shared/download_buttons/get_flash_player.gif LLE http://carlsencards.com/free-ecard-59-happy-birthday-flowers-rat.htm no-type #043 - - - -
2009-05-14T14:23:08.323Z   200      13854 http://carlsencards.com/free-ecard-59-happy-birthday-flowers-rat.htm LL http://carlsencards.com/index.htm text/html #002 20090514142308256+39 sha1:I4PATQIGMTKBNKUXMC4W3IAZ363KA3FW - content-size:14149
2009-05-14T14:23:09.756Z   200      21696 http://carlsencards.com/Scripts/swfobject_modified.js LLE http://carlsencards.com/free-ecard-59-happy-birthday-flowers-rat.htm application/x-javascript #028 20090514142309706+41 sha1:AWGYM6HKXI6BBR5YJU7ED5AGHUQY56UB - content-size:22006
2009-05-14T14:23:10.119Z   200      12845 http://carlsencards.com/free-ecard-08-i-hate-you-cat-kiss-my-ass.htm LL http://carlsencards.com/index.htm text/html #034 20090514142310066+31 sha1:GRIVWUKCVFERNNB33MDD75VSO43FN3PK - content-size:13140
2009-05-14T14:23:10.641Z   200     427099 http://carlsencards.com/Ecard8.swf LLE http://carlsencards.com/free-ecard-08-i-hate-you-cat-kiss-my-ass.htm application/x-shockwave-flash #032 20090514142310426+204 sha1:D4TXO4HZVXWUCHBG7K43ONTK4NDDVCEG - content-size:427416
2009-05-14T14:23:10.998Z   200      12877 http://carlsencards.com/free-ecard-29-merry-christmas-cat-mice-rudolph-prank.htm LL http://carlsencards.com/index.htm text/html #001 20090514142310946+30 sha1:GNMSN5P5P65VISBG6PGIKOJHI6TEUJO5 - content-size:13172
2009-05-14T14:23:11.539Z   200      12741 http://carlsencards.com/free-ecard-54-happy-anniversary-mice.htm LL http://carlsencards.com/index.htm text/html #006 20090514142311486+31 sha1:ZTL4JFYZFQNMWMTO7AOOZG3QCLGL7K55 - content-size:13036
2009-05-14T14:23:12.158Z   200     742942 http://carlsencards.com/Ecard54.swf LLE http://carlsencards.com/free-ecard-54-happy-anniversary-mice.htm application/x-shockwave-flash #020 20090514142311846+299 sha1:S5TVCFIG7IBS2RGYP7HPVH7YP4PLBJ7I - content-size:743259
2009-05-14T14:23:12.510Z   200      27027 http://carlsencards.com/images/seatFillers/xmasPenguins.swf LL http://carlsencards.com/index.htm application/x-shockwave-flash #006 20090514142312466+36 sha1:H6SXDCVMBVEV4NHSWIK6NKTPDACPJRN4 - content-size:27342
2009-05-14T14:23:13.887Z   200      12885 http://carlsencards.com/free-ecard-23-no-special-occasion-cosmopolitan-recipe.htm LL http://carlsencards.com/index.htm text/html #032 20090514142313616+249 sha1:QWLCLMIMODYLAIBUKAWPFMFA2ZK2AXXA - content-size:13180
2009-05-14T14:23:14.506Z   200     734327 http://carlsencards.com/Ecard23.swf LLE http://carlsencards.com/free-ecard-23-no-special-occasion-cosmopolitan-recipe.htm application/x-shockwave-flash #037 20090514142314196+292 sha1:LIPAW7R7C3QVM6BOTRWAF443GDYEA2JQ - content-size:734644
2009-05-14T14:23:14.868Z   200      12842 http://carlsencards.com/free-ecard-21-get-well-cheerleader-mice.htm LL http://carlsencards.com/index.htm text/html #024 20090514142314816+29 sha1:L4WY267PNNMKBZM6HRLWWF4W76R6M2F2 - content-size:13137
2009-05-14T14:23:16.196Z   200     561780 http://carlsencards.com/Ecard21.swf LLE http://carlsencards.com/free-ecard-21-get-well-cheerleader-mice.htm application/x-shockwave-flash #030 20090514142315956+227 sha1:3KRSWEVBILH3UGMXOZQIKKGP3BA4X45V - content-size:562097
2009-05-14T14:23:16.558Z   200      12743 http://carlsencards.com/free-ecard-18-i-love-you-cat-pictures-true-love.htm LL http://carlsencards.com/index.htm text/html #037 20090514142316506+30 sha1:FFPBHA6MUTD6OUZURW2TCXCRGFPSBQQN - content-size:13038
2009-05-14T14:23:17.354Z   200      12966 http://carlsencards.com/free-ecard-45-thank-you-dumpster-rat.htm LL http://carlsencards.com/index.htm text/html #002 20090514142317306+27 sha1:6BRGFKX7R45APLPOVCFXM4NJNT3V7VAZ - content-size:13261
2009-05-14T14:23:17.911Z   200     630097 http://carlsencards.com/Ecard45.swf LLE http://carlsencards.com/free-ecard-45-thank-you-dumpster-rat.htm application/x-shockwave-flash #028 20090514142317666+231 sha1:X5PJQHK2ODBIJVRA7LAVASXMR4JW3PJK - content-size:630414
2009-05-14T14:23:18.279Z   200      12775 http://carlsencards.com/free-ecard-56-happy-birthday-fireworks-mice.htm LL http://carlsencards.com/index.htm text/html #049 20090514142318216+27 sha1:KMG5BZ4ZE4TVQWTKN7LWSKB2F2P2632Z - content-size:13070
2009-05-14T14:23:19.513Z   200     781283 http://carlsencards.com/Ecard56.swf LLE http://carlsencards.com/free-ecard-56-happy-birthday-fireworks-mice.htm application/x-shockwave-flash #050 20090514142319156+320 sha1:WXCOHNJE3K62MVFE2AHG67CQQTACWLK5 - content-size:781600
2009-05-14T14:23:19.895Z   200      13067 http://carlsencards.com/free-ecard-44-fourth-of-july-mice-fireworks.htm LL http://carlsencards.com/index.htm text/html #036 20090514142319836+36 sha1:PTVIFCC7K3OH2B5DOZJHBWLEOBJXWYOT - content-size:13362
2009-05-14T14:23:21.114Z   200      12858 http://carlsencards.com/free-ecard-06-happy-birthday-devil-angel-cake.htm LL http://carlsencards.com/index.htm text/html #025 20090514142321056+35 sha1:JFDVYUGSSNI2TIRNJ6F256LLDFQBGBG4 - content-size:13153
2009-05-14T14:23:21.465Z   200      12805 http://carlsencards.com/free-ecard-30-i-hate-you-cat-toy-car-accident.htm LL http://carlsencards.com/index.htm text/html #043 20090514142321416+27 sha1:4RL2K5MXYJ42P76NMOI2T4L6S6OJCZGX - content-size:13100
2009-05-14T14:23:21.827Z   200      12926 http://carlsencards.com/free-ecard-41-happy-mothers-day-polar-bears.htm LL http://carlsencards.com/index.htm text/html #036 20090514142321776+29 sha1:3FEZET7FULV2XVUCZRGMTBYSDNWGWTYG - content-size:13221
2009-05-14T14:23:22.230Z   200      29979 http://carlsencards.com/cat-free-ecards-holiday-greetings.htm LL http://carlsencards.com/index.htm text/html #015 20090514142322136+48 sha1:3AJ7NI6HOEFQ6KDPDWQTQONI3I6WOHQP - content-size:30274
2009-05-14T14:23:22.588Z   200      12805 http://carlsencards.com/free-ecard-16-i-love-you-sheep-rock-my-world.htm LL http://carlsencards.com/index.htm text/html #006 20090514142322536+31 sha1:UHF3CBDLTTV3BULZXXGLDMIA6I3IUZBZ - content-size:13100
2009-05-14T14:23:23.166Z   200     639473 http://carlsencards.com/Ecard16.swf LLE http://carlsencards.com/free-ecard-16-i-love-you-sheep-rock-my-world.htm application/x-shockwave-flash #046 20090514142322896+257 sha1:EIRQHMKCBF3P275X4LUVTY5TUKLXFUY6 - content-size:639790
2009-05-14T14:23:23.529Z   200      12804 http://carlsencards.com/free-ecard-10-congratulations-cat-ebay-elvis-cake.htm LL http://carlsencards.com/index.htm text/html #043 20090514142323476+29 sha1:2763M6F5K5APSQLHVCNXMMB36HUNEXBT - content-size:13099
2009-05-14T14:23:24.548Z   200      12843 http://carlsencards.com/free-ecard-17-wont-come-to-work-scream-existential-angst.htm LL http://carlsencards.com/index.htm text/html #016 20090514142324427+100 sha1:FKE6SPMZJNRUMPMCN3RB7C3PXYOGI7ZW - content-size:13138
2009-05-14T14:23:25.087Z   200     539138 http://carlsencards.com/Ecard17.swf LLE http://carlsencards.com/free-ecard-17-wont-come-to-work-scream-existential-angst.htm application/x-shockwave-flash #039 20090514142324857+217 sha1:T4RHIP54QS7BW6EANQ3EOYLCK553XRM6 - content-size:539455
2009-05-14T14:23:25.446Z   200      12828 http://carlsencards.com/free-ecard-11-congratulations-african-drummer-ants.htm LL http://carlsencards.com/index.htm text/html #002 20090514142325396+28 sha1:FLG7C7W75DEALJ6E5NI2ZCK3LCPGFMTC - content-size:13123
2009-05-14T14:23:26.205Z   200      13106 http://carlsencards.com/free-ecard-31-no-special-occasion-cat-mouse-flash-game.htm LL http://carlsencards.com/index.htm text/html #022 20090514142326157+27 sha1:EJMZIM4BJQEOGGWVQ4QGHI3E3NZMOELW - content-size:13401
2009-05-14T14:23:26.856Z   200     798853 http://carlsencards.com/Ecard31.swf LLE http://carlsencards.com/free-ecard-31-no-special-occasion-cat-mouse-flash-game.htm application/x-shockwave-flash #043 20090514142326516+287 sha1:IQXBCHQU2INNCEQKG6XR4UHW424DFWJJ - content-size:799170
2009-05-14T14:23:27.216Z   200      12836 http://carlsencards.com/free-ecard-25-no-special-occasion-text-messaging-lemurs.htm LL http://carlsencards.com/index.htm text/html #046 20090514142327167+27 sha1:NNECWE2X7DBBNYRSVBEJ5HIMSDL3UV3E - content-size:13131
2009-05-14T14:23:28.658Z   200     492188 http://carlsencards.com/Ecard25.swf LLE http://carlsencards.com/free-ecard-25-no-special-occasion-text-messaging-lemurs.htm application/x-shockwave-flash #002 20090514142328427+219 sha1:R6MS3R4WW4ARBKE6G7J4SUEPYQXPVLWJ - content-size:492505
2009-05-14T14:23:29.018Z   200      12810 http://carlsencards.com/free-ecard-52-new-baby-polar-bears.htm LL http://carlsencards.com/index.htm text/html #023 20090514142328967+28 sha1:KS3A7LWJOYBRXEBNNG5YPIJCNVSFVUWB - content-size:13105
2009-05-14T14:23:29.687Z   200      12739 http://carlsencards.com/free-ecard-15-i-love-you-chameleon-chameleogram.htm LL http://carlsencards.com/index.htm text/html #006 20090514142329637+30 sha1:4O3GLBDZVA5ED6FSUCVUHRYU26JWQOW3 - content-size:13034
2009-05-14T14:23:30.371Z   200     784739 http://carlsencards.com/Ecard15.swf LLE http://carlsencards.com/free-ecard-15-i-love-you-chameleon-chameleogram.htm application/x-shockwave-flash #017 20090514142329997+358 sha1:MNTTNSSO6B6X7XUKNCAIVZXK6BO5DGZC - content-size:785056
2009-05-14T14:23:30.781Z   200      13358 http://carlsencards.com/images/seatFillers/littleMousie.swf LL http://carlsencards.com/index.htm application/x-shockwave-flash #043 20090514142330737+39 sha1:GRH7WLYZZZXEB3N7634EWEAZRPRV5BMY - content-size:13673
2009-05-14T14:23:31.990Z   200      12816 http://carlsencards.com/free-ecard-36-congratulations-interactive-surprise-cats-mice.htm LL http://carlsencards.com/index.htm text/html #036 20090514142331917+39 sha1:LKFPNEMGAFJFADHD5M6OMDLI6LEGH64T - content-size:13111
2009-05-14T14:23:32.671Z   200     808707 http://carlsencards.com/Ecard36.swf LLE http://carlsencards.com/free-ecard-36-congratulations-interactive-surprise-cats-mice.htm application/x-shockwave-flash #005 20090514142332297+353 sha1:4IJXPMAZCCYPP6E7ZS6FS644DSYT34DZ - content-size:809024
2009-05-14T14:23:33.088Z   200      12938 http://carlsencards.com/free-ecard-42-happy-mothers-day-llamas.htm LL http://carlsencards.com/index.htm text/html #027 20090514142333037+28 sha1:SZ4UYSQCYMJXASMP54UAGIHQC4HHQKEN - content-size:13233
2009-05-14T14:23:34.338Z   200      15453 http://carlsencards.com/cat-free-ecards-thank-you.htm LL http://carlsencards.com/index.htm text/html #022 20090514142334267+45 sha1:C7XJORMLUX2VDFN733OGFRPNEB6FLPC5 - content-size:15748
2009-05-14T14:23:34.697Z   200      12840 http://carlsencards.com/free-ecard-02-happy-birthday-piano-playing-mice.htm LL http://carlsencards.com/index.htm text/html #050 20090514142334647+29 sha1:2D6GYZDNYSQMB5QMQIQ32JRQSYDOESPS - content-size:13135
2009-05-14T14:23:35.054Z   200      12758 http://carlsencards.com/free-ecard-01-happy-birthday-bored-cats.htm LL http://carlsencards.com/index.htm text/html #036 20090514142335007+27 sha1:MNXGGU6324VYTPIPAC6PD7AGJHZXKOUR - content-size:13053
2009-05-14T14:23:35.627Z   200     630017 http://carlsencards.com/Ecard1.swf LLE http://carlsencards.com/free-ecard-01-happy-birthday-bored-cats.htm application/x-shockwave-flash #034 20090514142335357+254 sha1:JLRRL6G5OSZZ2YEOLOOTNO2C3XUMBR25 - content-size:630334
2009-05-14T14:23:35.987Z   200      12849 http://carlsencards.com/free-ecard-50-congratulations-pimp-my-ecard.htm LL http://carlsencards.com/index.htm text/html #036 20090514142335938+29 sha1:LFWTHGXPOBQBENWFMNTMY6N6GQK5OXTM - content-size:13144
2009-05-14T14:23:37.220Z   200     767128 http://carlsencards.com/Ecard50.swf LLE http://carlsencards.com/free-ecard-50-congratulations-pimp-my-ecard.htm application/x-shockwave-flash #036 20090514142336877+301 sha1:J6636X2B35KL3Z5DPXKPI4A6VQSNYBDV - le:IOException@ExtractorSWF,content-size:767445
2009-05-14T14:23:37.587Z   200      12774 http://carlsencards.com/free-ecard-09-congratulations-penguins-versus-animator.htm LL http://carlsencards.com/index.htm text/html #034 20090514142337537+29 sha1:RG2FABUZXB2HIRIUMSXPL3M7YJSW32QJ - content-size:13069
2009-05-14T14:23:38.786Z   200      12855 http://carlsencards.com/free-ecard-48-halloween-in-ghost-town.htm LL http://carlsencards.com/index.htm text/html #002 20090514142338737+28 sha1:VWBX6L4TGVZ6ZZ6ILM4AQCGSYJVKIZRU - content-size:13150
2009-05-14T14:23:39.146Z   200      12805 http://carlsencards.com/free-ecard-39-happy-easter-dancing-eggs.htm LL http://carlsencards.com/index.htm text/html #019 20090514142339097+27 sha1:Q6UZSP7UQN32JPJDF5WOSUQH4YM374OI - content-size:13100
2009-05-14T14:23:40.149Z   200     466459 http://carlsencards.com/Ecard39.swf LLE http://carlsencards.com/free-ecard-39-happy-easter-dancing-eggs.htm application/x-shockwave-flash #049 20090514142339457+375 sha1:I6OVSR3W7V2R2XAE7SNG4GJLU5UCD7WO - content-size:466776
2009-05-14T14:23:40.587Z   200      12911 http://carlsencards.com/free-ecard-32-happy-new-year-raccoon-party-fireworks.htm LL http://carlsencards.com/index.htm text/html #034 20090514142340537+29 sha1:VQT2AXKK7KDB4QJ4BHEC3H7JWARE7F7H - content-size:13206
2009-05-14T14:23:41.126Z   200      13020 http://carlsencards.com/free-ecard-46-thank-you-lemur-choir.htm LL http://carlsencards.com/index.htm text/html #029 20090514142341077+27 sha1:3T6A6UDEA4UILNGF3YR6GFIV5CAYHVBJ - content-size:13315
2009-05-14T14:23:41.486Z   200      12908 http://carlsencards.com/free-ecard-47-no-special-occasion-cheerleader-mouse.htm LL http://carlsencards.com/index.htm text/html #020 20090514142341437+28 sha1:LLAMVQLW3XILTL2MGIAWRB7AENXQ4DAZ - content-size:13203
2009-05-14T14:23:41.855Z   200      15743 http://carlsencards.com/cat-free-ecards-happy-anniversary.htm LL http://carlsencards.com/index.htm text/html #006 20090514142341797+35 sha1:IEHRDWR5P6J23GCHYTONRBDDTFUBDNXH - content-size:16038
2009-05-14T14:23:42.217Z   200      12807 http://carlsencards.com/free-ecard-53-new-baby-llamas.htm LL http://carlsencards.com/index.htm text/html #049 20090514142342167+29 sha1:KA4GM2K6MAAOSMPFJFQMRILKG5R5EEA3 - content-size:13102
2009-05-14T14:23:42.739Z   200     517872 http://carlsencards.com/Ecard53.swf LLE http://carlsencards.com/free-ecard-53-new-baby-llamas.htm application/x-shockwave-flash #034 20090514142342527+199 sha1:K33SDPS3UE2LOW24LUD3GTDZJQDRB7UX - content-size:518189
2009-05-14T14:23:43.106Z   200      16011 http://carlsencards.com/cat-free-ecards-spread-the-word.htm LL http://carlsencards.com/index.htm text/html #024 20090514142343047+36 sha1:ESUDTCPLQKKLUS22K64CD5QXREFRCLLC - content-size:16306
2009-05-14T14:23:43.886Z   200      15575 http://carlsencards.com/cat-free-ecards-sorry.htm LL http://carlsencards.com/index.htm text/html #005 20090514142343767+84 sha1:YLOXINCIYLD7KWBD2S2JCW2VAMAO6WZY - content-size:15870
2009-05-14T14:23:44.262Z   200      13075 http://carlsencards.com/free-ecard-40-no-special-occasion-memory-game-4cards.htm LL http://carlsencards.com/index.htm text/html #003 20090514142344197+31 sha1:IJZVJUOLCDX7LV6XAYNWU7FV7UBSBBAN - content-size:13370
2009-05-14T14:23:44.877Z   200     582268 http://carlsencards.com/Ecard40.swf LLE http://carlsencards.com/free-ecard-40-no-special-occasion-memory-game-4cards.htm application/x-shockwave-flash #016 20090514142344567+275 sha1:OK26KDXW662655CPM2J7W7TUE2WTYQVL - content-size:582585
2009-05-14T14:23:45.252Z   200      15769 http://carlsencards.com/cat-free-ecards-new-baby.htm LL http://carlsencards.com/index.htm text/html #032 20090514142345187+42 sha1:JWUCMFAYOLVPKMBRQA5TCN7BK4CNPNNR - content-size:16064
2009-05-14T14:23:46.084Z   200      12819 http://carlsencards.com/free-ecard-26-halloween-mouse-in-pumpkin-explosion.htm LL http://carlsencards.com/index.htm text/html #024 20090514142346027+37 sha1:FR5NZMGNLDOPRD5S2RHWDV5DTKP2DTMW - content-size:13114
2009-05-14T14:23:46.450Z   200      15684 http://carlsencards.com/cat-free-ecards-get-well-soon.htm LL http://carlsencards.com/index.htm text/html #046 20090514142346387+41 sha1:NQOMAPP2M7YW4SGWY65ZUBD6N7PPKO77 - content-size:15979
2009-05-14T14:23:46.834Z   200      14886 http://carlsencards.com/cat-free-ecards-i-hate-you.htm LL http://carlsencards.com/index.htm text/html #034 20090514142346757+57 sha1:XQEHX5UVL3IWLSU3ZZ7YREA56574LXFW - content-size:15181
2009-05-14T14:23:47.241Z   200     153651 http://carlsencards.com/EcardFooter2.swf LEX http://carlsencards.com/Ecard58.swf application/x-shockwave-flash #004 20090514142347137+100 sha1:UOY4YJMYPCB55BW3PKISM4PSVSWNZO5L - content-size:153968
2009-05-14T14:23:47.581Z   200          6 http://www.carlsencards.com/notify2.php LEX http://carlsencards.com/Ecard58.swf text/html #050 20090514142347547+33 sha1:33JIARWYMOT56TH5SVI4RR7DRN5DWK3E - content-size:205
2009-05-14T14:23:47.939Z   200      13106 http://www.carlsencards.com/free-ecard-31-no-special-occasion-cat-mouse-flash-game.htm LL http://www.carlsencards.com/index.htm text/html #016 20090514142347888+31 sha1:EJMZIM4BJQEOGGWVQ4QGHI3E3NZMOELW - content-size:13401
2009-05-14T14:23:48.306Z   200      13854 http://www.carlsencards.com/free-ecard-59-happy-birthday-flowers-rat.htm LL http://www.carlsencards.com/index.htm text/html #048 20090514142348247+37 sha1:I4PATQIGMTKBNKUXMC4W3IAZ363KA3FW - content-size:14149
2009-05-14T14:23:49.028Z   200     867621 http://www.carlsencards.com/Ecard59.swf LLE http://www.carlsencards.com/free-ecard-59-happy-birthday-flowers-rat.htm application/x-shockwave-flash #034 20090514142348617+372 sha1:NWKL2KDSDOWVT56UVYFGICXNGT6P2D4W - content-size:867938
2009-05-14T14:23:49.464Z   200      21696 http://www.carlsencards.com/Scripts/swfobject_modified.js LLE http://www.carlsencards.com/free-ecard-59-happy-birthday-flowers-rat.htm application/x-javascript #046 20090514142349418+37 sha1:AWGYM6HKXI6BBR5YJU7ED5AGHUQY56UB - content-size:22006
2009-05-14T14:23:50.776Z   200      12807 http://www.carlsencards.com/free-ecard-53-new-baby-llamas.htm LL http://www.carlsencards.com/index.htm text/html #037 20090514142350727+30 sha1:KA4GM2K6MAAOSMPFJFQMRILKG5R5EEA3 - content-size:13102
2009-05-14T14:23:51.136Z   200      12770 http://www.carlsencards.com/free-ecard-19-merry-christmas-penguins-writing-snow.htm LL http://www.carlsencards.com/index.htm text/html #024 20090514142351087+29 sha1:KOMECRDOGVZX5C3KYMHHD4DFSGPQDWRJ - content-size:13065
2009-05-14T14:23:51.497Z   200      13506 http://www.carlsencards.com/images/seatFillers/hollaMouse.swf LL http://www.carlsencards.com/index.htm application/x-shockwave-flash #046 20090514142351447+45 sha1:RRV3LAXCKTV4RCLK2MZT5OMXFYO6IFK4 - content-size:13821
2009-05-14T14:23:51.873Z   200      21678 http://www.carlsencards.com/cat-free-ecards-congratulations.htm LL http://www.carlsencards.com/index.htm text/html #002 20090514142351807+36 sha1:FPCYRRGCYMQQB5I53I5GCPIYAUGJMW55 - content-size:21973
2009-05-14T14:23:52.249Z   200      15971 http://www.carlsencards.com/sitemap.htm LL http://www.carlsencards.com/index.htm text/html #038 20090514142352177+38 sha1:G4K4GCX55X3QAHYG62NQETKUGV5E52YZ - content-size:16266
2009-05-14T14:23:52.607Z   200      12769 http://www.carlsencards.com/free-ecard-13-spread-the-word-make-a-difference-cat-mouse.htm LL http://www.carlsencards.com/index.htm text/html #017 20090514142352558+29 sha1:RSYMWFXZFGKESBY5QJZNK46JRUBVR6DP - content-size:13064
2009-05-14T14:23:53.151Z   200     502582 http://www.carlsencards.com/Ecard13.swf LLE http://www.carlsencards.com/free-ecard-13-spread-the-word-make-a-difference-cat-mouse.htm application/x-shockwave-flash #006 20090514142352917+210 sha1:KRQLDOFUGYSEDJMVXYNDBAZREDOI7NHK - content-size:502899
2009-05-14T14:23:53.505Z   200      13065 http://www.carlsencards.com/contact.htm LL http://www.carlsencards.com/index.htm text/html #046 20090514142353458+27 sha1:MJ4LB45MYP2AEVTAF2IXH7W2JW4YQURO - content-size:13360
2009-05-14T14:23:54.170Z   200      13020 http://www.carlsencards.com/images/contactCat.swf LLE http://www.carlsencards.com/contact.htm application/x-shockwave-flash #006 20090514142354138+27 sha1:62BCBWCUWHBND36NHBIQPPO2WQD233CE - content-size:13335
2009-05-14T14:23:54.502Z   404        211 http://www.carlsencards.com/images/contactCat LLX http://www.carlsencards.com/contact.htm text/html #046 20090514142354478+23 sha1:FQFAYHEKXIXD5WZBMVRVKGIPG3WLJ4UX - content-size:412
2009-05-14T14:23:54.855Z   200      12756 http://www.carlsencards.com/free-ecard-58-congratulations-hamster-on-strike.htm LL http://www.carlsencards.com/index.htm text/html #037 20090514142354808+27 sha1:UISKUXND75DU2QLB5XSH7OZNIXYNHNWI - content-size:13051
2009-05-14T14:23:55.205Z   200      12845 http://www.carlsencards.com/free-ecard-08-i-hate-you-cat-kiss-my-ass.htm LL http://www.carlsencards.com/index.htm text/html #024 20090514142355158+27 sha1:GRIVWUKCVFERNNB33MDD75VSO43FN3PK - content-size:13140
2009-05-14T14:23:55.883Z   200     427099 http://www.carlsencards.com/Ecard8.swf LLE http://www.carlsencards.com/free-ecard-08-i-hate-you-cat-kiss-my-ass.htm application/x-shockwave-flash #046 20090514142355508+366 sha1:D4TXO4HZVXWUCHBG7K43ONTK4NDDVCEG - content-size:427416
2009-05-14T14:23:56.308Z   200      12826 http://www.carlsencards.com/free-ecard-12-im-sorry-little-bear-big-words.htm LL http://www.carlsencards.com/index.htm text/html #021 20090514142356258+29 sha1:R667YUVYSI54CO5ZTKX6HYOU6ZRNOTEM - content-size:13121
2009-05-14T14:23:56.961Z   200     564357 http://www.carlsencards.com/Ecard12.swf LLE http://www.carlsencards.com/free-ecard-12-im-sorry-little-bear-big-words.htm application/x-shockwave-flash #037 20090514142356728+223 sha1:QGANSZLD6RYBAA72BPPYKZ4G53NZMOYR - content-size:564674
2009-05-14T14:23:57.332Z   200      12767 http://www.carlsencards.com/free-ecard-51-happy-birthday-fortune-teller-rat.htm LL http://www.carlsencards.com/index.htm text/html #011 20090514142357268+28 sha1:6WWIG35CG34DBV2WHNDAJKF3QNK3D6XY - content-size:13062
2009-05-14T14:23:58.206Z   200     301349 http://www.carlsencards.com/images/seatFillers/musicRadio.swf LL http://www.carlsencards.com/index.htm application/x-shockwave-flash #034 20090514142358068+127 sha1:IJF23YOY3E2XLX6YL6H6XB3MZLUUXM77 - content-size:301666
2009-05-14T14:23:58.566Z   200      12758 http://www.carlsencards.com/free-ecard-01-happy-birthday-bored-cats.htm LL http://www.carlsencards.com/index.htm text/html #022 20090514142358518+29 sha1:MNXGGU6324VYTPIPAC6PD7AGJHZXKOUR - content-size:13053
2009-05-14T14:23:59.130Z   200     630017 http://www.carlsencards.com/Ecard1.swf LLE http://www.carlsencards.com/free-ecard-01-happy-birthday-bored-cats.htm application/x-shockwave-flash #046 20090514142358878+236 sha1:JLRRL6G5OSZZ2YEOLOOTNO2C3XUMBR25 - content-size:630334
2009-05-14T14:23:59.487Z   200      12805 http://www.carlsencards.com/free-ecard-39-happy-easter-dancing-eggs.htm LL http://www.carlsencards.com/index.htm text/html #041 20090514142359438+28 sha1:Q6UZSP7UQN32JPJDF5WOSUQH4YM374OI - content-size:13100
2009-05-14T14:24:00.587Z   200     466459 http://www.carlsencards.com/Ecard39.swf LLE http://www.carlsencards.com/free-ecard-39-happy-easter-dancing-eggs.htm application/x-shockwave-flash #033 20090514142400378+196 sha1:I6OVSR3W7V2R2XAE7SNG4GJLU5UCD7WO - content-size:466776
2009-05-14T14:24:00.956Z   200      15807 http://www.carlsencards.com/cat-free-ecards-i-wont-be-coming-to-work-today.htm LL http://www.carlsencards.com/index.htm text/html #002 20090514142400898+37 sha1:QUJYJDHQWBWPYAFBAR7BPI6UX6YJY6JS - content-size:16102
2009-05-14T14:24:01.569Z   200      12858 http://www.carlsencards.com/free-ecard-06-happy-birthday-devil-angel-cake.htm LL http://www.carlsencards.com/index.htm text/html #022 20090514142401518+31 sha1:JFDVYUGSSNI2TIRNJ6F256LLDFQBGBG4 - content-size:13153
2009-05-14T14:24:01.929Z   200      12929 http://www.carlsencards.com/free-ecard-57-no-special-occasion-FAPSS.htm LL http://www.carlsencards.com/index.htm text/html #002 20090514142401878+30 sha1:VQ5AE7JROXRL6HAVF6MN5LQXBHYO62AT - content-size:13224
2009-05-14T14:24:02.429Z   200     437898 http://www.carlsencards.com/Ecard57.swf LLE http://www.carlsencards.com/free-ecard-57-no-special-occasion-FAPSS.htm application/x-shockwave-flash #006 20090514142402238+168 sha1:HYOIKRMTKIRNDUGB3KUWTA4T77H5JEA7 - le:IOException@ExtractorSWF,content-size:438215
2009-05-14T14:24:02.785Z   200      12819 http://www.carlsencards.com/free-ecard-26-halloween-mouse-in-pumpkin-explosion.htm LL http://www.carlsencards.com/index.htm text/html #002 20090514142402738+28 sha1:FR5NZMGNLDOPRD5S2RHWDV5DTKP2DTMW - content-size:13114
2009-05-14T14:24:03.600Z   200     785394 http://www.carlsencards.com/Ecard26.swf LLE http://www.carlsencards.com/free-ecard-26-halloween-mouse-in-pumpkin-explosion.htm application/x-shockwave-flash #008 20090514142403288+295 sha1:SZHWFS6PWBVWDUN5CZK77WSX7APKLIUV - content-size:785711
2009-05-14T14:24:03.956Z   200      12935 http://www.carlsencards.com/free-ecard-00-spread-the-word-tell-your-friends.htm LL http://www.carlsencards.com/index.htm text/html #037 20090514142403908+29 sha1:POGNYUZ4W4F25ZN4IZXYG7LU5IWCSYR2 - content-size:13230
2009-05-14T14:24:05.526Z   200     901909 http://www.carlsencards.com/Ecard0.swf LLE http://www.carlsencards.com/free-ecard-00-spread-the-word-tell-your-friends.htm application/x-shockwave-flash #046 20090514142405148+360 sha1:7XQKIMFXQTSDWUGH74X2VRH27FE5QKIT - content-size:902226
2009-05-14T14:24:05.963Z   200      20700 http://www.carlsencards.com/cat-free-ecards-no-special-occasion.htm LL http://www.carlsencards.com/index.htm text/html #037 20090514142405898+38 sha1:7BBSD3JNKWNEJA6DPNMPJT6NX34YXTW3 - content-size:20995
2009-05-14T14:24:07.345Z   200      12977 http://www.carlsencards.com/free-ecard-33-happy-valentines-day-funny-cats.htm LL http://www.carlsencards.com/index.htm text/html #024 20090514142407298+27 sha1:HOVVQTAAN6VKMXVGANW3D6L3IBNNS3FW - content-size:13272
2009-05-14T14:24:07.891Z   200     559671 http://www.carlsencards.com/Ecard33.swf LLE http://www.carlsencards.com/free-ecard-33-happy-valentines-day-funny-cats.htm application/x-shockwave-flash #033 20090514142407648+232 sha1:DASMMA4B6XY5X6D2257QUICUB6EJOT25 - content-size:559988
2009-05-14T14:24:08.247Z   200      12836 http://www.carlsencards.com/free-ecard-25-no-special-occasion-text-messaging-lemurs.htm LL http://www.carlsencards.com/index.htm text/html #034 20090514142408198+29 sha1:NNECWE2X7DBBNYRSVBEJ5HIMSDL3UV3E - content-size:13131
2009-05-14T14:24:09.047Z   200      13058 http://www.carlsencards.com/free-ecard-43-happy-fathers-day-mixer-board.htm LL http://www.carlsencards.com/index.htm text/html #005 20090514142408998+28 sha1:YTZJLSNMKKU32WIABLYYTJUKJVCVWF33 - content-size:13353
2009-05-14T14:24:09.619Z   200     566465 http://www.carlsencards.com/Ecard43.swf LLE http://www.carlsencards.com/free-ecard-43-happy-fathers-day-mixer-board.htm application/x-shockwave-flash #042 20090514142409358+222 sha1:PUAUQPGDUFUCE5VA56H4O6PL4E75YSXZ - content-size:566782
2009-05-14T14:24:09.975Z   200      12753 http://www.carlsencards.com/free-ecard-04-im-sorry-dog-interactive.htm LL http://www.carlsencards.com/index.htm text/html #037 20090514142409928+28 sha1:ZHZIA5QP6QI6RV5GJLXSUEB6XYH5KDOO - content-size:13048
2009-05-14T14:24:10.788Z   200      14886 http://www.carlsencards.com/cat-free-ecards-i-hate-you.htm LL http://www.carlsencards.com/index.htm text/html #030 20090514142410728+39 sha1:XQEHX5UVL3IWLSU3ZZ7YREA56574LXFW - content-size:15181
2009-05-14T14:24:11.295Z   200      13020 http://www.carlsencards.com/free-ecard-46-thank-you-lemur-choir.htm LL http://www.carlsencards.com/index.htm text/html #016 20090514142411099+131 sha1:3T6A6UDEA4UILNGF3YR6GFIV5CAYHVBJ - content-size:13315
2009-05-14T14:24:11.667Z   200      12775 http://www.carlsencards.com/free-ecard-56-happy-birthday-fireworks-mice.htm LL http://www.carlsencards.com/index.htm text/html #042 20090514142411618+29 sha1:KMG5BZ4ZE4TVQWTKN7LWSKB2F2P2632Z - content-size:13070
2009-05-14T14:24:12.306Z   200     781283 http://www.carlsencards.com/Ecard56.swf LLE http://www.carlsencards.com/free-ecard-56-happy-birthday-fireworks-mice.htm application/x-shockwave-flash #037 20090514142411978+294 sha1:WXCOHNJE3K62MVFE2AHG67CQQTACWLK5 - content-size:781600
2009-05-14T14:24:12.656Z   200      12842 http://www.carlsencards.com/free-ecard-21-get-well-cheerleader-mice.htm LL http://www.carlsencards.com/index.htm text/html #041 20090514142412608+29 sha1:L4WY267PNNMKBZM6HRLWWF4W76R6M2F2 - content-size:13137
2009-05-14T14:24:13.889Z   200      12865 http://www.carlsencards.com/free-ecard-24-no-special-occasion-hi-mouse-pixar.htm LL http://www.carlsencards.com/index.htm text/html #002 20090514142413838+32 sha1:QP6DSVZ7OFBXLB73KBU6UVAV4TMPVR6W - content-size:13160
2009-05-14T14:24:14.246Z   200      12759 http://www.carlsencards.com/free-ecard-05-happy-birthday-postcard-coffee.htm LL http://www.carlsencards.com/index.htm text/html #043 20090514142414198+29 sha1:LMIFRHO7UZSSZZXTJHZQ3FRU4DPZVG56 - content-size:13054
2009-05-14T14:24:14.621Z   200      21042 http://www.carlsencards.com/cat-free-ecards-happy-birthday.htm LL http://www.carlsencards.com/index.htm text/html #003 20090514142414548+43 sha1:2CZ6XZ26PZKAG3ESVIEK37JCSFFVYIDG - content-size:21337
2009-05-14T14:24:14.977Z   200      12908 http://www.carlsencards.com/free-ecard-47-no-special-occasion-cheerleader-mouse.htm LL http://www.carlsencards.com/index.htm text/html #033 20090514142414928+29 sha1:LLAMVQLW3XILTL2MGIAWRB7AENXQ4DAZ - content-size:13203
2009-05-14T14:24:15.518Z   200     586842 http://www.carlsencards.com/Ecard47.swf LLE http://www.carlsencards.com/free-ecard-47-no-special-occasion-cheerleader-mouse.htm application/x-shockwave-flash #034 20090514142415288+217 sha1:BAUZXWWOJ5XYROQP5RE6APTVXIZSIXKR - content-size:587159
2009-05-14T14:24:15.889Z   200      15453 http://www.carlsencards.com/cat-free-ecards-thank-you.htm LL http://www.carlsencards.com/index.htm text/html #017 20090514142415828+39 sha1:C7XJORMLUX2VDFN733OGFRPNEB6FLPC5 - content-size:15748
2009-05-14T14:24:16.738Z   200      12741 http://www.carlsencards.com/free-ecard-54-happy-anniversary-mice.htm LL http://www.carlsencards.com/index.htm text/html #041 20090514142416678+40 sha1:ZTL4JFYZFQNMWMTO7AOOZG3QCLGL7K55 - content-size:13036
2009-05-14T14:24:17.368Z   200     742942 http://www.carlsencards.com/Ecard54.swf LLE http://www.carlsencards.com/free-ecard-54-happy-anniversary-mice.htm application/x-shockwave-flash #005 20090514142417048+308 sha1:S5TVCFIG7IBS2RGYP7HPVH7YP4PLBJ7I - content-size:743259
2009-05-14T14:24:17.742Z   200      12805 http://www.carlsencards.com/free-ecard-16-i-love-you-sheep-rock-my-world.htm LL http://www.carlsencards.com/index.htm text/html #022 20090514142417689+32 sha1:UHF3CBDLTTV3BULZXXGLDMIA6I3IUZBZ - content-size:13100
2009-05-14T14:24:19.101Z   200     639473 http://www.carlsencards.com/Ecard16.swf LLE http://www.carlsencards.com/free-ecard-16-i-love-you-sheep-rock-my-world.htm application/x-shockwave-flash #020 20090514142418829+261 sha1:EIRQHMKCBF3P275X4LUVTY5TUKLXFUY6 - content-size:639790
2009-05-14T14:24:19.468Z   200      15684 http://www.carlsencards.com/cat-free-ecards-get-well-soon.htm LL http://www.carlsencards.com/index.htm text/html #024 20090514142419409+37 sha1:NQOMAPP2M7YW4SGWY65ZUBD6N7PPKO77 - content-size:15979
2009-05-14T14:24:20.432Z   200      20931 http://www.carlsencards.com/ecards-and-free-stuff-links.htm LL http://www.carlsencards.com/index.htm text/html #037 20090514142420369+37 sha1:A5RP4VMGI2XCALDS6AAKYDO656SFA5SJ - content-size:21226
2009-05-14T14:24:20.764Z   200       1026 http://www.carlsencards.com/images/extLinks/aford_promobanner_31x88.gif LLE http://www.carlsencards.com/ecards-and-free-stuff-links.htm image/gif #042 20090514142420739+24 sha1:ODZWNUYQLREA3DJSRRVQAFIV7YKLOX7G - content-size:1319
2009-05-14T14:24:21.118Z   200      12877 http://www.carlsencards.com/free-ecard-29-merry-christmas-cat-mice-rudolph-prank.htm LL http://www.carlsencards.com/index.htm text/html #020 20090514142421069+30 sha1:GNMSN5P5P65VISBG6PGIKOJHI6TEUJO5 - content-size:13172
2009-05-14T14:24:21.487Z   200      12774 http://www.carlsencards.com/free-ecard-09-congratulations-penguins-versus-animator.htm LL http://www.carlsencards.com/index.htm text/html #037 20090514142421429+39 sha1:RG2FABUZXB2HIRIUMSXPL3M7YJSW32QJ - content-size:13069
2009-05-14T14:24:22.073Z   200     622030 http://www.carlsencards.com/Ecard9.swf LLE http://www.carlsencards.com/free-ecard-09-congratulations-penguins-versus-animator.htm application/x-shockwave-flash #013 20090514142421799+223 sha1:NLZECFM7MZQUWQFMXPZEWYIMCDY5M2EW - content-size:622347
2009-05-14T14:24:22.436Z   200      14919 http://www.carlsencards.com/about.htm LL http://www.carlsencards.com/index.htm text/html #016 20090514142422379+36 sha1:N3MINUMWMCR3XPMAC4JMPHL2R7Z2VSZV - content-size:15214
2009-05-14T14:24:23.320Z   404        209 http://www.carlsencards.com/images/aboutCat LLX http://www.carlsencards.com/about.htm text/html #043 20090514142423299+20 sha1:O7YNBR4WAVNYDQVGEMIOJEEQROGGHAU5 - content-size:410
2009-05-14T14:24:23.690Z   200      32356 http://www.carlsencards.com/images/aboutCat.swf LLE http://www.carlsencards.com/about.htm application/x-shockwave-flash #006 20090514142423629+46 sha1:JYA2MPNONP6DSGQ6ZCSOLEVH43ZOGUMX - content-size:32671
2009-05-14T14:24:24.055Z   200      12885 http://www.carlsencards.com/free-ecard-23-no-special-occasion-cosmopolitan-recipe.htm LL http://www.carlsencards.com/index.htm text/html #033 20090514142423999+37 sha1:QWLCLMIMODYLAIBUKAWPFMFA2ZK2AXXA - content-size:13180
2009-05-14T14:24:24.409Z   200      12855 http://www.carlsencards.com/free-ecard-48-halloween-in-ghost-town.htm LL http://www.carlsencards.com/index.htm text/html #005 20090514142424359+31 sha1:VWBX6L4TGVZ6ZZ6ILM4AQCGSYJVKIZRU - content-size:13150
2009-05-14T14:24:24.775Z   200      13075 http://www.carlsencards.com/free-ecard-40-no-special-occasion-memory-game-4cards.htm LL http://www.carlsencards.com/index.htm text/html #046 20090514142424719+37 sha1:IJZVJUOLCDX7LV6XAYNWU7FV7UBSBBAN - content-size:13370
2009-05-14T14:24:25.341Z   200     582268 http://www.carlsencards.com/Ecard40.swf LLE http://www.carlsencards.com/free-ecard-40-no-special-occasion-memory-game-4cards.htm application/x-shockwave-flash #013 20090514142425079+241 sha1:OK26KDXW662655CPM2J7W7TUE2WTYQVL - content-size:582585
2009-05-14T14:24:25.708Z   200      15743 http://www.carlsencards.com/cat-free-ecards-happy-anniversary.htm LL http://www.carlsencards.com/index.htm text/html #049 20090514142425649+37 sha1:IEHRDWR5P6J23GCHYTONRBDDTFUBDNXH - content-size:16038
2009-05-14T14:24:26.672Z   200      13067 http://www.carlsencards.com/free-ecard-44-fourth-of-july-mice-fireworks.htm LL http://www.carlsencards.com/index.htm text/html #037 20090514142426489+110 sha1:PTVIFCC7K3OH2B5DOZJHBWLEOBJXWYOT - content-size:13362
2009-05-14T14:24:27.097Z   200      12832 http://www.carlsencards.com/free-ecard-27-happy-thanksgiving-funny-cats.htm LL http://www.carlsencards.com/index.htm text/html #017 20090514142426989+89 sha1:VUXUO4AORC3LCOSRUTS66FUW2LGUJN4L - content-size:13127
2009-05-14T14:24:27.458Z   200      12938 http://www.carlsencards.com/free-ecard-42-happy-mothers-day-llamas.htm LL http://www.carlsencards.com/index.htm text/html #005 20090514142427399+40 sha1:SZ4UYSQCYMJXASMP54UAGIHQC4HHQKEN - content-size:13233
2009-05-14T14:24:27.990Z   200     513935 http://www.carlsencards.com/Ecard42.swf LLE http://www.carlsencards.com/free-ecard-42-happy-mothers-day-llamas.htm application/x-shockwave-flash #046 20090514142427769+211 sha1:TKTPJWQ3TACLUYMCTK7X73J277OEYBPO - content-size:514252
2009-05-14T14:24:28.346Z   200      12851 http://www.carlsencards.com/free-ecard-03-wont-come-to-work-mona-lisa.htm LL http://www.carlsencards.com/index.htm text/html #043 20090514142428299+28 sha1:ZPSJXL2TW6VIYTQM4IZG6CQPCCWQ6VZI - content-size:13146
2009-05-14T14:24:29.237Z   200     574587 http://www.carlsencards.com/Ecard3.swf LLE http://www.carlsencards.com/free-ecard-03-wont-come-to-work-mona-lisa.htm application/x-shockwave-flash #002 20090514142428999+213 sha1:WLIZ2VVUYST4XLLUNJT5QSAXWAYIAIUB - content-size:574904
2009-05-14T14:24:29.609Z   200      12821 http://www.carlsencards.com/free-ecard-14-get-well-cat-flower-medicine.htm LL http://www.carlsencards.com/index.htm text/html #024 20090514142429549+30 sha1:RHSYS5RRIJDBPITTOLH2N6UB66BJLFO4 - content-size:13116
2009-05-14T14:24:30.610Z   200     517114 http://www.carlsencards.com/Ecard14.swf LLE http://www.carlsencards.com/free-ecard-14-get-well-cat-flower-medicine.htm application/x-shockwave-flash #013 20090514142430379+222 sha1:K7BWB7ELELLUIYOJ56AL4HR27AO7P3NV - content-size:517431
2009-05-14T14:24:30.966Z   200      12961 http://www.carlsencards.com/free-ecard-35-happy-valentines-day-jungle-love-lemurs.htm LL http://www.carlsencards.com/index.htm text/html #030 20090514142430919+28 sha1:KJ5VXNFTSFL5CBBUDDSSNEPXKFE2DMBC - content-size:13256
2009-05-14T14:24:31.676Z   200      13160 http://www.carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm LL http://www.carlsencards.com/index.htm text/html #023 20090514142431629+28 sha1:FOZSHUKZVNNCGIBD7DYY3NP3NBVBSKZT - content-size:13455
2009-05-14T14:24:32.356Z   200     854427 http://www.carlsencards.com/Ecard38.swf LLE http://www.carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm application/x-shockwave-flash #050 20090514142431979+327 sha1:HRQG67POJUAH5626L6HHZSWTWQOIQ4T2 - content-size:854744
2009-05-14T14:24:32.736Z   200      12832 http://www.carlsencards.com/free-ecard-55-happy-anniversary-lemurs.htm LL http://www.carlsencards.com/index.htm text/html #034 20090514142432689+28 sha1:MSIK7DDMIIBBJDZO33OJQAURCR5SH6TW - content-size:13127
2009-05-14T14:24:34.077Z   200      12848 http://www.carlsencards.com/free-ecard-49-no-special-occasion-sleep-e-card.htm LL http://www.carlsencards.com/index.htm text/html #046 20090514142434029+29 sha1:JBZN3KGSIYQDVME74VPO3A73J4NKSZHI - content-size:13143
2009-05-14T14:24:34.656Z   200     652154 http://www.carlsencards.com/Ecard49.swf LLE http://www.carlsencards.com/free-ecard-49-no-special-occasion-sleep-e-card.htm application/x-shockwave-flash #043 20090514142434379+246 sha1:2EAE6VSOUMOEA4YB36W3JWSDPZA37WNQ - content-size:652471
2009-05-14T14:24:35.006Z   200      12905 http://www.carlsencards.com/free-ecard-34-happy-valentines-day-romantic-mice.htm LL http://www.carlsencards.com/index.htm text/html #030 20090514142434959+28 sha1:Z6AHHHZ4ET6YNCMIDC7WHANCYOLLTXNL - content-size:13200
2009-05-14T14:24:35.998Z   200      15769 http://www.carlsencards.com/cat-free-ecards-new-baby.htm LL http://www.carlsencards.com/index.htm text/html #012 20090514142435939+37 sha1:JWUCMFAYOLVPKMBRQA5TCN7BK4CNPNNR - content-size:16064
2009-05-14T14:24:36.367Z   200      16011 http://www.carlsencards.com/cat-free-ecards-spread-the-word.htm LL http://www.carlsencards.com/index.htm text/html #050 20090514142436309+37 sha1:ESUDTCPLQKKLUS22K64CD5QXREFRCLLC - content-size:16306
2009-05-14T14:24:36.729Z   200      12926 http://www.carlsencards.com/free-ecard-41-happy-mothers-day-polar-bears.htm LL http://www.carlsencards.com/index.htm text/html #045 20090514142436680+29 sha1:3FEZET7FULV2XVUCZRGMTBYSDNWGWTYG - content-size:13221
2009-05-14T14:24:37.070Z   200      12291 http://www.carlsencards.com/images/seatFillers/penguinJoke.swf LL http://www.carlsencards.com/index.htm application/x-shockwave-flash #046 20090514142437039+27 sha1:QZ4AWBGYNIAXRCPC7ZZ63GM4ZQPYMFIR - content-size:12606
2009-05-14T14:24:37.425Z   200      12841 http://www.carlsencards.com/free-ecard-07-happy-birthday-devil-angel-directors-cut.htm LL http://www.carlsencards.com/index.htm text/html #005 20090514142437379+27 sha1:BF76XOLKMHBYF5QBD5VXZYRID73VI7F7 - content-size:13136
2009-05-14T14:24:38.018Z   200     772909 http://www.carlsencards.com/Ecard7.swf LLE http://www.carlsencards.com/free-ecard-07-happy-birthday-devil-angel-directors-cut.htm application/x-shockwave-flash #027 20090514142437729+272 sha1:HUCZAEHSVCO53EUFWYVQW4YHSQ4LZNJI - content-size:773226
2009-05-14T14:24:38.376Z   200      12825 http://www.carlsencards.com/free-ecard-37-congratulations-bored-funny-cats.htm LL http://www.carlsencards.com/index.htm text/html #050 20090514142438329+28 sha1:QJB7TEE5YQ7Z2YHY352PWKF6OJJERWIS - content-size:13120
2009-05-14T14:24:39.825Z   200     668732 http://www.carlsencards.com/Ecard37.swf LLE http://www.carlsencards.com/free-ecard-37-congratulations-bored-funny-cats.htm application/x-shockwave-flash #016 20090514142439540+269 sha1:SHJBUHF2KOA3OCI2ZHHYVGM5WE56T5ZG - content-size:669049
2009-05-14T14:24:40.195Z   200      15575 http://www.carlsencards.com/cat-free-ecards-sorry.htm LL http://www.carlsencards.com/index.htm text/html #033 20090514142440130+35 sha1:YLOXINCIYLD7KWBD2S2JCW2VAMAO6WZY - content-size:15870
2009-05-14T14:24:41.186Z   200      12804 http://www.carlsencards.com/free-ecard-10-congratulations-cat-ebay-elvis-cake.htm LL http://www.carlsencards.com/index.htm text/html #033 20090514142441139+28 sha1:2763M6F5K5APSQLHVCNXMMB36HUNEXBT - content-size:13099
2009-05-14T14:24:41.541Z   200      12843 http://www.carlsencards.com/free-ecard-17-wont-come-to-work-scream-existential-angst.htm LL http://www.carlsencards.com/index.htm text/html #043 20090514142441489+33 sha1:FKE6SPMZJNRUMPMCN3RB7C3PXYOGI7ZW - content-size:13138
2009-05-14T14:24:41.897Z   200      12828 http://www.carlsencards.com/free-ecard-11-congratulations-african-drummer-ants.htm LL http://www.carlsencards.com/index.htm text/html #039 20090514142441850+28 sha1:FLG7C7W75DEALJ6E5NI2ZCK3LCPGFMTC - content-size:13123
2009-05-14T14:24:42.419Z   200      12911 http://www.carlsencards.com/free-ecard-32-happy-new-year-raccoon-party-fireworks.htm LL http://www.carlsencards.com/index.htm text/html #017 20090514142442200+134 sha1:VQT2AXKK7KDB4QJ4BHEC3H7JWARE7F7H - content-size:13206
2009-05-14T14:24:43.070Z   200     644178 http://www.carlsencards.com/Ecard32.swf LLE http://www.carlsencards.com/free-ecard-32-happy-new-year-raccoon-party-fireworks.htm application/x-shockwave-flash #035 20090514142442750+307 sha1:ZU73EDO4CBJERBA6LUVBVHMFEMGHP6BJ - content-size:644495
2009-05-14T14:24:43.435Z   200      12849 http://www.carlsencards.com/free-ecard-50-congratulations-pimp-my-ecard.htm LL http://www.carlsencards.com/index.htm text/html #050 20090514142443390+27 sha1:LFWTHGXPOBQBENWFMNTMY6N6GQK5OXTM - content-size:13144
2009-05-14T14:24:44.386Z   200      12845 http://www.carlsencards.com/free-ecard-28-merry-christmas-easter-bunny-santa.htm LL http://www.carlsencards.com/index.htm text/html #020 20090514142444340+27 sha1:G2DPX42HEG5R4CCEVA5LJDGIPUL45ENX - content-size:13140
2009-05-14T14:24:44.940Z   200     528092 http://www.carlsencards.com/Ecard28.swf LLE http://www.carlsencards.com/free-ecard-28-merry-christmas-easter-bunny-santa.htm application/x-shockwave-flash #013 20090514142444689+222 sha1:55A56E5OSQQVICYGQO63E6Q3PHJQOQKV - content-size:528409
2009-05-14T14:24:45.302Z   200      12966 http://www.carlsencards.com/free-ecard-45-thank-you-dumpster-rat.htm LL http://www.carlsencards.com/index.htm text/html #020 20090514142445250+33 sha1:6BRGFKX7R45APLPOVCFXM4NJNT3V7VAZ - content-size:13261
2009-05-14T14:24:46.192Z   200     630097 http://www.carlsencards.com/Ecard45.swf LLE http://www.carlsencards.com/free-ecard-45-thank-you-dumpster-rat.htm application/x-shockwave-flash #042 20090514142445980+200 sha1:X5PJQHK2ODBIJVRA7LAVASXMR4JW3PJK - content-size:630414
2009-05-14T14:24:46.566Z   200      12739 http://www.carlsencards.com/free-ecard-15-i-love-you-chameleon-chameleogram.htm LL http://www.carlsencards.com/index.htm text/html #005 20090514142446500+48 sha1:4O3GLBDZVA5ED6FSUCVUHRYU26JWQOW3 - content-size:13034
2009-05-14T14:24:47.727Z   200     784739 http://www.carlsencards.com/Ecard15.swf LLE http://www.carlsencards.com/free-ecard-15-i-love-you-chameleon-chameleogram.htm application/x-shockwave-flash #017 20090514142447430+283 sha1:MNTTNSSO6B6X7XUKNCAIVZXK6BO5DGZC - content-size:785056
2009-05-14T14:24:48.517Z   200      12743 http://www.carlsencards.com/free-ecard-18-i-love-you-cat-pictures-true-love.htm LL http://www.carlsencards.com/index.htm text/html #022 20090514142448030+469 sha1:FFPBHA6MUTD6OUZURW2TCXCRGFPSBQQN - content-size:13038
2009-05-14T14:24:49.497Z   200     529401 http://www.carlsencards.com/Ecard18.swf LLE http://www.carlsencards.com/free-ecard-18-i-love-you-cat-pictures-true-love.htm application/x-shockwave-flash #020 20090514142449270+219 sha1:CCBT3XALOKFOXKHTQOO3J55AFQRXWC45 - content-size:529718
2009-05-14T14:24:49.847Z   200      12840 http://www.carlsencards.com/free-ecard-02-happy-birthday-piano-playing-mice.htm LL http://www.carlsencards.com/index.htm text/html #029 20090514142449800+28 sha1:2D6GYZDNYSQMB5QMQIQ32JRQSYDOESPS - content-size:13135
2009-05-14T14:24:50.576Z   200      13358 http://www.carlsencards.com/images/seatFillers/littleMousie.swf LL http://www.carlsencards.com/index.htm application/x-shockwave-flash #005 20090514142450540+31 sha1:GRH7WLYZZZXEB3N7634EWEAZRPRV5BMY - content-size:13673
2009-05-14T14:24:50.926Z   200      12805 http://www.carlsencards.com/free-ecard-30-i-hate-you-cat-toy-car-accident.htm LL http://www.carlsencards.com/index.htm text/html #014 20090514142450880+27 sha1:4RL2K5MXYJ42P76NMOI2T4L6S6OJCZGX - content-size:13100
2009-05-14T14:24:51.276Z   200      12790 http://www.carlsencards.com/free-ecard-20-happy-easter-eggs-jurassic.htm LL http://www.carlsencards.com/index.htm text/html #042 20090514142451230+27 sha1:6HN7WEIGJ4BIIED2MZZ56FZ3Z3ECE3K5 - content-size:13085
2009-05-14T14:24:51.853Z   200     755238 http://www.carlsencards.com/Ecard20.swf LLE http://www.carlsencards.com/free-ecard-20-happy-easter-eggs-jurassic.htm application/x-shockwave-flash #043 20090514142451580+249 sha1:YBE4C4W4HGBAEISY2T5YTFHN3PQMFHHX - content-size:755555
2009-05-14T14:24:52.206Z   200      12810 http://www.carlsencards.com/free-ecard-52-new-baby-polar-bears.htm LL http://www.carlsencards.com/index.htm text/html #030 20090514142452160+27 sha1:KS3A7LWJOYBRXEBNNG5YPIJCNVSFVUWB - content-size:13105
2009-05-14T14:24:53.649Z   200     825860 http://www.carlsencards.com/Ecard52.swf LLE http://www.carlsencards.com/free-ecard-52-new-baby-polar-bears.htm application/x-shockwave-flash #002 20090514142453340+295 sha1:HYWOREK7PHYTYK7WKFTY7JQSZ4HUHGNU - content-size:826177
2009-05-14T14:24:54.007Z   200      12791 http://www.carlsencards.com/free-ecard-22-i-hate-you-cat-latin-te-odio.htm LL http://www.carlsencards.com/index.htm text/html #031 20090514142453960+28 sha1:HPBOCJQPYHRGSUDKSSSFEAU4Y5NKTTFN - content-size:13086
2009-05-14T14:24:55.320Z   200      12816 http://www.carlsencards.com/free-ecard-36-congratulations-interactive-surprise-cats-mice.htm LL http://www.carlsencards.com/index.htm text/html #042 20090514142455270+29 sha1:LKFPNEMGAFJFADHD5M6OMDLI6LEGH64T - content-size:13111
2009-05-14T14:24:55.970Z   200     808707 http://www.carlsencards.com/Ecard36.swf LLE http://www.carlsencards.com/free-ecard-36-congratulations-interactive-surprise-cats-mice.htm application/x-shockwave-flash #002 20090514142455630+319 sha1:4IJXPMAZCCYPP6E7ZS6FS644DSYT34DZ - content-size:809024
2009-05-14T14:24:56.343Z   200      12601 http://www.carlsencards.com/disclaimer.htm LL http://www.carlsencards.com/index.htm text/html #033 20090514142456300+28 sha1:GEZSORQCPGAWMWLWS4ZGUK7XBXBYAK7Q - content-size:12896
2009-05-14T14:24:57.652Z   200      29979 http://www.carlsencards.com/cat-free-ecards-holiday-greetings.htm LL http://www.carlsencards.com/index.htm text/html #020 20090514142457560+54 sha1:3AJ7NI6HOEFQ6KDPDWQTQONI3I6WOHQP - content-size:30274
2009-05-14T14:24:58.202Z   200      17328 http://www.carlsencards.com/cat-free-ecards-i-love-you.htm LL http://www.carlsencards.com/index.htm text/html #026 20090514142457961+136 sha1:MK2X4UOC6NWY4SNG4RWLWMBBKJA6IEO4 - content-size:17623
2009-05-14T14:24:58.546Z   404        223 http://www.carlsencards.com/application/x-shockwave-flash LEX http://www.carlsencards.com/Scripts/AC_RunActiveContent.js text/html #022 20090514142458520+25 sha1:B77W4WVFI2JIIWG76POARQ7BKKO7B5JT - content-size:424
2009-05-14T14:24:59.049Z   200     116554 http://www.carlsencards.com/ LL http://carlsencards.com/sitemap.htm text/html #024 20090514142458850+68 sha1:S55ZEYEBJ76BSSWIT3INW3IV7GJXSW3J - content-size:116851
2009-05-14T14:24:59.394Z   200         15 http://carlsencards.com/SendEcard.php EXX http://carlsencards.com/EcardFooter.swf text/html #033 20090514142459361+32 sha1:OQZMZDS2B637MKIUIECTKS6D3ALB4GC7 - content-size:214
2009-05-14T14:25:00.107Z   200     659765 http://carlsencards.com/Ecard4.swf LLL http://carlsencards.com/free-ecard-04-im-sorry-dog-interactive.htm application/x-shockwave-flash #050 20090514142459700+238 sha1:CXQWBVSYRESPQN2XW6GXFF5C3L2LIXXB - content-size:660082
2009-05-14T14:25:00.662Z   200     566465 http://carlsencards.com/Ecard43.swf LLL http://carlsencards.com/free-ecard-43-happy-fathers-day-mixer-board.htm application/x-shockwave-flash #022 20090514142500410+224 sha1:PUAUQPGDUFUCE5VA56H4O6PL4E75YSXZ - content-size:566782
2009-05-14T14:25:01.432Z   404        205 http://carlsencards.com/_root.sound LLLX http://carlsencards.com/Ecard43.swf text/html #022 20090514142501410+21 sha1:JNWXGAFDSWGWVUECM452CFLODG2LS7L7 - content-size:406
2009-05-14T14:25:02.824Z   200     647747 http://carlsencards.com/Ecard35.swf LLL http://carlsencards.com/free-ecard-35-happy-valentines-day-jungle-love-lemurs.htm application/x-shockwave-flash #017 20090514142501780+1029 sha1:EDTCYO3537BB4ZO42L3VESTKXB73RDW2 - content-size:648064
2009-05-14T14:25:04.036Z   200     459792 http://carlsencards.com/Ecard22.swf LLL http://carlsencards.com/free-ecard-22-i-hate-you-cat-latin-te-odio.htm application/x-shockwave-flash #010 20090514142503830+197 sha1:DJBR4O65IQUB4T36W4CISDDI34PTGJZK - content-size:460109
2009-05-14T14:25:04.580Z   200     559671 http://carlsencards.com/Ecard33.swf LLL http://carlsencards.com/free-ecard-33-happy-valentines-day-funny-cats.htm application/x-shockwave-flash #042 20090514142504340+230 sha1:DASMMA4B6XY5X6D2257QUICUB6EJOT25 - content-size:559988
2009-05-14T14:25:05.238Z   200     747137 http://carlsencards.com/Ecard34.swf LLL http://carlsencards.com/free-ecard-34-happy-valentines-day-romantic-mice.htm application/x-shockwave-flash #018 20090514142504951+275 sha1:I7PRGQ27GTBECU5QXNMM24YMVHV5JTQP - content-size:747454
2009-05-14T14:25:05.898Z   200     502582 http://carlsencards.com/Ecard13.swf LLL http://carlsencards.com/free-ecard-13-spread-the-word-make-a-difference-cat-mouse.htm application/x-shockwave-flash #005 20090514142505680+196 sha1:KRQLDOFUGYSEDJMVXYNDBAZREDOI7NHK - content-size:502899
2009-05-14T14:25:07.033Z   200     809125 http://carlsencards.com/Ecard19.swf LLL http://carlsencards.com/free-ecard-19-merry-christmas-penguins-writing-snow.htm application/x-shockwave-flash #020 20090514142506700+316 sha1:S3DQB46CCPG2LIFML4J7CRLDRRD566PO - content-size:809442
2009-05-14T14:25:07.704Z   200     755238 http://carlsencards.com/Ecard20.swf LLL http://carlsencards.com/free-ecard-20-happy-easter-eggs-jurassic.htm application/x-shockwave-flash #046 20090514142507360+330 sha1:YBE4C4W4HGBAEISY2T5YTFHN3PQMFHHX - content-size:755555
2009-05-14T14:25:09.143Z   200     572772 http://carlsencards.com/Ecard24.swf LLL http://carlsencards.com/free-ecard-24-no-special-occasion-hi-mouse-pixar.htm application/x-shockwave-flash #046 20090514142508621+504 sha1:T4H5QRLCLIA6SYFWFKMGO5IWMEWAWSPX - content-size:573089
2009-05-14T14:25:09.683Z   404        214 http://carlsencards.com/www.carlsencards.com LLEX http://carlsencards.com/Ecard0.swf text/html #017 20090514142509661+21 sha1:XY352SNCYCXCFYWXLREDEW7CNB2BDJLY - content-size:415
2009-05-14T14:25:10.642Z   200     867621 http://carlsencards.com/Ecard59.swf LLL http://carlsencards.com/free-ecard-59-happy-birthday-flowers-rat.htm application/x-shockwave-flash #018 20090514142510271+329 sha1:NWKL2KDSDOWVT56UVYFGICXNGT6P2D4W - content-size:867938
2009-05-14T14:25:11.013Z   200        773 http://carlsencards.com/Scripts/expressInstall.swf LLL http://carlsencards.com/free-ecard-59-happy-birthday-flowers-rat.htm application/x-shockwave-flash #009 20090514142510981+27 sha1:BNOQJWUKRUPJU43BWRFR4OCAEND66GBE - content-size:1085
2009-05-14T14:25:12.375Z   404        209 http://carlsencards.com/Download.Failed LLLX http://carlsencards.com/Scripts/expressInstall.swf text/html #035 20090514142512351+23 sha1:375KQ5YI7D2O4J466F2PPAAONTIAFSDD - content-size:410
2009-05-14T14:25:12.703Z   404        212 http://carlsencards.com/Download.Cancelled LLLX http://carlsencards.com/Scripts/expressInstall.swf text/html #017 20090514142512681+21 sha1:2LZXNUPIAZGL3I3X4YHG5VCYUE4EIMQA - content-size:413
2009-05-14T14:25:13.033Z   404        211 http://carlsencards.com/Download.Complete LLLX http://carlsencards.com/Scripts/expressInstall.swf text/html #009 20090514142513011+21 sha1:CTS4ZBBE3QD3N7KTQECTQMNSWVZ7X3OS - content-size:412
2009-05-14T14:25:13.363Z   404        219 http://carlsencards.com/fpdownload.macromedia.com LLLX http://carlsencards.com/Scripts/expressInstall.swf text/html #026 20090514142513341+21 sha1:KBUJXTC2YH4BHDHAWSLMQVBHR6Z2VF6B - content-size:420
2009-05-14T14:25:13.886Z   404        202 http://carlsencards.com/8.0.35.0 LLL http://carlsencards.com/free-ecard-59-happy-birthday-flowers-rat.htm text/html #017 20090514142513671+158 sha1:6FHSYYJPB7EVUVUGVIPVYPDM26SEU4IO - content-size:403
2009-05-14T14:25:14.243Z   404        200 http://carlsencards.com/6.0.65 LLEX http://carlsencards.com/Scripts/swfobject_modified.js text/html #045 20090514142514221+21 sha1:LZDL5LTDXOVGYB2DVJV3KUWVJGC5KZQL - content-size:401
2009-05-14T14:25:14.572Z   404        202 http://carlsencards.com/text/css LLEX http://carlsencards.com/Scripts/swfobject_modified.js text/html #022 20090514142514551+20 sha1:F4GZUCRXPM2QKXWFV7RYZXPKD7SD42JW - content-size:403
2009-05-14T14:25:14.904Z   404        199 http://carlsencards.com/8.0.0 LLEX http://carlsencards.com/Scripts/swfobject_modified.js text/html #017 20090514142514881+22 sha1:EIWX3DTRFKG2BK6N7XGVFFNULJBMSKVX - content-size:400
2009-05-14T14:25:15.471Z   200     699483 http://carlsencards.com/Ecard29.swf LLL http://carlsencards.com/free-ecard-29-merry-christmas-cat-mice-rudolph-prank.htm application/x-shockwave-flash #037 20090514142515211+246 sha1:AB774C3BCVOOCRO5AGVJZ2SEBE7PQ7IM - content-size:699800
2009-05-14T14:25:16.008Z   200     529401 http://carlsencards.com/Ecard18.swf LLL http://carlsencards.com/free-ecard-18-i-love-you-cat-pictures-true-love.htm application/x-shockwave-flash #017 20090514142515781+220 sha1:CCBT3XALOKFOXKHTQOO3J55AFQRXWC45 - content-size:529718
2009-05-14T14:25:17.152Z   200     780884 http://carlsencards.com/Ecard44.swf LLL http://carlsencards.com/free-ecard-44-fourth-of-july-mice-fireworks.htm application/x-shockwave-flash #005 20090514142516841+277 sha1:ES52KOOUGMEUR6Y6MPEGRLAJX4P7VCGN - content-size:781201
2009-05-14T14:25:17.803Z   200     828009 http://carlsencards.com/Ecard6.swf LLL http://carlsencards.com/free-ecard-06-happy-birthday-devil-angel-cake.htm application/x-shockwave-flash #037 20090514142517461+325 sha1:NZVY77MDQELKOUFNBD56QZLFIICDPBHK - content-size:828326
2009-05-14T14:25:18.953Z   200     655979 http://carlsencards.com/Ecard30.swf LLL http://carlsencards.com/free-ecard-30-i-hate-you-cat-toy-car-accident.htm application/x-shockwave-flash #042 20090514142518691+233 sha1:MMER3CYSH7JHWHB6JREOLHJAIDTW6FXJ - content-size:656296
2009-05-14T14:25:19.697Z   200     694668 http://carlsencards.com/Ecard41.swf LLL http://carlsencards.com/free-ecard-41-happy-mothers-day-polar-bears.htm application/x-shockwave-flash #035 20090514142519431+253 sha1:5IKALWNGYQLUO3HRQF3SSH6SJHWZ5TKO - content-size:694985
2009-05-14T14:25:20.529Z   200     707307 http://carlsencards.com/Ecard10.swf LLL http://carlsencards.com/free-ecard-10-congratulations-cat-ebay-elvis-cake.htm application/x-shockwave-flash #003 20090514142520281+237 sha1:BHAGF56E6E5Y2J2YNLLF7LPJNZEMIAJ6 - content-size:707624
2009-05-14T14:25:21.343Z   200     594218 http://carlsencards.com/Ecard11.swf LLL http://carlsencards.com/free-ecard-11-congratulations-african-drummer-ants.htm application/x-shockwave-flash #009 20090514142521061+261 sha1:ENCY3SE5QPNAIPFJXNCM3BPPKOEDG7GS - content-size:594535
2009-05-14T14:25:22.232Z   200     825860 http://carlsencards.com/Ecard52.swf LLL http://carlsencards.com/free-ecard-52-new-baby-polar-bears.htm application/x-shockwave-flash #042 20090514142521921+292 sha1:HYWOREK7PHYTYK7WKFTY7JQSZ4HUHGNU - content-size:826177
2009-05-14T14:25:22.745Z   200     513935 http://carlsencards.com/Ecard42.swf LLL http://carlsencards.com/free-ecard-42-happy-mothers-day-llamas.htm application/x-shockwave-flash #033 20090514142522541+194 sha1:TKTPJWQ3TACLUYMCTK7X73J277OEYBPO - content-size:514252
2009-05-14T14:25:24.098Z   200     590955 http://carlsencards.com/Ecard2.swf LLL http://carlsencards.com/free-ecard-02-happy-birthday-piano-playing-mice.htm application/x-shockwave-flash #017 20090514142523861+221 sha1:ZBMYS36XVKPJEJ46GWN65LJEVI7TMNEO - content-size:591272
2009-05-14T14:25:24.684Z   200     622030 http://carlsencards.com/Ecard9.swf LLL http://carlsencards.com/free-ecard-09-congratulations-penguins-versus-animator.htm application/x-shockwave-flash #041 20090514142524401+232 sha1:NLZECFM7MZQUWQFMXPZEWYIMCDY5M2EW - content-size:622347
2009-05-14T14:25:25.571Z   200     714775 http://carlsencards.com/Ecard48.swf LLL http://carlsencards.com/free-ecard-48-halloween-in-ghost-town.htm application/x-shockwave-flash #046 20090514142525261+279 sha1:ZY7UDI4LXR4MOMEVJT2PXN4VNE6LGVDX - content-size:715092
2009-05-14T14:25:26.157Z   200     644178 http://carlsencards.com/Ecard32.swf LLL http://carlsencards.com/free-ecard-32-happy-new-year-raccoon-party-fireworks.htm application/x-shockwave-flash #017 20090514142525911+234 sha1:ZU73EDO4CBJERBA6LUVBVHMFEMGHP6BJ - content-size:644495
2009-05-14T14:25:27.302Z   200     789273 http://carlsencards.com/Ecard46.swf LLL http://carlsencards.com/free-ecard-46-thank-you-lemur-choir.htm application/x-shockwave-flash #017 20090514142526981+308 sha1:MUFPQQGNTT275X7ENVEGYIPLOGURC7U2 - content-size:789590
2009-05-14T14:25:27.877Z   200     586842 http://carlsencards.com/Ecard47.swf LLL http://carlsencards.com/free-ecard-47-no-special-occasion-cheerleader-mouse.htm application/x-shockwave-flash #022 20090514142527621+244 sha1:BAUZXWWOJ5XYROQP5RE6APTVXIZSIXKR - content-size:587159
2009-05-14T14:25:29.775Z   200     785394 http://carlsencards.com/Ecard26.swf LLL http://carlsencards.com/free-ecard-26-halloween-mouse-in-pumpkin-explosion.htm application/x-shockwave-flash #022 20090514142528851+404 sha1:SZHWFS6PWBVWDUN5CZK77WSX7APKLIUV - content-size:785711
2009-05-14T14:25:30.550Z   200     798853 http://www.carlsencards.com/Ecard31.swf LLL http://www.carlsencards.com/free-ecard-31-no-special-occasion-cat-mouse-flash-game.htm application/x-shockwave-flash #007 20090514142530192+314 sha1:IQXBCHQU2INNCEQKG6XR4UHW424DFWJJ - content-size:799170
2009-05-14T14:25:30.905Z   404        202 http://www.carlsencards.com/8.0.35.0 LLL http://www.carlsencards.com/free-ecard-59-happy-birthday-flowers-rat.htm text/html #037 20090514142530881+23 sha1:6FHSYYJPB7EVUVUGVIPVYPDM26SEU4IO - content-size:403
2009-05-14T14:25:32.147Z   200        773 http://www.carlsencards.com/Scripts/expressInstall.swf LLL http://www.carlsencards.com/free-ecard-59-happy-birthday-flowers-rat.htm application/x-shockwave-flash #038 20090514142532122+21 sha1:BNOQJWUKRUPJU43BWRFR4OCAEND66GBE - content-size:1085
2009-05-14T14:25:32.474Z   404        219 http://www.carlsencards.com/fpdownload.macromedia.com LLLX http://www.carlsencards.com/Scripts/expressInstall.swf text/html #008 20090514142532452+21 sha1:KBUJXTC2YH4BHDHAWSLMQVBHR6Z2VF6B - content-size:420
2009-05-14T14:25:32.804Z   404        211 http://www.carlsencards.com/Download.Complete LLLX http://www.carlsencards.com/Scripts/expressInstall.swf text/html #022 20090514142532781+22 sha1:CTS4ZBBE3QD3N7KTQECTQMNSWVZ7X3OS - content-size:412
2009-05-14T14:25:33.137Z   404        212 http://www.carlsencards.com/Download.Cancelled LLLX http://www.carlsencards.com/Scripts/expressInstall.swf text/html #038 20090514142533112+24 sha1:2LZXNUPIAZGL3I3X4YHG5VCYUE4EIMQA - content-size:413
2009-05-14T14:25:33.464Z   404        209 http://www.carlsencards.com/Download.Failed LLLX http://www.carlsencards.com/Scripts/expressInstall.swf text/html #009 20090514142533442+21 sha1:375KQ5YI7D2O4J466F2PPAAONTIAFSDD - content-size:410
2009-05-14T14:25:33.855Z   200     153651 http://www.carlsencards.com/EcardFooter2.swf LLEX http://www.carlsencards.com/Ecard59.swf application/x-shockwave-flash #022 20090514142533772+79 sha1:UOY4YJMYPCB55BW3PKISM4PSVSWNZO5L - content-size:153968
2009-05-14T14:25:34.184Z   404        223 http://www.carlsencards.com/ShockwaveFlash.ShockwaveFlash LLEX http://www.carlsencards.com/Scripts/swfobject_modified.js text/html #028 20090514142534162+21 sha1:C7U7YSJVHPBRKXHHE6JDWHVVNZEJ6H22 - content-size:424
2009-05-14T14:25:34.515Z   404        199 http://www.carlsencards.com/8.0.0 LLEX http://www.carlsencards.com/Scripts/swfobject_modified.js text/html #015 20090514142534492+22 sha1:EIWX3DTRFKG2BK6N7XGVFFNULJBMSKVX - content-size:400
2009-05-14T14:25:34.844Z   404        202 http://www.carlsencards.com/text/css LLEX http://www.carlsencards.com/Scripts/swfobject_modified.js text/html #035 20090514142534822+21 sha1:F4GZUCRXPM2QKXWFV7RYZXPKD7SD42JW - content-size:403
2009-05-14T14:25:35.173Z   404        200 http://www.carlsencards.com/6.0.65 LLEX http://www.carlsencards.com/Scripts/swfobject_modified.js text/html #028 20090514142535152+21 sha1:LZDL5LTDXOVGYB2DVJV3KUWVJGC5KZQL - content-size:401
2009-05-14T14:25:35.693Z   200     517872 http://www.carlsencards.com/Ecard53.swf LLL http://www.carlsencards.com/free-ecard-53-new-baby-llamas.htm application/x-shockwave-flash #008 20090514142535482+201 sha1:K33SDPS3UE2LOW24LUD3GTDZJQDRB7UX - content-size:518189
2009-05-14T14:25:36.304Z   200     809125 http://www.carlsencards.com/Ecard19.swf LLL http://www.carlsencards.com/free-ecard-19-merry-christmas-penguins-writing-snow.htm application/x-shockwave-flash #038 20090514142536002+285 sha1:S3DQB46CCPG2LIFML4J7CRLDRRD566PO - content-size:809442
2009-05-14T14:25:36.804Z   200     153651 http://www.carlsencards.com/EcardFooter.swf LLEX http://www.carlsencards.com/Ecard13.swf application/x-shockwave-flash #041 20090514142536722+79 sha1:UOY4YJMYPCB55BW3PKISM4PSVSWNZO5L - content-size:153968
2009-05-14T14:25:38.323Z   200    1107240 http://www.carlsencards.com/Ecard58.swf LLL http://www.carlsencards.com/free-ecard-58-congratulations-hamster-on-strike.htm application/x-shockwave-flash #018 20090514142537892+377 sha1:UFZ2A6SCTSWXJ5AMMFWFDJ4XD5NJC5FS - content-size:1107559
2009-05-14T14:25:39.029Z   200     479689 http://www.carlsencards.com/Ecard51.swf LLL http://www.carlsencards.com/free-ecard-51-happy-birthday-fortune-teller-rat.htm application/x-shockwave-flash #008 20090514142538712+292 sha1:4N5FRQ2J332RV2GLT5X4VTA364ZFCMQX - content-size:480006
2009-05-14T14:25:40.810Z   200     828009 http://www.carlsencards.com/Ecard6.swf LLL http://www.carlsencards.com/free-ecard-06-happy-birthday-devil-angel-cake.htm application/x-shockwave-flash #016 20090514142540492+300 sha1:NZVY77MDQELKOUFNBD56QZLFIICDPBHK - content-size:828326
2009-05-14T14:25:41.145Z   404        214 http://www.carlsencards.com/www.carlsencards.com LLEX http://www.carlsencards.com/Ecard0.swf text/html #028 20090514142541122+22 sha1:XY352SNCYCXCFYWXLREDEW7CNB2BDJLY - content-size:415
2009-05-14T14:25:42.662Z   200     492188 http://www.carlsencards.com/Ecard25.swf LLL http://www.carlsencards.com/free-ecard-25-no-special-occasion-text-messaging-lemurs.htm application/x-shockwave-flash #017 20090514142542442+210 sha1:R6MS3R4WW4ARBKE6G7J4SUEPYQXPVLWJ - content-size:492505
2009-05-14T14:25:42.994Z   404        205 http://www.carlsencards.com/_root.sound LLEX http://www.carlsencards.com/Ecard43.swf text/html #037 20090514142542972+21 sha1:JNWXGAFDSWGWVUECM452CFLODG2LS7L7 - content-size:406
2009-05-14T14:25:44.051Z   200     659765 http://www.carlsencards.com/Ecard4.swf LLL http://www.carlsencards.com/free-ecard-04-im-sorry-dog-interactive.htm application/x-shockwave-flash #026 20090514142543632+382 sha1:CXQWBVSYRESPQN2XW6GXFF5C3L2LIXXB - content-size:660082
2009-05-14T14:25:44.761Z   200     789273 http://www.carlsencards.com/Ecard46.swf LLL http://www.carlsencards.com/free-ecard-46-thank-you-lemur-choir.htm application/x-shockwave-flash #039 20090514142544452+295 sha1:MUFPQQGNTT275X7ENVEGYIPLOGURC7U2 - content-size:789590
2009-05-14T14:25:45.653Z   200     561780 http://www.carlsencards.com/Ecard21.swf LLL http://www.carlsencards.com/free-ecard-21-get-well-cheerleader-mice.htm application/x-shockwave-flash #043 20090514142545353+289 sha1:3KRSWEVBILH3UGMXOZQIKKGP3BA4X45V - content-size:562097
2009-05-14T14:25:46.539Z   200     572772 http://www.carlsencards.com/Ecard24.swf LLL http://www.carlsencards.com/free-ecard-24-no-special-occasion-hi-mouse-pixar.htm application/x-shockwave-flash #018 20090514142546312+215 sha1:T4H5QRLCLIA6SYFWFKMGO5IWMEWAWSPX - content-size:573089
2009-05-14T14:25:47.062Z   200     560226 http://www.carlsencards.com/Ecard5.swf LLL http://www.carlsencards.com/free-ecard-05-happy-birthday-postcard-coffee.htm application/x-shockwave-flash #016 20090514142546842+211 sha1:LVYDDXAXH4HNI37MZXYSTIF5SRWP5KFS - content-size:560543
2009-05-14T14:25:47.958Z   200     699483 http://www.carlsencards.com/Ecard29.swf LLL http://www.carlsencards.com/free-ecard-29-merry-christmas-cat-mice-rudolph-prank.htm application/x-shockwave-flash #027 20090514142547672+272 sha1:AB774C3BCVOOCRO5AGVJZ2SEBE7PQ7IM - content-size:699800
2009-05-14T14:25:48.582Z   200     734327 http://www.carlsencards.com/Ecard23.swf LLL http://www.carlsencards.com/free-ecard-23-no-special-occasion-cosmopolitan-recipe.htm application/x-shockwave-flash #038 20090514142548262+305 sha1:LIPAW7R7C3QVM6BOTRWAF443GDYEA2JQ - content-size:734644
2009-05-14T14:25:49.630Z   200     714775 http://www.carlsencards.com/Ecard48.swf LLL http://www.carlsencards.com/free-ecard-48-halloween-in-ghost-town.htm application/x-shockwave-flash #029 20090514142549342+258 sha1:ZY7UDI4LXR4MOMEVJT2PXN4VNE6LGVDX - content-size:715092
2009-05-14T14:25:50.373Z   200     780884 http://www.carlsencards.com/Ecard44.swf LLL http://www.carlsencards.com/free-ecard-44-fourth-of-july-mice-fireworks.htm application/x-shockwave-flash #037 20090514142550022+317 sha1:ES52KOOUGMEUR6Y6MPEGRLAJX4P7VCGN - content-size:781201
2009-05-14T14:25:51.317Z   200     658126 http://www.carlsencards.com/Ecard27.swf LLL http://www.carlsencards.com/free-ecard-27-happy-thanksgiving-funny-cats.htm application/x-shockwave-flash #016 20090514142551042+265 sha1:P5JLZNJUDNFZKIXL4J22KKLPSE7KFSF7 - content-size:658443
2009-05-14T14:25:52.175Z   200     647747 http://www.carlsencards.com/Ecard35.swf LLL http://www.carlsencards.com/free-ecard-35-happy-valentines-day-jungle-love-lemurs.htm application/x-shockwave-flash #041 20090514142551902+259 sha1:EDTCYO3537BB4ZO42L3VESTKXB73RDW2 - content-size:648064
2009-05-14T14:25:52.640Z   404        205 http://www.carlsencards.com/_root.card_ LLEX http://www.carlsencards.com/Ecard38.swf text/html #024 20090514142552613+26 sha1:NI6VE4CBCLMKK2K7MKPZ64ORE7XTCGTW - content-size:406
2009-05-14T14:25:53.711Z   200     647852 http://www.carlsencards.com/Ecard55.swf LLL http://www.carlsencards.com/free-ecard-55-happy-anniversary-lemurs.htm application/x-shockwave-flash #021 20090514142553453+241 sha1:YAPCWUGDDP5O4BUS2V6VW7KE2WJ4MNKH - content-size:648169
2009-05-14T14:25:54.318Z   200     747137 http://www.carlsencards.com/Ecard34.swf LLL http://www.carlsencards.com/free-ecard-34-happy-valentines-day-romantic-mice.htm application/x-shockwave-flash #035 20090514142554013+294 sha1:I7PRGQ27GTBECU5QXNMM24YMVHV5JTQP - content-size:747454
2009-05-14T14:25:55.299Z   200     694668 http://www.carlsencards.com/Ecard41.swf LLL http://www.carlsencards.com/free-ecard-41-happy-mothers-day-polar-bears.htm application/x-shockwave-flash #027 20090514142554983+303 sha1:5IKALWNGYQLUO3HRQF3SSH6SJHWZ5TKO - content-size:694985
2009-05-14T14:25:56.042Z   200     707307 http://www.carlsencards.com/Ecard10.swf LLL http://www.carlsencards.com/free-ecard-10-congratulations-cat-ebay-elvis-cake.htm application/x-shockwave-flash #012 20090514142555782+249 sha1:BHAGF56E6E5Y2J2YNLLF7LPJNZEMIAJ6 - content-size:707624
2009-05-14T14:25:56.881Z   200     539138 http://www.carlsencards.com/Ecard17.swf LLL http://www.carlsencards.com/free-ecard-17-wont-come-to-work-scream-existential-angst.htm application/x-shockwave-flash #029 20090514142556673+198 sha1:T4RHIP54QS7BW6EANQ3EOYLCK553XRM6 - content-size:539455
2009-05-14T14:25:57.680Z   200     594218 http://www.carlsencards.com/Ecard11.swf LLL http://www.carlsencards.com/free-ecard-11-congratulations-african-drummer-ants.htm application/x-shockwave-flash #023 20090514142557443+217 sha1:ENCY3SE5QPNAIPFJXNCM3BPPKOEDG7GS - content-size:594535
2009-05-14T14:25:58.298Z   200     767128 http://www.carlsencards.com/Ecard50.swf LLL http://www.carlsencards.com/free-ecard-50-congratulations-pimp-my-ecard.htm application/x-shockwave-flash #012 20090514142557983+274 sha1:J6636X2B35KL3Z5DPXKPI4A6VQSNYBDV - le:IOException@ExtractorSWF,content-size:767445
2009-05-14T14:25:59.180Z   200     590955 http://www.carlsencards.com/Ecard2.swf LLL http://www.carlsencards.com/free-ecard-02-happy-birthday-piano-playing-mice.htm application/x-shockwave-flash #030 20090514142558853+246 sha1:ZBMYS36XVKPJEJ46GWN65LJEVI7TMNEO - content-size:591272
2009-05-14T14:26:00.168Z   200     655979 http://www.carlsencards.com/Ecard30.swf LLL http://www.carlsencards.com/free-ecard-30-i-hate-you-cat-toy-car-accident.htm application/x-shockwave-flash #017 20090514142559803+336 sha1:MMER3CYSH7JHWHB6JREOLHJAIDTW6FXJ - content-size:656296
2009-05-14T14:26:01.119Z   200     459792 http://www.carlsencards.com/Ecard22.swf LLL http://www.carlsencards.com/free-ecard-22-i-hate-you-cat-latin-te-odio.htm application/x-shockwave-flash #019 20090514142600514+596 sha1:DJBR4O65IQUB4T36W4CISDDI34PTGJZK - content-size:460109

metadata://netarkivet.dk/crawl/logs/local-errors.log?heritrixVersion=1.14.3&harvestid=1&jobid=1 130.226.228.8 20090514142558 text/plain 5174
2009-05-14T14:23:06.370Z   200     437898 http://carlsencards.com/Ecard57.swf LLE http://carlsencards.com/free-ecard-57-no-special-occasion-FAPSS.htm application/x-shockwave-flash #002 20090514142306146+201 sha1:HYOIKRMTKIRNDUGB3KUWTA4T77H5JEA7 - le:IOException@ExtractorSWF,content-size:438215
 java.io.IOException: Unexpected end of input while reading a specified number of bytes
	at com.anotherbigidea.io.InStream.read(InStream.java:176)
	at com.anotherbigidea.flash.readers.ActionParser.createRecords(ActionParser.java:353)
	at com.anotherbigidea.flash.readers.ActionParser.parse(ActionParser.java:72)
	at com.anotherbigidea.flash.readers.TagParser.parseDefineButton2(TagParser.java:602)
	at com.anotherbigidea.flash.readers.TagParser.tag(TagParser.java:215)
	at org.archive.crawler.extractor.ExtractorSWF$ExtractorSWFReader.readOneTag(ExtractorSWF.java:185)
	at com.anotherbigidea.flash.readers.SWFReader.readTags(SWFReader.java:102)
	at com.anotherbigidea.flash.readers.SWFReader.readFile(SWFReader.java:92)
	at org.archive.crawler.extractor.ExtractorSWF.extract(ExtractorSWF.java:109)
	at org.archive.crawler.extractor.Extractor.innerProcess(Extractor.java:67)
	at org.archive.crawler.framework.Processor.process(Processor.java:112)
	at org.archive.crawler.framework.ToeThread.processCrawlUri(ToeThread.java:302)
	at org.archive.crawler.framework.ToeThread.run(ToeThread.java:151)
2009-05-14T14:23:37.220Z   200     767128 http://carlsencards.com/Ecard50.swf LLE http://carlsencards.com/free-ecard-50-congratulations-pimp-my-ecard.htm application/x-shockwave-flash #036 20090514142336877+301 sha1:J6636X2B35KL3Z5DPXKPI4A6VQSNYBDV - le:IOException@ExtractorSWF,content-size:767445
 java.io.IOException: Unexpected end of input
	at com.anotherbigidea.io.InStream.readUI16(InStream.java:318)
	at com.anotherbigidea.flash.readers.TagParser.parseDefineButton2(TagParser.java:594)
	at com.anotherbigidea.flash.readers.TagParser.tag(TagParser.java:215)
	at org.archive.crawler.extractor.ExtractorSWF$ExtractorSWFReader.readOneTag(ExtractorSWF.java:185)
	at com.anotherbigidea.flash.readers.SWFReader.readTags(SWFReader.java:102)
	at com.anotherbigidea.flash.readers.SWFReader.readFile(SWFReader.java:92)
	at org.archive.crawler.extractor.ExtractorSWF.extract(ExtractorSWF.java:109)
	at org.archive.crawler.extractor.Extractor.innerProcess(Extractor.java:67)
	at org.archive.crawler.framework.Processor.process(Processor.java:112)
	at org.archive.crawler.framework.ToeThread.processCrawlUri(ToeThread.java:302)
	at org.archive.crawler.framework.ToeThread.run(ToeThread.java:151)
2009-05-14T14:24:02.428Z   200     437898 http://www.carlsencards.com/Ecard57.swf LLE http://www.carlsencards.com/free-ecard-57-no-special-occasion-FAPSS.htm application/x-shockwave-flash #006 20090514142402238+168 sha1:HYOIKRMTKIRNDUGB3KUWTA4T77H5JEA7 - le:IOException@ExtractorSWF,content-size:438215
 java.io.IOException: Unexpected end of input while reading a specified number of bytes
	at com.anotherbigidea.io.InStream.read(InStream.java:176)
	at com.anotherbigidea.flash.readers.ActionParser.createRecords(ActionParser.java:353)
	at com.anotherbigidea.flash.readers.ActionParser.parse(ActionParser.java:72)
	at com.anotherbigidea.flash.readers.TagParser.parseDefineButton2(TagParser.java:602)
	at com.anotherbigidea.flash.readers.TagParser.tag(TagParser.java:215)
	at org.archive.crawler.extractor.ExtractorSWF$ExtractorSWFReader.readOneTag(ExtractorSWF.java:185)
	at com.anotherbigidea.flash.readers.SWFReader.readTags(SWFReader.java:102)
	at com.anotherbigidea.flash.readers.SWFReader.readFile(SWFReader.java:92)
	at org.archive.crawler.extractor.ExtractorSWF.extract(ExtractorSWF.java:109)
	at org.archive.crawler.extractor.Extractor.innerProcess(Extractor.java:67)
	at org.archive.crawler.framework.Processor.process(Processor.java:112)
	at org.archive.crawler.framework.ToeThread.processCrawlUri(ToeThread.java:302)
	at org.archive.crawler.framework.ToeThread.run(ToeThread.java:151)
2009-05-14T14:25:58.297Z   200     767128 http://www.carlsencards.com/Ecard50.swf LLL http://www.carlsencards.com/free-ecard-50-congratulations-pimp-my-ecard.htm application/x-shockwave-flash #012 20090514142557983+274 sha1:J6636X2B35KL3Z5DPXKPI4A6VQSNYBDV - le:IOException@ExtractorSWF,content-size:767445
 java.io.IOException: Unexpected end of input
	at com.anotherbigidea.io.InStream.readUI16(InStream.java:318)
	at com.anotherbigidea.flash.readers.TagParser.parseDefineButton2(TagParser.java:594)
	at com.anotherbigidea.flash.readers.TagParser.tag(TagParser.java:215)
	at org.archive.crawler.extractor.ExtractorSWF$ExtractorSWFReader.readOneTag(ExtractorSWF.java:185)
	at com.anotherbigidea.flash.readers.SWFReader.readTags(SWFReader.java:102)
	at com.anotherbigidea.flash.readers.SWFReader.readFile(SWFReader.java:92)
	at org.archive.crawler.extractor.ExtractorSWF.extract(ExtractorSWF.java:109)
	at org.archive.crawler.extractor.Extractor.innerProcess(Extractor.java:67)
	at org.archive.crawler.framework.Processor.process(Processor.java:112)
	at org.archive.crawler.framework.ToeThread.processCrawlUri(ToeThread.java:302)
	at org.archive.crawler.framework.ToeThread.run(ToeThread.java:151)

metadata://netarkivet.dk/crawl/logs/progress-statistics.log?heritrixVersion=1.14.3&harvestid=1&jobid=1 130.226.228.8 20090514142601 text/plain 3142
20090514142100 CRAWL RESUMED - Running
           timestamp  discovered      queued   downloaded       doc/s(avg)  KB/s(avg)   dl-failures   busy-thread   mem-use-KB  heap-size-KB   congestion   max-depth   avg-depth
2009-05-14T14:21:20Z         254         171           81       4.05(4.05)   171(171)             2             1        28916         40832            1         171         171
2009-05-14T14:21:40Z         254         115          137        2.8(3.42)    71(121)             2             0        15742         43840            1         115         115
2009-05-14T14:22:00Z         458         269          187        2.5(3.12)   119(120)             2             0        35775         43840            1         269         269
2009-05-14T14:22:20Z         459         213          244       2.85(3.05)    46(102)             2             0        20756         44288            1         213         213
2009-05-14T14:22:40Z         460         163          295       2.55(2.95)     41(90)             2             0        27905         44288            1         163         163
2009-05-14T14:23:00Z         479         145          332       1.85(2.77)   300(125)             2             0        15776         47744            1         145         145
2009-05-14T14:23:20Z         501         141          357       1.25(2.55)   292(149)             3             0        16655         44352            1         141         141
2009-05-14T14:23:40Z         521         129          389        1.6(2.43)   305(168)             3             0        28685         43520            1         129         129
2009-05-14T14:24:00Z         548         116          429          2(2.38)   264(179)             3             0        19625         41856            1         116         116
2009-05-14T14:24:20Z         567         103          461         1.6(2.3)   309(192)             3             0        32230         43200            1         103         103
2009-05-14T14:24:40Z         588          88          497        1.8(2.26)   299(202)             3             0        13816         38656            1          88          88
2009-05-14T14:25:00Z         603          72          528        1.55(2.2)   320(211)             3             1        16492         45888            1          72          72
2009-05-14T14:25:20Z         608          49          556        1.4(2.14)   556(238)             3             0        26359         51904            1          49          49
2009-05-14T14:25:40Z         612          26          583       1.35(2.08)   522(258)             3             1        24531         52672            1          26          26
2009-05-14T14:26:00Z         612           1          608       1.25(2.03)   715(289)             3             1        28630         52672            1           1           1
20090514142601 CRAWL ENDING - Finished
2009-05-14T14:26:01Z         612           0          609          0(2.02)     0(289)             3             0        30217         52672            1           0           0
20090514142601 CRAWL ENDED - Finished

metadata://netarkivet.dk/crawl/logs/runtime-errors.log?heritrixVersion=1.14.3&harvestid=1&jobid=1 130.226.228.8 20090514142059 text/plain 0

metadata://netarkivet.dk/crawl/logs/uri-errors.log?heritrixVersion=1.14.3&harvestid=1&jobid=1 130.226.228.8 20090514142103 text/plain 1501
2009-05-14T14:21:03.005Z http://pagead2.googlesyndication.com/pagead/show_ads.js "http scheme specific part is too short: //" http://
2009-05-14T14:21:03.014Z http://pagead2.googlesyndication.com/pagead/show_ads.js "gnu.inet.encoding.IDNAException: Contains non-LDH characters. '+q+'" http://'+q+'/pagead/expansion_embed.js
2009-05-14T14:21:03.015Z http://pagead2.googlesyndication.com/pagead/show_ads.js "gnu.inet.encoding.IDNAException: Contains non-LDH characters. '+q+'" http://'+q+'/pagead/render_ads.js
2009-05-14T14:21:03.022Z http://pagead2.googlesyndication.com/pagead/show_ads.js "http scheme specific part is too short: //" http://
2009-05-14T14:21:03.023Z http://pagead2.googlesyndication.com/pagead/show_ads.js "http scheme specific part is too short: //" http://
2009-05-14T14:21:03.023Z http://pagead2.googlesyndication.com/pagead/show_ads.js "http scheme specific part is too short: //" https://
2009-05-14T14:21:03.023Z http://pagead2.googlesyndication.com/pagead/show_ads.js "http scheme specific part is too short: //" http://
2009-05-14T14:21:03.024Z http://pagead2.googlesyndication.com/pagead/show_ads.js "http scheme specific part is too short: //" http://
2009-05-14T14:21:03.024Z http://pagead2.googlesyndication.com/pagead/show_ads.js "http scheme specific part is too short: //" http://
2009-05-14T14:21:03.025Z http://pagead2.googlesyndication.com/pagead/show_ads.js "gnu.inet.encoding.IDNAException: Contains non-LDH characters. '+q+'" http://'+q+'/pagead/show_ads_sra.js

metadata://netarkivet.dk/crawl/logs/heritrix.out?heritrixVersion=1.14.3&harvestid=1&jobid=1 130.226.228.8 20090514142603 text/plain 12459
The Heritrix process is started in the following environment
 (note that some entries will be changed by the starting JVM):
CLASSPATH=/home/test/SPIL/lib/heritrix/lib/heritrix-1.14.3.jar:/home/test/SPIL/lib/heritrix/lib/bsh-2.0b4.jar:/home/test/SPIL/lib/heritrix/lib/commons-cli-1.0.jar:/home/test/SPIL/lib/heritrix/lib/commons-codec-1.3.jar:/home/test/SPIL/lib/heritrix/lib/commons-collections-3.1.jar:/home/test/SPIL/lib/heritrix/lib/commons-httpclient-3.1.jar:/home/test/SPIL/lib/heritrix/lib/commons-io-1.3.1.jar:/home/test/SPIL/lib/heritrix/lib/commons-lang-2.3.jar:/home/test/SPIL/lib/heritrix/lib/commons-logging-1.0.4.jar:/home/test/SPIL/lib/heritrix/lib/commons-net-1.4.1.jar:/home/test/SPIL/lib/heritrix/lib/commons-pool-1.3.jar:/home/test/SPIL/lib/heritrix/lib/dnsjava-2.0.3.jar:/home/test/SPIL/lib/heritrix/lib/fastutil-5.0.3-heritrix-subset-1.0.jar:/home/test/SPIL/lib/heritrix/lib/itext-1.2.0.jar:/home/test/SPIL/lib/heritrix/lib/jasper-compiler-tomcat-4.1.30.jar:/home/test/SPIL/lib/heritrix/lib/jasper-runtime-tomcat-4.1.30.jar:/home/test/SPIL/lib/heritrix/lib/javaswf-CVS-SNAPSHOT-1.jar:/home/test/SPIL/lib/heritrix/lib/je-3.3.75.jar:/home/test/SPIL/lib/heritrix/lib/jericho-html-2.6.jar:/home/test/SPIL/lib/heritrix/lib/jets3t-0.5.0.jar:/home/test/SPIL/lib/heritrix/lib/jetty-4.2.23.jar:/home/test/SPIL/lib/heritrix/lib/junit-3.8.2.jar:/home/test/SPIL/lib/heritrix/lib/libidn-0.5.9.jar:/home/test/SPIL/lib/heritrix/lib/mg4j-1.0.1.jar:/home/test/SPIL/lib/heritrix/lib/poi-2.0-RC1-20031102.jar:/home/test/SPIL/lib/heritrix/lib/poi-scratchpad-2.0-RC1-20031102.jar:/home/test/SPIL/lib/heritrix/lib/servlet-tomcat-4.1.30.jar:/home/test/SPIL/lib/dk.netarkivet.harvester.jar:/home/test/SPIL/lib/dk.netarkivet.archive.jar:/home/test/SPIL/lib/dk.netarkivet.viewerproxy.jar:/home/test/SPIL/lib/dk.netarkivet.monitor.jar
DISPLAY=localhost:10.0
G_BROKEN_FILENAMES=1
HISTSIZE=1000
HOME=/home/test
HOSTNAME=kb-test-har-002.kb.dk
INPUTRC=/etc/inputrc
JAVA_HOME=/usr/java/jdk1.6.0_07
LANG=en_US.UTF-8
LD_LIBRARY_PATH=/usr/java/jdk1.6.0_07/jre/lib/i386/server:/usr/java/jdk1.6.0_07/jre/lib/i386:/usr/java/jdk1.6.0_07/jre/../lib/i386
LESSOPEN=|/usr/bin/lesspipe.sh %s
LOGNAME=test
LS_COLORS=
MAIL=/var/spool/mail/test
NLSPATH=/usr/dt/lib/nls/msg/%L/%N.cat
OLDPWD=/home/test/SPIL/conf
PATH=/usr/java/jdk1.6.0_07/bin:/usr/kerberos/bin:/usr/local/bin:/bin:/usr/bin:/usr/X11R6/bin
PWD=/home/test/SPIL
SHELL=/bin/bash
SHLVL=3
SSH_ASKPASS=/usr/libexec/openssh/gnome-ssh-askpass
SSH_CLIENT=130.226.231.15 39288 22
SSH_CONNECTION=130.226.231.15 39288 130.226.228.8 22
USER=test
XFILESEARCHPATH=/usr/dt/app-defaults/%L/Dt
_=/usr/java/jdk1.6.0_07/bin/java
Process properties:
dk.netarkivet.settings.file=/home/test/SPIL/conf/settings_HarvestControllerApplication_high.xml
file.encoding=UTF-8
file.encoding.pkg=sun.io
file.separator=/
heritrix.version=1.14.3
java.awt.graphicsenv=sun.awt.X11GraphicsEnvironment
java.awt.printerjob=sun.print.PSPrinterJob
java.class.path=/home/test/SPIL/lib/dk.netarkivet.harvester.jar:/home/test/SPIL/lib/dk.netarkivet.archive.jar:/home/test/SPIL/lib/dk.netarkivet.viewerproxy.jar:/home/test/SPIL/lib/dk.netarkivet.monitor.jar:
java.class.version=50.0
java.endorsed.dirs=/usr/java/jdk1.6.0_07/jre/lib/endorsed
java.ext.dirs=/usr/java/jdk1.6.0_07/jre/lib/ext:/usr/java/packages/lib/ext
java.home=/usr/java/jdk1.6.0_07/jre
java.io.tmpdir=/tmp
java.library.path=/usr/java/jdk1.6.0_07/jre/lib/i386/server:/usr/java/jdk1.6.0_07/jre/lib/i386:/usr/java/jdk1.6.0_07/jre/../lib/i386:/usr/java/packages/lib/i386:/lib:/usr/lib
java.runtime.name=Java(TM) SE Runtime Environment
java.runtime.version=1.6.0_07-b06
java.security.manager=
java.security.policy=/home/test/SPIL/conf/security.policy
java.specification.name=Java Platform API Specification
java.specification.vendor=Sun Microsystems Inc.
java.specification.version=1.6
java.util.logging.config.file=/home/test/SPIL/conf/log_HarvestControllerApplication_high.prop
java.vendor=Sun Microsystems Inc.
java.vendor.url=http://java.sun.com/
java.vendor.url.bug=http://java.sun.com/cgi-bin/bugreport.cgi
java.version=1.6.0_07
java.vm.info=mixed mode
java.vm.name=Java HotSpot(TM) Server VM
java.vm.specification.name=Java Virtual Machine Specification
java.vm.specification.vendor=Sun Microsystems Inc.
java.vm.specification.version=1.0
java.vm.vendor=Sun Microsystems Inc.
java.vm.version=10.0-b23
line.separator=

org.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger
org.archive.crawler.frontier.AbstractFrontier.queue-assignment-policy=org.archive.crawler.frontier.HostnameQueueAssignmentPolicy,org.archive.crawler.frontier.IPQueueAssignmentPolicy,org.archive.crawler.frontier.BucketQueueAssignmentPolicy,org.archive.crawler.frontier.SurtAuthorityQueueAssignmentPolicy,dk.netarkivet.harvester.harvesting.DomainnameQueueAssignmentPolicy
os.arch=i386
os.name=Linux
os.version=2.4.21-32.0.1.ELsmp
path.separator=:
sun.arch.data.model=32
sun.boot.class.path=/usr/java/jdk1.6.0_07/jre/lib/resources.jar:/usr/java/jdk1.6.0_07/jre/lib/rt.jar:/usr/java/jdk1.6.0_07/jre/lib/sunrsasign.jar:/usr/java/jdk1.6.0_07/jre/lib/jsse.jar:/usr/java/jdk1.6.0_07/jre/lib/jce.jar:/usr/java/jdk1.6.0_07/jre/lib/charsets.jar:/usr/java/jdk1.6.0_07/jre/classes
sun.boot.library.path=/usr/java/jdk1.6.0_07/jre/lib/i386
sun.cpu.endian=little
sun.cpu.isalist=
sun.io.unicode.encoding=UnicodeLittle
sun.java.launcher=SUN_STANDARD
sun.jnu.encoding=UTF-8
sun.management.compiler=HotSpot Tiered Compilers
sun.os.patch.level=unknown
user.country=US
user.dir=/home/test/SPIL
user.home=/home/test
user.language=en
user.name=test
user.timezone=Europe/Copenhagen
Working directory: harvester_high/1_1242310856461
14:20:58.063 EVENT  Starting Jetty/4.2.23
14:20:58.281 EVENT  Started WebApplicationContext[/,Heritrix Console]
14:20:58.390 EVENT  Started SocketListener on 0.0.0.0:8392
14:20:58.390 EVENT  Started org.mortbay.jetty.Server@766a24
05/14/2009 14:20:58 +0000 INFO org.archive.crawler.Heritrix postRegister org.archive.crawler:guiport=8392,host=kb-test-har-002.kb.dk,jmxport=8393,name=Heritrix,type=CrawlService registered to MBeanServerId=kb-test-har-002.kb.dk_1242310857560, SpecificationVersion=1.4, ImplementationVersion=1.6.0_07-b06, SpecificationVendor=Sun Microsystems
Heritrix version: 1.14.3
05/14/2009 14:20:59 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:20:59 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: pendingJobs []
05/14/2009 14:20:59 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: addJob []
05/14/2009 14:20:59 +0000 WARNING org.archive.crawler.settings.CrawlSettingsSAXHandler$SimpleElementHandler endElement Unknown attribute 'bind-address' in 'file:/home/test/SPIL/harvester_high/1_1242310856461/order.xml', line: 219, column: 38
05/14/2009 14:20:59 +0000 WARNING org.archive.crawler.settings.CrawlSettingsSAXHandler$SimpleElementHandler endElement Unknown attribute 'overly-eager-link-detection' in 'file:/home/test/SPIL/harvester_high/1_1242310856461/order.xml', line: 239, column: 67
05/14/2009 14:20:59 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: pendingJobs []
05/14/2009 14:20:59 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: startCrawling []
05/14/2009 14:20:59 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:21:00 +0000 INFO org.archive.crawler.admin.CrawlJob postRegister org.archive.crawler:host=kb-test-har-002.kb.dk,jmxport=8393,mother=Heritrix,name=1-1-20090514142059273,type=CrawlService.Job registered to MBeanServerId=kb-test-har-002.kb.dk_1242310857560, SpecificationVersion=1.4, ImplementationVersion=1.6.0_07-b06, SpecificationVendor=Sun Microsystems
05/14/2009 14:21:00 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:21:00 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:21:20 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:21:20 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:21:40 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:21:40 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:22:01 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:22:01 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:22:21 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:22:21 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:22:41 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:22:41 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:23:01 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:23:01 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:23:21 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:23:21 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:23:41 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:23:41 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:24:01 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:24:01 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:24:21 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:24:21 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:24:42 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:24:42 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:25:02 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:25:02 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:25:22 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:25:22 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:25:42 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:25:42 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:26:01 +0000 WARNING org.archive.crawler.settings.CrawlSettingsSAXHandler$SimpleElementHandler endElement Unknown attribute 'bind-address' in 'file:/home/test/SPIL/harvester_high/1_1242310856461/order.xml', line: 219, column: 38
05/14/2009 14:26:01 +0000 WARNING org.archive.crawler.settings.CrawlSettingsSAXHandler$SimpleElementHandler endElement Unknown attribute 'overly-eager-link-detection' in 'file:/home/test/SPIL/harvester_high/1_1242310856461/order.xml', line: 239, column: 67
05/14/2009 14:26:02 +0000 INFO org.archive.crawler.admin.CrawlJob postDeregister org.archive.crawler:host=kb-test-har-002.kb.dk,jmxport=8393,mother=Heritrix,name=1-1-20090514142059273,type=CrawlService.Job unregistered from MBeanServerId=kb-test-har-002.kb.dk_1242310857560, SpecificationVersion=1.4, ImplementationVersion=1.6.0_07-b06, SpecificationVendor=Sun Microsystems
05/14/2009 14:26:02 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: completedJobs []
05/14/2009 14:26:02 +0000 INFO org.archive.crawler.Heritrix invoke JMX invoke: shutdown []
05/14/2009 14:26:02 +0000 INFO org.archive.crawler.Heritrix postDeregister org.archive.crawler:guiport=8392,host=kb-test-har-002.kb.dk,jmxport=8393,name=Heritrix,type=CrawlService unregistered from MBeanServerId=kb-test-har-002.kb.dk_1242310857560, SpecificationVersion=1.4, ImplementationVersion=1.6.0_07-b06, SpecificationVendor=Sun Microsystems
14:26:02.481 EVENT  Stopping Acceptor ServerSocket[addr=0.0.0.0/0.0.0.0,port=0,localport=8392]
14:26:02.481 EVENT  Stopped SocketListener on 0.0.0.0:8392
14:26:02.482 EVENT  Stopped WebApplicationContext[/,Heritrix Console]
14:26:02.483 EVENT  Stopped org.mortbay.http.NCSARequestLog@3de6df
14:26:02.483 EVENT  Stopped org.mortbay.jetty.Server@766a24
14:26:02.483 EVENT  Stopped WebApplicationContext[/,Heritrix Console]
14:26:02.483 EVENT  Stopped org.mortbay.jetty.Server@766a24

metadata://netarkivet.dk/crawl/index/cdx?majorversion=1&minorversion=0&harvestid=1&jobid=1&timestamp=20090514142103&serialno=00002 130.226.228.8 20090514142609 application/x-cdx 38615
http://ad.yieldmanager.com/robots.txt 77.238.174.11 20090514142103 text/plain 280 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 1473 d02aa85ac3fc536a04c52c65baab9c94
http://www.macromedia.com/robots.txt 192.150.18.118 20090514142103 text/html 522 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 1836 b7b325de85aabfcdb3ce15bd872fc59e
http://googleads.g.doubleclick.net/ 74.125.79.156 20090514142104 text/html 439 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2440 b050bcc6e4cb1da926e17b86302c4711
http://googleads.g.doubleclick.net/pagead/ 74.125.79.156 20090514142104 text/html 440 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2959 d41620c463562ded856ae063fdb55d1d
http://www.macromedia.com/go/getflashplayer 192.150.18.118 20090514142104 text/html 626 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3486 e73ff7a00977eecd79151d016e023d87
dns:fpdownload2.macromedia.com 130.226.220.16 20090514142105 text/dns 97 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4201 20ff3208379391f31d2db4a22c0b4fce
http://fpdownload2.macromedia.com/get/shockwave/cabs/flash/swflash.cab 92.123.64.18 20090514142106 application/x-cab-compressed 1884214 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4372 cdd7547878e4433adafc533a075052ed
http://carlsencards.com/images/top8_micardsa.gif 80.196.101.244 20090514142108 image/gif 2348 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 1888723 98389c43d9aeeb4ae60eba62557e62cb
http://carlsencards.com/carlsencards.css 80.196.101.244 20090514142109 text/css 3218 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 1891166 2f321dcfe86941baddd9d50397e3ddb6
http://carlsencards.com/images/top1.gif 80.196.101.244 20090514142110 image/gif 745 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 1894470 cd886812d009c239f9bb13ba64d9c77b
http://carlsencards.com/images/topStore_up.gif 80.196.101.244 20090514142111 image/gif 2088 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 1895300 6b5d9a250e0b163cedb6009e8e330b45
http://carlsencards.com/images/aboutCat 80.196.101.244 20090514142112 text/html 410 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 1897481 0def2268b24c721826aaac45045de895
dns:s9.addthis.com 130.226.220.16 20090514142113 text/dns 61 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 1897976 2e95c7cd506cf74adfc7b29280d8ad3f
http://carlsencards.com/images/headerWontbecomingtowork.gif 80.196.101.244 20090514142114 image/gif 2230 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 1898099 8f3158dffecdb38a7535857ba90b13dd
http://s9.addthis.com/js/widget.php?v=10 92.123.220.20 20090514142114 text/html 9566 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 1900435 2ffeea50bb529a148d1e64dc3be6ea81
http://carlsencards.com/images/newCardTopFrontpg_fortTellR.jpg 80.196.101.244 20090514142115 image/jpeg 22996 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 1910087 166eaa26350df5068d1192173b188ca2
http://carlsencards.com/images/seatFillers/hollaMouse.swf 80.196.101.244 20090514142116 application/x-shockwave-flash 13821 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 1933194 266435cda0e07f0a76b4665cabc90591
http://carlsencards.com/images/extLinks/ofree_mini_3.gif 80.196.101.244 20090514142117 image/gif 1496 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 1947140 68e5108d0a574fa2c065fcf1c3afa9db
http://carlsencards.com/images/seatFillers/penguinJoke.swf 80.196.101.244 20090514142118 application/x-shockwave-flash 12606 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 1948739 a01a01aab383702f7aa670cb555a6af9
http://carlsencards.com/images/headerNoSpecOcc.gif 80.196.101.244 20090514142119 image/gif 1524 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 1961471 caa48506b8677ffb0d821494f28f81fd
http://carlsencards.com/images/free-ecard-thumb56.jpg 80.196.101.244 20090514142120 image/jpeg 39910 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 1963092 2f3847ed760f7b6d65becb92bfbd10b9
http://carlsencards.com/images/campBoxBtm.gif 80.196.101.244 20090514142121 image/gif 495 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2003104 309758313f1932d567f7c30839453d53
http://carlsencards.com/images/free-ecard-thumb53.jpg 80.196.101.244 20090514142122 image/jpeg 27432 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2003690 e53bc4fcfdcf9901fe84dbd31d4131a9
http://carlsencards.com/images/headerHappyAnniversary.gif 80.196.101.244 20090514142124 image/gif 1547 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2031224 5d9fba286222eedba35a47bec3a9606a
http://carlsencards.com/images/seatFillers/musicRadio.swf 80.196.101.244 20090514142125 application/x-shockwave-flash 301666 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2032875 35f4c3da667b39ad3b3b3a041c2e8e8b
http://carlsencards.com/images/free-ecard-thumb45.jpg 80.196.101.244 20090514142126 image/jpeg 28653 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2334667 e8eed4b54b8f82de5c12a1c636ade5d1
http://carlsencards.com/images/free-ecard-thumb16.jpg 80.196.101.244 20090514142127 image/jpeg 31363 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2363422 096f03b3cbe632767155caab8a0ed2c1
http://carlsencards.com/images/free-ecard-thumb35.jpg 80.196.101.244 20090514142128 image/jpeg 29206 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2394887 e186e819f27151fb0078953209df643f
http://carlsencards.com/images/headerHoliday.gif 80.196.101.244 20090514142129 image/gif 1415 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2424195 d1486259660e5001e0554e14abdb5f4b
http://carlsencards.com/images/free-ecard-thumb36.jpg 80.196.101.244 20090514142130 image/jpeg 30350 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2425705 2fd23a7b1f278fe51161f8f338afc68a
http://carlsencards.com/images/headerLinks2.gif 80.196.101.244 20090514142131 image/gif 1606 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2456157 0bdee6eb8ace4c8511c3f322c88fcf73
http://carlsencards.com/images/free-ecard-thumb54.jpg 80.196.101.244 20090514142132 image/jpeg 31607 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2457857 c5a791015359a85cf410d0d3da5a8fc1
http://carlsencards.com/images/free-ecard-thumb49.jpg 80.196.101.244 20090514142133 image/jpeg 35374 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2489566 ebdd909c386ebd9dbcc0d02f2fc275f4
http://carlsencards.com/images/free-ecard-thumb48.jpg 80.196.101.244 20090514142135 image/jpeg 21419 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2525042 22537ab7e66590410a6863d6a543b43a
http://carlsencards.com/images/free-ecard-thumb08.jpg 80.196.101.244 20090514142136 image/jpeg 31964 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2546563 4d78ae4434f49e697ed0afbb00dc3635
http://carlsencards.com/images/extLinks/freesitee.gif 80.196.101.244 20090514142137 image/gif 3226 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2578629 27ca47aa86dc7c8bd53bc012dffb1ed1
http://carlsencards.com/images/free-ecard-thumb34.jpg 80.196.101.244 20090514142138 image/jpeg 27039 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2581955 2828e5954ea0a5ccaeb656a2b9a0fcbf
http://carlsencards.com/images/free-ecard-thumb46.jpg 80.196.101.244 20090514142139 image/jpeg 40500 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2609096 fc06c057f7a0bcd048ac18a56465bcca
http://carlsencards.com/images/free-ecard-thumb55.jpg 80.196.101.244 20090514142140 image/jpeg 27490 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2649698 2f581bff9917cdf84e9dadec58312f06
http://carlsencards.com/images/free-ecard-thumb12.jpg 80.196.101.244 20090514142141 image/jpeg 23284 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2677290 9c7a23fec6f81bd27e65c209ca7d2b81
http://carlsencards.com/images/headerCongrats.gif 80.196.101.244 20090514142142 image/gif 1435 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2700676 2b8c3ec216bc958d786e3cb25765962d
http://carlsencards.com/images/headerHappybirthday.gif 80.196.101.244 20090514142143 image/gif 1303 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2702207 c1d1893c41274f57482f7a4c8c890cf2
http://carlsencards.com/images/campBoxTop.gif 80.196.101.244 20090514142144 image/gif 494 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2703611 b396335a82bff038e3946d82b118dec7
http://carlsencards.com/images/free-ecard-thumb07.jpg 80.196.101.244 20090514142145 image/jpeg 30603 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2704196 aaf29d68b1245ed5348f2647944d78ef
http://carlsencards.com/free-ecard-58-congratulations-hamster-on-strike.htm 80.196.101.244 20090514142146 text/html 13051 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2734901 120ec1599e686a9f38e7d290b6da8f7f
http://carlsencards.com/images/extLinks/aford_promobanner_31x88.gif 80.196.101.244 20090514142149 image/gif 1319 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2748075 1005b523eb37b24dff77a7ec13f97bd2
http://carlsencards.com/images/contactCat 80.196.101.244 20090514142150 text/html 412 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2749508 8cdf1524cdccf9ac3fe2fa151da221a8
http://carlsencards.com/Ecard51.swf 80.196.101.244 20090514142151 application/x-shockwave-flash 480006 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 2750007 155e78d35be9752fd86d00cbca474f6f
http://www.carlsencards.com/index.htm 80.196.101.244 20090514142153 text/html 116851 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3230117 e955fe17e46436fa8ad473977fec40f3
http://www.carlsencards.com/carlsencards.css 80.196.101.244 20090514142154 text/css 3218 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3347054 07d53abe1b8b90a8eb105f301d037af1
http://www.carlsencards.com/images/free-ecard-thumb25.jpg 80.196.101.244 20090514142155 image/jpeg 28695 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3350362 3725919ff036728e4d17d1308eef7f14
http://www.carlsencards.com/images/headerGetwellsoon.gif 80.196.101.244 20090514142156 image/gif 1290 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3379163 b9161ce478199d39a5cb78842974763d
http://www.carlsencards.com/images/free-ecard-thumb55.jpg 80.196.101.244 20090514142157 image/jpeg 27490 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3380556 43970e37303e65d094530273e531a60f
http://www.carlsencards.com/images/free-ecard-thumb34.jpg 80.196.101.244 20090514142158 image/jpeg 27039 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3408152 e841d9226e85ff8be92c31deab50421a
http://www.carlsencards.com/images/free-ecard-thumb14.jpg 80.196.101.244 20090514142159 image/jpeg 25265 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3435297 33515c7b50923587e88fe9cf8743bbff
http://www.carlsencards.com/images/seatFillers/freeTheFliesGame.swf 80.196.101.244 20090514142200 application/x-shockwave-flash 38171 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3460668 7a8f506765c112187aca7dffd8a7b20a
http://www.carlsencards.com/images/free-ecard-thumb54.jpg 80.196.101.244 20090514142201 image/jpeg 31607 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3498974 59be46cf7a918c7cdad6f2e859e999e6
http://www.carlsencards.com/images/headerNewBaby.gif 80.196.101.244 20090514142202 image/gif 2108 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3530687 3c07067f5eb020dd70748fe877d88565
http://www.carlsencards.com/Scripts/AC_RunActiveContent.js 80.196.101.244 20090514142203 application/x-javascript 3541 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3532894 7adbd2bfb889f71cde879e06d64e571e
http://www.carlsencards.com/images/free-ecard-thumb27.jpg 80.196.101.244 20090514142204 image/jpeg 26845 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3536555 ac41a87eebc1e69acae24d1afd249d61
http://www.carlsencards.com/images/free-ecard-thumb19.jpg 80.196.101.244 20090514142206 image/jpeg 20364 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3563506 44f4622d8806440239537c127a516a6b
http://www.carlsencards.com/images/headerWontbecomingtowork.gif 80.196.101.244 20090514142207 image/gif 2230 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3583976 f9fc437fd9855dba47b47c585540cb6a
http://www.carlsencards.com/images/top3.gif 80.196.101.244 20090514142208 image/gif 611 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3586316 adab6418faef1ffbd95d5b56c72ee97d
http://www.carlsencards.com/images/free-ecard-thumb24.jpg 80.196.101.244 20090514142209 image/jpeg 12852 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3587016 b6cf48167306c687735c7e539a7e28c0
http://www.carlsencards.com/images/free-ecard-thumb47.jpg 80.196.101.244 20090514142210 image/jpeg 24638 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3599974 cdacc569f9f744e526945ac0fd4e2b0c
http://www.carlsencards.com/images/free-ecard-thumb48.jpg 80.196.101.244 20090514142211 image/jpeg 21419 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3624718 051fd171d85139448ecbab20ba9b49c9
http://www.carlsencards.com/images/free-ecard-thumb31.jpg 80.196.101.244 20090514142212 image/jpeg 25735 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3646243 c78c7fdbdf9b7afaf4ec5073ff6f26f2
http://www.carlsencards.com/images/topStore_up.gif 80.196.101.244 20090514142213 image/gif 2088 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3672084 7a9d11033ab564aa2e5a43badd17b2f0
http://www.carlsencards.com/images/headerIhateYou.gif 80.196.101.244 20090514142214 image/gif 1114 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3674269 d976f813b3ec4a35c167942f6d83a1e4
http://www.carlsencards.com/images/free-ecard-thumb49.jpg 80.196.101.244 20090514142215 image/jpeg 35374 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3675483 c0d3f2f2e39210c701466d8414df807e
http://www.carlsencards.com/images/bottom.gif 80.196.101.244 20090514142216 image/gif 1253 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3710963 881bc16f650fe26ed9b8f3a34815256d
http://www.carlsencards.com/images/free-ecard-thumb41.jpg 80.196.101.244 20090514142217 image/jpeg 23607 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3712308 d0e58b72625b76c4051bb88cfac7f894
http://www.carlsencards.com/images/free-ecard-thumb26.jpg 80.196.101.244 20090514142218 image/jpeg 27102 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3736021 b7cd9964a7d9500fac618383b84470aa
http://www.carlsencards.com/images/newCardTopFrontpgHBFlowers.jpg 80.196.101.244 20090514142219 image/jpeg 38041 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3763229 4dffaa041a788893663a01cbc4f6cdc0
http://www.carlsencards.com/images/headerCongrats.gif 80.196.101.244 20090514142220 image/gif 1435 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3801384 ba12d4f9854b46573271e1600c003b20
http://www.carlsencards.com/images/line.gif 80.196.101.244 20090514142221 image/gif 522 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3802919 5c2913e17d5890813f4b6b38d218f6e1
http://www.carlsencards.com/images/free-ecard-thumb21.jpg 80.196.101.244 20090514142222 image/jpeg 26240 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3803530 e2028e8939b3924f65a9c0dd595f4d9a
http://www.carlsencards.com/images/free-ecard-thumb51.jpg 80.196.101.244 20090514142223 image/jpeg 30422 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3829876 127c269e61754bee77e973e971aefe71
http://www.carlsencards.com/images/free-ecard-thumb07.jpg 80.196.101.244 20090514142226 image/jpeg 30603 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3860404 d5817fd03ad88663aa27608bfa2852d8
http://www.carlsencards.com/images/campBoxTile.jpg 80.196.101.244 20090514142227 image/jpeg 710 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3891113 233c58928e8a7eb0be7e8b4f7b1d32ac
http://www.carlsencards.com/images/free-ecard-thumb44.jpg 80.196.101.244 20090514142228 image/jpeg 39910 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3891920 5972474ce52e77b9413c4508b8ff087b
http://www.carlsencards.com/images/free-ecard-thumb00.jpg 80.196.101.244 20090514142229 image/jpeg 21397 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3931936 5b1c4003b47266f0aed3b07e49aec46f
http://www.carlsencards.com/images/free-ecard-thumb46.jpg 80.196.101.244 20090514142230 image/jpeg 40500 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3953439 f1d030efec9960b6ecb04ac3187c4328
http://www.carlsencards.com/outgoing/cafepress 80.196.101.244 20090514142232 text/html 413 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3994045 c9a2e37c7d672eca47d7824dd1d102d9
http://www.carlsencards.com/images/free-ecard-thumb13.jpg 80.196.101.244 20090514142233 image/jpeg 24080 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 3994550 02164fb46a85a99c52d449022645539b
http://www.carlsencards.com/images/free-ecard-thumb05.jpg 80.196.101.244 20090514142234 image/jpeg 25080 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4018736 9992394e88996bfb3eba7c4e10648c4e
http://carlsencards.com/sitemap.htm 80.196.101.244 20090514142235 text/html 16266 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4043922 af459fe480d143c3680dda94935c5dcc
http://carlsencards.com/1.8.1 80.196.101.244 20090514142236 text/html 400 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4060271 59e5604cd3dcff37553db6fde8a3f602
http://carlsencards.com/ShockwaveFlash.ShockwaveFlash.7 80.196.101.244 20090514142237 text/html 426 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4060746 ed0f2524b37787a5b4ee5ecdf2b99b57
http://carlsencards.com/0.01 80.196.101.244 20090514142238 text/html 399 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4061273 222ce9f21eea9217884fb68f4a3f51f2
http://carlsencards.com/ShockwaveFlash.ShockwaveFlash.6 80.196.101.244 20090514142239 text/html 426 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4061746 c0be68be4fd182ee906e9a4c2756df61
http://carlsencards.com/notify2.php 80.196.101.244 20090514142240 text/html 205 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4062273 4be85bd337645f6e3eafbd84ace8a848
http://carlsencards.com/cat-free-ecards-congratulations.htm 80.196.101.244 20090514142241 text/html 21973 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4062559 bf12f22a1e45bba79a3594b7520d4e27
http://carlsencards.com/free-ecard-04-im-sorry-dog-interactive.htm 80.196.101.244 20090514142243 text/html 13048 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4084639 6ba51d69e06f7444a494aae07bf1978a
http://carlsencards.com/free-ecard-35-happy-valentines-day-jungle-love-lemurs.htm 80.196.101.244 20090514142244 text/html 13256 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4097801 78d3b4efe5475a4958b44681306e91fa
http://carlsencards.com/free-ecard-33-happy-valentines-day-funny-cats.htm 80.196.101.244 20090514142245 text/html 13272 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4111186 355b6bc68164815a73ed7285e5a841e5
http://carlsencards.com/free-ecard-37-congratulations-bored-funny-cats.htm 80.196.101.244 20090514142247 text/html 13120 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4124579 afd34d5f49cf7808747fdd4301f61bf5
http://carlsencards.com/cat-free-ecards-i-love-you.htm 80.196.101.244 20090514142249 text/html 17623 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4137821 b2d1b4a9684cb59901f83b027020f07e
http://carlsencards.com/free-ecard-27-happy-thanksgiving-funny-cats.htm 80.196.101.244 20090514142250 text/html 13127 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4155546 b7fac5016542ede5394569709408d55b
http://carlsencards.com/free-ecard-34-happy-valentines-day-romantic-mice.htm 80.196.101.244 20090514142253 text/html 13200 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4168792 7abd6ac87104762600929bf2fe085df9
http://carlsencards.com/Ecard14.swf 80.196.101.244 20090514142254 application/x-shockwave-flash 517431 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4182116 9b452c0b913affb7db8e1a2eb4f01144
http://carlsencards.com/free-ecard-03-wont-come-to-work-mona-lisa.htm 80.196.101.244 20090514142256 text/html 13146 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4699651 fbc079c16c9d32a2e19419de83e221bb
http://carlsencards.com/free-ecard-20-happy-easter-eggs-jurassic.htm 80.196.101.244 20090514142258 text/html 13085 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4712914 d7903df5d973061f721cc305f7a5d9e1
http://carlsencards.com/images/seatFillers/freeTheFliesGame.swf 80.196.101.244 20090514142300 application/x-shockwave-flash 38171 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4726115 c74933e40619d43147c6865cf6587832
http://carlsencards.com/free-ecard-57-no-special-occasion-FAPSS.htm 80.196.101.244 20090514142305 text/html 13224 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4764417 36579888448e180e5eee5b88d66ded84
http://carlsencards.com/free-ecard-00-spread-the-word-tell-your-friends.htm 80.196.101.244 20090514142307 text/html 13230 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4777756 dc6f2681fd14ab383c1c7fb68be11214
http://carlsencards.com/Scripts/swfobject_modified.js 80.196.101.244 20090514142309 application/x-javascript 22006 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4791109 85e8b04bb9e81a07bd1ac3bd93c1caf2
http://carlsencards.com/free-ecard-29-merry-christmas-cat-mice-rudolph-prank.htm 80.196.101.244 20090514142310 text/html 13172 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4813231 c764e4c9d1bf4637d835689c7fc93325
http://carlsencards.com/images/seatFillers/xmasPenguins.swf 80.196.101.244 20090514142312 application/x-shockwave-flash 27342 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4826531 b1b29354fc148ae814e846e8a5fe0813
http://carlsencards.com/free-ecard-21-get-well-cheerleader-mice.htm 80.196.101.244 20090514142314 text/html 13137 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4854000 a577f2b8de0b7c3b70cf0f76cabb6af7
http://carlsencards.com/free-ecard-45-thank-you-dumpster-rat.htm 80.196.101.244 20090514142317 text/html 13261 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4867252 355d2b70d673be249c6f437cf63f1d53
http://carlsencards.com/Ecard56.swf 80.196.101.244 20090514142319 application/x-shockwave-flash 781600 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 4880625 89a18be7f2b262f85f59b9fdea34d1fe
http://carlsencards.com/free-ecard-30-i-hate-you-cat-toy-car-accident.htm 80.196.101.244 20090514142321 text/html 13100 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 5662329 301e08b12e61b07f4c3cd16ed5362ecb
http://carlsencards.com/free-ecard-16-i-love-you-sheep-rock-my-world.htm 80.196.101.244 20090514142322 text/html 13100 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 5675550 fdbafa7d929f0d1101f169023032da6c
http://carlsencards.com/free-ecard-17-wont-come-to-work-scream-existential-angst.htm 80.196.101.244 20090514142324 text/html 13138 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 5688770 2e54e053a1c166974613a49d9a21157c
http://carlsencards.com/free-ecard-31-no-special-occasion-cat-mouse-flash-game.htm 80.196.101.244 20090514142326 text/html 13401 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 5702040 2ba98d7ad2eb7937908f0dfb5b6a3899
http://carlsencards.com/Ecard25.swf 80.196.101.244 20090514142328 application/x-shockwave-flash 492505 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 5715571 34c1c4384c3420bd137d0994e2554a9d
http://carlsencards.com/Ecard15.swf 80.196.101.244 20090514142329 application/x-shockwave-flash 785056 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 6208180 270268cc88a3cff9daf6fd54786237c9
http://carlsencards.com/Ecard36.swf 80.196.101.244 20090514142332 application/x-shockwave-flash 809024 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 6993340 7e1322149b51e1cd324aafce4637db9b
http://carlsencards.com/free-ecard-02-happy-birthday-piano-playing-mice.htm 80.196.101.244 20090514142334 text/html 13135 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 7802468 e81bc5203f9e1d97e0faefcfc0ea39df
http://carlsencards.com/free-ecard-50-congratulations-pimp-my-ecard.htm 80.196.101.244 20090514142335 text/html 13144 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 7815726 e8b8ce55a279de25c130a0f16f75ed3f
http://carlsencards.com/free-ecard-48-halloween-in-ghost-town.htm 80.196.101.244 20090514142338 text/html 13150 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 7828989 fe212c51ea7acda3cfecb7685dc172de
http://carlsencards.com/free-ecard-32-happy-new-year-raccoon-party-fireworks.htm 80.196.101.244 20090514142340 text/html 13206 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 7842252 f361214af2698198a6f1b62a11c670b3
http://carlsencards.com/cat-free-ecards-happy-anniversary.htm 80.196.101.244 20090514142341 text/html 16038 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 7855586 4cf31a5f64c9567b3469acee32251418
http://carlsencards.com/cat-free-ecards-spread-the-word.htm 80.196.101.244 20090514142343 text/html 16306 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 7871733 e668c254995e35ee51dd2e4e35f3fe5e
http://carlsencards.com/Ecard40.swf 80.196.101.244 20090514142344 application/x-shockwave-flash 582585 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 7888146 43035ce5eed01196454e201e9bcfd069
http://carlsencards.com/cat-free-ecards-get-well-soon.htm 80.196.101.244 20090514142346 text/html 15979 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 8470835 0e5baf209875aeb5d16fa5b80ff48756
http://www.carlsencards.com/notify2.php 80.196.101.244 20090514142347 text/html 205 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 8486919 7554c0b0390a981f5a933ae898ddf0b3
http://www.carlsencards.com/Ecard59.swf 80.196.101.244 20090514142348 application/x-shockwave-flash 867938 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 8487209 a2c7a461d3f4f55debd86249c50fbe1e
http://www.carlsencards.com/free-ecard-19-merry-christmas-penguins-writing-snow.htm 80.196.101.244 20090514142351 text/html 13065 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 9355255 ed20cc039ab3132cc226d9f0e7ed5616
http://www.carlsencards.com/sitemap.htm 80.196.101.244 20090514142352 text/html 16266 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 9368451 ec95e0164403a20323991eda6119d8a1
http://www.carlsencards.com/contact.htm 80.196.101.244 20090514142353 text/html 13360 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 9384804 44b7d5f29d0c27d4d933861462e5c921
http://www.carlsencards.com/free-ecard-58-congratulations-hamster-on-strike.htm 80.196.101.244 20090514142354 text/html 13051 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 9398251 91e002a6358039ca6d6cc710cb32806d
http://www.carlsencards.com/free-ecard-12-im-sorry-little-bear-big-words.htm 80.196.101.244 20090514142356 text/html 13121 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 9411429 78cdebb4c74b482c7b91daa9339863e1
http://www.carlsencards.com/images/seatFillers/musicRadio.swf 80.196.101.244 20090514142358 application/x-shockwave-flash 301666 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 9424674 73f5f015af39e54321676762023cc2ff
http://www.carlsencards.com/free-ecard-39-happy-easter-dancing-eggs.htm 80.196.101.244 20090514142359 text/html 13100 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 9726470 4dd2d2512ceb0efe7550ba08551caa7a
http://www.carlsencards.com/free-ecard-06-happy-birthday-devil-angel-cake.htm 80.196.101.244 20090514142401 text/html 13153 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 9739689 b93a4d37ca1e626550f9ef35f826be53
http://www.carlsencards.com/free-ecard-26-halloween-mouse-in-pumpkin-explosion.htm 80.196.101.244 20090514142402 text/html 13114 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 9752967 424b70f07f4759286296ab13d261853c
http://www.carlsencards.com/Ecard0.swf 80.196.101.244 20090514142405 application/x-shockwave-flash 902226 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 9766211 f4fa4c15c2e7c01b81b62f617d348a12
http://www.carlsencards.com/Ecard33.swf 80.196.101.244 20090514142407 application/x-shockwave-flash 559988 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 10668544 fe87a23423aa56e4a75869da65f1a132
http://www.carlsencards.com/Ecard43.swf 80.196.101.244 20090514142409 application/x-shockwave-flash 566782 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 11228640 3d6815cc3c0f29cead51ba02ddc0c7d6
http://www.carlsencards.com/free-ecard-46-thank-you-lemur-choir.htm 80.196.101.244 20090514142411 text/html 13315 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 11795530 ecf4a301961b0dd963e14159b7ca2d2e
http://www.carlsencards.com/free-ecard-21-get-well-cheerleader-mice.htm 80.196.101.244 20090514142412 text/html 13137 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 11808960 4071e38508c22153bcfa5067371afa0f
http://www.carlsencards.com/cat-free-ecards-happy-birthday.htm 80.196.101.244 20090514142414 text/html 21337 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 11822216 cbf3639158a04e470de9f0b70c4a9722
http://www.carlsencards.com/cat-free-ecards-thank-you.htm 80.196.101.244 20090514142415 text/html 15748 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 11843663 502dc75438a312f649b7b60dbf1a70e6
http://www.carlsencards.com/free-ecard-16-i-love-you-sheep-rock-my-world.htm 80.196.101.244 20090514142417 text/html 13100 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 11859516 38067e6aec116025e83e077900143ca3
http://www.carlsencards.com/ecards-and-free-stuff-links.htm 80.196.101.244 20090514142420 text/html 21226 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 11872740 06da1313e1ece3ec6a3de14d3468d81a
http://www.carlsencards.com/free-ecard-09-congratulations-penguins-versus-animator.htm 80.196.101.244 20090514142421 text/html 13069 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 11894073 8925dcd52ba65bfa18f5cd3d8c5d6921
http://www.carlsencards.com/images/aboutCat 80.196.101.244 20090514142423 text/html 410 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 11907276 cf81ec4d50e5a23dfedb88686a99b684
http://www.carlsencards.com/free-ecard-48-halloween-in-ghost-town.htm 80.196.101.244 20090514142424 text/html 13150 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 11907775 c7cc541dfe4d11759f57ae18b1bf50fc
http://www.carlsencards.com/cat-free-ecards-happy-anniversary.htm 80.196.101.244 20090514142425 text/html 16038 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 11921042 9553a2e6468e0ed2f97b867ffa062af9
http://www.carlsencards.com/free-ecard-42-happy-mothers-day-llamas.htm 80.196.101.244 20090514142427 text/html 13233 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 11937193 e69d0f09aed49bef5680f05aea1eb3e7
http://www.carlsencards.com/Ecard3.swf 80.196.101.244 20090514142428 application/x-shockwave-flash 574904 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 11950544 5101af3e00895547f3e8821241c22cc9
http://www.carlsencards.com/free-ecard-35-happy-valentines-day-jungle-love-lemurs.htm 80.196.101.244 20090514142430 text/html 13256 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 12525555 38edcea7b57f1410aa8747fe8114d924
http://www.carlsencards.com/free-ecard-55-happy-anniversary-lemurs.htm 80.196.101.244 20090514142432 text/html 13127 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 12538944 02a95d228674a0f82229f558918af8c8
http://www.carlsencards.com/free-ecard-34-happy-valentines-day-romantic-mice.htm 80.196.101.244 20090514142434 text/html 13200 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 12552189 ddb16ff61a8c3971257dc7bb20f6b7fe
http://www.carlsencards.com/free-ecard-41-happy-mothers-day-polar-bears.htm 80.196.101.244 20090514142436 text/html 13221 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 12565517 ae922d6cd0836c9b8672c3c679b543d3
http://www.carlsencards.com/Ecard7.swf 80.196.101.244 20090514142437 application/x-shockwave-flash 773226 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 12578861 68f5c0c0bffae1140d4be4e51f7985b6
http://www.carlsencards.com/cat-free-ecards-sorry.htm 80.196.101.244 20090514142440 text/html 15870 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 13352194 61b86688f596dfa67c02efbff571a70a
http://www.carlsencards.com/free-ecard-11-congratulations-african-drummer-ants.htm 80.196.101.244 20090514142441 text/html 13123 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 13368165 87d80290a62071e49b9691a3dd1eed50
http://www.carlsencards.com/free-ecard-50-congratulations-pimp-my-ecard.htm 80.196.101.244 20090514142443 text/html 13144 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 13381418 41e80a6ef87d4fb44ae6f0975008bc3d
http://www.carlsencards.com/free-ecard-45-thank-you-dumpster-rat.htm 80.196.101.244 20090514142445 text/html 13261 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 13394685 8eb90a4766dd70188e8c220589d38375
http://www.carlsencards.com/Ecard15.swf 80.196.101.244 20090514142447 application/x-shockwave-flash 785056 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 13408062 25e366f5ee0d5c2332d16d36a248bff6
http://www.carlsencards.com/free-ecard-02-happy-birthday-piano-playing-mice.htm 80.196.101.244 20090514142449 text/html 13135 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 14193226 4179a96d29277d33d87d95c59b3203bc
http://www.carlsencards.com/free-ecard-20-happy-easter-eggs-jurassic.htm 80.196.101.244 20090514142451 text/html 13085 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 14206488 57d372880b374a9b02478d2353e6bf15
http://www.carlsencards.com/Ecard52.swf 80.196.101.244 20090514142453 application/x-shockwave-flash 826177 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 14219693 7330680fc097aa4b01d9a14a6bdb5f55
http://www.carlsencards.com/Ecard36.swf 80.196.101.244 20090514142455 application/x-shockwave-flash 809024 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 15045978 f8556262eb81515867bdb02ba66ca6de
http://www.carlsencards.com/cat-free-ecards-i-love-you.htm 80.196.101.244 20090514142457 text/html 17623 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 15855110 95b01c5971278f9fd470e286fb84ecc6
http://carlsencards.com/SendEcard.php 80.196.101.244 20090514142459 text/html 214 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 15872839 4ccadb0df8e7135f6e5b7e8907330e20
http://carlsencards.com/_root.sound 80.196.101.244 20090514142501 text/html 406 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 15873136 89a09a41a4893cbbf0ecc09e89ce67e7
http://carlsencards.com/Ecard33.swf 80.196.101.244 20090514142504 application/x-shockwave-flash 559988 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 15873623 dc9a3234772bbf72cfec59cc885b722f
http://carlsencards.com/Ecard19.swf 80.196.101.244 20090514142506 application/x-shockwave-flash 809442 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 16433715 f744f7c1abd364b9281f6fce9b28f5bb
http://carlsencards.com/www.carlsencards.com 80.196.101.244 20090514142509 text/html 415 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 17243261 567f8d65ff113f5f9d9b3c793a7929ae
http://carlsencards.com/Download.Failed 80.196.101.244 20090514142512 text/html 410 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 17243766 dad9adee14c4bd12db72a5651b2183fc
http://carlsencards.com/fpdownload.macromedia.com 80.196.101.244 20090514142513 text/html 420 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 17244261 ef2a6a039c53d5e71d0d3afce589bf84
http://carlsencards.com/text/css 80.196.101.244 20090514142514 text/html 403 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 17244776 de7b3eacfbd81fc681fdc8676a02d6f3
http://carlsencards.com/Ecard18.swf 80.196.101.244 20090514142515 application/x-shockwave-flash 529718 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 17245257 06545c3618d8111b6129eb328ccc2244
http://carlsencards.com/Ecard30.swf 80.196.101.244 20090514142518 application/x-shockwave-flash 656296 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 17775079 fa2be9eb92945223f5929cc21549f67a
http://carlsencards.com/Ecard11.swf 80.196.101.244 20090514142521 application/x-shockwave-flash 594535 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 18431479 4cb084dff758e5f997e2c3be46b69dca
http://carlsencards.com/Ecard2.swf 80.196.101.244 20090514142523 application/x-shockwave-flash 591272 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 19026118 8a7a93b848f69c1ecbfa69f0a49f1494
http://carlsencards.com/Ecard32.swf 80.196.101.244 20090514142525 application/x-shockwave-flash 644495 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 19617493 30b0c4c4f5a72383ac0d1b9eefd5d254
http://carlsencards.com/Ecard26.swf 80.196.101.244 20090514142528 application/x-shockwave-flash 785711 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 20262092 724c48ebd1def340b5a2e18a91f315f1
http://www.carlsencards.com/Scripts/expressInstall.swf 80.196.101.244 20090514142532 application/x-shockwave-flash 1085 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 21047907 c7864148a042c631dcd8de77a18a327b
http://www.carlsencards.com/Download.Cancelled 80.196.101.244 20090514142533 text/html 413 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 21049113 277a9aa791975dc1fc3aa67d220e0e3e
http://www.carlsencards.com/ShockwaveFlash.ShockwaveFlash 80.196.101.244 20090514142534 text/html 424 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 21049618 174391c76adbf809ffbe5c58b1d6b4ea
http://www.carlsencards.com/6.0.65 80.196.101.244 20090514142535 text/html 401 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 21050145 82dd6001ebc1516b4b66d5b0323b7ffb
http://www.carlsencards.com/EcardFooter.swf 80.196.101.244 20090514142536 application/x-shockwave-flash 153968 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 21050626 ac486cbe1c2234c14b2176eb73b37494
http://www.carlsencards.com/Ecard6.swf 80.196.101.244 20090514142540 application/x-shockwave-flash 828326 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 21204706 e54322c9c084a8702fea1cc1c9a7077b
http://www.carlsencards.com/_root.sound 80.196.101.244 20090514142542 text/html 406 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 22033139 7b391bb4d073c80d804dc2e6e365e852
http://www.carlsencards.com/Ecard21.swf 80.196.101.244 20090514142545 application/x-shockwave-flash 562097 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 22033630 7882f4e9fef27f0479cd5d3f62da2aba
http://www.carlsencards.com/Ecard29.swf 80.196.101.244 20090514142547 application/x-shockwave-flash 699800 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 22595835 f2f3e64c3289c8fca08e1c217de229ee
http://www.carlsencards.com/Ecard44.swf 80.196.101.244 20090514142550 application/x-shockwave-flash 781201 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 23295743 22f330c3febe560bd5f45a81634f3aa5
http://www.carlsencards.com/_root.card_ 80.196.101.244 20090514142552 text/html 406 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 24077052 6c507260e491ce6910081f0154d56455
http://www.carlsencards.com/Ecard41.swf 80.196.101.244 20090514142554 application/x-shockwave-flash 694985 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 24077543 8baedfa2669f785e001752e1b34838ac
http://www.carlsencards.com/Ecard11.swf 80.196.101.244 20090514142557 application/x-shockwave-flash 594535 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 24772636 88a8009065f761b6191a113d88688fcc
http://www.carlsencards.com/Ecard30.swf 80.196.101.244 20090514142559 application/x-shockwave-flash 656296 1-1-20090514142103-00002-kb-test-har-002.kb.dk.arc 25367279 5ccc13cb400401d9442b1b1621935f3c

metadata://netarkivet.dk/crawl/index/cdx?majorversion=1&minorversion=0&harvestid=1&jobid=1&timestamp=20090514142103&serialno=00001 130.226.228.8 20090514142609 application/x-cdx 38727
http://pagead2.googlesyndication.com/ 74.125.79.167 20090514142103 text/html 443 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 1473 9d3faf163218bc6de7a7ad9c05e580d6
http://ad.yieldmanager.com/ 77.238.174.11 20090514142104 text/plain 280 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 1998 d02aa85ac3fc536a04c52c65baab9c94
http://partner.googleadservices.com/ 74.125.79.165 20090514142104 text/html 6685 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2351 a14ae50c4409b8baf6cdb0a486aa1a33
http://carlsencards.com/images/topStore_over.gif 80.196.101.244 20090514142104 image/gif 2098 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 9118 eb501d707e4dbd2d85fe65ababcd76fc
dns:www.adobe.com 130.226.220.16 20090514142105 text/dns 56 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 11311 d4282d3edf122714fede69262238b35c
http://carlsencards.com/images/top3.gif 80.196.101.244 20090514142106 image/gif 611 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 11428 c1bbcaa3d08f273bf774ccd466f045c6
http://carlsencards.com/images/top7.gif 80.196.101.244 20090514142107 image/gif 598 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 12124 e440dd6ce306cc803b8b747e2cac6707
http://carlsencards.com/images/top4About_up.gif 80.196.101.244 20090514142108 image/gif 1633 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 12807 d77b1a709594bb3035759bfef3fdbf29
http://carlsencards.com/outgoing/cafepress 80.196.101.244 20090514142109 text/html 413 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 14534 a225be059e240c5620776ed79467a1c0
http://carlsencards.com/images/top6Select_up.gif 80.196.101.244 20090514142110 image/gif 1961 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 15035 aeb45e091d3caa08198f54db52fab5af
http://carlsencards.com/images/top5Contact_up.gif 80.196.101.244 20090514142111 image/gif 1756 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 17091 27b3ab0f2503f7ae0b2b46084ca3b8f6
http://carlsencards.com/images/aboutCat.swf 80.196.101.244 20090514142112 application/x-shockwave-flash 32671 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 18943 523c006fc6f1bd1cb00651c9b36e21be
http://carlsencards.com/images/free-ecard-thumb17.jpg 80.196.101.244 20090514142113 image/jpeg 38141 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 51725 83692e0de07f6474040268eece54dac4
http://s9.addthis.com/button1-bm.gif 92.123.220.20 20090514142114 image/gif 877 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 89968 0e3bf1b12c1353df3f4699403ac7a648
http://carlsencards.com/images/seatFillers/twoCats.swf 80.196.101.244 20090514142114 application/x-shockwave-flash 12375 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 90926 ac53e189955db179a8c9e00e00b61a7e
http://carlsencards.com/images/free-ecard-thumb20.jpg 80.196.101.244 20090514142115 image/jpeg 40593 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 103423 f54b2330f4a4e2642eaebeeaaca1213e
http://carlsencards.com/images/free-ecard-thumb47.jpg 80.196.101.244 20090514142116 image/jpeg 24638 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 144118 39ae185883785b1b2c4c37c3ea2c814b
http://carlsencards.com/images/free-ecard-thumb02.jpg 80.196.101.244 20090514142117 image/jpeg 25771 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 168858 73c6993fc71a3af91b493f866cddcae5
http://carlsencards.com/images/campBoxTile.jpg 80.196.101.244 20090514142118 image/jpeg 710 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 194731 876f02ea2de991e1de443ae4a275b417
http://carlsencards.com/images/free-ecard-thumb40.jpg 80.196.101.244 20090514142120 image/jpeg 33469 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 195534 81fb61d3c894ddde6158dffceca9e260
http://carlsencards.com/images/free-ecard-thumb44.jpg 80.196.101.244 20090514142121 image/jpeg 39910 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 229105 8586505481ea52b9be0788b1da1944c5
http://carlsencards.com/images/free-ecard-thumb13.jpg 80.196.101.244 20090514142122 image/jpeg 24080 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 269117 0a26fdc642efb3567215bed34d841ab3
http://carlsencards.com/images/free-ecard-thumb15.jpg 80.196.101.244 20090514142123 image/jpeg 41624 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 293299 4a7b9a5c3118e5b50f6e3d4441faf676
http://carlsencards.com/images/extLinks/banner2.gif 80.196.101.244 20090514142124 image/gif 3347 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 335025 c7c2d93a0be0f72594f0f96b930e6b6f
http://carlsencards.com/images/free-ecard-thumb09.jpg 80.196.101.244 20090514142125 image/jpeg 26739 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 338470 b90525a26e86b0bee9e1b5379db841ee
http://carlsencards.com/images/free-ecard-thumb11.jpg 80.196.101.244 20090514142126 image/jpeg 30149 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 365311 778edf5e4ec8d536e0d5fddfd6c4f6e1
http://carlsencards.com/images/free-ecard-thumb23.jpg 80.196.101.244 20090514142128 image/jpeg 31839 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 395562 564d1278590c4c60033d5566e3488a0a
http://carlsencards.com/images/free-ecard-thumb58.jpg 80.196.101.244 20090514142129 image/jpeg 23710 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 427503 b2b79a5ed95bcf915b1453a32e47a897
http://carlsencards.com/images/free-ecard-thumb41.jpg 80.196.101.244 20090514142130 image/jpeg 23607 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 451315 21c94baf427189830a56db61a2bce08e
http://carlsencards.com/images/headerSorry.gif 80.196.101.244 20090514142131 image/gif 1021 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 475024 723b6ae22673bc971797db85c4e391d1
http://carlsencards.com/images/free-ecard-thumb32.jpg 80.196.101.244 20090514142132 image/jpeg 34258 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 476138 f5302568fd841ad6a8116855c4bbc3d9
http://carlsencards.com/images/extLinks/allfreethings_88.gif 80.196.101.244 20090514142133 image/gif 3248 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 510498 d0f553dd36f1eb6561a87edb0fbb995e
http://carlsencards.com/images/free-ecard-thumb19.jpg 80.196.101.244 20090514142134 image/jpeg 20364 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 513853 9da2f4fac6c913b6163285439123250e
http://carlsencards.com/images/headerSpreadtheword.gif 80.196.101.244 20090514142135 image/gif 1381 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 534319 eb66823dc80dab276a3f6c9bff16b21d
http://carlsencards.com/images/free-ecard-thumb51.jpg 80.196.101.244 20090514142136 image/jpeg 30422 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 535801 ed7ae6a543089cbefe2c7f09a803b355
http://carlsencards.com/images/seatFillers/frontpageCat.swf 80.196.101.244 20090514142137 application/x-shockwave-flash 16555 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 566325 49cbe7693343a8fb6cba5ffbc7b6eed5
http://carlsencards.com/images/free-ecard-thumb52.jpg 80.196.101.244 20090514142138 image/jpeg 23629 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 583007 13b84959b0373a9e552a97c4aede4c1e
http://carlsencards.com/images/free-ecard-thumb00.jpg 80.196.101.244 20090514142139 image/jpeg 21397 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 606738 b33abd910219ed0ef0d588192778420a
http://carlsencards.com/images/free-ecard-thumb42.jpg 80.196.101.244 20090514142140 image/jpeg 25481 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 628237 c7a1392824a7c809e015614d71c2e8e1
http://carlsencards.com/images/free-ecard-thumb25.jpg 80.196.101.244 20090514142141 image/jpeg 28695 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 653820 e967b318e58e2c4318b6319342cb279c
http://carlsencards.com/images/free-ecard-thumb59.jpg 80.196.101.244 20090514142142 image/jpeg 30211 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 682617 ff58f075a5093c5e910595dd1c0e972c
http://carlsencards.com/images/free-ecard-thumb57.jpg 80.196.101.244 20090514142143 image/jpeg 34457 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 712930 293cb3f8a1ca9bf0f696a2e094a95827
http://carlsencards.com/images/free-ecard-thumb43.jpg 80.196.101.244 20090514142144 image/jpeg 32274 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 747489 9abf582e20c33ac43296d7c2af81402b
http://carlsencards.com/images/extLinks/omg_button.gif 80.196.101.244 20090514142146 image/gif 7184 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 779865 f9ac9be9ffb633b65e6a430d45134724
http://carlsencards.com/Ecard58.swf 80.196.101.244 20090514142147 application/x-shockwave-flash 1107559 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 787150 6881200ecbaccbadf0fcb66c6adde47f
http://carlsencards.com/contact.htm 80.196.101.244 20090514142150 text/html 13360 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 1894814 62fa6d6df353677de4cb6f485ffdd97f
http://carlsencards.com/disclaimer.htm 80.196.101.244 20090514142151 text/html 12896 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 1908257 212f5f018520f7f769f220c7aa1f5475
dns:www.carlsencards.com 130.226.220.16 20090514142152 text/dns 62 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 1921239 bb2eca0b736f010e5621a1608ad98daa
http://www.carlsencards.com/images/free-ecard-thumb23.jpg 80.196.101.244 20090514142153 image/jpeg 31839 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 1921369 bace0d2f6a73b2f1097f2dd5b4fcfb1d
http://www.carlsencards.com/images/free-ecard-thumb36.jpg 80.196.101.244 20090514142155 image/jpeg 30350 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 1953314 bcce7b2aba015432cb1527c07684bc4b
http://www.carlsencards.com/images/free-ecard-thumb18.jpg 80.196.101.244 20090514142156 image/jpeg 23246 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 1983770 470825191aa8094082d54eddd1b916ba
http://www.carlsencards.com/images/headerNoSpecOcc.gif 80.196.101.244 20090514142157 image/gif 1524 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2007122 68810657b449dba64e5fe2af2ad245d7
http://www.carlsencards.com/images/free-ecard-thumb56.jpg 80.196.101.244 20090514142158 image/jpeg 39910 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2008747 156eb10c51e76daa84b11b00e5c101d1
http://www.carlsencards.com/images/top4About_up.gif 80.196.101.244 20090514142159 image/gif 1633 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2048763 9a5861b38dc8847b9f15f8720e426afe
http://www.carlsencards.com/images/top6Select_over.gif 80.196.101.244 20090514142200 image/gif 1977 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2050494 5bab75795e03efb6f5f09b9b9abdc051
http://www.carlsencards.com/images/top4About_over.gif 80.196.101.244 20090514142201 image/gif 1641 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2052572 66b621f7464e7ef1c99609846931eacf
http://www.carlsencards.com/images/extLinks/ofree_mini_3.gif 80.196.101.244 20090514142202 image/gif 1496 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2054313 5afcc9147b94176dae3c8d9e6f5cbe43
http://www.carlsencards.com/images/extLinks/allfreethings_88.gif 80.196.101.244 20090514142203 image/gif 3248 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2055916 e6a1e69c9b8df14232d3944558b4ea9c
http://www.carlsencards.com/images/free-ecard-thumb28.jpg 80.196.101.244 20090514142204 image/jpeg 34270 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2059275 145b5d8a9b4ca48132c08b24b96ab544
http://www.carlsencards.com/images/free-ecard-thumb22.jpg 80.196.101.244 20090514142205 image/jpeg 24918 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2093651 584f521354aa5feae6af902efd37b3c7
http://www.carlsencards.com/images/free-ecard-thumb50.jpg 80.196.101.244 20090514142206 image/jpeg 41779 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2118675 f8bb433a3eb8307765c675581d96e694
http://www.carlsencards.com/images/free-ecard-thumb37.jpg 80.196.101.244 20090514142207 image/jpeg 20797 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2160560 6c73440f888510c2aed84c761f89c28f
http://www.carlsencards.com/images/free-ecard-thumb52.jpg 80.196.101.244 20090514142208 image/jpeg 23629 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2181463 14604df03a27b0deef7beb1fd37bf848
http://www.carlsencards.com/images/top2Logo.gif 80.196.101.244 20090514142209 image/gif 9629 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2205198 72e42a8df2060bd03ec1f32b7a728d11
http://www.carlsencards.com/images/top1.gif 80.196.101.244 20090514142210 image/gif 745 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2214921 8b607c4c11e6df25d656197a7ebd8605
http://www.carlsencards.com/images/extLinks/banner2.gif 80.196.101.244 20090514142211 image/gif 3347 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2215755 37ecfec4e2aa598e02043dc6cf6ad819
http://www.carlsencards.com/images/free-ecard-thumb33.jpg 80.196.101.244 20090514142212 image/jpeg 23958 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2219204 6f6dc1b4474fb0aad478e49b31ae883f
http://www.carlsencards.com/images/free-ecard-thumb04.jpg 80.196.101.244 20090514142213 image/jpeg 18646 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2243268 b2a97c378462212d5d82a736a1b7fa01
http://www.carlsencards.com/images/headerThankYou.gif 80.196.101.244 20090514142214 image/gif 1151 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2262020 a08d21149ba61e14ac2550e5ed67419b
http://www.carlsencards.com/images/free-ecard-thumb58.jpg 80.196.101.244 20090514142215 image/jpeg 23710 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2263271 f5dd632cd66478d57ac8d4e00e6a9712
http://www.carlsencards.com/images/seatFillers/frontpageCat.swf 80.196.101.244 20090514142216 application/x-shockwave-flash 16555 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2287087 4210217d3708db76209e111eb952d86c
http://www.carlsencards.com/images/top5Contact_over.gif 80.196.101.244 20090514142217 image/gif 1767 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2303773 6e796128b73e468e70793903ef81bf9a
http://www.carlsencards.com/images/newCardTopFrontpg_fortTellR.jpg 80.196.101.244 20090514142218 image/jpeg 22996 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2305642 230c923e1337df7a24235b6ece5a004f
http://www.carlsencards.com/images/free-ecard-thumb12.jpg 80.196.101.244 20090514142219 image/jpeg 23284 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2328753 d267a49d8da9463cfa1d7db8ab5bcdea
http://www.carlsencards.com/images/free-ecard-thumb40.jpg 80.196.101.244 20090514142220 image/jpeg 33469 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2352143 35f6871164ef85e92e4beee545357bae
http://www.carlsencards.com/images/free-ecard-thumb03.jpg 80.196.101.244 20090514142222 image/jpeg 16363 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2385718 a3fe9a035576968780d483ab58224ae4
http://www.carlsencards.com/images/free-ecard-thumb15.jpg 80.196.101.244 20090514142223 image/jpeg 41624 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2402187 513565da5163eb91008c57d708d4de86
http://www.carlsencards.com/images/free-ecard-thumb09.jpg 80.196.101.244 20090514142225 image/jpeg 26739 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2443917 f0b0ea1f0cfd54be2f56f10a05b362b4
http://www.carlsencards.com/images/free-ecard-thumb10.jpg 80.196.101.244 20090514142226 image/jpeg 37587 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2470762 3cea31b662197266c69c007f09cf7264
http://www.carlsencards.com/images/seatFillers/twoCats.swf 80.196.101.244 20090514142227 application/x-shockwave-flash 12375 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2508455 3bf096c51fdf4b05072a0181b8bcb487
http://www.carlsencards.com/images/seatFillers/xmasPenguins.swf 80.196.101.244 20090514142228 application/x-shockwave-flash 27342 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2520956 53cf93116619a6b5fe2a6d970f27ba4f
http://www.carlsencards.com/images/free-ecard-thumb38.jpg 80.196.101.244 20090514142230 image/jpeg 31304 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2548429 257c5bd906409bf69d41c18998eb2fac
http://www.carlsencards.com/images/free-ecard-thumb32.jpg 80.196.101.244 20090514142231 image/jpeg 34258 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2579839 d686d6bce37b2355622a8fd31e78b4cb
http://www.carlsencards.com/images/top8_micardsa.gif 80.196.101.244 20090514142232 image/gif 2348 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2614203 1cb0cb39367de555b13467feb49aef51
http://www.carlsencards.com/images/top5Contact_up.gif 80.196.101.244 20090514142233 image/gif 1756 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2616650 d546495aa4968e71801c5cbedcd9dfec
http://www.carlsencards.com/images/newCardTemplateBtm_fortTell.jpg 80.196.101.244 20090514142234 image/jpeg 53477 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2618506 6d176e5c38b133a9c26a0fc07723ca81
http://carlsencards.com/application/x-shockwave-flash 80.196.101.244 20090514142235 text/html 424 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2672098 0a8c8ab523ff233099ee7c7d3be14f68
http://carlsencards.com/ShockwaveFlash.ShockwaveFlash 80.196.101.244 20090514142236 text/html 424 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2672621 4a83852f7804deb1c92823c1f184db4c
http://carlsencards.com/0.005 80.196.101.244 20090514142237 text/html 400 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2673144 4add7a6d4f142f068e67225582e91291
http://carlsencards.com/1.9 80.196.101.244 20090514142238 text/html 398 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2673619 280e7bd874ad2fb3a6167b441912f21e
http://carlsencards.com/pagead/test_domain.js 80.196.101.244 20090514142239 text/html 416 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2674090 e82576dd25f336e16e636e38bab3a3a1
http://www.carlsencards.com/dBText/ 80.196.101.244 20090514142240 text/html 406 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2674597 c2313fb28c5e68cb5f7bc4dba450ccf0
http://carlsencards.com/free-ecard-49-no-special-occasion-sleep-e-card.htm 80.196.101.244 20090514142242 text/html 13143 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2675084 b87397ef974cc274b722a6f3dd72fc85
http://carlsencards.com/cat-free-ecards-happy-birthday.htm 80.196.101.244 20090514142243 text/html 21337 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2688349 f0c58db40ab0fabf605d5a8870a72e74
http://carlsencards.com/free-ecard-22-i-hate-you-cat-latin-te-odio.htm 80.196.101.244 20090514142245 text/html 13086 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2709792 f0cb5b53298bbbff88d1cb50e4e59643
http://carlsencards.com/free-ecard-05-happy-birthday-postcard-coffee.htm 80.196.101.244 20090514142246 text/html 13054 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2722996 9f0deb1501a5ec01e797fa4c5ca128ac
http://carlsencards.com/Ecard37.swf 80.196.101.244 20090514142247 application/x-shockwave-flash 669049 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 2736170 d9fede252da10ef7981b36c19be3dd25
http://carlsencards.com/free-ecard-12-im-sorry-little-bear-big-words.htm 80.196.101.244 20090514142249 text/html 13121 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 3405323 7e593d31e99e25ceffc571de657baf40
http://carlsencards.com/Ecard27.swf 80.196.101.244 20090514142251 application/x-shockwave-flash 658443 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 3418564 5fa3096e25ed031b7e4675c2444844d9
http://carlsencards.com/free-ecard-13-spread-the-word-make-a-difference-cat-mouse.htm 80.196.101.244 20090514142253 text/html 13064 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 4077111 2bc0039f5d731da46a4507e3bbe580af
http://carlsencards.com/free-ecard-07-happy-birthday-devil-angel-directors-cut.htm 80.196.101.244 20090514142254 text/html 13136 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 4090308 dca8b9a18bee9ac07a12ed0998497397
http://carlsencards.com/Ecard3.swf 80.196.101.244 20090514142257 application/x-shockwave-flash 574904 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 4103574 d3034626f196e8cf212edf4ca3bfe9c3
http://carlsencards.com/free-ecard-55-happy-anniversary-lemurs.htm 80.196.101.244 20090514142259 text/html 13127 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 4678581 09fc5f235061f2af8c4da0f266c4cbae
http://carlsencards.com/free-ecard-28-merry-christmas-easter-bunny-santa.htm 80.196.101.244 20090514142301 text/html 13140 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 4691822 9b3a95069d3f281c0a5e019c4392b68e
http://carlsencards.com/Ecard57.swf 80.196.101.244 20090514142306 application/x-shockwave-flash 438215 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 4705086 bceab817affdacf1cde7dc1a9f1420ce
http://carlsencards.com/Ecard0.swf 80.196.101.244 20090514142307 application/x-shockwave-flash 902226 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 5143405 bc7ee47ba67fae5dc4026fd29645cfea
http://carlsencards.com/free-ecard-08-i-hate-you-cat-kiss-my-ass.htm 80.196.101.244 20090514142310 text/html 13140 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 6045734 8127282dca1c92220d0c53e3dbf35144
http://carlsencards.com/free-ecard-54-happy-anniversary-mice.htm 80.196.101.244 20090514142311 text/html 13036 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 6058990 efd3a7de02620fa93b3d5251cb299da7
http://carlsencards.com/free-ecard-23-no-special-occasion-cosmopolitan-recipe.htm 80.196.101.244 20090514142313 text/html 13180 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 6072138 c9657f5b6b878a2411308d27adaf38ef
http://carlsencards.com/Ecard21.swf 80.196.101.244 20090514142315 application/x-shockwave-flash 562097 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 6085447 7738e3b2d78f6422ead6d410b7bdea9d
http://carlsencards.com/Ecard45.swf 80.196.101.244 20090514142317 application/x-shockwave-flash 630414 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 6647648 518ec1ed41072bd52e8e7e4305647d69
http://carlsencards.com/free-ecard-44-fourth-of-july-mice-fireworks.htm 80.196.101.244 20090514142319 text/html 13362 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 7278166 34e2187d9ae5e439dfbd809d753a2d46
http://carlsencards.com/free-ecard-41-happy-mothers-day-polar-bears.htm 80.196.101.244 20090514142321 text/html 13221 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 7291647 f083046c67ffec1be8bd713f87898af7
http://carlsencards.com/Ecard16.swf 80.196.101.244 20090514142322 application/x-shockwave-flash 639790 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 7304987 81db274736caf0b7bfae57fab0e19ecd
http://carlsencards.com/Ecard17.swf 80.196.101.244 20090514142324 application/x-shockwave-flash 539455 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 7944881 ec4d72537b35e51985dd0035a929ea0d
http://carlsencards.com/Ecard31.swf 80.196.101.244 20090514142326 application/x-shockwave-flash 799170 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 8484440 28e70c3108f39b1fab847ea22b61070f
http://carlsencards.com/free-ecard-52-new-baby-polar-bears.htm 80.196.101.244 20090514142328 text/html 13105 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 9283714 9c9693dc871ac79670cb891bd475a0b4
http://carlsencards.com/images/seatFillers/littleMousie.swf 80.196.101.244 20090514142330 application/x-shockwave-flash 13673 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 9296929 592293f00a21c27f4b0799f7e9761b42
http://carlsencards.com/free-ecard-42-happy-mothers-day-llamas.htm 80.196.101.244 20090514142333 text/html 13233 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 9310729 02f08d63b2699b3f989e84a1f61ad540
http://carlsencards.com/free-ecard-01-happy-birthday-bored-cats.htm 80.196.101.244 20090514142335 text/html 13053 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 9324076 1bb9c595a5bc9c8fab47665e79b88d56
http://carlsencards.com/Ecard50.swf 80.196.101.244 20090514142336 application/x-shockwave-flash 767445 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 9337244 dc728c3d707c5ffb9063146da14819a2
http://carlsencards.com/free-ecard-39-happy-easter-dancing-eggs.htm 80.196.101.244 20090514142339 text/html 13100 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 10104793 66e9c8252aa20bc1a8c7e58b3290c5c4
http://carlsencards.com/free-ecard-46-thank-you-lemur-choir.htm 80.196.101.244 20090514142341 text/html 13315 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 10118008 e7502fcc8706ad28c3fdd34b90b11f5c
http://carlsencards.com/free-ecard-53-new-baby-llamas.htm 80.196.101.244 20090514142342 text/html 13102 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 10131434 20b6d1c4f6e8d3896c1163c8d970a459
http://carlsencards.com/cat-free-ecards-sorry.htm 80.196.101.244 20090514142343 text/html 15870 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 10144641 cf8f1ceaa31e76b3d06f5baff14d5724
http://carlsencards.com/cat-free-ecards-new-baby.htm 80.196.101.244 20090514142345 text/html 16064 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 10160608 d1cab7907233def949c068a470138409
http://carlsencards.com/cat-free-ecards-i-hate-you.htm 80.196.101.244 20090514142346 text/html 15181 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 10176772 f8dce626aa660cf7720f6c050f272bb2
http://www.carlsencards.com/free-ecard-31-no-special-occasion-cat-mouse-flash-game.htm 80.196.101.244 20090514142347 text/html 13401 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 10192055 654c41d61d40a234b5ab59986d2ac29a
http://www.carlsencards.com/Scripts/swfobject_modified.js 80.196.101.244 20090514142349 application/x-javascript 22006 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 10205590 7a6e186f97a41d1f625e6d1d01fb40a7
http://www.carlsencards.com/images/seatFillers/hollaMouse.swf 80.196.101.244 20090514142351 application/x-shockwave-flash 13821 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 10227716 6ba93812a3424b4602cf195ca6509668
http://www.carlsencards.com/free-ecard-13-spread-the-word-make-a-difference-cat-mouse.htm 80.196.101.244 20090514142352 text/html 13064 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 10241666 9e83071e961ccc16bb81a9d75af82e04
http://www.carlsencards.com/images/contactCat.swf 80.196.101.244 20090514142354 application/x-shockwave-flash 13335 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 10254867 393ec3e3db00e6ba04aafccbf53325f3
http://www.carlsencards.com/free-ecard-08-i-hate-you-cat-kiss-my-ass.htm 80.196.101.244 20090514142355 text/html 13140 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 10268319 e8b81453593829f77cc4750597ccbe63
http://www.carlsencards.com/Ecard12.swf 80.196.101.244 20090514142356 application/x-shockwave-flash 564674 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 10281579 d4f377793b7494520336955e39facd07
http://www.carlsencards.com/free-ecard-01-happy-birthday-bored-cats.htm 80.196.101.244 20090514142358 text/html 13053 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 10846361 705678a49d24463d783b8d50318b958a
http://www.carlsencards.com/Ecard39.swf 80.196.101.244 20090514142400 application/x-shockwave-flash 466776 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 10859533 815893a56b142a51c3ed25233484f988
http://www.carlsencards.com/free-ecard-57-no-special-occasion-FAPSS.htm 80.196.101.244 20090514142401 text/html 13224 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 11326417 86f04586dcc9863f38fb60683ef0c6be
http://www.carlsencards.com/Ecard26.swf 80.196.101.244 20090514142403 application/x-shockwave-flash 785711 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 11339760 b9edc9fc80bdd54a3aed139dc7840044
http://www.carlsencards.com/cat-free-ecards-no-special-occasion.htm 80.196.101.244 20090514142405 text/html 20995 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 12125579 4ae5015aa901806b17348bd2a94f0c45
http://www.carlsencards.com/free-ecard-25-no-special-occasion-text-messaging-lemurs.htm 80.196.101.244 20090514142408 text/html 13131 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 12146689 fbb9167eb8b6ec82fb2d062685a81421
http://www.carlsencards.com/free-ecard-04-im-sorry-dog-interactive.htm 80.196.101.244 20090514142409 text/html 13048 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 12159955 719144df3a6f8adeb9beb4576b6db975
http://www.carlsencards.com/free-ecard-56-happy-birthday-fireworks-mice.htm 80.196.101.244 20090514142411 text/html 13070 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 12173121 6c42b4c6c32ad66b542722ca4ad08268
http://www.carlsencards.com/free-ecard-24-no-special-occasion-hi-mouse-pixar.htm 80.196.101.244 20090514142413 text/html 13160 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 12186314 71963432de431bed179175871f7fc54e
http://www.carlsencards.com/free-ecard-47-no-special-occasion-cheerleader-mouse.htm 80.196.101.244 20090514142414 text/html 13203 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 12199602 fd73363beca68c12a143981a1b0d2a69
http://www.carlsencards.com/free-ecard-54-happy-anniversary-mice.htm 80.196.101.244 20090514142416 text/html 13036 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 12212936 b740f8e3d80ed1a0fe4362a2221cbaf4
http://www.carlsencards.com/Ecard16.swf 80.196.101.244 20090514142418 application/x-shockwave-flash 639790 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 12226088 154973f52dd8b3321dcc78852e6a62d4
http://www.carlsencards.com/images/extLinks/aford_promobanner_31x88.gif 80.196.101.244 20090514142420 image/gif 1319 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 12865986 84cf2716f4bd89283fab78e088208aa9
http://www.carlsencards.com/Ecard9.swf 80.196.101.244 20090514142421 application/x-shockwave-flash 622347 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 12867423 ec6efe56027b2e3acbc7aa392b63a10e
http://www.carlsencards.com/images/aboutCat.swf 80.196.101.244 20090514142423 application/x-shockwave-flash 32671 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 13489877 6c942320e92de173f2cdc52bcf2c564f
http://www.carlsencards.com/free-ecard-40-no-special-occasion-memory-game-4cards.htm 80.196.101.244 20090514142424 text/html 13370 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 13522663 4fbcc789fc6ac0b4f6e32d31b7eaaa9f
http://www.carlsencards.com/free-ecard-44-fourth-of-july-mice-fireworks.htm 80.196.101.244 20090514142426 text/html 13362 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 13536165 952df4ce10d820b68967fb4db75749cc
http://www.carlsencards.com/Ecard42.swf 80.196.101.244 20090514142427 application/x-shockwave-flash 514252 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 13549650 ed7664d16df6beaaff943caa83b38887
http://www.carlsencards.com/free-ecard-14-get-well-cat-flower-medicine.htm 80.196.101.244 20090514142429 text/html 13116 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 14064010 300d485b16661692cbb48bfb0c518918
http://www.carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm 80.196.101.244 20090514142431 text/html 13455 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 14077248 30e8deb49a1db853c6b01c6e54357304
http://www.carlsencards.com/free-ecard-49-no-special-occasion-sleep-e-card.htm 80.196.101.244 20090514142434 text/html 13143 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 14090835 d7c6506bccd291b76a83f944203000f5
http://www.carlsencards.com/cat-free-ecards-new-baby.htm 80.196.101.244 20090514142435 text/html 16064 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 14104104 4d6c7de962f5907c4f2de7002aa7047d
http://www.carlsencards.com/images/seatFillers/penguinJoke.swf 80.196.101.244 20090514142437 application/x-shockwave-flash 12606 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 14120272 23da2668e0194d59ccc92fb62ec6230a
http://www.carlsencards.com/free-ecard-37-congratulations-bored-funny-cats.htm 80.196.101.244 20090514142438 text/html 13120 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 14133008 1e24a2f45c2a6f3b938f1b1c631409df
http://www.carlsencards.com/free-ecard-10-congratulations-cat-ebay-elvis-cake.htm 80.196.101.244 20090514142441 text/html 13099 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 14146254 a07df9fb2265963cb3dd0cfe4ffb0113
http://www.carlsencards.com/free-ecard-32-happy-new-year-raccoon-party-fireworks.htm 80.196.101.244 20090514142442 text/html 13206 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 14159482 3106daabcdfce5c53e371713f56534d7
http://www.carlsencards.com/free-ecard-28-merry-christmas-easter-bunny-santa.htm 80.196.101.244 20090514142444 text/html 13140 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 14172820 4cc46a53507e2323a2d091dc5c0437c7
http://www.carlsencards.com/Ecard45.swf 80.196.101.244 20090514142445 application/x-shockwave-flash 630414 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 14186088 e319147a57d60aa3bc21125e6fa27f15
http://www.carlsencards.com/free-ecard-18-i-love-you-cat-pictures-true-love.htm 80.196.101.244 20090514142448 text/html 13038 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 14816610 bd9e01a9f6f56451924388d3e969dcbf
http://www.carlsencards.com/images/seatFillers/littleMousie.swf 80.196.101.244 20090514142450 application/x-shockwave-flash 13673 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 14829775 799138720173f0014b4f9e05c60732f3
http://www.carlsencards.com/Ecard20.swf 80.196.101.244 20090514142451 application/x-shockwave-flash 755555 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 14843579 9c2526df88fd564bea648af25de0935e
http://www.carlsencards.com/free-ecard-22-i-hate-you-cat-latin-te-odio.htm 80.196.101.244 20090514142453 text/html 13086 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 15599242 d42d8063a952d9dfcb2fcadab410ed5c
http://www.carlsencards.com/disclaimer.htm 80.196.101.244 20090514142456 text/html 12896 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 15612450 d246f762e6f8b425164d27f7dbb47145
http://www.carlsencards.com/application/x-shockwave-flash 80.196.101.244 20090514142458 text/html 424 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 15625436 eca064f293747d0af7f085befe6e8c07
http://carlsencards.com/Ecard4.swf 80.196.101.244 20090514142459 application/x-shockwave-flash 660082 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 15625963 d1487720d3b6c571c806484569af1d98
http://carlsencards.com/Ecard35.swf 80.196.101.244 20090514142501 application/x-shockwave-flash 648064 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 16286148 e7adbb015d7c7ee39316322358f9a520
http://carlsencards.com/Ecard34.swf 80.196.101.244 20090514142504 application/x-shockwave-flash 747454 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 16934316 7cde961684c65e584bc4d5b0ac2d45ac
http://carlsencards.com/Ecard20.swf 80.196.101.244 20090514142507 application/x-shockwave-flash 755555 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 17681874 c5f91dfc48045d1660b5b611ca827fc4
http://carlsencards.com/Ecard59.swf 80.196.101.244 20090514142510 application/x-shockwave-flash 867938 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 18437533 97f0cef9ebec2aa183b0787ca0df7e8e
http://carlsencards.com/Download.Cancelled 80.196.101.244 20090514142512 text/html 413 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 19305575 a4bf0bdcb6cdfd47f76ff1afc50dc5ef
http://carlsencards.com/8.0.35.0 80.196.101.244 20090514142513 text/html 403 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 19306076 500d4997f698aaf14a974c554cac6cbc
http://carlsencards.com/8.0.0 80.196.101.244 20090514142514 text/html 400 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 19306557 14ce2c512f1de9611d8414cf14048fdd
http://carlsencards.com/Ecard44.swf 80.196.101.244 20090514142516 application/x-shockwave-flash 781201 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 19307032 dec360453e402071d9a22eb72ec7e1fb
http://carlsencards.com/Ecard41.swf 80.196.101.244 20090514142519 application/x-shockwave-flash 694985 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 20088337 51113abcc73d7208399a4bd77ddf6565
http://carlsencards.com/Ecard52.swf 80.196.101.244 20090514142521 application/x-shockwave-flash 826177 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 20783426 cbfa9b4bfe487cd6fb59016ef37dfdf5
http://carlsencards.com/Ecard9.swf 80.196.101.244 20090514142524 application/x-shockwave-flash 622347 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 21609707 9af54c2dae54d351d307fac2a5108aaa
http://carlsencards.com/Ecard46.swf 80.196.101.244 20090514142526 application/x-shockwave-flash 789590 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 22232157 8745c8ba9de5ae1168fcb8bf9f4744dc
http://www.carlsencards.com/Ecard31.swf 80.196.101.244 20090514142530 application/x-shockwave-flash 799170 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 23021851 f4288c2329a5fa7edf5ba03d186c10bf
http://www.carlsencards.com/fpdownload.macromedia.com 80.196.101.244 20090514142532 text/html 420 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 23821129 b25d30283321780f80dc6abbda33c1c1
http://www.carlsencards.com/Download.Failed 80.196.101.244 20090514142533 text/html 410 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 23821648 4e67967bca1089a4f2ef6d411e1166f3
http://www.carlsencards.com/8.0.0 80.196.101.244 20090514142534 text/html 400 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 23822147 0b87a71e0d7bee8d35b0287156fc149d
http://www.carlsencards.com/Ecard53.swf 80.196.101.244 20090514142535 application/x-shockwave-flash 518189 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 23822626 2ef2c593fdf80269b033d7e368fdf07f
http://www.carlsencards.com/Ecard58.swf 80.196.101.244 20090514142537 application/x-shockwave-flash 1107559 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 24340923 d1093da9b2db7aea802d8b5171a9ac9d
http://www.carlsencards.com/www.carlsencards.com 80.196.101.244 20090514142541 text/html 415 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 25448591 53c3a812fda72890a68efac3f553df3f
http://www.carlsencards.com/Ecard4.swf 80.196.101.244 20090514142543 application/x-shockwave-flash 660082 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 25449100 09bfc9556888b90fd81939384dc0ce18
http://www.carlsencards.com/Ecard24.swf 80.196.101.244 20090514142546 application/x-shockwave-flash 573089 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 26109289 b18026326ab076d3cdbfac6d8d0ca2e2
http://www.carlsencards.com/Ecard23.swf 80.196.101.244 20090514142548 application/x-shockwave-flash 734644 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 26682486 70bf359479289f7f4217c4a1797d0f34
http://www.carlsencards.com/Ecard27.swf 80.196.101.244 20090514142551 application/x-shockwave-flash 658443 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 27417238 2a3022a6b346fbd17d70596bd6ad9f89
http://www.carlsencards.com/Ecard55.swf 80.196.101.244 20090514142553 application/x-shockwave-flash 648169 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 28075789 07923d4edefa15c647ccc17e1f8211b7
http://www.carlsencards.com/Ecard10.swf 80.196.101.244 20090514142555 application/x-shockwave-flash 707624 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 28724066 1f19012abcea71862c5682604a6c10aa
http://www.carlsencards.com/Ecard50.swf 80.196.101.244 20090514142557 application/x-shockwave-flash 767445 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 29431798 04e8557b7e4127c61401d767e8e01716
http://www.carlsencards.com/Ecard22.swf 80.196.101.244 20090514142600 application/x-shockwave-flash 460109 1-1-20090514142103-00001-kb-test-har-002.kb.dk.arc 30199351 b5ed34f4b2e2c5e38eab02e17c3aec61

metadata://netarkivet.dk/crawl/index/cdx?majorversion=1&minorversion=0&harvestid=1&jobid=1&timestamp=20090514142101&serialno=00000 130.226.228.8 20090514142610 application/x-cdx 42522
dns:carlsencards.com 130.226.220.16 20090514142100 text/dns 58 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1473 3e206edb9e16b493ccffd1dbadefafb1
http://carlsencards.com/robots.txt 80.196.101.244 20090514142101 text/plain 315 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1595 d80da027c10f4453514c71cfdda1b2c3
http://carlsencards.com/free-ecard-38-no-special-occasion-memory-game-2cards.htm 80.196.101.244 20090514142101 text/html 13455 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1991 9812b5f42aac75edcd046277f0f0a763
dns:www.google-analytics.com 130.226.220.16 20090514142101 text/dns 247 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 15574 d2bb2becd16309f74dbae6f0b0ff0b05
dns:pagead2.googlesyndication.com 130.226.220.16 20090514142101 text/dns 191 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 15894 2ef9f1e10fe28996386355766009df0b
dns:download.macromedia.com 130.226.220.16 20090514142102 text/dns 60 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 16163 0688e00a8cae2977eb7fd92b963b45bc
http://carlsencards.com/Scripts/AC_RunActiveContent.js 80.196.101.244 20090514142102 application/x-javascript 3541 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 16294 daa1b6246de57c28807995838cf882af
http://www.google-analytics.com/robots.txt 74.125.79.101 20090514142102 text/plain 4222 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 19951 4efdc8c91ed876614769c4b794961e27
http://download.macromedia.com/robots.txt 92.123.35.191 20090514142102 text/plain 271 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 24262 2ad21c022ae89ca2a78270cf35801d55
http://pagead2.googlesyndication.com/robots.txt 74.125.79.167 20090514142102 text/plain 174 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 24620 701eb52da97ec6e5bbf9ea9bf5ea1818
http://carlsencards.com/images/bottom.gif 80.196.101.244 20090514142102 image/gif 1253 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 24887 4c98fedcb11a35885c86d32ccebe2546
http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab 92.123.35.191 20090514142102 no-type 212 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 26228 3cb89b307217fdeaaca68b9ab11a32e6
http://www.google-analytics.com/urchin.js 74.125.79.101 20090514142102 text/javascript 22873 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 26550 7ed4b7023c113fc7f41c365bec760514
http://pagead2.googlesyndication.com/pagead/show_ads.js 74.125.79.167 20090514142102 text/javascript 31554 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 49517 6b71f9435f4c1cab82bf73e8ec72a32e
http://carlsencards.com/images/line.gif 80.196.101.244 20090514142103 image/gif 522 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 81179 e999dab5e6ba32c7fdc65fc6028c589b
dns:partner.googleadservices.com 130.226.220.16 20090514142103 text/dns 203 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 81786 6bcd6e2a8969b08e768db9c8de23f0af
dns:googleads.g.doubleclick.net 130.226.220.16 20090514142103 text/dns 211 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 82066 69c960da5d5310e8d627834564bae632
dns:ad.yieldmanager.com 130.226.220.16 20090514142103 text/dns 59 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 82353 f551d20443c345b768b8e20388ce75d4
dns:www.macromedia.com 130.226.220.16 20090514142103 text/dns 61 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 82479 58532f10c3d57a69233b1be79f7f4df7
http://carlsencards.com/images/newCardTemplateBtm_fortTell.jpg 80.196.101.244 20090514142103 image/jpeg 53477 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 82606 d6d6221f6cfabb183f8e273432174bdb
http://partner.googleadservices.com/robots.txt 74.125.79.165 20090514142103 text/plain 174 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 136194 a55802786217d6c3db92a815c663c255
http://googleads.g.doubleclick.net/robots.txt 74.125.79.156 20090514142103 text/plain 174 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 136460 a55802786217d6c3db92a815c663c255
http://carlsencards.com/images/top4About_over.gif 80.196.101.244 20090514142104 image/gif 1641 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 136725 eb1aafaaa87b3a08fc4ba4215530491a
http://pagead2.googlesyndication.com/pagead/ 74.125.79.167 20090514142104 text/html 390 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 138462 95af6091690d519cecab0ec9b360933f
http://carlsencards.com/images/top5Contact_over.gif 80.196.101.244 20090514142104 image/gif 1767 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 138941 18e826edb4463f32405c2524f8e8468c
http://partner.googleadservices.com/intl/en_com/images/logo_plain.png 74.125.79.165 20090514142104 image/png 7825 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 140806 bec8e58e3cb9f06f928cc8ddf0e51a53
http://carlsencards.com/Ecard38.swf 80.196.101.244 20090514142105 application/x-shockwave-flash 854744 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 148746 20a369407c2761d67b118ea86ff4e725
http://fpdownload2.macromedia.com/robots.txt 92.123.64.18 20090514142106 text/plain 273 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1003594 2e4b61c3665317164c18b593705b0e71
http://carlsencards.com/images/mainTile.gif 80.196.101.244 20090514142107 image/gif 414 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1003956 7dde35c445d17ff560b370df90bdfc71
http://carlsencards.com/images/top6Select_over.gif 80.196.101.244 20090514142108 image/gif 1977 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1004459 21814d1fb63aeb8f99e2994482c47e71
http://carlsencards.com/images/top2Logo.gif 80.196.101.244 20090514142109 image/gif 9629 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1006533 b4bbd435e469c34fdbb3724c76d47a8e
http://carlsencards.com/images/newCardTemplateBtm_hamster.jpg 80.196.101.244 20090514142110 image/jpeg 60167 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1016252 c73c6442ccce8d4317f34612da2bab7f
http://carlsencards.com/about.htm 80.196.101.244 20090514142111 text/html 15214 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1076529 b3d38695ec4b693fd25f47e7fb906686
http://carlsencards.com/index.htm 80.196.101.244 20090514142112 text/html 116851 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1091824 c00d9352464134da53d3e570091baa07
http://s9.addthis.com/robots.txt 92.123.220.20 20090514142113 text/plain 354 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1208757 88251b2f1e82feef9211f51e3b9d68eb
http://carlsencards.com/images/free-ecard-thumb26.jpg 80.196.101.244 20090514142114 image/jpeg 27102 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1209189 7b69b8b9aa4e178b335acd53aa299f06
http://carlsencards.com/images/newCardTopFrontpgHBFlowers.jpg 80.196.101.244 20090514142115 image/jpeg 38041 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1236393 b47318983bf6376ff97e7b7d0c4dad3e
http://carlsencards.com/images/free-ecard-thumb33.jpg 80.196.101.244 20090514142116 image/jpeg 23958 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1274544 41b614b9ac0ab9058a84d64b94bb023a
http://carlsencards.com/images/headerILoveYou.gif 80.196.101.244 20090514142117 image/gif 1110 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1298604 fdf1e4a8fb57e9b88457fee01978e56f
http://carlsencards.com/images/extLinks/top20free88x53PD.gif 80.196.101.244 20090514142118 image/gif 3376 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1299810 8b0ed509bd065d08596e6125db6ba1ca
http://carlsencards.com/images/free-ecard-thumb14.jpg 80.196.101.244 20090514142119 image/jpeg 25265 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1303293 9418668381cb97af653551025bc63c2a
http://carlsencards.com/images/free-ecard-thumb21.jpg 80.196.101.244 20090514142120 image/jpeg 26240 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1328660 960d675cf14b5fa80608b5499b58840d
http://carlsencards.com/images/free-ecard-thumb30.jpg 80.196.101.244 20090514142121 image/jpeg 23317 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1355002 09848eda63d1008a419b60ad56b78ec3
http://carlsencards.com/images/free-ecard-thumb18.jpg 80.196.101.244 20090514142122 image/jpeg 23246 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1378421 19ab7c1d74a330a44cd1c0fd48853fc2
http://carlsencards.com/images/free-ecard-thumb03.jpg 80.196.101.244 20090514142123 image/jpeg 16363 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1401769 83c1bdf31115d7e3e597463b4ce6e2ed
http://carlsencards.com/images/free-ecard-thumb27.jpg 80.196.101.244 20090514142125 image/jpeg 26845 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1418234 c29ffcb0bcb797adf0c4dd5905b4b7d9
http://carlsencards.com/images/free-ecard-thumb29.jpg 80.196.101.244 20090514142126 image/jpeg 29006 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1445181 568e2894f8d53ddaac9870bb1cec6897
http://carlsencards.com/images/extLinks/small2.gif 80.196.101.244 20090514142127 image/gif 2835 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1474289 d3aeef60860391fc41518780ed6fb17e
http://carlsencards.com/images/free-ecard-thumb22.jpg 80.196.101.244 20090514142128 image/jpeg 24918 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1477221 cfb668fdfce25462a339deb845f7736b
http://carlsencards.com/images/headerGetwellsoon.gif 80.196.101.244 20090514142129 image/gif 1290 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1502241 034976a46a6f7ea7a1d430bda2fe37d1
http://carlsencards.com/images/extLinks/LunarAntics_8031.gif 80.196.101.244 20090514142130 image/gif 1749 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1503630 17f0b627c4501b8b855b374991a48aba
http://carlsencards.com/images/free-ecard-thumb01.jpg 80.196.101.244 20090514142131 image/jpeg 22283 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1505486 ca4df11fe9e63629c2b1bb78870fb99d
http://carlsencards.com/images/free-ecard-thumb39.jpg 80.196.101.244 20090514142132 image/jpeg 28202 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1527871 8bdfd270e43a4a208037bb5ba3cfe376
http://carlsencards.com/images/free-ecard-thumb24.jpg 80.196.101.244 20090514142133 image/jpeg 12852 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1556175 20387591446c41c0822fe42d5d4ae718
http://carlsencards.com/images/headerIhateYou.gif 80.196.101.244 20090514142134 image/gif 1114 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1569129 73a08d888c7604f742366f802b650191
http://carlsencards.com/images/headerThankYou.gif 80.196.101.244 20090514142135 image/gif 1151 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1570339 b375fcf947db11c1791a672105148bbf
http://carlsencards.com/images/free-ecard-thumb28.jpg 80.196.101.244 20090514142136 image/jpeg 34270 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1571586 c9f3d70ee18eb2339347dd0bb89cac45
http://carlsencards.com/images/free-ecard-thumb05.jpg 80.196.101.244 20090514142137 image/jpeg 25080 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1605958 9ea0286fedc9fdc9fdc974bd5beb1865
http://carlsencards.com/images/free-ecard-thumb31.jpg 80.196.101.244 20090514142139 image/jpeg 25735 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1631140 826314bdd78ca4a0789f8756e9c2acce
http://carlsencards.com/images/free-ecard-thumb10.jpg 80.196.101.244 20090514142140 image/jpeg 37587 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1656977 2f1ef1af155d5bf49635aa4965434483
http://carlsencards.com/images/free-ecard-thumb37.jpg 80.196.101.244 20090514142141 image/jpeg 20797 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1694666 b99cdfbe5a3550785706914d38b1b920
http://carlsencards.com/images/free-ecard-thumb38.jpg 80.196.101.244 20090514142142 image/jpeg 31304 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1715565 4048b673f42738829b4e4219fe182f9d
http://carlsencards.com/images/free-ecard-thumb06.jpg 80.196.101.244 20090514142143 image/jpeg 25857 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1746971 f12e4dbb801c3625446108cc242daf22
http://carlsencards.com/images/free-ecard-thumb04.jpg 80.196.101.244 20090514142144 image/jpeg 18646 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1772930 3f46b1311f13d5b5cddb1853d0a2566a
http://carlsencards.com/images/free-ecard-thumb50.jpg 80.196.101.244 20090514142145 image/jpeg 41779 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1791678 7f0f907a83c0df9af7b55982bbc85866
http://carlsencards.com/images/headerNewBaby.gif 80.196.101.244 20090514142146 image/gif 2108 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1833559 dcac3c0d3453d253208342995e3a219f
http://carlsencards.com/ecards-and-free-stuff-links.htm 80.196.101.244 20090514142147 text/html 21226 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1835762 1481136de8ca215f2234d125b14c8df3
http://carlsencards.com/images/contactCat.swf 80.196.101.244 20090514142150 application/x-shockwave-flash 13335 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1857091 7317a6e42689d92f889cb8f98150369f
http://carlsencards.com/free-ecard-51-happy-birthday-fortune-teller-rat.htm 80.196.101.244 20090514142151 text/html 13062 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1870539 c6e77fa5defb2a4847967591484ede21
http://www.carlsencards.com/robots.txt 80.196.101.244 20090514142152 text/plain 315 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1883724 3c79608796540b02dec4dbe674363277
http://www.carlsencards.com/images/free-ecard-thumb45.jpg 80.196.101.244 20090514142154 image/jpeg 28653 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1884124 a450cae67c9ff5e8b6a15cd30231df58
http://www.carlsencards.com/images/topStore_over.gif 80.196.101.244 20090514142155 image/gif 2098 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1912883 2846bb74f23886135ae3ba3474c1be29
http://www.carlsencards.com/images/headerLinks2.gif 80.196.101.244 20090514142156 image/gif 1606 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1915080 52f9201b4a3e96b3179eaf4089bd9274
http://www.carlsencards.com/images/extLinks/omg_button.gif 80.196.101.244 20090514142157 image/gif 7184 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1916784 6f3b0688e9d7fa24dd617bf21586f23b
http://www.carlsencards.com/images/mainTile.gif 80.196.101.244 20090514142158 image/gif 414 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1924073 05b850e73f9275b32977931711aad3c9
http://www.carlsencards.com/images/seatFillers/sleepingCat.swf 80.196.101.244 20090514142159 application/x-shockwave-flash 13684 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1924580 91ce1bb4ab57a03a5118d3dc1fa80f91
http://www.carlsencards.com/images/top7.gif 80.196.101.244 20090514142200 image/gif 598 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1938394 b8d2a50b99a52088b4ca50536cd1a9eb
http://www.carlsencards.com/images/headerHappyAnniversary.gif 80.196.101.244 20090514142201 image/gif 1547 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1939081 25f617e4c9fc5922ab184ae93aa7e6ad
http://www.carlsencards.com/images/top6Select_up.gif 80.196.101.244 20090514142202 image/gif 1961 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1940736 c991d96fb61d752e670df04e3e722008
http://www.carlsencards.com/images/free-ecard-thumb53.jpg 80.196.101.244 20090514142203 image/jpeg 27432 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1942796 87a4862c8b347a9def3f740eb0ae5125
http://www.carlsencards.com/images/headerILoveYou.gif 80.196.101.244 20090514142204 image/gif 1110 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1970334 0436b2477d71bf6b7baf9766e6536e6f
http://www.carlsencards.com/images/free-ecard-thumb59.jpg 80.196.101.244 20090514142205 image/jpeg 30211 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 1971544 3272721aefe5e9e9eda78e34630a74d2
http://www.carlsencards.com/images/free-ecard-thumb42.jpg 80.196.101.244 20090514142206 image/jpeg 25481 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2001861 c75cbf1c7a03fb8d96ee32ff4e1d03e5
http://www.carlsencards.com/images/free-ecard-thumb39.jpg 80.196.101.244 20090514142207 image/jpeg 28202 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2027448 a8354054f18359b7d3bc36878064ef40
http://www.carlsencards.com/images/extLinks/small2.gif 80.196.101.244 20090514142208 image/gif 2835 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2055756 034dfb5bacc6cc9749d7000122680b32
http://www.carlsencards.com/images/free-ecard-thumb20.jpg 80.196.101.244 20090514142210 image/jpeg 40593 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2058692 ec9a699539a0578b5a9b7f779e7e4995
http://www.carlsencards.com/images/extLinks/LunarAntics_8031.gif 80.196.101.244 20090514142211 image/gif 1749 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2099391 9d9241a73a607a6e2c48398c7388adf4
http://www.carlsencards.com/images/campBoxBtm.gif 80.196.101.244 20090514142212 image/gif 495 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2101251 3fda57621541b6e62045a650e12851ed
http://www.carlsencards.com/images/free-ecard-thumb29.jpg 80.196.101.244 20090514142213 image/jpeg 29006 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2101841 cc03202ebf90de3218f6fbe1595005f7
http://www.carlsencards.com/images/headerHappybirthday.gif 80.196.101.244 20090514142214 image/gif 1303 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2130953 c079d50c2763dc7bb59005653551c08e
http://www.carlsencards.com/images/free-ecard-thumb30.jpg 80.196.101.244 20090514142215 image/jpeg 23317 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2132361 9b512d72dedff8e229e3b4984a06142b
http://www.carlsencards.com/images/free-ecard-thumb06.jpg 80.196.101.244 20090514142216 image/jpeg 25857 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2155784 03bca8d30622058bb196142a16877399
http://www.carlsencards.com/images/headerSpreadtheword.gif 80.196.101.244 20090514142217 image/gif 1381 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2181747 ad24c14074088439cc054d1ecd6399b9
http://www.carlsencards.com/images/free-ecard-thumb16.jpg 80.196.101.244 20090514142218 image/jpeg 31363 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2183233 f15c405a6058e2aaa3ca470b85e516a3
http://www.carlsencards.com/images/extLinks/freesitee.gif 80.196.101.244 20090514142219 image/gif 3226 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2214702 59217b0bcefe11f942a82c9c37cb6472
http://www.carlsencards.com/images/free-ecard-thumb17.jpg 80.196.101.244 20090514142220 image/jpeg 38141 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2218032 e1889d9c01826dc404639f1d76e4fed7
http://www.carlsencards.com/images/campBoxTop.gif 80.196.101.244 20090514142221 image/gif 494 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2256279 9242e72a19877b9bd6f0085e976ac47a
http://www.carlsencards.com/images/extLinks/top20free88x53PD.gif 80.196.101.244 20090514142222 image/gif 3376 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2256868 6edb3bed7da58ab892832d7627e6894a
http://www.carlsencards.com/images/free-ecard-thumb35.jpg 80.196.101.244 20090514142223 image/jpeg 29206 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2260355 8f1a204a3bd6a6747a0c445fec71f7b1
http://www.carlsencards.com/images/free-ecard-thumb11.jpg 80.196.101.244 20090514142225 image/jpeg 30149 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2289667 c77fde5446fd697d75bf973d12e9e6c0
http://www.carlsencards.com/images/free-ecard-thumb08.jpg 80.196.101.244 20090514142226 image/jpeg 31964 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2319922 7d417644a19f8d2d1135b08c852c8975
http://www.carlsencards.com/images/free-ecard-thumb43.jpg 80.196.101.244 20090514142227 image/jpeg 32274 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2351992 0c986a3739cfaf4d6ebf0731367ca803
http://www.carlsencards.com/images/free-ecard-thumb57.jpg 80.196.101.244 20090514142229 image/jpeg 34457 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2384372 2d994d7cbeb038c9ec7160729915cf13
http://www.carlsencards.com/images/headerHoliday.gif 80.196.101.244 20090514142230 image/gif 1415 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2418935 1914a3e1335b5c66ef5f41a33a1bb79d
http://www.carlsencards.com/images/free-ecard-thumb01.jpg 80.196.101.244 20090514142231 image/jpeg 22283 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2420449 5ce883a174d6b7c534987daf122ab9f1
http://www.carlsencards.com/images/newCardTemplateBtm_hamster.jpg 80.196.101.244 20090514142232 image/jpeg 60167 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2442838 eb4dcfdabaec6bc1df130930bcee63f6
http://www.carlsencards.com/images/headerSorry.gif 80.196.101.244 20090514142233 image/gif 1021 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2503119 c6b7c052a5c71a416e103595ee99a051
http://www.carlsencards.com/images/free-ecard-thumb02.jpg 80.196.101.244 20090514142234 image/jpeg 25771 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2504237 0155dda999841d75dae4a06665d09c82
http://carlsencards.com/pagead/atf.js 80.196.101.244 20090514142236 text/html 408 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2530114 ca529ca0a81ca6a6ab172f576765e825
http://carlsencards.com/gampad/cookie.js?callback=_GA_googleCookieHelper.setCookieInfo 80.196.101.244 20090514142237 text/html 411 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2530605 d70589c7287caae20347feb9e7365ae5
http://carlsencards.com/JavaScript1.1 80.196.101.244 20090514142238 text/html 408 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2531148 5d97be66dbb23cc2fded99f0f490a277
http://carlsencards.com/9.50 80.196.101.244 20090514142239 text/html 399 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2531639 3f7417ab562fbb55f18f47e9dfb5f30d
http://carlsencards.com/_root.card_ 80.196.101.244 20090514142240 text/html 406 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2532112 1df2613246be5a72d9e2982138c3de0c
http://carlsencards.com/EcardFooter.swf 80.196.101.244 20090514142241 application/x-shockwave-flash 153968 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2532599 7cf4de58aeeee63b90be6e17314132b4
http://carlsencards.com/Ecard49.swf 80.196.101.244 20090514142242 application/x-shockwave-flash 652471 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 2686675 816b319f96b2cdddcf9d319c99159cd3
http://carlsencards.com/free-ecard-43-happy-fathers-day-mixer-board.htm 80.196.101.244 20090514142244 text/html 13353 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 3339250 d719774e6adede501391f2c165782e14
http://carlsencards.com/cat-free-ecards-no-special-occasion.htm 80.196.101.244 20090514142245 text/html 20995 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 3352722 4496a5e50e1e5c0e430ac4a12df4df63
http://carlsencards.com/Ecard5.swf 80.196.101.244 20090514142246 application/x-shockwave-flash 560543 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 3373828 2b64574f83b99b3f70e268c1fd3a85cf
http://carlsencards.com/images/seatFillers/sleepingCat.swf 80.196.101.244 20090514142248 application/x-shockwave-flash 13684 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 3934474 7dc16b7f477f7be799b22eefeb97a1ce
http://carlsencards.com/Ecard12.swf 80.196.101.244 20090514142250 application/x-shockwave-flash 564674 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 3948284 342f2e5a572177316279b35f82380d54
http://carlsencards.com/cat-free-ecards-i-wont-be-coming-to-work-today.htm 80.196.101.244 20090514142252 text/html 16102 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 4513062 f75600de4741ff5bedab7314d11c764e
http://carlsencards.com/free-ecard-14-get-well-cat-flower-medicine.htm 80.196.101.244 20090514142253 text/html 13116 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 4529286 d14dd8ba225470ee888607fbb0156279
http://carlsencards.com/Ecard7.swf 80.196.101.244 20090514142255 application/x-shockwave-flash 773226 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 4542520 5634b6b6585046437291cd6ba0234a51
http://carlsencards.com/free-ecard-19-merry-christmas-penguins-writing-snow.htm 80.196.101.244 20090514142257 text/html 13065 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 5315849 0ccdce34d2630ad6764bdf22a8e4ad9e
http://carlsencards.com/Ecard55.swf 80.196.101.244 20090514142259 application/x-shockwave-flash 648169 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 5329041 f873dd6dfffce94ed5796a95fa7603e7
http://carlsencards.com/Ecard28.swf 80.196.101.244 20090514142301 application/x-shockwave-flash 528409 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 5977314 ae3dd48945363092dc0055ac6eb73d38
http://carlsencards.com/free-ecard-24-no-special-occasion-hi-mouse-pixar.htm 80.196.101.244 20090514142306 text/html 13160 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 6505827 b70f12ef66cd87e2cf16ed957e86158f
http://carlsencards.com/free-ecard-59-happy-birthday-flowers-rat.htm 80.196.101.244 20090514142308 text/html 14149 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 6519111 ada316d8f5a262e57b2777b07ac4b4fd
http://carlsencards.com/Ecard8.swf 80.196.101.244 20090514142310 application/x-shockwave-flash 427416 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 6533376 ced1dce2c999b50c1aa703c8a107559d
http://carlsencards.com/Ecard54.swf 80.196.101.244 20090514142311 application/x-shockwave-flash 743259 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 6960895 fdb54f07ae2b69ae673ae286b728d926
http://carlsencards.com/Ecard23.swf 80.196.101.244 20090514142314 application/x-shockwave-flash 734644 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 7704258 87ce047a4f943eb506ebd2b8074fcf91
http://carlsencards.com/free-ecard-18-i-love-you-cat-pictures-true-love.htm 80.196.101.244 20090514142316 text/html 13038 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 8439006 12536e371038fc7868f148e6db590d98
http://carlsencards.com/free-ecard-56-happy-birthday-fireworks-mice.htm 80.196.101.244 20090514142318 text/html 13070 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 8452167 aa8398ca0b6d815587f502fabfe3f02f
http://carlsencards.com/free-ecard-06-happy-birthday-devil-angel-cake.htm 80.196.101.244 20090514142321 text/html 13153 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 8465356 740d34eeca06b41b21366066f9aab912
http://carlsencards.com/cat-free-ecards-holiday-greetings.htm 80.196.101.244 20090514142322 text/html 30274 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 8478630 a1c0607810c7adaa4e5bec19f02209cd
http://carlsencards.com/free-ecard-10-congratulations-cat-ebay-elvis-cake.htm 80.196.101.244 20090514142323 text/html 13099 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 8509013 6d2aad67f34f0e01741d247f422f9add
http://carlsencards.com/free-ecard-11-congratulations-african-drummer-ants.htm 80.196.101.244 20090514142325 text/html 13123 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 8522237 e66a656aa1e48944d400778b29a8c660
http://carlsencards.com/free-ecard-25-no-special-occasion-text-messaging-lemurs.htm 80.196.101.244 20090514142327 text/html 13131 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 8535486 dc0eb2b7fc8bc8df4837b11bc87c764b
http://carlsencards.com/free-ecard-15-i-love-you-chameleon-chameleogram.htm 80.196.101.244 20090514142329 text/html 13034 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 8548748 41d1446034278a0ad22d61d8442e891b
http://carlsencards.com/free-ecard-36-congratulations-interactive-surprise-cats-mice.htm 80.196.101.244 20090514142331 text/html 13111 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 8561905 c4bc37e7069e275dc3b44896c61fc707
http://carlsencards.com/cat-free-ecards-thank-you.htm 80.196.101.244 20090514142334 text/html 15748 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 8575152 faf382ab0cd69b10db0a428c8aadaf43
http://carlsencards.com/Ecard1.swf 80.196.101.244 20090514142335 application/x-shockwave-flash 630334 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 8591001 9cf0c75f6ae70e4a1f144d0e8ace4377
http://carlsencards.com/free-ecard-09-congratulations-penguins-versus-animator.htm 80.196.101.244 20090514142337 text/html 13069 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 9221438 856ea3aebdf095d9eef7c6aeab72fc68
http://carlsencards.com/Ecard39.swf 80.196.101.244 20090514142339 application/x-shockwave-flash 466776 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 9234637 6f2b9694e5f203b0fd494019a131fdee
http://carlsencards.com/free-ecard-47-no-special-occasion-cheerleader-mouse.htm 80.196.101.244 20090514142341 text/html 13203 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 9701517 2c47f1fdcb36b246bbbe71b82bf3c635
http://carlsencards.com/Ecard53.swf 80.196.101.244 20090514142342 application/x-shockwave-flash 518189 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 9714847 7be2ba1713362717262c5d797ea0563c
http://carlsencards.com/free-ecard-40-no-special-occasion-memory-game-4cards.htm 80.196.101.244 20090514142344 text/html 13370 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 10233140 ecbe87c24874b7e532773f36ea781211
http://carlsencards.com/free-ecard-26-halloween-mouse-in-pumpkin-explosion.htm 80.196.101.244 20090514142346 text/html 13114 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 10246638 14d3241829e0b68b6befbabf5f1aea0b
http://carlsencards.com/EcardFooter2.swf 80.196.101.244 20090514142347 application/x-shockwave-flash 153968 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 10259878 a5c6ffbea17a0715ef292cd3f574aa15
http://www.carlsencards.com/free-ecard-59-happy-birthday-flowers-rat.htm 80.196.101.244 20090514142348 text/html 14149 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 10413955 f18da4a136463fc6968f78342520457d
http://www.carlsencards.com/free-ecard-53-new-baby-llamas.htm 80.196.101.244 20090514142350 text/html 13102 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 10428224 f744588424d202eae5ca06e2d4e61f0e
http://www.carlsencards.com/cat-free-ecards-congratulations.htm 80.196.101.244 20090514142351 text/html 21973 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 10441435 a3c413d5e2f16f2061cc1558172bf073
http://www.carlsencards.com/Ecard13.swf 80.196.101.244 20090514142352 application/x-shockwave-flash 502899 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 10463519 405224508d9ed08e9c58e44770cecbaf
http://www.carlsencards.com/images/contactCat 80.196.101.244 20090514142354 text/html 412 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 10966526 22e4c91f41e00ffadea61366bdc1014a
http://www.carlsencards.com/Ecard8.swf 80.196.101.244 20090514142355 application/x-shockwave-flash 427416 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 10967029 bfd3e22dd4298d07e90a96ad0b88d25b
http://www.carlsencards.com/free-ecard-51-happy-birthday-fortune-teller-rat.htm 80.196.101.244 20090514142357 text/html 13062 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 11394552 649e67a0f8e94ff360eb3f651fc3796f
http://www.carlsencards.com/Ecard1.swf 80.196.101.244 20090514142358 application/x-shockwave-flash 630334 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 11407741 8620078f5728453fa0a7ae4b555d3f6c
http://www.carlsencards.com/cat-free-ecards-i-wont-be-coming-to-work-today.htm 80.196.101.244 20090514142400 text/html 16102 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 12038182 1e76f83c025955cc1196a48980497e94
http://www.carlsencards.com/Ecard57.swf 80.196.101.244 20090514142402 application/x-shockwave-flash 438215 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 12054410 caa48536a202054f1bd4d9d330e27d79
http://www.carlsencards.com/free-ecard-00-spread-the-word-tell-your-friends.htm 80.196.101.244 20090514142403 text/html 13230 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 12492733 64e58e240f450f857dfcdf46fe26a50b
http://www.carlsencards.com/free-ecard-33-happy-valentines-day-funny-cats.htm 80.196.101.244 20090514142407 text/html 13272 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 12506090 53641277c41a4a4aa517f146b8a790a8
http://www.carlsencards.com/free-ecard-43-happy-fathers-day-mixer-board.htm 80.196.101.244 20090514142408 text/html 13353 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 12519487 a9cf1458c8544de1b8e8ddb11841076f
http://www.carlsencards.com/cat-free-ecards-i-hate-you.htm 80.196.101.244 20090514142410 text/html 15181 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 12532963 83aee62b33956a9672923663caab5cfa
http://www.carlsencards.com/Ecard56.swf 80.196.101.244 20090514142411 application/x-shockwave-flash 781600 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 12548250 68d16dbed5b7da1eef251011f1c81f21
http://www.carlsencards.com/free-ecard-05-happy-birthday-postcard-coffee.htm 80.196.101.244 20090514142414 text/html 13054 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 13329958 45b3472f660d90073fa0362232c1d0c0
http://www.carlsencards.com/Ecard47.swf 80.196.101.244 20090514142415 application/x-shockwave-flash 587159 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 13343136 54ab8cfa2c0d51dc03963327a09ada4f
http://www.carlsencards.com/Ecard54.swf 80.196.101.244 20090514142417 application/x-shockwave-flash 743259 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 13930403 ea4e72317d25598064a7414125ea0631
http://www.carlsencards.com/cat-free-ecards-get-well-soon.htm 80.196.101.244 20090514142419 text/html 15979 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 14673770 cf9d43c17ee9ccaddec64ea64d144199
http://www.carlsencards.com/free-ecard-29-merry-christmas-cat-mice-rudolph-prank.htm 80.196.101.244 20090514142421 text/html 13172 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 14689858 8e3f8b34f01f866c7debcc8fb9a2ec82
http://www.carlsencards.com/about.htm 80.196.101.244 20090514142422 text/html 15214 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 14703162 e5404e43ebc7f6bed253191af9c0de57
http://www.carlsencards.com/free-ecard-23-no-special-occasion-cosmopolitan-recipe.htm 80.196.101.244 20090514142423 text/html 13180 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 14718461 71759b26cd9b8718f00fc6dc8b95739d
http://www.carlsencards.com/Ecard40.swf 80.196.101.244 20090514142425 application/x-shockwave-flash 582585 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 14731774 e628dac4c259fb8329d0d23cb5724254
http://www.carlsencards.com/free-ecard-27-happy-thanksgiving-funny-cats.htm 80.196.101.244 20090514142426 text/html 13127 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 15314467 cadb51b7588ff685d70575d66ba22d28
http://www.carlsencards.com/free-ecard-03-wont-come-to-work-mona-lisa.htm 80.196.101.244 20090514142428 text/html 13146 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 15327717 3735a1150e45a066d4e816de73fe0dd7
http://www.carlsencards.com/Ecard14.swf 80.196.101.244 20090514142430 application/x-shockwave-flash 517431 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 15340984 48d5cf2df27d5ddc181fa4bdb4acc7a8
http://www.carlsencards.com/Ecard38.swf 80.196.101.244 20090514142431 application/x-shockwave-flash 854744 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 15858523 757b8994ad116c511436a7637731e8a2
http://www.carlsencards.com/Ecard49.swf 80.196.101.244 20090514142434 application/x-shockwave-flash 652471 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 16713375 f5340294e9958cc3d68f4d9cfca4b491
http://www.carlsencards.com/cat-free-ecards-spread-the-word.htm 80.196.101.244 20090514142436 text/html 16306 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 17365954 3f3eca757cd3e8a786a17f8327c52bbd
http://www.carlsencards.com/free-ecard-07-happy-birthday-devil-angel-directors-cut.htm 80.196.101.244 20090514142437 text/html 13136 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 17382371 ebd3d8e12515c5f5f09fa370e5f0a14d
http://www.carlsencards.com/Ecard37.swf 80.196.101.244 20090514142439 application/x-shockwave-flash 669049 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 17395641 e26c9759c9790342dca6cab7357d3995
http://www.carlsencards.com/free-ecard-17-wont-come-to-work-scream-existential-angst.htm 80.196.101.244 20090514142441 text/html 13138 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 18064798 7a4888a6c8c3e8019b9032c414565def
http://www.carlsencards.com/Ecard32.swf 80.196.101.244 20090514142442 application/x-shockwave-flash 644495 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 18078072 eb543833bef63e3da3f4c7abe1ebbc66
http://www.carlsencards.com/Ecard28.swf 80.196.101.244 20090514142444 application/x-shockwave-flash 528409 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 18722675 73659d14bf6ba68fabb63f74e686b945
http://www.carlsencards.com/free-ecard-15-i-love-you-chameleon-chameleogram.htm 80.196.101.244 20090514142446 text/html 13034 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 19251192 fde9eb5efc3f724304ed04809fa42110
http://www.carlsencards.com/Ecard18.swf 80.196.101.244 20090514142449 application/x-shockwave-flash 529718 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 19264353 27de40435177394f10ee653174a045c7
http://www.carlsencards.com/free-ecard-30-i-hate-you-cat-toy-car-accident.htm 80.196.101.244 20090514142450 text/html 13100 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 19794179 86fe566582b5952eed32fb33d5d97384
http://www.carlsencards.com/free-ecard-52-new-baby-polar-bears.htm 80.196.101.244 20090514142452 text/html 13105 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 19807404 a5d3a9a06d7f4262a5e2b1fe8409981a
http://www.carlsencards.com/free-ecard-36-congratulations-interactive-surprise-cats-mice.htm 80.196.101.244 20090514142455 text/html 13111 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 19820623 d71a4b61fe0fe529fc3f9eadf2f23f6d
http://www.carlsencards.com/cat-free-ecards-holiday-greetings.htm 80.196.101.244 20090514142457 text/html 30274 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 19833874 5cb0f1e709722abe05a73b7bc1664a4d
http://www.carlsencards.com/ 80.196.101.244 20090514142458 text/html 116851 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 19864261 bbc7018d1c9f7585b89637e291ce2499
http://carlsencards.com/Ecard43.swf 80.196.101.244 20090514142500 application/x-shockwave-flash 566782 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 19981189 ec238da26d45b3abb35d3c4d10ba2d49
http://carlsencards.com/Ecard22.swf 80.196.101.244 20090514142503 application/x-shockwave-flash 460109 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 20548075 21545281f779d0564dbb52480f25daeb
http://carlsencards.com/Ecard13.swf 80.196.101.244 20090514142505 application/x-shockwave-flash 502899 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 21008288 df947b03ba2acbc1f34b8baa2743e25a
http://carlsencards.com/Ecard24.swf 80.196.101.244 20090514142508 application/x-shockwave-flash 573089 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 21511291 fb1bfe130e09c433cf2ea6631fa75b3e
http://carlsencards.com/Scripts/expressInstall.swf 80.196.101.244 20090514142510 application/x-shockwave-flash 1085 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 22084484 7523ec52f7730d2e2e44050256d26bba
http://carlsencards.com/Download.Complete 80.196.101.244 20090514142513 text/html 412 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 22085686 0c5f37944d8715bf1f0def5bdee1d48f
http://carlsencards.com/6.0.65 80.196.101.244 20090514142514 text/html 401 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 22086185 44f192dfa71a226c56ee9f4f25e24553
http://carlsencards.com/Ecard29.swf 80.196.101.244 20090514142515 application/x-shockwave-flash 699800 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 22086662 66d64ccc74c8f4d5c8b849265bc01f3d
http://carlsencards.com/Ecard6.swf 80.196.101.244 20090514142517 application/x-shockwave-flash 828326 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 22786566 ddba464fb338ebb4f1133a5036d41650
http://carlsencards.com/Ecard10.swf 80.196.101.244 20090514142520 application/x-shockwave-flash 707624 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 23614995 ed13bd37c5e8b7c61be761ace33a3ab8
http://carlsencards.com/Ecard42.swf 80.196.101.244 20090514142522 application/x-shockwave-flash 514252 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 24322723 c4c7c83bad45919619f176a78dde2690
http://carlsencards.com/Ecard48.swf 80.196.101.244 20090514142525 application/x-shockwave-flash 715092 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 24837079 3d92eab3d164c571a5a7c00b16f1d3b6
http://carlsencards.com/Ecard47.swf 80.196.101.244 20090514142527 application/x-shockwave-flash 587159 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 25552275 1a1ec6bd7bd32fd4a0075fe06ac83f36
http://www.carlsencards.com/8.0.35.0 80.196.101.244 20090514142530 text/html 403 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 26139538 3a62ce23fce31c922b662b6ef1ba0b4d
http://www.carlsencards.com/Download.Complete 80.196.101.244 20090514142532 text/html 412 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 26140023 d11b9a2b8793e0c9c9b147d06276a8bf
http://www.carlsencards.com/EcardFooter2.swf 80.196.101.244 20090514142533 application/x-shockwave-flash 153968 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 26140526 689b5c6573ee6cacd77679d863a1c6f8
http://www.carlsencards.com/text/css 80.196.101.244 20090514142534 text/html 403 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 26294607 55a54d9e3540671488f1fcd57b8ae25a
http://www.carlsencards.com/Ecard19.swf 80.196.101.244 20090514142536 application/x-shockwave-flash 809442 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 26295092 fcfc324fb67af9fbcc7285f5a58176e9
http://www.carlsencards.com/Ecard51.swf 80.196.101.244 20090514142538 application/x-shockwave-flash 480006 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 27104642 48f332e8f19750e089069f8e0852746a
http://www.carlsencards.com/Ecard25.swf 80.196.101.244 20090514142542 application/x-shockwave-flash 492505 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 27584756 6f35d7557689122333322c5a04ba9278
http://www.carlsencards.com/Ecard46.swf 80.196.101.244 20090514142544 application/x-shockwave-flash 789590 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 28077369 fe735272d898eb07f038568e701cb3ff
http://www.carlsencards.com/Ecard5.swf 80.196.101.244 20090514142546 application/x-shockwave-flash 560543 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 28867067 620e61a5b52dd9b3d0ddc60e934cd175
http://www.carlsencards.com/Ecard48.swf 80.196.101.244 20090514142549 application/x-shockwave-flash 715092 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 29427717 1cc3b820669d6ba068b6728a2f23c3a0
http://www.carlsencards.com/Ecard35.swf 80.196.101.244 20090514142551 application/x-shockwave-flash 648064 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 30142917 b208cb6dbe884bae9d9f9a16b7d66584
http://www.carlsencards.com/Ecard34.swf 80.196.101.244 20090514142554 application/x-shockwave-flash 747454 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 30791089 9c5a580b606ca30d877be40cb473d8e2
http://www.carlsencards.com/Ecard17.swf 80.196.101.244 20090514142556 application/x-shockwave-flash 539455 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 31538651 52bdcef4845e882e9101d7d56e1f89dc
http://www.carlsencards.com/Ecard2.swf 80.196.101.244 20090514142558 application/x-shockwave-flash 591272 1-1-20090514142101-00000-kb-test-har-002.kb.dk.arc 32078214 59ec334f20729d1e9cec2c0f68788e48


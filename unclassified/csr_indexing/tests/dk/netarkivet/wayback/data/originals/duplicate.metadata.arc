filedesc://1121-metadata-1.arc 0.0.0.0 20090601153645 text/plain 77
1 0 InternetArchive
URL IP-address Archive-date Content-type Archive-length

metadata://netarkivet.dk/crawl/setup/duplicatereductionjobs?majorversion=1&minorversion=0&harvestid=130&harvestnum=58&jobid=1121 130.225.27.140 20090601153611 text/plain 4
1119
metadata://netarkivet.dk/crawl/setup/order.xml?heritrixVersion=1.12.1b&harvestid=130&jobid=1121 130.225.27.140 20090601153616 text/xml 26069
<?xml version="1.0" encoding="UTF-8"?>

<crawl-order xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="heritrix_settings.xsd">
  <meta>
    <name>forsider_plus_2_niveauer</name>
    <description>Special template harvesting only in 2 levels below seeds</description>
    <operator>Admin</operator>
    <organization/>
    <audience/>
    <date>20080118111217</date>
  </meta>
  <controller>
    <string name="settings-directory">settings</string>
    <string name="disk-path">/home/netarkiv/PLIGT/harvester_8082/1121_1243870571640</string>
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
    <integer name="bdb-cache-percent">40</integer>
    <!-- DecidingScope migrated from DomainScope -->
    <newObject name="scope" class="org.archive.crawler.deciderules.DecidingScope">
      <boolean name="enabled">true</boolean>
      <string name="seedsfile">/home/netarkiv/PLIGT/harvester_8082/1121_1243870571640/seeds.txt</string>
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
            <integer name="max-hops">2</integer>
          </newObject>
          <newObject name="rejectIfPathological" class="org.archive.crawler.deciderules.PathologicalPathDecideRule">
            <integer name="max-repetitions">3</integer>
          </newObject>
          <newObject name="acceptIfTranscluded" class="org.archive.crawler.deciderules.TransclusionDecideRule">
            <integer name="max-trans-hops">5</integer>
            <integer name="max-speculative-hops">1</integer>
          </newObject>
          <newObject name="pathdepthfilter" class="org.archive.crawler.deciderules.TooManyPathSegmentsDecideRule">
            <integer name="max-path-depth">20</integer>
          </newObject>
          <newObject name="acceptIfPrerequisite" class="org.archive.crawler.deciderules.PrerequisiteAcceptDecideRule"/>
          <newObject name="selektive_crawlertraps" class="org.archive.crawler.deciderules.MatchesListRegExpDecideRule">
            <string name="decision">REJECT</string>
            <string name="list-logic">OR</string>
            <stringList name="regexp-list">
              <string>.*dr\.dk.*epg\.asp.*</string>
              <string>.*eavisen.ekstrabladet.dk.*\.zip</string>
              <string>.*files\.tv2\.dk.*\.mp3.*</string>
              <string>.*sportsresultater\.tv2\.dk.*</string>
              <string>.*euroinvestor\.dk\/Stock.*</string>
              <string>.*edition\.borsen\.dk\/arkiv.*</string>
              <string>.*x-cago.com\/arkiv.*</string>
            </stringList>
          </newObject>
        </map>
        <!-- end rules -->
      </newObject>
      <!-- end decide-rules -->
    </newObject>
    <!-- End DecidingScope -->
    <map name="http-headers">
      <string name="user-agent">Mozilla/5.0 (compatible; heritrix/1.12.1b +http://netarkivet.dk/website/info.html)</string>
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
      <integer name="preference-embed-hops">2</integer>
      <integer name="total-bandwidth-usage-KB-sec">3000</integer>
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
      <newObject name="stillinger_i_staten_timecode" class="org.archive.crawler.url.canonicalize.RegexRule">
        <boolean name="enabled">true</boolean>
        <string name="matching-regex">^(.*stillinger-i-staten.dk.*)(tc=.*)$</string>
        <string name="format">${1}</string>
        <string name="comment">fjerner tc=... fra stillinger-i-staten.dk</string>
      </newObject>
      <newObject name="publicus_com" class="org.archive.crawler.url.canonicalize.RegexRule">
        <boolean name="enabled">true</boolean>
        <string name="matching-regex">^(.*publicus.com.*)(Req.*)$</string>
        <string name="format">${1}</string>
        <string name="comment">fjerner Req=... fra publicus.com (stiften.dk)</string>
      </newObject>
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
        <long name="group-max-all-kb">976563</long>
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
        <string name="load-cookies-from-file">/tmp/cookies.txt</string>
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
        <boolean name="treat-frames-as-embed-links">false</boolean>
        <boolean name="ignore-form-action-urls">true</boolean>
        <boolean name="overly-eager-link-detection">false</boolean>
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
      <newObject name="ExtractorImpliedURI0" class="org.archive.crawler.extractor.ExtractorImpliedURI">
        <boolean name="enabled">true</boolean>
        <string name="trigger-regexp">^(http://www.e-pages.dk/urban/[0-9]*/)$</string>
        <string name="build-pattern">$1print.pdf</string>
        <boolean name="remove-trigger-uris">false</boolean>
      </newObject>
      <newObject name="ExtractorImpliedURI1" class="org.archive.crawler.extractor.ExtractorImpliedURI">
        <boolean name="enabled">true</boolean>
        <string name="trigger-regexp">^(.*)/swf/.*\.swf\?.*video_id=([^&amp;]+).*(t=[^&amp;]+).*$</string>
        <string name="build-pattern">$1/get_video?video_id=$2&amp;$3&amp;eurl=http%3A%2F%2Fdansk%2Dpolitik%2Etv%2F</string>
        <boolean name="remove-trigger-uris">false</boolean>
      </newObject>
      <newObject name="ExtractorImpliedURI2" class="org.archive.crawler.extractor.ExtractorImpliedURI">
        <boolean name="enabled">true</boolean>
        <string name="trigger-regexp">^(.*)/swf/.*\.swf\?.*video_id=([^&amp;]+).*(t=[^&amp;]+).*$</string>
        <string name="build-pattern">$1/v/$2</string>
        <boolean name="remove-trigger-uris">false</boolean>
      </newObject>
      <newObject name="ExtractorImpliedURI3" class="org.archive.crawler.extractor.ExtractorImpliedURI">
        <boolean name="enabled">true</boolean>
        <string name="trigger-regexp">^(.*)/swf/.*\.swf\?.*video_id=([^&amp;]+).*(t=[^&amp;]+).*$</string>
        <string name="build-pattern">$1/get_video?video_id=$2&amp;$3&amp;eurl=http%3A%2F%2Fwww%2Edansk%2Dpolitik%2Etv%2F</string>
        <boolean name="remove-trigger-uris">false</boolean>
      </newObject>
      <newObject name="ExtractorImpliedURI4" class="org.archive.crawler.extractor.ExtractorImpliedURI">
        <boolean name="enabled">true</boolean>
        <string name="trigger-regexp">^(.*)/swf/.*\.swf\?.*video_id=([^&amp;]+).*eurl=.*p%3D([^&amp;]+).*(t=[^&amp;]+).*$</string>
        <string name="build-pattern">$1/get_video?video_id=$2&amp;$4&amp;eurl=http%3A%2F%2Fdansk%2Dpolitik%2Etv%2F%3Fp%3D$3</string>
        <boolean name="remove-trigger-uris">false</boolean>
      </newObject>
      <newObject name="ExtractorImpliedURI5" class="org.archive.crawler.extractor.ExtractorImpliedURI">
        <boolean name="enabled">true</boolean>
        <string name="trigger-regexp">^(.*)/swf/.*\.swf\?.*video_id=([^&amp;]+).*eurl=.*tag%3D([^&amp;]+).*(t=[^&amp;]+).*$</string>
        <string name="build-pattern">$1/get_video?video_id=$2&amp;$4&amp;eurl=http%3A%2F%2Fdansk%2Dpolitik%2Etv%2Findex%2Ephp%3Ftag%3D$3</string>
        <boolean name="remove-trigger-uris">false</boolean>
      </newObject>
      <newObject name="ExtractorImpliedURI6" class="org.archive.crawler.extractor.ExtractorImpliedURI">
        <boolean name="enabled">true</boolean>
        <string name="trigger-regexp">^(.*)/swf/.*\.swf\?.*video_id=([^&amp;]+).*eurl=.*tag%3D([^&amp;]+).*(t=[^&amp;]+).*$</string>
        <string name="build-pattern">$1/get_video?video_id=$2&amp;$4&amp;eurl=http%3A%2F%2Fdansk%2Dpolitik%2Etv%3Ftag%3D$3</string>
        <boolean name="remove-trigger-uris">false</boolean>
      </newObject>
      <newObject name="ExtractorImpliedURI7" class="org.archive.crawler.extractor.ExtractorImpliedURI">
        <boolean name="enabled">true</boolean>
        <string name="trigger-regexp">^(.*)/swf/.*\.swf\?.*video_id=([^&amp;]+).*(t=[^&amp;]+).*$</string>
        <string name="build-pattern">$1/api2_rest?method=youtube%2Evideos%2Eget%5Fvideo%5Finfo&amp;video_id=$2</string>
        <boolean name="remove-trigger-uris">false</boolean>
      </newObject>
      <newObject name="ExtractorImpliedURI8" class="org.archive.crawler.extractor.ExtractorImpliedURI">
        <boolean name="enabled">true</boolean>
        <string name="trigger-regexp">^(.*)/swf/.*\.swf\?.*video_id=([^&amp;]+).*(t=[^&amp;]+).*$</string>
        <string name="build-pattern">$1/embed_api_rest?method=list%5Frecs&amp;v=$2&amp;$3</string>
        <boolean name="remove-trigger-uris">false</boolean>
      </newObject>
      <newObject name="ExtractorImpliedURI9" class="org.archive.crawler.extractor.ExtractorImpliedURI">
        <boolean name="enabled">true</boolean>
        <string name="trigger-regexp">^(.*)/swf/.*\.swf\?.*video_id=([^&amp;]+).*(t=[^&amp;]+).*$</string>
        <string name="build-pattern">http://img.youtube.com/vi/$2/default.jpg</string>
        <boolean name="remove-trigger-uris">false</boolean>
      </newObject>
      <newObject name="ExtractorImpliedURI10" class="org.archive.crawler.extractor.ExtractorImpliedURI">
        <boolean name="enabled">true</boolean>
        <string name="trigger-regexp">^(.*)/player2.swf\?.*video_id=([^&amp;]+).*(t=[^&amp;]+).*$</string>
        <string name="build-pattern">$1/get_video?video_id=$2&amp;$3</string>
        <boolean name="remove-trigger-uris">false</boolean>
      </newObject>
      <newObject name="ExtractorImpliedURI11" class="org.archive.crawler.extractor.ExtractorImpliedURI">
        <boolean name="enabled">true</boolean>
        <string name="trigger-regexp">http://(.*)/watch.*v=(.*)</string>
        <string name="build-pattern">http://$1/v/$2&amp;rel=1</string>
        <boolean name="remove-trigger-uris">false</boolean>
      </newObject>
      <newObject name="ExtractorImpliedURI12" class="org.archive.crawler.extractor.ExtractorImpliedURI">
        <boolean name="enabled">true</boolean>
        <string name="trigger-regexp">^(.*)/swf/.*\.swf\?video_id=([^&amp;]+).*(t=[^&amp;]+).*$</string>
        <string name="build-pattern">$1/get_video?video_id=$2&amp;$3</string>
        <boolean name="remove-trigger-uris">false</boolean>
      </newObject>
    </map>
    <!-- end of Heritrix extract processors -->
    <!-- Heritrix write processors -->
    <map name="write-processors">
      <newObject name="DeDuplicator" class="is.hi.bok.deduplicator.DeDuplicator">
        <boolean name="enabled">true</boolean>
        <map name="filters"/>
        <string name="index-location">/home/netarkiv/PLIGT/cache/DEDUP_CRAWL_LOG/1119-cache</string>
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
        <string name="prefix">1121-130</string>
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
      <map name="credentials">
        <newObject name="licitationen_login_1" class="org.archive.crawler.datamodel.credential.Rfc2617Credential">
          <string name="credential-domain">www.licitationen.dk</string>
          <string name="realm">Dagbladet Licitationen</string>
          <string name="login">453587</string>
          <string name="password">2730Alle</string>
        </newObject>
        <newObject name="mymusic_login_1" class="org.archive.crawler.datamodel.credential.HtmlFormCredential">
          <string name="credential-domain">www.mymusic.dk</string>
          <string name="login-uri">http://www.mymusic.dk/konto/login2.asp</string>
          <string name="http-method">POST</string>
          <map name="form-items">
            <string name="username">atman</string>
            <string name="password">ziggyzig</string>
            <string name="autologin">y</string>
          </map>
        </newObject>
        <newObject name="arto_login_1" class="org.archive.crawler.datamodel.credential.HtmlFormCredential">
          <string name="credential-domain">www.arto.dk</string>
          <string name="login-uri">http://www.arto.dk/r2/frames/navigation.asp</string>
          <string name="http-method">POST</string>
          <map name="form-items">
            <string name="action">submit</string>
            <string name="brugernavn">Statsbib</string>
            <string name="kodeord">bogorm</string>
            <string name="AutoLogin">Ja</string>
            <string name="loginKnap">Log ind</string>
          </map>
        </newObject>
        <newObject name="weekendavisen_login_1" class="org.archive.crawler.datamodel.credential.HtmlFormCredential">
          <string name="credential-domain">www.weekendavisen.dk</string>
          <string name="login-uri">http://validate.fbwea.weekendavisen.dk/dc/user_status</string>
          <string name="http-method">POST</string>
          <map name="form-items">
            <string name="url"/>
            <string name="login_user_name">AVIS34</string>
            <string name="login_password">SEKTIONER</string>
            <string name="action">Log ind</string>
          </map>
        </newObject>
      </map>
    </newObject>
  </controller>
</crawl-order>

metadata://netarkivet.dk/crawl/setup/harvestInfo.xml?heritrixVersion=1.12.1b&harvestid=130&jobid=1121 130.225.27.140 20090601153611 text/xml 394
<?xml version="1.0" encoding="UTF-8"?>

<harvestInfo>
  <version>0.2</version>
  <jobId>1121</jobId>
  <priority>HIGHPRIORITY</priority>
  <harvestNum>58</harvestNum>
  <origHarvestDefinitionID>130</origHarvestDefinitionID>
  <maxBytesPerDomain>1000000000</maxBytesPerDomain>
  <maxObjectsPerDomain>-1</maxObjectsPerDomain>
  <orderXMLName>forsider_plus_2niveauer</orderXMLName>
</harvestInfo>

metadata://netarkivet.dk/crawl/setup/seeds.txt?heritrixVersion=1.12.1b&harvestid=130&jobid=1121 130.225.27.140 20090601153611 text/plain 31
http://www.aros.dk/enteraction/
metadata://netarkivet.dk/crawl/reports/crawl-report.txt?heritrixVersion=1.12.1b&harvestid=130&jobid=1121 130.225.27.140 20090601153639 text/plain 318
Crawl Name: forsider_plus_2_niveauer
Crawl Status: Finished
Duration Time: 19s600ms
Total Seeds Crawled: 1
Total Seeds not Crawled: 0
Total Hosts Crawled: 8
Total Documents Crawled: 51
Processed docs/sec: 2.68
Bandwidth in Kbytes/sec: 124
Total Raw Data Size in Bytes: 2421158 (2.3 MB) 
Novel Bytes: 2421158 (2.3 MB) 

metadata://netarkivet.dk/crawl/reports/frontier-report.txt?heritrixVersion=1.12.1b&harvestid=130&jobid=1121 130.225.27.140 20090601153639 text/plain 15
frontier empty

metadata://netarkivet.dk/crawl/reports/hosts-report.txt?heritrixVersion=1.12.1b&harvestid=130&jobid=1121 130.225.27.140 20090601153639 text/plain 246
[#urls] [#bytes] [host]
15 460753 www.adobe.com
12 17809 www.aros.dk
8 910 dns:
5 10567 wwwimages.adobe.com
3 43679 get.adobe.com
2 483 download.macromedia.com
2 1884487 fpdownload2.macromedia.com
2 1322 stats.adobe.com
2 1148 www.macromedia.com

metadata://netarkivet.dk/crawl/reports/mimetype-report.txt?heritrixVersion=1.12.1b&harvestid=130&jobid=1121 130.225.27.140 20090601153639 text/plain 258
[#urls] [#bytes] [mime-types]
18 84668 text/html
8 351579 application/x-javascript
8 910 text/dns
6 3489 text/plain
3 82453 text/css
2 3105 image/gif
2 3875 image/png
1 1884214 application/x-cab-compressed
1 5238 image/jpeg
1 1415 image/x-icon
1 212 no-type

metadata://netarkivet.dk/crawl/reports/processors-report.txt?heritrixVersion=1.12.1b&harvestid=130&jobid=1121 130.225.27.140 20090601153639 text/plain 4148
Processors report - 200906011536
  Job being crawled:    forsider_plus_2_niveauer
  Number of Processors: 29
  NOTE: Some processors may not return a report!

Processor: org.archive.crawler.fetcher.FetchHTTP
  Function:          Fetch HTTP URIs
  CrawlURIs handled: 43
  Recovery retries:   0

Processor: org.archive.crawler.extractor.ExtractorHTTP
  Function:          Extracts URIs from HTTP response headers
  CrawlURIs handled: 43
  Links extracted:   7

Processor: org.archive.crawler.extractor.ExtractorHTML
  Function:          Link extraction on HTML documents
  CrawlURIs handled: 8
  Links extracted:   293

Processor: org.archive.crawler.extractor.ExtractorCSS
  Function:          Link extraction on Cascading Style Sheets (.css)
  CrawlURIs handled: 3
  Links extracted:   166

Processor: org.archive.crawler.extractor.ExtractorJS
  Function:          Link extraction on JavaScript code
  CrawlURIs handled: 8
  Links extracted:   89

Processor: org.archive.crawler.extractor.ExtractorSWF
  Function:          Link extraction on Shockwave Flash documents (.swf)
  CrawlURIs handled: 0
  Links extracted:   0

Processor: org.archive.crawler.extractor.ExtractorImpliedURI
  Function:          Extracts links inside other URIs
  CrawlURIs handled: 51
  Links extracted:   0

Processor: org.archive.crawler.extractor.ExtractorImpliedURI
  Function:          Extracts links inside other URIs
  CrawlURIs handled: 51
  Links extracted:   0

Processor: org.archive.crawler.extractor.ExtractorImpliedURI
  Function:          Extracts links inside other URIs
  CrawlURIs handled: 51
  Links extracted:   0

Processor: org.archive.crawler.extractor.ExtractorImpliedURI
  Function:          Extracts links inside other URIs
  CrawlURIs handled: 51
  Links extracted:   0

Processor: org.archive.crawler.extractor.ExtractorImpliedURI
  Function:          Extracts links inside other URIs
  CrawlURIs handled: 51
  Links extracted:   0

Processor: org.archive.crawler.extractor.ExtractorImpliedURI
  Function:          Extracts links inside other URIs
  CrawlURIs handled: 51
  Links extracted:   0

Processor: org.archive.crawler.extractor.ExtractorImpliedURI
  Function:          Extracts links inside other URIs
  CrawlURIs handled: 51
  Links extracted:   0

Processor: org.archive.crawler.extractor.ExtractorImpliedURI
  Function:          Extracts links inside other URIs
  CrawlURIs handled: 51
  Links extracted:   0

Processor: org.archive.crawler.extractor.ExtractorImpliedURI
  Function:          Extracts links inside other URIs
  CrawlURIs handled: 51
  Links extracted:   0

Processor: org.archive.crawler.extractor.ExtractorImpliedURI
  Function:          Extracts links inside other URIs
  CrawlURIs handled: 51
  Links extracted:   0

Processor: org.archive.crawler.extractor.ExtractorImpliedURI
  Function:          Extracts links inside other URIs
  CrawlURIs handled: 51
  Links extracted:   0

Processor: org.archive.crawler.extractor.ExtractorImpliedURI
  Function:          Extracts links inside other URIs
  CrawlURIs handled: 51
  Links extracted:   0

Processor: org.archive.crawler.extractor.ExtractorImpliedURI
  Function:          Extracts links inside other URIs
  CrawlURIs handled: 51
  Links extracted:   0

Processor: is.hi.bok.digest.DeDuplicator
  Function:          Abort processing of duplicate records
                     - Lookup by url in use
  Total handled:     15
  Duplicates found:  15 100.0%
  Bytes total:       2249426 (2.1 MB)
  Bytes discarded:   2249426 (2.1 MB) 100.0%
  New (no hits):     0
  Exact hits:        15
  Equivalent hits:   0
  Timestamp predicts: (Where exact URL existed in the index)
  Change correctly:  0
  Change falsly:     0
  Non-change correct:8
  Non-change falsly: 0
  Missing timpestamp:7
  [Host] [total] [duplicates] [bytes] [bytes discarded] [new] [exact] [equiv] [change correct] [change falsly] [non-change correct] [non-change falsly] [no timestamp]
  www.aros.dk 1 1 8606 8606 0 1 0 0 0 1 0 0
  wwwimages.adobe.com 4 4 9758 9758 0 4 0 0 0 4 0 0
  www.adobe.com 9 9 346848 346848 0 9 0 0 0 2 0 7
  fpdownload2.macromedia.com 1 1 1884214 1884214 0 1 0 0 0 1 0 0


metadata://netarkivet.dk/crawl/reports/responsecode-report.txt?heritrixVersion=1.12.1b&harvestid=130&jobid=1121 130.225.27.140 20090601153639 text/plain 48
[rescode] [#urls]
200 26
404 11
1 8
301 3
302 3

metadata://netarkivet.dk/crawl/reports/seeds-report.txt?heritrixVersion=1.12.1b&harvestid=130&jobid=1121 130.225.27.140 20090601153639 text/plain 78
[code] [status] [seed] [redirect]
200 CRAWLED http://www.aros.dk/enteraction/

metadata://netarkivet.dk/crawl/logs/crawl.log?heritrixVersion=1.12.1b&harvestid=130&jobid=1121 130.225.27.140 20090601153639 text/plain 13586
2009-06-01T15:36:20.336Z     1         52 dns:www.aros.dk P http://www.aros.dk/enteraction/ text/dns #047 20090601153619868+93 sha1:5KHFF2YLG4ZEWCN5CIS73HXAVG6BWQRI - content-size:52
2009-06-01T15:36:20.747Z   200          0 http://www.aros.dk/robots.txt P http://www.aros.dk/enteraction/ text/plain #048 20090601153620665+76 sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ - content-size:267
2009-06-01T15:36:21.191Z   200       3269 http://www.aros.dk/enteraction/ - - text/html #047 20090601153621058+56 sha1:BXTYXJNRYJ7QFESWH3LYCXBF57C23VX4 - content-size:3602,3t
2009-06-01T15:36:21.558Z   404          0 http://www.aros.dk/enteraction/text/javascript X http://www.aros.dk/enteraction/ text/html #029 20090601153621507+47 sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ - content-size:283
2009-06-01T15:36:21.558Z     1         61 dns:download.macromedia.com XP http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab text/dns #048 20090601153621201+346 sha1:RSLLXPZR63WOMJLDJB3EAQZXGMRHZFD4 - content-size:61
2009-06-01T15:36:21.952Z   200       3269 http://www.aros.dk/enteraction/Default.html R http://www.aros.dk/enteraction/ text/html #050 20090601153621867+54 sha1:BXTYXJNRYJ7QFESWH3LYCXBF57C23VX4 - content-size:3539
2009-06-01T15:36:22.057Z   200         26 http://download.macromedia.com/robots.txt XP http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab text/plain #049 20090601153621935+119 sha1:MNSXZO35OCDMK2YM2TS4NGM3W2BWMSDI - content-size:271
2009-06-01T15:36:22.379Z   200       8321 http://www.aros.dk/enteraction/AC_RunActiveContent.js E http://www.aros.dk/enteraction/ application/x-javascript #029 20090601153622257+67 sha1:OUZ5LFEXKVOTUNOH6DQSKMCPAAGQAOO3 - duplicate:"1039-130-20090501153616-00000-sb-test-har-001.statsbiblioteket.dk.arc,9358",content-size:8606
2009-06-01T15:36:22.497Z   302          0 http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab X http://www.aros.dk/enteraction/ no-type #048 20090601153622384+106 sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ - content-size:212,3t
2009-06-01T15:36:22.739Z   404          0 http://www.aros.dk/enteraction/ShockwaveFlash.ShockwaveFlash.3 EX http://www.aros.dk/enteraction/AC_RunActiveContent.js text/html #017 20090601153622688+48 sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ - content-size:216
2009-06-01T15:36:23.107Z   404          0 http://www.aros.dk/enteraction/ShockwaveFlash.ShockwaveFlash.7 EX http://www.aros.dk/enteraction/AC_RunActiveContent.js text/html #049 20090601153623057+47 sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ - content-size:216
2009-06-01T15:36:23.260Z     1         61 dns:www.macromedia.com XP http://www.macromedia.com/go/getflashplayer text/dns #013 20090601153622826+425 sha1:2DVGQDR73VEYFHIE5OWWXO6G2BDGZMVP - content-size:61
2009-06-01T15:36:23.473Z   404          0 http://www.aros.dk/enteraction/ShockwaveFlash.ShockwaveFlash.6 EX http://www.aros.dk/enteraction/AC_RunActiveContent.js text/html #048 20090601153623417+53 sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ - content-size:216
2009-06-01T15:36:23.832Z   404          0 http://www.aros.dk/enteraction/ShockwaveFlash.ShockwaveFlash EX http://www.aros.dk/enteraction/AC_RunActiveContent.js text/html #040 20090601153623788+41 sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ - content-size:216
2009-06-01T15:36:24.107Z   301        239 http://www.macromedia.com/robots.txt XP http://www.macromedia.com/go/getflashplayer text/html #005 20090601153623711+386 sha1:5H5MF2CFFF4ENJS6I7MBTT2HG652XTKN - content-size:522
2009-06-01T15:36:24.191Z   404          0 http://www.aros.dk/enteraction/webtv/2.5 EX http://www.aros.dk/enteraction/AC_RunActiveContent.js text/html #013 20090601153624146+42 sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ - content-size:216
2009-06-01T15:36:24.343Z     1         56 dns:www.adobe.com XPRP http://www.adobe.com/robots.txt text/dns #047 20090601153624121+215 sha1:EAAUMEA5IHLYUKJARJOXA5UDLP6D4REI - content-size:56
2009-06-01T15:36:24.553Z   404          0 http://www.aros.dk/enteraction/application/x-shockwave-flash EX http://www.aros.dk/enteraction/AC_RunActiveContent.js text/html #017 20090601153624507+44 sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ - content-size:216
2009-06-01T15:36:24.768Z   200        724 http://www.adobe.com/robots.txt XPR http://www.macromedia.com/robots.txt text/plain #022 20090601153624656+110 sha1:VU5QIZ3TIJEOLJOMVNTO3CY2JAWD2J76 - content-size:1094,2t
2009-06-01T15:36:24.910Z   404          0 http://www.aros.dk/enteraction/webtv/2.6 EX http://www.aros.dk/enteraction/AC_RunActiveContent.js text/html #032 20090601153624867+41 sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ - content-size:216
2009-06-01T15:36:25.100Z   301        291 http://www.macromedia.com/go/getflashplayer X http://www.aros.dk/enteraction/ text/html #040 20090601153624507+584 sha1:7SLZKPNRQSPGM5TSPEL7RLSK7ZH2CBYE - content-size:626,3t
2009-06-01T15:36:25.205Z   301        360 http://www.adobe.com/shockwave/download/download.cgi?P1_Prod_Version=ShockwaveFlash XR http://www.macromedia.com/go/getflashplayer text/html #049 20090601153625103+95 sha1:KL3KTQ6ZG6KOUHLMSESYJ6BZFP27QFF7 - content-size:657
2009-06-01T15:36:25.961Z     1         56 dns:get.adobe.com XRRP http://get.adobe.com/flashplayer/ text/dns #022 20090601153625534+419 sha1:EGOBIHNKCT4VFCDQCDK2DQHEK7AYGFUB - content-size:56
2009-06-01T15:36:25.964Z     1         97 dns:fpdownload2.macromedia.com XRP http://fpdownload2.macromedia.com/get/shockwave/cabs/flash/swflash.cab text/dns #006 20090601153625705+249 sha1:CT4PX5FKBQ5VMTKE6DNRRHG3CYUR4RDA - content-size:97
2009-06-01T15:36:26.336Z   200         26 http://fpdownload2.macromedia.com/robots.txt XRP http://fpdownload2.macromedia.com/get/shockwave/cabs/flash/swflash.cab text/plain #048 20090601153626281+52 sha1:MNSXZO35OCDMK2YM2TS4NGM3W2BWMSDI - content-size:273
2009-06-01T15:36:26.750Z   404      19818 http://get.adobe.com/robots.txt XRRP http://get.adobe.com/flashplayer/ text/html #030 20090601153626400+156 sha1:LF54GQ2ITHSPXYDWTPGEPOYCRO2KF6A3 - content-size:20076
2009-06-01T15:36:27.193Z   302          0 http://get.adobe.com/flashplayer/ XRR http://www.adobe.com/shockwave/download/download.cgi?P1_Prod_Version=ShockwaveFlash text/html #005 20090601153627066+123 sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ - content-size:283,3t
2009-06-01T15:36:27.851Z   200    1883940 http://fpdownload2.macromedia.com/get/shockwave/cabs/flash/swflash.cab XR http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab application/x-cab-compressed #009 20090601153626658+1186 sha1:ZJQ2JJVYW7IWJ2UEMM3DM3NQNJ4URF2T - duplicate:"1039-130-20090501153616-00000-sb-test-har-001.statsbiblioteket.dk.arc,82164",content-size:1884214,3t
2009-06-01T15:36:27.851Z   200      22980 http://get.adobe.com/flashplayer/otherversions/ XRRR http://get.adobe.com/flashplayer/ text/html #030 20090601153627506+201 sha1:K2TXLGJUHR2ICP3YYAD4UDPIY7PPJLQ4 - content-size:23320
2009-06-01T15:36:28.363Z   404      29498 http://www.adobe.com/images/downloadcenter/ajax-loader.gif XRRRE http://get.adobe.com/flashplayer/otherversions/ text/html #005 20090601153628156+205 sha1:FI2ORUOFJIC3N7CUAK2YFYRA5I6XY2UC - content-size:29701
2009-06-01T15:36:29.026Z     1         99 dns:wwwimages.adobe.com XRRREP http://wwwimages.adobe.com/www.adobe.com/shockwave/download/images/flashplayer_100x100.jpg text/dns #030 20090601153628683+338 sha1:HRJSU2PEFYOZ7R5EG2ERAIT34CBIVXM6 - content-size:99
2009-06-01T15:36:29.778Z   200        507 http://wwwimages.adobe.com/robots.txt XRRREP http://wwwimages.adobe.com/www.adobe.com/shockwave/download/images/flashplayer_100x100.jpg text/plain #009 20090601153629379+397 sha1:VBZOQ7AYYC5IRCMPVZCG45JSRDQDMGIL - content-size:809
2009-06-01T15:36:30.230Z   200       4934 http://wwwimages.adobe.com/www.adobe.com/shockwave/download/images/flashplayer_100x100.jpg XRRRE http://get.adobe.com/flashplayer/otherversions/ image/jpeg #013 20090601153630186+41 sha1:HZF42BFUXPGVCQIYQAOFX7ZOSEES3XZC - duplicate:"1039-130-20090501153616-00000-sb-test-har-001.statsbiblioteket.dk.arc,2307731",content-size:5238,3t
2009-06-01T15:36:30.671Z   200      17076 http://www.adobe.com/lib/com.adobe/module/SearchBuddy.js XRRRE http://get.adobe.com/flashplayer/otherversions/ application/x-javascript #009 20090601153630535+124 sha1:KT6GMMU2L3FISEVKKUPSCI5OYSAMXEQN - duplicate:"1039-130-20090501153616-00000-sb-test-har-001.statsbiblioteket.dk.arc,2270061",content-size:17357
2009-06-01T15:36:31.331Z   200      62582 http://www.adobe.com/lib/com.adobe/template/screen.css XRRRE http://get.adobe.com/flashplayer/otherversions/ text/css #022 20090601153630976+243 sha1:DKBJZEGH5C6JYYB2AYIAHBB365ZINGHK - content-size:62845
2009-06-01T15:36:31.714Z   200       1995 http://www.adobe.com/ubi/globalnav/include/adobe-lq.png XRRRE http://get.adobe.com/flashplayer/otherversions/ image/png #009 20090601153631636+75 sha1:ACXJF73RQ47FG76YTS5KUR7MGK7X7FBZ - duplicate:"1039-130-20090501153616-00000-sb-test-har-001.statsbiblioteket.dk.arc,2168888",content-size:2367
2009-06-01T15:36:32.158Z   200      16418 http://www.adobe.com/js/downloadcenter/flashplayer.js XRRRE http://get.adobe.com/flashplayer/otherversions/ application/x-javascript #050 20090601153632026+123 sha1:47XGS6KD4NPIHF7HGUOLNHAERR7SDZYI - duplicate:"1094-130-20090522153621-00000-sb-test-har-001.statsbiblioteket.dk.arc,107494",content-size:16699
2009-06-01T15:36:32.555Z   200       4011 http://www.adobe.com/lib/com.adobe/template/gnavOverflowFix.js XRRRE http://get.adobe.com/flashplayer/otherversions/ application/x-javascript #009 20090601153632466+86 sha1:3REQAVY4PPSATXFA6G6USCP4XQUBGGO4 - duplicate:"1039-130-20090501153616-00000-sb-test-har-001.statsbiblioteket.dk.arc,2171355",content-size:4314
2009-06-01T15:36:32.910Z   200       1150 http://wwwimages.adobe.com/www.adobe.com/favicon.ico XRRRE http://get.adobe.com/flashplayer/otherversions/ image/x-icon #038 20090601153632866+42 sha1:GMW5NWOZ6D5MJLEUCH3JDIEURJMAEIIM - duplicate:"1039-130-20090501153616-00000-sb-test-har-001.statsbiblioteket.dk.arc,2175791",content-size:1415
2009-06-01T15:36:33.305Z   200       3701 http://www.adobe.com/lib/com.adobe/template/fixH1Size.js XRRRE http://get.adobe.com/flashplayer/otherversions/ application/x-javascript #013 20090601153633216+86 sha1:XI2PDSUDJ3DNAETALRWJ6B3PY6AIGC2Z - duplicate:"1039-130-20090501153616-00000-sb-test-har-001.statsbiblioteket.dk.arc,2163248",content-size:4004
2009-06-01T15:36:33.882Z   200      78178 http://www.adobe.com/lib/yui/_all_adc_yui.js XRRRE http://get.adobe.com/flashplayer/otherversions/ application/x-javascript #038 20090601153633616+231 sha1:VQGLBEVXNEA54GM7HCXYZHXRERAL3DL2 - duplicate:"1039-130-20090501153616-00000-sb-test-har-001.statsbiblioteket.dk.arc,2361369",content-size:78459
2009-06-01T15:36:34.386Z     1        428 dns:stats.adobe.com XRRREP http://stats.adobe.com/b/ss/mxmacromedia/1/G.7--NS/0 text/dns #047 20090601153634202+179 sha1:VOULPDXJIOL2HBVZNWEGJVAW2UYFVEFH - content-size:428
2009-06-01T15:36:35.096Z   404        395 http://stats.adobe.com/robots.txt XRRREP http://stats.adobe.com/b/ss/mxmacromedia/1/G.7--NS/0 text/html #007 20090601153634699+395 sha1:HPDS67UJCM7A27HGZO7HIV5PVJOAR4WA - content-size:547
2009-06-01T15:36:35.899Z   302          0 http://stats.adobe.com/b/ss/mxmacromedia/1/G.7--NS/0 XRRRE http://get.adobe.com/flashplayer/otherversions/ text/plain #009 20090601153635506+389 sha1:3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ - content-size:775,3t
2009-06-01T15:36:36.341Z   200       2460 http://wwwimages.adobe.com/www.adobe.com/images/globalnav/truste_seal_eu.gif XRRRE http://get.adobe.com/flashplayer/otherversions/ image/gif #013 20090601153636296+42 sha1:7U7QV5OA7H2AGEZSPGFIRD762IMYJ27E - duplicate:"1039-130-20090501153616-00000-sb-test-har-001.statsbiblioteket.dk.arc,2314714",content-size:2763
2009-06-01T15:36:36.757Z   200       1136 http://www.adobe.com/ubi/globalnav/include/adobe-hq.png XRRRE http://get.adobe.com/flashplayer/otherversions/ image/png #038 20090601153636646+109 sha1:QKGQATZXSBRS2JGPR2Y3O3ZLOPJAMZLD - duplicate:"1039-130-20090501153616-00000-sb-test-har-001.statsbiblioteket.dk.arc,2313106",content-size:1508
2009-06-01T15:36:37.258Z   200      25231 http://www.adobe.com/uber/js/omniture_s_code.js XRRRE http://get.adobe.com/flashplayer/otherversions/ application/x-javascript #006 20090601153637066+163 sha1:MUKMFCH25VSRW2BAEN4ZJVN6YFDKR22Z - duplicate:"1039-130-20090501153616-00000-sb-test-har-001.statsbiblioteket.dk.arc,2317599",content-size:25512
2009-06-01T15:36:37.643Z   200        807 http://www.adobe.com/css/downloadcenter/flashplayer.css XRRRE http://get.adobe.com/flashplayer/otherversions/ text/css #038 20090601153637566+76 sha1:L5PJWUY355VVNYKBV2RGWX23STLO3OJT - content-size:1098
2009-06-01T15:36:38.497Z   200     196347 http://www.adobe.com/lib/com.adobe/_all.js XRRRE http://get.adobe.com/flashplayer/otherversions/ application/x-javascript #018 20090601153637956+375 sha1:5OIXRW5BN2DAJUVLSOAOTU5SWTNC56BA - duplicate:"1039-130-20090501153616-00000-sb-test-har-001.statsbiblioteket.dk.arc,1966516",content-size:196628
2009-06-01T15:36:38.930Z   200         42 http://wwwimages.adobe.com/www.adobe.com/images/pixel.gif XRRRE http://get.adobe.com/flashplayer/otherversions/ image/gif #009 20090601153638886+42 sha1:GKHEOJZBVEZULAA62VJTEQHKYLI7QSMM - duplicate:"1039-130-20090501153616-00000-sb-test-har-001.statsbiblioteket.dk.arc,2168444",content-size:342
2009-06-01T15:36:39.373Z   200      18247 http://www.adobe.com/lib/com.adobe/template/print.css XRRRE http://get.adobe.com/flashplayer/otherversions/ text/css #049 20090601153639236+133 sha1:TVU3CBZF4MYL4OA4FRIPUSNHP3XFKYLB - content-size:18510

metadata://netarkivet.dk/crawl/logs/local-errors.log?heritrixVersion=1.12.1b&harvestid=130&jobid=1121 130.225.27.140 20090601153618 text/plain 0

metadata://netarkivet.dk/crawl/logs/progress-statistics.log?heritrixVersion=1.12.1b&harvestid=130&jobid=1121 130.225.27.140 20090601153639 text/plain 472
20090601153619 CRAWL RESUMED - Running
           timestamp  discovered      queued   downloaded       doc/s(avg)  KB/s(avg)   dl-failures   busy-thread   mem-use-KB  heap-size-KB   congestion   max-depth   avg-depth
20090601153639 CRAWL ENDING - Finished
2009-06-01T15:36:39Z          51           0           51       2.68(2.68)   124(124)             0             0        32226         42304            1           0           0
20090601153639 CRAWL ENDED - Finished

metadata://netarkivet.dk/crawl/logs/runtime-errors.log?heritrixVersion=1.12.1b&harvestid=130&jobid=1121 130.225.27.140 20090601153618 text/plain 0

metadata://netarkivet.dk/crawl/logs/uri-errors.log?heritrixVersion=1.12.1b&harvestid=130&jobid=1121 130.225.27.140 20090601153637 text/plain 430
2009-06-01T15:36:37.230Z http://www.adobe.com/uber/js/omniture_s_code.js "Unsupported scheme: javascript" javascript:,adobe,macromedia,dreamweaver,flash,shockwave,sdc,markme,sdc.shockwave,infopoll,developerlocator.macromedia,adobemax2007.com,photoshop.com,acrobat.com,../
2009-06-01T15:36:37.233Z http://www.adobe.com/uber/js/omniture_s_code.js "incorrect path" length;i++){n=a[i];m=m||(u==1?(n==v):(n.toLowerCase()==v.toLowerCas

metadata://netarkivet.dk/crawl/logs/heritrix.out?heritrixVersion=1.12.1b&harvestid=130&jobid=1121 130.225.27.140 20090601153640 text/plain 8647
The Heritrix process is started in the following environment
 (note that some entries will be changed by the starting JVM):
CLASSPATH=/home/netarkiv/PLIGT/lib/heritrix/lib/heritrix-1.12.1b.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/bsh-2.0b4.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/commons-cli-1.0.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/commons-codec-1.3.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/commons-collections-3.1.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/commons-httpclient-3.0.1.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/commons-lang-2.3.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/commons-logging-1.0.4.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/commons-pool-1.3.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/dnsjava-2.0.3.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/fastutil-5.0.3-heritrix-subset-1.0.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/itext-1.2.0.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/jasper-compiler-tomcat-4.1.30.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/jasper-runtime-tomcat-4.1.30.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/javaswf-CVS-SNAPSHOT-1.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/je-3.2.23.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/jericho-html-2.3.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/jets3t-0.5.0.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/jetty-4.2.23.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/junit-3.8.2.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/libidn-0.5.9.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/mg4j-1.0.1.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/poi-2.0-RC1-20031102.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/poi-scratchpad-2.0-RC1-20031102.jar:/home/netarkiv/PLIGT/lib/heritrix/lib/servlet-tomcat-4.1.30.jar:/home/netarkiv/PLIGT/lib/dk.netarkivet.harvester.jar:/home/netarkiv/PLIGT/lib/dk.netarkivet.archive.jar:/home/netarkiv/PLIGT/lib/dk.netarkivet.viewerproxy.jar:/home/netarkiv/PLIGT/lib/dk.netarkivet.monitor.jar:/home/netarkiv/PLIGT/lib/dk.netarkivet.harvester.jar:/home/netarkiv/PLIGT/lib/dk.netarkivet.archive.jar:/home/netarkiv/PLIGT/lib/dk.netarkivet.viewerproxy.jar:/home/netarkiv/PLIGT/lib/dk.netarkivet.monitor.jar
G_BROKEN_FILENAMES=1
HISTSIZE=1000
HOME=/home/netarkiv
HOSTNAME=sb-test-har-001
INPUTRC=/etc/inputrc
JAVA_HOME=/usr/java/jdk1.6.0_07
LANG=en_US.UTF-8
LD_LIBRARY_PATH=/usr/java/jdk1.6.0_07/jre/lib/i386/server:/usr/java/jdk1.6.0_07/jre/lib/i386:/usr/java/jdk1.6.0_07/jre/../lib/i386
LESSOPEN=|/usr/bin/lesspipe.sh %s
LOGNAME=netarkiv
LS_COLORS=
MAIL=/var/spool/mail/netarkiv
NLSPATH=/usr/dt/lib/nls/msg/%L/%N.cat
OLDPWD=/home/netarkiv/PLIGT
PATH=/usr/java/jdk1.6.0_07/bin:/usr/kerberos/bin:/usr/local/bin:/bin:/usr/bin:/usr/X11R6/bin
PWD=/home/netarkiv/PLIGT
SHELL=/bin/bash
SHLVL=4
SSH_CLIENT=130.226.231.15 33461 22
SSH_CONNECTION=130.226.231.15 33461 130.225.27.140 22
USER=netarkiv
XFILESEARCHPATH=/usr/dt/app-defaults/%L/Dt
_=/usr/java/jdk1.6.0_07/bin/java
Process properties:
dk.netarkivet.settings.file=/home/netarkiv/PLIGT/conf/settings_harvester_8082.xml
file.encoding=UTF-8
file.encoding.pkg=sun.io
file.separator=/
heritrix.version=1.12.1b
java.awt.graphicsenv=sun.awt.X11GraphicsEnvironment
java.awt.printerjob=sun.print.PSPrinterJob
java.class.path=/home/netarkiv/PLIGT/lib/dk.netarkivet.harvester.jar:/home/netarkiv/PLIGT/lib/dk.netarkivet.archive.jar:/home/netarkiv/PLIGT/lib/dk.netarkivet.viewerproxy.jar:/home/netarkiv/PLIGT/lib/dk.netarkivet.monitor.jar:/home/netarkiv/PLIGT/lib/dk.netarkivet.harvester.jar:/home/netarkiv/PLIGT/lib/dk.netarkivet.archive.jar:/home/netarkiv/PLIGT/lib/dk.netarkivet.viewerproxy.jar:/home/netarkiv/PLIGT/lib/dk.netarkivet.monitor.jar:
java.class.version=50.0
java.endorsed.dirs=/usr/java/jdk1.6.0_07/jre/lib/endorsed
java.ext.dirs=/usr/java/jdk1.6.0_07/jre/lib/ext:/usr/java/packages/lib/ext
java.home=/usr/java/jdk1.6.0_07/jre
java.io.tmpdir=/tmp
java.library.path=/usr/java/jdk1.6.0_07/jre/lib/i386/server:/usr/java/jdk1.6.0_07/jre/lib/i386:/usr/java/jdk1.6.0_07/jre/../lib/i386:/usr/java/packages/lib/i386:/lib:/usr/lib
java.runtime.name=Java(TM) SE Runtime Environment
java.runtime.version=1.6.0_07-b06
java.security.manager=
java.security.policy=/home/netarkiv/PLIGT/conf/security.policy
java.specification.name=Java Platform API Specification
java.specification.vendor=Sun Microsystems Inc.
java.specification.version=1.6
java.util.logging.config.file=/home/netarkiv/PLIGT/conf/log_harvestcontrollerapplication.prop
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
os.version=2.4.21-58.ELsmp
path.separator=:
settings.common.jmx.passwordFile=/home/netarkiv/PLIGT/conf/jmxremote.password
settings.common.jmx.port=8152
settings.common.jmx.rmiPort=8252
settings.harvester.harvesting.heritrix.guiPort=8092
settings.harvester.harvesting.heritrix.jmxPort=8093
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
user.dir=/home/netarkiv/PLIGT
user.home=/home/netarkiv
user.language=en
user.name=netarkiv
user.timezone=Europe/Copenhagen
Working directory: /home/netarkiv/PLIGT/harvester_8082/1121_1243870571640
15:36:17.398 EVENT  Starting Jetty/4.2.23
15:36:17.653 EVENT  Started WebApplicationContext[/,Heritrix Console]
15:36:17.759 EVENT  Started SocketListener on 0.0.0.0:8092
15:36:17.759 EVENT  Started org.mortbay.jetty.Server@14e3f41
06/01/2009 15:36:18 +0000 INFO org.archive.crawler.Heritrix postRegister org.archive.crawler:guiport=8092,host=sb-test-har-001.statsbiblioteket.dk,jmxport=8093,name=Heritrix,type=CrawlService registered to MBeanServerId=sb-test-har-001.statsbiblioteket.dk_1243870576944, SpecificationVersion=1.4, ImplementationVersion=1.6.0_07-b06, SpecificationVendor=Sun Microsystems
Heritrix version: 1.12.1b
06/01/2009 15:36:19 +0000 INFO org.archive.crawler.admin.CrawlJob postRegister org.archive.crawler:host=sb-test-har-001.statsbiblioteket.dk,jmxport=8093,mother=Heritrix,name=1121-130-20090601153618672,type=CrawlService.Job registered to MBeanServerId=sb-test-har-001.statsbiblioteket.dk_1243870576944, SpecificationVersion=1.4, ImplementationVersion=1.6.0_07-b06, SpecificationVendor=Sun Microsystems
06/01/2009 15:36:39 +0000 INFO org.archive.crawler.admin.CrawlJob postDeregister org.archive.crawler:host=sb-test-har-001.statsbiblioteket.dk,jmxport=8093,mother=Heritrix,name=1121-130-20090601153618672,type=CrawlService.Job unregistered from MBeanServerId=sb-test-har-001.statsbiblioteket.dk_1243870576944, SpecificationVersion=1.4, ImplementationVersion=1.6.0_07-b06, SpecificationVendor=Sun Microsystems
06/01/2009 15:36:40 +0000 INFO org.archive.crawler.Heritrix postDeregister org.archive.crawler:guiport=8092,host=sb-test-har-001.statsbiblioteket.dk,jmxport=8093,name=Heritrix,type=CrawlService unregistered from MBeanServerId=sb-test-har-001.statsbiblioteket.dk_1243870576944, SpecificationVersion=1.4, ImplementationVersion=1.6.0_07-b06, SpecificationVendor=Sun Microsystems
15:36:40.093 EVENT  Stopping Acceptor ServerSocket[addr=0.0.0.0/0.0.0.0,port=0,localport=8092]
15:36:40.094 EVENT  Stopped SocketListener on 0.0.0.0:8092
15:36:40.095 EVENT  Stopped WebApplicationContext[/,Heritrix Console]
15:36:40.095 EVENT  Stopped org.mortbay.http.NCSARequestLog@1f80c0e
15:36:40.095 EVENT  Stopped org.mortbay.jetty.Server@14e3f41
15:36:40.096 EVENT  Stopped WebApplicationContext[/,Heritrix Console]
15:36:40.096 EVENT  Stopped org.mortbay.jetty.Server@14e3f41

metadata://netarkivet.dk/crawl/index/cdx?majorversion=1&minorversion=0&harvestid=130&jobid=1121&timestamp=20090601153620&serialno=00000 130.225.27.140 20090601153645 application/x-cdx 6990
dns:www.aros.dk 130.225.24.33 20090601153619 text/dns 52 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 1477 000edcb7e40100aa8bfedf4e80c93bdb
http://www.aros.dk/robots.txt 89.233.1.209 20090601153620 text/plain 267 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 1587 794ac83b64f07235e4a900c69f74c53d
http://www.aros.dk/enteraction/ 89.233.1.209 20090601153621 text/html 3602 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 1928 e3283dc1b798f4ffd8042e599d9fbdc2
dns:download.macromedia.com 130.225.24.33 20090601153621 text/dns 61 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 5606 266781c0e94040d6119bd248e1775992
http://www.aros.dk/enteraction/text/javascript 89.233.1.209 20090601153621 text/html 283 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 5737 9dc486d2bac2fb32322ff638403a9bda
http://www.aros.dk/enteraction/Default.html 89.233.1.209 20090601153621 text/html 3539 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 6110 13849692e000e9fed7028e006e085963
http://download.macromedia.com/robots.txt 92.122.223.191 20090601153621 text/plain 271 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 9737 f4618947f48e1103ba99a231d7a4d0bd
http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab 92.122.223.191 20090601153622 no-type 212 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 10096 eea2e65f592c8eddc716a24cd62d4ca7
http://www.aros.dk/enteraction/ShockwaveFlash.ShockwaveFlash.3 89.233.1.209 20090601153622 text/html 216 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 10419 cde510365f4992d7a7651c73ef48e536
http://www.aros.dk/enteraction/ShockwaveFlash.ShockwaveFlash.7 89.233.1.209 20090601153623 text/html 216 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 10741 cde510365f4992d7a7651c73ef48e536
dns:www.macromedia.com 130.225.24.33 20090601153622 text/dns 61 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 11063 742bf7940e14287ed3ea8de50ce0697e
http://www.aros.dk/enteraction/ShockwaveFlash.ShockwaveFlash.6 89.233.1.209 20090601153623 text/html 216 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 11189 cde510365f4992d7a7651c73ef48e536
http://www.aros.dk/enteraction/ShockwaveFlash.ShockwaveFlash 89.233.1.209 20090601153623 text/html 216 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 11511 940379e4d4163c60b98a1b6adf04b9cf
http://www.macromedia.com/robots.txt 192.150.18.118 20090601153623 text/html 522 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 11831 82e5e678ae41461c38837cf8ba100936
http://www.aros.dk/enteraction/webtv/2.5 89.233.1.209 20090601153624 text/html 216 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 12435 940379e4d4163c60b98a1b6adf04b9cf
dns:www.adobe.com 130.225.24.33 20090601153624 text/dns 56 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 12735 a235d7c370e808aa53bf1f9c0da6b2cf
http://www.aros.dk/enteraction/application/x-shockwave-flash 89.233.1.209 20090601153624 text/html 216 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 12851 940379e4d4163c60b98a1b6adf04b9cf
http://www.adobe.com/robots.txt 192.150.8.60 20090601153624 text/plain 1094 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 13171 07dc6f452686c233e6f22757fc4521d4
http://www.aros.dk/enteraction/webtv/2.6 89.233.1.209 20090601153624 text/html 216 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 14342 97dbc6b50ef1332179acd36377fe8544
http://www.macromedia.com/go/getflashplayer 192.150.18.118 20090601153624 text/html 626 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 14642 823b37d675e92be2b4b63030dc84bb22
http://www.adobe.com/shockwave/download/download.cgi?P1_Prod_Version=ShockwaveFlash 192.150.8.60 20090601153625 text/html 657 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 15357 4a92e329dea9b1576499045f10806adf
dns:get.adobe.com 130.225.24.33 20090601153625 text/dns 56 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 16141 8a9183716d5d1728fe13e1da28ec92d9
dns:fpdownload2.macromedia.com 130.225.24.33 20090601153625 text/dns 97 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 16257 e06402bc982f8210257bade8db7e1cd2
http://fpdownload2.macromedia.com/robots.txt 92.123.64.59 20090601153626 text/plain 273 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 16427 64d0613b9b08d776081e763cb9e03b5b
http://get.adobe.com/robots.txt 192.150.8.45 20090601153626 text/html 20076 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 16789 b14a162728d162358fe00e98b803398e
http://get.adobe.com/flashplayer/ 192.150.8.45 20090601153627 text/html 283 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 36942 5900ef7105738eb9faa53f8d8be91123
http://get.adobe.com/flashplayer/otherversions/ 192.150.8.45 20090601153627 text/html 23320 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 37302 bcb633e47a5f265bbfd1a19257795df0
http://www.adobe.com/images/downloadcenter/ajax-loader.gif 192.150.8.60 20090601153628 text/html 29701 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 60715 e9e575093b50cddd45c3552a99bb0230
dns:wwwimages.adobe.com 130.225.24.33 20090601153628 text/dns 99 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 90520 ddb87c24fce330fb02bd771e68e803ed
http://wwwimages.adobe.com/robots.txt 92.123.65.192 20090601153629 text/plain 809 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 90685 d82cf9efd974ecd50b7511c5195c055f
http://www.adobe.com/lib/com.adobe/template/screen.css 192.150.8.60 20090601153630 text/css 62845 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 91577 daf05063289a49d2ce98fff969245213
dns:stats.adobe.com 130.225.24.33 20090601153634 text/dns 428 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 154521 994db113584a130f69c326bad7fb0d70
http://stats.adobe.com/robots.txt 66.235.133.3 20090601153634 text/html 547 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 155012 a47858aba9588a986b487b8899ed3f50
http://stats.adobe.com/b/ss/mxmacromedia/1/G.7--NS/0 66.235.133.3 20090601153635 text/plain 775 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 155636 070b35bd3d117a452daf1b9811e0d9f3
http://www.adobe.com/css/downloadcenter/flashplayer.css 192.150.8.60 20090601153637 text/css 1098 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 156508 6fce3fce468946a4a0089f77bd15869b
http://www.adobe.com/lib/com.adobe/template/print.css 192.150.8.60 20090601153639 text/css 18510 1121-130-20090601153620-00000-sb-test-har-001.statsbiblioteket.dk.arc 157705 13c8969bb235218b4fe8891eec78e9c8


filedesc://3-metadata-4.arc.gz.open 0.0.0.0 20170125093039 text/plain 76
1 0 InternetArchive
URL IP-address Archive-date Content-type Archive-length

filedesc://3-metadata-1.arc.open 0.0.0.0 20170125093039 text/plain 0

metadata://netarkivet.dk/crawl/setup/duplicatereductionjobs?majorversion=1&minorversion=0&harvestid=1&harvestnum=3&jobid=3 130.225.27.140 20170125093039 text/plain 1
2
metadata://netarkivet.dk/crawl/setup/crawler-beans.cxml?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 61342
<?xml version="1.0" encoding="UTF-8"?>
<!-- HERITRIX 3 CRAWL JOB CONFIGURATION FILE - For use with NetarchiveSuite 5.1.0 -->
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xmlns:tx="http://www.springframework.org/schema/tx"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd
                           http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop-3.0.xsd
                           http://www.springframework.org/schema/tx http://www.springframework.org/schema/tx/spring-tx-3.0.xsd
                           http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context-3.0.xsd">

  <context:annotation-config/>

  <!-- OVERRIDES (START)
  Values elsewhere in the configuration may be replaced ('overridden')
  by a Properties map declared in a PropertiesOverrideConfigurer,
  using a dotted-bean-path to address individual bean properties.
  This allows us to collect a few of the most-often changed values
  in an easy-to-edit format here at the beginning of the model configuration.
  -->

  <!-- SIMPLE OVERRIDES (START)
  Overrides from a text property list
  -->
  <bean id="simpleOverrides" class="org.springframework.beans.factory.config.PropertyOverrideConfigurer">
    <property name="properties">
      <!-- Overrides the default values used by Heritrix -->
      <value>
        ## This Properties map is specified in the Java 'property list' text format
        ## http://java.sun.com/javase/6/docs/api/java/util/Properties.html#load%28java.io.Reader%29

        ###
        ### some of these overrides is actually just the default value, so they can be skipped
        ###

        ## (W)ARC Writer Metadata
        ###warcWriter.writeMetadata=true
        ###warcWriter.compress=true
      </value>
    </property>
  </bean>
  <!-- SIMPLE OVERRIDES (END) -->

  <!-- LONGER OVERRIDES (START)
  Overrides from declared <prop> elements, more easily allowing
  multiline values or even declared beans
  -->
  <bean id="longerOverrides" class="org.springframework.beans.factory.config.PropertyOverrideConfigurer">
    <property name="properties">
      <props>
      </props>
    </property>
  </bean>
  <!-- LONGER OVERRIDES (END) -->
  <!-- OVERRIDES (END) -->

  <!-- CRAWL METADATA (START)
  Including identification of crawler/operator
  using NetarchiveSuites own extended version of the org.archive.modules.CrawlMetadata
  -->
  <bean id="metadata" class="dk.netarkivet.harvester.harvesting.NasCrawlMetadata" autowire="byName">
    <!-- Job name use string value -->
    <property name="jobName" value="default_orderxml" />
    <!-- Description use string value -->
    <property name="description" value="Default Profile xyz" />
    <!-- User agent template use string value -->
    <property name="userAgentTemplate" value="Mozilla/5.0 (compatible; heritrix/3.3.0 +@OPERATOR_CONTACT_URL@)" />
    <!-- Operator name use string value -->
    <property name="operator" value="Admin" />
    <!-- Operator from use string value -->
    <property name="operatorFrom" value="info@netarkivet.dk" />
    <!-- Operator contact URL use string value -->
    <property name="operatorContactUrl" value="http://netarkivet.dk/webcrawler/" />
    <!-- Organization name use string value -->
    <property name="organization" value="Netarkivet" />
    <!-- Robot.txt policy use string value (one of: ignore, obey, custom) -->
    <property name="robotsPolicyName" value="ignore" />
    <!-- Audience of the sheet use string value -->
    <property name="audience" value="" />
    <!-- This field is not available in the CrawlMetadata class bundled with heritrix, so we extended the class to add this field -->
    <property name="date" value="20160802" />
  </bean>
  <!-- CRAWL METADATA (END) -->

  <!-- SEEDS (START)
  Crawl starting points
  -->
  <bean id="seeds" class="org.archive.modules.seeds.TextSeedModule">
    <property name="textSource">
      <bean class="org.archive.spring.ConfigFile">
        <!-- ConfigFile approach: specifying external seeds.txt file -->
        <property name="path" value="seeds.txt" />
      </bean>
    </property>
    <!-- No source-report.txt if this is false -->
    <property name="sourceTagSeeds" value="true" />
  </bean>
  <!-- SEEDS (END) -->

  <!-- SCOPE (START)
  Rules for which discovered URIs to crawl; order is very
  important because last decision returned other than 'NONE' wins.
  -->
  <bean id="scope" class="org.archive.modules.deciderules.DecideRuleSequence">
    <!-- Only set to true for test purposes -->
    <property name="logToFile" value="true" />
    <!-- Only set to true for test purposes -->
    <property name="logExtraInfo" value="true" />
    <property name="rules">
      <list>
        <!-- Begin by REJECTing all... -->
        <bean class="org.archive.modules.deciderules.RejectDecideRule">
        </bean>
        <!-- ...then ACCEPT those within configured/seed-implied SURT prefixes... -->
        <bean class="dk.netarkivet.harvester.harvesting.NASSurtPrefixedDecideRule">
          <property name="seedsAsSurtPrefixes" value="true" />
          <property name="alsoCheckVia" value="false" />
          <property name="surtsDumpFile" value="surts.dump" />
          <!-- NASSurtPrefixedDecideRule properties only -->
          <property name="removeW3xSubDomain" value="true" />
          <property name="addBeforeRemovingW3xSubDomain" value="true" />
          <property name="addW3SubDomain" value="true" />
          <property name="addBeforeAddingW3SubDomain" value="true" />
          <property name="allowSubDomainsRewrite" value="true" />
        </bean>
        <!-- ...but REJECT those more than a configured link-hop-count from start... -->
        <bean class="org.archive.modules.deciderules.TooManyHopsDecideRule">
          <!-- Max number of (L) and (R) in discovery path -->
          <property name="maxHops" value="20" />
        </bean>
        <!-- ...but ACCEPT those more than a configured link-hop-count from start... -->
        <bean class="org.archive.modules.deciderules.TransclusionDecideRule">
          <property name="maxTransHops" value="3" />
          <property name="maxSpeculativeHops" value="0" />
        </bean>
        <!-- ...but REJECT those from a configurable (initially empty) set of REJECT SURTs... -->
        <bean class="org.archive.modules.deciderules.surt.SurtPrefixedDecideRule">
          <!-- Decision value (ACCEPT, REJECT, NONE) -->
          <property name="decision" value="REJECT" />
          <property name="seedsAsSurtPrefixes" value="false" />
          <property name="surtsDumpFile" value="negative-surts.dump" />
        </bean>
        <!-- ...and REJECT those from a configurable (initially empty) set of URI regexes... -->
        <bean class="org.archive.modules.deciderules.MatchesListRegexDecideRule">
          <property name="decision" value="REJECT" />
          <property name="listLogicalOr" value="true" />
          <property name="regexList">
            <list>
              <!-- IA STANDARD GLOBAL CRAWLTRAP FILTERS (START) -->
              <value>.*core\.UserAdmin.*core\.UserLogin.*</value>
              <value>.*core\.UserAdmin.*register\.UserSelfRegistration.*</value>
              <value>.*\/w\/index\.php\?title=Speci[ae]l:Recentchanges.*</value>
              <value>.*act=calendar&amp;cal_id=.*</value>
              <value>.*advCalendar_pi.*</value>
              <value>.*cal\.asp\?date=.*</value>
              <value>.*cal\.asp\?view=monthly&amp;date=.*</value>
              <value>.*cal\.asp\?view=weekly&amp;date=.*</value>
              <value>.*cal\.asp\?view=yearly&amp;date=.*</value>
              <value>.*cal\.asp\?view=yearly&amp;year=.*</value>
              <value>.*cal\/cal_day\.php\?op=day&amp;date=.*</value>
              <value>.*cal\/cal_week\.php\?op=week&amp;date=.*</value>
              <value>.*cal\/calendar\.php\?op=cal&amp;month=.*</value>
              <value>.*cal\/yearcal\.php\?op=yearcal&amp;ycyear=.*</value>
              <value>.*calendar\.asp\?calmonth=.*</value>
              <value>.*calendar\.asp\?qMonth=.*</value>
              <value>.*calendar\.php\?sid=.*</value>
              <value>.*calendar\.php\?start=.*</value>
              <value>.*calendar\.php\?Y=.*</value>
              <value>.*calendar\/\?CLmDemo_horizontal=.*</value>
              <value>.*calendar_menu\/calendar\.php\?.*</value>
              <value>.*calendar_scheduler\.php\?d=.*</value>
              <value>.*calendar_year\.asp\?qYear=.*</value>
              <value>.*calendarix\/calendar\.php\?op=.*</value>
              <value>.*calendarix\/yearcal\.php\?op=.*</value>
              <value>.*calender\/default\.asp\?month=.*</value>
              <value>.*Default\.asp\?month=.*</value>
              <value>.*events\.asp\?cat=0&amp;mDate=.*</value>
              <value>.*events\.asp\?cat=1&amp;mDate=.*</value>
              <value>.*events\.asp\?MONTH=.*</value>
              <value>.*events\.asp\?month=.*</value>
              <value>.*index\.php\?iDate=.*</value>
              <value>.*index\.php\?module=PostCalendar&amp;func=view.*</value>
              <value>.*index\.php\?option=com_events&amp;task=view.*</value>
              <value>.*index\.php\?option=com_events&amp;task=view_day&amp;year=.*</value>
              <value>.*index\.php\?option=com_events&amp;task=view_detail&amp;year=.*</value>
              <value>.*index\.php\?option=com_events&amp;task=view_month&amp;year=.*</value>
              <value>.*index\.php\?option=com_events&amp;task=view_week&amp;year=.*</value>
              <value>.*index\.php\?option=com_events&amp;task=view_year&amp;year=.*</value>
              <value>.*index\.php\?option=com_extcalendar&amp;Itemid.*</value>
              <value>.*modules\.php\?name=Calendar&amp;op=modload&amp;file=index.*</value>
              <value>.*modules\.php\?name=vwar&amp;file=calendar&amp;action=list&amp;month=.*</value>
              <value>.*modules\.php\?name=vwar&amp;file=calendar.*</value>
              <value>.*modules\.php\?name=vWar&amp;mod=calendar.*</value>
              <value>.*modules\/piCal\/index\.php\?caldate=.*</value>
              <value>.*modules\/piCal\/index\.php\?cid=.*</value>
              <value>.*option,com_events\/task,view_day\/year.*</value>
              <value>.*option,com_events\/task,view_month\/year.*</value>
              <value>.*option,com_extcalendar\/Itemid.*</value>
              <value>.*task,view_month\/year.*</value>
              <value>.*shopping_cart\.php.*</value>
              <value>.*action.add_product.*</value>
              <value>.*action.remove_product.*</value>
              <value>.*action.buy_now.*</value>
              <value>.*checkout_payment\.php.*</value>
              <value>.*login.*login.*login.*login.*</value>
              <value>.*homepage_calendar\.asp.*</value>
              <value>.*MediaWiki.*Movearticle.*</value>
              <value>.*index\.php.*action=edit.*</value>
              <value>.*comcast\.net.*othastar.*</value>
              <value>.*Login.*Login.*Login.*</value>
              <value>.*redir.*redir.*redir.*</value>
              <value>.*bookingsystemtime\.asp\?dato=.*</value>
              <value>.*bookingsystem\.asp\?date=.*</value>
              <value>.*cart\.asp\?mode=add.*</value>
              <value>.*\/photo.*\/photo.*\/photo.*</value>
              <value>.*\/skins.*\/skins.*\/skins.*</value>
              <value>.*\/scripts.*\/scripts.*\/scripts.*</value>
              <value>.*\/styles.*\/styles.*\/styles.*</value>
              <value>.*\/coppermine\/login\.php\?referer=.*</value>
              <value>.*\/images.*\/images.*\/images.*</value>
              <value>.*\/stories.*\/stories.*\/stories.*</value>
              <!-- IA STANDARD GLOBAL CRAWLTRAP FILTERS (END) -->
              <!-- NETARCHIVESUITE GLOBAL CRAWLTRAP FILTERS (START)
              Here we inject our global crawlertraps, domain specific crawlertraps -->
              
              <!-- NETARCHIVESUITE GLOBAL CRAWLTRAP FILTERS (END) -->
              <!-- NETARCHIVESUITE GLOBAL CRAWLTRAP FILTERS (START) -->
              <value>.*\/(Microsoft|Msxml2)\.(XMLHTTP|XMLDOM)$</value>
              <value>.*\/(text|application)\/[a-zA-Z0-9_-[\.]]+$.*</value>
              <value>.*\/audio\/(aac|aiff|basic|flv|it|make|make\.my\.funk|m4a|mid|midi|mod|mp3|mp4|mpeg|mpeg3|nspaudio|ogg|s3m|tsp-audio|tsplayer|vnd\.qcelp|voc|voxware|wav|wave|webm|wma|youtube)$.*</value>
              <value>.*\/audio/x-(adpcm|aiff|au|flv|gsm|jam|liveaudio|xm|mid|midi|mod|mp3|mp4a|mpeg|mpeg-3|mpequrl|ms-wma|nspaudio|pn-realaudio|pn-realaudio-plugin|psid|realaudio|twinvq|twinvq-plugin|vimeo|vnd\.audioexplosion\.mjuicemediafile|voc|wav|webm|youtube)$.*</value>
              <value>.*\/chemical\/x-pdb$.*</value>
              <value>.*\/drawing\/x-dwf$.*</value>
              <value>.*\/image\/(bmp|cmu-raster|fif|florian|g3fax|gif|ief|jpeg|jutvision|naplps|pict|pjpeg|png|tiff|vasa|vnd\.(dwg|fpx|net-fpx|rn-realflash|rn-realpix|wap\.wbmp|xiff)|xbm|xpm)$.*</value>
              <value>.*\/image\/x-(cmu-raster|dwg|icon|jg|jps|niff|pcx|pict|portable-anymap|portable-bitmap|portable-graymap|portable-greymap|portable-pixmap|quicktime|rgb|tiff|windows-bmp|xbitmap|xbm|xpixmap|xwd|xwindowdump)$.*</value>
              <value>.*\/i-world\/i-vrml$.*</value>
              <value>.*\/message\/rfc822$.*</value>
              <value>.*\/model\/(iges|vnd\.dwf|vrml|x-pov)$.*</value>
              <value>.*\/multipart\/x-(gzip|ustar|zip)$.*</value>
              <value>.*\/music\/(crescendo|x-karaoke)$.*</value>
              <value>.*\/paleovu\/x-pv$.*</value>
              <value>.*\/video\/x-(amt-demorun|amt-showrun|atomic3d-feature|dl|dv|fli|flv|gl|isvideo|motion-jpeg|mpeg|mpeq2a|ms-asf|ms-asf-plugin|ms-wmv|msvideo|qtc|scm|sgi-movie)$.*</value>
              <value>.*\/video\/(animaflex|avi|avs-video|divx|dl|fli|gl|mp4|mpeg|msvideo|quicktime|vdo|video|vimeo|vivo|vnd\.rn-realvideo|vnd\.vivo|vosaic|webm|youtube)$.*</value>
              <value>.*\/windows\/metafile$.*</value>
              <value>.*\/www\/mime$.*</value>
              <value>.*\/x-conference\/x-cooltalk$.*</value>
              <value>.*\/xgl\/(drawing|movie)$.*</value>
              <value>.*\/x-music\/x-(midi|3dmf|svr|vrml|vrt)$.*</value>
              <value>.*https?:\/\/www\.visit\w+\.(com|cn|co\.uk|de|dk).*(\/search\/|global-map|im_field_product_category.*im_field_product_category|zoomin=.*zoomin=|all\/.*all\/|modules\/.*modules\/.*modules\/|google_analytics|contrib\/.*contrib\/|global\?keys=).*</value>
              <value>.*(adpark|antikguide|apppoint|artlinks|asias|auto356|babyudstyr24|barnedåbsgaver|bilisten|billige-møbler|billig-murer|bloggerwave|blomus|boligven|bond|booman|botiva|bowmore|bozoka|brugskunst-outlet|bukom|byggemarked24|cmspoint|crane|cykelshop24|design24|dit-supermarked|dyreartikler24|efab|efactory|efterskolepriser|el-installationen|fartgal|fastfood24|fenomen|find-bager|find-klip|find-murer|find-nummer|find-revisor|find-slagter|firmasjovtur|fliser24|fotorammer-online|frederikkes|frklivsstil|fynskferie|gave-butik|gaveland|gaver-gaveideer|gch|getgames|gods|grill24|guide24|habengut|helseguide|helse-shopping|herremand|herre-smykker|hostingpoint|hushave24|isenkramnet|juke|julegave24|kaliber|klippeklip|knager-online|landtransport|livezilla|livret|luckybastard|maler24|mathildes|mensgear|miamia|modesmykke|moosh|mortil3|mysales|netactive|noos|online-apotek|parfumeguide|parfume-shopping|printertoner24|psykolog24|restaurantoversigten|rowells|shopbot|shoppoint|sitelist|smukkesmykker|smykkegave|smykkegaver|smykker-deluxe|smykkerne|smykker-outlet|sportt|styleguide|super24|supercute|viabella|viacommerce|villadelux|vores-byg|vores-læge|vores-tandlæge|vvsworld)\.dk.*</value>
              <value>.*add-to-cart=.*</value>
              <value>.*[\/=\u0026_&amp;\-\?\%Ff][Ll]ogin.*</value>
              <value>.*\/\d{1,3}\.\d{1,3}(\.\d{1,3})?$</value>
              <value>.*\/\?\w+=\w+\;\w+\=\w+$</value>
              <value>.*\/gtm\.(js|start)$</value>
              <value>.*vimeo\.com.*\/(fallback\?noscript|format\:(detail|thumbnail))$</value>
              <value>.*\/u00\d(\d|[a-z])(\d|[a-z])+($|\/).*</value>
              <value>.*\/u00\d[a-z[\.]]+$</value>
              <value>.*\/[a-z0-9AGNOST_]+\._(set(Account|Allow(Anchor|Hash|Linker)|CustomVar|DomainName|Namespace|SampleRate|SiteSpeedSampleRate|Var)|track(Event|Page(LoadTime|view)|Trans))$.*</value>
              <value>.*https:\/\/[^d][^k]\.pinterest\.com.*</value>
              <value>.*(((year|week|day)\.listevents)|(month\.calendar)|(search\.form)).*</value>
              <value>.*twitter\.com.*(rss|logged|time.*\d\d:\d\d:\d\d).*</value>
              <value>.*\/kalender\/(20\d\d($|-(\d|W\d))|liste\/20|ical).*</value>
              <value>.*google\.com\/calendar\/(ical|feeds)\/.*</value>
              <value>.*visit\w+\.dk.*(\/search\/global\?keys=|all\/.*all\/|addthis.*google_analytics|contrib\/.*contrib\/).*</value>
              <value>.*forexticket.*</value>
              <value>.*http:\/\/www\.eznox\.com.*</value>
              <value>.*\/misc.*\/misc.*\/misc.*</value>
              <value>.*\/modules.*\/modules.*\/modules.*</value>
              <value>.*themes\/.*theme\/.*themes\/.*</value>
              <value>.*min-side.*min-side.*side.*</value>
              <value>.*\/\/.*\/\/.*\/\/.*</value>
              <value>.*\/public\/.*\/public\/.*\/public\/.*</value>
              <value>.*productSearch\?category=.*refinement=Pfunds.*sort=default_ranking.*start=[1-9].*</value>
              <value>.*http:\/\/.*\.zara\.com\/.*</value>
              <value>.*tequila.*(test|recipe)\/recipe\/\d\/\d.*</value>
              <value>.*\/earch\/.*\/earch\/.*</value>
              <value>.*tlg\.uci\.edu.*</value>
              <value>.*cart.*add.*</value>
              <value>.*(forum|wapb|mobil|valg)\.tv2\.no.*</value>
              <value>.*tv2\.no.*(CacheString=|ref=$).*</value>
              <value>.*tv2\.dk.*(spoergsmaal-fra-seerne|comments).*page.*page.*page.*</value>
              <value>.*(people|sina)\.com\.(cn|hk|tw).*</value>
              <value>.*ajprodukter\.se.*</value>
              <value>.*linkedin\.com\/(people|directory)\/.*</value>
              <value>.*thumbshots\.com.*url=[a-zA-Z0-9-]{1,}\.[a-z]{2,3}$.*</value>
              <value>.*css.*css.*css(\w|\/\w|\.).*</value>
              <value>.*func=post.*do=reply.*</value>
              <value>.*replytocom=.*</value>
              <value>.*messages\.php\?msg_send=.*</value>
              <value>.*add2wishlist.*</value>
              <value>.*add2Basket.*</value>
              <value>.*CartCmd=add.*Product.*</value>
              <value>.*toughroad\.dk.*(contact-me\/\w|toughroad\.dk|login|da\/kontakt|(1\.6\.2|6\.0\.65|XMLDOM|XMLHTTP|urlencoded|forward\/)$).*</value>
              <value>.*basket.*method=add.*</value>
              <value>.*order\/cart\/add\/.*</value>
              <value>.*ProductComparisonWizard.*</value>
              <value>.*sendlink.*</value>
              <value>.*forum.*(newthread|order=(asc|desc)|printthread|newreply|mode=(hybrid|threaded)).*</value>
              <value>.*blogger\.com.*(login|comment|signup|feeds|Login|post-edit|share-post-menu).*</value>
              <value>.*UserAdmin.*UserRecoverPassword.*</value>
              <value>.*sexcounter\.com.*</value>
              <value>.*tradedoubler\.com\/click\?a.*</value>
              <value>.*rate_item.*rating=.*</value>
              <value>.*life\.com.*in-gallery.*</value>
              <value>.*photobucket\.com.*</value>
              <value>.*ebay\.com.*</value>
              <value>.*bigfishgames\.com.*</value>
              <value>.*webmercs\.com.*Login.*</value>
              <value>.*(add.*product|AddProduct|AddToOrder|AddToBasket|addtocart|action=add|basket.*tilfoej|cart\.php.*add|command=add.*cart=|add_cart).*</value>
              <value>.*main\.php\?g2_view=core\.UserAdmin.*User.*</value>
              <value>.*facebook\.com.*((\.(11|0\.4))|\/J)$.*</value>
              <value>.*facebook\.com.*(\/\/.*\/\/|feeds\/page).*</value>
              <value>.*(af-za|ar-ar|az-az|be-by|bg-bg|bn-in|bs-ba|ca-es|cs-cz|cy-gb|de-de|el-gr|en-gb|eo-eo|es-es|es-la|et-ee|eu-es|fa-ir|fb-lt|fi-fi|fr-fr|fy-nl|ga-ie|gl-es|he-il|hi-in|hr-hr|hu-hu|hy-am|id-id|is-is|it-it|ja-jp|ka-ge|ko-kr|ku-tr|la-va|lt-lt|lv-lv|mk-mk|ml-in|ms-my|nb-no|ne-np|nl-nl|nn-no|pa-in|pl-pl|ps-af|pt-br|pt-pt|ro-ro|ru-ru|sk-sk|sl-si|sq-al|sr-rs|sv-se|sw-ke|ta-in|te-in|th-th|tl-ph|tr-tr|uk-ua|vi-vn|zh-cn|zh-hk|zh-tw)\.facebook\.com.*</value>
              <value>.*expectporn\.com.*</value>
              <value>.*domain-export\.com.*viewsimilar.*</value>
              <value>.*doubleclick\.net.*</value>
              <value>.*adsrv\.ads\.eniro\.com.*</value>
              <value>.*ecs-dk\.kelkoo\.dk.*(ts=\d{12,}|\/sitesearchGo).*</value>
              <value>.*forward302.*(google\.com|YahooRelatedLink).*</value>
              <value>.*youtube\.com.*(algorithm|results\?search_query=|feature=related|feeds.*alt=rss).*</value>
              <value>.*iloapp.*Mobile\?Mobile.*</value>
              <value>.*hangman1.*</value>
              <value>.*mailto.*</value>
              <value>.*linkven.*</value>
              <value>.*city-map\.(de|nl|pl|si|at).*</value>
              <value>.*ratepic.*rate=.*</value>
              <value>.*jcalpro.*date.*</value>
              <value>.*tx_cal_controller.*</value>
              <value>.*tx_calendar_pi1.*</value>
              <value>.*([Tt](ell|ip)|[sS]end|[Mm]ail|[Ff]riend).*([Ff]riend|[Vv]en|[Pp]age|[Ll]ink|[Ss]end|[Mm]ail|[Ss]ide|[Mm]obil|[Uu][Rr][Ll]).*</value>
              <value>.*(album=|displayimage).*lang=(albanian|arabic|basque|brazilian_portuguese|bulgarian|catalan|chinese_big5|chinese_gb|czech|dutch|english_gb|estonian|finnish|french|galician|georgian|german|german_sie|greek|hebrew|hindi|hungarian|indonesian|italian|japanese|korean|latvian|lithuanian|macedonian|norwegian|persian|polish|portuguese|romanian|russian|serbian|serbian_cy|slovak|slovenian|spanish|swedish|thai|turkish|ukrainian|vietnamese|welsh|xxx).*</value>
              <value>.*(c|C|K|k)alend(a|e)r.*(cal_controller|Date=|date=|Date|dato=|heute=|week=|month|maaned=|year|value=|day=|date(F|f)ield|displaymonth|displayweek|(c|C|k|K)alend(a|e)r).*</value>
              <value>.*(w|t)iki.*(feed=(rss|atom)|from=[0-9]{14,}|(l|L)og_|days=(1|3)|limit=(1|2|4|6|7|8|9)).*</value>
              <value>.*\/internet\/\?qs=.*</value>
              <value>.*\/internet\/expand\.aspx\?qs=.*</value>
              <value>.*\/parking\.php4\?ses=.*</value>
              <value>.*Recentchanges.*hide.*=.*</value>
              <value>.*[cC]al[Mm]onth=.*</value>
              <value>.*[Ii]ndex\.php\?[yY]=.*</value>
              <value>.*[Aa]ction.*(add_product|buy|=AddToBasket|=DayView|=display.*year=|=edit|=history|=MonthView|=WeekView|=remove_product|order).*</value>
              <value>.*\/coppermine\/login\.php\?referer=.*</value>
              <value>.*\/images.*\/images.*</value>
              <value>.*\/login\.php.*referer=login\.php.*</value>
              <value>.*\/login\.php\?referer=.*</value>
              <value>.*\/mustcheck\/\/error_msg\/\/page.*</value>
              <value>.*\/mustselect\/\/.*</value>
              <value>.*\/photo.*\/photo.*</value>
              <value>.*\/scripts.*\/scripts.*</value>
              <value>.*\/skiftsprog.*\/skiftsprog.*</value>
              <value>.*\/skins.*\/skins.*</value>
              <value>.*\/stories.*\/stories.*</value>
              <value>.*\/styles.*\/styles.*</value>
              <value>.*\/typo3conf\/.*\/typo3conf\/.*</value>
              <value>.*\?cmno=.*cyear=.*</value>
              <value>.*\?q=event.*month.*</value>
              <value>.*=mini_cal.*d=.*</value>
              <value>.*addthis\.com\/bookmark.*</value>
              <value>.*(addbasket|addtobasket|add_to_basket|add_to_cart=|return_from_cart=|addtowishlist).*</value>
              <value>.*adlog\.com\.com.*</value>
              <value>.*admin\/login\.html\?id=.*</value>
              <value>.*adstream_mjx\.ads.*click_nx\.ads.*</value>
              <value>.*aktivitetskalender.*id=.*</value>
              <value>.*album=.*pos=.*lang=.*</value>
              <value>.*album=favpics.*</value>
              <value>.*album=random.*cat=.*pos=.*</value>
              <value>.*album=topn.*cat=.*</value>
              <value>.*album=toprated.*cat=.*</value>
              <value>.*anbefal.*</value>
              <value>.*application\/Bricksite.*</value>
              <value>.*basket.*additem.*</value>
              <value>.*blogger\.com\/next-blog\?navBar=true.*</value>
              <value>.*book\/calendarPopup.*</value>
              <value>.*book\/priceCalendar.*</value>
              <value>.*book\/stbookingkalender\.php\?X=.*y=.*</value>
              <value>.*booking.*dato=.*</value>
              <value>.*bookingboks.*</value>
              <value>.*bookingsystem\.asp\?date=.*</value>
              <value>.*bookingsystemtime\.asp\?dato=.*</value>
              <value>.*Bricksite\/Modules.*</value>
              <value>.*Bricksite\/Pages\/Welcome\/Bricksite.*</value>
              <value>.*Bricksite\/Systemfiles.*</value>
              <value>.*cal.*date=.*</value>
              <value>.*cal_controller\[getdate].*</value>
              <value>.*cal_controller\[view].*</value>
              <value>.*cal_print\.php\?month.*</value>
              <value>.*cal=.*getdate=.*</value>
              <value>.*cal=month.*view=.*</value>
              <value>.*CalDate=.*</value>
              <value>.*calendar.*cal_id=.*</value>
              <value>.*calendar.*m=.*</value>
              <value>.*calendar.*Y=.*</value>
              <value>.*Calendar\.asp\?Time.*</value>
              <value>.*calendar\.aspx.*</value>
              <value>.*calendar\.google\.com.*</value>
              <value>.*calendar\.php\?.*</value>
              <value>.*calendar\/embed.*</value>
              <value>.*calendar_menu\/event.*</value>
              <value>.*calendarix_extended.*</value>
              <value>.*calender\.php.*year=.*</value>
              <value>.*calender\/\?m=.*</value>
              <value>.*cart\.asp\?mode=add.*</value>
              <value>.*catalog\/product_compare.*</value>
              <value>.*checkinCalendar.*</value>
              <value>.*checkout\/cart.*</value>
              <value>.*click_nx\.ads.*adstream_mjx\.ads.*</value>
              <value>.*com_dwod.*</value>
              <value>.*com_events.*view_month.*</value>
              <value>.*com_extcalendar.*Itemid=.*</value>
              <value>.*comcast\.net.*othastar.*</value>
              <value>.*component.*(((year|week|day)\.listevents)|(month\.calendar)|(search\.form))\/20[0-9]{2,}\/(0[1-9]{1,}|1[0-2]{1,})\/((0[1-9]{1,})|([1-2]{1,}[0-9]{1,})|(3[0-1]{1,}))\/.*</value>
              <value>.*courseBookingCalendar.*</value>
              <value>.*curid=.*diff=.*oldid=.*</value>
              <value>.*CustomDWCart\.asp.*</value>
              <value>.*Daily.*caldate=.*</value>
              <value>.*date.*extmode.*</value>
              <value>.*Date_From.*</value>
              <value>.*Date_To.*</value>
              <value>.*DatePicker\/Bricksite.*</value>
              <value>.*default\.asp\?id=.*date=.*</value>
              <value>.*default\.aspx\?.*year=.*</value>
              <value>.*displayimage\.php.*album=.*lang=.*</value>
              <value>.*displayimage\.php.*slideshow=.*</value>
              <value>.*dwodp_live.*</value>
              <value>.*e107_plugins\/calendar_menu\/event\.php\?.*</value>
              <value>.*easycalendar\/index\.php\?PageSection=.*</value>
              <value>.*Edit.*Page=.*</value>
              <value>.*Edit\.aspx.*</value>
              <value>.*event.*month\/all.*</value>
              <value>.*EventMonth.*EventCalendar.*</value>
              <value>.*events-calendar.*</value>
              <value>.*extmode.*date.*</value>
              <value>.*fbconnect_postThis.*</value>
              <value>.*fileadmin.*fileadmin.*</value>
              <value>.*flickr\.com.*(format=rss_200|format=atom|intl=us|[a-z0-9]{10,}\/|start_index=|=slideshow)$.*</value>
              <value>.*g2_view=search\.SearchScan.*g2.*</value>
              <value>.*galleri\/login\.php.*</value>
              <value>.*gallery2\/main\.php\?g2_view=cart\.ViewCart.*g2_navId=.*</value>
              <value>.*google\.com\/calendar.*</value>
              <value>.*google\.com\/calendar\/embed.*</value>
              <value>.*grafMM1=.*grafYY1=.*</value>
              <value>.*group\.calendar.*</value>
              <value>.*hangman\.php\?letters=.*</value>
              <value>.*home\.php\?date=.*</value>
              <value>.*homepage_calendar\.asp.*</value>
              <value>.*HotelSearchResults.*</value>
              <value>.*id=dag.*tx_calendar_pi1.*</value>
              <value>.*id=maaned.*tx_calendar_pi1.*</value>
              <value>.*id=uge.*tx_calendar_pi1.*</value>
              <value>.*index\.lasso\?d=.*</value>
              <value>.*Index\.php.*date=.*</value>
              <value>.*index\.php.*maaned=.*</value>
              <value>.*index\.php\?Booking.*Y=.*</value>
              <value>.*index\.php\?date=.*</value>
              <value>.*index\.php\?id=.*month.*</value>
              <value>.*index\.php\?Kalender.*Y=.*</value>
              <value>.*index\.php\?m=.*</value>
              <value>.*index\.php\?month=.*</value>
              <value>.*index\.php\?option=com_extcalendar.*Itemid=.*</value>
              <value>.*index\.php\?option=com_gcalendar.*</value>
              <value>.*index\.php\?option=com_jcalpro.*Itemid.*</value>
              <value>.*index_html\?mon=.*</value>
              <value>.*input_calendar.*days.*</value>
              <value>.*Javascript\/Bricksite\/Systemfiles.*</value>
              <value>.*javascripts\/javascripts.*</value>
              <value>.*jcalpro.*extmode=cal.*</value>
              <value>.*kalender\.asp.*d=.*</value>
              <value>.*kalender\.asp\?md=.*</value>
              <value>.*KALENDER\/DDCevents.*</value>
              <value>.*kalender\/minical.*</value>
              <value>.*kalender-dag-visning.*</value>
              <value>.*kalender-maaneds-visning.*</value>
              <value>.*kalenderoffentliginclude.*</value>
              <value>.*lang=.*lang=.*</value>
              <value>.*left\.asp\?date=.*</value>
              <value>.*limit=.*date=.*</value>
              <value>.*linkator\.php\?date=.*</value>
              <value>.*List.*caldate=.*</value>
              <value>.*lizearle\.com.*</value>
              <value>.*Login.*Login.*</value>
              <value>.*main\.php\?mo=.*</value>
              <value>.*maned\?month.*</value>
              <value>.*maxchars\/\/minchars\/\/mustfill.*</value>
              <value>.*md=.*aar=.*</value>
              <value>.*mdr=.*aar=.*</value>
              <value>.*MediaWiki.*Movearticle.*</value>
              <value>.*mod\.calendar.*</value>
              <value>.*module=crpCalendar.*func=.*</value>
              <value>.*module=Kalendern.*func=view.*</value>
              <value>.*modules\/piCal\/index\.php\?caldate=.*</value>
              <value>.*month.*cHash=.*</value>
              <value>.*Mozilla\/Mozilla.*</value>
              <value>.*Mozilla\/text\/text.*</value>
              <value>.*maaned=.*aar=.*</value>
              <value>.*nbjmup.*typo3conf.*</value>
              <value>.*nbjmup.*fileadmin.*templates.*css.*</value>
              <value>.*nbjmup.*fileadmin.*user_upload.*</value>
              <value>.*nbjmup.*nbjmup.*</value>
              <value>.*nbjmup.*typo3temp.*</value>
              <value>.*nbjmup\+.*</value>
              <value>.*news\.php\?y=.*</value>
              <value>.*next.*nextmonth.*</value>
              <value>.*Next_Day.*</value>
              <value>.*Next_Month.*</value>
              <value>.*Next_Week.*</value>
              <value>.*opac\/soegeresultat\?query=.*</value>
              <value>.*opendocument.*monthshown.*</value>
              <value>.*option=com_gcalendar.*Itemid=.*</value>
              <value>.*pagelayout\/compiledmenu\/.*</value>
              <value>.*parking\.php\?ses=.*</value>
              <value>.*pg=event_handling.*id=.*</value>
              <value>.*photogallery\/login\.php.*</value>
              <value>.*portal\.php\?month=.*</value>
              <value>.*posting.*mode=newtopic.*</value>
              <value>.*posting.*mode=quote.*</value>
              <value>.*posting.*mode=reply.*</value>
              <value>.*PostSchedule.*view=month.*</value>
              <value>.*Previous_Day.*</value>
              <value>.*Previous_Month.*</value>
              <value>.*PreviousSearchId.*</value>
              <value>.*print_programStadium.*</value>
              <value>.*print_team[iI]nfo.*</value>
              <value>.*printable=yes.*</value>
              <value>.*printLink=true.*</value>
              <value>.*product_compare.*</value>
              <value>.*product_reviews_write.*</value>
              <value>.*product_reviews_write.*</value>
              <value>.*productalert\/add.*</value>
              <value>.*productid=.*cartcmd=add.*</value>
              <value>.*qs=06oENya.*</value>
              <value>.*recommend.*</value>
              <value>.*redir.*redir.*redir.*</value>
              <value>.*refreshCalendar.*</value>
              <value>.*ReturnUrl=\/password.*ReturnUrl=\/password.*</value>
              <value>.*search\.SearchScan.*g2_form.*</value>
              <value>.*Seneste.*ndringer.*hide.*</value>
              <value>.*shop.*orderby=.*date.*limit=.*</value>
              <value>.*Special:Recentchanges.*limit=.*</value>
              <value>.*Special:Userlogin.*</value>
              <value>.*startdate=.*enddate=.*</value>
              <value>.*static=tbkalender.*</value>
              <value>.*Systemfiles\/Bricksite\/Systemfiles.*</value>
              <value>.*tiki-lastchanges.*</value>
              <value>.*title=Speciel.*Seneste.*namespace=.*</value>
              <value>.*title=Speciel:Henvisningsliste.*</value>
              <value>.*title=Speciel:Hvad_linker_hertil.*</value>
              <value>.*title=Speciel:Loglister.*page=.*</value>
              <value>.*title=Speciel:Recentchanges.*</value>
              <value>.*title=Speciel:Search.*</value>
              <value>.*title=Speciel:Seneste.*</value>
              <value>.*true.*calendaraction.*</value>
              <value>.*type=basket.*shopid=.*</value>
              <value>.*typo3conf.*typo3conf.*</value>
              <value>.*typo3temp.*typo3temp.*</value>
              <value>.*vcal.*(day|week|year|month).*</value>
              <value>.*view_day.*</value>
              <value>.*view_month.*</value>
              <value>.*view_week.*</value>
              <value>.*view_year.*</value>
              <value>.*view=comment\.ShowAllComments.*g2_itemId=.*</value>
              <value>.*view=ecard\.SendEcard.*</value>
              <value>.*view=rss\.SimpleRender.*</value>
              <value>.*wishlist.*add.*</value>
              <value>.*www\.infokatalogas\.lt.*</value>
              <value>.*www\.www.*Bricksite.*</value>
              <value>.*www\.www\..*</value>
              <value>.*aar=.*maaned=.*dag=.*</value>
              <value>.*acs\.org.*</value>
              <value>.*acm\.org.*</value>
              <value>.*ams\.org.*</value>
              <value>.*ansinet\.org.*</value>
              <value>.*arjournals\.annualreviews\.org.*</value>
              <value>.*bepress\.com.*</value>
              <value>.*bioline\.org\.br.*</value>
              <value>.*biomedcentral\.com.*</value>
              <value>.*blackwell-synergy\.com.*</value>
              <value>.*census\.gov.*</value>
              <value>.*content\.karger\.com.*</value>
              <value>.*csa\.com.*</value>
              <value>.*current-reports\.com.*</value>
              <value>.*elibrary\.unm\.edu.*</value>
              <value>.*emeraldinsight\.com.*</value>
              <value>.*emis\.de.*</value>
              <value>.*extenza-eps\.com.*</value>
              <value>.*future-drugs\.com.*</value>
              <value>.*gateway\.ovid\.com.*</value>
              <value>.*gateway\.proquest\.com.*</value>
              <value>.*haworthpress\.com.*</value>
              <value>.*heinonline\.org.*</value>
              <value>.*home\.mdconsult\.com.*</value>
              <value>.*ias\.ac\.in.*</value>
              <value>.*ieee\.org.*</value>
              <value>.*ieeexplore\.ieee\.org.*</value>
              <value>.*ingenta\.com.*</value>
              <value>.*ingentaconnect\.com.*</value>
              <value>.*internurse\.com.*</value>
              <value>.*iop\.org.*</value>
              <value>.*ispub\.com.*</value>
              <value>.*journals\.cambridge\.org.*</value>
              <value>.*journals\.humanapress\.com.*</value>
              <value>.*journalsonline\.tandf\.co\.uk.*</value>
              <value>.*journals\.tubitak\.gov\.tr.*</value>
              <value>.*journals\.uchicago\.edu.*</value>
              <value>.*jstage\.jst\.go\.jp.*</value>
              <value>.*jstor\.org.*</value>
              <value>.*karger\.ch.*</value>
              <value>.*kluwerlawonline\.com.*</value>
              <value>.*leaonline\.com.*</value>
              <value>.*liebertonline\.com.*</value>
              <value>.*medind\.nic\.in.*</value>
              <value>.*metapress\.com.*</value>
              <value>.*mitpressjournals\.org.*</value>
              <value>.*muse\.jhu\.edu.*</value>
              <value>.*nature\.com.*</value>
              <value>.*news\.nnyln\.net.*</value>
              <value>.*new\.sourceoecd\.org.*</value>
              <value>.*numdam\.org.*</value>
              <value>.*ojps\.aip\.org.*</value>
              <value>.*online\.sagepub\.com.*</value>
              <value>.*portal\.acm\.org.*</value>
              <value>.*projecteuclid\.org.*</value>
              <value>.*pubmedcentral\.gov.*</value>
              <value>.*pubs\.acs\.org.*</value>
              <value>.*purl\.access\.gpo\.gov.*</value>
              <value>.*rsc\.org.*</value>
              <value>.*saber\.ula\.ve.*</value>
              <value>.*scielo\.br.*</value>
              <value>.*scielo\.cl.*</value>
              <value>.*scielo\.isciii\.es.*</value>
              <value>.*scielo-mx\.bvs\.br.*</value>
              <value>.*scielo\.org\.ve.*</value>
              <value>.*scielo\.sld\.cu.*</value>
              <value>.*sciencedirect\.com.*</value>
              <value>.*search\.ebscohost\.com.*</value>
              <value>.*search\.epnet\.com.*</value>
              <value>.*siam\.org.*</value>
              <value>.*springerlink\.com.*</value>
              <value>.*taylorandfrancis\.metapress\.com.*</value>
              <value>.*thieme-connect\.com.*</value>
              <value>.*worldscinet\.com.*</value>
              <value>.*www3\.interscience\.wiley\.com.*</value>
              <value>.*www-gdz\.sub\.uni-goettingen\.de.*</value>
              <value>.*tlg\.uci\.edu.*</value>
              <value>.*www\.hempel\.\w{2,3}\/product-list\/.*download.*lang=(ru-RU|fi-FI|fr-FR|nb-NO|es-ES|sv-SE).*</value>
              <value>.*\/u002F.*\/u002F.*</value>
              <value>.*linkedin\.com.*(login|\/(reg|uas)\/|linkedin\.com|AnonymousFramework).*</value>
              <value>.*licdn\.com\/scds\/.*</value>
              <value>.*wayf\.dk.*SSOService.*</value>
              <value>.*winzip\.com.*</value>
              <value>.*\/order\/cart\/.*</value>
              <value>.*basketContent.*</value>
              <value>.*\/account\/login\/.*</value>
              <value>.*\/tell-a-friend.*</value>
              <value>.*product\/.*(span\.basketlink|div\.minibasket).*</value>
              <!-- 4xstring: Match urls with 4 or more repetetive paths ('/xxx')
              e.g. http://www.olbutikken.dk/h/txt/xss/trackPageview/css/txt/holder/txt/txt/place/df.js -->
              <value>^[^?]*(/[^/]{3,}(?=/))[^?]*\1(?=/)[^?]*\1(?=/)[^?]*\1(?=/|$).*</value>
              <!-- 3xSet: Match urls with 3 or more repetetive sets ('/xxx/yyy')
              e.g. http://www.olbutikken.dk/txt/css/forall/txt/css/trackPageview/css/txt/holder/txt/txt/css/place/df.js -->
              <value>^[^\?]*(/[^/]+/[^/]+)[^\?]*\1(?=/)[^\?]*\1(?=/|$).*</value>
              <!-- Til at finde url’er der ender med domænenavn (evt. inkl. subdomæne), eks:
              http://rip.rap.mads.dk/indhold/folder/mads.dk
              http://rip.rap.mads.dk/indhold/folder/rip.rap.mads.dk-->
              <!-- DO NOT USE REMOVES VALID URLs
              <value>^https?://((?:[A-Za-z0-9-]+\.)*)([A-Za-z0-9-]+\.[A-Za-z]{2,})(?=/).*/\1?\2.*</value>-->
              <!-- Til at finde url’er der indeholder /x+.x+//x+.x+/x+.x+
              http://www.olbutikken.dk/browse/a._setAccount/a._trackPageview/b._setDomainName/1
              http://www.olbutikken.dk/browse/a._setAccount/b._setDomainName/a._trackPageview/1 -->
              <value>^https?://[^/]+.*(?:/[^\?\.\/]+\.[^\?\/]+){3,}.*</value>
              <value>.*(\/mm(?=/).*\/mm(?=/).*\/mm\/|\/dd(?=/).*\/dd(?=/).*\/dd\/|\/yyyy(?=/).*\/yyyy\/).*</value>
              <value>.*kelkoo\.com.*searchId=.*</value>
              <value>.*facebook\.com\/sharer\/sharer.*</value>
              <value>.*(my|user|auth\.|api\.|\_fe)login.*</value>
              <!-- NETARCHIVESUITE GLOBAL CRAWLTRAP FILTERS (END) -->
            </list>
          </property>
        </bean>
        <!-- ...and REJECT those with suspicious repeating path-segments... -->
        <bean class="org.archive.modules.deciderules.PathologicalPathDecideRule">
          <!-- Max number of identical path repetitions -->
          <property name="maxRepetitions" value="2" />
        </bean>
        <!-- ...and REJECT those with more than threshold number of path-segments... -->
        <bean class="org.archive.modules.deciderules.TooManyPathSegmentsDecideRule">
          <!-- Max number of (/) in URL not including the first (//) -->
          <property name="maxPathDepth" value="20" />
        </bean>
        <!-- ...but always ACCEPT those marked as prerequisites for another URI... -->
        <bean class="org.archive.modules.deciderules.PrerequisiteAcceptDecideRule">
        </bean>
        <!-- ...but always REJECT those with unsupported URI schemes. -->
        <bean class="org.archive.modules.deciderules.SchemeNotInSetDecideRule">
        </bean>
      </list>
    </property>
  </bean>
  <!-- SCOPE (END)-->

  <!-- PROCESSING CHAINS (START)
  Much of the crawler's work is specified by the sequential
  application of swappable Processor modules. These Processors
  are collected into three 'chains. The CandidateChain is applied
  to URIs being considered for inclusion, before a URI is enqueued
  for collection. The FetchChain is applied to URIs when their
  turn for collection comes up. The DispositionChain is applied
  after a URI is fetched and analyzed/link-extracted.
  -->

  <!-- CANDIDATE CHAIN (START)
  Processors declared as named beans
  -->
  <bean id="candidateScoper" class="org.archive.crawler.prefetch.CandidateScoper">
  </bean>
  <bean id="preparer" class="org.archive.crawler.prefetch.FrontierPreparer">
    <property name="preferenceDepthHops" value="-1" />
    <property name="preferenceEmbedHops" value="1" />
    <property name="canonicalizationPolicy">
      <ref bean="NetarkivetCanonicalizationPolicy" />
    </property>
    <property name="queueAssignmentPolicy">
      <!-- Bundled with NAS is two queueAssignPolicies (code is in heritrix3-extensions):
      dk.netarkivet.harvester.harvesting.DomainnameQueueAssignmentPolicy
      dk.netarkivet.harvester.harvesting.SeedUriDomainnameQueueAssignmentPolicy
      -->
      <ref bean="NASQueueAssignmentPolicy" />
    </property>
  </bean>
  <!-- Assembled into ordered CandidateChain bean -->
  <bean id="candidateProcessors" class="org.archive.modules.CandidateChain">
    <property name="processors">
      <list>
        <!-- Apply scoping rules to each individual candidate URI... -->
        <ref bean="candidateScoper" />
        <!-- ...then prepare those ACCEPTed for enqueuing to frontier. -->
        <ref bean="preparer" />
      </list>
    </property>
  </bean>
  <!-- CANDIDATE CHAIN (END) -->

  <!-- FETCH CHAIN (START)
  Processors declared as named beans
  -->
  <bean id="preselector" class="org.archive.crawler.prefetch.Preselector">
    <property name="enabled" value="true" />
    <property name="logToFile" value="true" />
    <property name="recheckScope" value="true" />
    <property name="blockAll" value="false" />
  </bean>
  <bean id="preconditions" class="org.archive.crawler.prefetch.PreconditionEnforcer">
    <property name="enabled" value="true" />
    <property name="ipValidityDurationSeconds" value="21600" />
    <property name="robotsValidityDurationSeconds" value="86400" />
    <property name="calculateRobotsOnly" value="false" />
  </bean>
  <bean id="fetchDns" class="org.archive.modules.fetcher.FetchDNS">
    <property name="enabled" value="true" />
    <property name="acceptNonDnsResolves" value="false" />
    <property name="digestContent" value="true" />
    <property name="digestAlgorithm" value="sha1" />
  </bean>
  <bean id="fetchHttp" class="org.archive.modules.fetcher.FetchHTTP">
    <property name="enabled" value="true" />
    <property name="timeoutSeconds" value="1200" />
    <property name="soTimeoutMs" value="20000" />
    <property name="maxFetchKBSec" value="0" />
    <property name="maxLengthBytes" value="0" />
    <property name="ignoreCookies" value="false" />
    <property name="sslTrustLevel" value="OPEN" />
    <property name="defaultEncoding" value="UTF-8" />
    <property name="digestContent" value="true" />
    <property name="digestAlgorithm" value="sha1" />
    <property name="sendIfModifiedSince" value="true" />
    <property name="sendIfNoneMatch" value="true" />
    <property name="sendConnectionClose" value="true" />
    <property name="sendReferer" value="true" />
    <property name="sendRange" value="false" />
    <!-- Accept headers for HTTP fetching -->
    <property name="acceptHeaders">
      <list>
        <value>Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8</value>
      </list>
    </property>
  </bean>
  <bean id="fetchFtp" class="org.archive.modules.fetcher.FetchFTP">
    <!-- DUMMY username and password set for the FTP fetcher.
    Should probably be configured using overlays to allow different
    username/passwords for different sites.
    -->
    <property name="username" value="USERNAME" />
    <property name="password" value="PASSWORD" />
    <property name="extractFromDirs" value="true" />
    <property name="extractParent" value="true" />
    <property name="maxLengthBytes" value="0" />
    <property name="maxFetchKBSec" value="0" />
    <property name="timeoutSeconds" value="1200" />
  </bean>
  <bean id="extractorHttp" class="org.archive.modules.extractor.ExtractorHTTP">
    <property name="enabled" value="true" />
  </bean>
  <bean id="extractorHtml" class="org.archive.modules.extractor.ExtractorHTML">
    <property name="enabled" value="true" />
    <property name="extractJavascript" value="true" />
    <property name="treatFramesAsEmbedLinks" value="true" />
    <property name="ignoreFormActionUrls" value="true" />
    <property name="extractValueAttributes" value="false" />
    <property name="ignoreUnexpectedHtml" value="true" />
  </bean>
  <bean id="extractorCss" class="org.archive.modules.extractor.ExtractorCSS">
    <property name="enabled" value="true" />
  </bean>
  <bean id="icelandicExtractorJs" class="dk.netarkivet.harvester.harvesting.extractor.IcelandicExtractorJS">
    <property name="enabled" value="true" />
    <property name="rejectRelativeMatchingRegexList">
      <list>
        <value>^text/javascript$</value>
        <value>^text/css$</value>
        <value>^a\.[^/]+$</value>
        <value>^div\.[^/]+$</value>
        <value>^[a-zA-Z-]+\.dk$</value>
        <!-- E.g. 3.5.0. Very common in some JS libraries for strings of this nature but very unlikely to be a relative URL -->
        <value>^[0-9]\.([0-9]\.)[0-9]$</value>
        <value>^Microsoft\.XMLHTTP$</value>
      </list>
    </property>
  </bean>
  <bean id="extractorSwf" class="org.archive.modules.extractor.ExtractorSWF">
    <property name="enabled" value="true" />
  </bean>
  <bean id="extractorOAI" class="dk.netarkivet.harvester.harvesting.extractor.ExtractorOAI">
    <property name="enabled" value="false" />
  </bean>
  <bean id="extractorXML" class="org.archive.modules.extractor.ExtractorXML">
    <property name="enabled" value="false" />
  </bean>
  <!-- Assembled into ordered FetchChain bean -->
  <bean id="fetchProcessors" class="org.archive.modules.FetchChain">
    <property name="processors">
      <list>
        <!-- Recheck scope, if so enabled... -->
        <ref bean="preselector" />
        <!-- ...then verify or trigger prerequisite URIs fetched, allow crawling... -->
        <ref bean="preconditions" />
        <!-- ...then check, if quotas is already superseded... -->
        <ref bean="quotaenforcer" />
        <!-- ...then fetch if DNS URI... -->
        <ref bean="fetchDns" />
        <!-- ...then fetch if HTTP URI... -->
        <ref bean="fetchHttp" />
        <!-- ...then fetch if FTP URI... -->
        <ref bean="fetchFtp" />
        <!-- ...then extract oulinks from HTTP headers... -->
        <ref bean="extractorHttp" />
        <!-- ...then extract oulinks from HTML content... -->
        <ref bean="extractorHtml" />
        <!-- ...then extract oulinks from CSS content... -->
        <ref bean="extractorCss" />
        <!-- ...then extract oulinks from Javascript content... -->
        <ref bean="icelandicExtractorJs" />
        <!-- ...then extract oulinks from Flash content. -->
        <ref bean="extractorSwf" />
      </list>
    </property>
  </bean>
  <!-- FETCH CHAIN (END)-->

  <!-- (W)ARC WRITER (START)
  NETARCHIVESUITE: Here the (w)arc writer is inserted
  -->
  <bean id="arcWriter" class="org.archive.modules.writer.ARCWriterProcessor">

<property name="compress" value="false"/>
<property name="prefix" value="3-1"/>
<property name="maxFileSizeBytes" value="1000000000"/>
<property name="poolMaxActive" value="1"/>
<property name="skipIdenticalDigests" value="false"/></bean>
  <!-- (W)ARC WRITER (END) -->

  <!-- DISPOSITION CHAIN (START) -->
  <!-- Processors declared as named beans -->
  <bean id="DeDuplicator" class="is.hi.bok.deduplicator.DeDuplicator">
    <!-- DEDUPLICATION_INDEX_LOCATION_PLACEHOLDER is replaced by path on harvest-server -->
    <property name="indexLocation" value="/home/netarkdv/COMPRESSIONCORPUS/cache/DEDUP_CRAWL_LOG/2-cache" />
    <property name="matchingMethod" value="URL" />
    <property name="tryEquivalent" value="TRUE" />
    <property name="changeContentSize" value="false" />
    <property name="mimeFilter" value="^text/.*" />
    <property name="filterMode" value="BLACKLIST" />
    <property name="origin" value="" />
    <property name="originHandling" value="INDEX" />
    <property name="statsPerHost" value="true" />
  </bean>
  <bean id="candidates" class="org.archive.crawler.postprocessor.CandidatesProcessor">
    <!-- Allow redirected seeds to be accepted as seeds
    In H1, this property belonged to the LinkScoper object, in H3, it is part of the CandidatesProcessor object
    -->
    <property name="seedsRedirectNewSeeds" value="false" />
  </bean>
  <bean id="disposition" class="org.archive.crawler.postprocessor.DispositionProcessor">
    <!-- Politeness -->
    <property name="delayFactor" value="1.0" />
    <property name="maxDelayMs" value="1000" />
    <property name="minDelayMs" value="300" />
    <property name="respectCrawlDelayUpToSeconds" value="0" />
    <property name="maxPerHostBandwidthUsageKbSec" value="0" />
  </bean>
  <!-- Assembled into ordered DispositionChain bean -->
  <bean id="dispositionProcessors" class="org.archive.modules.DispositionChain">
    <property name="processors">
      <list>
        <!-- Write to aggregate archival files... -->

        <!-- NETARCHIVESUITE: Remove the reference below, and the DeDuplicator bean itself to disable Deduplication -->
        <ref bean="DeDuplicator"/>

        <!-- NETARCHIVESUITE: Here the reference to the (w)arcWriter bean is inserted during job-generation -->
        <ref bean="arcWriter"/>

        <!-- NETARCHIVESUITE: This bean is required to report back the number of bytes harvested for each domain  -->
        <bean id="ContentSizeAnnotationPostProcessor"  class="dk.netarkivet.harvester.harvesting.ContentSizeAnnotationPostProcessor"/>

        <!-- ...send each outlink candidate URI to CandidatesChain,
        and enqueue those ACCEPTed to the frontier... -->
        <ref bean="candidates"/>
        <!-- ...then update stats, shared-structures, frontier decisions. -->
        <ref bean="disposition"/>
      </list>
    </property>
  </bean>
  <!-- DISPOSITION CHAIN (END) -->

  <!-- CRAWLCONTROLLER (START)
  Control interface, unifying context
  -->
  <bean id="crawlController" class="org.archive.crawler.framework.CrawlController">
    <property name="maxToeThreads" value="50" />
    <property name="recorderOutBufferBytes" value="4096" />
    <property name="recorderInBufferBytes" value="65536" />
    <property name="pauseAtStart" value="false" />
    <property name="runWhileEmpty" value="false" />
    <property name="scratchDir" value="scratch" />
  </bean>
  <!-- CRAWLCONTROLLER (START) -->

  <!-- FRONTIER (START)
  Record of all URIs discovered and queued-for-collection
  -->
  <bean id="frontier" class="org.archive.crawler.frontier.BdbFrontier">
    <property name="maxRetries" value="3" />
    <property name="retryDelaySeconds" value="300" />
    <property name="recoveryLogEnabled" value="false" />
    <property name="balanceReplenishAmount" value="3000" />
    <property name="errorPenaltyAmount" value="100" />
    <!-- NETARCHIVESUITE: Placeholder -1 -->
    <property name="queueTotalBudget" value="-1" />
    <property name="snoozeLongMs" value="300000" />
    <property name="extract404s" value="false" />
    <property name="extractIndependently" value="false" />
  </bean>
  <!-- FRONTIER (END) -->

  <!-- URI UNIQ FILTER (START)
  Used by frontier to remember already-included URIs
  -->
  <bean id="uriUniqFilter" class="org.archive.crawler.util.BdbUriUniqFilter">
  </bean>
  <!-- URI UNIQ FILTER (END) -->

  <!-- OPTIONAL BUT RECOMMENDED BEANS (START) -->

  <!-- ACTIONDIRECTORY (START)
  Disk directory for mid-crawl operations
  Running job will watch directory for new files with URIs,
  scripts, and other data to be processed during a crawl.
  -->
  <bean id="actionDirectory" class="org.archive.crawler.framework.ActionDirectory">
  </bean>
  <!-- ACTIONDIRECTORY (END) -->

  <!--  CRAWLLIMITENFORCER (START)
  Stops crawl when it reaches configured limits
  -->
  <bean id="crawlLimiter" class="org.archive.crawler.framework.CrawlLimitEnforcer">
    <property name="maxBytesDownload" value="0" />
    <property name="maxDocumentsDownload" value="0" />
    <!-- NETARCHIVESUITE: Placeholder 0 -->
    <property name="maxTimeSeconds" value="0" />
  </bean>
  <!--  CRAWLLIMITENFORCER (END) -->

  <!-- CHECKPOINTSERVICE (START)
  Checkpointing assistance
  -->
  <bean id="checkpointService" class="org.archive.crawler.framework.CheckpointService">
  </bean>
  <!-- CHECKPOINTSERVICE (END) -->
  <!-- OPTIONAL BUT RECOMMENDED BEANS (END) -->

  <!-- OPTIONAL BEANS (START)
  Uncomment and expand as needed, or if non-default alternate implementations are preferred.
  -->

  <!-- RULES CANONICALIZATION POLICY (START) -->
  <bean id="NetarkivetCanonicalizationPolicy" class="org.archive.modules.canonicalize.RulesCanonicalizationPolicy">
    <property name="rules">
      <list>
        <bean class="org.archive.modules.canonicalize.LowercaseRule" />
        <bean class="org.archive.modules.canonicalize.StripUserinfoRule" />
        <!-- disabled by default in PROD templates
        <bean class="org.archive.modules.canonicalize.StripWWWNRule" />
        -->
        <bean class="org.archive.modules.canonicalize.StripWWWRule" />
        <bean class="org.archive.modules.canonicalize.StripSessionIDs" />
        <bean class="org.archive.modules.canonicalize.StripSessionCFIDs" />
        <!-- new in H3 should it be disabled or enabled? -->
        <bean class="org.archive.modules.canonicalize.FixupQueryString" />
      </list>
    </property>
  </bean>
  <!-- RULES CANONICALIZATION POLICY (END) -->

  <!-- QUEUE ASSIGNMENT POLICY (START) -->
  <bean id="NASQueueAssignmentPolicy" class="dk.netarkivet.harvester.harvesting.SeedUriDomainnameQueueAssignmentPolicy">
    <!-- default forceQueueAssignment is "" -->
    <property name="forceQueueAssignment" value="" />
    <!-- default deferToPrevious is true -->
    <property name="deferToPrevious" value="true" />
    <!-- dafault parallelQueues is 1 -->
    <property name="parallelQueues" value="1" />
  </bean>
  <!-- QUEUE ASSIGNMENT POLICY (END) -->

  <!-- COST ASSIGNMENT POLICY (START) -->
  <bean id="costAssignmentPolicy" class="org.archive.crawler.frontier.UnitCostAssignmentPolicy">
  </bean>
  <!-- COST ASSIGNMENT POLICY (END) -->

  <!-- QUOTA ENFORCER (START) -->
  <bean id="quotaenforcer" class="org.archive.crawler.prefetch.QuotaEnforcer">
    <property name="forceRetire" value="false" />
    <!-- Server properties -->
    <property name="serverMaxFetchSuccesses" value="-1" />
    <property name="serverMaxSuccessKb" value="-1" />
    <property name="serverMaxFetchResponses" value="-1" />
    <property name="serverMaxAllKb" value="-1" />
    <!-- Host properties -->
    <property name="hostMaxFetchSuccesses" value="-1" />
    <property name="hostMaxSuccessKb" value="-1" />
    <property name="hostMaxFetchResponses" value="-1" />
    <property name="hostMaxAllKb" value="-1" />
    <!-- Group properties -->
    <!-- NETARCHIVESUITE: Placeholder 2000 -->
    <property name="groupMaxFetchSuccesses" value="2000" />
    <property name="groupMaxSuccessKb" value="-1" />
    <property name="groupMaxFetchResponses" value="-1" />
    <!-- NETARCHIVESUITE: Placeholder 488282 -->
    <property name="groupMaxAllKb" value="488282" />
  </bean>
  <!-- QUOTA ENFORCER (END) -->
  <!-- OPTIONAL BEANS (END) -->

  <!-- REQUIRED STANDARD BEANS (START)
  It will be very rare to replace or reconfigure the following beans.
  -->

  <!-- STATISTICSTRACKER (START)
  Standard stats/reporting collector
  -->
  <bean id="statisticsTracker" class="org.archive.crawler.reporting.StatisticsTracker" autowire="byName">
    <property name="intervalSeconds" value="20" />
  </bean>
  <!-- STATISTICSTRACKER (END) -->

  <!-- CRAWLERLOGGERMODULE: shared logging facility -->
  <bean id="loggerModule" class="org.archive.crawler.reporting.CrawlerLoggerModule">
    <property name="path" value="logs" />
  </bean>

  <!-- SHEETOVERLAYMANAGER (START)
  Manager of sheets of contextual overlays
  Autowired to include any SheetForSurtPrefix or SheetForDecideRuled beans
  -->
  <bean id="sheetOverlaysManager" autowire="byType" class="org.archive.crawler.spring.SheetOverlaysManager">
  </bean>
  <!-- SHEETOVERLAYMANAGER (END) -->

  <!-- BDBMODULE (START)
  Shared BDB-JE disk persistence manager
  -->
  <bean id="bdb" class="org.archive.bdb.BdbModule">
    <property name="dir" value="state" />
    <property name="cachePercent" value="40" />
  </bean>
  <!-- BDBMODULE (END) -->

  <!-- BDBCOOKIESTORAGE (START)
  Disk-based cookie storage for FetchHTTP
  -->
  <bean id="cookieStorage" class="org.archive.modules.fetcher.BdbCookieStore">
  </bean>
  <!-- BDBCOOKIESTORAGE (END) -->

  <!-- SERVERCACHE (START)
  Shared cache of server/host info
  -->
  <bean id="serverCache" class="org.archive.modules.net.BdbServerCache">
  </bean>
  <!-- SERVERCACHE (END) -->

  <!-- CONFIG PATH CONFIGURER (START)
  Required helper making crawl paths relative
  to crawler-beans.cxml file, and tracking crawl files for web UI
  -->
  <bean id="configPathConfigurer" class="org.archive.spring.ConfigPathConfigurer">
  </bean>
  <!-- CONFIG PATH CONFIGURER (END) -->
  <!-- REQUIRED STANDARD BEANS (END) -->

  <!-- A processor to enforce runtime limits on crawls if wanted
  The operations available is Pause, Terminate, Block_Uris
  -->

  <!-- TODO CHECK, if this bean can coexist with the crawlLimitenforcer
  <bean id="runtimeLimitEnforcer" class="org.archive.crawler.prefetch.RuntimeLimitEnforcer">
  <property name="runtimeSeconds" value="82800"/>
  <property name="operation" value="Terminate"/>
  </bean> -->

</beans>

metadata://netarkivet.dk/crawl/setup/harvestInfo.xml?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/xml 590
<?xml version="1.0" encoding="UTF-8"?>

<harvestInfo>
  <version>0.5</version>
  <jobId>3</jobId>
  <channel>HIGHPRIORITY</channel>
  <harvestNum>3</harvestNum>
  <origHarvestDefinitionID>1</origHarvestDefinitionID>
  <maxBytesPerDomain>500000000</maxBytesPerDomain>
  <maxObjectsPerDomain>2000</maxObjectsPerDomain>
  <orderXMLName>default_orderxml</orderXMLName>
  <origHarvestDefinitionName>t1</origHarvestDefinitionName>
  <scheduleName>Once_a_week</scheduleName>
  <harvestFilenamePrefix>3-1</harvestFilenamePrefix>
  <jobSubmitDate>2016-12-05T10:09:39Z</jobSubmitDate>
</harvestInfo>

metadata://netarkivet.dk/crawl/setup/seeds.txt?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 14
www.kaarefc.dk
metadata://netarkivet.dk/crawl/reports/archivefiles-report.txt?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 150
[ARCHIVEFILE] [Opened] [Closed] [Size]
3-1-20161205101105604-00000-14970~sb-test-har-001.statsbiblioteket.dk~8171.arc 2016-12-05T10:11:43.000Z 155907

metadata://netarkivet.dk/crawl/reports/crawl-report.txt?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 392
crawl name: default_orderxml
crawl status: Finished
duration: 41s968ms

seeds crawled: 1
seeds uncrawled: 0

hosts visited: 4

URIs processed: 16
URI successes: 15
URI failures: 1
URI disregards: 0

novel URIs: 8
duplicate-by-hash URIs: 7

total crawled bytes: 153399 (150 KiB) 
novel crawled bytes: 7405 (7.2 KiB)
duplicate-by-hash crawled bytes: 145994 (143 KiB) 

URIs/sec: 0.36
KB/sec: 3

metadata://netarkivet.dk/crawl/reports/frontier-summary-report.txt?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 4121
Frontier report - 201612051011
 Job being crawled: default_orderxml

 -----===== STATS =====-----
 Discovered:    16
 Queued:        0
 Finished:      16
  Successfully: 15
  Failed:       1
  Disregarded:  0

 -----===== QUEUES =====-----
 Already included size:     16
               pending:     0

 All class queues map size: 2
             Active queues: 0
                    In-process: 0
                         Ready: 0
                       Snoozed: 0
           Inactive queues: 0 (p3: 0)
            Retired queues: 0
          Exhausted queues: 2

             Last state: EMPTY
 -----===== MANAGER THREAD =====-----
Java Thread State: RUNNABLE
Blocked/Waiting On: NONE
    java.lang.Thread.getStackTrace(Thread.java:1552)
    org.archive.crawler.framework.ToeThread.reportThread(ToeThread.java:492)
    org.archive.crawler.frontier.WorkQueueFrontier.reportTo(WorkQueueFrontier.java:1308)
    org.archive.crawler.reporting.FrontierSummaryReport.write(FrontierSummaryReport.java:39)
    org.archive.crawler.reporting.StatisticsTracker.writeReportFile(StatisticsTracker.java:898)
    org.archive.crawler.reporting.StatisticsTracker.dumpReports(StatisticsTracker.java:926)
    org.archive.crawler.reporting.StatisticsTracker.stop(StatisticsTracker.java:342)
    org.springframework.context.support.DefaultLifecycleProcessor.doStop(DefaultLifecycleProcessor.java:236)
    org.springframework.context.support.DefaultLifecycleProcessor.doStop(DefaultLifecycleProcessor.java:213)
    org.springframework.context.support.DefaultLifecycleProcessor.doStop(DefaultLifecycleProcessor.java:213)
    org.springframework.context.support.DefaultLifecycleProcessor.doStop(DefaultLifecycleProcessor.java:213)
    org.springframework.context.support.DefaultLifecycleProcessor.doStop(DefaultLifecycleProcessor.java:213)
    org.springframework.context.support.DefaultLifecycleProcessor.doStop(DefaultLifecycleProcessor.java:213)
    org.springframework.context.support.DefaultLifecycleProcessor.access$2(DefaultLifecycleProcessor.java:206)
    org.springframework.context.support.DefaultLifecycleProcessor$LifecycleGroup.stop(DefaultLifecycleProcessor.java:352)
    org.springframework.context.support.DefaultLifecycleProcessor.stopBeans(DefaultLifecycleProcessor.java:195)
    org.springframework.context.support.DefaultLifecycleProcessor.stop(DefaultLifecycleProcessor.java:103)
    org.springframework.context.support.AbstractApplicationContext.stop(AbstractApplicationContext.java:1241)
    org.archive.crawler.framework.CrawlController.completeStop(CrawlController.java:392)
    org.archive.crawler.framework.CrawlController.noteFrontierState(CrawlController.java:663)
    org.archive.crawler.frontier.AbstractFrontier.reachedState(AbstractFrontier.java:442)
    org.archive.crawler.frontier.AbstractFrontier.managementTasks(AbstractFrontier.java:399)
    org.archive.crawler.frontier.AbstractFrontier$1.run(AbstractFrontier.java:316)

 -----===== 2 LONGEST QUEUES =====-----
LONGEST#0:
Queue kaarefc.dk (p3)
  0 items
    last enqueued: http://www.kaarefc.dk/avatar.png
      last peeked: http://www.kaarefc.dk/avatar.png
   total expended: 6 (total budget: -1)
   active balance: 2994
   last(avg) cost: 1(1)
   totalScheduled fetchSuccesses fetchFailures fetchDisregards fetchResponses robotsDenials successBytes totalBytes fetchNonResponses lastSuccessTime
   6 6 0 0 6 0 133404 133404 2 2016-12-05T10:11:12.296Z
   SimplePrecedenceProvider
   3

LONGEST#1:
Queue w3.org (p3)
  0 items
    last enqueued: http://jigsaw.w3.org/favicon.ico
      last peeked: http://jigsaw.w3.org/favicon.ico
   total expended: 110 (total budget: -1)
   active balance: 2890
   last(avg) cost: 1(11)
   totalScheduled fetchSuccesses fetchFailures fetchDisregards fetchResponses robotsDenials successBytes totalBytes fetchNonResponses lastSuccessTime
   10 9 1 0 9 0 19995 19995 5 2016-12-05T10:11:40.893Z
   SimplePrecedenceProvider
   3


 -----===== IN-PROCESS QUEUES =====-----

 -----===== READY QUEUES =====-----

 -----===== SNOOZED QUEUES =====-----

 -----===== INACTIVE QUEUES =====-----

 -----===== RETIRED QUEUES =====-----

metadata://netarkivet.dk/crawl/reports/hosts-report.txt?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 361
[#urls] [#bytes] [host] [#robots] [#remaining] [#novel-urls] [#novel-bytes] [#dup-by-hash-urls] [#dup-by-hash-bytes] [#not-modified-urls] [#not-modified-bytes]
5 133352 www.kaarefc.dk 0 0 4 3961 1 129391 0 0 
4 15098 www.w3.org 0 0 1 3287 3 11811 0 0 
3 157 dns: 0 0 3 157 0 0 0 0 
3 4792 jigsaw.w3.org 0 0 0 0 3 4792 0 0 
0 0 validator.w3.org 0 0 0 0 0 0 0 0 

metadata://netarkivet.dk/crawl/reports/mimetype-report.txt?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 195
[#urls] [#bytes] [mime-types]
3 134293 image/png
3 157 text/dns
3 2626 text/html
2 4239 image/gif
1 553 application/octet-stream
1 6909 image/vnd.microsoft.icon
1 1335 text/css
1 3287 text/plain

metadata://netarkivet.dk/crawl/reports/processors-report.txt?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 2247
CandidateChain - Processors report - 201612051011
  Number of Processors: 2

Processor: org.archive.crawler.prefetch.CandidateScoper

Processor: org.archive.crawler.prefetch.FrontierPreparer


FetchChain - Processors report - 201612051011
  Number of Processors: 11

Processor: org.archive.crawler.prefetch.Preselector

Processor: org.archive.crawler.prefetch.PreconditionEnforcer

Processor: org.archive.crawler.prefetch.QuotaEnforcer

Processor: org.archive.modules.fetcher.FetchDNS

Processor: org.archive.modules.fetcher.FetchHTTP

Processor: org.archive.modules.fetcher.FetchFTP

Processor: org.archive.modules.extractor.ExtractorHTTP
  2 links from 12 CrawlURIs

Processor: org.archive.modules.extractor.ExtractorHTML
  7 links from 1 CrawlURIs

Processor: org.archive.modules.extractor.ExtractorCSS
  0 links from 1 CrawlURIs

Processor: dk.netarkivet.harvester.harvesting.extractor.IcelandicExtractorJS
  0 links from 0 CrawlURIs
  False positives eliminated: 0

Processor: org.archive.modules.extractor.ExtractorSWF
  0 links from 0 CrawlURIs


DispositionChain - Processors report - 201612051011
  Number of Processors: 5

Processor: is.hi.bok.digest.DeDuplicator
  Function:          Abort processing of duplicate records
                     - Lookup by url in use
  Total handled:     7
  Duplicates found:  7 100.0%
  Bytes total:       145994 (143 KiB)
  Bytes discarded:   145994 (143 KiB) 100.0%
  New (no hits):     0
  Exact hits:        7
  Equivalent hits:   0
  Timestamp predicts: (Where exact URL existed in the index)
  Change correctly:  0
  Change falsely:     0
  Non-change correct:7
  Non-change falsely: 0
  Missing timpestamp:0
  [Host] [total] [duplicates] [bytes] [bytes discarded] [new] [exact] [equiv] [change correct] [change falsely] [non-change correct] [non-change falsely] [no timestamp]
  jigsaw.w3.org 3 3 4792 4792 0 3 0 0 0 3 0 0
  www.w3.org 3 3 11811 11811 0 3 0 0 0 3 0 0
  www.kaarefc.dk 1 1 129391 129391 0 1 0 0 0 1 0 0


Processor: org.archive.modules.writer.ARCWriterProcessor

Processor: dk.netarkivet.harvester.harvesting.ContentSizeAnnotationPostProcessor

Processor: org.archive.crawler.postprocessor.CandidatesProcessor

Processor: org.archive.crawler.postprocessor.DispositionProcessor



metadata://netarkivet.dk/crawl/reports/responsecode-report.txt?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 35
[#urls] [rescode]
10 200
3 1
2 404

metadata://netarkivet.dk/crawl/reports/seeds-report.txt?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 69
[code] [status] [seed] [redirect]
200 CRAWLED http://www.kaarefc.dk/

metadata://netarkivet.dk/crawl/reports/source-report.txt?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 137
[source] [host] [#urls]
www.kaarefc.dk www.kaarefc.dk 5
www.kaarefc.dk www.w3.org 4
www.kaarefc.dk dns: 3
www.kaarefc.dk jigsaw.w3.org 3

metadata://netarkivet.dk/crawl/reports/threads-report.txt?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 13
no ToeThreads
metadata://netarkivet.dk/crawl/logs/alerts.log?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 0

metadata://crawl/index/deduplicationmigration?majorversion=0&minorversion=0 130.225.27.140 20170125093039 text/plain 542
2-1-20161205100306320-00000-4320~kb-test-har-004.kb.dk~8173.arc 138604 133943
2-1-20161205100306320-00000-4320~kb-test-har-004.kb.dk~8173.arc 9132 4434
2-1-20161205100306320-00000-4320~kb-test-har-004.kb.dk~8173.arc 142039 136539
2-1-20161205100306320-00000-4320~kb-test-har-004.kb.dk~8173.arc 144299 138362
2-1-20161205100306320-00000-4320~kb-test-har-004.kb.dk~8173.arc 151298 140090
2-1-20161205100306320-00000-4320~kb-test-har-004.kb.dk~8173.arc 154442 142487
2-1-20161205100306320-00000-4320~kb-test-har-004.kb.dk~8173.arc 153798 142099

metadata://netarkivet.dk/crawl/logs/crawl.log?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 4159
2016-12-05T10:11:05.612Z     1         52 dns:www.kaarefc.dk P http://www.kaarefc.dk/ text/dns #010 20161205101104859+228 sha1:I2WOSD2OY4ZGSSHKQVMRC3WFAPOHHZIB www.kaarefc.dk content-size:52
2016-12-05T10:11:08.075Z   404        208 http://www.kaarefc.dk/robots.txt P http://www.kaarefc.dk/ text/html #028 20161205101106672+1389 sha1:XNFFJ6VAXWKDEXG4RE4KTA2W6ENTMXMG www.kaarefc.dk content-size:447
2016-12-05T10:11:09.265Z   200       1403 http://www.kaarefc.dk/ - - text/html #028 20161205101109092+65 sha1:SRUXQCLFCC4GVEDXGETBPUTUHRENTCRT www.kaarefc.dk content-size:1731,3t
2016-12-05T10:11:09.445Z     1         51 dns:www.w3.org EP http://www.w3.org/Icons/valid-xhtml10-blue text/dns #028 20161205101109316+126 sha1:GUC62XH5YLRN72B7GN2WN4XK3TMMP7TE www.kaarefc.dk content-size:51
2016-12-05T10:11:09.729Z   404        209 http://www.kaarefc.dk/favicon.ico I http://www.kaarefc.dk/ text/html #040 20161205101109658+64 sha1:XVZRRMALCW32DSFEQUSEDH5C4XBHUW3N www.kaarefc.dk content-size:448
2016-12-05T10:11:10.559Z   200       1028 http://www.kaarefc.dk/style.css E http://www.kaarefc.dk/ text/css #028 20161205101110485+62 sha1:5SVQUXJZSFCOIBOVYSVD6UQRB5KTVKYZ www.kaarefc.dk content-size:1335
2016-12-05T10:11:10.821Z   200       2914 http://www.w3.org/robots.txt EP http://www.w3.org/Icons/valid-xhtml10-blue text/plain #028 20161205101110573+232 sha1:BQSX2EEWJYE5RZ4B26NUWTTAJLUBRJZG www.kaarefc.dk content-size:3287
2016-12-05T10:11:12.147Z   200       2026 http://www.w3.org/Icons/valid-xhtml10-blue E http://www.kaarefc.dk/ image/png #048 20161205101111667+248 sha1:C3PH3IWTSURQ7XILQRHDIDDGAD2ORRPH www.kaarefc.dk duplicate:"2-1-20161205100306320-00000-4320~kb-test-har-004.kb.dk~8173.arc,138604,20161205100310645",content-size:2494,3t
2016-12-05T10:11:12.313Z   200     129079 http://www.kaarefc.dk/avatar.png E http://www.kaarefc.dk/ image/png #041 20161205101111668+628 sha1:LDC3XHXXOUWTPL3U2MPZOGYEBFMUJ53I www.kaarefc.dk duplicate:"2-1-20161205100306320-00000-4320~kb-test-har-004.kb.dk~8173.arc,9132,20161205100310384",content-size:129391
2016-12-05T10:11:13.554Z     1         54 dns:jigsaw.w3.org EP http://jigsaw.w3.org/css-validator/images/vcss-blue text/dns #027 20161205101112698+854 sha1:KMVTTVEWQUZ7VBE3IJTRNNODLQYE7JS2 www.kaarefc.dk content-size:54
2016-12-05T10:11:34.636Z  -404          0 http://jigsaw.w3.org/robots.txt EP http://jigsaw.w3.org/css-validator/images/vcss-blue unknown #027 - - www.kaarefc.dk -
2016-12-05T10:11:34.893Z   200       1759 http://jigsaw.w3.org/css-validator/images/vcss-blue E http://www.kaarefc.dk/ image/gif #027 20161205101134647+219 sha1:YEX3QI4MLIW3EPJCYCA2OZWT4ZEQGA36 www.kaarefc.dk duplicate:"2-1-20161205100306320-00000-4320~kb-test-har-004.kb.dk~8173.arc,142039,20161205100313871",content-size:2164,3t
2016-12-05T10:11:36.559Z   200       6518 http://www.w3.org/favicon.ico EPI http://www.w3.org/robots.txt image/vnd.microsoft.icon #041 20161205101135333+1218 sha1:2SASTUEUQUZKXH5LWWYUFANTHXZGOBVT www.kaarefc.dk duplicate:"2-1-20161205100306320-00000-4320~kb-test-har-004.kb.dk~8173.arc,144299,20161205100314879",content-size:6909
2016-12-05T10:11:38.808Z   200       2026 http://www.w3.org/Icons/valid-xhtml10-blue.png EI http://www.w3.org/Icons/valid-xhtml10-blue image/png #041 20161205101137572+1226 sha1:C3PH3IWTSURQ7XILQRHDIDDGAD2ORRPH www.kaarefc.dk duplicate:"2-1-20161205100306320-00000-4320~kb-test-har-004.kb.dk~8173.arc,151298,20161205100315930",content-size:2408
2016-12-05T10:11:40.073Z   200       1759 http://jigsaw.w3.org/css-validator/images/vcss-blue.gif EI http://jigsaw.w3.org/css-validator/images/vcss-blue image/gif #041 20161205101139823+242 sha1:YEX3QI4MLIW3EPJCYCA2OZWT4ZEQGA36 www.kaarefc.dk duplicate:"2-1-20161205100306320-00000-4320~kb-test-har-004.kb.dk~8173.arc,154442,20161205100318947",content-size:2075
2016-12-05T10:11:40.904Z   200        318 http://jigsaw.w3.org/favicon.ico EI http://jigsaw.w3.org/css-validator/images/vcss-blue application/octet-stream #042 20161205101140671+222 sha1:7WSXRDYAC7LQOMXF5SLVAC27YITWZHNH www.kaarefc.dk duplicate:"2-1-20161205100306320-00000-4320~kb-test-har-004.kb.dk~8173.arc,153798,20161205100316927",content-size:553

metadata://netarkivet.dk/crawl/logs/heritrix3_err.log?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 4569
2016-12-05 10:10:49.867 INFO thread-1 org.archive.crawler.framework.Engine.addJobDirectory() added crawl job: CrawlRSS-Sample-Profile
2016-12-05 10:10:49.986 INFO thread-1 org.archive.crawler.framework.Engine.addJobDirectory() added crawl job: CrawlRSS-Sample-Profile-DB-conf
2016-12-05 10:10:53.344 INFO thread-13 org.archive.crawler.framework.Engine.addJobDirectory() added crawl job: 3_1480932579647
2016-12-05 10:10:57.749 INFO thread-13 org.archive.crawler.framework.CrawlJob.instantiateContainer() Job instantiated
2016-12-05 10:10:58.614 INFO thread-13 org.archive.crawler.framework.CrawlJob.launch() Job launched
2016-12-05 10:11:00.213 INFO thread-16 org.archive.spring.PathSharingContext.initLaunchId() launch id 20161205101100
2016-12-05 10:11:01.508 INFO thread-16 org.archive.io.WriterPool.<init>() Initial configuration: prefix=3-1, template=${prefix}-${timestamp17}-${serialno}-${heritrix.pid}~${heritrix.hostname}~${heritrix.port}, compress=false, maxSize=1000000000, maxActive=1, maxWait=500
2016-12-05 10:11:01.702 INFO thread-16 org.archive.crawler.framework.CrawlJob.onApplicationEvent() PREPARING 20161205101100
2016-12-05 10:11:04.619 INFO thread-21 org.archive.crawler.framework.CrawlController.noteFrontierState() Crawl running.
2016-12-05 10:11:04.631 INFO thread-21 org.archive.crawler.framework.CrawlJob.onApplicationEvent() RUNNING 20161205101100
2016-12-05 10:11:41.675 INFO thread-21 org.archive.crawler.framework.CrawlController.noteFrontierState() Crawl empty.
2016-12-05 10:11:41.676 INFO thread-21 org.archive.crawler.framework.CrawlJob.onApplicationEvent() STOPPING 20161205101100
2016-12-05 10:11:41.677 INFO thread-21 org.archive.crawler.framework.CrawlJob.onApplicationEvent() EMPTY 20161205101100
2016-12-05 10:11:43.698 INFO thread-21 org.archive.crawler.reporting.StatisticsTracker.writeReportFile() wrote report: /home/netarkdv/COMPRESSIONCORPUS/harvester_high/3_1480932579647/heritrix3/./jobs/3_1480932579647/20161205101100/reports/crawl-report.txt
2016-12-05 10:11:43.744 INFO thread-21 org.archive.crawler.reporting.StatisticsTracker.writeReportFile() wrote report: /home/netarkdv/COMPRESSIONCORPUS/harvester_high/3_1480932579647/heritrix3/./jobs/3_1480932579647/20161205101100/reports/seeds-report.txt
2016-12-05 10:11:43.761 INFO thread-21 org.archive.crawler.reporting.StatisticsTracker.writeReportFile() wrote report: /home/netarkdv/COMPRESSIONCORPUS/harvester_high/3_1480932579647/heritrix3/./jobs/3_1480932579647/20161205101100/reports/hosts-report.txt
2016-12-05 10:11:43.772 INFO thread-21 org.archive.crawler.reporting.StatisticsTracker.writeReportFile() wrote report: /home/netarkdv/COMPRESSIONCORPUS/harvester_high/3_1480932579647/heritrix3/./jobs/3_1480932579647/20161205101100/reports/source-report.txt
2016-12-05 10:11:43.787 INFO thread-21 org.archive.crawler.reporting.StatisticsTracker.writeReportFile() wrote report: /home/netarkdv/COMPRESSIONCORPUS/harvester_high/3_1480932579647/heritrix3/./jobs/3_1480932579647/20161205101100/reports/mimetype-report.txt
2016-12-05 10:11:43.797 INFO thread-21 org.archive.crawler.reporting.StatisticsTracker.writeReportFile() wrote report: /home/netarkdv/COMPRESSIONCORPUS/harvester_high/3_1480932579647/heritrix3/./jobs/3_1480932579647/20161205101100/reports/responsecode-report.txt
2016-12-05 10:11:43.801 INFO thread-21 org.archive.crawler.reporting.StatisticsTracker.writeReportFile() wrote report: /home/netarkdv/COMPRESSIONCORPUS/harvester_high/3_1480932579647/heritrix3/./jobs/3_1480932579647/20161205101100/reports/processors-report.txt
2016-12-05 10:11:43.818 INFO thread-21 org.archive.crawler.reporting.StatisticsTracker.writeReportFile() wrote report: /home/netarkdv/COMPRESSIONCORPUS/harvester_high/3_1480932579647/heritrix3/./jobs/3_1480932579647/20161205101100/reports/frontier-summary-report.txt
2016-12-05 10:11:43.819 INFO thread-21 org.archive.crawler.reporting.StatisticsTracker.writeReportFile() wrote report: /home/netarkdv/COMPRESSIONCORPUS/harvester_high/3_1480932579647/heritrix3/./jobs/3_1480932579647/20161205101100/reports/threads-report.txt
2016-12-05 10:11:43.820 INFO thread-21 org.archive.crawler.framework.CheckpointService.stop() Cleaned up Checkpoint TimerThread.
2016-12-05 10:11:43.823 INFO thread-21 org.archive.crawler.framework.CrawlJob.onApplicationEvent() FINISHED 20161205101100
2016-12-05 10:11:43.824 INFO thread-21 org.archive.crawler.frontier.AbstractFrontier.crawlEnded() Closing with 0 urls still in queue.
2016-12-05 10:12:05.472 INFO thread-13 org.archive.crawler.framework.CrawlJob.doTeardown() Job instance discarded

metadata://netarkivet.dk/crawl/logs/heritrix3_out.log?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 12436
Oracle Corporation Java(TM) SE Runtime Environment 1.8.0_05-b13
engine listening at port 8171
operator login set per command-line
Heritrix version: 3.3.0-LBS-2016-02
2016-12-05 10:11:02.001 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.bd', ignoring
2016-12-05 10:11:02.022 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.bn', ignoring
2016-12-05 10:11:02.036 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.nom.br', ignoring
2016-12-05 10:11:02.044 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.ck', ignoring
2016-12-05 10:11:02.045 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '!www.ck', ignoring
2016-12-05 10:11:02.057 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.er', ignoring
2016-12-05 10:11:02.059 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.fj', ignoring
2016-12-05 10:11:02.061 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.fk', ignoring
2016-12-05 10:11:02.067 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.gu', ignoring
2016-12-05 10:11:02.094 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.jm', ignoring
2016-12-05 10:11:02.100 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.kawasaki.jp', ignoring
2016-12-05 10:11:02.101 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.kitakyushu.jp', ignoring
2016-12-05 10:11:02.103 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.kobe.jp', ignoring
2016-12-05 10:11:02.104 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.nagoya.jp', ignoring
2016-12-05 10:11:02.105 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.sapporo.jp', ignoring
2016-12-05 10:11:02.106 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.sendai.jp', ignoring
2016-12-05 10:11:02.113 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.yokohama.jp', ignoring
2016-12-05 10:11:02.114 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '!city.kawasaki.jp', ignoring
2016-12-05 10:11:02.115 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '!city.kitakyushu.jp', ignoring
2016-12-05 10:11:02.116 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '!city.kobe.jp', ignoring
2016-12-05 10:11:02.118 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '!city.nagoya.jp', ignoring
2016-12-05 10:11:02.119 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '!city.sapporo.jp', ignoring
2016-12-05 10:11:02.120 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '!city.sendai.jp', ignoring
2016-12-05 10:11:02.121 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '!city.yokohama.jp', ignoring
2016-12-05 10:11:02.202 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.ke', ignoring
2016-12-05 10:11:02.204 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.kh', ignoring
2016-12-05 10:11:02.216 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.kw', ignoring
2016-12-05 10:11:02.231 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.mm', ignoring
2016-12-05 10:11:02.270 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.mz', ignoring
2016-12-05 10:11:02.276 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '!teledata.mz', ignoring
2016-12-05 10:11:02.312 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.np', ignoring
2016-12-05 10:11:02.316 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.pg', ignoring
2016-12-05 10:11:02.352 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.sch.uk', ignoring
2016-12-05 10:11:02.368 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.ye', ignoring
2016-12-05 10:11:02.372 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.zw', ignoring
2016-12-05 10:11:02.428 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.compute.estate', ignoring
2016-12-05 10:11:02.430 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.alces.network', ignoring
2016-12-05 10:11:02.436 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.platform.sh', ignoring
2016-12-05 10:11:02.438 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.cryptonomic.net', ignoring
2016-12-05 10:11:02.455 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.api.githubcloud.com', ignoring
2016-12-05 10:11:02.457 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.ext.githubcloud.com', ignoring
2016-12-05 10:11:02.458 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.githubcloudusercontent.com', ignoring
2016-12-05 10:11:02.460 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.0emm.com', ignoring
2016-12-05 10:11:02.465 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.magentosite.cloud', ignoring
2016-12-05 10:11:02.936 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.bd', ignoring
2016-12-05 10:11:02.939 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.bn', ignoring
2016-12-05 10:11:02.941 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.nom.br', ignoring
2016-12-05 10:11:02.944 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.ck', ignoring
2016-12-05 10:11:02.946 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '!www.ck', ignoring
2016-12-05 10:11:02.951 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.er', ignoring
2016-12-05 10:11:02.952 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.fj', ignoring
2016-12-05 10:11:02.953 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.fk', ignoring
2016-12-05 10:11:02.956 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.gu', ignoring
2016-12-05 10:11:03.501 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.jm', ignoring
2016-12-05 10:11:03.503 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.kawasaki.jp', ignoring
2016-12-05 10:11:03.504 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.kitakyushu.jp', ignoring
2016-12-05 10:11:03.515 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.kobe.jp', ignoring
2016-12-05 10:11:03.516 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.nagoya.jp', ignoring
2016-12-05 10:11:03.517 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.sapporo.jp', ignoring
2016-12-05 10:11:03.518 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.sendai.jp', ignoring
2016-12-05 10:11:03.518 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.yokohama.jp', ignoring
2016-12-05 10:11:03.519 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '!city.kawasaki.jp', ignoring
2016-12-05 10:11:03.520 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '!city.kitakyushu.jp', ignoring
2016-12-05 10:11:03.520 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '!city.kobe.jp', ignoring
2016-12-05 10:11:03.521 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '!city.nagoya.jp', ignoring
2016-12-05 10:11:03.522 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '!city.sapporo.jp', ignoring
2016-12-05 10:11:03.522 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '!city.sendai.jp', ignoring
2016-12-05 10:11:03.523 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '!city.yokohama.jp', ignoring
2016-12-05 10:11:03.624 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.ke', ignoring
2016-12-05 10:11:03.625 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.kh', ignoring
2016-12-05 10:11:03.628 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.kw', ignoring
2016-12-05 10:11:03.631 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.mm', ignoring
2016-12-05 10:11:03.643 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.mz', ignoring
2016-12-05 10:11:03.644 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '!teledata.mz', ignoring
2016-12-05 10:11:03.659 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.np', ignoring
2016-12-05 10:11:03.661 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.pg', ignoring
2016-12-05 10:11:03.676 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.sch.uk', ignoring
2016-12-05 10:11:03.684 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.ye', ignoring
2016-12-05 10:11:03.686 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.zw', ignoring
2016-12-05 10:11:03.709 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.compute.estate', ignoring
2016-12-05 10:11:03.710 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.alces.network', ignoring
2016-12-05 10:11:03.713 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.platform.sh', ignoring
2016-12-05 10:11:03.714 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.cryptonomic.net', ignoring
2016-12-05 10:11:03.722 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.api.githubcloud.com', ignoring
2016-12-05 10:11:03.723 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.ext.githubcloud.com', ignoring
2016-12-05 10:11:03.723 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.githubcloudusercontent.com', ignoring
2016-12-05 10:11:03.724 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.0emm.com', ignoring
2016-12-05 10:11:03.728 WARNING thread-16 dk.netarkivet.common.utils.TLD.readTldsFromPublicSuffixFile() Invalid tld '*.magentosite.cloud', ignoring

metadata://netarkivet.dk/crawl/logs/heritrix_out.log?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 1135
Mon Dec  5 11:10:48 CET 2016 Starting heritrix
Linux sb-test-har-001 2.6.32-642.6.2.el6.x86_64 #1 SMP Wed Oct 26 06:52:09 UTC 2016 x86_64 x86_64 x86_64 GNU/Linux
java version "1.8.0_05"
Java(TM) SE Runtime Environment (build 1.8.0_05-b13)
Java HotSpot(TM) 64-Bit Server VM (build 25.5-b02, mixed mode)
JAVA_OPTS=-Xmx1598M  -Ddk.netarkivet.settings.file=/home/netarkdv/COMPRESSIONCORPUS/conf/settings_HarvestControllerApplication_sbhigh_h3.xml
core file size          (blocks, -c) 0
data seg size           (kbytes, -d) unlimited
scheduling priority             (-e) 0
file size               (blocks, -f) unlimited
pending signals                 (-i) 15221
max locked memory       (kbytes, -l) 64
max memory size         (kbytes, -m) unlimited
open files                      (-n) 4096
pipe size            (512 bytes, -p) 8
POSIX message queues     (bytes, -q) 819200
real-time priority              (-r) 0
stack size              (kbytes, -s) 10240
cpu time               (seconds, -t) unlimited
max user processes              (-u) 1024
virtual memory          (kbytes, -v) unlimited
file locks                      (-x) unlimited

metadata://netarkivet.dk/crawl/logs/job.log?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 450
2016-12-05T11:10:57.749+01:00 INFO Job instantiated
2016-12-05T11:10:58.614+01:00 INFO Job launched
2016-12-05T11:11:01.702+01:00 INFO PREPARING 20161205101100
2016-12-05T11:11:04.631+01:00 INFO RUNNING 20161205101100
2016-12-05T11:11:41.676+01:00 INFO STOPPING 20161205101100
2016-12-05T11:11:41.677+01:00 INFO EMPTY 20161205101100
2016-12-05T11:11:43.823+01:00 INFO FINISHED 20161205101100
2016-12-05T11:12:05.472+01:00 INFO Job instance discarded

metadata://netarkivet.dk/crawl/logs/nonfatal-errors.log?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 4299
2016-12-05T10:11:34.634Z  -404          0 http://jigsaw.w3.org/robots.txt EP http://jigsaw.w3.org/css-validator/images/vcss-blue unknown #027 - - www.kaarefc.dk -
 org.apache.http.conn.ConnectTimeoutException: Connect to jigsaw.w3.org [/128.30.52.21] failed: connect timed out
	at org.apache.http.impl.conn.HttpClientConnectionOperator.connect(HttpClientConnectionOperator.java:134)
	at org.apache.http.impl.conn.BasicHttpClientConnectionManager.connect(BasicHttpClientConnectionManager.java:318)
	at org.apache.http.impl.execchain.MainClientExec.establishRoute(MainClientExec.java:363)
	at org.apache.http.impl.execchain.MainClientExec.execute(MainClientExec.java:219)
	at org.apache.http.impl.execchain.ProtocolExec.execute(ProtocolExec.java:195)
	at org.apache.http.impl.execchain.RetryExec.execute(RetryExec.java:86)
	at org.apache.http.impl.client.InternalHttpClient.doExecute(InternalHttpClient.java:184)
	at org.apache.http.impl.client.CloseableHttpClient.execute(CloseableHttpClient.java:72)
	at org.apache.http.impl.client.CloseableHttpClient.execute(CloseableHttpClient.java:57)
	at org.archive.modules.fetcher.FetchHTTPRequest.execute(FetchHTTPRequest.java:751)
	at org.archive.modules.fetcher.FetchHTTP.innerProcess(FetchHTTP.java:658)
	at org.archive.modules.Processor.innerProcessResult(Processor.java:175)
	at org.archive.modules.Processor.process(Processor.java:142)
	at org.archive.modules.ProcessorChain.process(ProcessorChain.java:131)
	at org.archive.crawler.framework.ToeThread.run(ToeThread.java:148)
Caused by: java.net.SocketTimeoutException: connect timed out
	at java.net.PlainSocketImpl.socketConnect(Native Method)
	at java.net.AbstractPlainSocketImpl.doConnect(AbstractPlainSocketImpl.java:345)
	at java.net.AbstractPlainSocketImpl.connectToAddress(AbstractPlainSocketImpl.java:206)
	at java.net.AbstractPlainSocketImpl.connect(AbstractPlainSocketImpl.java:188)
	at java.net.SocksSocketImpl.connect(SocksSocketImpl.java:392)
	at java.net.Socket.connect(Socket.java:589)
	at org.apache.http.conn.socket.PlainConnectionSocketFactory.connectSocket(PlainConnectionSocketFactory.java:72)
	at org.apache.http.impl.conn.HttpClientConnectionOperator.connect(HttpClientConnectionOperator.java:125)
	... 14 more
 org.apache.http.conn.ConnectTimeoutException: Connect to jigsaw.w3.org [/128.30.52.21] failed: connect timed out
	at org.apache.http.impl.conn.HttpClientConnectionOperator.connect(HttpClientConnectionOperator.java:134)
	at org.apache.http.impl.conn.BasicHttpClientConnectionManager.connect(BasicHttpClientConnectionManager.java:318)
	at org.apache.http.impl.execchain.MainClientExec.establishRoute(MainClientExec.java:363)
	at org.apache.http.impl.execchain.MainClientExec.execute(MainClientExec.java:219)
	at org.apache.http.impl.execchain.ProtocolExec.execute(ProtocolExec.java:195)
	at org.apache.http.impl.execchain.RetryExec.execute(RetryExec.java:86)
	at org.apache.http.impl.client.InternalHttpClient.doExecute(InternalHttpClient.java:184)
	at org.apache.http.impl.client.CloseableHttpClient.execute(CloseableHttpClient.java:72)
	at org.apache.http.impl.client.CloseableHttpClient.execute(CloseableHttpClient.java:57)
	at org.archive.modules.fetcher.FetchHTTPRequest.execute(FetchHTTPRequest.java:751)
	at org.archive.modules.fetcher.FetchHTTP.innerProcess(FetchHTTP.java:658)
	at org.archive.modules.Processor.innerProcessResult(Processor.java:175)
	at org.archive.modules.Processor.process(Processor.java:142)
	at org.archive.modules.ProcessorChain.process(ProcessorChain.java:131)
	at org.archive.crawler.framework.ToeThread.run(ToeThread.java:148)
Caused by: java.net.SocketTimeoutException: connect timed out
	at java.net.PlainSocketImpl.socketConnect(Native Method)
	at java.net.AbstractPlainSocketImpl.doConnect(AbstractPlainSocketImpl.java:345)
	at java.net.AbstractPlainSocketImpl.connectToAddress(AbstractPlainSocketImpl.java:206)
	at java.net.AbstractPlainSocketImpl.connect(AbstractPlainSocketImpl.java:188)
	at java.net.SocksSocketImpl.connect(SocksSocketImpl.java:392)
	at java.net.Socket.connect(Socket.java:589)
	at org.apache.http.conn.socket.PlainConnectionSocketFactory.connectSocket(PlainConnectionSocketFactory.java:72)
	at org.apache.http.impl.conn.HttpClientConnectionOperator.connect(HttpClientConnectionOperator.java:125)
	... 14 more

metadata://netarkivet.dk/crawl/logs/preselector.log?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 1469
2016-12-05T10:11:04.743Z ACCEPT http://www.kaarefc.dk/
2016-12-05T10:11:04.857Z ACCEPT dns:www.kaarefc.dk
2016-12-05T10:11:06.634Z ACCEPT http://www.kaarefc.dk/
2016-12-05T10:11:06.669Z ACCEPT http://www.kaarefc.dk/robots.txt
2016-12-05T10:11:09.090Z ACCEPT http://www.kaarefc.dk/
2016-12-05T10:11:09.284Z ACCEPT http://www.w3.org/Icons/valid-xhtml10-blue
2016-12-05T10:11:09.316Z ACCEPT dns:www.w3.org
2016-12-05T10:11:09.657Z ACCEPT http://www.kaarefc.dk/favicon.ico
2016-12-05T10:11:10.465Z ACCEPT http://www.w3.org/Icons/valid-xhtml10-blue
2016-12-05T10:11:10.484Z ACCEPT http://www.kaarefc.dk/style.css
2016-12-05T10:11:10.573Z ACCEPT http://www.w3.org/robots.txt
2016-12-05T10:11:11.660Z ACCEPT http://www.kaarefc.dk/avatar.png
2016-12-05T10:11:11.661Z ACCEPT http://www.w3.org/Icons/valid-xhtml10-blue
2016-12-05T10:11:12.667Z ACCEPT http://jigsaw.w3.org/css-validator/images/vcss-blue
2016-12-05T10:11:12.697Z ACCEPT dns:jigsaw.w3.org
2016-12-05T10:11:14.586Z ACCEPT http://jigsaw.w3.org/css-validator/images/vcss-blue
2016-12-05T10:11:14.614Z ACCEPT http://jigsaw.w3.org/robots.txt
2016-12-05T10:11:34.646Z ACCEPT http://jigsaw.w3.org/css-validator/images/vcss-blue
2016-12-05T10:11:35.332Z ACCEPT http://www.w3.org/favicon.ico
2016-12-05T10:11:37.571Z ACCEPT http://www.w3.org/Icons/valid-xhtml10-blue.png
2016-12-05T10:11:39.822Z ACCEPT http://jigsaw.w3.org/css-validator/images/vcss-blue.gif
2016-12-05T10:11:40.670Z ACCEPT http://jigsaw.w3.org/favicon.ico

metadata://netarkivet.dk/crawl/logs/progress-statistics.log?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 1114
           timestamp  discovered      queued   downloaded       doc/s(avg)  KB/s(avg)   dl-failures   busy-thread   mem-use-KB  heap-size-KB   congestion   max-depth   avg-depth
2016-12-05T10:11:01Z           0           0            0           0(NaN)       0(0)             0             0        27112         83456            0          -1           0
2016-12-05T10:11:01Z CRAWL RUNNING - Preparing
2016-12-05T10:11:04Z CRAWL RUNNING - Running
2016-12-05T10:11:21Z          14           4           10        0.5(0.51)       6(6)             0             1        90495        148480            1           4           4
2016-12-05T10:11:41Z          16           0           15       0.25(0.38)       0(3)             1             0       121019        148480            1           0           0
2016-12-05T10:11:41Z CRAWL ENDING - Finished
2016-12-05T10:11:41Z CRAWL EMPTY - Running
2016-12-05T10:11:43Z          16           0           15          0(0.36)       0(3)             1             0       125551        148480          NaN           0           0
2016-12-05T10:11:43Z CRAWL ENDED - Finished

metadata://netarkivet.dk/crawl/logs/runtime-errors.log?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 0

metadata://netarkivet.dk/crawl/logs/scope.log?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 9085
2016-12-05T10:11:04.741Z 1 NASSurtPrefixedDecideRule ACCEPT http://www.kaarefc.dk/ {"seed":"www.kaarefc.dk","host":"www.kaarefc.dk","hopPath":""}
2016-12-05T10:11:04.771Z 8 PrerequisiteAcceptDecideRule ACCEPT dns:www.kaarefc.dk {"seed":"www.kaarefc.dk","host":"dns:","hopPath":"P","via":"http://www.kaarefc.dk/"}
2016-12-05T10:11:04.856Z 8 PrerequisiteAcceptDecideRule ACCEPT dns:www.kaarefc.dk {"seed":"www.kaarefc.dk","host":"dns:","hopPath":"P","via":"http://www.kaarefc.dk/"}
2016-12-05T10:11:06.634Z 1 NASSurtPrefixedDecideRule ACCEPT http://www.kaarefc.dk/ {"seed":"www.kaarefc.dk","host":"www.kaarefc.dk","hopPath":""}
2016-12-05T10:11:06.639Z 8 PrerequisiteAcceptDecideRule ACCEPT http://www.kaarefc.dk/robots.txt {"seed":"www.kaarefc.dk","host":"www.kaarefc.dk","hopPath":"P","via":"http://www.kaarefc.dk/"}
2016-12-05T10:11:06.667Z 8 PrerequisiteAcceptDecideRule ACCEPT http://www.kaarefc.dk/robots.txt {"seed":"www.kaarefc.dk","host":"www.kaarefc.dk","hopPath":"P","via":"http://www.kaarefc.dk/"}
2016-12-05T10:11:09.090Z 1 NASSurtPrefixedDecideRule ACCEPT http://www.kaarefc.dk/ {"seed":"www.kaarefc.dk","host":"www.kaarefc.dk","hopPath":""}
2016-12-05T10:11:09.190Z 3 TransclusionDecideRule ACCEPT http://www.kaarefc.dk/favicon.ico {"seed":"www.kaarefc.dk","host":"www.kaarefc.dk","hopPath":"I","via":"http://www.kaarefc.dk/"}
2016-12-05T10:11:09.205Z 3 TransclusionDecideRule ACCEPT http://www.kaarefc.dk/style.css {"seed":"www.kaarefc.dk","host":"www.kaarefc.dk","hopPath":"E","via":"http://www.kaarefc.dk/"}
2016-12-05T10:11:09.217Z 9 SchemeNotInSetDecideRule REJECT mailto:mailweb {"seed":"www.kaarefc.dk","host":"-","hopPath":"L","via":"http://www.kaarefc.dk/"}
2016-12-05T10:11:09.221Z 3 TransclusionDecideRule ACCEPT http://www.kaarefc.dk/avatar.png {"seed":"www.kaarefc.dk","host":"www.kaarefc.dk","hopPath":"E","via":"http://www.kaarefc.dk/"}
2016-12-05T10:11:09.235Z 0 RejectDecideRule REJECT http://validator.w3.org/check?uri=referer {"seed":"www.kaarefc.dk","host":"validator.w3.org","hopPath":"L","via":"http://www.kaarefc.dk/"}
2016-12-05T10:11:09.240Z 3 TransclusionDecideRule ACCEPT http://www.w3.org/Icons/valid-xhtml10-blue {"seed":"www.kaarefc.dk","host":"www.w3.org","hopPath":"E","via":"http://www.kaarefc.dk/"}
2016-12-05T10:11:09.254Z 0 RejectDecideRule REJECT http://jigsaw.w3.org/css-validator/check/referer {"seed":"www.kaarefc.dk","host":"jigsaw.w3.org","hopPath":"L","via":"http://www.kaarefc.dk/"}
2016-12-05T10:11:09.258Z 3 TransclusionDecideRule ACCEPT http://jigsaw.w3.org/css-validator/images/vcss-blue {"seed":"www.kaarefc.dk","host":"jigsaw.w3.org","hopPath":"E","via":"http://www.kaarefc.dk/"}
2016-12-05T10:11:09.283Z 3 TransclusionDecideRule ACCEPT http://www.w3.org/Icons/valid-xhtml10-blue {"seed":"www.kaarefc.dk","host":"www.w3.org","hopPath":"E","via":"http://www.kaarefc.dk/"}
2016-12-05T10:11:09.286Z 8 PrerequisiteAcceptDecideRule ACCEPT dns:www.w3.org {"seed":"www.kaarefc.dk","host":"dns:","hopPath":"EP","via":"http://www.w3.org/Icons/valid-xhtml10-blue"}
2016-12-05T10:11:09.315Z 8 PrerequisiteAcceptDecideRule ACCEPT dns:www.w3.org {"seed":"www.kaarefc.dk","host":"dns:","hopPath":"EP","via":"http://www.w3.org/Icons/valid-xhtml10-blue"}
2016-12-05T10:11:09.656Z 3 TransclusionDecideRule ACCEPT http://www.kaarefc.dk/favicon.ico {"seed":"www.kaarefc.dk","host":"www.kaarefc.dk","hopPath":"I","via":"http://www.kaarefc.dk/"}
2016-12-05T10:11:10.465Z 3 TransclusionDecideRule ACCEPT http://www.w3.org/Icons/valid-xhtml10-blue {"seed":"www.kaarefc.dk","host":"www.w3.org","hopPath":"E","via":"http://www.kaarefc.dk/"}
2016-12-05T10:11:10.468Z 8 PrerequisiteAcceptDecideRule ACCEPT http://www.w3.org/robots.txt {"seed":"www.kaarefc.dk","host":"www.w3.org","hopPath":"EP","via":"http://www.w3.org/Icons/valid-xhtml10-blue"}
2016-12-05T10:11:10.484Z 3 TransclusionDecideRule ACCEPT http://www.kaarefc.dk/style.css {"seed":"www.kaarefc.dk","host":"www.kaarefc.dk","hopPath":"E","via":"http://www.kaarefc.dk/"}
2016-12-05T10:11:10.556Z 3 TransclusionDecideRule ACCEPT http://www.kaarefc.dk/favicon.ico {"seed":"www.kaarefc.dk","host":"www.kaarefc.dk","hopPath":"EI","via":"http://www.kaarefc.dk/style.css"}
2016-12-05T10:11:10.572Z 8 PrerequisiteAcceptDecideRule ACCEPT http://www.w3.org/robots.txt {"seed":"www.kaarefc.dk","host":"www.w3.org","hopPath":"EP","via":"http://www.w3.org/Icons/valid-xhtml10-blue"}
2016-12-05T10:11:10.809Z 3 TransclusionDecideRule ACCEPT http://www.w3.org/favicon.ico {"seed":"www.kaarefc.dk","host":"www.w3.org","hopPath":"EPI","via":"http://www.w3.org/robots.txt"}
2016-12-05T10:11:11.659Z 3 TransclusionDecideRule ACCEPT http://www.kaarefc.dk/avatar.png {"seed":"www.kaarefc.dk","host":"www.kaarefc.dk","hopPath":"E","via":"http://www.kaarefc.dk/"}
2016-12-05T10:11:11.661Z 3 TransclusionDecideRule ACCEPT http://www.w3.org/Icons/valid-xhtml10-blue {"seed":"www.kaarefc.dk","host":"www.w3.org","hopPath":"E","via":"http://www.kaarefc.dk/"}
2016-12-05T10:11:12.124Z 3 TransclusionDecideRule ACCEPT http://www.w3.org/Icons/valid-xhtml10-blue.png {"seed":"www.kaarefc.dk","host":"www.w3.org","hopPath":"EI","via":"http://www.w3.org/Icons/valid-xhtml10-blue"}
2016-12-05T10:11:12.142Z 3 TransclusionDecideRule ACCEPT http://www.w3.org/favicon.ico {"seed":"www.kaarefc.dk","host":"www.w3.org","hopPath":"EI","via":"http://www.w3.org/Icons/valid-xhtml10-blue"}
2016-12-05T10:11:12.310Z 3 TransclusionDecideRule ACCEPT http://www.kaarefc.dk/favicon.ico {"seed":"www.kaarefc.dk","host":"www.kaarefc.dk","hopPath":"EI","via":"http://www.kaarefc.dk/avatar.png"}
2016-12-05T10:11:12.665Z 3 TransclusionDecideRule ACCEPT http://jigsaw.w3.org/css-validator/images/vcss-blue {"seed":"www.kaarefc.dk","host":"jigsaw.w3.org","hopPath":"E","via":"http://www.kaarefc.dk/"}
2016-12-05T10:11:12.670Z 8 PrerequisiteAcceptDecideRule ACCEPT dns:jigsaw.w3.org {"seed":"www.kaarefc.dk","host":"dns:","hopPath":"EP","via":"http://jigsaw.w3.org/css-validator/images/vcss-blue"}
2016-12-05T10:11:12.696Z 8 PrerequisiteAcceptDecideRule ACCEPT dns:jigsaw.w3.org {"seed":"www.kaarefc.dk","host":"dns:","hopPath":"EP","via":"http://jigsaw.w3.org/css-validator/images/vcss-blue"}
2016-12-05T10:11:14.586Z 3 TransclusionDecideRule ACCEPT http://jigsaw.w3.org/css-validator/images/vcss-blue {"seed":"www.kaarefc.dk","host":"jigsaw.w3.org","hopPath":"E","via":"http://www.kaarefc.dk/"}
2016-12-05T10:11:14.590Z 8 PrerequisiteAcceptDecideRule ACCEPT http://jigsaw.w3.org/robots.txt {"seed":"www.kaarefc.dk","host":"jigsaw.w3.org","hopPath":"EP","via":"http://jigsaw.w3.org/css-validator/images/vcss-blue"}
2016-12-05T10:11:14.614Z 8 PrerequisiteAcceptDecideRule ACCEPT http://jigsaw.w3.org/robots.txt {"seed":"www.kaarefc.dk","host":"jigsaw.w3.org","hopPath":"EP","via":"http://jigsaw.w3.org/css-validator/images/vcss-blue"}
2016-12-05T10:11:34.646Z 3 TransclusionDecideRule ACCEPT http://jigsaw.w3.org/css-validator/images/vcss-blue {"seed":"www.kaarefc.dk","host":"jigsaw.w3.org","hopPath":"E","via":"http://www.kaarefc.dk/"}
2016-12-05T10:11:34.878Z 3 TransclusionDecideRule ACCEPT http://jigsaw.w3.org/css-validator/images/vcss-blue.gif {"seed":"www.kaarefc.dk","host":"jigsaw.w3.org","hopPath":"EI","via":"http://jigsaw.w3.org/css-validator/images/vcss-blue"}
2016-12-05T10:11:34.885Z 3 TransclusionDecideRule ACCEPT http://jigsaw.w3.org/favicon.ico {"seed":"www.kaarefc.dk","host":"jigsaw.w3.org","hopPath":"EI","via":"http://jigsaw.w3.org/css-validator/images/vcss-blue"}
2016-12-05T10:11:35.332Z 3 TransclusionDecideRule ACCEPT http://www.w3.org/favicon.ico {"seed":"www.kaarefc.dk","host":"www.w3.org","hopPath":"EPI","via":"http://www.w3.org/robots.txt"}
2016-12-05T10:11:36.558Z 0 RejectDecideRule REJECT http://www.w3.org/favicon.ico {"seed":"www.kaarefc.dk","host":"www.w3.org","hopPath":"EPII","via":"http://www.w3.org/favicon.ico"}
2016-12-05T10:11:37.571Z 3 TransclusionDecideRule ACCEPT http://www.w3.org/Icons/valid-xhtml10-blue.png {"seed":"www.kaarefc.dk","host":"www.w3.org","hopPath":"EI","via":"http://www.w3.org/Icons/valid-xhtml10-blue"}
2016-12-05T10:11:38.805Z 3 TransclusionDecideRule ACCEPT http://www.w3.org/favicon.ico {"seed":"www.kaarefc.dk","host":"www.w3.org","hopPath":"EII","via":"http://www.w3.org/Icons/valid-xhtml10-blue.png"}
2016-12-05T10:11:39.821Z 3 TransclusionDecideRule ACCEPT http://jigsaw.w3.org/css-validator/images/vcss-blue.gif {"seed":"www.kaarefc.dk","host":"jigsaw.w3.org","hopPath":"EI","via":"http://jigsaw.w3.org/css-validator/images/vcss-blue"}
2016-12-05T10:11:40.071Z 3 TransclusionDecideRule ACCEPT http://jigsaw.w3.org/favicon.ico {"seed":"www.kaarefc.dk","host":"jigsaw.w3.org","hopPath":"EII","via":"http://jigsaw.w3.org/css-validator/images/vcss-blue.gif"}
2016-12-05T10:11:40.670Z 3 TransclusionDecideRule ACCEPT http://jigsaw.w3.org/favicon.ico {"seed":"www.kaarefc.dk","host":"jigsaw.w3.org","hopPath":"EI","via":"http://jigsaw.w3.org/css-validator/images/vcss-blue"}
2016-12-05T10:11:40.901Z 3 TransclusionDecideRule ACCEPT http://jigsaw.w3.org/favicon.ico {"seed":"www.kaarefc.dk","host":"jigsaw.w3.org","hopPath":"EII","via":"http://jigsaw.w3.org/favicon.ico"}

metadata://netarkivet.dk/crawl/logs/uri-errors.log?heritrixVersion=3.3.0-LBS-2016-02&harvestid=1&jobid=3 130.225.27.140 20170125093039 text/plain 0

metadata://netarkivet.dk/crawl/index/cdx?majorversion=2&minorversion=0&harvestid=1&jobid=3&filename=3-1-20161205101105604-00000-14970~sb-test-har-001.statsbiblioteket.dk~8171.arc.gz 130.225.27.140 20170125093039 application/x-cdx 2806
dns:www.kaarefc.dk - 20161205101104 text/dns 52 3-1-20161205101105604-00000-14970~sb-test-har-001.statsbiblioteket.dk~8171.arc.gz 663 1d45be954e63de4c690fc6e6589eb295
http://www.kaarefc.dk/robots.txt - 20161205101106 text/html 447 3-1-20161205101105604-00000-14970~sb-test-har-001.statsbiblioteket.dk~8171.arc.gz 771 c39c0db1ad96eb8f0422769818151719
http://www.kaarefc.dk/ - 20161205101109 text/html 1731 3-1-20161205101105604-00000-14970~sb-test-har-001.statsbiblioteket.dk~8171.arc.gz 1164 e0d4abbe46984f990a9586b686b43ead
dns:www.w3.org - 20161205101109 text/dns 51 3-1-20161205101105604-00000-14970~sb-test-har-001.statsbiblioteket.dk~8171.arc.gz 2092 d8316609ba4e51ccb9cb8083db2603bb
http://www.kaarefc.dk/favicon.ico - 20161205101109 text/html 448 3-1-20161205101105604-00000-14970~sb-test-har-001.statsbiblioteket.dk~8171.arc.gz 2194 f17acaf929c60c9a4620b92e4963c79b
http://www.kaarefc.dk/style.css - 20161205101110 text/css 1335 3-1-20161205101105604-00000-14970~sb-test-har-001.statsbiblioteket.dk~8171.arc.gz 2590 61e9c5d6f9cd4ba276ca94c25e77a3a8
http://www.w3.org/robots.txt - 20161205101110 text/plain 3287 3-1-20161205101105604-00000-14970~sb-test-har-001.statsbiblioteket.dk~8171.arc.gz 3241 cf8020609ff0de352a44184ef2b6eecf
http://www.w3.org/Icons/valid-xhtml10-blue - 20161205101111 image/png 2494 3-1-20161205101105604-00000-14970~sb-test-har-001.statsbiblioteket.dk~8171.arc.gz 4440 f8d456bad90bc37e2fa26eb8c0fd46d9
http://www.kaarefc.dk/avatar.png - 20161205101111 image/png 129391 3-1-20161205101105604-00000-14970~sb-test-har-001.statsbiblioteket.dk~8171.arc.gz 6496 059329b5fd522bd994b80ccfb9f68254
dns:jigsaw.w3.org - 20161205101112 text/dns 54 3-1-20161205101105604-00000-14970~sb-test-har-001.statsbiblioteket.dk~8171.arc.gz 136004 e4d3faac42cec4f7867e31e3f877f161
http://jigsaw.w3.org/css-validator/images/vcss-blue - 20161205101134 image/gif 2164 3-1-20161205101105604-00000-14970~sb-test-har-001.statsbiblioteket.dk~8171.arc.gz 136110 e08322797fb6ef200d720c38279a61d2
http://www.w3.org/favicon.ico - 20161205101135 image/vnd.microsoft.icon 6909 3-1-20161205101105604-00000-14970~sb-test-har-001.statsbiblioteket.dk~8171.arc.gz 137932 0e9e4cade0087ffe3d159bb0f0f4f92f
http://www.w3.org/Icons/valid-xhtml10-blue.png - 20161205101137 image/png 2408 3-1-20161205101105604-00000-14970~sb-test-har-001.statsbiblioteket.dk~8171.arc.gz 139666 5691ee69b86944269551328ba872e2c5
http://jigsaw.w3.org/css-validator/images/vcss-blue.gif - 20161205101139 image/gif 2075 3-1-20161205101105604-00000-14970~sb-test-har-001.statsbiblioteket.dk~8171.arc.gz 141676 cf8a33f742aca172c3831bf0740e4328
http://jigsaw.w3.org/favicon.ico - 20161205101140 application/octet-stream 553 3-1-20161205101105604-00000-14970~sb-test-har-001.statsbiblioteket.dk~8171.arc.gz 143475 307ad6fcedc4927e99a4ab770cc39856

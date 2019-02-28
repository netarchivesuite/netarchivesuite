The purpose of this module is to run a local instance of the wayback-indexer. This will make it possible for us to develop a Hadoop version instead.


NAS is running on <http://kb-test-adm-001.kb.dk:8078/>

Config is at `ssh devel@kb-prod-udv-001.kb.dk:/home/devel/prepared_software/TEST1/sb-test-har-001.statsbiblioteket.dk`

The version of the running NAS code is
<https://github.com/netarchivesuite/netarchivesuite/commit/bcb1233a7ab6325d68fa64e34dfb51bf984e9641>

Devel environment:
<https://sbprojects.statsbiblioteket.dk/pages/viewpage.action?pageId=37597440>


Pakke: wayback-indexer
Klasse: ArchiveFile

Note, use the port-forwarder to get the nessesary access.

Note: we changed `settings_BitarchiveMonitorApplication_SBBM.xml` to use `HTTPRemoteFile` on port 8075, which just happened to not be firewalled and not used by anyone.
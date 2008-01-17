#!/usr/bin/perl -w

use strict;

# Run this with -n -i

while (<>) {
    if (/force-queue-assignment/) {
	print;
	print <<EOF
            <boolean name="pause-at-start">false</boolean>
            <boolean name="pause-at-finish">false</boolean>
            <boolean name="recovery-log-enabled">false</boolean>
            <boolean name="hold-queues">true</boolean>
            <integer name="balance-replenish-amount">3000</integer>
            <integer name="error-penalty-amount">100</integer>
            <long name="queue-total-budget">-1</long>
            <string name="cost-policy">org.archive.crawler.frontier.UnitCostAssignmentPolicy</string>
            <long name="snooze-deactivate-ms">300000</long>
            <string name="uri-included-structure">org.archive.crawler.util.BdbUriUniqFilter</string>
EOF
	} elsif (/pre-fetch-processors/) {
	    print;
	    print <<EOF
		<newObject name="QuotaEnforcer" class="org.archive.crawler.prefetch.QuotaEnforcer">
                <boolean name="enabled">true</boolean>
                <map name="filters">
                </map>
		<long name="server-max-fetch-successes">-1</long>
		<long name="server-max-success-kb">-1</long>
		<long name="host-max-fetch-successes">-1</long>
		<long name="host-max-success-kb">-1</long>
		<long name="server-max-fetch-successes">-1</long>
		<long name="group-max-success-kb">-1</long>
		</newObject>
EOF
	    } elsif (/robot-validity-duration/) {
		print;
		print <<EOF
		    <boolean name="calculate-robots-only">false</boolean>
EOF
		} elsif (/ExtractorHTMLNoForms/) {
# Notice original line being dropped.
		    print <<EOF
			<newObject name="ExtractorHTML" class="org.archive.crawler.extractor.ExtractorHTML">
			<boolean name="ignore-form-action-urls">true</boolean>
EOF
		    } elsif (/recover-path/) {
			print;
			print <<EOF
			    <boolean name="checkpoint-copy-bdbje-logs">true</boolean>
EOF
			} else {
			    print;
			}
}

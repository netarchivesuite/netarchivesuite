#!/usr/bin/env bash
# Start vagrant with NAS latest version (default) or optional other version

program=`basename $0`
usage="usage: ${program}"$'\n'"       ${program} -b<branch-name>"$'\n'"       ${program} -l"$'\n'

set -u  # Give error msg on use of unset variables or exit with non-zero status


if [ $# -gt 1 ]; then
	printf "${program}: illegal number of operands\n${usage}"
	exit 1
fi


if [ $# -eq 0 ]; then
	# No arguments -> default case
	echo 'mvn org.apache.maven.plugins:maven-dependency-plugin:2.9:get -DrepoUrl=https://sbforge.org/nexus/content/repositories/snapshots/ -Dartifact=org.netarchivesuite:distribution:LATEST:zip -Dtransitive=false -Ddest=NetarchiveSuite.zip; mvn org.apache.maven.plugins:maven-dependency-plugin:2.9:get -DrepoUrl=https://sbforge.org/nexus/content/repositories/snapshots/ -Dartifact=org.netarchivesuite:heritrix3-bundler:LATEST:zip -Dtransitive=false -Ddest=NetarchiveSuite-heritrix3-bundler.zip' >'Vagrantfile-include'
	version='the latest distribution of Netarchive suite'
else
	# Exactly one command-line argument

    bflag=0
    lflag=0
	# Read command line option -b <branch-name>
	while getopts 'b:l' option; do     # The : signals that -b needs an argument
		case "$option" in
			l)
				lflag=1
				;;
			b)
				bflag=1
				bargument=("$OPTARG")
				;;
			?)
				printf "${usage}"
				exit 1
				;;
		esac
	done
	# Now no more options were found, and OPTIND equals the index of the first
	# non-option argument found. Errors encountered were reported by getopts.

	if [ $bflag -eq 1 ]; then
		# There was a -b option

		# Result of this will be empty iff branch was not found
		getbranchhits="git ls-remote --heads https://github.com/netarchivesuite/netarchivesuite.git $bargument"
        existingbranch=`$getbranchhits`

        if [ -n "$existingbranch" ]; then
            # The branch exists
            echo 'Confirmed: the branch exists, all well, continuing...'

            # Insert code into Vagrantfile-include that retrieves a local copy
            # of the requested branch
            # git clone -b <branch> --depth 1 <remote_repo>
            echo "pwd; mkdir gitting; cd gitting; git clone -b $bargument --depth 1 https://github.com/netarchivesuite/netarchivesuite.git" >'Vagrantfile-include'

            # Build the newly cloned NAS using maven
            #echo ''>'Vagrantfile-include'
            echo 'echo 'PWDing'; pwd; cd netarchivesuite; mvn -DskipTests clean package' >>'Vagrantfile-include'

            version="Netarchive suite from branch $bargument"
        else
            echo "error: unknown branch '$bargument'"
            exit 1
        fi
	elif [ $lflag -eq 1 ]; then
		# There was a -l option
        # Insert code into Vagrantfile-include that uses the local copy
        # git clone -l --depth 1 <remote_repo>
        echo "pwd; mkdir gitting; cd gitting; git clone -l --depth 1 https://github.com/netarchivesuite/netarchivesuite.git" >'Vagrantfile-include'
        echo 'echo 'PWDing'; pwd; cd netarchivesuite; mvn -DskipTests clean package' >>'Vagrantfile-include'
        version="Netarchive suite from current local version"
	else
 		printf "${usage}"
		exit 1
	fi
fi


# Destroy any existing vagrant and startup a new, redirecting top-level
# "progress" to stderr, so user can redirect output of this script to a file,
# and thus see the short overview without the heaps of output done by the
# vagrant up.
date
echo "Installing $version, hang on..."
echo "Progress can be followed in file 'vagrant-up-output.txt'"
echo 'Running: vagrant destroy'
vagrant destroy -f
echo 'Running: vagrant up'
vagrant up >vagrant-up-output.txt
echo 'Vagrant is up!'
echo 'To build from a totally clean vagrant, do'
echo '   vagrant destroy -f; vagrant box remove "ubuntu/precise64"'
echo 'before running this script.'
date

#!/bin/bash

if [ "$USER" == "vagrant" ] ; then
  cd /home/vagrant/gitting/netarchivesuite
  sudo git pull
  sudo mvn -B -q -DskipTests clean package
  sudo rm /home/vagrant/netarchive/NetarchiveSuite*.zip
  sudo rm -rf /home/vagrant/netarchive/deploy
  sudo mv /home/vagrant/gitting/netarchivesuite/harvester/heritrix3/heritrix3-bundler/target/NetarchiveSuite-heritrix3-bundler*.zip /home/vagrant/netarchive/NetarchiveSuite-heritrix3-bundler.zip
  sudo mv /home/vagrant/gitting/netarchivesuite/deploy/distribution/target/NetarchiveSuite-*.zip /home/vagrant/netarchive/NetarchiveSuite.zip
  cd /home/vagrant/netarchive/
  ssh test@localhost QUICKSTART/conf/killall.sh
  sudo ./RunNetarchiveSuite.sh NetarchiveSuite.zip deploy_standalone_vagrant_example.xml deploy NetarchiveSuite-heritrix3-bundler.zip
else
  vagrant ssh -c /home/vagrant/gitting/netarchivesuite/quickstart-vagrant-environment/redeploy.sh
fi

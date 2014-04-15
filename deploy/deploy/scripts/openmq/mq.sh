#!/bin/sh

INSTALLDIR=~/openmq4.5
echo InstallDir:$INSTALLDIR

installMQ()
{
    echo InstallDir:$INSTALLDIR
    if [ -d "$INSTALLDIR" ]
    then
      echo "Installing openmq in $INSTALLDIR."
      #mkdir $INSTALLDIR
      #cd $INSTALLDIR
      #wget http://download.java.net/mq/open-mq/4.5.2/latest/openmq4_5_2-binary-Linux_X86.zip
      #unzip openmq4*.zip
      startBroker
      stopBroker
    else
      echo "Openmq already installed."
    fi
}

setupEnvironment()
{
    export IMQ_HOME=$INSTALLDIR/mq
    export IMQ_VARHOME=$INSTALLDIR/var
    export IMQ_ETCHOME=$INSTALLDIR/etc
}

startBroker()
{
    $INSTALLDIR/mq/bin/imqbrokerd &
    sleep 3
    echo Broker started
}

stopBroker()
{
    pkill -f "com.sun.messaging.jmq.jmsserver.Broker"
    sleep 3
    echo Broker stopped
}

case $1 in
    install)
        installMQ
        ;;
    start)
        startBroker
        ;;
    stop)
        stopBroker
        ;;
    status)
        ;;
    *)
        echo "usage: $0 { install | start | stop | status }"
        ;;
esac
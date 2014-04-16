#!/bin/sh

INSTALLDIR=${installdir:-openmq4.5}
echo "Using installdir=$INSTALLDIR"

installMQ()
{
    if [ ! -d "$INSTALLDIR" ]
    then
      echo "Installing openmq."
      mkdir $INSTALLDIR
      cd $INSTALLDIR
      wget http://download.java.net/mq/open-mq/4.5.2/latest/openmq4_5_2-binary-Linux_X86.zip
      unzip openmq4*.zip
      cd -

      echo "Initial start of broker to create configurations."
      startBroker
      stopBroker

      echo "Customizing broker configuration."
      updateConfig
    else
      echo "Openmq already installed."
    fi
    startBroker
}

updateConfig()
{
    line="imq.autocreate.queue.maxNumActiveConsumers"
    configfile="$INSTALLDIR/var/mq/instances/imqbroker/props/config.properties"

    # Uncomment line
    sed -i "/${line}/ s/# *//" $configfile
    echo "Set maxNumActiveConsumers to 20."
    sed -i "/maxNumActiveConsumers/s/$/\=20&/" $configfile
}

startBroker()
{
    if pgrep -f "com.sun.messaging.jmq.jmsserver.Broker" >/dev/null 2>&1
        then
            echo "Broker is already running."
        else
            echo "Starting broker"
            $INSTALLDIR/mq/bin/imqbrokerd &
            sleep 3
            echo "Broker started"
        fi
}

stopBroker()
{
    if pgrep -f "com.sun.messaging.jmq.jmsserver.Broker" >/dev/null 2>&1
        then
            echo "Stopping broker"
            pkill -f "com.sun.messaging.jmq.jmsserver.Broker"
            sleep 3
            echo "Broker stopped"
        else
            echo "Broker is not running"
    fi
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
        if pgrep -f "com.sun.messaging.jmq.jmsserver.Broker" >/dev/null 2>&1
        then
            echo "Broker is running."
        else
            echo "Broker is not running."
        fi
        ;;
    *)
        echo "usage: $0 { install | start | stop | status }"
        ;;
esac
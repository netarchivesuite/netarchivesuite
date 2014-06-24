package dk.netarkivet.common.utils;

import dk.netarkivet.common.utils.CleanupIF;

public class HelloWorld extends Thread implements CleanupIF {

    private static HelloWorld theHelloWorld;

    private HelloWorld() {
    }

    public void cleanup() {
        theHelloWorld = null;

    }

    public static HelloWorld getInstance() {
        if (theHelloWorld == null) {
            theHelloWorld = new HelloWorld();
        }
        return theHelloWorld;
    }
    public void run() {
      while (true) {
          System.err.println("Hello World!");
      }
    }

}

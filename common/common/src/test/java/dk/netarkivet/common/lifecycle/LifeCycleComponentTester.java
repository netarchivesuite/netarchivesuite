package dk.netarkivet.common.lifecycle;

import org.junit.Test;

public class LifeCycleComponentTester {

    @Test
    public void canLifeCycleComponentStartAndShutdown() {
        LifeCycleComponent lcc = new LifeCycleComponent();
        lcc.addChild(new OneTestLifeCycle());
        lcc.addChild(new AnotherTestLifeCycle());
        lcc.start();
        lcc.shutdown();
    }
    
    static private class OneTestLifeCycle implements ComponentLifeCycle {

        @Override
        public void start() {
            
        }

        @Override
        public void shutdown() {
            
        }
        
    }
    
    static private class AnotherTestLifeCycle implements ComponentLifeCycle {

        @Override
        public void start() {
            
        }

        @Override
        public void shutdown() {
            
        }       
    }
}

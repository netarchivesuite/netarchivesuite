package dk.netarkivet.common.lifecycle;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/** 
 * Takes care of the lifecycling of subcomponents(children). 
 * 
 * When extending this class you must: <ol>
 * <li> Add all lifecycle subcomponents with the addChild, before the start 
 * method is called.
 * <li> Call the <code>super.start()</code> operation to start the children.
 * <li> Call the <code>super.shutdown</code> operation to  
 */
public class LifeCycleComponent implements ComponentLifeCycle { 
    /** The child-components of this lifecycle. */
    private List<ComponentLifeCycle> children 
        = new ArrayList<ComponentLifeCycle>();
    /** The instance logger. */
    private final Log log = LogFactory.getLog(getClass().getName());

    @Override
    public void start() {
        log.debug("Starting " + toString());
        for (ComponentLifeCycle child: children) {
            child.start();
        }
    }
    
    @Override
    public void shutdown() {
        log.debug("Shutting down " + toString());
        for (ComponentLifeCycle child: children) {
            child.shutdown();
        }
    }
    
    /**
     * Adds a child <code>ComponentLifeCycle</code>. The childs lifecycle will 
     * be managed by by the <code>LifeCycleComponent</code>.
     * @param childComponent The child to add
     */
    public void addChild(ComponentLifeCycle childComponent) {
        ArgumentNotValid.checkNotNull(childComponent, "Child can not be null");
        children.add(childComponent);        
    }
}

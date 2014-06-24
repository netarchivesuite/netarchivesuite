package dk.netarkivet.archive.arcrepositoryadmin;

import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

import java.util.Date;

/** This class contains a storestate, and the time,
  *  when it was last set.
  */
public class ArchiveStoreState {

    /** The state for a specific archive, or overall state. */
    private ReplicaStoreState storestate;

    /** Time of last state change. */
    private Date lastchanged;

    /**
     * Constructor for this class.
     * This sets the lastchanged value to Now.
     * @param storestate A BitArchiveStoreState
     */
    public ArchiveStoreState(ReplicaStoreState storestate) {
        setState(storestate);
    }

    /**
     * Constructor for this class.
     * @param storestate A BitArchiveStoreState
     * @param lastchanged Time for when this state was set
     */
    public ArchiveStoreState(ReplicaStoreState storestate,
            Date lastchanged) {
        setState(storestate, lastchanged);
    }

    /***
     * Return the current BitArchiveStoreState.
     * @return the current BitArchiveStoreState
     */
    public ReplicaStoreState getState(){
        return storestate;
    }

    /**
     * Sets the current ReplicaStoreState.
     * @param state The ReplicaStoreState.
     * @param lastDate The lastchanged date.
     * @throws ArgumentNotValid If the state or the lastDate is null.
     */
    public void setState(ReplicaStoreState state, Date lastDate) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(state, "ReplicaStoreState state");
        ArgumentNotValid.checkNotNull(lastDate, "Date lastDate");
    
        this.storestate = state;
        this.lastchanged = lastDate;
    }

    /**
     * Sets the current ReplicaStoreState.
     * As a sideeffect lastchanged is set to NOW.
     * 
     * @param state the ReplicaStoreState.
     * @throws ArgumentNotValid If the state is null.
     */
    public void setState(ReplicaStoreState state) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(state, "ReplicaStoreState state");
        this.storestate = state;
        this.lastchanged = new Date();
    }

    /**
     * Get the Date for when the state was lastchanged.
     * @return the Date for when the state was lastchanged
     */
    public Date getLastChanged() {
        return this.lastchanged;

    }

    /**
     * Creates an string representation of this instance.
     * 
     * @return The string representation of this instance.
     */
    public String toString() {
        String stringRepresentation = getState() + " "
        + getLastChanged().getTime();
        return stringRepresentation;
    }
}

package org.geowebcache.seed;

import java.util.Iterator;

public class JobUtils {
    private JobUtils(){};
    
    public static GWCTask.STATE combineState(Iterator<GWCTask.STATE> states) {

        boolean allReadyUnset = true; // No tasks that aren't READY or UNSET have been seen
        boolean running = false; // At least one running task has been seen
        while(states.hasNext()){
            GWCTask.STATE s = states.next();
            switch(s){
            case FAILED:
                return GWCTask.STATE.FAILED; // TODO not sure this is right, maybe it should only be if all are DEAD.
            case DONE:
                allReadyUnset = false;
                break;
            case READY:
                break;
            case RUNNING:
                allReadyUnset = false;
                running = true;
                break;
            default:
                break;
            }
        }
        // None are dead, some are running, so the job is running
        if(running) {
            return GWCTask.STATE.RUNNING;
        }
        // All are Ready/Unset
        if(allReadyUnset) {
            return GWCTask.STATE.READY;
        }
        // Some are Done, any others are Ready/Unset
        return GWCTask.STATE.DONE;
    }
}

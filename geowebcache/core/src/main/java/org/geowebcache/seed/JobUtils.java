package org.geowebcache.seed;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geowebcache.seed.GWCTask.STATE;

public class JobUtils {
    private static final Log log = LogFactory.getLog(GWCTask.class);
    
    private JobUtils(){};
    
    public static GWCTask.STATE combineState(Iterator<GWCTask.STATE> states, GWCTask.STATE currentState) {
        boolean anyInitializing = false;
        boolean anyRunning = false;
        boolean anyStopped = false;
        boolean anyReady = false;
        while(states.hasNext()){
            GWCTask.STATE s = states.next();
            switch(s){
            case INITIALIZING:
                anyInitializing = true;
                break;
                
            case READY:
                anyReady = true;
                break;
                
            case RUNNING:
                anyRunning = true;
                break;
            
            case FAILED:
            case DONE:
            case ABORTED:
                anyStopped = true;
                break;
            }

        }
        
        if(anyInitializing && (anyRunning||anyStopped)) 
            throw new IllegalStateException("If a Job has an initializing task, all should be initializing or ready.");
        
        if(anyInitializing) {
            return STATE.INITIALIZING;
        }
        
        if(!(anyReady || anyRunning)){
            if(currentState.isStopped()){
                return currentState;
            } else {
                throw new IllegalStateException("Job should have been set to a stopped state when its last thread stopped.");
            }
        }
        if(anyRunning || (anyReady && currentState==STATE.RUNNING)){
            return STATE.RUNNING;
        }
        
        if(anyReady) {
            return STATE.READY;
        }
        
        throw new IllegalStateException("Unexpected task states for job, this should not happen.");
    }
}

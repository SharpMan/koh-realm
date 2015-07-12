
package koh.realm.network;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Alleos13
 */
public class RealmLoader implements Runnable {
    
    private final List<RealmClient> waitingList;
    private final ScheduledExecutorService executor;
    
    public RealmLoader(){
        executor = Executors.newSingleThreadScheduledExecutor();
        this.waitingList = new ArrayList<RealmClient>();
        executor.scheduleWithFixedDelay(this, 20, 20, TimeUnit.MILLISECONDS);
    }
    
    public void addClient(RealmClient t){
        synchronized(waitingList){
            waitingList.add(t);
            waitingList.notify();
        }
    }

    @Override
    public void run() {
        RealmClient toThreat = null;
        synchronized(waitingList){
            while(waitingList.isEmpty()){
                try{
                    waitingList.wait();
                }catch(InterruptedException e){
                }
            }
            toThreat = waitingList.remove(0);
        }
        try{
            if(toThreat!=null){
                toThreat.threatWaiting();
            }
        }catch(Exception e){
        }
        
    }
    
    public int getPosition(RealmClient t) {
        return waitingList.indexOf(t) + 1;
    }

    public int getTotal() {
        return waitingList.size();
    }

    public void onClientDisconnect(RealmClient t) {
        synchronized (waitingList) {
            if (waitingList.contains(t)) {
                waitingList.remove(t);
            }
        }
    }
    
}

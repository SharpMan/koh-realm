package koh.realm.app;

import koh.patterns.services.api.DependsOn;
import koh.patterns.services.api.Service;
import koh.repositories.MemoryFinalizer;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@DependsOn(Loggers.class)
public class MemoryService implements Service {

    private MemoryFinalizer cleaner;

    @Override
    public void start() {
        if(cleaner != null)
            return;

        cleaner = new MemoryFinalizer(1, TimeUnit.HOURS);
    }

    @Override
    public void stop() {
        if(cleaner != null)
            cleaner.dispose();
    }

}

package koh.realm.refact_network;

import com.google.inject.Inject;
import koh.mina.MinaServer;
import koh.mina.api.MinaListener;
import koh.patterns.event.EventExecutor;
import koh.patterns.handler.ConsumerHandlerExecutor;
import koh.patterns.handler.SimpleHandlerExecutor;
import koh.patterns.services.api.DependsOn;
import koh.patterns.services.api.Service;
import koh.protocol.client.Message;
import koh.protocol.client.codec.Dofus2ProtocolDecoder;
import koh.protocol.client.codec.Dofus2ProtocolEncoder;
import koh.realm.app.DatabaseSource;
import koh.realm.app.Logs;
import koh.realm.inter.InterServer;
import koh.realm.network.annotations.RealmPackage;
import koh.realm.utils.Settings;

import java.io.IOException;

@DependsOn({Logs.class, DatabaseSource.class, InterServer.class})
public class RealmServer implements Service, MinaListener<RealmClient> {

    /**
     * 1 * estimated client optimal size (64)
     */
    private static final int DEFAULT_READ_SIZE = 64;

    /**
     * max used client packet size (realm) + additional size for infos of the next packet
     */
    private static final int MAX_READ_SIZE = 4096 + 0xFF;

    private final MinaServer<RealmClient, Message> minaServer;
    private final Settings settings;

    @Inject
    public RealmServer(Settings settings, Logs logs,
                       @RealmPackage ConsumerHandlerExecutor<RealmClient, Message> messages,
                       @RealmPackage EventExecutor events,
                       @RealmPackage SimpleHandlerExecutor<RealmClient> actions,
                       Dofus2ProtocolDecoder decoder,
                       Dofus2ProtocolEncoder encoder) {

        this.settings = settings;
        this.minaServer = new MinaServer<>(RealmClient::new, actions,
                messages, this, Message.class);

        minaServer.configure(decoder, encoder, DEFAULT_READ_SIZE, MAX_READ_SIZE, 30 * 60);
    }

    @Override
    public void start() {
        try {
            minaServer.bind(settings.getStringElement("Login.Host"),
                    settings.getIntElement("Login.Port"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        minaServer.dispose();
    }

    @Override
    public void onException(RealmClient client, Throwable exception) {
        exception.printStackTrace();
    }

    @Override
    public void onMessageSent(RealmClient client, Object message) {
        System.out.println("Sent : " + message);
    }
}

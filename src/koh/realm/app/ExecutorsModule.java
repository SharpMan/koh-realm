package koh.realm.app;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import koh.commons.ImprovedCachedThreadPool;
import koh.inter.InterMessage;
import koh.patterns.event.EventExecutor;
import koh.patterns.handler.ConsumerHandlerExecutor;
import koh.patterns.handler.SimpleHandlerExecutor;
import koh.protocol.client.Message;
import koh.protocol.client.codec.Dofus2ProtocolDecoder;
import koh.protocol.client.codec.Dofus2ProtocolEncoder;
import koh.protocol.messages.connection.BypassIdentificationMessage;
import koh.protocol.messages.connection.HelloConnectMessage;
import koh.protocol.messages.connection.IdentificationMessage;
import koh.protocol.messages.handshake.ProtocolRequired;
import koh.realm.entities.GameServer;
import koh.realm.inter.annotations.InterPackage;
import koh.realm.network.RealmClient;
import koh.realm.network.annotations.RealmPackage;

import java.util.HashMap;

public class ExecutorsModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @InterPackage
    @Provides
    @Singleton
    ConsumerHandlerExecutor<GameServer, InterMessage> provideInterMessagesExecutor() {
        return new ConsumerHandlerExecutor<>();
    }

    @InterPackage @Provides
    @Singleton
    SimpleHandlerExecutor<GameServer> provideInterActionsExecutor() {
        return new SimpleHandlerExecutor<>();
    }

    @InterPackage @Provides
    @Singleton
    EventExecutor provideInterEventsExecutor() {
        return new EventExecutor(new ImprovedCachedThreadPool("InterEventsExecutor", 10, 50));
    }

    @RealmPackage
    @Provides
    @Singleton
    EventExecutor provideRealmEventsExecutor() {
        return new EventExecutor(new ImprovedCachedThreadPool("RealmEventsExecutor", 10, 50));
    }

    @RealmPackage @Provides
    @Singleton
    ConsumerHandlerExecutor<RealmClient, Message> provideRealmMessagesExecutor() {
        return new ConsumerHandlerExecutor<>();
    }

    @RealmPackage @Provides
    @Singleton
    SimpleHandlerExecutor<RealmClient> provideRealmActionsExecutor() {
        return new SimpleHandlerExecutor<>();
    }

    @Provides   @Named("Dofus2MessagesDictionary")
    @Singleton
    HashMap<Integer, Class<? extends Message>> provideDofus2Messages() {
        return new HashMap<Integer, Class<? extends Message>>(){{
            put(HelloConnectMessage.MESSAGE_ID, HelloConnectMessage.class);
            put(ProtocolRequired.MESSAGE_ID, ProtocolRequired.class);
            put(BypassIdentificationMessage.MESSAGE_ID, BypassIdentificationMessage.class);
            put(IdentificationMessage.MESSAGE_ID, IdentificationMessage.class);
        }};
    }

    @Provides
    @Singleton
    Dofus2ProtocolDecoder provideDofus2Decoder(
            @Named("Dofus2MessagesDictionary") HashMap<Integer, Class<? extends Message>> messages ) {
        return new Dofus2ProtocolDecoder(messages);
    }

    @Provides
    @Singleton
    Dofus2ProtocolEncoder provideDofus2Encoder() {
        return new Dofus2ProtocolEncoder();
    }
}

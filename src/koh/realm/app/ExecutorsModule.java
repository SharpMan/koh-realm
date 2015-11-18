package koh.realm.app;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import koh.concurrency.ImprovedCachedThreadPool;
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
import koh.protocol.messages.security.RawDataMessage;
import koh.realm.Main;
import koh.realm.entities.GameServer;
import koh.realm.inter.annotations.InterPackage;
import koh.realm.refact_network.RealmClient;
import koh.realm.refact_network.RealmPackage;
import koh.realm.utils.Settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        //TODO : Pre-gen java file with auto-generated HashMap<messageId, Class<? extends Message> with reflection before runtime
        return new HashMap<Integer, Class<? extends Message>>(){{
            put(HelloConnectMessage.MESSAGE_ID, HelloConnectMessage.class);
            //put(ProtocolRequired.MESSAGE_ID, ProtocolRequired.class);
            //put(BypassIdentificationMessage.MESSAGE_ID, BypassIdentificationMessage.class);
            put(IdentificationMessage.MESSAGE_ID, IdentificationMessage.class);
        }};
    }

    @Provides
    @Singleton
    Dofus2ProtocolDecoder provideDofus2Decoder(
            @Named("Dofus2MessagesDictionary") HashMap<Integer, Class<? extends Message>> messages ) {
        return new Dofus2ProtocolDecoder(messages);
    }

    @Provides @Singleton @Named("Messages.ProtocolRequired")
    private ProtocolRequired provideProtocolRequiredMessage(Settings settings) {
        return new ProtocolRequired(settings.getIntElement("Protocol.requiredVersion"),
                settings.getIntElement("Protocol.currentVersion"));
    }

    @Provides @Singleton @Named("Messages.AuthenticationBypasser")
    private RawDataMessage provideBypassPacketMessage(Settings settings) throws IOException {
        byte[] binaryFile = Files.readAllBytes(Paths.get(settings.getStringElement("Login.BypassPacket")));
        return new RawDataMessage((short)binaryFile.length, binaryFile);
    }

    @Provides
    @Singleton
    Dofus2ProtocolEncoder provideDofus2Encoder() {
        return new Dofus2ProtocolEncoder();
    }
}

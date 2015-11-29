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
import koh.protocol.messages.connection.HelloConnectMessage;
import koh.protocol.messages.connection.IdentificationMessage;
import koh.protocol.messages.connection.ServerSelectionMessage;
import koh.protocol.messages.handshake.ProtocolRequired;
import koh.protocol.messages.security.RawDataMessage;
import koh.realm.intranet.GameServerClient;
import koh.realm.intranet.InterPackage;
import koh.realm.internet.RealmClient;
import koh.realm.internet.RealmPackage;
import koh.realm.utils.Settings;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class CoreModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    Settings provideConfiguration() {
        return new Settings("../koh-realm/Settings.ini");
    }

    @InterPackage
    @Provides
    @Singleton
    ConsumerHandlerExecutor<GameServerClient, InterMessage> provideInterMessagesExecutor() {
        return new ConsumerHandlerExecutor<>();
    }

    @InterPackage @Provides
    @Singleton
    SimpleHandlerExecutor<GameServerClient> provideInterActionsExecutor() {
        return new SimpleHandlerExecutor<>();
    }

    @Provides @Singleton
    EventExecutor provideInterEventsExecutor() {
        return new EventExecutor(new ImprovedCachedThreadPool("RealmServicesEventsExecutor", 10, 50, 5, 100));
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
            put(ServerSelectionMessage.MESSAGE_ID, ServerSelectionMessage.class);
            put(HelloConnectMessage.MESSAGE_ID, HelloConnectMessage.class);
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

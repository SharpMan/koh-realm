package koh.realm.app;

import com.google.inject.*;
import com.google.inject.name.Named;
import koh.protocol.client.Message;
import koh.protocol.client.codec.Dofus2ProtocolDecoder;
import koh.protocol.client.codec.Dofus2ProtocolEncoder;
import koh.protocol.messages.connection.BypassIdentificationMessage;
import koh.protocol.messages.connection.HelloConnectMessage;
import koh.protocol.messages.connection.IdentificationMessage;
import koh.protocol.messages.handshake.ProtocolRequired;
import koh.realm.dao.DAOModule;
import koh.realm.utils.Settings;
import org.apache.mina.core.buffer.CachedBufferAllocator;
import org.apache.mina.core.buffer.IoBufferAllocator;

import java.util.HashMap;

class ServicesModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Logs.class).in(Scopes.SINGLETON);
        bind(DatabaseSource.class).in(Scopes.SINGLETON);

        install(new DAOModule());
    }

    @Provides
    @Singleton
    Settings provideSettings() {
        return new Settings("../koh-realm/Settings.ini");
    }

    @Provides   @Named("EncoderIoBufferAllocator")
    @Singleton
    IoBufferAllocator provideEncoderIoBufferAllocator() {
        return new CachedBufferAllocator(4, 0xFFFF);
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
    Dofus2ProtocolEncoder provideDofus2Encoder(
            @Named("EncoderIoBufferAllocator") IoBufferAllocator allocator ) {
        return new Dofus2ProtocolEncoder(allocator);
    }
}

package koh.realm.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import com.google.inject.Inject;
import koh.inter.InterMessage;
import koh.inter.messages.HelloMessage;
import koh.patterns.handler.ConsumerHandlerExecutor;
import koh.protocol.client.Message;
import koh.protocol.client.codec.Dofus2ProtocolDecoder;
import koh.protocol.client.codec.Dofus2ProtocolEncoder;
import koh.realm.Main;
import koh.realm.entities.GameServer;
import koh.realm.network.RealmClient.State;
import koh.realm.utils.Settings;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 *
 * @author Neo-Craft
 */

public class RealmServer {

    private final NioSocketAcceptor acceptor;
    private final InetSocketAddress address;
    private final RealmHandler handler;
    private final Dofus2ProtocolDecoder decoder;
    private final Dofus2ProtocolEncoder encoder;

    /**
     * 1 * estimated client optimal size (64)
     */
    private static final int DEFAULT_READ_SIZE = 64;

    /**
     * max used client packet size (realm) + additional size for infos of the next packet
     */
    private static final int MAX_READ_SIZE = 4096 + 0xFF;

    @Inject
    public RealmServer(Settings settings, RealmHandler handler,
                       Dofus2ProtocolDecoder decoder, Dofus2ProtocolEncoder encoder) {
        this.handler = handler;
        this.acceptor = new NioSocketAcceptor(Runtime.getRuntime().availableProcessors() * 4);
        this.address = new InetSocketAddress(settings.getStringElement("Login.Host"),
                settings.getIntElement("Login.Port"));
        this.decoder = decoder;
        this.encoder = encoder;
    }

    public RealmServer configure() {
        acceptor.setReuseAddress(true);
        acceptor.setBacklog(100000);

        this.acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(encoder, decoder));
        this.acceptor.setHandler(handler);

        this.acceptor.getSessionConfig().setMaxReadBufferSize(MAX_READ_SIZE);
        this.acceptor.getSessionConfig().setMinReadBufferSize(DEFAULT_READ_SIZE);
        this.acceptor.getSessionConfig().setReaderIdleTime(Main.MIN_TIMEOUT * 60);
        this.acceptor.getSessionConfig().setTcpNoDelay(true);
        this.acceptor.getSessionConfig().setKeepAlive(true);

        return this;
    }

    @Inject private ConsumerHandlerExecutor<GameServer, InterMessage> messagesHandling;

    public RealmServer launch() {
        try {
            this.acceptor.bind(address);
            //Connect the acceptor with the HostAdress

            Main.onShutdown(() -> {
                try {
                    stop();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            GameServer server = new GameServer();
            try {
                messagesHandling.handle(server, new HelloMessage());
            } catch (Throwable e) {
                e.printStackTrace();
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return this;
        //this.inactivity_manager.start();
    }

    //TODO(Alleos) : use parallel() or Executors.newFixedThreadPool(n) for an async foreach before async write
    public void SendPacket(Message message) {
        acceptor.getManagedSessions().values().stream().filter((session) -> (session.getAttribute("session") instanceof RealmClient) && ((RealmClient) session.getAttribute("session")).ClientState == State.ON_GAMESERVER_LIST).forEach((session) -> {
            ((RealmClient) session.getAttribute("session")).sendPacket(message);
        });
    }

    //TODO(Alleos) : use concurrent hashMap with WeakReferences
    public ArrayList<RealmClient> getAllClient() {
        ArrayList<RealmClient> client = new ArrayList<>();
        acceptor.getManagedSessions().values().stream().filter((session) -> (session.getAttribute("session") instanceof RealmClient)).forEach((session) -> {
            client.add((RealmClient) session.getAttribute("session"));
        });
        return client;
    }

    //TODO(Alleos) : use concurrent hashMap<Integer, RealmClient> with WeakReferences
    public RealmClient getClient(int guid) {
        for (IoSession session : acceptor.getManagedSessions().values()) {
            if (session.getAttribute("session") instanceof RealmClient) {
                RealmClient client = (RealmClient) session.getAttribute("session");
                if (client.Compte != null && client.Compte.get().ID == guid) {
                    return client;
                }
            }
        }
        return null;
    }

    public void stop() throws InterruptedException {
        acceptor.unbind();
        acceptor.dispose(true);
    }

}

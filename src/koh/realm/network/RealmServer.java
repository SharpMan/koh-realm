package koh.realm.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import koh.protocol.client.Message;
import koh.protocol.client.codec.ProtocolEncoder;
import koh.realm.Main;
import koh.realm.network.RealmClient.State;
import koh.realm.network.codec.ProtocolDecoder;
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
    private final InetSocketAddress adress;

    public RealmServer(int port) {
        this.acceptor = new NioSocketAcceptor(Runtime.getRuntime().availableProcessors() * 4);
        this.adress = new InetSocketAddress(Settings.GetStringElement("Login.Host"), port);
    }

    public RealmServer configure() {
        acceptor.setReuseAddress(true);
        acceptor.setBacklog(100000);

        this.acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ProtocolEncoder(), new ProtocolDecoder()));
        this.acceptor.setHandler(new RealmHandler());

        //this.acceptor.getSessionConfig().setMaxReadBufferSize(2048); 
        this.acceptor.getSessionConfig().setReadBufferSize(1024); // Debug
        this.acceptor.getSessionConfig().setReaderIdleTime(Main.MIN_TIMEOUT * 60);
        this.acceptor.getSessionConfig().setTcpNoDelay(true);
        this.acceptor.getSessionConfig().setKeepAlive(true);

        return this;
    }

    public RealmServer launch() {
        try {
            //Connect the acceptor with the HostAdress
            this.acceptor.bind(adress);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return this;
        //this.inactivity_manager.start();
    }

    public void SendPacket(Message message) {
        acceptor.getManagedSessions().values().stream().filter((session) -> (session.getAttribute("session") instanceof RealmClient) && ((RealmClient) session.getAttribute("session")).ClientState == State.ON_GAMESERVER_LIST).forEach((session) -> {
            ((RealmClient) session.getAttribute("session")).sendPacket(message);
        });
    }

    public ArrayList<RealmClient> getAllClient() {
        ArrayList<RealmClient> client = new ArrayList<>();
        acceptor.getManagedSessions().values().stream().filter((session) -> (session.getAttribute("session") instanceof RealmClient)).forEach((session) -> {
            client.add((RealmClient) session.getAttribute("session"));
        });
        return client;
    }

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

package koh.realm.inter;

import java.io.IOException;
import java.net.InetSocketAddress;
import koh.inter.PtDecoder;
import koh.inter.PtEncoder;
import koh.realm.entities.GameServer;
import koh.realm.utils.Settings;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 *
 * @author Neo-Craft
 */
public class InterServer {

    private final NioSocketAcceptor acceptor;
    private final InetSocketAddress adress;

    public InterServer(int port) {
        this.acceptor = new NioSocketAcceptor(Runtime.getRuntime().availableProcessors());
        this.adress = new InetSocketAddress(Settings.GetStringElement("Inter.Host"), port);
    }

    public InterServer configure() {
        acceptor.setReuseAddress(true);
        this.acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new PtEncoder(), new PtDecoder()));
        this.acceptor.setHandler(new InterHandler());

        this.acceptor.getSessionConfig().setReadBufferSize(2048);
        this.acceptor.getSessionConfig().setSendBufferSize(2048);
        //this.acceptor.getSessionConfig().setReaderIdleTime(Main.MIN_TIMEOUT * 60);
        this.acceptor.getSessionConfig().setTcpNoDelay(true);
        this.acceptor.getSessionConfig().setKeepAlive(true);

        return this;
    }

    public InterServer launch() {
        try {
            this.acceptor.bind(adress);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return this;
    }
    

    public void stop() throws InterruptedException {
        acceptor.unbind();
        acceptor.dispose(true);
    }

}

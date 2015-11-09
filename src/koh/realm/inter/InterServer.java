package koh.realm.inter;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import koh.inter.PtDecoder;
import koh.inter.PtEncoder;
import koh.realm.Main;
import koh.realm.utils.Settings;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 *
 * @author Neo-Craft
 */

public class InterServer {

    private final NioSocketAcceptor acceptor;
    private final InetSocketAddress address;
    private final InterHandler handler;

    @Inject
    public InterServer(Settings settings, InterHandler handler) {
        this.handler = handler;
        this.acceptor = new NioSocketAcceptor(Runtime.getRuntime().availableProcessors());
        this.address = new InetSocketAddress(settings.getStringElement("Inter.Host"), settings.getIntElement("Inter.Port"));
    }

    public InterServer configure() {
        acceptor.setReuseAddress(true);
        this.acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new PtEncoder(), new PtDecoder()));
        this.acceptor.setHandler(handler);

        this.acceptor.getSessionConfig().setReadBufferSize(2048);
        this.acceptor.getSessionConfig().setSendBufferSize(2048);
        //this.acceptor.getSessionConfig().setReaderIdleTime(Main.MIN_TIMEOUT * 60);
        this.acceptor.getSessionConfig().setTcpNoDelay(true);
        this.acceptor.getSessionConfig().setKeepAlive(true);

        return this;
    }

    public InterServer launch() {
        try {
            this.acceptor.bind(address);

            Main.onShutdown(() -> {
                try {
                    stop();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
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

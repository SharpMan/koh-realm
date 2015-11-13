package koh.realm.inter;

import java.net.InetSocketAddress;

import com.google.inject.Inject;
import koh.inter.IntercomDecoder;
import koh.inter.IntercomEncoder;
import koh.patterns.services.api.DependsOn;
import koh.patterns.services.api.Service;
import koh.realm.app.DatabaseSource;
import koh.realm.app.Logs;
import koh.realm.utils.Settings;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 *
 * @author Neo-Craft
 */

@DependsOn({Logs.class, DatabaseSource.class})
public class InterServer implements Service {

    private final NioSocketAcceptor acceptor;
    private final InetSocketAddress address;
    private final InterHandler handler;

    @Inject
    public InterServer(Settings settings, InterHandler handler) {
        this.handler = handler;
        this.acceptor = new NioSocketAcceptor(Runtime.getRuntime().availableProcessors());
        this.address = new InetSocketAddress(settings.getStringElement("Inter.Host"), settings.getIntElement("Inter.Port"));
    }

    private InterServer configure() {
        acceptor.setReuseAddress(true);
        this.acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new IntercomEncoder(), new IntercomDecoder()));
        this.acceptor.setHandler(handler);

        this.acceptor.getSessionConfig().setReadBufferSize(256);
        this.acceptor.getSessionConfig().setSendBufferSize(256);
        //this.acceptor.getSessionConfig().setReaderIdleTime(Main.MIN_TIMEOUT * 60);
        this.acceptor.getSessionConfig().setTcpNoDelay(true);
        this.acceptor.getSessionConfig().setKeepAlive(true);

        return this;
    }

    @Override
    public void start() {
        this.configure();
        try {
            this.acceptor.bind(address);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void stop() {
        acceptor.unbind();
        acceptor.dispose(true);
    }

}

package koh.realm.intranet.handlers;

import com.google.inject.Inject;
import koh.inter.messages.HelloMessage;
import koh.mina.api.annotations.Connect;
import koh.mina.api.annotations.Receive;
import koh.patterns.Controller;
import koh.patterns.event.EventExecutor;
import koh.patterns.handler.context.Ctx;
import koh.patterns.handler.context.RequireContexts;
import koh.protocol.client.enums.ServerStatusEnum;
import koh.realm.dao.api.GameServerDAO;
import koh.realm.entities.GameServer;
import koh.realm.intranet.GameServerClient;
import koh.realm.intranet.InterPackage;
import koh.realm.intranet.InterServerContexts;

@RequireContexts(@Ctx(value = InterServerContexts.Authenticating.class))
public class AuthenticatingHandler implements Controller {

    @Inject @InterPackage
    private EventExecutor eventListening;

    @Inject
    private GameServerDAO serversDAO;

    @Connect
    public void onConnect(GameServerClient server) throws Exception {
        if(!server.getRemoteAddress().getAddress().getHostAddress().equalsIgnoreCase("127.0.0.1"))
            server.disconnect(false);
    }

    @Receive
    public void authenticate(GameServerClient server, HelloMessage message) {
        GameServer serverEntity = serversDAO.getGameServers().stream()
                .filter(x -> x.Hash.equals(message.authKey)).findFirst().get();

        if(serverEntity == null) {
            server.disconnect(false);
            return;
        }

        serverEntity.setClient(server);
        server.setEntity(serverEntity);
        server.setHandlerContext(InterServerContexts.AUTHENTICATED);
        serverEntity.setStatus(ServerStatusEnum.ONLINE);
    }

}

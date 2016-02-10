package koh.realm.internet;

import koh.protocol.messages.connection.IdentificationMessage;

public class AuthenticationToken {

    public final IdentificationMessage identificationMessage;

    public AuthenticationToken(IdentificationMessage identificationMessage) {
        this.identificationMessage = identificationMessage;
    }
}

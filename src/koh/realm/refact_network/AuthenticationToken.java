package koh.realm.refact_network;

import koh.protocol.messages.connection.IdentificationMessage;

public class AuthenticationToken {

    public final IdentificationMessage identificationMessage;

    public AuthenticationToken(IdentificationMessage identificationMessage) {
        this.identificationMessage = identificationMessage;
    }
}

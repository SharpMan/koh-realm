package koh.realm.network.codec;

import com.google.inject.Inject;
import koh.protocol.MessageEnum;
import koh.protocol.client.Message;
import koh.protocol.messages.connection.*;
import koh.protocol.messages.handshake.ProtocolRequired;
import koh.realm.Logs;
import koh.realm.Main;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 *
 * @author Neo-Craft
 */
public class ProtocolDecoder extends CumulativeProtocolDecoder {

    private final Logs logs;

    @Inject
    public ProtocolDecoder(Logs logs) {
        this.logs = logs;
    }

    private static final int BIT_MASK = 3;
    private static final int BIT_RIGHT_SHIFT_LEN_PACKET_ID = 2;

    public static int getMessageLength(IoBuffer buf, int header) {
        switch (header & BIT_MASK) {
            case 0:
                return 0;
            case 1:
                return buf.get();
            case 2:
                return buf.getShort();
            case 3:
                return (((buf.get() & 255) << 16) + ((buf.get() & 255) << 8) + (buf.get() & 255));
            default:
                return -1;
        }
    }

    public static int getMessageId(int header) {
        return header >> BIT_RIGHT_SHIFT_LEN_PACKET_ID;
    }

    @Override
    protected boolean doDecode(IoSession session, IoBuffer buf, ProtocolDecoderOutput out) throws Exception {
        if (buf.remaining() < 2) {
            return false;
        }

        int header = buf.getShort(), messageLength = getMessageLength(buf, header);

        if (buf.remaining() < messageLength) {
            return false;
        }

        Message message;

        switch (getMessageId(header)) {
            case ProtocolRequired.MESSAGE_ID:
                message = new ProtocolRequired();
                break;
            case BypassIdentificationMessage.MESSAGE_ID:
                message = new BypassIdentificationMessage();
                break;
            case ServerSelectionMessage.MESSAGE_ID:
                message = new ServerSelectionMessage();
                break;
            case IdentificationMessage.MESSAGE_ID:
                message = new IdentificationMessage();
                break;
            default:
                logs.writeError("[ERROR] Unknown Message Header " + MessageEnum.valueOf(getMessageId(header)) + session.getRemoteAddress().toString());
                session.write(new BasicNoOperationMessage());
                buf.skip(messageLength);
                return true;
        }
        message.deserialize(buf);
        out.write(message);
        return true;
    }

}

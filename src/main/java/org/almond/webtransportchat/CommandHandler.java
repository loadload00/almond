package org.almond.webtransportchat;

import org.almond.webtransportchat.protobuf.ChatMsg;

import io.netty.channel.ChannelHandlerContext;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.util.ReferenceCountUtil;

// import io.netty.util.internal.logging.InternalLogger;
// import io.netty.util.internal.logging.InternalLoggerFactory;

public class CommandHandler extends ProtobufMsgHandler {

  // private static final InternalLogger logger =
  // InternalLoggerFactory.getInstance(CommandHandler.class);

  private ConnectionGroup connectionGroup = new ConnectionGroup();

  public CommandHandler() {
  }

  @Override
  public void UserMsgHandler(ChannelHandlerContext ctx, ChatMsg.UserMsgPack msg) {
    String roomId = msg.getRoomId();
    if (!roomId.isEmpty()) {
      QuicChannel quicChannel = (QuicChannel) ctx.channel();
      if (!msg.getContent().isEmpty()) {
        byte[] pack = ChatMsg.Msg.newBuilder().setParkType(ChatMsg.ParkType.UserMsg_Pack).setUserMsgPack(msg).build()
            .toByteArray();
        connectionGroup.sendMessage(quicChannel.id().toString(), pack, roomId);
      }
    }
    ReferenceCountUtil.release(msg);
  }

  @Override
  public void EventPackHandler(ChannelHandlerContext ctx, ChatMsg.EventPack msg) {
    String roomId = msg.getRoomId();
    if (!roomId.isEmpty()) {
      QuicChannel quicChannel = (QuicChannel) ctx.channel();
      String channelId = quicChannel.id().toString();
      if (msg.getJoin()) {
        if (connectionGroup.joinMessageGroup(quicChannel, roomId, msg.getUserId())) {
          connectionGroup.joinRoomEvent(channelId, msg.getUsername(), msg.getUserId(), roomId);
        }
      }
      byte[] pack = ChatMsg.Msg.newBuilder().setParkType(ChatMsg.ParkType.Event_Pack).setEventPack(msg).build()
          .toByteArray();
      connectionGroup.sendMessage(channelId, pack, roomId);
    }
    ReferenceCountUtil.release(msg);
  }

  @Override
  public void DataPackHandler(ChannelHandlerContext ctx, ChatMsg.DataPack msg) {
    QuicChannel quicChannel = (QuicChannel) ctx.channel();
    byte[] pack = ChatMsg.Msg.newBuilder().setParkType(ChatMsg.ParkType.Data_Pack).setDataPack(msg).build()
        .toByteArray();
    connectionGroup.datagramSend(quicChannel, pack);
    ReferenceCountUtil.release(msg);
  }

}

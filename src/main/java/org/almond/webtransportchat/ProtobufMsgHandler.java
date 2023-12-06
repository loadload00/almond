package org.almond.webtransportchat;

import org.almond.webtransportchat.protobuf.ChatMsg;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

// import io.netty.util.internal.logging.InternalLogger;
// import io.netty.util.internal.logging.InternalLoggerFactory;

public abstract class ProtobufMsgHandler extends ChannelInboundHandlerAdapter {

  // private static final InternalLogger logger =
  // InternalLoggerFactory.getInstance(ProtobufMsgHandler.class);

  public ProtobufMsgHandler() {
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof ChatMsg.Msg) {
      ChatMsg.Msg message = (ChatMsg.Msg) msg;
      ChatMsgHandler(ctx, message);
    } else if (msg instanceof ByteBuf) {
      ReferenceCountUtil.release(msg);
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  private void ChatMsgHandler(ChannelHandlerContext ctx, ChatMsg.Msg msg) {
    switch (msg.getParkType()) {
      case Event_Pack:
        EventPackHandler(ctx, msg.getEventPack());
        break;
      case UserMsg_Pack:
        UserMsgHandler(ctx, msg.getUserMsgPack());
        break;
      case Data_Pack:
        DataPackHandler(ctx, msg.getDataPack());
        break;
      case Frame_Pack:
        // should not recv here
        ReferenceCountUtil.release(msg);
        break;
      default:
        ReferenceCountUtil.release(msg);
        break;
    }
  }

  protected abstract void EventPackHandler(ChannelHandlerContext ctx, ChatMsg.EventPack msg);

  protected abstract void UserMsgHandler(ChannelHandlerContext ctx, ChatMsg.UserMsgPack msg);

  protected abstract void DataPackHandler(ChannelHandlerContext ctx, ChatMsg.DataPack msg);
}

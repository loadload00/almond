package org.almond.webtransportchat;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.util.ReferenceCountUtil;

import org.almond.webtransport.WebtransportSessionId;

// import io.netty.util.internal.logging.InternalLogger;
// import io.netty.util.internal.logging.InternalLoggerFactory;

public class ProtobufBeforeHandler extends ChannelInboundHandlerAdapter {

  // private static final InternalLogger logger =
  // InternalLoggerFactory.getInstance(ProtobufBeforeHandler.class);

  private static final WebtransportSessionId webtransportSessionId = new WebtransportSessionId();

  public ProtobufBeforeHandler() {
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof ByteBuf) {
      ByteBuf buf = (ByteBuf) msg;
      QuicChannel quicChannel = (QuicChannel) ctx.channel();
      long id = (long) buf.readByte();
      if (webtransportSessionId.get(quicChannel.id().toString()) == id) {
        ctx.fireChannelRead(buf);
      } else {
        ReferenceCountUtil.release(buf);
      }
    } else {
      ctx.fireChannelRead(msg);
    }
  }
}

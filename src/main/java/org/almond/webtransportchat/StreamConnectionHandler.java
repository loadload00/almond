package org.almond.webtransportchat;

import org.almond.webtransport.WebtransportBidStreamFrame;
import org.almond.webtransport.WebtransportStreamHandler;
import org.almond.webtransport.WebtransportUnidStreamFrame;

import io.netty.channel.ChannelHandlerContext;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;

// import io.netty.util.internal.logging.InternalLogger;
// import io.netty.util.internal.logging.InternalLoggerFactory;

public class StreamConnectionHandler extends WebtransportStreamHandler {

  // private static final InternalLogger logger =
  // InternalLoggerFactory.getInstance(StreamConnectionHandler.class);

  public StreamConnectionHandler() {

  }

  private static final ConnectionGroup connectionGroup = new ConnectionGroup();

  @Override
  public void channelRead(ChannelHandlerContext ctx, WebtransportBidStreamFrame frame) throws Exception {
    connectionGroup.pushMedia(ctx, frame.content());
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, WebtransportUnidStreamFrame frame) throws Exception {
    connectionGroup.pushMedia(ctx, frame.content());
  }

  @Override
  protected void webtransportConnected(ChannelHandlerContext ctx) {
    QuicChannel quicChannel = (QuicChannel) ctx.channel().parent();
    connectionGroup.addConnection(quicChannel);
  }

  @Override
  protected void channelInputClosed(ChannelHandlerContext ctx) {
    QuicStreamChannel channel = (QuicStreamChannel) ctx.channel();
    if (connectionGroup.hasConnection(channel.id().toString())) {
      channel.close();
    }
  }
}

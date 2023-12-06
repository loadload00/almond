package org.almond.webtransportchat;

import org.almond.webtransport.CodecUtils;
import org.almond.webtransport.WebtransportConnectionHandler;
import org.almond.webtransportchat.config.WebServerConfig;
import org.almond.webtransportchat.protobuf.ChatMsg;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.codec.http3.DefaultHttp3SettingsFrame;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3SettingsFrame;
import io.netty.incubator.codec.quic.EpollQuicUtils;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicChannelOption;
import io.netty.incubator.codec.quic.QuicCongestionControlAlgorithm;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.socket.nio.NioDatagramChannel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.handler.codec.protobuf.ProtobufDecoder;

// import io.netty.util.internal.logging.InternalLogger;
// import io.netty.util.internal.logging.InternalLoggerFactory;

public class ServerStart {

  // private static final InternalLogger logger = InternalLoggerFactory
  // .getInstance(ServerStart.class);

  private static final int cores = Runtime.getRuntime().availableProcessors();

  public static void start(WebServerConfig config) {

    int port = config.getPort();
    Http3SettingsFrame localsetting = new DefaultHttp3SettingsFrame();
    localsetting.put(CodecUtils.H3_DATAGRAM, (long) 1);
    localsetting.put(CodecUtils.ENABLE_WEBTRANSPORT, (long) 1);

    EventLoopGroup group = Epoll.isAvailable()
        ? new EpollEventLoopGroup()
        : new NioEventLoopGroup();

    QuicSslContext sslContext = QuicSslContextBuilder
        .forServer(config.getSslConfig().getPrivateKey(), null, config.getSslConfig().getCert())
        .applicationProtocols(Http3.supportedApplicationProtocols()).earlyData(true).build();
    QuicServerCodecBuilder builder = Http3.newQuicServerCodecBuilder()
        .hystart(false).activeMigration(true).grease(true)
        .congestionControlAlgorithm(QuicCongestionControlAlgorithm.CUBIC)
        .datagram(65536, 65536).maxSendUdpPayloadSize(1408)
        .sslContext(sslContext).maxIdleTimeout(20, TimeUnit.SECONDS).initialMaxData(10 * 1024 * 1024)
        .initialMaxStreamDataBidirectionalLocal(512 * 1024)
        .initialMaxStreamDataBidirectionalRemote(512 * 1024)
        .initialMaxStreamDataUnidirectional(512 * 1024)
        .initialMaxStreamsBidirectional(500).initialMaxStreamsUnidirectional(500)
        .tokenHandler(null)
        .handler(new ChannelInitializer<QuicChannel>() {
          @Override
          protected void initChannel(QuicChannel ch) {
            ch.pipeline().addLast(new ProtobufBeforeHandler());
            ch.pipeline().addLast(new ProtobufDecoder(ChatMsg.Msg.getDefaultInstance()));
            ch.pipeline().addLast(new CommandHandler());
            ch.pipeline().addLast(new WebtransportConnectionHandler(new ChannelInitializer<QuicStreamChannel>() {
              @Override
              protected void initChannel(QuicStreamChannel ch) {
                ch.pipeline().addLast(new StreamConnectionHandler());
              }
            }, localsetting));
          }
        });
    if (group instanceof EpollEventLoopGroup) {
      builder.option(QuicChannelOption.SEGMENTED_DATAGRAM_PACKET_ALLOCATOR,
          EpollQuicUtils.newSegmentedAllocator(8));
    }
    try {
      Bootstrap bs = new Bootstrap();
      if (group instanceof EpollEventLoopGroup) {
        bs.group(group)
            .channel(EpollDatagramChannel.class)
            .option(EpollChannelOption.SO_REUSEPORT, true)
            .option(EpollChannelOption.MAX_DATAGRAM_PAYLOAD_SIZE, 2048)
            .option(EpollChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(2048 * 8));

        InetSocketAddress socketport = new InetSocketAddress(port);
        List<ChannelFuture> futures = new ArrayList<>(cores);
        for (int i = 0; i < cores; i++) {
          ChannelHandler builderCodec = builder.build();
          bs.handler(builderCodec);
          ChannelFuture future = bs.bind(socketport).await();
          futures.add(future);
        }
        for (final ChannelFuture future : futures) {
          future.channel().closeFuture().await();
        }
      } else {
        ChannelHandler codec = builder.build();
        Channel channel = bs.group(group).option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(2048 * 8))
            .channel(NioDatagramChannel.class)
            .handler(codec)
            .bind(new InetSocketAddress(port)).sync().channel();
        channel.closeFuture().await();
      }
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          group.shutdownGracefully();
        }
      });
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      group.shutdownGracefully();
    }
  }
}

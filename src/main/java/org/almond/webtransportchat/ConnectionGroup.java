package org.almond.webtransportchat;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.almond.webtransport.Webtransport;
import org.almond.webtransport.WebtransportSessionId;
import org.almond.webtransportchat.protobuf.ChatMsg;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

// import io.netty.util.internal.logging.InternalLogger;
// import io.netty.util.internal.logging.InternalLoggerFactory;

public class ConnectionGroup {

  // private static final InternalLogger logger = InternalLoggerFactory
  // .getInstance(ConnectionGroup.class);

  private static final ConcurrentMap<String, UserConnectionInfo> userChannel = new ConcurrentHashMap<>();

  private static final ConcurrentMap<String, String> streamGroup = new ConcurrentHashMap<>();

  private static final ConcurrentMap<String, ChannelGroup> messageGroup = new ConcurrentHashMap<>();

  private static final ConcurrentMap<String, ChannelGroup> mediaGroup = new ConcurrentHashMap<>();

  private static final Webtransport webtransport = new Webtransport();

  private static WebtransportSessionId webtransportSessionId = new WebtransportSessionId();

  public ConnectionGroup() {
  }

  public void addConnection(QuicChannel channel) {
    String id = channel.id().toString();
    if (userChannel.get(id) == null) {
      UserConnectionInfo info = new UserConnectionInfo();
      userChannel.put(id, info);
      channel.closeFuture().addListener(f -> {
        UserConnectionInfo leaveUser = userChannel.get(id);
        if (leaveUser.getUserId() == null) {
          userChannel.remove(id);
          return;
        }
        leaveRoom(id, leaveUser.getRoomId(), leaveUser.getUserId());
        ChannelGroup msgGroup = messageGroup.get(leaveUser.getRoomId());
        if (msgGroup != null) {
          if (msgGroup.size() == 1) {
            msgGroup.close();
            messageGroup.remove(leaveUser.getRoomId());
          }
        }
      });
    } else {
      channel.close();
    }
  }

  public void pushMedia(ChannelHandlerContext ctx, ByteBuf buf) {
    QuicStreamChannel streamChannel = (QuicStreamChannel) ctx.channel();
    String pushId = streamChannel.id().toString();
    ChannelGroup pushGroup = mediaGroup.get(pushId);
    if (pushGroup == null) {
      QuicChannel quicChannel = streamChannel.parent();
      String quicChannelId = quicChannel.id().toString();
      String roomId = userChannel.get(quicChannelId).getRoomId();
      if (roomId == null) {
        ReferenceCountUtil.release(buf);
        streamChannel.close();
        return;
      }
      newPushStream(quicChannel, streamChannel, pushId, roomId, buf);
      return;
    }
    if (pushGroup.size() != 0) {
      pushGroup.writeAndFlush(buf);
      return;
    }
    ReferenceCountUtil.release(buf);
  }

  public void newPushStream(QuicChannel quicChannel, QuicStreamChannel streamChannel, String pushId, String roomId,
      ByteBuf buf) {
    ChannelGroup msgGroup = messageGroup.get(roomId);
    if (msgGroup == null) {
      ReferenceCountUtil.release(buf);
      return;
    }
    ChannelGroup newGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    msgGroup.forEach(ch -> {
      QuicChannel channel = (QuicChannel) ch;
      if (channel != quicChannel) {
        try {
          QuicStreamChannel quicStreamChannel = webtransport.CreateUnidStream(channel, null);
          newGroup.add(quicStreamChannel);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    if (newGroup.size() != 0) {
      newGroup.writeAndFlush(buf);
    } else {
      ReferenceCountUtil.release(buf);
    }
    mediaGroup.put(pushId, newGroup);
    streamGroup.put(pushId, roomId);
    streamChannel.closeFuture().addListener(f -> {
      streamGroup.remove(pushId);
      ChannelGroup shouldClose = mediaGroup.get(pushId);
      if (shouldClose != null) {
        shouldClose.close();
        shouldClose.clear();
        mediaGroup.remove(pushId);
      }
    });
  }

  public boolean joinMessageGroup(QuicChannel channel, String roomId, String userId) {
    UserConnectionInfo user = userChannel.get(channel.id().toString());
    if (user.getRoomId() != null || userId == null) {
      return false;
    }
    ChannelGroup group = messageGroup.get(roomId);
    if (group == null) {
      ChannelGroup newRoom = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
      newRoom.add(channel);
      messageGroup.put(roomId, newRoom);
    } else {
      group.add(channel);
    }
    user.setRoomId(roomId);
    user.setUserId(userId);
    joinPushStream(channel, roomId);
    return true;
  }

  public void sendMessage(String channelId, byte[] msg, String roomId) {
    ChannelGroup group = messageGroup.get(roomId);
    if (group == null) {
      ReferenceCountUtil.release(msg);
      return;
    }
    ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer();
    buf.writeByte((byte) webtransportSessionId.get(channelId));
    buf.writeBytes(msg);
    group.writeAndFlush(buf);
  }

  private void joinPushStream(QuicChannel channel, String roomId) {
    for (String streamId : streamGroup.keySet()) {
      String rooms = streamGroup.get(streamId);
      if (rooms.equals(roomId)) {
        ChannelGroup pushRoom = mediaGroup.get(streamId);
        if (pushRoom != null) {
          try {
            QuicStreamChannel quicStreamChannel = webtransport.CreateUnidStream(channel, null);
            pushRoom.add(quicStreamChannel);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    }
  }

  public void joinRoomEvent(String channelId, String username, String userId, String roomId) {
    ChatMsg.EventPack eventPack = ChatMsg.EventPack.newBuilder().setUserId(userId).setUsername(username).setJoin(true)
        .build();
    byte[] msg = ChatMsg.Msg.newBuilder().setParkType(ChatMsg.ParkType.Event_Pack).setEventPack(eventPack).build()
        .toByteArray();
    sendMessage(channelId, msg, roomId);
  }

  private void leaveRoom(String channelId, String roomId, String userId) {
    ChatMsg.EventPack eventPack = ChatMsg.EventPack.newBuilder().setUserId(userId).setLeave(true).build();
    byte[] msg = ChatMsg.Msg.newBuilder().setParkType(ChatMsg.ParkType.Event_Pack).setEventPack(eventPack)
        .build().toByteArray();
    sendMessage(channelId, msg, roomId);
  }

  public void datagramSend(QuicChannel channel, byte[] msg) {
    ByteBuf buf = channel.alloc().directBuffer();
    buf.writeByte((byte) webtransportSessionId.get(channel.id().toString()));
    buf.writeBytes(msg);
    channel.writeAndFlush(buf);
  }

  public boolean hasConnection(String id) {
    return streamGroup.get(id) != null;
  }
}

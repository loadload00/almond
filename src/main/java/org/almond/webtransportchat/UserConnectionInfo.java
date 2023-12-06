package org.almond.webtransportchat;

public class UserConnectionInfo {

  private String userId = null;
  private String roomId = null;
  private String channelId = null;

  public UserConnectionInfo() {
  }

  public String getUserId() {
    return this.userId;
  }

  public String getRoomId() {
    return this.roomId;
  }

  public String getChannelId() {
    return this.channelId;
  }

  public void setAll(String roomId, String userId, String channelId) {
    this.roomId = roomId;
    this.userId = userId;
    this.channelId = channelId;
  }

  public void setUserId(String id) {
    this.userId = id;
  }

  public void setRoomId(String id) {
    this.roomId = id;
  }

  public void setChannelId(String id) {
    this.channelId = id;
  }

}

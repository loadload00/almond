package org.almond.webtransportchat.config;

public class WebServerConfig {
  public static final String CONFIG_HOST = "host";
  public static final String CONFIG_PORT = "port";
  public static final String CONFIG_SSL_KEY = "privkey";
  public static final String CONFIG_SSL_CERT = "cert";
  private String host = "0.0.0.0";
  private int port = 443;
  private SSLConfig sslConfig;

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public SSLConfig getSslConfig() {
    return sslConfig;
  }

  public void setSslConfig(SSLConfig sslConfig) {
    this.sslConfig = sslConfig;
  }
}

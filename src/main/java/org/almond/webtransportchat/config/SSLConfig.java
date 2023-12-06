package org.almond.webtransportchat.config;

import java.io.File;

public class SSLConfig {
  private final File privateKey;
  private final File cert;

  public SSLConfig(File privateKey, File cert) {
    this.privateKey = privateKey;
    this.cert = cert;
  }

  public File getPrivateKey() {
    return this.privateKey;
  }

  public File getCert() {
    return this.cert;
  }
}

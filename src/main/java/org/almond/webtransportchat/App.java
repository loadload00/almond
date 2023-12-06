package org.almond.webtransportchat;

import org.almond.webtransportchat.config.WebServerConfig;
import org.almond.webtransportchat.config.WebServerConfigFactory;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class App {
  public static void main(String[] args) {
    WebServerConfig config = new WebServerConfigFactory().getConfig(args);
    ServerStart.start(config);
  }
}
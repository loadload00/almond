package org.almond.webtransportchat.config;

import java.util.HashMap;
import java.util.Map;

public class SystemEnvVarConfigReader implements ConfigReader {
  @Override
  public Map<String, Object> readConfig() {
    String serverHost = System.getenv(WebServerConfig.CONFIG_HOST);
    String serverPort = System.getenv(WebServerConfig.CONFIG_PORT);
    HashMap<String, String> map = new HashMap<>();
    map.put(WebServerConfig.CONFIG_HOST, serverHost);
    map.put(WebServerConfig.CONFIG_PORT, serverPort);
    return validateConfig(map);
  }
}

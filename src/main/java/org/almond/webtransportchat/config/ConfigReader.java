package org.almond.webtransportchat.config;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public interface ConfigReader {
  Map<String, Object> readConfig() throws Exception;

  default Map<String, Object> validateConfig(Map<String, String> origin) {
    String serverHost = origin.get(WebServerConfig.CONFIG_HOST);
    String serverPort = origin.get(WebServerConfig.CONFIG_PORT);
    String sslKey = origin.get(WebServerConfig.CONFIG_SSL_KEY);
    String sslCert = origin.get(WebServerConfig.CONFIG_SSL_CERT);
    String defaultKey = "./fullchain.pem";
    String defaultCert = "./privkey.pem";
    HashMap<String, Object> map = new HashMap<>();
    if (StringUtils.isNotBlank(serverHost))
      map.put(WebServerConfig.CONFIG_HOST, serverHost);
    if (StringUtils.isNotBlank(serverPort)) {
      try {
        int port = Integer.parseInt(serverPort);
        if (port <= 0 || port > 65535) {
          throw new RuntimeException("非法端口：" + port);
        }
        map.put(WebServerConfig.CONFIG_PORT, port);
      } catch (NumberFormatException ignore) {
      }
    }
    if (sslKey != null && sslCert != null) {
      File sslCertFile = new File(sslCert);
      File sslKeyFile = new File(sslKey);
      boolean sslKeyFileExists = sslKeyFile.exists();
      if (!sslKeyFileExists) {
        throw new WebServerConfigException(String.format("Private Key not exists!",
            sslKeyFile.getAbsoluteFile()));
      }
      boolean sslKeyCertExists = sslCertFile.exists();
      if (!sslKeyCertExists) {
        throw new WebServerConfigException(String.format("Cert not exists!",
            sslCertFile.getAbsoluteFile()));
      }
      map.put(WebServerConfig.CONFIG_SSL_KEY, sslKeyFile);
      map.put(WebServerConfig.CONFIG_SSL_CERT, sslCertFile);
    } else {
      File sslCertFile = new File(defaultCert);
      File sslKeyFile = new File(defaultKey);
      if (sslCertFile.exists() && sslKeyFile.exists()) {
        map.put(WebServerConfig.CONFIG_SSL_KEY, sslCertFile);
        map.put(WebServerConfig.CONFIG_SSL_CERT, sslKeyFile);
      } else {
        throw new WebServerConfigException(String.format("ssl config is require",
            sslKeyFile.getAbsoluteFile()));
      }
    }
    return map;
  }
}

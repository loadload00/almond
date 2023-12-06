package org.almond.webtransportchat.config;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class WebServerConfigFactory {

  public WebServerConfig getConfig(String[] args) {
    CommandLineConfigReader commandLineConfigReader = new CommandLineConfigReader(args);
    Map<String, Object> propParam = new SystemPropertiesConfigReader().readConfig();
    Map<String, Object> envParam = new SystemEnvVarConfigReader().readConfig();
    Map<String, Object> cliParam = commandLineConfigReader.readConfig();
    HashMap<String, Object> mergeMap = new HashMap<>(propParam);
    mergeMap.putAll(envParam);
    mergeMap.putAll(cliParam);
    WebServerConfig webServerConfig = new WebServerConfig();
    String host = (String) mergeMap.get(WebServerConfig.CONFIG_HOST);
    if (StringUtils.isNotBlank(host))
      webServerConfig.setHost(host);
    Integer port = (Integer) mergeMap.get(WebServerConfig.CONFIG_PORT);
    if (port != null)
      webServerConfig.setPort(port);
    File sslKeyFile = (File) mergeMap.get(WebServerConfig.CONFIG_SSL_KEY);
    File sslCertFile = (File) mergeMap.get(WebServerConfig.CONFIG_SSL_CERT);
    if (sslKeyFile != null && sslCertFile != null) {
      SSLConfig sslConfig = new SSLConfig(sslKeyFile, sslCertFile);
      webServerConfig.setSslConfig(sslConfig);
      if (webServerConfig.getPort() == 0) {
        webServerConfig.setPort(443);
      }
    }
    return webServerConfig;
  }
}

package org.almond.webtransportchat.config;

import org.apache.commons.cli.*;
import org.apache.commons.cli.ParseException;
import java.util.HashMap;
import java.util.Map;

public class CommandLineConfigReader implements ConfigReader {
  private final static String PARMA_BIND_HOST = "host";
  private final static String PARMA_BIND_PORT = "port";
  private final static String PARMA_SERVER_SSL_KEY = "privkey";
  private final static String PARMA_SERVER_SSL_CERT = "cert";
  private final String[] args;
  private final Options options;

  public CommandLineConfigReader(String[] args) {
    this.args = args;
    this.options = new Options();
    this.options.addOption(Option.builder()
        .longOpt(PARMA_BIND_HOST)
        .hasArg()
        .argName("hostname/ip").build());
    this.options.addOption(Option.builder()
        .longOpt(PARMA_BIND_PORT)
        .hasArg()
        .argName("port").build());
    this.options.addOption(Option.builder()
        .longOpt(PARMA_SERVER_SSL_KEY)
        .hasArg()
        .argName("filepath").build());
    this.options.addOption(Option.builder()
        .longOpt(PARMA_SERVER_SSL_CERT)
        .hasArg()
        .argName("filepath").build());
  }

  @Override
  public Map<String, Object> readConfig() {
    DefaultParser defaultParser = new DefaultParser();
    CommandLine cli;
    try {
      cli = defaultParser.parse(options, args);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
    String bindHost = cli.getOptionValue(PARMA_BIND_HOST);
    String bindPort = cli.getOptionValue(PARMA_BIND_PORT);
    String sslKey = cli.getOptionValue(PARMA_SERVER_SSL_KEY);
    String sslCert = cli.getOptionValue(PARMA_SERVER_SSL_CERT);
    HashMap<String, String> map = new HashMap<>();
    map.put(WebServerConfig.CONFIG_HOST, bindHost);
    map.put(WebServerConfig.CONFIG_PORT, bindPort);
    map.put(WebServerConfig.CONFIG_SSL_KEY, sslKey);
    map.put(WebServerConfig.CONFIG_SSL_CERT, sslCert);
    return validateConfig(map);
  }
}

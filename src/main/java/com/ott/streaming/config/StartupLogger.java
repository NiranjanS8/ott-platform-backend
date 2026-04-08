package com.ott.streaming.config;

import com.ott.streaming.config.properties.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);

    private final AppProperties appProperties;
    private final String graphqlPath;
    private final int serverPort;

    public StartupLogger(AppProperties appProperties,
                         @Value("${spring.graphql.path:/graphql}") String graphqlPath,
                         @Value("${server.port}") int serverPort) {
        this.appProperties = appProperties;
        this.graphqlPath = graphqlPath;
        this.serverPort = serverPort;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Application '{}' started in '{}' mode on port {}", appProperties.name(),
                appProperties.environment(), serverPort);
        log.info("GraphQL endpoint available at http://localhost:{}{}", serverPort, graphqlPath);
    }
}


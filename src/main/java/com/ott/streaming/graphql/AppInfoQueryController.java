package com.ott.streaming.graphql;

import com.ott.streaming.config.properties.AppProperties;
import com.ott.streaming.dto.AppInfo;
import com.ott.streaming.exception.ApiException;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.stereotype.Controller;

@Controller
@Validated
public class AppInfoQueryController {

    private final AppProperties appProperties;
    private final String graphqlPath;

    public AppInfoQueryController(AppProperties appProperties,
                                  @Value("${spring.graphql.path:/graphql}") String graphqlPath) {
        this.appProperties = appProperties;
        this.graphqlPath = graphqlPath;
    }

    @QueryMapping
    public String health() {
        return "UP";
    }

    @QueryMapping
    public AppInfo appInfo() {
        return new AppInfo(
                appProperties.name(),
                appProperties.environment(),
                graphqlPath
        );
    }

    @QueryMapping
    public String ping(@Argument String message) {
        if (message == null || message.isBlank()) {
            throw ApiException.validation("Message must not be blank");
        }

        return "PONG: " + message.trim();
    }
}

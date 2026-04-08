package com.ott.streaming.dto;

public record AppInfo(
        String name,
        String environment,
        String graphqlPath
) {
}


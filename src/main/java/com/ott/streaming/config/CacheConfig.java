package com.ott.streaming.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                CacheNames.CONTENT_MOVIES,
                CacheNames.CONTENT_SERIES,
                CacheNames.CONTENT_MOVIE_BY_ID,
                CacheNames.CONTENT_SERIES_BY_ID
        );
    }
}

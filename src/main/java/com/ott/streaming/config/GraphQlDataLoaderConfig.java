package com.ott.streaming.config;

import com.ott.streaming.dto.content.EpisodePayload;
import com.ott.streaming.dto.content.GenrePayload;
import com.ott.streaming.dto.content.PersonPayload;
import com.ott.streaming.dto.content.SeasonPayload;
import com.ott.streaming.dto.review.RatingSummaryPayload;
import com.ott.streaming.service.ContentQueryService;
import com.ott.streaming.service.ReviewService;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import reactor.core.publisher.Mono;

@Configuration
public class GraphQlDataLoaderConfig {

    public GraphQlDataLoaderConfig(BatchLoaderRegistry batchLoaderRegistry,
                                   ContentQueryService contentQueryService,
                                   ReviewService reviewService) {
        batchLoaderRegistry.<Long, java.util.List<GenrePayload>>forName("movieGenresDataLoader")
                .registerMappedBatchLoader((movieIds, environment) ->
                        Mono.just(contentQueryService.getMovieGenresByMovieIds(movieIds)));
        batchLoaderRegistry.<Long, java.util.List<PersonPayload>>forName("movieCastDataLoader")
                .registerMappedBatchLoader((movieIds, environment) ->
                        Mono.just(contentQueryService.getMovieCastByMovieIds(movieIds)));
        batchLoaderRegistry.<Long, java.util.List<PersonPayload>>forName("movieDirectorsDataLoader")
                .registerMappedBatchLoader((movieIds, environment) ->
                        Mono.just(contentQueryService.getMovieDirectorsByMovieIds(movieIds)));
        batchLoaderRegistry.<Long, RatingSummaryPayload>forName("movieRatingSummaryDataLoader")
                .registerMappedBatchLoader((movieIds, environment) ->
                        Mono.just(reviewService.getMovieRatingSummariesByMovieIds(movieIds)));

        batchLoaderRegistry.<Long, java.util.List<GenrePayload>>forName("seriesGenresDataLoader")
                .registerMappedBatchLoader((seriesIds, environment) ->
                        Mono.just(contentQueryService.getSeriesGenresBySeriesIds(seriesIds)));
        batchLoaderRegistry.<Long, java.util.List<PersonPayload>>forName("seriesCastDataLoader")
                .registerMappedBatchLoader((seriesIds, environment) ->
                        Mono.just(contentQueryService.getSeriesCastBySeriesIds(seriesIds)));
        batchLoaderRegistry.<Long, java.util.List<PersonPayload>>forName("seriesDirectorsDataLoader")
                .registerMappedBatchLoader((seriesIds, environment) ->
                        Mono.just(contentQueryService.getSeriesDirectorsBySeriesIds(seriesIds)));
        batchLoaderRegistry.<Long, java.util.List<SeasonPayload>>forName("seriesSeasonsDataLoader")
                .registerMappedBatchLoader((seriesIds, environment) ->
                        Mono.just(contentQueryService.getSeriesSeasonsBySeriesIds(seriesIds)));
        batchLoaderRegistry.<Long, RatingSummaryPayload>forName("seriesRatingSummaryDataLoader")
                .registerMappedBatchLoader((seriesIds, environment) ->
                        Mono.just(reviewService.getSeriesRatingSummariesBySeriesIds(seriesIds)));

        batchLoaderRegistry.<Long, java.util.List<EpisodePayload>>forName("seasonEpisodesDataLoader")
                .registerMappedBatchLoader((seasonIds, environment) ->
                        Mono.just(contentQueryService.getSeasonEpisodesBySeasonIds(seasonIds)));
    }
}

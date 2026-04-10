package com.ott.streaming.graphql;

import com.ott.streaming.dto.content.CreateEpisodeInput;
import com.ott.streaming.dto.content.CreateGenreInput;
import com.ott.streaming.dto.content.CreateMovieInput;
import com.ott.streaming.dto.content.CreatePersonInput;
import com.ott.streaming.dto.content.CreateSeasonInput;
import com.ott.streaming.dto.content.CreateSeriesInput;
import com.ott.streaming.dto.content.EpisodePayload;
import com.ott.streaming.dto.content.GenrePayload;
import com.ott.streaming.dto.content.MoviePayload;
import com.ott.streaming.dto.content.PersonPayload;
import com.ott.streaming.dto.content.SeasonPayload;
import com.ott.streaming.dto.content.SeriesPayload;
import com.ott.streaming.dto.content.UpdateEpisodeInput;
import com.ott.streaming.dto.content.UpdateGenreInput;
import com.ott.streaming.dto.content.UpdateMovieInput;
import com.ott.streaming.dto.content.UpdatePersonInput;
import com.ott.streaming.dto.content.UpdateSeasonInput;
import com.ott.streaming.dto.content.UpdateSeriesInput;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
public class ContentGraphQlController {

    @MutationMapping
    public GenrePayload createGenre(@Argument @Valid CreateGenreInput input) {
        throw new UnsupportedOperationException("createGenre is not implemented yet");
    }

    @MutationMapping
    public GenrePayload updateGenre(@Argument Long id, @Argument @Valid UpdateGenreInput input) {
        throw new UnsupportedOperationException("updateGenre is not implemented yet");
    }

    @MutationMapping
    public Boolean deleteGenre(@Argument Long id) {
        throw new UnsupportedOperationException("deleteGenre is not implemented yet");
    }

    @MutationMapping
    public PersonPayload createPerson(@Argument @Valid CreatePersonInput input) {
        throw new UnsupportedOperationException("createPerson is not implemented yet");
    }

    @MutationMapping
    public PersonPayload updatePerson(@Argument Long id, @Argument @Valid UpdatePersonInput input) {
        throw new UnsupportedOperationException("updatePerson is not implemented yet");
    }

    @MutationMapping
    public Boolean deletePerson(@Argument Long id) {
        throw new UnsupportedOperationException("deletePerson is not implemented yet");
    }

    @MutationMapping
    public MoviePayload createMovie(@Argument @Valid CreateMovieInput input) {
        throw new UnsupportedOperationException("createMovie is not implemented yet");
    }

    @MutationMapping
    public MoviePayload updateMovie(@Argument Long id, @Argument @Valid UpdateMovieInput input) {
        throw new UnsupportedOperationException("updateMovie is not implemented yet");
    }

    @MutationMapping
    public Boolean deleteMovie(@Argument Long id) {
        throw new UnsupportedOperationException("deleteMovie is not implemented yet");
    }

    @MutationMapping
    public SeriesPayload createSeries(@Argument @Valid CreateSeriesInput input) {
        throw new UnsupportedOperationException("createSeries is not implemented yet");
    }

    @MutationMapping
    public SeriesPayload updateSeries(@Argument Long id, @Argument @Valid UpdateSeriesInput input) {
        throw new UnsupportedOperationException("updateSeries is not implemented yet");
    }

    @MutationMapping
    public Boolean deleteSeries(@Argument Long id) {
        throw new UnsupportedOperationException("deleteSeries is not implemented yet");
    }

    @MutationMapping
    public SeasonPayload createSeason(@Argument @Valid CreateSeasonInput input) {
        throw new UnsupportedOperationException("createSeason is not implemented yet");
    }

    @MutationMapping
    public SeasonPayload updateSeason(@Argument Long id, @Argument @Valid UpdateSeasonInput input) {
        throw new UnsupportedOperationException("updateSeason is not implemented yet");
    }

    @MutationMapping
    public Boolean deleteSeason(@Argument Long id) {
        throw new UnsupportedOperationException("deleteSeason is not implemented yet");
    }

    @MutationMapping
    public EpisodePayload createEpisode(@Argument @Valid CreateEpisodeInput input) {
        throw new UnsupportedOperationException("createEpisode is not implemented yet");
    }

    @MutationMapping
    public EpisodePayload updateEpisode(@Argument Long id, @Argument @Valid UpdateEpisodeInput input) {
        throw new UnsupportedOperationException("updateEpisode is not implemented yet");
    }

    @MutationMapping
    public Boolean deleteEpisode(@Argument Long id) {
        throw new UnsupportedOperationException("deleteEpisode is not implemented yet");
    }

    @QueryMapping
    public List<MoviePayload> movies() {
        throw new UnsupportedOperationException("movies is not implemented yet");
    }

    @QueryMapping
    public MoviePayload movie(@Argument Long id) {
        throw new UnsupportedOperationException("movie is not implemented yet");
    }

    @QueryMapping
    public List<SeriesPayload> seriesList() {
        throw new UnsupportedOperationException("seriesList is not implemented yet");
    }

    @QueryMapping
    public SeriesPayload series(@Argument Long id) {
        throw new UnsupportedOperationException("series is not implemented yet");
    }

    @QueryMapping
    public SeasonPayload season(@Argument Long id) {
        throw new UnsupportedOperationException("season is not implemented yet");
    }

    @QueryMapping
    public EpisodePayload episode(@Argument Long id) {
        throw new UnsupportedOperationException("episode is not implemented yet");
    }
}

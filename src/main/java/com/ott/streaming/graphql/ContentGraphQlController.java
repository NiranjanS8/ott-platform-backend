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
import com.ott.streaming.service.ContentAdminService;
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

    private final ContentAdminService contentAdminService;

    public ContentGraphQlController(ContentAdminService contentAdminService) {
        this.contentAdminService = contentAdminService;
    }

    @MutationMapping
    public GenrePayload createGenre(@Argument @Valid CreateGenreInput input) {
        return contentAdminService.createGenre(input);
    }

    @MutationMapping
    public GenrePayload updateGenre(@Argument Long id, @Argument @Valid UpdateGenreInput input) {
        return contentAdminService.updateGenre(id, input);
    }

    @MutationMapping
    public Boolean deleteGenre(@Argument Long id) {
        return contentAdminService.deleteGenre(id);
    }

    @MutationMapping
    public PersonPayload createPerson(@Argument @Valid CreatePersonInput input) {
        return contentAdminService.createPerson(input);
    }

    @MutationMapping
    public PersonPayload updatePerson(@Argument Long id, @Argument @Valid UpdatePersonInput input) {
        return contentAdminService.updatePerson(id, input);
    }

    @MutationMapping
    public Boolean deletePerson(@Argument Long id) {
        return contentAdminService.deletePerson(id);
    }

    @MutationMapping
    public MoviePayload createMovie(@Argument @Valid CreateMovieInput input) {
        return contentAdminService.createMovie(input);
    }

    @MutationMapping
    public MoviePayload updateMovie(@Argument Long id, @Argument @Valid UpdateMovieInput input) {
        return contentAdminService.updateMovie(id, input);
    }

    @MutationMapping
    public Boolean deleteMovie(@Argument Long id) {
        return contentAdminService.deleteMovie(id);
    }

    @MutationMapping
    public SeriesPayload createSeries(@Argument @Valid CreateSeriesInput input) {
        return contentAdminService.createSeries(input);
    }

    @MutationMapping
    public SeriesPayload updateSeries(@Argument Long id, @Argument @Valid UpdateSeriesInput input) {
        return contentAdminService.updateSeries(id, input);
    }

    @MutationMapping
    public Boolean deleteSeries(@Argument Long id) {
        return contentAdminService.deleteSeries(id);
    }

    @MutationMapping
    public SeasonPayload createSeason(@Argument @Valid CreateSeasonInput input) {
        return contentAdminService.createSeason(input);
    }

    @MutationMapping
    public SeasonPayload updateSeason(@Argument Long id, @Argument @Valid UpdateSeasonInput input) {
        return contentAdminService.updateSeason(id, input);
    }

    @MutationMapping
    public Boolean deleteSeason(@Argument Long id) {
        return contentAdminService.deleteSeason(id);
    }

    @MutationMapping
    public EpisodePayload createEpisode(@Argument @Valid CreateEpisodeInput input) {
        return contentAdminService.createEpisode(input);
    }

    @MutationMapping
    public EpisodePayload updateEpisode(@Argument Long id, @Argument @Valid UpdateEpisodeInput input) {
        return contentAdminService.updateEpisode(id, input);
    }

    @MutationMapping
    public Boolean deleteEpisode(@Argument Long id) {
        return contentAdminService.deleteEpisode(id);
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

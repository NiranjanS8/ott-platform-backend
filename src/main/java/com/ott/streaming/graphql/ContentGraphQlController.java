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
import com.ott.streaming.dto.discovery.CatalogPagePayload;
import com.ott.streaming.dto.discovery.CatalogQueryInput;
import com.ott.streaming.service.ContentAdminService;
import com.ott.streaming.service.ContentQueryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
public class ContentGraphQlController {

    private final ContentAdminService contentAdminService;
    private final ContentQueryService contentQueryService;

    public ContentGraphQlController(ContentAdminService contentAdminService, ContentQueryService contentQueryService) {
        this.contentAdminService = contentAdminService;
        this.contentQueryService = contentQueryService;
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
        return contentQueryService.getMovies();
    }

    @QueryMapping
    public MoviePayload movie(@Argument Long id) {
        return contentQueryService.getMovieById(id);
    }

    @QueryMapping
    public List<SeriesPayload> seriesList() {
        return contentQueryService.getSeriesList();
    }

    @QueryMapping
    public SeriesPayload series(@Argument Long id) {
        return contentQueryService.getSeriesById(id);
    }

    @QueryMapping
    public SeasonPayload season(@Argument Long id) {
        return contentQueryService.getSeasonById(id);
    }

    @QueryMapping
    public EpisodePayload episode(@Argument Long id) {
        return contentQueryService.getEpisodeById(id);
    }

    @QueryMapping
    public CatalogPagePayload discoverCatalog(@Argument @Valid CatalogQueryInput input) {
        return contentQueryService.discoverCatalog(input);
    }

    @SchemaMapping(typeName = "Movie", field = "genres")
    public List<GenrePayload> movieGenres(MoviePayload source) {
        return contentQueryService.getMovieGenres(source);
    }

    @SchemaMapping(typeName = "Movie", field = "cast")
    public List<PersonPayload> movieCast(MoviePayload source) {
        return contentQueryService.getMovieCast(source);
    }

    @SchemaMapping(typeName = "Movie", field = "directors")
    public List<PersonPayload> movieDirectors(MoviePayload source) {
        return contentQueryService.getMovieDirectors(source);
    }

    @SchemaMapping(typeName = "Series", field = "genres")
    public List<GenrePayload> seriesGenres(SeriesPayload source) {
        return contentQueryService.getSeriesGenres(source);
    }

    @SchemaMapping(typeName = "Series", field = "cast")
    public List<PersonPayload> seriesCast(SeriesPayload source) {
        return contentQueryService.getSeriesCast(source);
    }

    @SchemaMapping(typeName = "Series", field = "directors")
    public List<PersonPayload> seriesDirectors(SeriesPayload source) {
        return contentQueryService.getSeriesDirectors(source);
    }

    @SchemaMapping(typeName = "Series", field = "seasons")
    public List<SeasonPayload> seriesSeasons(SeriesPayload source) {
        return contentQueryService.getSeriesSeasons(source);
    }

    @SchemaMapping(typeName = "Season", field = "episodes")
    public List<EpisodePayload> seasonEpisodes(SeasonPayload source) {
        return contentQueryService.getSeasonEpisodes(source);
    }
}

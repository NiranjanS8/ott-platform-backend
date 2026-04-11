package com.ott.streaming.graphql;

import com.ott.streaming.dto.engagement.AddToWatchlistInput;
import com.ott.streaming.dto.engagement.WatchlistItemPayload;
import com.ott.streaming.entity.ContentType;
import com.ott.streaming.service.WatchlistService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
public class WatchlistGraphQlController {

    private final WatchlistService watchlistService;

    public WatchlistGraphQlController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @MutationMapping
    public WatchlistItemPayload addToWatchlist(@AuthenticationPrincipal(expression = "username") String email,
                                               @Argument @Valid AddToWatchlistInput input) {
        return watchlistService.addToWatchlist(email, input);
    }

    @MutationMapping
    public Boolean removeFromWatchlist(@AuthenticationPrincipal(expression = "username") String email,
                                       @Argument ContentType contentType,
                                       @Argument Long contentId) {
        return watchlistService.removeFromWatchlist(email, contentType, contentId);
    }

    @QueryMapping
    public List<WatchlistItemPayload> myWatchlist(@AuthenticationPrincipal(expression = "username") String email) {
        return watchlistService.getMyWatchlist(email);
    }
}

package com.ott.streaming.graphql;

import com.ott.streaming.dto.engagement.UpdateWatchProgressInput;
import com.ott.streaming.dto.engagement.WatchProgressPayload;
import com.ott.streaming.entity.ContentType;
import com.ott.streaming.service.WatchProgressService;
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
public class WatchProgressGraphQlController {

    private final WatchProgressService watchProgressService;

    public WatchProgressGraphQlController(WatchProgressService watchProgressService) {
        this.watchProgressService = watchProgressService;
    }

    @MutationMapping
    public WatchProgressPayload updateWatchProgress(@AuthenticationPrincipal(expression = "username") String email,
                                                    @Argument @Valid UpdateWatchProgressInput input) {
        return watchProgressService.updateWatchProgress(email, input);
    }

    @MutationMapping
    public WatchProgressPayload markAsCompleted(@AuthenticationPrincipal(expression = "username") String email,
                                                @Argument ContentType contentType,
                                                @Argument Long contentId,
                                                @Argument Long episodeId) {
        return watchProgressService.markAsCompleted(email, contentType, contentId, episodeId);
    }

    @QueryMapping
    public List<WatchProgressPayload> continueWatching(@AuthenticationPrincipal(expression = "username") String email) {
        return watchProgressService.getContinueWatching(email);
    }

    @QueryMapping
    public List<WatchProgressPayload> watchHistory(@AuthenticationPrincipal(expression = "username") String email) {
        return watchProgressService.getWatchHistory(email);
    }
}

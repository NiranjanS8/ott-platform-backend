package com.ott.streaming.graphql;

import com.ott.streaming.dto.playback.PlaybackSessionPayload;
import com.ott.streaming.dto.playback.StartPlaybackInput;
import com.ott.streaming.service.PlaybackService;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
public class PlaybackGraphQlController {

    private final PlaybackService playbackService;

    public PlaybackGraphQlController(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    @MutationMapping
    public PlaybackSessionPayload startPlayback(@AuthenticationPrincipal(expression = "username") String email,
                                                @Argument @Valid StartPlaybackInput input) {
        return playbackService.startPlayback(email, input);
    }
}

package com.ott.streaming.graphql;

import com.ott.streaming.dto.content.MoviePayload;
import com.ott.streaming.dto.content.SeriesPayload;
import com.ott.streaming.dto.subscription.CreateSubscriptionPlanInput;
import com.ott.streaming.dto.subscription.SubscribeToPlanInput;
import com.ott.streaming.dto.subscription.SubscriptionPlanPayload;
import com.ott.streaming.dto.subscription.UpdateContentAccessInput;
import com.ott.streaming.dto.subscription.UpdateSubscriptionPlanInput;
import com.ott.streaming.dto.subscription.UserSubscriptionPayload;
import com.ott.streaming.service.SubscriptionAdminService;
import com.ott.streaming.service.UserSubscriptionService;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
public class SubscriptionGraphQlController {

    private final SubscriptionAdminService subscriptionAdminService;
    private final UserSubscriptionService userSubscriptionService;

    public SubscriptionGraphQlController(SubscriptionAdminService subscriptionAdminService,
                                         UserSubscriptionService userSubscriptionService) {
        this.subscriptionAdminService = subscriptionAdminService;
        this.userSubscriptionService = userSubscriptionService;
    }

    @MutationMapping
    public SubscriptionPlanPayload createSubscriptionPlan(@Argument @Valid CreateSubscriptionPlanInput input) {
        return subscriptionAdminService.createSubscriptionPlan(input);
    }

    @MutationMapping
    public SubscriptionPlanPayload updateSubscriptionPlan(@Argument Long id,
                                                          @Argument @Valid UpdateSubscriptionPlanInput input) {
        return subscriptionAdminService.updateSubscriptionPlan(id, input);
    }

    @MutationMapping
    public MoviePayload updateMovieAccessLevel(@Argument Long id, @Argument @Valid UpdateContentAccessInput input) {
        return subscriptionAdminService.updateMovieAccessLevel(id, input);
    }

    @MutationMapping
    public SeriesPayload updateSeriesAccessLevel(@Argument Long id, @Argument @Valid UpdateContentAccessInput input) {
        return subscriptionAdminService.updateSeriesAccessLevel(id, input);
    }

    @MutationMapping
    public UserSubscriptionPayload subscribeToPlan(@AuthenticationPrincipal(expression = "username") String email,
                                                   @Argument @Valid SubscribeToPlanInput input) {
        return userSubscriptionService.subscribeToPlan(email, input);
    }

    @QueryMapping
    public UserSubscriptionPayload currentSubscription(@AuthenticationPrincipal(expression = "username") String email) {
        return userSubscriptionService.getCurrentSubscription(email);
    }
}

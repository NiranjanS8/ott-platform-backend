package com.ott.streaming.graphql;

import com.ott.streaming.dto.content.MoviePayload;
import com.ott.streaming.dto.content.SeriesPayload;
import com.ott.streaming.dto.subscription.CreateSubscriptionPlanInput;
import com.ott.streaming.dto.subscription.SubscribeToPlanInput;
import com.ott.streaming.dto.subscription.SubscriptionPlanPayload;
import com.ott.streaming.dto.subscription.UpdateContentAccessInput;
import com.ott.streaming.dto.subscription.UpdateSubscriptionPlanInput;
import com.ott.streaming.dto.subscription.UserSubscriptionPayload;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
public class SubscriptionGraphQlController {

    @MutationMapping
    public SubscriptionPlanPayload createSubscriptionPlan(@Argument @Valid CreateSubscriptionPlanInput input) {
        throw new UnsupportedOperationException("Subscription plan mutations will be implemented in phase 5.3");
    }

    @MutationMapping
    public SubscriptionPlanPayload updateSubscriptionPlan(@Argument Long id,
                                                          @Argument @Valid UpdateSubscriptionPlanInput input) {
        throw new UnsupportedOperationException("Subscription plan mutations will be implemented in phase 5.3");
    }

    @MutationMapping
    public MoviePayload updateMovieAccessLevel(@Argument Long id, @Argument @Valid UpdateContentAccessInput input) {
        throw new UnsupportedOperationException("Content access mutations will be implemented in phase 5.3");
    }

    @MutationMapping
    public SeriesPayload updateSeriesAccessLevel(@Argument Long id, @Argument @Valid UpdateContentAccessInput input) {
        throw new UnsupportedOperationException("Content access mutations will be implemented in phase 5.3");
    }

    @MutationMapping
    public UserSubscriptionPayload subscribeToPlan(@Argument @Valid SubscribeToPlanInput input) {
        throw new UnsupportedOperationException("User subscription flow will be implemented in phase 5.4");
    }

    @QueryMapping
    public UserSubscriptionPayload currentSubscription() {
        throw new UnsupportedOperationException("Current subscription query will be implemented in phase 5.4");
    }
}

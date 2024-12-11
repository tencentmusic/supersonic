package com.tencent.supersonic.auth.authentication.strategy;


import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import com.tencent.supersonic.auth.api.authentication.service.UserStrategy;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

@Configuration
@Data
public class UserStrategyFactory {

    private List<UserStrategy> userStrategyList;

    private AuthenticationConfig authenticationConfig;

    public UserStrategyFactory(AuthenticationConfig authenticationConfig,
            List<UserStrategy> userStrategyList) {
        this.authenticationConfig = authenticationConfig;
        this.userStrategyList = userStrategyList;
    }

    @PostConstruct
    public void setUserStrategy() {

        boolean enabled = authenticationConfig.isEnabled();
        if (!enabled) {
            for (UserStrategy userStrategy : userStrategyList) {
                if (userStrategy.accept(authenticationConfig.isEnabled())) {
                    UserHolder.setStrategy(userStrategy);
                }
            }
            return;
        }

        String strategy = authenticationConfig.getStrategy();
        Optional<UserStrategy> strategyOptional = userStrategyList.stream()
                .filter(t -> t.accept(true) && strategy.equalsIgnoreCase(t.getStrategyName()))
                .findAny();

        if (strategyOptional.isPresent()) {
            UserHolder.setStrategy(strategyOptional.get());
        } else {
            throw new IllegalStateException("strategy is not found: " + strategy);
        }
    }
}

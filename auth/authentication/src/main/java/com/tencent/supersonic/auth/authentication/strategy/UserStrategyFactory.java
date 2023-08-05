package com.tencent.supersonic.auth.authentication.strategy;


import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import com.tencent.supersonic.auth.api.authentication.service.UserStrategy;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.Data;
import org.springframework.context.annotation.Configuration;


@Configuration
@Data
public class UserStrategyFactory {


    private List<UserStrategy> userStrategyList;


    private AuthenticationConfig authenticationConfig;

    public UserStrategyFactory(AuthenticationConfig authenticationConfig, List<UserStrategy> userStrategyList) {
        this.authenticationConfig = authenticationConfig;
        this.userStrategyList = userStrategyList;
    }

    @PostConstruct
    public void setUserStrategy() {
        for (UserStrategy userStrategy : userStrategyList) {
            if (userStrategy.accept(authenticationConfig.isEnabled())) {
                UserHolder.setStrategy(userStrategy);
            }
        }
    }
}

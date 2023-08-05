package com.tencent.supersonic.auth.authentication.utils;

import com.tencent.supersonic.auth.api.authentication.adaptor.UserAdaptor;
import org.springframework.core.io.support.SpringFactoriesLoader;
import java.util.Objects;

public class ComponentFactory {

    private static UserAdaptor userAdaptor;

    public static UserAdaptor getUserAdaptor() {
        if (Objects.isNull(userAdaptor)) {
            userAdaptor = init(UserAdaptor.class);
        }
        return userAdaptor;
    }

    private static <T> T init(Class<T> factoryType) {
        return SpringFactoriesLoader.loadFactories(factoryType,
                Thread.currentThread().getContextClassLoader()).get(0);
    }

}

package com.tencent.supersonic.headless.server.pojo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PrestoParametersBuilder extends DefaultParametersBuilder {

    @Override
    public List<DatabaseParameter> build() {
        return super.build();
    }
}

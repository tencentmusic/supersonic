package com.tencent.supersonic.chat.test.context;

import com.tencent.supersonic.chat.infrastructure.mapper.ChatContextMapper;
import com.tencent.supersonic.chat.infrastructure.repository.ChatContextRepositoryImpl;
import com.tencent.supersonic.chat.infrastructure.semantic.DefaultSemanticLayerImpl;
import com.tencent.supersonic.chat.test.ChatBizLauncher;
import com.tencent.supersonic.semantic.core.domain.DimensionService;
import com.tencent.supersonic.semantic.core.domain.DomainService;
import com.tencent.supersonic.semantic.core.domain.MetricService;
import com.tencent.supersonic.semantic.query.domain.QueryService;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

@MockBean(ChatContextRepositoryImpl.class)
@MockBean(QueryService.class)
@MockBean(DimensionService.class)
@MockBean(MetricService.class)
@MockBean(DomainService.class)
@MockBean(ChatContextMapper.class)
@MockBean(RestTemplate.class)
@MockBean(DefaultSemanticLayerImpl.class)
//@MybatisTest
//@AutoConfigureMybatis
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ChatBizLauncher.class)
public class ContextTest {

    protected final Logger logger = LoggerFactory.getLogger(ContextTest.class);
}

package com.tencent.supersonic.chat.test.context;

import com.tencent.supersonic.chat.persistence.repository.impl.ChatContextRepositoryImpl;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import com.tencent.supersonic.chat.persistence.mapper.ChatContextMapper;
import com.tencent.supersonic.knowledge.semantic.RemoteSemanticInterpreter;
import com.tencent.supersonic.chat.test.ChatBizLauncher;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import com.tencent.supersonic.semantic.query.service.QueryService;
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
@MockBean(ModelService.class)
@MockBean(ChatContextMapper.class)
@MockBean(RestTemplate.class)
@MockBean(RemoteSemanticInterpreter.class)
@MockBean(ComponentFactory.class)
//@MybatisTest
//@AutoConfigureMybatis
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ChatBizLauncher.class)
public class ContextTest {

    protected final Logger logger = LoggerFactory.getLogger(ContextTest.class);
}

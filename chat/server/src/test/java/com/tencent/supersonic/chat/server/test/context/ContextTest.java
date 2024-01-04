
package com.tencent.supersonic.chat.server.test.context;

import com.tencent.supersonic.chat.core.knowledge.semantic.RemoteSemanticInterpreter;
import com.tencent.supersonic.chat.core.utils.ComponentFactory;
import com.tencent.supersonic.chat.server.persistence.mapper.ChatContextMapper;
import com.tencent.supersonic.chat.server.persistence.repository.impl.ChatContextRepositoryImpl;
import com.tencent.supersonic.chat.server.service.QueryService;
import com.tencent.supersonic.chat.server.test.ChatBizLauncher;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.service.MetricService;
import com.tencent.supersonic.headless.server.service.ModelService;
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
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ChatBizLauncher.class)
public class ContextTest {

    protected final Logger logger = LoggerFactory.getLogger(ContextTest.class);
}
package com.tencent.supersonic;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {StandaloneLauncher.class})
@ActiveProfiles("local")
public class BaseApplication {

}

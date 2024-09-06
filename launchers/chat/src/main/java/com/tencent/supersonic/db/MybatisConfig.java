package com.tencent.supersonic.db;

import javax.sql.DataSource;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
@MapperScan(value = "com.tencent.supersonic", annotationClass = Mapper.class)
public class MybatisConfig {

    private static final String MAPPER_LOCATION = "classpath*:mapper/**/*.xml";

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        org.apache.ibatis.session.Configuration configuration =
                new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        bean.setConfiguration(configuration);
        bean.setDataSource(dataSource);

        bean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources(MAPPER_LOCATION));
        return bean.getObject();
    }
}

package com.tokenhub.common.mybatis.routing;

import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * O-9：可选启用的动态数据源装配。开关 {@code tokenhub.routing.enabled=true} 时生效。
 *
 * <p>用法：
 * <pre>
 * tokenhub:
 *   routing:
 *     enabled: true
 *     reader:
 *       url: jdbc:mysql://reader.host/...
 *       username: ...
 *       password: ...
 * </pre>
 *
 * <p>未配置 reader 节时，路由数据源仍会注册，但所有路由都落到 writer，便于平滑上线。
 */
@Configuration
@ConditionalOnProperty(prefix = "tokenhub.routing", name = "enabled", havingValue = "true")
public class DataSourceRoutingConfiguration {

  @Bean
  @ConfigurationProperties("spring.datasource")
  public DataSourceProperties writerDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  @ConfigurationProperties("tokenhub.routing.reader")
  public DataSourceProperties readerDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean(name = "writerDataSource")
  public DataSource writerDataSource(
      @org.springframework.beans.factory.annotation.Qualifier("writerDataSourceProperties")
      DataSourceProperties writer
  ) {
    return writer.initializeDataSourceBuilder().type(HikariDataSource.class).build();
  }

  @Bean(name = "readerDataSource")
  public DataSource readerDataSource(
      @org.springframework.beans.factory.annotation.Qualifier("readerDataSourceProperties")
      DataSourceProperties reader
  ) {
    if (reader.getUrl() == null || reader.getUrl().isBlank()) {
      return null;
    }
    return reader.initializeDataSourceBuilder().type(HikariDataSource.class).build();
  }

  @Bean
  @Primary
  public DataSource dataSource(
      @org.springframework.beans.factory.annotation.Qualifier("writerDataSource") DataSource writer,
      @org.springframework.beans.factory.annotation.Qualifier("readerDataSource") DataSource reader
  ) {
    boolean readerAvailable = reader != null;
    DynamicRoutingDataSource routing = new DynamicRoutingDataSource(readerAvailable);
    Map<Object, Object> targets = new HashMap<>();
    targets.put(DynamicRoutingDataSource.WRITER_KEY, writer);
    if (readerAvailable) {
      targets.put(DynamicRoutingDataSource.READER_KEY, reader);
    }
    routing.setTargetDataSources(targets);
    routing.setDefaultTargetDataSource(writer);
    routing.afterPropertiesSet();
    return routing;
  }

  @Value("${tokenhub.routing.enabled:false}")
  private boolean enabled;
}

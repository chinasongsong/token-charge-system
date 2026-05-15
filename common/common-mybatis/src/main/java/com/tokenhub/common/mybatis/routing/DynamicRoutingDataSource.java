package com.tokenhub.common.mybatis.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * O-9：根据 {@link RoutingContext} 在「writer」和「reader」之间选择物理 DataSource。
 *
 * <p>当未配置 reader 时，{@code targetDataSources} 仅含 writer 一项，
 * {@code determineCurrentLookupKey} 永远命中 writer，行为与未启用一致。
 */
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {

  private static final Logger log = LoggerFactory.getLogger(DynamicRoutingDataSource.class);

  public static final String WRITER_KEY = "writer";
  public static final String READER_KEY = "reader";

  private final boolean readerAvailable;

  public DynamicRoutingDataSource(boolean readerAvailable) {
    this.readerAvailable = readerAvailable;
  }

  @Override
  protected Object determineCurrentLookupKey() {
    RoutingContext.Route route = RoutingContext.currentOrDefault();
    if (route == RoutingContext.Route.READER) {
      if (readerAvailable) {
        return READER_KEY;
      }
      log.debug("ReadOnly hint requested but no reader configured; falling back to writer");
    }
    return WRITER_KEY;
  }
}

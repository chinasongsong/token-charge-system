package com.tokenhub.payment.application;

/**
 * O-8：渠道查单端口（PSP 查单）。
 *
 * <p>实现：
 * <ul>
 *   <li>{@code MockChannelQueryPort}：开发与单测，根据本地订单 + 配置开关返回；
 *   <li>{@code WxPayChannelQueryPort} / {@code AliPayChannelQueryPort}：接入真实通道后实现（P5+）。
 * </ul>
 */
public interface ChannelQueryPort {

  enum ChannelStatus {
    PAID,
    UNPAID,
    UNKNOWN
  }

  record QueryResult(ChannelStatus status, Long channelAmount, String channelOrderNo, String raw) {
    public static QueryResult unknown() {
      return new QueryResult(ChannelStatus.UNKNOWN, null, null, null);
    }
  }

  /**
   * 查询渠道侧订单状态。实现需自行做超时/重试。
   *
   * @param channel 渠道名（mock/wechat/alipay）
   * @param orderNo 本地订单号；与渠道单号映射由实现决定
   * @return 查询结果；返回 UNKNOWN 时调用方不得入账
   */
  QueryResult query(String channel, String orderNo);
}

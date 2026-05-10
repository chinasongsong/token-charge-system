package com.tokenhub.adapter.domain.routing;

/**
 * Selects which upstream provider should be tried first (weighted / cost strategy).
 * Failover to the alternate remains in {@link com.tokenhub.adapter.infrastructure.routing.FailoverRoutingAdapter}.
 */
public interface RoutingPolicy {

  ProviderRoute chooseFirstHop();
}

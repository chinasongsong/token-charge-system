package com.tokenhub.common.web.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Configuration;

/**
 * Placeholder MVC configuration bundle; servlet filters ({@link com.tokenhub.common.web.filter.TraceBootstrapFilter})
 * cover trace propagation for MVC services.
 */
@Configuration
@ConditionalOnWebApplication(type = Type.SERVLET)
public class CommonWebMvcConfiguration {
}

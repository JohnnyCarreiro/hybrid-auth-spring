package com.johnnycarreiro.hybridauth.resource.infra.mvc;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the {@link MirrorSyncInterceptor} for the domain routes (SDD-002 §8 F-sync). The
 * liveness probe is excluded — it carries no token and owns no data.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final MirrorSyncInterceptor mirrorSyncInterceptor;

  public WebConfig(MirrorSyncInterceptor mirrorSyncInterceptor) {
    this.mirrorSyncInterceptor = mirrorSyncInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(mirrorSyncInterceptor).excludePathPatterns("/health", "/health/**");
  }
}

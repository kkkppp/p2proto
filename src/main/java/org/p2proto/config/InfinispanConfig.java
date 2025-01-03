package org.p2proto.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import javax.cache.CacheManager;
import java.net.URI;

@Configuration
@EnableCaching
public class InfinispanConfig {

    @Bean
    public CacheManager jCacheManager() throws Exception {
        // Obtain the default CachingProvider (Infinispan's JCache implementation)
        CachingProvider cachingProvider = Caching.getCachingProvider();

        // Load Infinispan configuration from XML
        URI configUri = getClass().getResource("/infinispan-config.xml").toURI();
        javax.cache.CacheManager cacheManager = cachingProvider.getCacheManager(configUri, getClass().getClassLoader());

        return cacheManager;
    }

    @Bean
    public org.springframework.cache.CacheManager springCacheManager(CacheManager jCacheManager) {
        return new JCacheCacheManager(jCacheManager);
    }
}

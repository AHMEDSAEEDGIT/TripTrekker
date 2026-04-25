package com.triptrekker.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonObjectReader;
import org.springframework.data.redis.serializer.JacksonObjectWriter;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.cfg.ConstructorDetector;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(name = "cache.type", havingValue = "redis", matchIfMissing = true)
class CacheConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new ResilientCacheErrorHandler();
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory, CacheProperties cacheProperties) {
        var redisObjectMapper = JsonMapper.builder()
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfSubType("com.triptrekker.")
                                .allowIfSubType("java.")
                                .build(),
                        DefaultTyping.NON_FINAL
                )
                .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                .changeDefaultVisibility(vc -> vc.withCreatorVisibility(JsonAutoDetect.Visibility.ANY))
                .build();

        JacksonObjectWriter writer = (mapper, value) -> mapper.writerFor(Object.class).writeValueAsBytes(value);

        var javaType = redisObjectMapper.constructType(Object.class);
        var jsonSerializer = RedisSerializationContext.SerializationPair
                .fromSerializer(new JacksonJsonRedisSerializer<>(redisObjectMapper, javaType, JacksonObjectReader.create(), writer));

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(cacheProperties.defaultTtl())
                .serializeValuesWith(jsonSerializer);

        Map<String, RedisCacheConfiguration> perCacheConfigs = cacheProperties.caches().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> defaultConfig.entryTtl(e.getValue())
                ));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCacheConfigs)
                .build();
    }
}
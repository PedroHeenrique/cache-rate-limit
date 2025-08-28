package com.pedrojdev.cacheRatelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.function.Supplier;


@Configuration
public class BeanConfiguration {

				@Value("${cache-ttl-seconds:60}")
				private long  ttlSeconds;

				@Value("${REDIS_HOST}")
				private String redisHost;

				@Value("${spring.data.redis.port}")
				private String redisPort;


				@Bean
				public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory){
								RedisCacheConfiguration cacheConfiguration  = RedisCacheConfiguration.defaultCacheConfig()
																.entryTtl(Duration.ofSeconds(ttlSeconds))
																.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new org.springframework.data.redis.serializer.StringRedisSerializer()))
																.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
																.disableCachingNullValues();

								return RedisCacheManager.builder(redisConnectionFactory).cacheDefaults(cacheConfiguration).build();

				}

				@Bean
				public FilterRegistrationBean<Filter> bucket4jFilter(@Qualifier("rateLimiteFilter")Filter filter){
								FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
								registrationBean.setFilter(filter);
								registrationBean.addUrlPatterns("/api/v1/pokemon/*");
								return registrationBean;
				}

				@Bean
				public ProxyManager<String> proxyManager(LettuceConnectionFactory connectionFactory){
								RedisClient redisClient = RedisClient.create("redis://"+redisHost+":"+redisPort);
								StatefulRedisConnection<String, byte[]> redisConnection =
																redisClient.connect(RedisCodec.of(StringCodec.UTF8,ByteArrayCodec.INSTANCE));
								return LettuceBasedProxyManager.builderFor(redisConnection).build();
				}

				@Bean
				public Supplier<BucketConfiguration> bucketConfigurationSupplier (){
								Bandwidth bandwidth  = Bandwidth.builder()
																.capacity(5)
																.refillIntervally(5, Duration.ofMinutes(2))
																.initialTokens(5)
																.build();
								return () -> BucketConfiguration.builder()
																.addLimit(bandwidth)
																.build();
				}

}

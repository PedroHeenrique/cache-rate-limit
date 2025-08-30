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

/**
	* Classe de configuração de beans da aplicação.
	*
	* <p>
	* Configura:
	* <ul>
	*     <li>Cache Redis com TTL configurável</li>
	*     <li>Filter de rate limiting via Bucket4j para endpoints específicos</li>
	*     <li>ProxyManager do Bucket4j para integração com Redis</li>
	*     <li>Supplier de BucketConfiguration com regras de rate limit</li>
	* </ul>
	* </p>
	*/
@Configuration
public class BeanConfiguration {
				/**
					* Tempo de vida em segundos para entradas de cache Redis.
					*/
				@Value("${cache-ttl-seconds:60}")
				private long  ttlSeconds;

				/**
					* Host do Redis configurado via variável de ambiente.
					*/
				@Value("${REDIS_HOST}")
				private String redisHost;

				/**
					* Porta do Redis configurada via propriedade Spring.
					*/
				@Value("${spring.data.redis.port}")
				private String redisPort;


				/**
					* Configura o {@link CacheManager} com Redis, incluindo TTL e serialização.
					*
					* @param redisConnectionFactory fábrica de conexão Redis
					* @return CacheManager configurado
					*/
				@Bean
				public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory){
								RedisCacheConfiguration cacheConfiguration  = RedisCacheConfiguration.defaultCacheConfig()
																.entryTtl(Duration.ofSeconds(ttlSeconds))
																.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new org.springframework.data.redis.serializer.StringRedisSerializer()))
																.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
																.disableCachingNullValues();

								return RedisCacheManager.builder(redisConnectionFactory).cacheDefaults(cacheConfiguration).build();

				}

				/**
					* Registra o {@link Filter} de rate limiting para endpoints específicos.
					*
					* @param filter filtro de rate limit injetado pelo Spring
					* @return FilterRegistrationBean configurado
					*/
				@Bean
				public FilterRegistrationBean<Filter> bucket4jFilter(@Qualifier("rateLimiteFilter")Filter filter){
								FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
								registrationBean.setFilter(filter);
								registrationBean.addUrlPatterns("/api/v1/*");
								return registrationBean;
				}

				/**
					* Cria o {@link ProxyManager} do Bucket4j integrado com Redis via Lettuce.
					*
					* @param connectionFactory conexão Lettuce do Spring
					* @return ProxyManager configurado
					*/

				@Bean
				public ProxyManager<String> proxyManager(LettuceConnectionFactory connectionFactory){
								RedisClient redisClient = RedisClient.create("redis://"+redisHost+":"+redisPort);
								StatefulRedisConnection<String, byte[]> redisConnection =
																redisClient.connect(RedisCodec.of(StringCodec.ASCII,ByteArrayCodec.INSTANCE));
								return LettuceBasedProxyManager.builderFor(redisConnection).build();
				}

				/**
					* Supplier de {@link BucketConfiguration} padrão para rate limiting.
					*
					* <p>
					* Neste caso, cada bucket terá:
					* <ul>
					*     <li>Capacidade máxima de 5 tokens</li>
					*     <li>Refil de 5 tokens a cada 2 minutos</li>
					*     <li>Tokens iniciais: 5</li>
					* </ul>
					* </p>
					*
					* @return Supplier de BucketConfiguration
					*/
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

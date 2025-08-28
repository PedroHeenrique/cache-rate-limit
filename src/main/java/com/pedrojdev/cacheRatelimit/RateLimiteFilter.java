package com.pedrojdev.cacheRatelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class RateLimiteFilter implements Filter {

				private final Logger log = LoggerFactory.getLogger(RateLimiteFilter.class);

				private final ProxyManager<String> proxyManager;

				private final Supplier<BucketConfiguration> bucketConfigurationSupplier;

				public RateLimiteFilter(ProxyManager<String> proxyManager, Supplier<BucketConfiguration> bucketConfigurationSupplier){
								this.proxyManager = proxyManager;
								this.bucketConfigurationSupplier = bucketConfigurationSupplier;
				}

				@Override
				public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse , FilterChain filterChain) throws IOException, ServletException {
								HttpServletRequest request  = (HttpServletRequest) servletRequest;
								HttpServletResponse response = (HttpServletResponse) servletResponse;

								String clientKey = getClientKey(request);

								Bucket bucket = proxyManager.builder().build(clientKey, bucketConfigurationSupplier);

								if(bucket.tryConsume(1)){
												filterChain.doFilter(servletRequest,servletResponse);
								}else{
												handlerRateLimitExceeded(response);
								}

				}

				private String getClientKey(HttpServletRequest request){
								//estrategia para identificar o cliente

								String ip = request.getRemoteAddr();

								String apiKey = request.getHeader("X-API-Key");

								String authHeader = request.getHeader("Authorization");

								log.info("Requicao vinda do ip: {} ", apiKey);
								return apiKey; // ou combinar varios identificadores

				}

				private void handlerRateLimitExceeded(HttpServletResponse response)throws IOException{
								response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
								response.setContentType(MediaType.APPLICATION_JSON_VALUE);

								ObjectMapper objectMapper = new ObjectMapper();
								objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
								log.warn("Rate limit atingido");
								Map<String,Object> erroDetails = new HashMap<>();
								erroDetails.put("timestamp", LocalDateTime.now());
								erroDetails.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
								erroDetails.put("error","Too many Requests");
								erroDetails.put("message", "Rate limit exceeded. Please try again later");

								response.getWriter().write(objectMapper.writeValueAsString(erroDetails));

				}
}

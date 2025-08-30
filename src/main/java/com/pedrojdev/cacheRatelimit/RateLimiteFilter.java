package com.pedrojdev.cacheRatelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
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
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
	* Filtro de rate limiting que utiliza o Bucket4j para controlar a quantidade de requisições
	* que um cliente pode fazer à API em um determinado intervalo de tempo.
	*
	* <p>
	* Este filtro é registrado como um bean Spring (@Component) e pode ser associado
	* a URLs específicas usando um {@link FilterRegistration}.
	* Ele funciona utilizando um {@link ProxyManager} para armazenar o estado do bucket
	* (tokens disponíveis) em Redis ou outra implementação de armazenamento suportada pelo Bucket4j.
	* </p>
	*
	* <p>
	* O filtro adiciona headers de controle de rate limit na resposta:
	* <ul>
	*     <li>{@code X-Rate-Limit-Remaining} - tokens restantes</li>
	*     <li>{@code X-Rate-Limit-Retry-After-Seconds} - tempo em segundos até o próximo token estar disponível</li>
	* </ul>
	* </p>
	*/

@Component
public class RateLimiteFilter implements Filter {

				private final Logger log = LoggerFactory.getLogger(RateLimiteFilter.class);

				private final ProxyManager<String> proxyManager;

				private final Supplier<BucketConfiguration> bucketConfigurationSupplier;

				/**
					* Construtor do filtro.
					*
					* @param proxyManager gerenciador de buckets
					* @param bucketConfigurationSupplier fornecedor da configuração do bucket
					*/
				public RateLimiteFilter(ProxyManager<String> proxyManager, Supplier<BucketConfiguration> bucketConfigurationSupplier){
								this.proxyManager = proxyManager;
								this.bucketConfigurationSupplier = bucketConfigurationSupplier;
				}


				/**
					* Método principal do filtro que intercepta cada requisição HTTP.
					* Verifica se o cliente ainda possui tokens disponíveis no bucket.
					*
					* <p>
					*
					* @param servletRequest requisição HTTP
					* @param servletResponse resposta HTTP
					* @param filterChain cadeia de filtros
					* @throws IOException se houver erro de IO ao escrever na resposta
					* @throws ServletException se houver erro ao processar o filtro
					*/

				@Override
				public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse , FilterChain filterChain) throws IOException, ServletException {
								HttpServletRequest request  = (HttpServletRequest) servletRequest;
								HttpServletResponse response = (HttpServletResponse) servletResponse;

								String clientKey = getClientKey(request);

								Bucket bucket = proxyManager.builder().build(clientKey, bucketConfigurationSupplier);

								ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);


								if(probe.isConsumed()){
												response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
												filterChain.doFilter(servletRequest,servletResponse);
								}else{
												long nanosToWait = probe.getNanosToWaitForRefill();
												long secondsToWait = TimeUnit.NANOSECONDS.toSeconds(nanosToWait);
												response.setHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(secondsToWait));
												handlerRateLimitExceeded(response);
								}

				}

				/**
					* Define a chave do cliente utilizada pelo Bucket4j para identificar o bucket.
					* Pode usar IP, API Key, user logado ou combinação destes.
					*
					* @param request requisição HTTP
					* @return chave do cliente
					*/
				private String getClientKey(HttpServletRequest request){
								//estrategia para identificar o cliente

								String ip = request.getRemoteAddr();

								String apiKey = request.getHeader("X-API-Key");

								String authHeader = request.getHeader("Authorization");

								log.info("Requicao vinda do ip: {} ", apiKey);
								return apiKey; // ou combinar varios identificadores

				}

				/**
					* Retorna resposta HTTP 429 (Too Many Requests) com corpo JSON informando
					* o erro e timestamp.
					*
					* @param response resposta HTTP
					* @throws IOException se houver erro ao escrever no corpo da resposta
					*/
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

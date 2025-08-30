package com.pedrojdev.cacheRatelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


/**
	* Aspecto de rate limiting que aplica limites de requisições a métodos anotados com {@link RateLimited}.
	*
	* <p>
	* Esta classe intercepta chamadas de métodos específicos da API e utiliza o Bucket4j para controlar
	* a quantidade de requisições que um cliente pode fazer dentro de um intervalo de tempo configurado.
	* Pode aplicar limites por IP, por usuário (ainda a implementar) ou de forma global.
	* </p>
	*
	* <p>
	* O aspecto adiciona headers na resposta HTTP para informar ao cliente sobre o estado do rate limit:
	* <ul>
	*     <li>{@code X-Rate-Limit-Remaining} - tokens restantes no bucket</li>
	*     <li>{@code X-Rate-Limit-Retry-After-Seconds} - tempo em segundos até o próximo token estar disponível</li>
	* </ul>
	* </p>
	*/
@Aspect
@Component
public class RateLimitAspect {

				private final ProxyManager<String> proxyManager;

				private final HttpServletRequest servletHttpRequest;

				private final HttpServletResponse servletResponse;

				/**
					* Construtor do aspecto.
					*
					* @param proxyManager gerenciador de buckets
					* @param servletHttpRequest requisição HTTP atual
					* @param servletResponse resposta HTTP atual
					*/
				public RateLimitAspect(ProxyManager<String> proxyManager, HttpServletRequest servletHttpRequest, HttpServletResponse servletResponse) {
								this.proxyManager = proxyManager;
								this.servletHttpRequest = servletHttpRequest;
								this.servletResponse = servletResponse;
				}

				/**
					* Intercepta métodos anotados com {@link RateLimited} e aplica a lógica de rate limit.
					*
					* <p>
					* Se houver tokens disponíveis:
					* <ul>
					*     <li>Decrementa um token do bucket</li>
					*     <li>Adiciona o header {@code X-Rate-Limit-Remaining}</li>
					*     <li>Permite a execução do método</li>
					* </ul>
					* </p>
					* <p>
					* Se não houver tokens disponíveis:
					* <ul>
					*     <li>Calcula o tempo até o próximo token estar disponível</li>
					*     <li>Adiciona o header {@code X-Rate-Limit-Retry-After-Seconds}</li>
					*     <li>Lança {@link RateLimitExceededException}</li>
					* </ul>
					* </p>
					*
					* @param joinPoint ponto de execução do método interceptado
					* @param rateLimited anotação {@link RateLimited} aplicada ao método
					* @return resultado do método interceptado caso o rate limit permita
					* @throws Throwable se ocorrer erro durante a execução do método
					*/

				@Around("@annotation(rateLimited)")
				public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable{
								String bucketKey = generateBucketKey(joinPoint, rateLimited);

								Bucket bucket = proxyManager.builder().build(bucketKey,bucketConfigurationSupplier(rateLimited));

								ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);


				     if(!probe.isConsumed()){
													long nanosToWait = probe.getNanosToWaitForRefill();
													long seconds = TimeUnit.NANOSECONDS.toSeconds(nanosToWait);
													servletResponse.setHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(seconds));
													throw new RateLimitExceededException("Rate limit exceeded");
								 }

         servletResponse.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
									return joinPoint.proceed();

				}

				/**
					* Cria a configuração do bucket baseada nos parâmetros da anotação {@link RateLimited}.
					*
					* @param rateLimited anotação aplicada ao método
					* @return fornecedor de {@link BucketConfiguration} para o Bucket4j
					*/
				private Supplier<BucketConfiguration> bucketConfigurationSupplier(RateLimited rateLimited){
								Bandwidth bandwidth  = Bandwidth.builder()
																.capacity(rateLimited.maxCapacity())
																.refillIntervally(rateLimited.tokensPerTimeWindow(), Duration.ofSeconds(rateLimited.timeWindow()))
																.initialTokens(rateLimited.initialTokens())
																.build();
								return () -> BucketConfiguration.builder()
																.addLimit(bandwidth)
																.build();
				}

				/**
					* Gera a chave do bucket usada para identificar o cliente.
					* Pode variar dependendo do escopo definido na anotação:
					* <ul>
					*     <li>"IP" - chave baseada no endereço IP da requisição</li>
					*     <li>"USER" - chave baseada no usuário autenticado (TODO: implementar)</li>
					*     <li>default - chave global</li>
					* </ul>
					*
					* @param joinPoint ponto de execução do método
					* @param rateLimited anotação aplicada ao método
					* @return chave do bucket
					*/

				private String generateBucketKey(ProceedingJoinPoint joinPoint, RateLimited rateLimited) {
								return switch (rateLimited.scope()){
												case  "IP" -> servletHttpRequest.getRemoteAddr();
												//TODO: implementar spring security
												case  "USER" -> "";
												default -> "GLOBAL";
								};
				}


}

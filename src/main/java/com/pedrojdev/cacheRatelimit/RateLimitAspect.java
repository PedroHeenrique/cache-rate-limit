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

@Aspect
@Component
public class RateLimitAspect {

				private final ProxyManager<String> proxyManager;

				private final HttpServletRequest servletHttpRequest;

				private final HttpServletResponse servletResponse;


				public RateLimitAspect(ProxyManager<String> proxyManager, HttpServletRequest servletHttpRequest, HttpServletResponse servletResponse) {
								this.proxyManager = proxyManager;
								this.servletHttpRequest = servletHttpRequest;
								this.servletResponse = servletResponse;
				}

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



				private String generateBucketKey(ProceedingJoinPoint joinPoint, RateLimited rateLimited) {
								return switch (rateLimited.scope()){
												case  "IP" -> servletHttpRequest.getRemoteAddr();
												case  "USER" -> "PEDRO";
												default -> "GLOBAL";
								};
				}


}

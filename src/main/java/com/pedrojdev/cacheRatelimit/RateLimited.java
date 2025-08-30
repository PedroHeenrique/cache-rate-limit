package com.pedrojdev.cacheRatelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RateLimited {
				int tokensPerTimeWindow() default 10;
				int timeWindow() default 1;
				int maxCapacity() default 10;
				int initialTokens() default 10;
				String scope() default "GLOBAL";
}

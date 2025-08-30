package com.pedrojdev.cacheRatelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
	* Anotação para definir limites de requisições (rate limit) em métodos de API.
	*
	* <p>
	* Esta anotação deve ser aplicada a métodos que precisam ser controlados quanto à
	* quantidade de requisições que um cliente pode realizar dentro de um intervalo de tempo.
	* Ela é processada por um {@link RateLimitAspect} ou outro mecanismo que implemente
	* a lógica de rate limiting.
	* </p>
	*
	* <p>
	* Exemplo de uso:
	* <pre>{@code
	* @GetMapping("/pokemon")
	* @RateLimited(maxCapacity = 5, initialTokens = 5, tokensPerTimeWindow = 2, timeWindow = 60, scope = "IP")
	* public ResponseEntity<?> getPokemon(@RequestParam String nome) { ... }
	* }</pre>
	* </p>
	*/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RateLimited {
				int tokensPerTimeWindow() default 10;
				int timeWindow() default 1;
				int maxCapacity() default 10;
				int initialTokens() default 10;
				String scope() default "GLOBAL";
}

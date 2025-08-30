package com.pedrojdev.cacheRatelimit;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


/**
	* Exceção lançada quando um cliente excede o limite de requisições (rate limit) permitido.
	*
	* <p>
	* Esta exceção é utilizada em conjunto com {@link RateLimitAspect} ou filtros de rate limiting
	* para interromper a execução de um método ou requisição quando não há tokens disponíveis no bucket.
	* </p>
	*
	* <p>
	* Quando lançada, o Spring MVC automaticamente retorna o status HTTP {@link HttpStatus#TOO_MANY_REQUESTS (429)}.
	* </p>
	*
	* <p>
	* Exemplo de uso:
	* <pre>{@code
	* if (!bucket.tryConsume(1)) {
	*     throw new RateLimitExceededException("Rate limit exceeded");
	* }
	* }</pre>
	* </p>
	*/
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitExceededException extends RuntimeException {
				/**
					* Construtor da exceção.
					*
					* @param message mensagem detalhando o motivo da exceção
					*/
				public RateLimitExceededException(String message){
								super(message);
				}
}

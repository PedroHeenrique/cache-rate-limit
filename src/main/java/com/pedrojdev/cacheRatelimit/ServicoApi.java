package com.pedrojdev.cacheRatelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ServicoApi {
    @Value("${api-url}")
				private String url;

				private final RestTemplate rest = new RestTemplate();

				@Cacheable(value = "pokemonCache", key = "#nome", unless = "#result == null")
				public Object get(String nome){
								return rest.getForObject(url + nome.toLowerCase(), Object.class);
				}
}

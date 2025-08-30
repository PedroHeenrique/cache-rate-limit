package com.pedrojdev.cacheRatelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
	* Serviço responsável por consumir a API externa.
	*
	* <p>
	* Este serviço utiliza {@link RestTemplate} para realizar chamadas HTTP GET
	* e {@link Cacheable} do Spring para armazenar respostas em cache, evitando
	* chamadas repetidas para o mesmo recurso.
	* </p>
	*
	* <p>
	* A URL base da API externa é configurada via property {@code api-url} no arquivo
	* {@code application.properties} ou {@code application.yml}.
	* </p>
	*
	* <p>
	* Exemplo de uso:
	* <pre>{@code
	* ServicoApi servicoApi = new ServicoApi();
	* Object resultado = servicoApi.get("pikachu");
	* }</pre>
	* </p>
	*/

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

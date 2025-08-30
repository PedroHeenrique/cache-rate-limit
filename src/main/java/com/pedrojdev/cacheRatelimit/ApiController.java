package com.pedrojdev.cacheRatelimit;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("api/v1")
public class ApiController {

				private static Logger log = LoggerFactory.getLogger(ApiController.class);

				private final ServicoApi servicoApi;

				public ApiController(ServicoApi servicoApi) {
								this.servicoApi = servicoApi;
				}


				@GetMapping("/pokemon/")
				@RateLimited(maxCapacity = 5,initialTokens = 5 ,tokensPerTimeWindow = 2, scope = "IP")
				public ResponseEntity<?> getPokemon(@RequestParam(name = "nome") String nome){
        log.info("Requisicao recebida {}", nome);
								Object result = servicoApi.get(nome);
								return ResponseEntity.ok(result);
				}
}

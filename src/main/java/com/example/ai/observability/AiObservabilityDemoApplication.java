package com.example.ai.observability;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import reactor.core.publisher.Flux;

/**
 * https://programmingtechie.com/2023/09/09/spring-boot3-observability-grafana-stack/
 */
@SpringBootApplication
public class AiObservabilityDemoApplication {

	private final Logger logger = LoggerFactory.getLogger(AiObservabilityDemoApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(AiObservabilityDemoApplication.class, args);
	}

	@Bean
	CommandLineRunner test(ChatClient.Builder builder) {
		ChatClient ai = builder.build();
		return args -> {
			for (int i = 0; i < 150; i++) {
				// var response = ai.prompt()	
				// 		.options(OpenAiChatOptions.builder().withSeed(new Random().nextInt()).build())
				// 		.user("tell me a joke?")
				// 		.call().chatResponse();
				// logger.info("Response: {}", response.getResult().getOutput().getContent());


				Flux<String> streamResponse = ai.prompt()	
						.options(OpenAiChatOptions.builder().withSeed(new Random().nextInt()).build())
						.user("tell me a joke?")
						.stream().content();
				
				streamResponse.collectList().block().stream().forEach(s -> logger.info("Stream Response: {}", s));

				Thread.sleep(10000);
			}
		};
	}

	// @Bean
	// @ConditionalOnMissingBean
	// ObservedAspect observedAspect(ObservationRegistry registry) {
	// 	return new ObservedAspect(registry);
	// }
}

package com.example.ai.observability;

import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;
import org.springframework.http.server.observation.ServerRequestObservationContext;

import io.micrometer.observation.ObservationPredicate;
import reactor.core.publisher.Flux;

@SpringBootApplication
public class AiObservabilityDemoApplication {

	private final Logger logger = LoggerFactory.getLogger(AiObservabilityDemoApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(AiObservabilityDemoApplication.class, args);
	}

	@Bean
	CommandLineRunner test(ChatClient.Builder builder, EmbeddingModel embeddingModel, DataLoadingService loader,
			VectorStore vectorStore) {

		// loader.load();
		var chatMemory = new InMemoryChatMemory();

		ChatClient chatClient = builder.build();

		return args -> {
			for (int i = 0; i < 5; i++) {

				functionCalling(chatClient);

				// functionCallingStreaming(chatClient);

				// questionAnswerWithChatMemory(chatClient, chatMemory, vectorStore);

				// questionAnswerWithChatMemoryStreaming(chatClient, chatMemory,
				// vectorStore);

				Thread.sleep(5000);
			}
		};

	}

	private void questionAnswerWithChatMemory(ChatClient chatClient, InMemoryChatMemory chatMemory,
			VectorStore vectorStore) {

		var response = chatClient.prompt()
			.user("How does Carina work?")
			.advisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()))
			.advisors(new PromptChatMemoryAdvisor(chatMemory))
			.call()
			.chatResponse();
		logger.info("Response: {}", response.getResult().getOutput().getContent());

	}

	private void questionAnswerWithChatMemoryStreaming(ChatClient chatClient, InMemoryChatMemory chatMemory,
			VectorStore vectorStore) {

		var response = chatClient.prompt()
			.user("How does Carina work?")
			.advisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()))
			.advisors(new PromptChatMemoryAdvisor(chatMemory))
			.stream()
			.chatResponse();

		response.collectList().block().stream().forEach(s -> logger.info("Stream Response: {}", s));

	}

	// Function calling

	private void functionCalling(ChatClient chatClient) {

		String response = chatClient.prompt()
			.user("What is the status of my payment transactions 001, 002 and 003?")
			.functions("paymentStatus")
			.call()
			.content();

		logger.info("\n\n Response: {} \n\n", response);

	}

	private void functionCallingStreaming(ChatClient chatClient) {

		Flux<String> response = chatClient.prompt()
			.user("What is the status of my payment transactions 001, 002 and 003?")
			.functions("paymentStatus")
			.stream()
			.content();

		response.collectList().block().stream().forEach(s -> logger.info("Stream Response: {}", s));

	}

	record Transaction(String id) {
	}

	record Status(String name) {
	}

	@Bean
	@Description("Get the status of a single payment transaction")
	public Function<Transaction, Status> paymentStatus() {
		return transaction -> {
			logger.info("Single transaction: " + transaction);
			return DATASET.get(transaction);
		};
	}

	static final Map<Transaction, Status> DATASET = Map.of(new Transaction("001"), new Status("pending"),
			new Transaction("002"), new Status("approved"), new Transaction("003"), new Status("rejected"));

	// Optional suppress the actuator server observations. This hides the actuator
	// prometheus traces.
	@Bean
	ObservationPredicate noActuatorServerObservations() {
		return (name, context) -> {
			if (name.equals("http.server.requests")
					&& context instanceof ServerRequestObservationContext serverContext) {
				return !serverContext.getCarrier().getRequestURI().startsWith("/actuator");
			}
			else {
				return true;
			}
		};
	}

}

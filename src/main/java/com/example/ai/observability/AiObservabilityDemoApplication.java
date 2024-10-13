package com.example.ai.observability;

import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import org.springframework.ai.azure.openai.AzureOpenAiChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
// import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.api.OllamaOptions;
// import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
// import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;
import org.springframework.http.server.observation.ServerRequestObservationContext;

import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import reactor.core.publisher.Flux;

@SpringBootApplication
public class AiObservabilityDemoApplication {

	private final Logger logger = LoggerFactory.getLogger(AiObservabilityDemoApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(AiObservabilityDemoApplication.class, args);
	}

	static Random random = new Random();
	@Bean
	CommandLineRunner test(ChatClient.Builder builder, EmbeddingModel embeddingModel, DataLoadingService loader,
			VectorStore vectorStore, ObservationRegistry registry) {

		loader.load();
		var chatMemory = new InMemoryChatMemory();

		ChatClient chatClient = builder.build();

		return args -> {
			for (int i = 0; i < 500; i++) {

				callWithError(chatClient, chatMemory, i);

				// tellMeAJoke(chatClient, chatMemory);

				// functionCalling(chatClient);

				// functionCallingStreaming(chatClient);

				questionAnswerWithChatMemory2(chatClient, chatMemory, vectorStore, registry); // BLa

				// questionAnswerWithChatMemoryStreaming2(chatClient, chatMemory, vectorStore);

				Thread.sleep(500 + random.nextInt(500));
			}
		};

	}

	private void callWithError(ChatClient chatClient, InMemoryChatMemory chatMemory, int index) {
		
		double temperature = 0.7;
		int maxTokenSize = 500;
		if (index % 10 == 0) {
			maxTokenSize = -1;
			temperature = -0.7;
		}

		// int numBatch = -1;
		int numBatch = 500;
		
		if (index % 10 == 0) {
			numBatch = -1;
		}
		try {
			var response = chatClient.prompt()
				.options(OllamaOptions.builder().withNumBatch(numBatch))
				// .options(OpenAiChatOptions.builder().withMaxTokens(maxTokenSize).build())
				// .options(AzureOpenAiChatOptions.builder().withMaxTokens(maxTokenSize).build())
				// .options(VertexAiGeminiChatOptions.builder().withTemperature(temperature).build())
				.user("Tell me a different joke?")
				.advisors(new PromptChatMemoryAdvisor(chatMemory))
				.call()
				.chatResponse();
			logger.info("Response: {}", response.getResult().getOutput().getContent());
		}
		catch (Exception e) {
			logger.error("Error: {}", e.getMessage());
		}

	}

	private void tellMeAJoke(ChatClient chatClient, InMemoryChatMemory chatMemory) {

		for (int i = 1; i < 6; i++) {

			var maxTokenSize = new Random().nextInt(100) + 10;

			var response = chatClient.prompt()
				// .options(OpenAiChatOptions.builder().withMaxTokens(maxTokenSize).build())
				.user("Tell me a different joke?")
				.advisors(new PromptChatMemoryAdvisor(chatMemory))
				.call()
				.chatResponse();
			logger.info("Response: {}", response.getResult().getOutput().getContent());

			try {
				Thread.sleep(10000);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	private void questionAnswerWithChatMemory(ChatClient chatClient, InMemoryChatMemory chatMemory,
			VectorStore vectorStore) {

		var response = chatClient.prompt()
			.user("How does Carina work?")
			.advisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()))
			// .advisors(new PromptChatMemoryAdvisor(chatMemory))
			// .advisors(new SafeGuardAroundAdvisor(List.of("bla bla")))
			.call()
			.chatResponse();
		logger.info("Response: {}", response.getResult().getOutput().getContent());

	}

	private void questionAnswerWithChatMemory2(ChatClient chatClient, InMemoryChatMemory chatMemory,
			VectorStore vectorStore, ObservationRegistry registry) {

		var response = chatClient.prompt()
			.user("How does Carina work?")
			.advisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()))
			.advisors(new PromptChatMemoryAdvisor(chatMemory))
			// .advisors(new SafeGuardAdvisor(List.of("bla bla")))
			.advisors(new VectorStoreChatMemoryAdvisor(vectorStore))
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
			// .advisors(new SafeGuardAdvisor(List.of("bla bla")))
			.stream()
			.chatResponse();

		response.collectList().block().stream().forEach(s -> logger.info("Stream Response: {}", s));

	}

	private void questionAnswerWithChatMemoryStreaming2(ChatClient chatClient, InMemoryChatMemory chatMemory,
			VectorStore vectorStore) {

		var response = chatClient.prompt()
			// .user("How does Carina work?")
			.system("Questions about the transactions status are realtime questions. You can not use the memory to answer the question.")
			.user("What is the status of my payment transactions 001, 002 and 003?")
			.functions("paymentStatus")
			.advisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()))
			.advisors(new PromptChatMemoryAdvisor(chatMemory))
			.advisors(new VectorStoreChatMemoryAdvisor(vectorStore))
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

	// @Bean
	// public ObservationHandler<?> bla(Tracer tracer) {
	// 	return new ErrorLoggingObservationHandler(tracer,
	// 			List.of(ChatClientObservationContext.class, ChatModelObservationContext.class));
	// 	// return new ErrorLoggingObservationHandler(tracer,
	// 	// List.of(ChatClientObservationContext.class, ChatModelObservationContext.class),
	// 	// error -> logger.error("Error in chat client", error));
	// }

	// @Bean
	// public ObservationHandler<ChatClientObservationContext> observationHandler(Tracer tracer) {
	// 	return new ObservationHandler<ChatClientObservationContext>() {

	// 		@Override
	// 		public boolean supportsContext(Context context) {
	// 			return context instanceof ChatClientObservationContext;
	// 		}

	// 		@Override
	// 		public void onStart(ChatClientObservationContext context) {
	// 			TracingContext tracingContext = context.get(TracingContext.class);
	// 			if (tracingContext != null) {
	// 				try (var val = tracer.withSpan(tracingContext.getSpan())) {
	// 					logger.info("Context Name: " + context.getContextualName());
	// 				}
	// 			}
	// 		}

	// 		@Override
	// 		public void onError(ChatClientObservationContext context) {
	// 			TracingContext tracingContext = context.get(TracingContext.class);
	// 			if (tracingContext != null) {
	// 				try (var val = tracer.withSpan(tracingContext.getSpan())) {
	// 					logger.error("Error in chat client", context.getError());
	// 				}
	// 			}
	// 		}

	// 	};

	// }

	// @SuppressWarnings("rawtypes")
	// public static class ErrorLoggingObservationHandler implements ObservationHandler {

	// 	private static final Logger logger = LoggerFactory.getLogger(ErrorLoggingObservationHandler.class);

	// 	private final Tracer tracer;

	// 	private final List<Class<? extends Observation.Context>> supportedContextTypes;

	// 	private final Consumer<Context> errorConsumer;

	// 	public ErrorLoggingObservationHandler(Tracer tracer,
	// 			List<Class<? extends Observation.Context>> supportedContextTypes) {
	// 		this(tracer, supportedContextTypes, context -> logger.error("ERROR: ", context.getError()));
	// 	}

	// 	public ErrorLoggingObservationHandler(Tracer tracer,
	// 			List<Class<? extends Observation.Context>> supportedContextTypes, Consumer<Context> errorConsumer) {

	// 		Assert.notNull(tracer, "Tracer must not be null");
	// 		Assert.notNull(supportedContextTypes, "SupportedContextTypes must not be null");
	// 		Assert.notNull(errorConsumer, "ErrorConsumer must not be null");

	// 		this.tracer = tracer;
	// 		this.supportedContextTypes = supportedContextTypes;
	// 		this.errorConsumer = errorConsumer;
	// 	}

	// 	@Override
	// 	public boolean supportsContext(Context context) {
	// 		return (context == null) ? false
	// 				: this.supportedContextTypes.stream().anyMatch(clz -> clz.isInstance(context));
	// 	}

	// 	@Override
	// 	public void onError(Context context) {
	// 		if (context == null) {
	// 			return;
	// 		}
	// 		TracingContext tracingContext = context.get(TracingContext.class);
	// 		if (tracingContext != null) {
	// 			try (var val = this.tracer.withSpan(tracingContext.getSpan())) {
	// 				this.errorConsumer.accept(context);
	// 			}
	// 		}
	// 	}

	// }

}

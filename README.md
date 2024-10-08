# Read Me First

## RAG Example 

Chat client call with Question/Answering (aka RAG) and ChatMemory configurations:

```java
var response = chatClient.prompt()
    .user("How does Carina work?")
    .advisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()))
    .advisors(new PromptChatMemoryAdvisor(chatMemory))
    .call()
    .chatResponse();

```

The traces would looks like:

![ChatClient with RAG and ChatMemory](/doc/rag_with_memory.png "ChatClient with RAG and ChatMemory")

On call the `chat-client` calls:
* the `Before` `question-answer-advisor`. 
Internally it uses the `pg-vector-store` to retrieve the similar documents and the `open-ai-embedding-model` to encode the input user question. Internally it uses the OpenAiApi REST client.
* the `Before` `prompt-chat-memory-advisor`to retrieve the history and store the user message.
* then the chat-client uses the `openai-chat-model` (gpt-4o) perform the chatcompletion request. Later delegates to inner REST client (OpenAiApi).
* finally it visits the `After` QA and memory advisors and returns the response.

## Function Calling Examples

Call the function `paymentStatus` for 3 different transaction. 
But enforce non-parallel mode. So for each transaction the LLM will have to return and separate tool call massage.

```java
    String response = chatClient.prompt()
        .options(OpenAiChatOptions.builder()
            .withParallelToolCalls(false).build())
        .user("What is the status of my payment transactions 001, 002 and 003?")
        .functions("paymentStatus")
        .call()
        .content();

```

This produces traces like:

![Function Calling non-parallel](/doc/funciton_calling_sequential.png "Function Calling non-parallel")

Diagram shows that 3 consecutive tool calls are performed before the final result. 

If the parallel calling is enabled the diagram looks like this:


![Function Calling parallel](/doc/function_calling_parallel.png "Function Calling parallel")

## Random links

* https://docs.micrometer.io/micrometer/reference/observation/introduction.html
* https://programmingtechie.com/2023/09/09/spring-boot3-observability-grafana-stack/
* https://www.baeldung.com/spring-boot-3-observability
* https://medium.com/@ahmadalammar/comprehensive-observability-in-spring-boot-using-opentelemetry-prometheus-grafana-tempo-and-067196eee539
* https://medium.com/@ahmadalammar/comprehensive-observability-in-spring-boot-using-opentelemetry-prometheus-grafana-tempo-and-4d50c2f2b711
* https://medium.com/@ahmadalammar/comprehensive-observability-in-spring-boot-using-opentelemetry-prometheus-grafana-tempo-and-e08842ae96be


## Observability

- For streaming make sure to enable the context propagation:
`spring.reactor.context-propagation=auto`

- For percentiles histograms enable for the target metric
`management.metrics.distribution.percentiles-histogram.<metric name>=true`

For example for AI model calls:
`management.metrics.distribution.percentiles-histogram.gen_ai.client.operation=true`
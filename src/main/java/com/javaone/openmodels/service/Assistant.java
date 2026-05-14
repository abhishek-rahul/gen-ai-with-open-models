package com.javaone.openmodels.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

public interface Assistant {

    // [ LLM / ChatModel: AiServices is a LangChain4j abstraction over the configured ChatLanguageModel. ]
    @SystemMessage("You are a helpful assistant for a Java development team.")
    String chat(@UserMessage String message);

    // [ PromptTemplate: variables are injected into the prompt instead of string-concatenating user input. ]
    @SystemMessage("You are a {role}. Answer about {topic}. Keep the answer practical for Java developers.")
    String answerAsRole(@V("role") String role, @V("topic") String topic, @UserMessage String question);

    // [ Output Parsers: LangChain4j converts model output into a Java List<String> for the caller. ]
    @UserMessage("""
        Return exactly three short bullet items as a plain list.
        Topic: {{topic}}
        """)
    List<String> threeLearningPoints(@V("topic") String topic);

}

package com.javaone.openmodels.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface Assistant {

    @SystemMessage("You are a helpful assistant for a Java development team.")
    String chat(@UserMessage String message);
}

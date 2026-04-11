package com.ican.service;

import reactor.core.publisher.Flux;

public interface AiWriteAssistService {

    Flux<String> writeAssist(String action, String content);
}

package com.ican.service;

import com.ican.model.vo.AiChatResponseVO;

public interface AiRagService {

    AiChatResponseVO chat(String question);
}

package com.ican.service;

import com.ican.model.dto.ChatMesDTO;
import com.ican.model.vo.ChatRecordVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Author：yep
 * @Project：blog-springboot
 * @name：ChatService
 * @Date：2024/11/25 16:29
 * @Filename：ChatService
 */
public interface ChatService {
    List<ChatRecordVO> getByCouple(String s, String s1);

    String getUserUid();

    void addChat(ChatMesDTO message);
}

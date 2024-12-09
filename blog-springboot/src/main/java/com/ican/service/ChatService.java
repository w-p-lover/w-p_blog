package com.ican.service;

import com.ican.model.dto.ChatMesDTO;
import com.ican.model.vo.FriendshipVO;
import com.ican.model.vo.ChatRecordVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Author：yep
 * @Project：blog-springboot
 * @name：ChatService
 * @Date：2024/11/25 16:29
 * @Filename：ChatService
 */
public interface ChatService {
    List<ChatRecordVO> getChatRecordByCouple(String s, String s1);

    String getUserUid();

    void addChat(ChatMesDTO message);

    List<FriendshipVO> getFriendList(Integer userId);

    String uploadTalkFile(String type, MultipartFile file);
}

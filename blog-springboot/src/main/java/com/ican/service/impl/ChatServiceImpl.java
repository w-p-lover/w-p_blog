package com.ican.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ican.entity.Chat;
import com.ican.entity.User;
import com.ican.exception.ServiceException;
import com.ican.mapper.ChatMapper;
import com.ican.mapper.UserMapper;
import com.ican.model.dto.ChatMesDTO;
import com.ican.model.vo.ChatRecordVO;
import com.ican.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author：yep
 * @Project：blog-springboot
 * @name：CahtServiceImlp
 * @Date：2024/11/25 16:29
 * @Filename：CahtServiceImlp
 */
@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatMapper chatRecordMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public List<ChatRecordVO> getByCouple(String send, String receive) {
        List<ChatRecordVO> chatMessages = new ArrayList<>();
        int senderId = Integer.parseInt(send);
        int receiverId = Integer.parseInt(receive);
        List<Chat> chatRecords = chatRecordMapper.selectByCouple(senderId, receiverId);
        for (Chat chatRecord : chatRecords) {
            ChatRecordVO chatRecordVO = new ChatRecordVO();
            BeanUtil.copyProperties(chatRecord,chatRecordVO);
            chatRecordVO.setSenderAvatar("https://s2.loli.net/2024/11/27/jt6igkmJRo3VfGK.png");
            chatMessages.add(chatRecordVO);
        }
        return chatMessages;
    }

    @Override
    public String getUserUid() {
        if (StpUtil.isLogin()) {
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .select(User::getId)
                    .eq(User::getId, StpUtil.getLoginIdAsInt()));
            return user.getId().toString();
        }else{
            throw new ServiceException("用户未登录,登录后可见");
        }
    }

    @Override
    public void addChat(ChatMesDTO message) {
        if(ObjectUtil.isNotNull(message)){
            Chat chat = Chat.builder()
                    .senderId(Integer.parseInt(message.getSenderId()))
                    .receiverId(Integer.parseInt(message.getReceiveId()))
                    .content(message.getContent())
                    .messageType(message.getMessageType())
                    .senderName(message.getName())
                    .build();
            chatRecordMapper.insert(chat);
        }
    }

}




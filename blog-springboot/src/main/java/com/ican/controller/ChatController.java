package com.ican.controller;

import com.ican.mapper.ChatMapper;
import com.ican.model.dto.ChatMesDTO;
import com.ican.model.dto.ChatListDTO;
import com.ican.model.vo.ChatRecordVO;
import com.ican.service.ChatService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/friend")
public class ChatController {
    @Autowired
    private ChatService chatService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/send") // 客户端发送消息的目标路径
    public void sendMessage(ChatMesDTO message) {
        chatService.addChat(message);
        String destination = "/queue/messages/" + message.getReceiveId();
        messagingTemplate.convertAndSend(destination,message);
    }


    // 模拟好友列表
    @PostMapping("/friendList")
    public List<ChatListDTO> getFriendList() {
        List<ChatListDTO> friendList = new ArrayList<>();

        ChatListDTO friend1 = new ChatListDTO();
        friend1.setId("8");
        friend1.setName("大毛");
        friend1.setDetail("我是大毛");
        friend1.setLastMsg("to do");
        friend1.setHeadImg("https://s2.loli.net/2024/11/27/jt6igkmJRo3VfGK.png");

        ChatListDTO friend2 = new ChatListDTO();
        friend2.setId("7");
        friend2.setName("小毛");
        friend2.setDetail("我是小毛");
        friend2.setLastMsg("dada dw ertgthy j uy");
        friend2.setHeadImg("https://s2.loli.net/2024/11/27/jt6igkmJRo3VfGK.png");

        ChatListDTO friend3 = new ChatListDTO();
        friend3.setId("9");
        friend3.setName("小王");
        friend3.setDetail("我是小王");
        friend3.setLastMsg("大萨达萨达所大大萨达");
        friend3.setHeadImg("https://s2.loli.net/2024/11/27/jt6igkmJRo3VfGK.png");

        friendList.add(friend1);
        friendList.add(friend2);
        friendList.add(friend3);
        return friendList;
    }

    // 根据好友ID获取聊天记录
    @CrossOrigin(origins = "http://localhost:5173")
    @PostMapping("/chatMsg")
    public List<ChatRecordVO> getChatMessages(@RequestBody ArrayList<String> friendInfo) {
        return chatService.getByCouple(friendInfo.get(0), friendInfo.get(1));
    }

    @GetMapping("/getUserUid")
    @ApiOperation("获取用户Uid")
        public String getUserUid() {
            //好像没用
            return chatService.getUserUid();
        }
}

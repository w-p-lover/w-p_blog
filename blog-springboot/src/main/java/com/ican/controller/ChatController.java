package com.ican.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ican.annotation.OptLogger;
import com.ican.model.dto.ChatMesDTO;
import com.ican.model.vo.FriendshipVO;
import com.ican.model.vo.ChatRecordVO;
import com.ican.service.ChatService;
import com.qcloud.cos.transfer.Upload;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static com.ican.constant.OptTypeConstant.UPLOAD;

@RestController
@RequestMapping("/chat")
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

    @OptLogger(value = UPLOAD)
    @ApiOperation(value = "上传聊天文件")
    @ApiImplicitParam(name = "file", value = "聊天文件", required = true, dataType = "MultipartFile")
    @PostMapping("/chat/upload")
    public String uploadTalkFile(@RequestParam("type") String type,@RequestParam("file") MultipartFile file) {
        return chatService.uploadTalkFile(type,file);
    }


    // 模拟好友列表
    @PostMapping("/friendList/{userId}")
    public List<FriendshipVO> getFriendList(@PathVariable("userId") Integer userId) {
        return chatService.getFriendList(userId);
    }

    // 根据好友ID获取聊天记录
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

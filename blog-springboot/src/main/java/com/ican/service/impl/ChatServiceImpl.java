package com.ican.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ican.entity.BlogFile;
import com.ican.entity.Chat;
import com.ican.entity.User;
import com.ican.enums.FilePathEnum;
import com.ican.exception.ServiceException;
import com.ican.mapper.BlogFileMapper;
import com.ican.mapper.ChatMapper;
import com.ican.mapper.FriendshipMapper;
import com.ican.mapper.UserMapper;
import com.ican.model.dto.ChatMesDTO;
import com.ican.model.vo.ChatRecordVO;
import com.ican.model.vo.FriendshipVO;
import com.ican.service.ChatService;
import com.ican.strategy.context.UploadStrategyContext;
import com.ican.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.ican.constant.CommonConstant.FALSE;

/**
 * @Author：yep
 * @Project：blog-springboot
 * @name：CahtServiceImlp
 * @Date：2024/11/25 16:29
 * @Filename：CahtServiceImlp
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatMapper chatRecordMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FriendshipMapper friendshipsMapper;
    @Autowired
    private UploadStrategyContext uploadStrategyContext;
    @Autowired
    private BlogFileMapper blogFileMapper;

    @Override
    public List<ChatRecordVO> getByCouple(String send, String receive) {
        List<ChatRecordVO> chatMessages = new ArrayList<>();
        int senderId = Integer.parseInt(send);
        int receiverId = Integer.parseInt(receive);
        List<Chat> chatRecords = chatRecordMapper.selectByCouple(senderId, receiverId);
        for (Chat chatRecord : chatRecords) {
            ChatRecordVO chatRecordVO = new ChatRecordVO();
            BeanUtil.copyProperties(chatRecord,chatRecordVO);
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
                    .senderName(message.getSenderName())
                    .build();
            chatRecordMapper.insert(chat);
        }
    }

    @Override
    public List<FriendshipVO> getFriendList(Integer userId) {
        List<Integer> friendIdList = friendshipsMapper.selectFriendshipVOList(userId);
        List<FriendshipVO> friendshipList = new ArrayList<>();
        for (Integer integer : friendIdList) {
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .select(User::getId,User::getAvatar,User::getNickname,User::getIntro)
                    .eq(User::getId,integer));
            FriendshipVO friendshipVO = new FriendshipVO();
            friendshipVO.setId(user.getId().toString());
            friendshipVO.setName(user.getNickname());
            friendshipVO.setDetail(user.getIntro());
            friendshipVO.setHeadImg(user.getAvatar());
            friendshipList.add(friendshipVO);
        }
        return friendshipList;
    }

    @Override
    public String uploadTalkFile(String type, MultipartFile file) {
        String url = "https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/img/202410311645295.png";
        switch (type){
            case "img":
                url = uploadStrategyContext.executeUploadStrategy(file, FilePathEnum.CHAT.getPath());
                try {
                    // 获取文件md5值
                    String md5 = FileUtils.getMd5(file.getInputStream());
                    // 获取文件扩展名
                    String extName = FileUtils.getExtension(file);
                    BlogFile existFile = blogFileMapper.selectOne(new LambdaQueryWrapper<BlogFile>()
                            .select(BlogFile::getId)
                            .eq(BlogFile::getFileName, md5)
                            .eq(BlogFile::getFilePath, FilePathEnum.CHAT.getFilePath()));
                    if (Objects.isNull(existFile)) {
                        // 保存文件信息
                        BlogFile newFile = BlogFile.builder()
                                .fileUrl(url)
                                .fileName(md5)
                                .filePath(FilePathEnum.CHAT.getFilePath())
                                .extendName(extName)
                                .fileSize((int) file.getSize())
                                .isDir(FALSE)
                                .build();
                        blogFileMapper.insert(newFile);
                    }
                    break;
                } catch (IOException e) {
                    log.error("文件上传持久化错误:" + e.getMessage());
                }
            case "doc":
                break;
        }
        return url;
    }
}




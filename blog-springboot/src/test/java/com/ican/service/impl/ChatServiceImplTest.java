package com.ican.service.impl;

import com.ican.entity.Chat;
import com.ican.entity.User;
import com.ican.mapper.BlogFileMapper;
import com.ican.mapper.ChatMapper;
import com.ican.mapper.FriendshipMapper;
import com.ican.mapper.UserMapper;
import com.ican.model.vo.ChatMessagePageVO;
import com.ican.model.vo.FriendshipVO;
import com.ican.strategy.context.UploadStrategyContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatMapper chatMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private FriendshipMapper friendshipMapper;
    @Mock
    private UploadStrategyContext uploadStrategyContext;
    @Mock
    private BlogFileMapper blogFileMapper;

    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl(
                chatMapper,
                userMapper,
                friendshipMapper,
                uploadStrategyContext,
                blogFileMapper
        );
    }

    @Test
    void getChatRecordPageByCoupleReturnsChronologicalPageAndHasMore() {
        when(chatMapper.selectPageByCouple(1, 2, 0, 2)).thenReturn(List.of(
                chat(4, "newest", "2026-04-30T12:04:00"),
                chat(3, "older", "2026-04-30T12:03:00")
        ));
        when(chatMapper.countByCouple(1, 2)).thenReturn(4L);

        ChatMessagePageVO page = chatService.getChatRecordPageByCouple("1", "2", 1, 2);

        assertEquals(4L, page.getTotal());
        assertTrue(page.getHasMore());
        assertEquals(List.of("older", "newest"), page.getRecords().stream().map(item -> item.getContent()).toList());
    }

    @Test
    void getFriendListAttachesUnreadCountFromChatRecords() {
        User friend = User.builder()
                .id(2)
                .nickname("星野")
                .avatar("/avatar.png")
                .intro("摄影")
                .build();
        when(friendshipMapper.selectFriendshipVOList(1)).thenReturn(List.of(2));
        when(userMapper.selectById(2)).thenReturn(friend);
        when(chatMapper.countUnreadByCouple(2, 1)).thenReturn(3L);

        List<FriendshipVO> friendList = chatService.getFriendList(1);

        assertEquals(1, friendList.size());
        assertEquals("2", friendList.get(0).getId());
        assertEquals(3, friendList.get(0).getUnreadCount());
    }

    @Test
    void markChatRecordReadMarksMessagesFromFriendToCurrentUser() {
        chatService.markChatRecordRead("1", "2");

        verify(chatMapper).markReadByCouple(2, 1);
    }

    private Chat chat(Integer id, String content, String createTime) {
        return Chat.builder()
                .id(id)
                .senderId(1)
                .receiverId(2)
                .senderName("sender")
                .content(content)
                .messageType("text")
                .createTime(LocalDateTime.parse(createTime))
                .build();
    }
}

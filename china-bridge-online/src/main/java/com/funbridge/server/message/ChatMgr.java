package com.funbridge.server.message;

import com.funbridge.server.message.data.Chatroom;
import com.funbridge.server.message.data.GenericChatroom;
import com.funbridge.server.message.data.GenericMessage;
import com.funbridge.server.message.data.Message;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by bplays on 04/11/16.
 */
@Component(value="chatMgr")
@Scope(value="singleton")
public class ChatMgr extends GenericChatMgr {

    @Resource(name="mongoChatTemplate")
    private MongoTemplate mongoTemplate;

    @PostConstruct
    @Override
    public void init() {
        try {
            pattern = Pattern.compile("\\{(.+?)\\}");
        } catch (PatternSyntaxException e) {
            log.error("Pattern not valid !",e);
        }
    }

    @Override
    public void startUp() {

    }

    @PreDestroy
    @Override
    public void destroy() {

    }

    @Override
    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }

    @Override
    public Class<? extends GenericChatroom> getChatroomEntity() {
        return Chatroom.class;
    }

    @Override
    public Class<? extends GenericMessage> getMessageEntity() {
        return Message.class;
    }

    @Override
    public String getChatroomCollectionName() {
        return "chatroom";
    }

    @Override
    public String getMessageCollectionName() {
        return "message";
    }
}

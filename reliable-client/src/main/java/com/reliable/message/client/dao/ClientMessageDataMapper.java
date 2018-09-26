package com.reliable.message.client.dao;


import com.reliable.message.model.domain.ClientMessageData;

public interface ClientMessageDataMapper {

    int insert(ClientMessageData mqMessageData);

    ClientMessageData getClientMessageByMessageId(Long messageId);
}
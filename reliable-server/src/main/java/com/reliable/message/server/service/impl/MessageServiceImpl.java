package com.reliable.message.server.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.reliable.message.model.domain.ClientMessageData;
import com.reliable.message.model.util.TimeUtil;
import com.reliable.message.server.dao.ServerMessageMapper;
import com.reliable.message.model.domain.ServerMessageData;
import com.reliable.message.server.domain.MessageConfirm;
import com.reliable.message.server.enums.MessageSendStatusEnum;
import com.reliable.message.server.service.MessageConfirmService;
import com.reliable.message.server.service.MessageConsumerService;
import com.reliable.message.server.service.MessageService;
import com.reliable.message.server.util.UniqueId;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by 李雷 on 2018/5/11.
 */
@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private ServerMessageMapper serverMessageMapper;

    @Autowired
    private MessageConsumerService messageConsumerService;

    @Autowired
    private MessageConfirmService messageConfirmService;

    @Autowired
    private UniqueId uniqueId;

    @Autowired
    private KafkaTemplate kafkaTemplate;

    private static final Integer FIRST_SEND_TIME_COUNT =1;
    private static final Integer DEFAULT_DEAD_STATUS =0;

    @Override
    public void saveMessageWaitingConfirm(ClientMessageData clientMessageData) {

        if (StringUtils.isEmpty(clientMessageData.getMessageTopic())) {
//            throw new TpcBizException(ErrorCodeEnum.TPC10050001);
        }

        Date now = new Date();
        ServerMessageData message = new ModelMapper().map(clientMessageData, ServerMessageData.class);
        message.setStatus(MessageSendStatusEnum.WAIT_CONFIRM.sendStatus());
        message.setProducerMessageId(clientMessageData.getId());
        message.setId(uniqueId.getNextIdByApplicationName(ServerMessageData.class.getSimpleName()));
        message.setUpdateTime(now);
        message.setCreateTime(now);
        serverMessageMapper.insert(message);
    }

    @Override
    public void confirmAndSendMessage(Long clientMessageId) {
        final ServerMessageData message = serverMessageMapper.getByProducerMessageId(clientMessageId);
        if (message == null) {
//            throw new TpcBizException(ErrorCodeEnum.TPC10050002);
        }

        //save record
        List<MessageConfirm> confirmList = confirmAndSendMessage(message);

        if(confirmList.size() == 0) return;

        //send message to mq
        sendMessageToMessageQueue(confirmList,message);

    }

    @Override
    public void directSendMessage(ServerMessageData messageData, String topic, String key) {
        if(StringUtils.isBlank(key)){
            kafkaTemplate.send(topic, JSONObject.toJSONString(messageData));
        }else {
            kafkaTemplate.send(topic,key,JSONObject.toJSONString(messageData));
        }
    }

    @Override
    public void confirmReceiveMessage(String consumerGroup, String producerMessageId) {
        Long confirmId = serverMessageMapper.getConfirmIdByGroupAndKey(consumerGroup, producerMessageId);
        // 3. 更新消费信息的状态
        serverMessageMapper.confirmReceiveMessage(confirmId);
    }

    @Override
    public void confirmFinishMessage(String consumerGroup, String producerMessageId) {
        messageConfirmService.confirmFinishMessage(consumerGroup,producerMessageId);
    }

    @Override
    public ServerMessageData getServerMessageDataByProducerMessageId(Long producerMessageId) {
        return serverMessageMapper.getByProducerMessageId(producerMessageId);
    }

    @Override
    public List<ServerMessageData> getServerMessageDataByParams(JSONObject jsonObject) {
        jsonObject.put("status", MessageSendStatusEnum.SENDING.sendStatus());
        jsonObject.put("clearTime", TimeUtil.getBeforeByHourTime(1));
        return serverMessageMapper.getServerMessageDataByParams(jsonObject);
    }

    @Override
    public void deleteServerMessageDataById(Long id) {
        serverMessageMapper.deleteServerMessageDataById(id);
    }

    @Override
    public List<ServerMessageData> getWaitConfirmServerMessageData(JSONObject jobTaskParameter) {
        return serverMessageMapper.getWaitConfirmServerMessageData(jobTaskParameter);
    }

    @Override
    public List<ServerMessageData> getSendingMessageData(JSONObject jobTaskParameter) {
        return serverMessageMapper.getSendingMessageData(jobTaskParameter);
    }

    @Transactional
    private List<MessageConfirm>  confirmAndSendMessage(ServerMessageData message){
        ServerMessageData update = new ServerMessageData();
        update.setStatus(MessageSendStatusEnum.SENDING.sendStatus());
        update.setId(message.getId());
        update.setUpdateTime(new Date());
        serverMessageMapper.updateById(update);

        // 创建消费待确认列表
        List<MessageConfirm> confirmList =  this.createMqConfirmListByTopic(message.getMessageTopic(), message.getId(),message.getProducerGroup(), message.getProducerMessageId());
        return confirmList;
    }

    private List<MessageConfirm> createMqConfirmListByTopic(String messageTopic, Long messageId,String producerGroup, Long producerMessageId) {
        List<MessageConfirm> list = new ArrayList<>();
        MessageConfirm messageConfirm;
        List<String> consumerGroupList = messageConsumerService.listConsumerGroupByTopic(messageTopic);

        if (consumerGroupList ==null || consumerGroupList.size() == 0) {
            return new ArrayList<>();
        }

        for (final String consumerCode : consumerGroupList) {
            messageConfirm = new MessageConfirm(uniqueId.getNextIdByApplicationName(MessageConfirm.class.getSimpleName()), messageId,producerGroup, producerMessageId,
                    consumerCode,FIRST_SEND_TIME_COUNT,DEFAULT_DEAD_STATUS);
            Date currentTime = new Date();
            messageConfirm.setCreateTime(currentTime);
            messageConfirm.setUpdateTime(currentTime);
            list.add(messageConfirm);
        }

        messageConfirmService.batchCreateMqConfirm(list);
        return list;
    }

    public void sendMessageToMessageQueue(List<MessageConfirm> confirmList, final ServerMessageData message ){

        for (MessageConfirm confirm: confirmList) {
            this.directSendMessage(message, message.getMessageTopic()+"_"+confirm.getConsumerGroup().toUpperCase(), message.getMessageKey());
        }
    }

    @Override
    public void directSendMessage(ClientMessageData clientMessageData) {
        ServerMessageData message = new ModelMapper().map(clientMessageData, ServerMessageData.class);
        List<String> consumerGroupList = messageConsumerService.listConsumerGroupByTopic(message.getMessageTopic());
        for (String consumer : consumerGroupList) {
            this.directSendMessage(message,message.getMessageTopic()+"-"+consumer,message.getMessageKey());
        }
    }

    @Override
    @Transactional
    public void saveAndSendMessage(ClientMessageData clientMessageData) {
        ServerMessageData message = new ModelMapper().map(clientMessageData, ServerMessageData.class);
        message.setStatus(MessageSendStatusEnum.SENDING.sendStatus());
        Date now = new Date();
        message.setProducerMessageId(clientMessageData.getId());
        message.setId(uniqueId.getNextIdByApplicationName(ServerMessageData.class.getSimpleName()));
        message.setUpdateTime(now);
        message.setCreateTime(now);
        serverMessageMapper.insert(message);
        List<MessageConfirm> confirmList = createMqConfirmListByTopic(message.getMessageTopic(),message.getId(),message.getProducerGroup(),message.getProducerMessageId());
        sendMessageToMessageQueue(confirmList,message);
    }
}

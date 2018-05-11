package com.reliable.message.server.service.impl;

import com.reliable.message.model.dto.TpcMqMessageDto;
import com.reliable.message.server.dao.MqMessageMapper;
import com.reliable.message.server.domain.TpcMqConfirm;
import com.reliable.message.server.domain.TpcMqMessage;
import com.reliable.message.server.enums.MqSendStatusEnum;
import com.reliable.message.server.service.MqConfirmService;
import com.reliable.message.server.service.MqConsumerService;
import com.reliable.message.server.service.MqMessageService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by 李雷 on 2018/5/11.
 */
@Service
public class MqMessageServiceImpl implements MqMessageService {

    @Autowired
    private MqMessageMapper mqMessageMapper;


    @Autowired
    private MqConsumerService mqConsumerService;

    @Autowired
    private MqConfirmService mqConfirmService;

    @Override
    public void saveMessageWaitingConfirm(TpcMqMessageDto tpcMqMessageDto) {

        if (StringUtils.isEmpty(tpcMqMessageDto.getMessageTopic())) {
//            throw new TpcBizException(ErrorCodeEnum.TPC10050001);
        }

        Date now = new Date();
        TpcMqMessage message = new ModelMapper().map(tpcMqMessageDto, TpcMqMessage.class);
        message.setMessageStatus(MqSendStatusEnum.WAIT_SEND.sendStatus());
        message.setUpdateTime(now);
        message.setCreatedTime(now);
        mqMessageMapper.insert(message);
    }

    @Override
    public void confirmAndSendMessage(String messageKey) {
        final TpcMqMessage message = mqMessageMapper.getByMessageKey(messageKey);
        if (message == null) {
//            throw new TpcBizException(ErrorCodeEnum.TPC10050002);
        }

        TpcMqMessage update = new TpcMqMessage();
        update.setMessageStatus(MqSendStatusEnum.SENDING.sendStatus());
        update.setId(message.getId());
        update.setUpdateTime(new Date());
        mqMessageMapper.updateByMessageKey(update);


        // 创建消费待确认列表
        this.createMqConfirmListByTopic(message.getMessageTopic(), message.getId(), message.getMessageKey());
        // 直接发送消息
//        this.directSendMessage(message.getMessageBody(), message.getMessageTopic(), message.getMessageTag(), message.getMessageKey(), message.getProducerGroup(), message.getDelayLevel());
    }

    private void createMqConfirmListByTopic(String messageTopic, Long messageId, String messageKey) {
        List<TpcMqConfirm> list = new ArrayList<TpcMqConfirm>();
        TpcMqConfirm tpcMqConfirm;
        List<String> consumerGroupList = mqConsumerService.listConsumerGroupByTopic(messageTopic);
        if (consumerGroupList ==null || consumerGroupList.size() == 0) {
//            throw new TpcBizException(ErrorCodeEnum.TPC100500010, topic);
        }
        for (final String consumerCode : consumerGroupList) {
            tpcMqConfirm = new TpcMqConfirm(UniqueIdGenerator.generateId(), messageId, messageKey, consumerCode);
            list.add(tpcMqConfirm);
        }

        mqConfirmService.batchCreateMqConfirm(list);
    }
}

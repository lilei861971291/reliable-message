package com.reliable.message.client.service.impl;

import com.reliable.message.client.dao.ClientMessageDataMapper;
import com.reliable.message.client.feign.MqMessageFeign;
import com.reliable.message.client.service.MqMessageService;
import com.reliable.message.model.domain.ClientMessageData;
import com.reliable.message.model.dto.TpcMqMessageDto;
import com.reliable.message.model.enums.ExceptionCodeEnum;
import com.reliable.message.model.enums.MqMessageTypeEnum;
import com.reliable.message.model.exception.BusinessException;
import com.reliable.message.model.wrapper.Wrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Slf4j
@Service
public class MqMessageServiceImpl implements MqMessageService {
	@Autowired
	private ClientMessageDataMapper mqMessageDataMapper;
	@Autowired
	private MqMessageFeign mqMessageFeign;

	@Override
	public void saveWaitConfirmMessage(final ClientMessageData mqMessageData) {
		//当前应用的本地消息存储
		this.saveMqProducerMessage(mqMessageData);
		//可靠消息服务远程接口
		mqMessageFeign.saveMessageWaitingConfirm(mqMessageData);
		log.info("<== saveWaitConfirmMessage - 存储预发送消息成功. messageKey={}", mqMessageData.getMessageKey());
	}

	@Override
	public void saveMqProducerMessage(ClientMessageData mqMessageData) {
		// 校验消息数据
		this.checkMessage(mqMessageData);
		// 先保存消息
		mqMessageData.setMessageType(MqMessageTypeEnum.PRODUCER_MESSAGE.messageType());
		mqMessageDataMapper.insert(mqMessageData);
	}

	@Async
	@Override
	public void confirmAndSendMessage(String messageId) {
		Wrapper wrapper = mqMessageFeign.confirmAndSendMessage(messageId);
		if (wrapper == null) {
//			throw new TpcBizException(ErrorCodeEnum.GL99990002);
		}
		if (wrapper.error()) {
//			throw new TpcBizException(ErrorCodeEnum.TPC10050004, wrapper.getMessage(), messageKey);
		}
		log.info("<== saveMqProducerMessage - 存储并发送消息给消息中心成功. messageId={}", messageId);
	}

	@Override
	public void confirmReceiveMessage(String consumerGroup, ClientMessageData messageData) {
		final Long messageId = messageData.getId();
		log.info("confirmReceiveMessage - 消费者={}, 确认收到messageId={}的消息", consumerGroup, messageId);
		// 先保存消息
		messageData.setMessageType(MqMessageTypeEnum.CONSUMER_MESSAGE.messageType());
		mqMessageDataMapper.insert(messageData);

//		Wrapper wrapper = mqMessageFeign.confirmReceiveMessage(consumerGroup, messageData.getProducerMessageId());
//		log.info("tpcMqMessageFeignApi.confirmReceiveMessage result={}", wrapper);
//		if (wrapper == null) {
//			throw new BusinessException(ExceptionCodeEnum.MSG_CONSUMER_ARGS_CONVERT_EXCEPTION);
//		}
//		if (wrapper.error()) {
//			throw new BusinessException(ExceptionCodeEnum.MSG_CONSUMER_ARGS_CONVERT_EXCEPTION);
//		}
	}

	@Override
	public void confirmFinishMessage(String consumerGroup, String messageKey) {
		Wrapper wrapper = mqMessageFeign.confirmFinishMessage(consumerGroup, messageKey);
		log.info("tpcMqMessageFeignApi.confirmReceiveMessage result={}", wrapper);
		if (wrapper == null) {
//			throw new TpcBizException(ErrorCodeEnum.GL99990002);
		}
		if (wrapper.error()) {
//			throw new TpcBizException(ErrorCodeEnum.TPC10050004, wrapper.getMessage(), messageKey);
		}
	}

	@Override
	public boolean checkMessageStatus(Long messageId) {
		ClientMessageData clientMessageData = mqMessageDataMapper.getClientMessageByMessageId(messageId);
		if(clientMessageData !=null ) return true;
		return false;
	}


	private void checkMessage(ClientMessageData mqMessageData) {
		if (null == mqMessageData) {
//			throw new TpcBizException(ErrorCodeEnum.TPC10050007);
		}
		String messageTopic = mqMessageData.getMessageTopic();
		String messageBody = mqMessageData.getMessageBody();
		String messageKey = mqMessageData.getMessageKey();
		String producerGroup = mqMessageData.getProducerGroup();
		if (StringUtils.isEmpty(messageKey)) {
//			throw new TpcBizException(ErrorCodeEnum.TPC10050009);
		}
		if (StringUtils.isEmpty(messageTopic)) {
//			throw new TpcBizException(ErrorCodeEnum.TPC10050001);
		}
		if (StringUtils.isEmpty(messageBody)) {
//			throw new TpcBizException(ErrorCodeEnum.TPC10050008, mqMessageData.getMessageKey());
		}

		if (StringUtils.isEmpty(producerGroup)) {
//			throw new TpcBizException(ErrorCodeEnum.TPC100500015, mqMessageData.getMessageKey());
		}
	}


}

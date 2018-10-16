package com.reliable.message.client.aspect;

import com.reliable.message.client.annotation.MessageProducerStore;
import com.reliable.message.client.service.ReliableMessageService;
import com.reliable.message.model.domain.ClientMessageData;
import com.reliable.message.model.enums.DelayLevelEnum;
import com.reliable.message.model.enums.ExceptionCodeEnum;
import com.reliable.message.model.enums.MessageSendTypeEnum;
import com.reliable.message.model.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.lang.reflect.Method;


/**
 * 生产者切面
 */
@Slf4j
@Aspect
public class MessageProducerStoreAspect {
	@Resource
	private ReliableMessageService reliableMessageService;
	@Value("${spring.application.name}")
	private String appName;



	@Value("${reliable.message.producerGroup:}")
	private String producerGroup;

	@Pointcut("@annotation(com.reliable.message.client.annotation.MessageProducerStore)")
	public void mqProducerStoreAnnotationPointcut() {

	}

	@Around(value = "mqProducerStoreAnnotationPointcut()")
	public Object processMqProducerStoreJoinPoint(ProceedingJoinPoint joinPoint) throws Throwable {
		log.info("processMqProducerStoreJoinPoint - 线程id={}", Thread.currentThread().getId());
		if(StringUtils.isBlank(producerGroup)) producerGroup = appName;
		Object result;
		Object[] args = joinPoint.getArgs();
		MessageProducerStore annotation = getAnnotation(joinPoint);
		MessageSendTypeEnum type = annotation.sendType();
		DelayLevelEnum delayLevelEnum = annotation.delayLevel();
		if (args.length == 0) {
			throw new BusinessException(ExceptionCodeEnum.MSG_PRODUCER_ARGS_IS_NULL);
		}
		ClientMessageData domain = null;
		for (Object object : args) {
			if (object instanceof ClientMessageData) {
				domain = (ClientMessageData) object;
				break;
			}
		}

		if (domain == null) {
			throw new BusinessException(ExceptionCodeEnum.MSG_PRODUCER_ARGS_TYPE_IS_WRONG);
		}

		if(domain.getId() == null){
			throw new BusinessException(ExceptionCodeEnum.MSG_PRODUCER_ENTITY_ID_IS_EMPTY);
		}

		domain.setProducerGroup(producerGroup);

		if (type == MessageSendTypeEnum.WAIT_CONFIRM) {
			if (delayLevelEnum != DelayLevelEnum.ZERO) {
				domain.setDelayLevel(delayLevelEnum.delayLevel());
			}
			reliableMessageService.saveWaitConfirmMessage(domain);
		}
		result = joinPoint.proceed();
		if (type == MessageSendTypeEnum.SAVE_AND_SEND) {
			reliableMessageService.saveAndSendMessage(domain);
		} else if (type == MessageSendTypeEnum.DIRECT_SEND) {
			reliableMessageService.directSendMessage(domain);
		} else {
			reliableMessageService.confirmAndSendMessage(domain.getProducerGroup()+"-"+domain.getId());
		}
		return result;
	}

	private static MessageProducerStore getAnnotation(JoinPoint joinPoint) {
		MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
		Method method = methodSignature.getMethod();
		return method.getAnnotation(MessageProducerStore.class);
	}
}

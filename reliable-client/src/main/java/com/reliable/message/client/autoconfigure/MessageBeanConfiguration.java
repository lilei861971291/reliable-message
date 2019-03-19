package com.reliable.message.client.autoconfigure;

import com.reliable.message.client.aspect.MessageConsumerStoreAspect;
import com.reliable.message.client.aspect.MessageProducerStoreAspect;
import com.reliable.message.client.job.ClientMessageDataflow;
import com.reliable.message.client.service.ReliableMessageService;
import com.reliable.message.client.service.impl.ReliableMessageServiceImpl;
import com.reliable.message.client.util.MessageClientUniqueId;
import com.reliable.message.client.web.ReliableController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class MessageBeanConfiguration {

	@Bean
	@ConditionalOnExpression("${reliable.message.reliableMessageProducer:false}")
	public MessageProducerStoreAspect messageProducerStoreAspect() {
		return new MessageProducerStoreAspect();
	}


	@Bean
	@ConditionalOnExpression("${reliable.message.reliableMessageConsumer:false}")
	public MessageConsumerStoreAspect messageConsumerStoreAspect() {
		return new MessageConsumerStoreAspect();
	}

	@Bean
	@ConditionalOnExpression("${reliable.message.reliableMessageConsumer:false} || ${reliable.message.reliableMessageProducer:false}")
	public ReliableController reliableController(){
		return new ReliableController();
	}

	@Bean
	@ConditionalOnExpression("${reliable.message.reliableMessageConsumer:false} || ${reliable.message.reliableMessageProducer:false}")
	public ReliableMessageService reliableMessageService(){
		return new ReliableMessageServiceImpl();
	}


	@Bean
	@ConditionalOnExpression("${reliable.message.producerMessageDelteFlag:false}")
	public ClientMessageDataflow clientMessageDataflow(){
		return new ClientMessageDataflow();
	}

	@Bean
	@ConditionalOnExpression("${reliable.message.reliableMessageConsumer:false} || ${reliable.message.reliableMessageProducer:false}")
	public MessageClientUniqueId messageClientUniqueId(){
		return new MessageClientUniqueId();
	}

}

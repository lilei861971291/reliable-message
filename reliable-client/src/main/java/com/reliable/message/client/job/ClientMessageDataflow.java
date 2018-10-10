package com.reliable.message.client.job;

import com.alibaba.fastjson.JSONObject;
import com.job.lite.annotation.ElasticJobConfig;
import com.job.lite.job.AbstractBaseDataflowJob;
import com.reliable.message.client.feign.MqMessageFeign;
import com.reliable.message.client.service.MqMessageService;
import com.reliable.message.model.domain.ClientMessageData;
import com.reliable.message.model.wrapper.Wrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
@ElasticJobConfig(cron = "0/50 * * * * ? ", jobParameter = "{'fetchNum':200,'taskType':'SENDING_MESSAGE'}",description="生产者消息清理")
public class ClientMessageDataflow extends AbstractBaseDataflowJob<ClientMessageData> {

    private Logger logger = LoggerFactory.getLogger(ClientMessageDataflow.class);

    @Autowired
    private MqMessageService mqMessageService;

    @Autowired
    private MqMessageFeign mqMessageFeign;


    @Override
    protected List<ClientMessageData> fetchJobData(final JSONObject jobTaskParameter) {
        logger.info("fetchJobData - jobTaskParameter={}", jobTaskParameter);
        List<ClientMessageData> clientMessageData = mqMessageService.getProducerMessage(jobTaskParameter);
        return clientMessageData;
    }

    @Override
    protected void processJobData(final List<ClientMessageData> clientMessageDatas) {
        logger.info("processJobData - clientMessageDatas={}", clientMessageDatas);

        //查询服务端消息状态，该消息是否已经消费成功（从消息服务段查询不到该消息）
        for (ClientMessageData clientMessageData : clientMessageDatas) {
            String producerMessageId = clientMessageData.getProducerMessageId();
            Wrapper wrapper = mqMessageFeign.checkServerMessageIsExist(producerMessageId);
            boolean deleteFlag = (boolean) wrapper.getResult();
            if(deleteFlag){
                //主动清清除
                mqMessageService.deleteMessageByProducerMessageId(producerMessageId);
            }
        }
    }
}
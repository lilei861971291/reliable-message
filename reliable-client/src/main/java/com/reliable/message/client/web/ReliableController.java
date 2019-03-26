package com.reliable.message.client.web;

import com.reliable.message.client.service.ReliableMessageService;
import com.reliable.message.common.domain.ClientMessageData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by 李雷 on 2018/10/8.
 */
@RestController
public class ReliableController {


    @Autowired
    private ReliableMessageService reliableMessageService;


    @RequestMapping("/deleteMessage/{producerMessageId}")
    public void deleteMessageByProducerMessageId(@PathVariable("producerMessageId") String producerMessageId){

        reliableMessageService.deleteMessageByProducerMessageId(producerMessageId);

    }


    @RequestMapping("/getClientMessage/{producerMessageId}")
    public ClientMessageData getClientMessageByProducerMessageId(@PathVariable("producerMessageId") String producerMessageId){

        return reliableMessageService.getClientMessageDataByProducerMessageId(producerMessageId);

    }

}

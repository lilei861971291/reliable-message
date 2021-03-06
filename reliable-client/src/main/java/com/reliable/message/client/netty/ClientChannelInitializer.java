package com.reliable.message.client.netty;

import com.reliable.message.common.netty.MessageCodecHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {


    private ClientRpcHandler clientRpcHandler;

    public ClientChannelInitializer(ClientRpcHandler clientRpcHandler){
        this.clientRpcHandler = clientRpcHandler;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new IdleStateHandler(0, 4, 0, TimeUnit.SECONDS));
        pipeline.addLast("messageCodec", new MessageCodecHandler());
        pipeline.addLast("nettyClientHandler", clientRpcHandler);

    }
}
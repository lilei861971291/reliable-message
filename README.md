# reliable-parent
可靠消息服务


bug
ok - saveAndSendMessage 客户端不能保证保存后的消息一定被消费 客户端定时执行任务，查看服务端时候存有message 且已经消费，如果不存在，重发消息，如果存在标记状态



# TODO
1:  client 端 DB 消息清除处理逻辑

2:  saveAndSendMessage 消息处理 ok

3:  directSendMessage 消息处理 ok 

4:  consumer 端消息中间件jar的依赖

5:  对多消息中间件的支持 RocketMQ kafka ActiveMQ RabbitMQ

6:  server 端对 mybatis 的依赖

7:  serverConsole

8:  最大努力通知方式的加入

9:  服务注册/发现多方式接入

10: 服务端客户端，消息id 方式统一 ok 

11：futures 中，超时message 清理

12：tcp客户端池化 --GenericKeyedObjectPool 管理 空闲 channel 的释放操作

13: 单节点 服务端重启，客户端发送消息报错

14: 客户端断线重连 ok

15: 服务端重启，收不到消息，定时任务也不执行


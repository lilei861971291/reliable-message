package com.reliable.message.server.domain;

import lombok.Data;

import java.util.Date;


@Data
public class MessageConfirm {
	/**
	 * ID
	 */
	private Long id;

	/**
	 * 版本号
	 */
	private Integer version;

	/**
	 * 任务ID
	 */
	private Long messageId;

	/**
	 * 消费者组编码
	 */
	private String consumerGroup;

	/**
	 * 消费的数次
	 */
	private Integer consumeCount;

	private String producerMessageId;

	/**
	 * 状态, 10 - 未确认 ; 20 - 已确认; 30 已消费
	 */
	private Integer status;


	private Integer sendTimes;

	/**
	 * 是否死亡 0 - 活着; 1-死亡
	 */
	private Integer dead;

	/**
	 * 创建时间
	 */
	private Date createTime;

	/**
	 * 更新时间
	 */
	private Date updateTime;

	/**
	 * Instantiates a new Tpc mq confirm.
	 *
	 * @param id           the id
	 * @param messageId    the server message id
	 * @param producerMessageId   the producerMessageId
	 * @param consumerGroup the consumer code
	 */
	public MessageConfirm(final Long id, final Long messageId,
						  final String producerMessageId,
						  final String consumerGroup,
						  final Integer sendTimes,
						  final Integer dead) {
		this.id = id;
		this.messageId = messageId;
		this.producerMessageId = producerMessageId;
		this.consumerGroup = consumerGroup;
		this.sendTimes=sendTimes;
		this.dead = dead;
	}
}
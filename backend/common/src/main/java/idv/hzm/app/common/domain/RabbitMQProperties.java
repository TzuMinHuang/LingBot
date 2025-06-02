package idv.hzm.app.common.domain;

public class RabbitMQProperties {

	/*
	 * 使用者訊息進入初始隊列： queue.incoming → bot 消費 → 意圖分析後分流
	 */
	public static final String INCOMING_USER_QUEUE_NAME = "queue.incoming.user";
	public static final String INCOMING_USER_EXCHANGE_NAME = "exchange.incoming.user";
	public static final String INCOMING_USER_ROUTING_KEY_NAME = "incoming.user";

	/*
	 * Bot 回覆： 可處理：queue.reply.user.{userId}
	 */
	public static final String REPLY_USER_QUEUE_NAME = "queue.reply.user.%s";
	public static final String REPLY_USER_EXCHANGE_NAME = "exchange.reply.user";
	public static final String REPLY_USER_ROUTING_KEY_NAME = "reply.user.%s";

	/*
	 * Bot 轉接客服： 無法處理 → queue.transfer_request（排隊中）agent
	 */
	public static final String TRANSFER_QEQUEST_QUEUE_NAME = "queue.transfer_request";
	public static final String TRANSFER_QEQUEST_EXCHANGE_NAME = "exchange.transfer_request";
	public static final String TRANSFER_QEQUEST_RROUTING_KEY_NAME = "transfer_request";

	/*
	 * 客服接線流程： 客服點擊接線 → queue.customer_service.{roomId}
	 * 雙向對話：queue.customer_service.{roomId} <→ queue.reply.user.{userId}
	 */
	public static final String CUSTOMER_SERVICE_QUEUE_NAME = "queue.customer.service.%s";
	public static final String CUSTOMER_SERVICE_EXCHANGE_NAME = "exchange.customer.service";
	public static final String CUSTOMER_SERVICE_RROUTING_KEY_NAME = "customer.service.%s";

	/*
	 * Agent 接收消息的 Queue ： ：queue.receive.agent.{agentId}
	 */
	public static final String RECEIVE_AGENT_QUEUE_NAME = "queue.receive.agent.%s";
	public static final String RECEIVE_AGENT_EXCHANGE_NAME = "exchange.receive.agent";
	public static final String RECEIVE_AGENT_ROUTING_KEY_NAME = "receive.agent.%s";

}

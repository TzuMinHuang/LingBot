package idv.hzm.app.bot.listener;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import idv.hzm.app.bot.entity.IntentInfoDTO;
import idv.hzm.app.bot.entity.Issue;
import idv.hzm.app.bot.entity.Topic;
import idv.hzm.app.bot.flow.core.ProcessContext;
import idv.hzm.app.bot.flow.core.ProcessEngine;
import idv.hzm.app.bot.rasa.dto.RasaNluResponse;
import idv.hzm.app.bot.rasa.service.RasaBotService;
import idv.hzm.app.bot.repo.IntentRepository;
import idv.hzm.app.bot.repo.IssueRepository;
import idv.hzm.app.bot.repo.TopicRepository;
import idv.hzm.app.common.domain.RabbitMQProperties;
import idv.hzm.app.common.dto.BasePayload;
import idv.hzm.app.common.dto.EventDto;
import idv.hzm.app.common.dto.EventFactory;
import idv.hzm.app.common.dto.MessagePayload;

@Component
public class BotConsumer {

	@Autowired
	private IssueRepository issueRepository;
	@Autowired
	private IntentRepository intentRepository;
	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private ProcessEngine processEngine;
	@Autowired
	private RasaBotService rasaBotService;

	@RabbitListener(queues = RabbitMQProperties.INCOMING_USER_QUEUE_NAME)
	public void receive(String message) {
		try {
			EventDto eventDto = EventFactory.createEvent(message);
			BasePayload payload = eventDto.getPayload();
			if (payload instanceof MessagePayload) {
				MessagePayload msg = (MessagePayload) payload;
				final String sessionId = eventDto.getSessionId();
				final String content = msg.getContent();
				final String intent = this.getIntent(content);

				// sessionId,content,intent -> topicName -> response_text
				ProcessContext processContext = this.createProcessContext(sessionId, content, intent);
				System.out.println(processContext);
				if (processContext.getTopicName() != null) {
					this.processEngine.execute(processContext);
				}

			}
		} catch (Exception e) {

			System.out.println(e.getMessage());
		}

	}

	private String getIntent(String content) {
		// 請求 RasaNlu 服務取得使用者對話意圖
		RasaNluResponse rasaNluResponse = this.rasaBotService.parseMessage(content).block();
		System.out.println(rasaNluResponse);
		String intentName = rasaNluResponse.getIntent().getName();
		// 當 RasaNlu 無法辨識
		if (intentName == null || "".equals(intentName) || "nlu_fallback".equals(intentName)) {
			intentName = "fallback";
		}
		return intentName;
	}

	/*
	 * 
	 * 哪一是明確的意圖？ 1.RasaBot 返回的 Intent 2.後台提出問題希望收到的 Intent
	 * 
	 * 如何正確的處理？ 1.沒有後台提出問題希望收到的 Intent， RasaBot 返回的 Intent 為主 2.當有後台提出問題希望收到的 Intent
	 * 只處理特定 RasaBot 返回的 Intent，如取消、暫停，提供中斷/恢復機制，其他會忽略
	 */
	private ProcessContext createProcessContext(String sessionId, String content, String intent) {
		// 查看目前對話狀態是否有 issue 需處理
		Optional<Issue> optionalIssue = this.issueRepository.findBySessionIdAndStatus(sessionId, "active");
		if (optionalIssue.isPresent()) {
			Issue issue = optionalIssue.get();
			if (this.isIntentInTopic(intent, issue.getTopic())) {
				// 採用原先的 issue 的 topic
				ProcessContext processContext = new ProcessContext();
				processContext.setSessionId(sessionId);
				processContext.setContent(content);
				processContext.setIntent(intent);
				processContext.setTopicName(issue.getTopic().getName());
				return processContext;
			} else {
				// 將目前的 issue 暫停
				issue.setStatus("paused");
				this.issueRepository.save(issue);
				// 根據 Intent 創建 新 issue
				return this.createBaseProcessContext(sessionId, content, intent);
			}
		} else {
			// 根據 Intent 創建 新 issue
			return this.createBaseProcessContext(sessionId, content, intent);
		}

	}

	private ProcessContext createBaseProcessContext(String sessionId, String content, String intent) {

		ProcessContext processContext = new ProcessContext();
		processContext.setSessionId(sessionId);
		processContext.setContent(content);
		processContext.setIntent(intent);

		List<IntentInfoDTO> result = this.intentRepository.findIntentDetailByName(intent);
		if (result.size() == 1) {
			IntentInfoDTO row = result.get(0);
			if (row.getSubtopicPriority() == 1) { // 意圖明確
				processContext.setSubtopic(row.getSubtopicName());
				processContext.setTopicName(row.getTopicName());
				return processContext;
			} else {
				// 回覆顧客是否需要建立什麼流程
				processContext.setIntent("help_customer");
				processContext.setSubtopic("FQA_QUESTION");
				processContext.setTopicName("SINGLE_TURN");
				return processContext;
			}
		} else if (result.size() > 1) {
			// 詢問顧客需要建立什麼流程，提供查到的主題做選項
			processContext.setIntent("help_customer");
			processContext.setSubtopic("FQA_QUESTION");
			processContext.setTopicName("SINGLE_TURN");
		} else {
			// 詢問顧客需要建立什麼流程，提供預設的多選項
			processContext.setIntent("help_customer");
			processContext.setSubtopic("FQA_QUESTION");
			processContext.setTopicName("SINGLE_TURN");
		}

		Topic topic = this.topicRepository.findByName(processContext.getTopicName())
				.orElseThrow(() -> new RuntimeException("Topic not found"));
		// 建立issue
		Issue issue = new Issue();
		issue.setSessionId(sessionId);
		issue.setTopic(topic);
		issue.setStatus("active");
		issue.setCreatedAt(OffsetDateTime.now());
		issue.setUpdatedAt(OffsetDateTime.now());
		issue = this.issueRepository.save(issue);

		return processContext;
	}

	private boolean isIntentInTopic(String intentName, Topic topic) {
		List<IntentInfoDTO> result = this.intentRepository.findIntentDetailByName(intentName);
		for (IntentInfoDTO row : result) {
			if (row.getTopicName().equals(topic.getName())) {
				return true;
			}
		}
		return false;
	}

}

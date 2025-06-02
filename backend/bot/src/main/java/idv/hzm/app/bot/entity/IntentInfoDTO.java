package idv.hzm.app.bot.entity;

public interface IntentInfoDTO {

	Long getIntentId();

	String getIntentName();

	Long getSubtopicId();

	String getSubtopicName();

	String getSubtopicStatus();

	Integer getSubtopicPriority();
	
	Long getTopicId();

	String getTopicName();

	String getTopicStatus();
}

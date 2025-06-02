package idv.hzm.app.bot.rasa.dto;

import java.util.List;

public class SelectorDetail {
    private Response response;
    private List<Object> ranking; // 可改成具體類型
	public Response getResponse() {
		return response;
	}
	public void setResponse(Response response) {
		this.response = response;
	}
	public List<Object> getRanking() {
		return ranking;
	}
	public void setRanking(List<Object> ranking) {
		this.ranking = ranking;
	}
	@Override
	public String toString() {
		return "SelectorDetail [response=" + response + ", ranking=" + ranking + "]";
	}

    // getters and setters
    
}


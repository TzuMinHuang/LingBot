package idv.hzm.app.bot.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import idv.hzm.app.bot.service.SuggestionService;

/**
 * Seeds default FAQ suggestions into Redis on first startup if empty. Modify
 * the list below to match your product's common questions.
 */
@Component
public class SuggestionSeeder implements CommandLineRunner {

	@Autowired
	private SuggestionService suggestionService;

	@Override
	public void run(String... args) {
		suggestionService
				.seedIfEmpty(List.of("產品規格查詢", "保固期限說明", "訂單狀態查詢", "技術支援聯繫方式", "退換貨流程", "韌體更新方式", "帳號相關問題", "付款方式說明"));
	}
}

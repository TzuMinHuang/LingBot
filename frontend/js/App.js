import { ChatApp } from './ChatApp.js';
import { getEnvConfig } from './env-config.js';

document.addEventListener('DOMContentLoaded', async () => {
  const config = await getEnvConfig();
  console.info(`[chatbot] env=${config.env}, apiBase=${config.apiBase}`);
  const app = new ChatApp(config);
  window.addEventListener('beforeunload', () => app.destroy());
});

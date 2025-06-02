import { ChatUI } from './ChatUI.js';
import { ChatService } from './ChatService.js';
import { EventBus } from './EventBus.js';

export class ChatApp {
  constructor(baseUrl) {
    this.baseUrl = baseUrl;
    this.sessionId = null;
    this.chatUI = new ChatUI();
    this.chatService = null;
    this.eventBus = new EventBus();

    this._bindEvents();
    this._initSession();
  }

  _bindEvents() {
    this.chatUI.onSend((message) => this._handleUserMessage(message));
    this.eventBus.on('incoming', (message) => this.chatUI.showIncoming(message));
  }

  async _initSession() {
    try {
      const res = await fetch(`${this.baseUrl}/api/chat/initial`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      });
      const data = await res.json();
      this.sessionId = data.sessionId;

      this.chatService = new ChatService(`${this.baseUrl}/ws`, this.sessionId, (msg) => {
        this.eventBus.emit('incoming', msg.payload?.content || '');
      });
    } catch (e) {
      console.error("初始化失敗", e);
      this.chatUI.showIncoming("連線失敗，請稍後再試。");
    }
  }

  _handleUserMessage(message) {
    if (!message || !this.chatService) return;

    this.chatUI.showOutgoing(message);
    this.chatUI.showThinking();

    this.chatService.sendMessage({ type: 'message', content: message });
  }
}

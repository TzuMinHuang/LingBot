import { ChatUI } from './ChatUI.js';
import { ChatService } from './ChatService.js';
import { AuthService } from './AuthService.js';
import { EventBus } from './EventBus.js';
import { fetchWithTimeout } from './utils.js';

export class ChatApp {
  constructor(config) {
    const ui = config.ui || {};
    this.baseUrl = config.apiBase;
    this.sessionId = null;
    this._idleTimeoutMs = ui.idleTimeoutMs || 300000;
    this._idleSuggestMs = ui.idleSuggestMs || 240000;
    this._suggestCacheMs = ui.suggestCacheMs || 3600000;
    this._defaultSuggestions = ui.defaultSuggestions || [
      '產品規格查詢', '保固期限說明', '訂單狀態查詢', '技術支援聯繫方式',
      '退換貨流程', '韌體更新方式', '帳號相關問題', '付款方式說明'
    ];
    this.chatUI = new ChatUI({
      maxInputLength: ui.maxInputLength,
      streamSpeed: ui.streamSpeed,
      locale: ui.locale,
    });
    this.chatService = null;
    this.authService = new AuthService({
      validateUrl: config.authValidateUrl,
      ssoUrl: config.authSsoUrl,
    });
    this.eventBus = new EventBus();
    this._idleTimer = null;
    this._idleSuggestTimer = null;
    this._disconnected = false;
    this._suggestionsLoaded = false;
    this._suggestFetchedAt = 0;
    this._queuePollTimer = null;

    this._bindEvents();
    this._initSession();
    this._startIdleDetection();
  }

  // ── Event binding ──────────────────────────────────────────

  _bindEvents() {
    this.chatUI.onSend((message) => this._handleUserMessage(message));
    this.chatUI.onEnd(() => this._handleEndConversation());
    this.chatUI.onReconnect(() => this._handleReconnect());
    this.chatUI.onSuggest(() => this._fetchSuggestions());
    this.eventBus.on('incoming', (message) => this.chatUI.showIncoming(message));
  }

  // ── Session lifecycle ──────────────────────────────────────

  async _initSession() {
    try {
      const headers = { 'Content-Type': 'application/json' };
      const token = this.authService.getToken();
      if (token) headers['Authorization'] = `Bearer ${token}`;

      const res = await fetchWithTimeout(`${this.baseUrl}/bot/chat/initial`, {
        method: 'POST',
        headers
      });
      const data = await res.json();
      this.sessionId = data.sessionId;

      this.chatUI.showIncoming("你好！有什麼可以幫助你的嗎？");

      // Show suggestion cards on first entry
      await this._fetchSuggestions();
      this.chatUI.showSuggestionCards(null, '以下是常見問題，您可以直接點選：');

      this._connectSSE();
      this._disconnected = false;
    } catch (e) {
      console.error("初始化失敗", e);
      this.chatUI.showIncoming("連線失敗，請稍後再試。");
    }
  }

  _connectSSE() {
    this.chatService = new ChatService(this.baseUrl, this.sessionId, (msg) => {
      this._resetIdleTimer(); // SSE activity counts as non-idle
      if (msg.type === 'PROCESSING_START') {
        this._stopQueuePolling();
        this.chatUI.showProcessing();
      } else if (msg.type === 'STREAM_CHUNK') {
        this._stopQueuePolling();
        this.chatUI.appendToStreamBubble(msg.payload?.content || '');
      } else if (msg.type === 'STREAM_END') {
        this._stopQueuePolling();
        this.chatUI.finishStreamBubble(msg.payload?.sources);
      } else {
        this.eventBus.emit('incoming', msg.payload?.content || '');
      }
    });
  }

  _disconnect() {
    if (this.chatService) {
      this.chatService.disconnect();
      this.chatService = null;
    }
    this._disconnected = true;
  }

  // ── Suggestions ───────────────────────────────────────────

  async _fetchSuggestions() {
    // Use cache if fetched within the last hour
    if (this._suggestionsLoaded && (Date.now() - this._suggestFetchedAt < this._suggestCacheMs)) {
      return;
    }
    try {
      const res = await fetchWithTimeout(`${this.baseUrl}/bot/chat/suggestions`);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      const items = data.suggestions || data || [];
      this.chatUI.setSuggestions(items.length ? items : this._defaultSuggestions);
      this._suggestionsLoaded = true;
      this._suggestFetchedAt = Date.now();
    } catch (e) {
      console.warn('[ChatApp] Failed to fetch suggestions, using defaults:', e);
      this.chatUI.setSuggestions(this._defaultSuggestions);
      this._suggestionsLoaded = true;
      this._suggestFetchedAt = Date.now();
    }
  }

  // ── User message handling ──────────────────────────────────

  _handleUserMessage(message) {
    if (!message) return;

    // If disconnected, block sending and prompt reconnect
    if (this._disconnected || !this.chatService) {
      this.chatUI.showReconnectMessage('連線已中斷，請點擊「重新連線」繼續對話。');
      return;
    }

    this._resetIdleTimer();
    this.chatUI.finishStreamBubble();
    this.chatUI.showOutgoing(message);
    this.chatUI.showThinking();
    this.chatService.sendMessage({ type: 'message', content: message });
    this._startQueuePolling();
  }

  _handleEndConversation() {
    if (this.chatService) {
      this.chatService.stopReply();
    }
    this.chatUI.stopCurrentStream();
  }

  // ── Idle detection ─────────────────────────────────────────

  _startIdleDetection() {
    const events = ['keydown', 'mousedown', 'mousemove', 'touchstart', 'scroll', 'click'];
    events.forEach(evt => {
      document.addEventListener(evt, () => this._resetIdleTimer(), { passive: true });
    });
    this._resetIdleTimer();
  }

  _resetIdleTimer() {
    if (this._disconnected) return; // don't reset if already disconnected

    // 4-min suggestion prompt
    clearTimeout(this._idleSuggestTimer);
    this._idleSuggestTimer = setTimeout(() => this._onIdleSuggest(), this._idleSuggestMs);

    // 5-min disconnect
    clearTimeout(this._idleTimer);
    this._idleTimer = setTimeout(() => this._onIdleTimeout(), this._idleTimeoutMs);
  }

  _onIdleSuggest() {
    if (this._disconnected) return;
    console.info('Idle 4 min — showing suggestion prompt');
    this.chatUI.showSuggestionCards(null, '您還在嗎？是否需要再次詢問：');
  }

  _onIdleTimeout() {
    console.info('Idle timeout — disconnecting SSE');
    clearTimeout(this._idleSuggestTimer);
    this.chatUI.stopCurrentStream();
    this._disconnect();
    this.chatUI.showReconnectMessage('因長時間未操作，連線已自動中斷。請點擊「重新連線」繼續對話。');
  }

  // ── Reconnect flow ─────────────────────────────────────────

  async _handleReconnect() {
    this.chatUI.showIncoming('正在重新連線...');

    const tokenValid = await this.authService.validateToken();

    if (tokenValid) {
      // Token still valid — reconnect directly
      await this._reconnect();
    } else if (this.authService.getToken()) {
      // Token exists but expired — redirect to middleware SSO
      this.chatUI.showIncoming('登入已過期，正在導向登入頁面...');
      setTimeout(() => this.authService.redirectToSSO(), 1500);
    } else {
      // No token at all — try reconnect without auth (current anonymous mode)
      await this._reconnect();
    }
  }

  // ── Queue position polling ──────────────────────────────────

  _startQueuePolling() {
    this._stopQueuePolling();
    if (!this.sessionId) return; // guard: no session yet
    const poll = async () => {
      try {
        const res = await fetchWithTimeout(
          `${this.baseUrl}/bot/chat/${this.sessionId}/queue-position`
        );
        if (res.ok) {
          const data = await res.json();
          this.chatUI.updateQueuePosition(data.position);
        }
      } catch (e) {
        console.warn('[ChatApp] Queue position poll failed:', e);
      }
    };
    poll();
    this._queuePollTimer = setInterval(poll, 2000);
  }

  _stopQueuePolling() {
    if (this._queuePollTimer) {
      clearInterval(this._queuePollTimer);
      this._queuePollTimer = null;
    }
  }

  destroy() {
    clearTimeout(this._idleTimer);
    clearTimeout(this._idleSuggestTimer);
    this._stopQueuePolling();
    this._disconnect();
  }

  async _reconnect() {
    try {
      await this._initSession();
      this._resetIdleTimer();
    } catch (e) {
      console.error('重新連線失敗', e);
      this.chatUI.showReconnectMessage('重新連線失敗，請點擊「重新連線」再試一次。');
    }
  }
}

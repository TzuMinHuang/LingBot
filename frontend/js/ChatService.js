export class ChatService {
  constructor(baseUrl, sessionId, onMessageCallback) {
    this.baseUrl = baseUrl;
    this.sessionId = sessionId;
    this.callback = onMessageCallback;
    this._connect();
  }

  _connect() {
    this._stopped = false;
    const url = `${this.baseUrl}/bot/chat/${this.sessionId}/stream`;
    this.eventSource = new EventSource(url);

    this.eventSource.addEventListener('chat', (e) => {
      if (this._stopped) return; // ignore chunks after stop
      try {
        const msg = JSON.parse(e.data);
        this.callback(msg);
      } catch (err) {
        console.error('Failed to parse SSE message:', err);
      }
    });

    this.eventSource.onerror = () => {
      console.warn('SSE connection error, auto-reconnecting...');
    };
  }

  sendMessage(message) {
    this._stopped = false;
    fetch(`${this.baseUrl}/bot/chat/${this.sessionId}/send`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(message)
    }).catch(err => console.error('Send failed:', err));
  }

  /** Stop processing current reply — ignore further chunks until next sendMessage */
  stopReply() {
    this._stopped = true;
    // Notify backend to abort generation (fire-and-forget)
    fetch(`${this.baseUrl}/bot/chat/${this.sessionId}/stop`, {
      method: 'POST'
    }).catch(() => {});
  }

  disconnect() {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }
}

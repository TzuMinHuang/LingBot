import { WebSocketClient } from './websocket.js';

export class ChatService {
  constructor(url, sessionId, onMessageCallback) {
    this.client = new WebSocketClient(url, sessionId, onMessageCallback);
  }

  sendMessage(message) {
    this.client.sendMessage(message);
  }
}

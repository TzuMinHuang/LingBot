// websocket.js
class WebSocketClient {

    constructor(url,sessionId,onMessageCallback) {
        this.callbacks = [];
        this.onMessage(onMessageCallback);
        this.sessionId = sessionId;
        this.socket = new SockJS(url); //WebSocket 端點
        this.stompClient = Stomp.over(this.socket);
        this.stompClient.connect({}, (frame) => {
            console.log("Connected: " + frame);
            // 訂閱從後端送出的訊息
            this.stompClient.subscribe(`/topic/user/${sessionId}/receive`, (message) => {
                console.log("msessageg:", message);
                const msg = JSON.parse(message.body);
                this._handleMessage(msg);
            });
        
            //發送訊息（送到 @MessageMapping）
            const chatMessage = {
              type: "message",
              content: "你好"
            };
        
            this.sendMessage(chatMessage);
        });
   }

   sendMessage(message) {
    this.stompClient.send(`/app/chat/${this.sessionId}/send`, {}, JSON.stringify(message));
   }
  
    onMessage(callback) {
      this.callbacks.push(callback);
    }
  
    _handleMessage(data) {
      this.callbacks.forEach(callback => {
        callback(data);
      });
    }
  }
  
  function initWebSocket(url,sessionId,onMessageCallback) {
    return new WebSocketClient(url,sessionId,onMessageCallback);
  }
  
  export { initWebSocket, WebSocketClient };
  
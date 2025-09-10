export class ChatUI {
    constructor() {
      this.chatbox = document.querySelector('.chatbox');
      this.chatInput = document.querySelector('.chat-input textarea');
      this.sendBtn = document.querySelector('.chat-input span');
      this.inputInitHeight = this.chatInput.scrollHeight;
      this.sendCallback = null;
  
      this._setupInputHandler();
      this._setupButtonHandler();
      this.showThinking();
    }
  
    _setupInputHandler() {
      this.chatInput.addEventListener('input', () => {
        this.chatInput.style.height = `${this.inputInitHeight}px`;
        this.chatInput.style.height = `${this.chatInput.scrollHeight}px`;
      });

      let isComposing = false;

      this.chatInput.addEventListener('compositionstart', () => {
        isComposing = true;
      });

      this.chatInput.addEventListener('compositionend', () => {
        isComposing = false;
      });
  
      this.chatInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey && !isComposing) {
          e.preventDefault();
          const message = this.chatInput.value.trim();
          if (!message) return;

          this.chatInput.value = "";
          if (typeof this.sendCallback === 'function') {
            this.sendCallback(message);
          }
        }
      });
    }
  
    _setupButtonHandler() {
      this.sendBtn.addEventListener('click', () => {
        const message = this.chatInput.value.trim();
        if (message && this.sendCallback) {
          this.chatInput.value = "";
          this.sendCallback(message);
        }
      });
    }
  
    onSend(callback) {
      this.sendCallback = callback;
    }
  
    showOutgoing(message) {
      this._appendChat(message, 'outgoing');
    }
  
    showIncoming(message) {
      this._replaceThinkingIfExists(message) || this._appendChat(message, 'incoming');
    }
  
    showThinking() {
      this._appendChat("Thinking...", 'incoming', true);
    }
  
    _appendChat(message, type, isThinking = false) {
      const li = document.createElement('li');
      li.className = `chat ${type}`;
      li.innerHTML = type === "outgoing"
        ? `<p>${message}</p>`
        : `<span class="material-symbols-outlined">smart_toy</span><p>${message}</p>`;
      if (isThinking) li.dataset.thinking = "true";
  
      this.chatbox.appendChild(li);
      requestAnimationFrame(() => this.chatbox.scrollTop = this.chatbox.scrollHeight);
    }
  
    _replaceThinkingIfExists(message) {
      const last = this.chatbox.querySelector('li.chat.incoming[data-thinking="true"] p');
      if (last) {
        last.textContent = message;
        last.parentElement.removeAttribute('data-thinking');
        return true;
      }
      return false;
    }

 addBotMessageWithCards(message, cardsHtml) {
  const li = document.createElement('li');
  li.classList.add('chat', 'incoming');

  // 卡片容器
  const cardContainer = document.createElement('div');
  cardContainer.classList.add('card-slider');
  cardContainer.innerHTML = `
  <div class="card">
    <h3>阿發猜你想知道</h3>
    <div class="btns">
      <button>保費分期零利率</button>
      <button>本期帳單金額</button>
      <button>刷卡消費分期</button>
    </div>
  </div>
  <div class="card">
    <h3>信用卡熱門查詢</h3>
    <div class="btns">
      <button>查詢消費明細</button>
      <button>查詢可用額度</button>
      <button>申請額度調整</button>
    </div>
  </div>
  <div class="card">
    <h3>信用卡刷卡必知</h3>
    <div class="btns">
      <button>查詢回饋點數</button>
      <button>申請預借現金</button>
      <button>補寄電子帳單</button>
    </div>
  </div>
`;

  li.appendChild(cardContainer);
  this.chatbox.appendChild(li);

  // 初始化 Slick
  $(cardContainer).slick({
    slidesToShow: 1,
    slidesToScroll: 1,
    dots: true,
    infinite: false,
    arrows: true
  });

  // 點擊按鈕也會發送訊息
  cardContainer.querySelectorAll('button').forEach(btn => {
    btn.addEventListener('click', () => {
      this.sendCallback(btn.textContent);
    });
  });

  this.chatbox.scrollTop = this.chatbox.scrollHeight;
}

// 範例卡片 HTML


}
  
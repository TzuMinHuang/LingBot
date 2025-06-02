export class ChatUI {
    constructor() {
      this.chatbox = document.querySelector('.chatbox');
      this.chatInput = document.querySelector('.chat-input textarea');
      this.sendBtn = document.querySelector('.chat-input span');
      this.inputInitHeight = this.chatInput.scrollHeight;
      this.sendCallback = null;
  
      this._setupInputHandler();
      this._setupButtonHandler();
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
  }
  
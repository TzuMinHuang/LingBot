export class ChatUI {
  constructor({ maxInputLength = 500, streamSpeed = 18, locale = 'zh-TW' } = {}) {
    this._maxInputLength = maxInputLength;
    this._locale = locale;

    this.chatbox = document.querySelector('.chatbox');
    this.chatInputWrap = document.querySelector('.chat-input');
    this.chatInput = document.querySelector('.chat-input textarea');
    this.actionBtn = document.querySelector('#action-btn');
    this.endBtn = document.querySelector('#end-btn');
    this.charCounter = document.querySelector('.char-counter');
    this.inputInitHeight = this.chatInput.scrollHeight;
    this.suggestBtn = document.querySelector('#suggest-btn');
    this.sendCallback = null;
    this.endCallback = null;
    this.reconnectCallback = null;
    this.suggestCallback = null;
    this._suggestions = [];

    // Typewriter streaming queue
    this._streamQueue = [];    // pending characters
    this._streamTarget = null; // the <p> being typed into
    this._streamTimer = null;
    this._streamSpeed = streamSpeed;

    this._recognition = null;
    this._isRecording = false;
    this._streamRawText = ''; // accumulate raw text during streaming

    // Configure marked for safe rendering
    if (typeof marked !== 'undefined') {
      marked.setOptions({ breaks: true, gfm: true });
    }

    // Sync maxlength attribute with config
    this.chatInput.maxLength = this._maxInputLength;

    this._setupInputHandler();
    this._setupActionButton();
    this._setupSpeechRecognition();
    this._setupEndButton();
    this._setupSuggestButton();
    this._updateCharCounter();
  }

    _setupInputHandler() {
      this.chatInput.addEventListener('input', () => {
        this.chatInput.style.height = `${this.inputInitHeight}px`;
        this.chatInput.style.height = `${this.chatInput.scrollHeight}px`;
        this._updateCharCounter();
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
          this._trySend();
        }
      });
    }

    _setupActionButton() {
      this.actionBtn.addEventListener('click', () => {
        if (this.actionBtn.classList.contains('mode-send')) {
          this._trySend();
        } else if (this._isRecording) {
          this._stopRecording();
        } else {
          this._startRecording();
        }
      });
      // Switch to send mode when textarea has content
      this.chatInput.addEventListener('input', () => this._updateActionMode());
    }

    _updateActionMode() {
      const hasText = this.chatInput.value.trim().length > 0;
      if (hasText && !this._isRecording) {
        this.actionBtn.classList.add('mode-send');
        this.actionBtn.textContent = 'send';
        this.actionBtn.title = '送出';
      } else if (!this._isRecording) {
        this.actionBtn.classList.remove('mode-send');
        this.actionBtn.textContent = 'mic';
        this.actionBtn.title = '語音輸入';
      }
    }

    _lockInput() {
      this.chatInput.disabled = true;
      this.chatInput.placeholder = '等待回覆中...';
      if (this.suggestBtn) this.suggestBtn.classList.add('disabled');
    }

    _unlockInput() {
      this.chatInput.disabled = false;
      this.chatInput.placeholder = '輸入指令...';
      this.chatInput.focus();
      if (this.suggestBtn) this.suggestBtn.classList.remove('disabled');
    }

    _setupSpeechRecognition() {
      const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
      if (!SpeechRecognition) {
        // Browser doesn't support — hide mic, show only when has text (send mode)
        this.actionBtn.classList.add('no-speech');
        return;
      }
      const recognition = new SpeechRecognition();
      recognition.lang = this._locale;
      recognition.continuous = true;
      recognition.interimResults = true;

      recognition.onresult = (event) => {
        let transcript = '';
        for (let i = 0; i < event.results.length; i++) {
          transcript += event.results[i][0].transcript;
        }
        this.chatInput.value = transcript;
        this._updateCharCounter();
        this.chatInput.style.height = `${this.inputInitHeight}px`;
        this.chatInput.style.height = `${this.chatInput.scrollHeight}px`;
      };

      recognition.onend = () => {
        // continuous mode may auto-stop (e.g. network hiccup) — restart if still recording
        if (this._isRecording) {
          try { recognition.start(); } catch (_) { /* already started */ }
          return;
        }
        this.actionBtn.classList.remove('recording');
        this._updateActionMode();
      };

      recognition.onerror = (event) => {
        console.warn('[Speech] Error:', event.error);
        this._isRecording = false;
        this.actionBtn.classList.remove('recording');
        this._updateActionMode();
      };

      this._recognition = recognition;
    }

    _startRecording() {
      if (!this._recognition) return;
      this._isRecording = true;
      this.actionBtn.classList.add('recording');
      this.actionBtn.classList.remove('mode-send');
      this.actionBtn.textContent = 'stop';
      this.actionBtn.title = '停止錄音';
      this._recognition.start();
    }

    _stopRecording() {
      if (!this._recognition) return;
      this._isRecording = false;
      this._recognition.stop();
    }

    _setupEndButton() {
      if (!this.endBtn) return;
      this.endBtn.addEventListener('click', () => {
        if (typeof this.endCallback === 'function') {
          this.endCallback();
        }
      });
    }

    onEnd(callback) {
      this.endCallback = callback;
    }

    onReconnect(callback) {
      this.reconnectCallback = callback;
    }

    /** Show a bot message with a clickable "reconnect" link */
    showReconnectMessage(text) {
      const li = document.createElement('li');
      li.className = 'chat incoming';

      const icon = document.createElement('span');
      icon.className = 'material-symbols-outlined';
      icon.textContent = 'smart_toy';
      li.appendChild(icon);

      const p = document.createElement('p');
      p.className = 'reconnect-msg';
      p.textContent = text + ' ';

      const link = document.createElement('a');
      link.className = 'reconnect-link';
      link.textContent = '重新連線';
      link.href = '#';
      link.addEventListener('click', (e) => {
        e.preventDefault();
        // Disable link to prevent double-click
        link.classList.add('disabled');
        link.textContent = '連線中...';
        if (typeof this.reconnectCallback === 'function') {
          this.reconnectCallback();
        }
      });
      p.appendChild(link);

      li.appendChild(p);
      this.chatbox.appendChild(li);
      requestAnimationFrame(() => this.chatbox.scrollTop = this.chatbox.scrollHeight);
    }

    _trySend() {
      const message = this.chatInput.value.trim();
      if (!message) return;
      if (message.length > this._maxInputLength) return;
      this.chatInput.value = "";
      this._updateCharCounter();
      this._updateActionMode();
      if (typeof this.sendCallback === 'function') {
        this.sendCallback(message);
      }
    }

    _updateCharCounter() {
      const len = this.chatInput.value.length;
      this.charCounter.textContent = `${len} / ${this._maxInputLength}`;
      this.charCounter.classList.toggle('over-limit', len >= this._maxInputLength);
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
      this._appendChat("排隊中...", 'incoming', true);
      this.chatInputWrap.classList.add('streaming');
      this._lockInput();
    }

    updateQueuePosition(position) {
      const el = this.chatbox.querySelector('li.chat.incoming[data-thinking="true"] p');
      if (!el) return;
      if (position > 0) {
        el.textContent = `排隊中，前方約 ${position} 人...`;
      } else {
        el.textContent = 'AI 正在回覆...';
      }
    }

    showProcessing() {
      const el = this.chatbox.querySelector('li.chat.incoming[data-thinking="true"] p');
      if (el) {
        el.textContent = 'Thinking...';
      }
    }
  
    _appendChat(message, type, isThinking = false) {
      const li = document.createElement('li');
      li.className = `chat ${type}`;
      if (type !== 'outgoing') {
        const icon = document.createElement('span');
        icon.className = 'material-symbols-outlined';
        icon.textContent = 'smart_toy';
        li.appendChild(icon);
      }
      // Render markdown for incoming bot messages (not thinking/outgoing)
      const html = (type === 'incoming' && !isThinking) ? this._renderMarkdown(message) : null;
      let el;
      if (html) {
        el = document.createElement('div');
        el.className = 'msg-body md-rendered';
        el.innerHTML = html;
      } else {
        el = document.createElement('p');
        el.textContent = message;
      }
      li.appendChild(el);
      if (isThinking) li.dataset.thinking = 'true';

      this.chatbox.appendChild(li);
      requestAnimationFrame(() => this.chatbox.scrollTop = this.chatbox.scrollHeight);
    }
  
    _replaceThinkingIfExists(message) {
      const all = this.chatbox.querySelectorAll('li.chat.incoming[data-thinking="true"] p');
      const last = all.length ? all[all.length - 1] : null;
      if (last) {
        last.textContent = message;
        last.parentElement.removeAttribute('data-thinking');
        return true;
      }
      return false;
    }

    /** Queue a chunk for typewriter display */
    appendToStreamBubble(chunk) {
      // Find or create the streaming bubble
      const streamingAll = this.chatbox.querySelectorAll('li.chat.incoming[data-streaming="true"] p');
      let p = streamingAll.length ? streamingAll[streamingAll.length - 1] : null;

      if (!p) {
        this._streamRawText = ''; // reset accumulator for new stream
        const thinkingAll = this.chatbox.querySelectorAll('li.chat.incoming[data-thinking="true"]');
        const thinking = thinkingAll.length ? thinkingAll[thinkingAll.length - 1] : null;
        if (thinking) {
          p = thinking.querySelector('p');
          p.textContent = '';
          thinking.removeAttribute('data-thinking');
          thinking.dataset.streaming = 'true';
        } else {
          const li = document.createElement('li');
          li.className = 'chat incoming';
          li.dataset.streaming = 'true';
          li.innerHTML = `<span class="material-symbols-outlined">smart_toy</span><p></p>`;
          this.chatbox.appendChild(li);
          p = li.querySelector('p');
        }
      }

      // Accumulate raw text for markdown rendering on finish
      this._streamRawText += chunk;

      // Set target and enqueue characters
      this._streamTarget = p;
      for (const ch of chunk) {
        this._streamQueue.push(ch);
      }
      if (!this._streamTimer) {
        this._drainQueue();
      }
    }

    _drainQueue() {
      if (this._streamQueue.length === 0) {
        this._streamTimer = null;
        return;
      }
      const ch = this._streamQueue.shift();
      if (this._streamTarget) {
        this._streamTarget.textContent += ch;
        this.chatbox.scrollTop = this.chatbox.scrollHeight;
      }
      this._streamTimer = setTimeout(() => this._drainQueue(), this._streamSpeed);
    }

    /** Stop current streaming/thinking: flush queue, remove markers */
    stopCurrentStream() {
      // Clear typewriter queue
      this._streamQueue = [];
      if (this._streamTimer) {
        clearTimeout(this._streamTimer);
        this._streamTimer = null;
      }
      this._streamTarget = null;

      // Remove streaming markers
      this.chatbox.querySelectorAll('li.chat.incoming[data-streaming="true"]').forEach(el => {
        el.removeAttribute('data-streaming');
      });

      // Remove thinking bubbles
      this.chatbox.querySelectorAll('li.chat.incoming[data-thinking="true"]').forEach(el => {
        el.remove();
      });

      this.chatInputWrap.classList.remove('streaming');
      this._unlockInput();
    }

    /** 串流結束：等 queue 清空後，渲染 markdown，移除 data-streaming 標記，並附加參考資料 */
    finishStreamBubble(sources) {
      const flush = () => {
        if (this._streamQueue.length > 0) {
          // Still draining — check again shortly
          setTimeout(flush, 50);
          return;
        }
        const all = this.chatbox.querySelectorAll('li.chat.incoming[data-streaming="true"]');
        const el = all.length ? all[all.length - 1] : null;
        if (!el) return;

        // Render accumulated raw text as markdown — replace <p> with <div> for valid HTML
        const p = el.querySelector('p');
        if (p && this._streamRawText) {
          const html = this._renderMarkdown(this._streamRawText);
          if (html) {
            const div = document.createElement('div');
            div.className = 'msg-body md-rendered';
            div.innerHTML = html;
            p.replaceWith(div);
          }
        }
        this._streamRawText = '';

        el.removeAttribute('data-streaming');
        this._streamTarget = null;
        this.chatInputWrap.classList.remove('streaming');
        this._unlockInput();
        if (sources && sources.length > 0) {
          this._appendSources(el, sources);
        }
        requestAnimationFrame(() => this.chatbox.scrollTop = this.chatbox.scrollHeight);
      };
      flush();
    }

    _appendSources(li, sources) {
      const container = document.createElement('div');
      container.className = 'sources-container';

      const label = document.createElement('span');
      label.className = 'sources-label';
      label.textContent = '參考資料';
      container.appendChild(label);

      sources.forEach(source => {
        const title = source.title || source.url || '文件';
        const tag = document.createElement('a');
        tag.className = 'source-tag';
        tag.textContent = title;
        if (source.url) {
          tag.href = source.url;
          tag.target = '_blank';
          tag.rel = 'noopener noreferrer';
        }
        container.appendChild(tag);
      });

      // Wrap message body and sources in a bubble-wrap so they appear as one bubble
      const msgEl = li.querySelector('.msg-body') || li.querySelector('p');
      if (msgEl) {
        const wrap = document.createElement('div');
        wrap.className = 'bubble-wrap';
        msgEl.parentNode.insertBefore(wrap, msgEl);
        wrap.appendChild(msgEl);
        wrap.appendChild(container);
      } else {
        li.appendChild(container);
      }

      requestAnimationFrame(() => this.chatbox.scrollTop = this.chatbox.scrollHeight);
    }

    /** Render markdown string to sanitized HTML */
    _renderMarkdown(text) {
      if (typeof marked === 'undefined' || typeof DOMPurify === 'undefined') {
        return null; // fallback: caller should use textContent
      }
      const raw = marked.parse(text);
      return DOMPurify.sanitize(raw);
    }

    // ── Suggest button ──

    _setupSuggestButton() {
      if (!this.suggestBtn) return;
      this.suggestBtn.addEventListener('click', async () => {
        if (this.suggestBtn.classList.contains('disabled')) return;
        if (typeof this.suggestCallback === 'function') {
          await this.suggestCallback();
        }
        this.showSuggestionCards(null, '常見問題：');
      });
    }

    onSuggest(callback) {
      this.suggestCallback = callback;
    }

    /** Update cached suggestions (called when API data arrives) */
    setSuggestions(suggestions) {
      this._suggestions = suggestions || [];
    }

    /** Show suggestion chips inside a bot message bubble */
    showSuggestionCards(suggestions, message) {
      const items = suggestions || this._suggestions;
      if (!items.length) return;

      const li = document.createElement('li');
      li.className = 'chat incoming';

      const icon = document.createElement('span');
      icon.className = 'material-symbols-outlined';
      icon.textContent = 'smart_toy';
      li.appendChild(icon);

      const wrap = document.createElement('div');
      wrap.className = 'bubble-wrap';

      const p = document.createElement('p');
      p.textContent = message || '常見問題：';
      wrap.appendChild(p);

      const container = document.createElement('div');
      container.className = 'suggestion-cards';

      items.forEach(item => {
        const chip = document.createElement('span');
        chip.className = 'suggestion-chip';
        chip.textContent = item;
        chip.addEventListener('click', () => {
          if (typeof this.sendCallback === 'function') {
            this.sendCallback(item);
          }
        });
        container.appendChild(chip);
      });

      wrap.appendChild(container);
      li.appendChild(wrap);
      this.chatbox.appendChild(li);
      requestAnimationFrame(() => this.chatbox.scrollTop = this.chatbox.scrollHeight);
    }

}
  
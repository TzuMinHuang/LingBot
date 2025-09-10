(function() {
    // 避免全局變量衝突
    const CHATBOT_IFRAME_ID = 'chatbot-plugin-iframe';
    const CHATBOT_TOGGLE_ID = 'chatbot-plugin-toggle';
  
    // ----- 建立 Toggle Button -----
    const toggleBtn = document.createElement('button');
    toggleBtn.id = CHATBOT_TOGGLE_ID;
    toggleBtn.innerText = '💬';
    Object.assign(toggleBtn.style, {
      position: 'fixed',
      right: '40px',
      bottom: '35px',
      height: '50px',
      width: '50px',
      borderRadius: '50%',
      background: '#007FFF',
      color: 'white',
      border: 'none',
      cursor: 'pointer',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      fontSize: '1.5rem',
      zIndex: '9999',
      transition: 'transform 0.2s ease'
    });
    document.body.appendChild(toggleBtn);
  
    // ----- 建立 iframe -----
    const iframe = document.createElement('iframe');
    iframe.id = CHATBOT_IFRAME_ID;
    iframe.src = 'http://localhost:5500/chatbot.html'; // 替換成你的 Chatbot URL
    Object.assign(iframe.style, {
      position: 'fixed',
      right: '40px',
      bottom: '100px',
      width: '420px',
      height: '600px',
      border: 'none',
      borderRadius: '15px',
      boxShadow: '0 0 128px rgba(0,0,0,0.1), 0 32px 64px -48px rgba(0,0,0,0.5)',
      transform: 'scale(0.5)',
      opacity: '0',
      pointerEvents: 'none',
      transformOrigin: 'bottom right',
      transition: 'all 0.2s ease',
      zIndex: '9998'
    });
    document.body.appendChild(iframe);
  
    // ----- Toggle 功能 -----
    let isOpen = false;
    toggleBtn.addEventListener('click', () => {
      isOpen = !isOpen;
      if (isOpen) {
        iframe.style.transform = 'scale(1)';
        iframe.style.opacity = '1';
        iframe.style.pointerEvents = 'auto';
        toggleBtn.style.transform = 'rotate(90deg)';
      } else {
        iframe.style.transform = 'scale(0.5)';
        iframe.style.opacity = '0';
        iframe.style.pointerEvents = 'none';
        toggleBtn.style.transform = 'rotate(0deg)';
      }
    });
  
    // ----- 可選：按 ESC 鍵關閉 Chatbot -----
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && isOpen) {
        isOpen = false;
        iframe.style.transform = 'scale(0.5)';
        iframe.style.opacity = '0';
        iframe.style.pointerEvents = 'none';
        toggleBtn.style.transform = 'rotate(0deg)';
      }
    });
  })();
  
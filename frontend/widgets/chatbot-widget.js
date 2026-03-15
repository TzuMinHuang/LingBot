(function () {
  const CHATBOT_IFRAME_ID = 'chatbot-plugin-iframe';
  const CHATBOT_TOGGLE_ID = 'chatbot-plugin-toggle';

  // ── Environment config ──
  //
  // Priority:
  //   1. data-env attribute   → look up ENV_MAP
  //   2. data-chatbot-url     → use directly (full override)
  //   3. Auto-detect from script src origin (cross-domain)
  //   4. Auto-detect from page hostname (same-domain)
  //   5. Fallback to dev
  //
  // Usage examples:
  //   Same site:    <script src="./widgets/chatbot-widget.js"></script>
  //   Same site:    <script src="./widgets/chatbot-widget.js" data-env="test"></script>
  //   Cross domain: <script src="https://chatbot.example.com/widgets/chatbot-widget.js"></script>
  //   Cross domain: <script src="https://chatbot.example.com/widgets/chatbot-widget.js" data-env="prod"></script>
  //   Full override:<script src="https://chatbot.example.com/widgets/chatbot-widget.js" data-chatbot-url="https://chatbot.example.com/pages/chatbot.html"></script>

  // ── Config loading ──
  // All environment settings are loaded from /config/env.json (single source of truth).
  // The widget resolves the config base URL from its own script src for cross-domain support.

  const currentScript = document.currentScript;

  // Resolve where config/env.json lives (same origin as the script)
  function getConfigBaseUrl() {
    if (currentScript && currentScript.src) {
      try {
        const scriptUrl = new URL(currentScript.src);
        if (scriptUrl.origin !== window.location.origin) {
          return scriptUrl.origin;
        }
      } catch (e) { /* ignore */ }
    }
    return '';
  }

  const configBaseUrl = getConfigBaseUrl();
  let chatbotUrl = '/pages/chatbot.html'; // fallback

  function isDomainAllowed(hostname, allowedDomains) {
    return allowedDomains.some(pattern => {
      if (pattern.startsWith('*.')) {
        const suffix = pattern.slice(1);
        return hostname === pattern.slice(2) || hostname.endsWith(suffix);
      }
      return hostname === pattern;
    });
  }

  function resolveChatbotUrl(config) {
    const envMap = config.envMap;
    const hostEnvMap = config.hostEnvMap;

    // 1. data-chatbot-url — full override
    const urlOverride = currentScript && currentScript.getAttribute('data-chatbot-url');
    if (urlOverride) return { env: 'custom', chatbotUrl: urlOverride };

    // 2. data-env — explicit environment
    const envAttr = currentScript && currentScript.getAttribute('data-env');
    if (envAttr && envMap[envAttr]) {
      return { env: envAttr, chatbotUrl: envMap[envAttr].chatbotUrl };
    }

    // 3. Auto-detect from page hostname
    const pageEnv = hostEnvMap[window.location.hostname];
    if (pageEnv) return { env: pageEnv, chatbotUrl: envMap[pageEnv].chatbotUrl };

    // 4. Cross-domain: derive base URL from script src
    if (configBaseUrl) {
      return { env: 'remote', chatbotUrl: `${configBaseUrl}/pages/chatbot.html` };
    }

    // 5. Fallback to dev
    return { env: 'dev', chatbotUrl: envMap.dev.chatbotUrl };
  }

  // Load config and apply domain check
  fetch(`${configBaseUrl}/config/env.json`)
    .then(res => res.json())
    .then(config => {
      // Domain whitelist check
      if (!isDomainAllowed(window.location.hostname, config.allowedDomains)) {
        console.warn(`[chatbot-widget] Domain "${window.location.hostname}" is not allowed. Widget disabled.`);
        toggleBtn.remove();
        iframe.remove();
        return;
      }

      const resolved = resolveChatbotUrl(config);
      chatbotUrl = resolved.chatbotUrl;
      console.info(`[chatbot-widget] env=${resolved.env}, chatbotUrl=${chatbotUrl}`);
    })
    .catch(err => {
      console.warn('[chatbot-widget] Failed to load config, using defaults.', err);
    });

  // ── Inject shared font ──
  const fontLink = document.createElement('link');
  fontLink.rel = 'stylesheet';
  fontLink.href = 'https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500&display=swap';
  document.head.appendChild(fontLink);

  // ── Inject global toggle styles ──
  const style = document.createElement('style');
  style.textContent = `
    #${CHATBOT_TOGGLE_ID} {
      position: fixed;
      right: 24px;
      bottom: 24px;
      width: 54px;
      height: 54px;
      border-radius: 6px;
      background: linear-gradient(135deg, #0e8585 0%, #0a6b6b 100%);
      color: #ffffff;
      border: none;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 22px;
      z-index: 9999;
      transition: box-shadow 0.25s, transform 0.2s, background 0.2s;
      box-shadow: 0 2px 12px rgba(14, 133, 133, 0.5), 0 0 0 2px rgba(15,181,163,0.2);
      font-family: 'Noto Sans TC', sans-serif;
    }
    #${CHATBOT_TOGGLE_ID}:hover {
      background: linear-gradient(135deg, #12a3a3 0%, #0e8585 100%);
      box-shadow: 0 4px 20px rgba(14, 133, 133, 0.6), 0 0 0 2px rgba(15,181,163,0.4);
      transform: translateY(-2px);
    }
    #${CHATBOT_TOGGLE_ID}:active {
      transform: translateY(0);
    }
    #${CHATBOT_TOGGLE_ID}.open {
      background: linear-gradient(135deg, #0a6b6b 0%, #0e8585 100%);
      box-shadow: 0 4px 20px rgba(14, 133, 133, 0.45);
    }
    #${CHATBOT_IFRAME_ID} {
      position: fixed;
      right: 24px;
      bottom: 92px;
      width: 420px;
      height: 610px;
      max-width: calc(100vw - 32px);
      max-height: calc(100vh - 110px);
      border: none;
      border-radius: 6px;
      box-shadow:
        0 0 0 1px #d0d0d0,
        0 0 60px rgba(0,0,0,0.15),
        0 0 0 2px rgba(14,133,133,0.12);
      transform: scale(0.92) translateY(8px);
      opacity: 0;
      pointer-events: none;
      transform-origin: bottom right;
      transition: transform 0.22s cubic-bezier(0.34, 1.56, 0.64, 1),
                  opacity 0.18s ease;
      z-index: 9998;
    }
    #${CHATBOT_IFRAME_ID}.open {
      transform: scale(1) translateY(0);
      opacity: 1;
      pointer-events: auto;
    }
    @media (max-width: 600px) {
      #${CHATBOT_IFRAME_ID} {
        right: 0;
        bottom: 86px;
        width: 100vw;
        height: calc(100dvh - 86px);
        max-width: 100vw;
        max-height: calc(100dvh - 86px);
        border-radius: 0;
        transform-origin: bottom center;
      }
      #${CHATBOT_IFRAME_ID}.open {
        transform: scale(1) translateY(0);
      }
      #${CHATBOT_TOGGLE_ID} {
        right: 16px;
        bottom: 16px;
      }
    }
  `;
  document.head.appendChild(style);

  // ── Toggle Button ──
  const toggleBtn = document.createElement('button');
  toggleBtn.id = CHATBOT_TOGGLE_ID;
  toggleBtn.setAttribute('aria-label', 'Open chatbot');
  toggleBtn.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
    <line x1="9" y1="10" x2="15" y2="10" stroke-opacity="0.6"/>
    <line x1="9" y1="13" x2="13" y2="13" stroke-opacity="0.4"/>
  </svg>`;
  document.body.appendChild(toggleBtn);

  // ── iframe ──
  const iframe = document.createElement('iframe');
  iframe.id = CHATBOT_IFRAME_ID;
  iframe.title = 'Industrial Chatbot';
  document.body.appendChild(iframe);

  // ── Toggle logic ──
  let isOpen = false;

  const closeIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
  </svg>`;
  const chatIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
    <line x1="9" y1="10" x2="15" y2="10" stroke-opacity="0.6"/>
    <line x1="9" y1="13" x2="13" y2="13" stroke-opacity="0.4"/>
  </svg>`;

  toggleBtn.addEventListener('click', () => {
    isOpen = !isOpen;
    if (isOpen) {
      if (!iframe.src) iframe.src = chatbotUrl;
      iframe.classList.add('open');
      toggleBtn.classList.add('open');
      toggleBtn.innerHTML = closeIcon;
      toggleBtn.setAttribute('aria-label', 'Close chatbot');
    } else {
      closeChatbot();
    }
  });

  // ESC to close
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && isOpen) {
      closeChatbot();
    }
  });

  // Listen for close message from chatbot iframe
  window.addEventListener('message', (e) => {
    if (e.data && e.data.type === 'chatbot-close' && isOpen) {
      closeChatbot();
    }
  });

  function closeChatbot() {
    isOpen = false;
    iframe.classList.remove('open');
    toggleBtn.classList.remove('open');
    toggleBtn.innerHTML = chatIcon;
    toggleBtn.setAttribute('aria-label', 'Open chatbot');
  }
})();

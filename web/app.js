// ==========================================================================
// MyClaw Web Cockpit Logic & Accessibility Harness
// ==========================================================================

document.addEventListener('DOMContentLoaded', () => {
  // DOM Elements
  const fontIncreaseBtn = document.getElementById('font-increase-btn');
  const fontDecreaseBtn = document.getElementById('font-decrease-btn');
  const ttsToggleBtn = document.getElementById('tts-toggle-btn');
  const activeBackendName = document.getElementById('active-backend-name');
  const promptInput = document.getElementById('prompt-input');
  const inputForm = document.getElementById('input-form');
  const transcriptStream = document.getElementById('transcript-stream');
  const transcriptContainer = document.getElementById('transcript-container');
  const routeBtns = document.querySelectorAll('.route-btn');
  const voiceBtn = document.getElementById('voice-dictate-btn');

  // Accessibility State
  let currentFontSize = 18;
  let ttsEnabled = true;
  let currentRouteMode = 'hybrid';

  // 1. Font Resizing for Low Vision
  fontIncreaseBtn.addEventListener('click', () => {
    if (currentFontSize < 28) {
      currentFontSize += 2;
      document.documentElement.style.setProperty('--font-size-base', `${currentFontSize}px`);
      announceAccessibility(`Font size increased to ${currentFontSize} pixels.`);
    }
  });

  fontDecreaseBtn.addEventListener('click', () => {
    if (currentFontSize > 14) {
      currentFontSize -= 2;
      document.documentElement.style.setProperty('--font-size-base', `${currentFontSize}px`);
      announceAccessibility(`Font size decreased to ${currentFontSize} pixels.`);
    }
  });

  // 2. Speech Output / TTS Control
  ttsToggleBtn.addEventListener('click', () => {
    ttsEnabled = !ttsEnabled;
    ttsToggleBtn.textContent = ttsEnabled ? '🔊 Speech: ON' : '🔇 Speech: OFF';
    ttsToggleBtn.setAttribute('aria-label', `Toggle Speech. Currently ${ttsEnabled ? 'On' : 'Off'}`);
    announceAccessibility(`Text to speech ${ttsEnabled ? 'enabled' : 'disabled'}.`);
  });

  function speakText(text) {
    if (!ttsEnabled || !('speechSynthesis' in window)) return;
    window.speechSynthesis.cancel(); // Cancel any ongoing speech
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.rate = 1.0;
    utterance.pitch = 1.0;
    window.speechSynthesis.speak(utterance);
  }

  function announceAccessibility(text) {
    const liveRegion = document.createElement('div');
    liveRegion.setAttribute('aria-live', 'polite');
    liveRegion.className = 'sr-only';
    liveRegion.style.position = 'absolute';
    liveRegion.style.width = '1px';
    liveRegion.style.height = '1px';
    liveRegion.style.overflow = 'hidden';
    liveRegion.textContent = text;
    document.body.appendChild(liveRegion);
    setTimeout(() => liveRegion.remove(), 1000);
  }

  // 3. Routing Mode Selection
  routeBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      routeBtns.forEach(b => {
        b.classList.remove('active');
        b.setAttribute('aria-checked', 'false');
      });
      btn.classList.add('active');
      btn.setAttribute('aria-checked', 'true');

      currentRouteMode = btn.dataset.mode;
      updateBackendBadge(currentRouteMode);
    });
  });

  function updateBackendBadge(mode) {
    if (mode === 'hybrid') {
      activeBackendName.textContent = 'Ollama (GLM-4 9B) + Claude Advisor';
    } else if (mode === 'local-only') {
      activeBackendName.textContent = 'Ollama (GLM-4 9B) - Deskside Airgapped';
    } else if (mode === 'frontier') {
      activeBackendName.textContent = 'Claude 3.7 / Gemini 2.5 Direct';
    }
    announceAccessibility(`Routing mode changed to ${mode}`);
  }

  // 4. Prompt Submission
  inputForm.addEventListener('submit', (e) => {
    e.preventDefault();
    const prompt = promptInput.value.trim();
    if (!prompt) return;

    // Append User Message Card
    appendMessageCard('user', '👤 Ray (User)', prompt);
    promptInput.value = '';

    // Simulate Agent Response
    setTimeout(() => {
      generateResponse(prompt);
    }, 600);
  });

  // Handle Ctrl+Enter to submit
  promptInput.addEventListener('keydown', (e) => {
    if (e.ctrlKey && e.key === 'Enter') {
      inputForm.dispatchEvent(new Event('submit'));
    }
  });

  function appendMessageCard(type, author, content, metaInfo = null) {
    const card = document.createElement('div');
    card.className = `chat-card ${type}-card`;
    const now = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    let metaHtml = '';
    if (metaInfo) {
      metaHtml = `
        <div class="card-meta-tags">
          <span class="meta-tag local">${metaInfo.local}</span>
          ${metaInfo.advisor ? `<span class="meta-tag advisor">${metaInfo.advisor}</span>` : ''}
          <span class="card-timestamp">${now}</span>
        </div>
      `;
    } else {
      metaHtml = `<span class="card-timestamp">${now}</span>`;
    }

    card.innerHTML = `
      <div class="card-header">
        <span class="card-author">${author}</span>
        ${metaHtml}
      </div>
      <div class="card-body">
        <p>${escapeHtml(content)}</p>
      </div>
      ${type === 'assistant' ? `
        <div class="card-footer">
          <button class="action-btn speak-btn" aria-label="Read Message Aloud">🔊 Read Aloud</button>
          <button class="action-btn copy-btn" aria-label="Copy Response Text">📋 Copy Text</button>
          <span class="telemetry-detail">Latency: 120ms • Local Watts: 32W</span>
        </div>
      ` : ''}
    `;

    transcriptStream.appendChild(card);
    transcriptContainer.scrollTop = transcriptContainer.scrollHeight;

    // Attach speak button listener if assistant
    if (type === 'assistant') {
      const speakBtn = card.querySelector('.speak-btn');
      speakBtn.addEventListener('click', () => speakText(content));
      const copyBtn = card.querySelector('.copy-btn');
      copyBtn.addEventListener('click', () => {
        navigator.clipboard.writeText(content);
        announceAccessibility('Response text copied to clipboard.');
      });

      if (ttsEnabled) {
        speakText(content);
      }
    }
  }

  function generateResponse(userPrompt) {
    let responseText = '';
    let meta = { local: 'Local: GLM-4 9B', advisor: 'Advisor: Claude 3.7' };

    if (currentRouteMode === 'local-only') {
      meta = { local: 'Local: GLM-4 9B (Airgapped)', advisor: null };
      responseText = `[Local Execution] Processed query via Ollama (GLM-4 9B). Your request "${userPrompt}" was analyzed on deskside compute with zero network transmission. Session record saved to local disk.`;
    } else if (currentRouteMode === 'frontier') {
      meta = { local: 'Frontier Direct', advisor: 'Claude 3.7' };
      responseText = `[Frontier API Direct] Query processed via Claude 3.7 API. Transcripts and session provenance logged locally to disk at ~/eclipse-workspace/myclaw/runs/.`;
    } else {
      responseText = `[Hybrid Orchestration] Local GLM-4 9B handled initial routing and context retrieval. Claude 3.7 advisor verified technical precision for "${userPrompt}". High token value per watt maintained.`;
    }

    appendMessageCard('assistant', '🐾 MyClaw Harness', responseText, meta);
  }

  // Helper function
  function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  // Attach existing speak buttons
  document.querySelectorAll('.speak-btn').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const cardBody = e.target.closest('.chat-card').querySelector('.card-body').textContent;
      speakText(cardBody);
    });
  });

  // Voice Dictation Mock / Web Speech
  voiceBtn.addEventListener('click', () => {
    if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
      const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
      const recognition = new SpeechRecognition();
      recognition.lang = 'en-US';
      voiceBtn.textContent = '🎙️ Listening...';
      announceAccessibility('Speech dictation active. Speak now.');
      
      recognition.onresult = (event) => {
        const transcript = event.results[0][0].transcript;
        promptInput.value = transcript;
        voiceBtn.textContent = '🎤 Dictate';
        announceAccessibility(`Dictated text: ${transcript}`);
      };

      recognition.onerror = () => {
        voiceBtn.textContent = '🎤 Dictate';
        announceAccessibility('Speech dictation error or timeout.');
      };

      recognition.start();
    } else {
      alert('Speech Recognition API is not supported in this browser environment. Use modern Edge/Chrome for direct voice dictation.');
    }
  });
});

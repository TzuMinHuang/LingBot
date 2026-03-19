// @ts-check
const { test, expect } = require('@playwright/test');

/**
 * SSE mock tests — intercept /api/chat/stream via page.route to simulate
 * backend streaming events and verify the typewriter UI renders correctly
 * with auto-scroll behavior.
 *
 * These tests load the actual chatbot.html page with mocked network calls
 * so no backend is required.
 */

const SESSION_ID = 'ABCDEF123456';
const INTERACTION_ID = 'test-interaction-001';

/**
 * Build an SSE text payload from an array of EventDto-shaped objects.
 * Each event is sent as "event: chat\ndata: {...}\n\n".
 */
function buildSsePayload(events) {
  return events.map(evt =>
    `event: chat\nid: ${evt.interactionId || INTERACTION_ID}\ndata: ${JSON.stringify(evt)}\n\n`
  ).join('');
}

test.describe('SSE Stream Mocking', () => {

  test.beforeEach(async ({ page }) => {
    // Mock the initial session endpoint
    await page.route('**/chat/initial', route =>
      route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify({
          sessionId: SESSION_ID,
          userId: 'test-user',
          history: [],
        }),
      }));

    // Mock suggestions endpoint
    await page.route('**/chat/suggestions*', route =>
      route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify({ suggestions: ['Hello', 'Help me'] }),
      }));

    // Mock env config
    await page.route('**/config/env.json', route =>
      route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify({
          ui: { title: 'Test Bot', locale: 'zh-TW', maxInputLength: 500, streamSpeed: 5, idleTimeoutMs: 300000, idleSuggestMs: 240000 },
          allowedDomains: ['localhost', '127.0.0.1'],
          hostEnvMap: { localhost: 'dev' },
          envMap: { dev: { apiBase: '', chatbotUrl: '/pages/chatbot.html', authSsoUrl: '' } },
        }),
      }));

    // Mock queue position
    await page.route('**/chat/*/queue-position', route =>
      route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify({ position: 0 }),
      }));

    // Mock events endpoint
    await page.route('**/chat/*/events', route => {
      if (route.request().method() === 'GET') {
        return route.fulfill({
          contentType: 'application/json',
          body: JSON.stringify([]),
        });
      }
      return route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify({ status: 'recorded' }),
      });
    });

    // Mock history endpoint
    await page.route('**/chat/*/history', route =>
      route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify({ userId: 'test-user', messages: [] }),
      }));
  });

  test('typewriter renders streaming chunks in order', async ({ page }) => {
    const chunks = ['Hello', ', this', ' is a', ' test', ' response.'];

    // Mock send endpoint — returns interactionId
    await page.route('**/chat/*/send', route =>
      route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify({ interactionId: INTERACTION_ID }),
      }));

    // Mock the SSE stream with delayed chunks
    await page.route(`**/chat/${SESSION_ID}/stream`, route => {
      const events = [
        { sessionId: SESSION_ID, interactionId: INTERACTION_ID, type: 'PROCESSING_START', payload: null },
        ...chunks.map(text => ({
          sessionId: SESSION_ID,
          interactionId: INTERACTION_ID,
          type: 'STREAM_CHUNK',
          payload: { content: text },
        })),
        { sessionId: SESSION_ID, interactionId: INTERACTION_ID, type: 'STREAM_END', payload: { sources: [] } },
      ];

      route.fulfill({
        status: 200,
        headers: {
          'Content-Type': 'text/event-stream',
          'Cache-Control': 'no-cache',
          'Connection': 'keep-alive',
        },
        body: buildSsePayload(events),
      });
    });

    await page.goto('/pages/chatbot.html');
    await page.waitForLoadState('networkidle');

    // Type a message and send
    const textarea = page.locator('.chat-input textarea');
    await textarea.fill('test question');
    await page.keyboard.press('Enter');

    // Wait for the bot response to appear in the chatbox
    const chatbox = page.locator('.chatbox');
    await expect(chatbox).toContainText('Hello, this is a test response.', { timeout: 15000 });
  });

  test('chatbox auto-scrolls to bottom during streaming', async ({ page }) => {
    // Build a long response to trigger scroll
    const longChunks = Array.from({ length: 50 }, (_, i) => `Line ${i + 1} of the response. `);

    await page.route('**/chat/*/send', route =>
      route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify({ interactionId: INTERACTION_ID }),
      }));

    await page.route(`**/chat/${SESSION_ID}/stream`, route => {
      const events = [
        { sessionId: SESSION_ID, interactionId: INTERACTION_ID, type: 'PROCESSING_START', payload: null },
        ...longChunks.map(text => ({
          sessionId: SESSION_ID,
          interactionId: INTERACTION_ID,
          type: 'STREAM_CHUNK',
          payload: { content: text },
        })),
        { sessionId: SESSION_ID, interactionId: INTERACTION_ID, type: 'STREAM_END', payload: { sources: [] } },
      ];

      route.fulfill({
        status: 200,
        headers: { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache' },
        body: buildSsePayload(events),
      });
    });

    await page.goto('/pages/chatbot.html');
    await page.waitForLoadState('networkidle');

    const textarea = page.locator('.chat-input textarea');
    await textarea.fill('give me a long answer');
    await page.keyboard.press('Enter');

    // Wait for streaming to complete
    await expect(page.locator('.chatbox')).toContainText('Line 50', { timeout: 30000 });

    // Verify chatbox is scrolled to the bottom (scrollTop + clientHeight ~= scrollHeight)
    const isScrolledToBottom = await page.locator('.chatbox').evaluate(el => {
      const tolerance = 50;
      return (el.scrollTop + el.clientHeight) >= (el.scrollHeight - tolerance);
    });
    expect(isScrolledToBottom).toBe(true);
  });
});

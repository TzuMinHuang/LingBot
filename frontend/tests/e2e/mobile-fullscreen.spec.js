// @ts-check
const { test, expect } = require('@playwright/test');

/**
 * Mobile fullscreen tests — simulate a 375x812 viewport (iPhone-class)
 * and verify the widget expands to 100dvh when opened.
 */

const DUMMY_HOST_HTML = `
<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Host</title></head>
<body>
  <h1>Host</h1>
  <script src="/widgets/chatbot-widget.js"></script>
</body>
</html>`;

const CHATBOT_STUB_HTML = `
<!DOCTYPE html>
<html><head><meta charset="UTF-8"></head>
<body><div class="chatbot"><h2>Industrial AI</h2></div></body>
</html>`;

test.describe('Mobile Fullscreen Behavior', () => {

  test.use({ viewport: { width: 375, height: 812 } });

  test.beforeEach(async ({ page }) => {
    await page.route('**/host.html', route =>
      route.fulfill({ contentType: 'text/html', body: DUMMY_HOST_HTML }));

    await page.route('**/config/env.json', route =>
      route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify({
          allowedDomains: ['localhost', '127.0.0.1'],
          hostEnvMap: { localhost: 'dev' },
          envMap: { dev: { apiBase: '', chatbotUrl: '/pages/chatbot.html' } },
        }),
      }));

    await page.route('**/pages/chatbot.html', route =>
      route.fulfill({ contentType: 'text/html', body: CHATBOT_STUB_HTML }));

    await page.goto('/host.html');
  });

  test('toggle button is visible on mobile viewport', async ({ page }) => {
    const toggle = page.locator('#chatbot-plugin-toggle');
    await expect(toggle).toBeVisible();
  });

  test('iframe expands to full viewport on mobile when opened', async ({ page }) => {
    const toggle = page.locator('#chatbot-plugin-toggle');
    const iframe = page.locator('#chatbot-plugin-iframe');

    await toggle.click();
    await expect(iframe).toHaveClass(/open/);

    // On mobile (<=600px) the CSS sets width:100vw, height:100dvh
    const box = await iframe.boundingBox();
    expect(box).toBeTruthy();
    expect(box.width).toBeCloseTo(375, -1);
    // Height should fill the viewport (allow small tolerance for browser chrome)
    expect(box.height).toBeGreaterThan(750);
  });

  test('toggle button hides when iframe is open on mobile', async ({ page }) => {
    const toggle = page.locator('#chatbot-plugin-toggle');

    await toggle.click();

    // CSS rule: #chatbot-plugin-toggle.open { opacity: 0; pointer-events: none; }
    await expect(toggle).toHaveClass(/open/);
    await expect(toggle).toHaveCSS('opacity', '0');
    await expect(toggle).toHaveCSS('pointer-events', 'none');
  });

  test('iframe has no border-radius on mobile (edge-to-edge)', async ({ page }) => {
    const toggle = page.locator('#chatbot-plugin-toggle');
    const iframe = page.locator('#chatbot-plugin-iframe');

    await toggle.click();
    await expect(iframe).toHaveClass(/open/);

    // CSS rule: border-radius: 0 on mobile
    await expect(iframe).toHaveCSS('border-radius', '0px');
  });
});

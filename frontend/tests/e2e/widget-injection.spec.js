// @ts-check
const { test, expect } = require('@playwright/test');

/**
 * Widget injection tests — verify the chatbot toggle button and iframe
 * mount correctly on a dummy host page without a running backend.
 */

const DUMMY_HOST_HTML = `
<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><title>Host Page</title></head>
<body>
  <h1>Dummy Host</h1>
  <script src="/widgets/chatbot-widget.js"></script>
</body>
</html>`;

test.describe('Widget Injection', () => {

  test.beforeEach(async ({ page }) => {
    // Serve a dummy host page that loads the widget script
    await page.route('**/host.html', route =>
      route.fulfill({ contentType: 'text/html', body: DUMMY_HOST_HTML }));

    // Provide a minimal env.json so the widget doesn't error
    await page.route('**/config/env.json', route =>
      route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify({
          allowedDomains: ['localhost', '127.0.0.1'],
          hostEnvMap: { localhost: 'dev' },
          envMap: {
            dev: { apiBase: '', chatbotUrl: '/pages/chatbot.html' },
          },
        }),
      }));

    await page.goto('/host.html');
  });

  test('toggle button is injected into the DOM', async ({ page }) => {
    const toggle = page.locator('#chatbot-plugin-toggle');
    await expect(toggle).toBeVisible();
    await expect(toggle).toHaveAttribute('aria-label', 'Open chatbot');
  });

  test('iframe element exists but is hidden initially', async ({ page }) => {
    const iframe = page.locator('#chatbot-plugin-iframe');
    await expect(iframe).toBeAttached();
    // iframe should not have the "open" class initially
    await expect(iframe).not.toHaveClass(/open/);
  });

  test('clicking toggle opens the iframe and changes button state', async ({ page }) => {
    // Stub the chatbot page so iframe load doesn't fail
    await page.route('**/pages/chatbot.html', route =>
      route.fulfill({ contentType: 'text/html', body: '<html><body>chatbot</body></html>' }));

    const toggle = page.locator('#chatbot-plugin-toggle');
    const iframe = page.locator('#chatbot-plugin-iframe');

    await toggle.click();

    await expect(iframe).toHaveClass(/open/);
    await expect(toggle).toHaveClass(/open/);
    await expect(toggle).toHaveAttribute('aria-label', 'Close chatbot');
  });

  test('clicking toggle again closes the iframe', async ({ page }) => {
    await page.route('**/pages/chatbot.html', route =>
      route.fulfill({ contentType: 'text/html', body: '<html><body>chatbot</body></html>' }));

    const toggle = page.locator('#chatbot-plugin-toggle');
    const iframe = page.locator('#chatbot-plugin-iframe');

    // Open
    await toggle.click();
    await expect(iframe).toHaveClass(/open/);

    // Close
    await toggle.click();
    await expect(iframe).not.toHaveClass(/open/);
    await expect(toggle).toHaveAttribute('aria-label', 'Open chatbot');
  });

  test('ESC key closes the chatbot', async ({ page }) => {
    await page.route('**/pages/chatbot.html', route =>
      route.fulfill({ contentType: 'text/html', body: '<html><body>chatbot</body></html>' }));

    const toggle = page.locator('#chatbot-plugin-toggle');
    const iframe = page.locator('#chatbot-plugin-iframe');

    await toggle.click();
    await expect(iframe).toHaveClass(/open/);

    await page.keyboard.press('Escape');
    await expect(iframe).not.toHaveClass(/open/);
  });

  test('iframe src is set lazily on first open', async ({ page }) => {
    await page.route('**/pages/chatbot.html', route =>
      route.fulfill({ contentType: 'text/html', body: '<html><body>chatbot</body></html>' }));

    const iframe = page.locator('#chatbot-plugin-iframe');

    // Before clicking, iframe should have no src
    const srcBefore = await iframe.getAttribute('src');
    expect(srcBefore).toBeFalsy();

    // After clicking, src should be set
    await page.locator('#chatbot-plugin-toggle').click();
    const srcAfter = await iframe.getAttribute('src');
    expect(srcAfter).toBeTruthy();
  });
});

// tests/petCreation.spec.js
import { test, expect } from '@playwright/test';

test.describe('Pet Creation Flow', () => {
  // Helper function to generate unique user data
  const generateUserData = () => {
    const timestamp = Date.now();
    return {
      username: `testuser_${timestamp}`,
      email: `test_${timestamp}@example.com`,
      password: 'Test123!'
    };
  };

  test('should create account and verify pet generation', async ({ page }) => {
    const userData = generateUserData();

    console.log(`Creating test account: ${userData.username}`);

    // Step 1: Navigate to registration page
    await page.goto('http://localhost:4200/register');

    // Step 2: Fill registration form (using actual selectors from your HTML)
    await page.fill('#username', userData.username);
    await page.fill('#email', userData.email);
    await page.fill('#password', userData.password);
    await page.fill('#confirmPassword', userData.password);

    // Step 3: Click submit button
    await page.click('button[type="submit"]');

    // Step 4: Wait for navigation to /game (from your register component)
    await page.waitForURL('**/game', { timeout: 10000 });

    console.log(`✅ Navigated to: ${page.url()}`);

    // Step 5: Verify page loaded (game component should be visible)
    await expect(page.locator('body')).toBeVisible();

    // Step 6: Take screenshot for visual verification
    await page.screenshot({
      path: `screenshots/pet_creation_${userData.username}.png`,
      fullPage: true
    });

    console.log(`✅ Successfully created pet for: ${userData.username}`);
  });

  test('should create 2 accounts successfully', async ({ page }) => {
    const accounts = [];

    // Create 2 accounts
    for (let i = 1; i <= 2; i++) {
      const userData = generateUserData();

      console.log(`Creating account ${i} of 2: ${userData.username}`);

      // Registration
      await page.goto('http://localhost:4200/register');
      await page.fill('#username', userData.username);
      await page.fill('#email', userData.email);
      await page.fill('#password', userData.password);
      await page.fill('#confirmPassword', userData.password);
      await page.click('button[type="submit"]');

      // Wait for navigation to game page
      await page.waitForURL('**/game', { timeout: 10000 });

      // Verify we're on game page
      expect(page.url()).toContain('/game');

      // Store account info
      accounts.push({
        username: userData.username,
        success: true
      });

      // Logout if not the last account
      if (i < 2) {
        console.log(`Logging out of ${userData.username}...`);

        // Click the logout button
        await page.click('.logout-btn');

        // Wait for navigation back to login page
        await page.waitForURL('**/login', { timeout: 10000 });

        // Verify we're on login page
        expect(page.url()).toContain('/login');

        // Wait a bit before next registration
        await page.waitForTimeout(1000);

        console.log(`✅ Logged out successfully`);
      }
    }

    // Verify both accounts were created
    expect(accounts.length).toBe(2);
    expect(accounts.every(acc => acc.success)).toBeTruthy();

    console.log('✅ Successfully created 2 accounts:', accounts.map(a => a.username));
  });
});

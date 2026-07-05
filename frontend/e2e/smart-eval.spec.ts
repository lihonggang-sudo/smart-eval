import { expect, test } from '@playwright/test';

test('renders admin and mobile login routes', async ({ page }) => {
  await page.goto('/admin/login');

  await expect(page).toHaveTitle('Smart Eval');
  await expect(page.getByRole('heading', { name: '管理员登录' })).toBeVisible();

  await page.goto('/mobile/login');

  await expect(page.getByLabel('账号')).toBeVisible();
  await expect(page.getByLabel('密码')).toBeVisible();
});

test('mobile login page fits narrow mobile viewports without horizontal overflow', async ({
  page
}) => {
  for (const width of [390, 320]) {
    await page.setViewportSize({ width, height: 844 });
    await page.goto('/mobile/login');

    const metrics = await page.evaluate(() => ({
      bodyClientWidth: document.body.clientWidth,
      bodyScrollWidth: document.body.scrollWidth,
      documentClientWidth: document.documentElement.clientWidth,
      documentScrollWidth: document.documentElement.scrollWidth
    }));

    expect(metrics.bodyScrollWidth).toBeLessThanOrEqual(metrics.bodyClientWidth);
    expect(metrics.documentScrollWidth).toBeLessThanOrEqual(metrics.documentClientWidth);
  }
});

test('mobile login action area sits near the card bottom on tall phones', async ({ page }) => {
  await page.setViewportSize({ width: 400, height: 896 });
  await page.goto('/mobile/login');

  const cardBox = await page.getByTestId('mobile-login-card').boundingBox();
  const actionsBox = await page.getByTestId('mobile-login-actions').boundingBox();

  expect(cardBox).not.toBeNull();
  expect(actionsBox).not.toBeNull();
  expect(actionsBox!.y).toBeGreaterThan(cardBox!.y + cardBox!.height * 0.65);
  expect(actionsBox!.y + actionsBox!.height).toBeLessThanOrEqual(cardBox!.y + cardBox!.height);
});

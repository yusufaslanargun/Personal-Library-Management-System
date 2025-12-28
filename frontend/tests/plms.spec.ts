import { test, expect } from '@playwright/test'

const pastDate = new Date(Date.now() - 5 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)

test('PLMS smoke flow', async ({ page }) => {
  await page.goto('/')
  const email = `user-${Date.now()}@example.com`
  await page.getByRole('button', { name: 'Register' }).click()
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill('password123')
  await page.getByLabel('Display Name').fill('Tester')
  await page.getByRole('button', { name: 'Create Account' }).click()

  await page.getByText('Add').click()

  await page.getByPlaceholder('ISBN or barcode').fill('9780000000000')
  await page.getByRole('button', { name: 'Lookup' }).click()
  await page.waitForSelector('.candidate')
  await page.locator('.candidate button').first().click()
  const createdBanner = page.locator('.banner.success')
  await expect(createdBanner).toBeVisible()
  const bannerText = await createdBanner.textContent()
  const itemId = bannerText?.match(/#(\d+)/)?.[1]
  expect(itemId).toBeTruthy()

  const manualCard = page.locator('.card', { hasText: 'Manual Entry' })
  await manualCard.getByLabel('Title').fill('Manual Book')
  await manualCard.getByLabel('Year').fill('2022')
  await manualCard.getByLabel('Authors').fill('Author One')
  await manualCard.getByRole('button', { name: 'Create Item' }).click()
  await expect(createdBanner).toContainText('Manual Book')
  const manualText = await createdBanner.textContent()
  const manualId = manualText?.match(/#(\d+)/)?.[1]
  expect(manualId).toBeTruthy()

  await manualCard.getByLabel('Type').selectOption('DVD')
  await manualCard.getByLabel('Title').fill('Manual DVD')
  await manualCard.getByLabel('Year').fill('2021')
  await manualCard.getByLabel('Runtime (min)').fill('120')
  await manualCard.getByLabel('Director').fill('Director One')
  await manualCard.getByLabel('Cast').fill('Actor One')
  await manualCard.getByRole('button', { name: 'Create Item' }).click()
  await expect(createdBanner).toContainText('Manual DVD')
  const manualDvdText = await createdBanner.textContent()
  const manualDvdId = manualDvdText?.match(/#(\d+)/)?.[1]
  expect(manualDvdId).toBeTruthy()

  await page.getByText('Search').click()
  await page.getByPlaceholder('Title, author, keyword').fill('Mock Book')
  await page.waitForSelector('.result-card')
  await page.locator('.result-card').first().click()

  await page.getByLabel('Page / Minute').fill('10')
  await page.getByRole('button', { name: 'Log Progress' }).click()
  await expect(page.getByText('10 (', { exact: false })).toBeVisible()

  await page.getByLabel('To Whom').fill('Alex')
  await page.getByLabel('Start Date').fill(pastDate)
  await page.getByLabel('Due Date').fill(pastDate)
  await page.getByRole('button', { name: 'Mark as Loaned' }).click()
  await expect(page.getByText('Loaned to')).toBeVisible()

  await page.getByText('Dashboard').click()
  await expect(page.getByText('Overdue')).toBeVisible()

  await page.getByText('Lists').click()
  await page.getByPlaceholder('List name').fill('Reading Queue')
  await page.getByRole('button', { name: 'Create' }).click()
  await page.getByText('Reading Queue').click()
  await page.getByPlaceholder('Item ID').fill(itemId || '')
  await page.getByRole('button', { name: 'Add Item' }).click()
  await page.getByPlaceholder('Item ID').fill(manualId || '')
  await page.getByRole('button', { name: 'Add Item' }).click()
  await page.locator('.actions button', { hasText: 'Up' }).last().click()
  page.once('dialog', (dialog) => dialog.accept())
  await page.getByRole('button', { name: 'Delete' }).click()
  await expect(page.getByText('Reading Queue')).toHaveCount(0)

  await page.getByText('Settings').click()
  const [download] = await Promise.all([
    page.waitForEvent('download'),
    page.getByRole('button', { name: 'Download' }).click()
  ])
  const path = await download.path()
  if (path) {
    await page.setInputFiles('input[type="file"]', path)
    await expect(page.getByText('Added:', { exact: false })).toBeVisible()
  }

  await page.getByText('Search').click()
  await page.getByLabel('Query').fill('')
  await page.getByLabel('Author').fill('Author One')
  await page.waitForSelector('.result-card')
  await expect(page.getByText('Manual Book')).toBeVisible()
  await page.getByLabel('Author').fill('')
  await page.getByLabel('Cast').fill('Actor One')
  await page.waitForSelector('.result-card')
  await expect(page.getByText('Manual DVD')).toBeVisible()
})

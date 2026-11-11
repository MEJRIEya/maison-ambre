// Prices arrive as { amount, currency } and are shown in the boutique's own currency.
export function formatMoney(money) {
  if (!money) return '';
  return new Intl.NumberFormat('en-GB', { style: 'currency', currency: money.currency }).format(money.amount);
}

// Cart total shown before checkout. Computed in cents to avoid floating-point errors.
// It is only an estimate: the API computes the real total from current prices.
export function cartTotal(lines) {
  if (lines.length === 0) return null;
  const cents = lines.reduce((sum, line) => sum + Math.round(line.unitPrice.amount * 100) * line.quantity, 0);
  return { amount: cents / 100, currency: lines[0].unitPrice.currency };
}

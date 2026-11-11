// All calls go to /api on the same origin: Vite forwards them in development, nginx in Docker.
async function request(path, options = {}) {
  const response = await fetch(`/api${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  const body = await response.json().catch(() => null);

  if (!response.ok) {
    // Errors come back as problem details: { status, detail, code, errors? }
    const error = new Error(body?.detail ?? `Request failed with status ${response.status}`);
    error.status = response.status;
    error.code = body?.code;
    throw error;
  }
  return body;
}

export const api = {
  boutiques: () => request('/boutiques'),
  products: (boutiqueId) => request(`/boutiques/${boutiqueId}/products`),
  product: (boutiqueId, productId) => request(`/boutiques/${boutiqueId}/products/${productId}`),
  identify: (email, boutiqueId) =>
    request('/customers', { method: 'POST', body: JSON.stringify({ email, boutiqueId }) }),
  customer: (customerId) => request(`/customers/${customerId}`),
  placeOrder: (order) => request('/orders', { method: 'POST', body: JSON.stringify(order) }),
  insights: (boutiqueId) => request(`/boutiques/${boutiqueId}/insights`),
};

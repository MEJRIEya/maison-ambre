import { createContext, useContext, useEffect, useState } from 'react';
import { api } from './api.js';

// Everything the shop remembers across page reloads lives in localStorage:
// the selected boutique, one cart per boutique, and the identified customer.
const STORAGE_KEYS = {
  boutiqueId: 'maisonAmbre.boutiqueId',
  carts: 'maisonAmbre.carts',
  customer: 'maisonAmbre.customer',
};

function load(key, fallback) {
  try {
    const value = localStorage.getItem(key);
    return value ? JSON.parse(value) : fallback;
  } catch {
    return fallback;
  }
}

function save(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Storage can be unavailable (private mode): the shop still works, it just forgets on reload.
  }
}

const ShopContext = createContext(null);

export function ShopProvider({ children }) {
  const [boutiques, setBoutiques] = useState([]);
  const [boutiqueId, setBoutiqueId] = useState(() => load(STORAGE_KEYS.boutiqueId, null));
  // { [boutiqueId]: [{ boutiqueId, productId, name, unitPrice, quantity, maxQuantity }] }
  const [carts, setCarts] = useState(() => load(STORAGE_KEYS.carts, {}));
  const [customer, setCustomer] = useState(() => load(STORAGE_KEYS.customer, null));
  const [loadError, setLoadError] = useState(null);

  useEffect(() => {
    api
      .boutiques()
      .then((list) => {
        setBoutiques(list);
        // Keep the saved boutique if it still exists, otherwise start with the first one.
        setBoutiqueId((current) => (list.some((b) => b.id === current) ? current : (list[0]?.id ?? null)));
      })
      .catch((error) => setLoadError(error.message));
  }, []);

  useEffect(() => save(STORAGE_KEYS.boutiqueId, boutiqueId), [boutiqueId]);
  useEffect(() => save(STORAGE_KEYS.carts, carts), [carts]);
  useEffect(() => save(STORAGE_KEYS.customer, customer), [customer]);

  const boutique = boutiques.find((b) => b.id === boutiqueId) ?? null;

  // Cart decision: each boutique keeps its own cart. Switching boutique shows that boutique's cart
  // and keeps the others untouched, so nothing is lost and a cart can never mix boutiques.
  const cart = carts[boutiqueId] ?? [];

  function updateCart(update) {
    setCarts((all) => {
      const lines = update(all[boutiqueId] ?? []);
      const next = { ...all };
      if (lines.length > 0) next[boutiqueId] = lines;
      else delete next[boutiqueId];
      return next;
    });
  }

  function addToCart(product, quantity = 1) {
    updateCart((lines) => {
      const existing = lines.find((line) => line.productId === product.id);
      const line = {
        boutiqueId,
        productId: product.id,
        name: product.name,
        unitPrice: product.price,
        quantity: Math.min((existing?.quantity ?? 0) + quantity, product.stock),
        maxQuantity: product.stock,
      };
      return existing ? lines.map((l) => (l.productId === product.id ? line : l)) : [...lines, line];
    });
  }

  function setQuantity(productId, quantity) {
    // Between 1 and the stock shown in the catalogue. The API still checks the real stock at checkout.
    updateCart((lines) =>
      lines.map((line) =>
        line.productId === productId ? { ...line, quantity: Math.max(1, Math.min(quantity, line.maxQuantity)) } : line,
      ),
    );
  }

  function removeFromCart(productId) {
    updateCart((lines) => lines.filter((line) => line.productId !== productId));
  }

  function clearCart() {
    updateCart(() => []);
  }

  const value = {
    boutiques,
    boutique,
    boutiqueId,
    setBoutiqueId,
    carts,
    cart,
    addToCart,
    setQuantity,
    removeFromCart,
    clearCart,
    customer,
    setCustomer,
    loadError,
  };
  return <ShopContext.Provider value={value}>{children}</ShopContext.Provider>;
}

export function useShop() {
  return useContext(ShopContext);
}

import { useEffect, useState } from 'react';
import { api } from '../api.js';
import { formatMoney } from '../money.js';
import { useShop } from '../ShopContext.jsx';

export function availabilityLabel(product) {
  if (!product.available) return 'Out of stock';
  if (product.stock <= 5) return `Only ${product.stock} left`;
  return 'In stock';
}

export default function CataloguePage() {
  const { boutique, cart, addToCart } = useShop();
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    // "ignore" drops a late response if the user switched boutique in the meantime.
    let ignore = false;
    setLoading(true);
    setError(null);
    api
      .products(boutique.id)
      .then((list) => !ignore && setProducts(list))
      .catch((e) => !ignore && setError(e.message))
      .finally(() => !ignore && setLoading(false));
    return () => {
      ignore = true;
    };
  }, [boutique.id]);

  const inCart = (productId) => cart.find((line) => line.productId === productId)?.quantity ?? 0;

  return (
    <section>
      <h1>{boutique.name}</h1>
      <p className="muted">Prices in {boutique.currency}. Stock and prices are specific to this boutique.</p>

      {error && <p className="alert error">{error}</p>}
      {loading && <p className="muted">Loading products…</p>}

      <div className="grid">
        {products.map((product) => {
          const full = inCart(product.id) >= product.stock;
          return (
            <article key={product.id} className="card">
              <span className="tag">{product.family}</span>
              <h2>{product.name}</h2>
              <p className="muted">{product.sizeMl} ml</p>
              <p className="price">{formatMoney(product.price)}</p>
              <p className={product.available ? 'stock' : 'stock out'}>{availabilityLabel(product)}</p>
              <button disabled={!product.available || full} onClick={() => addToCart(product)}>
                {full && product.available ? 'All in your cart' : 'Add to cart'}
              </button>
            </article>
          );
        })}
      </div>
    </section>
  );
}

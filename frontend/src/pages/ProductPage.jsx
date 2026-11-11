import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { formatMoney } from '../money.js';
import { useShop } from '../ShopContext.jsx';
import { availabilityLabel } from './CataloguePage.jsx';

export default function ProductPage() {
  const { productId } = useParams();
  const { boutique, cart, addToCart } = useShop();
  const [product, setProduct] = useState(null);
  const [error, setError] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [added, setAdded] = useState(false);

  // Reloads when the boutique changes: same product, local price and stock.
  useEffect(() => {
    let ignore = false;
    setProduct(null);
    setError(null);
    setAdded(false);
    api
      .product(boutique.id, productId)
      .then((p) => !ignore && setProduct(p))
      .catch((e) => !ignore && setError(e.status === 404 ? `This product is not sold in ${boutique.city}.` : e.message));
    return () => {
      ignore = true;
    };
  }, [boutique.id, boutique.city, productId]);

  if (error) {
    return (
      <section>
        <p className="alert error">{error}</p>
        <Link to="/">Back to the catalogue</Link>
      </section>
    );
  }
  if (!product) return <p className="muted">Loading…</p>;

  const alreadyInCart = cart.find((line) => line.productId === product.id)?.quantity ?? 0;
  const canAdd = Math.max(0, product.stock - alreadyInCart);

  return (
    <section className="product">
      <Link to="/">← Catalogue</Link>
      <span className="tag">{product.family}</span>
      <h1>{product.name}</h1>
      <p className="muted">
        {product.sizeMl} ml · SKU {product.sku}
      </p>
      <p>{product.description}</p>
      <p className="price">{formatMoney(product.price)}</p>
      <p className={product.available ? 'stock' : 'stock out'}>
        {availabilityLabel(product)} in {boutique.city}
      </p>

      {canAdd > 0 && (
        <div className="row">
          <input
            className="qty"
            type="number"
            min="1"
            max={canAdd}
            value={quantity}
            onChange={(event) => setQuantity(Math.max(1, Math.min(canAdd, Number(event.target.value) || 1)))}
          />
          <button
            onClick={() => {
              addToCart(product, quantity);
              setAdded(true);
            }}
          >
            Add to cart
          </button>
        </div>
      )}
      {product.available && canAdd === 0 && <p className="muted">All available bottles are in your cart.</p>}
      {added && (
        <p className="alert success">
          Added. <Link to="/cart">Go to cart</Link>
        </p>
      )}
    </section>
  );
}

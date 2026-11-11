import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { cartTotal, formatMoney } from '../money.js';
import { useShop } from '../ShopContext.jsx';

function EmailStep() {
  const { boutique, customer, setCustomer } = useShop();
  const [email, setEmail] = useState('');
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  if (customer) {
    return (
      <p>
        Ordering as <strong>{customer.email}</strong> ({customer.status.toLowerCase()}).{' '}
        <button className="link" onClick={() => setCustomer(null)}>
          Not you?
        </button>
      </p>
    );
  }

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      // Creates a prospect (first seen in this boutique) or returns the existing prospect or client.
      setCustomer(await api.identify(email, boutique.id));
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="email-form" onSubmit={submit}>
      <label htmlFor="email">Your email (no password needed)</label>
      <div className="row">
        <input
          id="email"
          type="email"
          required
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          placeholder="you@example.com"
        />
        <button type="submit" disabled={busy}>
          Continue
        </button>
      </div>
      {error && <p className="alert error">{error}</p>}
    </form>
  );
}

function OtherCarts() {
  const { boutiques, boutiqueId, carts, setBoutiqueId } = useShop();
  const others = boutiques.filter((b) => b.id !== boutiqueId && carts[b.id]?.length);
  if (others.length === 0) return null;
  return (
    <p className="muted">
      Each boutique keeps its own cart. You also have items in:{' '}
      {others.map((b) => (
        <button key={b.id} className="link" onClick={() => setBoutiqueId(b.id)}>
          {b.city}
        </button>
      ))}
    </p>
  );
}

export default function CartPage() {
  const { boutique, cart, customer, setCustomer, setQuantity, removeFromCart, clearCart } = useShop();
  const [placing, setPlacing] = useState(false);
  const [error, setError] = useState(null);
  const [confirmation, setConfirmation] = useState(null);

  async function placeOrder() {
    setPlacing(true);
    setError(null);
    try {
      const order = await api.placeOrder({
        boutiqueId: boutique.id,
        customerId: customer.id,
        lines: cart.map((line) => ({ boutiqueId: line.boutiqueId, productId: line.productId, quantity: line.quantity })),
      });
      const wasProspect = customer.status === 'PROSPECT';
      const updatedCustomer = await api.customer(customer.id);
      setCustomer(updatedCustomer);
      clearCart();
      setConfirmation({ order, becameClient: wasProspect && updatedCustomer.status === 'CLIENT' });
    } catch (e) {
      setError(e.message);
    } finally {
      setPlacing(false);
    }
  }

  if (confirmation) {
    const { order, becameClient } = confirmation;
    return (
      <section>
        <h1>Thank you!</h1>
        <p className="alert success">
          Order #{order.id} is confirmed: {formatMoney(order.total)}.
          {becameClient && ' Welcome, you are now a Maison Ambre client.'}
        </p>
        <button onClick={() => setConfirmation(null)}>Back to cart</button>{' '}
        <Link to="/">Continue shopping</Link>
      </section>
    );
  }

  const total = cartTotal(cart);

  return (
    <section>
      <h1>Your {boutique.city} cart</h1>
      <OtherCarts />

      {cart.length === 0 ? (
        <p>
          Your cart is empty. <Link to="/">Browse the catalogue</Link>
        </p>
      ) : (
        <>
          <table className="table">
            <thead>
              <tr>
                <th>Product</th>
                <th>Unit price</th>
                <th>Quantity</th>
                <th className="num">Total</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {cart.map((line) => (
                <tr key={line.productId}>
                  <td>{line.name}</td>
                  <td>{formatMoney(line.unitPrice)}</td>
                  <td>
                    <input
                      className="qty"
                      type="number"
                      min="1"
                      max={line.maxQuantity}
                      value={line.quantity}
                      onChange={(event) => {
                        // Ignore an empty field while typing; "Remove" is the way to delete a line.
                        if (event.target.value !== '') setQuantity(line.productId, Number(event.target.value));
                      }}
                    />
                  </td>
                  <td className="num">
                    {formatMoney({ amount: line.unitPrice.amount * line.quantity, currency: line.unitPrice.currency })}
                  </td>
                  <td>
                    <button className="link" onClick={() => removeFromCart(line.productId)}>
                      Remove
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <td colSpan="3">Total</td>
                <td className="num">
                  <strong>{formatMoney(total)}</strong>
                </td>
                <td />
              </tr>
            </tfoot>
          </table>

          <div className="checkout">
            <h2>Checkout</h2>
            <EmailStep />
            {error && <p className="alert error">{error}</p>}
            <button disabled={!customer || placing} onClick={placeOrder}>
              {placing ? 'Placing order…' : `Place order (${formatMoney(total)})`}
            </button>{' '}
            <button className="link" onClick={clearCart}>
              Empty cart
            </button>
          </div>
        </>
      )}
    </section>
  );
}

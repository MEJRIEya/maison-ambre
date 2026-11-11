import { NavLink, Route, Routes } from 'react-router-dom';
import { useShop } from './ShopContext.jsx';
import CataloguePage from './pages/CataloguePage.jsx';
import CartPage from './pages/CartPage.jsx';

function BoutiqueSwitcher() {
  const { boutiques, boutiqueId, setBoutiqueId } = useShop();
  return (
    <label className="switcher">
      <span>Boutique</span>
      <select value={boutiqueId ?? ''} onChange={(event) => setBoutiqueId(Number(event.target.value))}>
        {boutiques.map((b) => (
          <option key={b.id} value={b.id}>
            {b.city} ({b.currency})
          </option>
        ))}
      </select>
    </label>
  );
}

export default function App() {
  const { boutique, cart, loadError } = useShop();
  const itemCount = cart.reduce((sum, line) => sum + line.quantity, 0);

  return (
    <>
      <header className="header">
        <div className="header-inner">
          <NavLink to="/" className="brand">
            Maison Ambre
          </NavLink>
          <nav className="nav">
            <NavLink to="/" end>
              Catalogue
            </NavLink>
            <NavLink to="/cart">Cart ({itemCount})</NavLink>
          </nav>
          <BoutiqueSwitcher />
        </div>
      </header>

      <main className="main">
        {loadError && <p className="alert error">Cannot reach the API: {loadError}</p>}
        {!boutique && !loadError && <p className="muted">Loading boutiques…</p>}
        {boutique && (
          <Routes>
            <Route path="/" element={<CataloguePage />} />
            <Route path="/cart" element={<CartPage />} />
            <Route path="*" element={<p>Page not found.</p>} />
          </Routes>
        )}
      </main>
    </>
  );
}

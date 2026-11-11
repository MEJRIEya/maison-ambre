import { useEffect, useState } from 'react';
import { api } from '../api.js';
import { formatMoney } from '../money.js';
import { useShop } from '../ShopContext.jsx';

function Kpi({ label, value, hint }) {
  return (
    <div className="kpi">
      <div className="kpi-label">{label}</div>
      <div className="kpi-value">{value}</div>
      {hint && <div className="kpi-hint">{hint}</div>}
    </div>
  );
}

export default function InsightsPage() {
  const { boutique } = useShop();
  const [insights, setInsights] = useState(null);
  const [error, setError] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    let ignore = false;
    setError(null);
    api
      .insights(boutique.id)
      .then((data) => !ignore && setInsights(data))
      .catch((e) => !ignore && setError(e.message));
    return () => {
      ignore = true;
    };
  }, [boutique.id, refreshKey]);

  if (error) return <p className="alert error">{error}</p>;
  if (!insights || insights.boutiqueId !== boutique.id) return <p className="muted">Loading insights…</p>;

  const rate = insights.conversionRate === null ? '–' : `${(insights.conversionRate * 100).toFixed(1)}%`;

  return (
    <section>
      <div className="row space-between">
        <h1>Manager view · {boutique.city}</h1>
        <button onClick={() => setRefreshKey((k) => k + 1)}>Refresh</button>
      </div>
      <p className="muted">Sales count completed orders only. Amounts in {boutique.currency}.</p>

      <div className="kpis">
        <Kpi label="Sales revenue" value={formatMoney(insights.salesRevenue)} />
        <Kpi label="Sales" value={insights.salesCount} hint="completed orders" />
        <Kpi label="Units sold" value={insights.unitsSold} />
        <Kpi label="Prospects" value={insights.prospects} hint="first seen here, clients included" />
        <Kpi label="Clients" value={insights.clients} />
        <Kpi label="Conversion rate" value={rate} hint="clients ÷ prospects" />
      </div>

      <div className="two-columns">
        <div>
          <h2>Top 3 products</h2>
          {insights.topProducts.length === 0 ? (
            <p className="muted">No sales yet.</p>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th className="num">Units</th>
                  <th className="num">Revenue</th>
                </tr>
              </thead>
              <tbody>
                {insights.topProducts.map((p) => (
                  <tr key={p.productId}>
                    <td>{p.name}</td>
                    <td className="num">{p.unitsSold}</td>
                    <td className="num">{formatMoney(p.revenue)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
        <div>
          <h2>Low stock (≤ {insights.lowStockThreshold})</h2>
          {insights.lowStock.length === 0 ? (
            <p className="muted">Nothing is running low.</p>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th className="num">Stock</th>
                </tr>
              </thead>
              <tbody>
                {insights.lowStock.map((p) => (
                  <tr key={p.productId}>
                    <td>{p.name}</td>
                    <td className={p.stock === 0 ? 'num out' : 'num'}>{p.stock}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </section>
  );
}

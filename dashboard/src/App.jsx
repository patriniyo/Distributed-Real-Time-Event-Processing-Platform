import { useEffect, useMemo, useState } from 'react';
import TenantSwitcher from './components/TenantSwitcher';
import LiveFeed from './components/LiveFeed';
import AnalyticsCharts from './components/AnalyticsCharts';
import DlqInspector from './components/DlqInspector';

const DEFAULT_TENANTS = ['tenant-a', 'tenant-b', 'tenant-c'];
const DEFAULT_API_KEY = 'admin-key';

export default function App() {
  const [tenant, setTenant] = useState(DEFAULT_TENANTS[0]);
  const [eventType, setEventType] = useState('');
  const [apiKey, setApiKey] = useState(() => localStorage.getItem('drep-api-key') || DEFAULT_API_KEY);

  useEffect(() => {
    localStorage.setItem('drep-api-key', apiKey);
  }, [apiKey]);

  const wsUrl = useMemo(() => {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    const params = new URLSearchParams({ tenant, token: apiKey });
    if (eventType) {
      params.set('eventType', eventType);
    }
    return `${protocol}//${host}/ws/events?${params.toString()}`;
  }, [tenant, eventType, apiKey]);

  return (
    <div className="app">
      <header className="header">
        <h1>DREP Dashboard</h1>
        <div className="controls">
          <TenantSwitcher tenants={DEFAULT_TENANTS} tenant={tenant} onChange={setTenant} />
          <label>
            Event type
            <input
              type="text"
              placeholder="all"
              value={eventType}
              onChange={(e) => setEventType(e.target.value)}
            />
          </label>
          <label>
            API key
            <input type="password" value={apiKey} onChange={(e) => setApiKey(e.target.value)} />
          </label>
        </div>
      </header>

      <main className="main">
        <section className="panel">
          <LiveFeed wsUrl={wsUrl} tenant={tenant} />
        </section>
        <section className="panel">
          <AnalyticsCharts tenant={tenant} eventType={eventType} apiKey={apiKey} />
        </section>
        <section className="panel full-width">
          <DlqInspector apiKey={apiKey} />
        </section>
      </main>
    </div>
  );
}

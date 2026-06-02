export default function TenantSwitcher({ tenants, tenant, onChange }) {
  return (
    <label>
      Tenant
      <select value={tenant} onChange={(e) => onChange(e.target.value)}>
        {tenants.map((t) => (
          <option key={t} value={t}>
            {t}
          </option>
        ))}
      </select>
    </label>
  );
}

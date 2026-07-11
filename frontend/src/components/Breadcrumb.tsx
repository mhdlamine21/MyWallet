import { Link } from "react-router-dom";

export interface BreadcrumbItem {
  label: string;
  to?: string;
}

interface BreadcrumbProps {
  items: BreadcrumbItem[];
}

export default function Breadcrumb({ items }: BreadcrumbProps) {
  return (
    <nav aria-label="Breadcrumb" className="flex items-center gap-2 text-xs text-slate-400 mb-4">
      <Link to="/dashboard" className="hover:text-indigo-400 transition-colors flex items-center gap-1">
        <span>🏠</span>
        <span>Dashboard</span>
      </Link>
      {items.map((item, index) => (
        <div key={index} className="flex items-center gap-2">
          <span className="text-slate-600">/</span>
          {item.to ? (
            <Link to={item.to} className="hover:text-indigo-400 transition-colors">
              {item.label}
            </Link>
          ) : (
            <span className="text-slate-200 font-medium">{item.label}</span>
          )}
        </div>
      ))}
    </nav>
  );
}

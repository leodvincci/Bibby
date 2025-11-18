# Section 11: Signature Project (Phase 2)
**Building the Analytics Dashboard Frontend**

**Week 11: From API to Visual Impact**

---

## Overview

Section 10 gave you a robust analytics backend with 12 metrics, caching, and CSV export. Now we'll build the visual layer that makes this work **portfolio-worthy**.

A backend API alone doesn't impress in portfolios. Interviewers can't see it without reading code. But a **dashboard with live charts?** That's immediately compelling. It demonstrates:
- Full-stack capability (backend + frontend)
- UX thinking (how should data be presented?)
- Modern frontend skills (React, charting libraries)
- Production polish (loading states, error handling, responsive design)

By the end of this section, you'll have a professional dashboard that looks like industrial monitoring software—because it's built on the same principles.

**This Week's Focus:**
- Set up React project with Vite
- Build reusable dashboard components
- Integrate Chart.js for data visualization
- Connect to analytics API
- Implement auto-refresh for real-time updates
- Deploy to Vercel/Netlify

---

## Part 1: Project Setup

### Why Vite Instead of Create React App?

**Vite** is the modern choice:
- 10-100x faster build times
- Better developer experience (instant HMR)
- Smaller bundle sizes
- Industry standard for new projects (Create React App is deprecated)

### Initialize React Project

```bash
# Navigate to Bibby root directory
cd /path/to/Bibby

# Create frontend directory
npm create vite@latest frontend -- --template react

# Navigate to frontend
cd frontend

# Install dependencies
npm install

# Install additional libraries
npm install axios recharts date-fns lucide-react
```

**Dependencies explained:**
- **axios:** HTTP client for API calls
- **recharts:** React charting library (simpler than Chart.js for React)
- **date-fns:** Date formatting utilities
- **lucide-react:** Modern icon library

### Project Structure

```
frontend/
├── src/
│   ├── api/
│   │   └── analyticsApi.js       # API service layer
│   ├── components/
│   │   ├── Dashboard.jsx          # Main dashboard container
│   │   ├── KPICard.jsx            # Reusable metric card
│   │   ├── CirculationChart.jsx   # Line chart for trends
│   │   ├── PopularBooksChart.jsx  # Bar chart for top books
│   │   ├── LoadingSpinner.jsx     # Loading state
│   │   └── ErrorMessage.jsx       # Error state
│   ├── hooks/
│   │   └── useAnalytics.js        # Custom hook for data fetching
│   ├── utils/
│   │   └── formatters.js          # Number/date formatting
│   ├── App.jsx
│   ├── main.jsx
│   └── index.css
├── package.json
└── vite.config.js
```

### Configure CORS in Backend

Update `WebConfig.java` to allow frontend origin:

```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins(
            "http://localhost:5173",  // Vite dev server
            "https://bibby-dashboard.vercel.app"  // Production
        )
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
}
```

---

## Part 2: API Service Layer

**File:** `src/api/analyticsApi.js`

```javascript
import axios from 'axios';

// Base URL from environment variable or default to localhost
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

const analyticsApi = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * API service for analytics endpoints.
 *
 * Industrial analogy: SCADA data acquisition layer.
 * Handles communication with backend, error handling, retries.
 */
export const AnalyticsAPI = {
  /**
   * Get high-level KPI summary.
   *
   * @returns {Promise<Object>} Summary with all KPIs
   */
  getSummary: async () => {
    try {
      const response = await analyticsApi.get('/analytics/summary');
      return response.data;
    } catch (error) {
      console.error('Error fetching summary:', error);
      throw new Error('Failed to load dashboard summary');
    }
  },

  /**
   * Get detailed circulation metrics.
   */
  getCirculationMetrics: async () => {
    try {
      const response = await analyticsApi.get('/analytics/circulation');
      return response.data;
    } catch (error) {
      console.error('Error fetching circulation metrics:', error);
      throw new Error('Failed to load circulation data');
    }
  },

  /**
   * Get top N popular books.
   *
   * @param {number} limit - Number of books to return
   */
  getTopBooks: async (limit = 10) => {
    try {
      const response = await analyticsApi.get('/analytics/top-books', {
        params: { limit },
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching top books:', error);
      throw new Error('Failed to load popular books');
    }
  },

  /**
   * Get circulation trend data.
   *
   * @param {number} days - Number of days to include
   */
  getTrends: async (days = 30) => {
    try {
      const response = await analyticsApi.get('/analytics/trends', {
        params: { days },
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching trends:', error);
      throw new Error('Failed to load trend data');
    }
  },

  /**
   * Download CSV export.
   *
   * @param {string} type - 'trends' or 'top-books'
   * @param {Object} params - Query parameters
   */
  exportCSV: async (type, params = {}) => {
    try {
      const response = await analyticsApi.get(`/analytics/export/${type}`, {
        params,
        responseType: 'blob',
      });

      // Trigger browser download
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${type}-${new Date().toISOString().split('T')[0]}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (error) {
      console.error('Error exporting CSV:', error);
      throw new Error('Failed to export data');
    }
  },
};
```

**Environment Variables:**

**File:** `.env`

```bash
VITE_API_URL=http://localhost:8080/api/v1
```

**File:** `.env.production`

```bash
VITE_API_URL=https://bibby-api.your-domain.com/api/v1
```

---

## Part 3: Custom Hook for Data Fetching

**File:** `src/hooks/useAnalytics.js`

```javascript
import { useState, useEffect, useCallback } from 'react';
import { AnalyticsAPI } from '../api/analyticsApi';

/**
 * Custom hook for fetching analytics data.
 * Handles loading states, errors, and auto-refresh.
 *
 * @param {number} refreshInterval - Auto-refresh interval in ms (0 = disabled)
 * @returns {Object} { data, loading, error, refresh }
 */
export const useAnalytics = (refreshInterval = 0) => {
  const [data, setData] = useState({
    summary: null,
    circulation: null,
    topBooks: null,
    trends: null,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  /**
   * Fetch all analytics data.
   * Industrial analogy: SCADA data poll cycle.
   */
  const fetchData = useCallback(async () => {
    try {
      setError(null);

      // Fetch all endpoints in parallel
      const [summary, circulation, topBooks, trends] = await Promise.all([
        AnalyticsAPI.getSummary(),
        AnalyticsAPI.getCirculationMetrics(),
        AnalyticsAPI.getTopBooks(10),
        AnalyticsAPI.getTrends(30),
      ]);

      setData({
        summary,
        circulation,
        topBooks,
        trends,
      });
    } catch (err) {
      setError(err.message);
      console.error('Analytics fetch error:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  // Initial fetch
  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // Auto-refresh if interval specified
  useEffect(() => {
    if (refreshInterval > 0) {
      const intervalId = setInterval(fetchData, refreshInterval);
      return () => clearInterval(intervalId);
    }
  }, [refreshInterval, fetchData]);

  return {
    data,
    loading,
    error,
    refresh: fetchData,
  };
};
```

---

## Part 4: Reusable Components

### KPI Card Component

**File:** `src/components/KPICard.jsx`

```jsx
import React from 'react';
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';

/**
 * KPI Card component for displaying metrics.
 *
 * Industrial analogy: SCADA gauge/indicator.
 * Shows current value, trend, and status color.
 */
const KPICard = ({
  title,
  value,
  subtitle,
  trend,
  icon: Icon,
  color = 'blue'
}) => {
  const colorClasses = {
    blue: 'bg-blue-50 text-blue-600 border-blue-200',
    green: 'bg-green-50 text-green-600 border-green-200',
    yellow: 'bg-yellow-50 text-yellow-600 border-yellow-200',
    red: 'bg-red-50 text-red-600 border-red-200',
  };

  const getTrendIcon = () => {
    if (trend > 0) return <TrendingUp className="w-4 h-4 text-green-500" />;
    if (trend < 0) return <TrendingDown className="w-4 h-4 text-red-500" />;
    return <Minus className="w-4 h-4 text-gray-400" />;
  };

  return (
    <div className={`p-6 rounded-lg border-2 ${colorClasses[color]} transition-all hover:shadow-lg`}>
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <p className="text-sm font-medium text-gray-600 mb-1">{title}</p>
          <p className="text-3xl font-bold mb-2">
            {typeof value === 'number' ? value.toLocaleString() : value}
          </p>
          {subtitle && (
            <p className="text-sm text-gray-500">{subtitle}</p>
          )}
        </div>
        {Icon && (
          <div className="ml-4">
            <Icon className="w-8 h-8 opacity-50" />
          </div>
        )}
      </div>
      {trend !== undefined && trend !== null && (
        <div className="flex items-center mt-4 pt-4 border-t border-gray-200">
          {getTrendIcon()}
          <span className="ml-2 text-sm text-gray-600">
            {trend > 0 ? '+' : ''}{trend}% from last period
          </span>
        </div>
      )}
    </div>
  );
};

export default KPICard;
```

### Loading Spinner Component

**File:** `src/components/LoadingSpinner.jsx`

```jsx
import React from 'react';

const LoadingSpinner = ({ message = 'Loading analytics...' }) => {
  return (
    <div className="flex flex-col items-center justify-center min-h-screen">
      <div className="animate-spin rounded-full h-16 w-16 border-b-4 border-blue-600"></div>
      <p className="mt-4 text-gray-600 text-lg">{message}</p>
    </div>
  );
};

export default LoadingSpinner;
```

### Error Message Component

**File:** `src/components/ErrorMessage.jsx`

```jsx
import React from 'react';
import { AlertCircle, RefreshCw } from 'lucide-react';

const ErrorMessage = ({ error, onRetry }) => {
  return (
    <div className="flex flex-col items-center justify-center min-h-screen px-4">
      <div className="bg-red-50 border-2 border-red-200 rounded-lg p-8 max-w-md w-full">
        <div className="flex items-center justify-center mb-4">
          <AlertCircle className="w-12 h-12 text-red-600" />
        </div>
        <h2 className="text-xl font-bold text-gray-900 text-center mb-2">
          Unable to Load Dashboard
        </h2>
        <p className="text-gray-600 text-center mb-6">
          {error || 'An unexpected error occurred'}
        </p>
        <button
          onClick={onRetry}
          className="w-full bg-blue-600 text-white py-3 px-4 rounded-lg font-medium hover:bg-blue-700 transition-colors flex items-center justify-center"
        >
          <RefreshCw className="w-5 h-5 mr-2" />
          Retry
        </button>
      </div>
    </div>
  );
};

export default ErrorMessage;
```

---

## Part 5: Chart Components

### Circulation Trend Chart

**File:** `src/components/CirculationChart.jsx`

```jsx
import React from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { format, parseISO } from 'date-fns';

/**
 * Line chart for circulation trends over time.
 *
 * Industrial analogy: SCADA trend graph.
 * Shows checkouts, returns, and active over time.
 */
const CirculationChart = ({ data }) => {
  if (!data || data.length === 0) {
    return (
      <div className="flex items-center justify-center h-64 text-gray-500">
        No trend data available
      </div>
    );
  }

  // Format data for Recharts
  const chartData = data.map(point => ({
    date: format(parseISO(point.date), 'MMM dd'),
    checkouts: point.checkoutCount,
    returns: point.returnCount,
    active: point.activeCheckouts,
  }));

  return (
    <div className="bg-white p-6 rounded-lg border-2 border-gray-200">
      <h3 className="text-lg font-bold text-gray-900 mb-4">
        Circulation Trends (30 Days)
      </h3>
      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={chartData}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis
            dataKey="date"
            tick={{ fontSize: 12 }}
            angle={-45}
            textAnchor="end"
            height={60}
          />
          <YAxis tick={{ fontSize: 12 }} />
          <Tooltip />
          <Legend />
          <Line
            type="monotone"
            dataKey="checkouts"
            stroke="#3b82f6"
            strokeWidth={2}
            name="Checkouts"
          />
          <Line
            type="monotone"
            dataKey="returns"
            stroke="#10b981"
            strokeWidth={2}
            name="Returns"
          />
          <Line
            type="monotone"
            dataKey="active"
            stroke="#f59e0b"
            strokeWidth={2}
            name="Active"
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};

export default CirculationChart;
```

### Popular Books Bar Chart

**File:** `src/components/PopularBooksChart.jsx`

```jsx
import React from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';

/**
 * Bar chart for most popular books.
 */
const PopularBooksChart = ({ data }) => {
  if (!data || data.length === 0) {
    return (
      <div className="flex items-center justify-center h-64 text-gray-500">
        No popularity data available
      </div>
    );
  }

  // Format data: truncate long titles
  const chartData = data.map(book => ({
    title: book.title.length > 20
      ? book.title.substring(0, 20) + '...'
      : book.title,
    checkouts: book.checkoutCount,
    fullTitle: book.title,
  }));

  return (
    <div className="bg-white p-6 rounded-lg border-2 border-gray-200">
      <h3 className="text-lg font-bold text-gray-900 mb-4">
        Top 10 Most Popular Books
      </h3>
      <ResponsiveContainer width="100%" height={300}>
        <BarChart data={chartData} layout="vertical">
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis type="number" tick={{ fontSize: 12 }} />
          <YAxis
            type="category"
            dataKey="title"
            tick={{ fontSize: 11 }}
            width={150}
          />
          <Tooltip
            content={({ active, payload }) => {
              if (active && payload && payload.length) {
                return (
                  <div className="bg-white p-3 border border-gray-300 rounded shadow-lg">
                    <p className="font-semibold text-sm">{payload[0].payload.fullTitle}</p>
                    <p className="text-sm text-gray-600">
                      Checkouts: {payload[0].value}
                    </p>
                  </div>
                );
              }
              return null;
            }}
          />
          <Bar dataKey="checkouts" fill="#3b82f6" />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
};

export default PopularBooksChart;
```

---

## Part 6: Main Dashboard Component

**File:** `src/components/Dashboard.jsx`

```jsx
import React, { useState } from 'react';
import {
  BookOpen,
  CheckCircle,
  Clock,
  AlertTriangle,
  Download,
  RefreshCw,
} from 'lucide-react';
import { useAnalytics } from '../hooks/useAnalytics';
import { AnalyticsAPI } from '../api/analyticsApi';
import KPICard from './KPICard';
import CirculationChart from './CirculationChart';
import PopularBooksChart from './PopularBooksChart';
import LoadingSpinner from './LoadingSpinner';
import ErrorMessage from './ErrorMessage';

/**
 * Main dashboard component.
 *
 * Industrial analogy: SCADA HMI overview screen.
 * At-a-glance KPIs, trend graphs, operational status.
 */
const Dashboard = () => {
  const [autoRefresh, setAutoRefresh] = useState(true);
  const refreshInterval = autoRefresh ? 300000 : 0; // 5 minutes

  const { data, loading, error, refresh } = useAnalytics(refreshInterval);

  const handleExportTrends = async () => {
    try {
      await AnalyticsAPI.exportCSV('trends', { days: 30 });
    } catch (err) {
      alert('Failed to export data: ' + err.message);
    }
  };

  const handleExportTopBooks = async () => {
    try {
      await AnalyticsAPI.exportCSV('top-books', { limit: 10 });
    } catch (err) {
      alert('Failed to export data: ' + err.message);
    }
  };

  if (loading && !data.summary) {
    return <LoadingSpinner />;
  }

  if (error && !data.summary) {
    return <ErrorMessage error={error} onRetry={refresh} />;
  }

  const { summary, circulation, topBooks, trends } = data;

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-4xl font-bold text-gray-900 mb-2">
                Library Analytics Dashboard
              </h1>
              <p className="text-gray-600">
                Real-time operational metrics and circulation trends
              </p>
            </div>
            <div className="flex gap-3">
              <button
                onClick={() => setAutoRefresh(!autoRefresh)}
                className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                  autoRefresh
                    ? 'bg-green-100 text-green-700 border-2 border-green-300'
                    : 'bg-gray-100 text-gray-700 border-2 border-gray-300'
                }`}
              >
                Auto-refresh: {autoRefresh ? 'ON' : 'OFF'}
              </button>
              <button
                onClick={refresh}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors flex items-center"
              >
                <RefreshCw className="w-4 h-4 mr-2" />
                Refresh
              </button>
            </div>
          </div>
        </div>

        {/* KPI Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          <KPICard
            title="Total Books"
            value={summary?.totalBooks || 0}
            subtitle="In library system"
            icon={BookOpen}
            color="blue"
          />
          <KPICard
            title="Available Books"
            value={summary?.availableBooks || 0}
            subtitle={`${((summary?.availableBooks / summary?.totalBooks) * 100 || 0).toFixed(1)}% of total`}
            icon={CheckCircle}
            color="green"
          />
          <KPICard
            title="Active Checkouts"
            value={summary?.activeCheckouts || 0}
            subtitle="Currently checked out"
            icon={Clock}
            color="yellow"
          />
          <KPICard
            title="Overdue Books"
            value={summary?.overdueBooks || 0}
            subtitle={`${summary?.criticalOverdue || 0} critical (>30 days)`}
            icon={AlertTriangle}
            color={summary?.overdueBooks > 0 ? 'red' : 'green'}
          />
        </div>

        {/* Circulation Metrics */}
        <div className="bg-white p-6 rounded-lg border-2 border-gray-200 mb-8">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">
            Circulation Overview
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div>
              <p className="text-sm text-gray-600 mb-1">This Week</p>
              <p className="text-2xl font-bold text-blue-600">
                {circulation?.checkoutsThisWeek || 0} checkouts
              </p>
              <p className="text-sm text-gray-500">
                {circulation?.returnsThisWeek || 0} returns
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-600 mb-1">This Month</p>
              <p className="text-2xl font-bold text-blue-600">
                {circulation?.checkoutsThisMonth || 0} checkouts
              </p>
              <p className="text-sm text-gray-500">
                {circulation?.returnsThisMonth || 0} returns
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-600 mb-1">Average Duration</p>
              <p className="text-2xl font-bold text-blue-600">
                {circulation?.averageCheckoutDurationDays?.toFixed(1) || 0} days
              </p>
              <p className="text-sm text-gray-500">
                All-time: {circulation?.totalCheckoutsAllTime || 0} checkouts
              </p>
            </div>
          </div>
        </div>

        {/* Charts */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
          <div className="relative">
            <CirculationChart data={trends} />
            <button
              onClick={handleExportTrends}
              className="absolute top-8 right-8 p-2 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
              title="Export to CSV"
            >
              <Download className="w-5 h-5 text-gray-600" />
            </button>
          </div>
          <div className="relative">
            <PopularBooksChart data={topBooks} />
            <button
              onClick={handleExportTopBooks}
              className="absolute top-8 right-8 p-2 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
              title="Export to CSV"
            >
              <Download className="w-5 h-5 text-gray-600" />
            </button>
          </div>
        </div>

        {/* Footer */}
        <div className="text-center text-sm text-gray-500">
          <p>
            Last updated: {summary?.generatedAt
              ? new Date(summary.generatedAt).toLocaleString()
              : 'Never'}
          </p>
          <p className="mt-1">
            Data refreshes automatically every 5 minutes when auto-refresh is enabled
          </p>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
```

---

## Part 7: App Entry Point and Styling

**File:** `src/App.jsx`

```jsx
import React from 'react';
import Dashboard from './components/Dashboard';

function App() {
  return <Dashboard />;
}

export default App;
```

**File:** `src/index.css`

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen',
    'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue',
    sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* Custom scrollbar */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
}

::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #555;
}
```

### Configure Tailwind CSS

**File:** `tailwind.config.js`

```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {},
  },
  plugins: [],
}
```

**Install Tailwind:**

```bash
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
```

---

## Part 8: Running and Testing

### Development

```bash
# Terminal 1: Start backend (from Bibby root)
mvn spring-boot:run

# Terminal 2: Start frontend (from frontend directory)
cd frontend
npm run dev

# Access dashboard at http://localhost:5173
```

### Testing Workflow

1. **Verify API Connection:**
   - Open browser console
   - Check Network tab for API calls
   - Ensure no CORS errors

2. **Test Features:**
   - KPI cards display correct data
   - Charts render with data
   - Auto-refresh toggle works
   - Manual refresh updates data
   - CSV export downloads files

3. **Test Error Handling:**
   - Stop backend server
   - Verify error message displays
   - Verify retry button works

4. **Test Responsive Design:**
   - Resize browser window
   - Test on mobile (Chrome DevTools)
   - Verify layout adapts

---

## Part 9: Production Build and Deployment

### Build for Production

```bash
npm run build
```

This creates optimized bundle in `dist/` directory.

### Deploy to Vercel

```bash
# Install Vercel CLI
npm install -g vercel

# Login
vercel login

# Deploy
vercel

# Production deployment
vercel --prod
```

**Configure environment variables in Vercel dashboard:**
- `VITE_API_URL` → Your production backend URL

### Deploy to Netlify

```bash
# Install Netlify CLI
npm install -g netlify-cli

# Login
netlify login

# Deploy
netlify deploy

# Production deployment
netlify deploy --prod
```

**Or use Netlify UI:**
1. Connect GitHub repository
2. Set build command: `npm run build`
3. Set publish directory: `dist`
4. Add environment variable: `VITE_API_URL`

### Backend Deployment

Deploy Spring Boot backend to:
- **Render:** Easy free tier, automatic deploys from GitHub
- **Railway:** Modern platform, simple database setup
- **Fly.io:** Global edge deployment

**Example for Render:**
1. Create new Web Service
2. Connect GitHub repository
3. Build command: `mvn clean package`
4. Start command: `java -jar target/Bibby-0.0.1-SNAPSHOT.jar`
5. Add environment variables (database credentials)

---

## Part 10: Portfolio Presentation

### Screenshots to Capture

1. **Full dashboard view** - Overview with all KPIs and charts
2. **Individual charts** - Close-up of trend chart
3. **Mobile view** - Responsive design on phone
4. **Loading state** - Shows polish
5. **Error handling** - Shows production thinking

### README Section

Add to Bibby README:

```markdown
## Analytics Dashboard

![Dashboard Screenshot](docs/images/dashboard-screenshot.png)

Real-time analytics dashboard for library operations, built with React and Spring Boot.

### Features

- **12 Key Metrics:** Inventory, circulation, and operational health KPIs
- **Live Charts:** Interactive trend analysis with Recharts
- **Auto-Refresh:** Configurable 5-minute polling for real-time updates
- **CSV Export:** Download reports for offline analysis
- **Responsive Design:** Works on desktop, tablet, and mobile
- **Production Performance:** Sub-50ms API response via Redis caching

### Tech Stack

**Backend:**
- Spring Boot 3.x
- PostgreSQL for data storage
- Redis for caching
- Scheduled jobs for pre-computation

**Frontend:**
- React 18 with Vite
- Recharts for data visualization
- Tailwind CSS for styling
- Axios for API communication

### Live Demo

- **Dashboard:** https://bibby-dashboard.vercel.app
- **API Docs:** https://bibby-api.onrender.com/swagger-ui.html

### Running Locally

See [Development Setup](docs/DEVELOPMENT.md)
```

---

## Action Items for Week 11

### Critical (Must Complete)

**1. Set Up React Project** (2-3 hours)
- Initialize Vite project
- Install dependencies
- Configure Tailwind CSS
- Set up project structure

**Deliverable:** Working React development environment

**2. Build API Service Layer** (2-3 hours)
- Create `analyticsApi.js`
- Implement all API methods
- Add error handling
- Configure environment variables

**Deliverable:** Functional API client

**3. Create Core Components** (6-8 hours)
- Implement KPICard, LoadingSpinner, ErrorMessage
- Build CirculationChart and PopularBooksChart
- Create custom useAnalytics hook
- Build main Dashboard component

**Deliverable:** Complete component library

**4. Integration and Testing** (4-5 hours)
- Connect frontend to backend
- Test all features (KPIs, charts, refresh, export)
- Fix CORS issues
- Test error handling

**Deliverable:** Fully functional dashboard

**5. Deploy to Production** (3-4 hours)
- Build production bundle
- Deploy frontend to Vercel/Netlify
- Deploy backend to Render/Railway
- Configure environment variables
- Test production deployment

**Deliverable:** Live dashboard with public URL

### Important (Should Complete)

**6. Add Responsive Design Polish** (2-3 hours)
- Test on multiple screen sizes
- Optimize mobile layout
- Improve touch interactions

**7. Performance Optimization** (2-3 hours)
- Lazy load charts
- Optimize bundle size
- Add service worker for offline support

**8. Accessibility** (2 hours)
- Add ARIA labels
- Keyboard navigation
- Screen reader testing

### Bonus (If Time Permits)

**9. Advanced Features** (4-5 hours)
- Date range picker for custom periods
- Dark mode toggle
- Dashboard layout customization

**10. Analytics** (2 hours)
- Add Google Analytics
- Track user interactions
- Monitor performance metrics

---

## Success Metrics for Week 11

By the end of this week, you should have:

✅ **Complete Frontend Dashboard:**
- Responsive design (desktop, tablet, mobile)
- All 12 KPIs displayed in cards
- Interactive charts (trends, popular books)
- Auto-refresh and manual refresh
- CSV export functionality

✅ **Production Deployment:**
- Frontend deployed to Vercel/Netlify with custom URL
- Backend deployed to Render/Railway
- Working end-to-end in production
- No CORS errors

✅ **Portfolio Ready:**
- Screenshots captured
- README updated with dashboard section
- Live demo accessible via public URL
- Code documented and clean

✅ **Interview Ready:**
- Can explain technical choices (React, Recharts, caching)
- Can demonstrate live dashboard
- Can discuss performance optimizations
- Can connect to SCADA/HMI experience

---

## Industrial Connection: Dashboard Design Principles

### SCADA HMI Best Practices Applied to Bibby

**Principle 1: At-a-Glance Status**
- SCADA: Equipment status (running, stopped, fault) with color coding
- Bibby: Book status (available, checked out, overdue) with color-coded KPI cards

**Principle 2: Hierarchical Information**
- SCADA: Overview → Detail drill-down
- Bibby: Summary KPIs → Detailed charts → CSV export for deep analysis

**Principle 3: Real-Time Updates**
- SCADA: Polling every 1-5 seconds for critical systems
- Bibby: Polling every 5 minutes (appropriate for library operations)

**Principle 4: Alarm/Alert Visibility**
- SCADA: Critical alarms highlighted, sound alerts
- Bibby: Overdue books highlighted in red, count prominently displayed

**Principle 5: Historical Trending**
- SCADA: Line charts for temperature, pressure, flow over time
- Bibby: Line charts for checkouts, returns, active over time

### Interview Narrative

> "The dashboard design draws directly from SCADA interfaces I used at Kinder Morgan. In pipeline operations, operators need at-a-glance status—green means normal, red means attention required. I applied the same principle with color-coded KPI cards. The trend charts mirror SCADA historians that track equipment performance over time—librarians can spot circulation patterns just like operators spot pressure trends. Auto-refresh with configurable interval is standard in SCADA (we used 5-second polls for critical systems, 1-minute for non-critical). For libraries, 5-minute refresh balances data freshness with server load."

**This demonstrates:**
- Direct operational experience ✓
- Design thinking based on real systems ✓
- Appropriate engineering trade-offs ✓
- User-centered approach ✓

---

## What's Next

**Section 12: Signature Project (Phase 3) — Advanced Features**

Final week of signature project:
- Predictive alerts (circulation dropping, inventory low)
- Drill-down from KPIs to detailed views
- Custom date range filtering
- User preferences (save dashboard configuration)
- Anomaly detection in trends
- Preparation for technical interviews

You've now built a complete full-stack application with enterprise patterns. Section 12 adds the final polish that elevates it from "good" to "exceptional."

---

**Progress Tracker:** 11/32 sections complete (34%)

**Next Section:** Signature Project (Phase 3) — Advanced analytics features and interview preparation

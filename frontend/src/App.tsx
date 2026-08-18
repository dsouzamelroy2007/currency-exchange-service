import { Dashboard } from './components/dashboard/Dashboard';
import { HistoricalRateChart } from './components/chart/HistoricalRateChart';

function App() {
  return (
    <div className="app">
      <h1>Currency Exchange</h1>
      <Dashboard />
      <HistoricalRateChart />
    </div>
  );
}

export default App;

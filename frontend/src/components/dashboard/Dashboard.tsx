import { useTrackedPairs } from '../../hooks/useTrackedPairs';
import { pairKey } from '../../utils/currencyPair';
import { ConnectionStatusBadge } from '../common/ConnectionStatusBadge';
import { AddPairForm } from './AddPairForm';
import { PairCard } from './PairCard';

export function Dashboard() {
  const { pairs, addPair, removePair } = useTrackedPairs();

  return (
    <section className="dashboard">
      <div className="dashboard-header">
        <h2>Dashboard</h2>
        <ConnectionStatusBadge />
      </div>
      <AddPairForm onAdd={addPair} />
      <div className="pair-card-list">
        {pairs.map((pair) => (
          <PairCard key={pairKey(pair)} pair={pair} onRemove={removePair} />
        ))}
      </div>
    </section>
  );
}

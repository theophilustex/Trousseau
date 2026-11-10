import React, { useState } from 'react';
import {
  WashingMachine,
  CheckCircle2,
  AlertTriangle,
  RotateCcw,
  Sparkles,
  Shirt
} from 'lucide-react';
import type { ClothingItem } from '@trousseau/shared';

interface LaundryManagerProps {
  items: ClothingItem[];
  onWashItem: (id: string) => Promise<void>;
  onWashAll: () => Promise<void>;
}

export const LaundryManager: React.FC<LaundryManagerProps> = ({
  items,
  onWashItem,
  onWashAll
}) => {
  const [isWashingAll, setIsWashingAll] = useState(false);

  const dirtyItems = items.filter(
    (item) => item.status === 'needs_wash' || item.wearsSinceWash >= item.maxWearsBeforeWash
  );

  const handleWashAll = async () => {
    if (dirtyItems.length === 0) return;
    setIsWashingAll(true);
    try {
      await onWashAll();
    } finally {
      setIsWashingAll(false);
    }
  };

  return (
    <div className="max-w-5xl mx-auto px-4 py-8 space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white dark:bg-stone-900 rounded-3xl p-6 border border-stone-200 dark:border-stone-800 shadow-sm">
        <div className="flex items-center space-x-4">
          <div className="w-12 h-12 bg-amber-50 dark:bg-amber-950/60 rounded-2xl flex items-center justify-center text-amber-700 dark:text-amber-400">
            <WashingMachine className="w-6 h-6" />
          </div>
          <div>
            <h2 className="text-2xl font-bold font-serif text-stone-900 dark:text-stone-100">Laundry Basket</h2>
            <p className="text-stone-500 dark:text-stone-400 text-sm">
              {dirtyItems.length} item{dirtyItems.length === 1 ? '' : 's'} waiting to be washed.
            </p>
          </div>
        </div>

        {dirtyItems.length > 0 && (
          <button
            onClick={handleWashAll}
            disabled={isWashingAll}
            className="flex items-center justify-center space-x-2 px-6 py-3 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-sm font-semibold shadow-sm transition-all disabled:opacity-50"
          >
            <RotateCcw className="w-4 h-4" />
            <span>{isWashingAll ? 'Washing...' : `Wash All (${dirtyItems.length}) Items`}</span>
          </button>
        )}
      </div>

      {/* Dirty Items List */}
      {dirtyItems.length > 0 ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {dirtyItems.map((item) => (
            <div
              key={item.id}
              className="bg-white dark:bg-stone-900 rounded-2xl p-4 border border-red-200/80 dark:border-red-900/40 shadow-sm flex items-center space-x-4"
            >
              <div className="w-16 h-16 rounded-xl bg-stone-50 dark:bg-stone-800 border border-stone-200 dark:border-stone-700 overflow-hidden flex items-center justify-center flex-shrink-0">
                {item.thumbnailUrl || item.imageUrl ? (
                  <img src={item.thumbnailUrl || item.imageUrl || ''} alt={item.name} className="w-full h-full object-contain" />
                ) : (
                  <Shirt className="w-6 h-6 text-stone-300 dark:text-stone-600" />
                )}
              </div>

              <div className="flex-1 min-w-0">
                <div className="flex items-center space-x-1.5 text-red-600 dark:text-red-400 text-[11px] font-bold">
                  <AlertTriangle className="w-3.5 h-3.5 flex-shrink-0" />
                  <span>Threshold reached</span>
                </div>
                <h4 className="font-bold text-stone-900 dark:text-stone-100 text-sm truncate">{item.name}</h4>
                <div className="text-xs text-stone-500 dark:text-stone-400 font-mono">
                  {item.wearsSinceWash} / {item.maxWearsBeforeWash} wears completed
                </div>
              </div>

              <button
                onClick={() => onWashItem(item.id)}
                className="px-3 py-1.5 bg-emerald-50 dark:bg-emerald-950/40 hover:bg-emerald-100 dark:hover:bg-emerald-900/60 text-emerald-800 dark:text-emerald-300 rounded-lg text-xs font-semibold border border-emerald-200 dark:border-emerald-800 transition-colors"
              >
                Wash
              </button>
            </div>
          ))}
        </div>
      ) : (
        <div className="bg-white dark:bg-stone-900 rounded-3xl p-12 text-center border border-stone-200 dark:border-stone-800 space-y-3">
          <div className="w-14 h-14 bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 rounded-2xl flex items-center justify-center mx-auto">
            <CheckCircle2 className="w-8 h-8" />
          </div>
          <h3 className="font-bold text-lg font-serif text-stone-900 dark:text-stone-100">Your Laundry is All Clean!</h3>
          <p className="text-stone-500 dark:text-stone-400 text-sm max-w-sm mx-auto">
            Every cataloged item in your wardrobe is washed, clean, and ready for rotation.
          </p>
        </div>
      )}
    </div>
  );
};

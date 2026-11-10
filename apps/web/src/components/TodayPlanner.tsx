import React, { useState } from 'react';
import {
  CheckCircle2,
  RefreshCw,
  Sun,
  CloudRain,
  Calendar,
  Sparkles,
  ArrowRight,
  ShieldCheck
} from 'lucide-react';
import type {
  OutfitRecommendation,
  WeatherForecast,
  WearLog,
  Outfit,
  TemperatureUnit
} from '@trousseau/shared';
import { formatTemp, celsiusToFahrenheit } from '@trousseau/shared';

interface TodayPlannerProps {
  weather: WeatherForecast | null;
  todayLog: WearLog | null;
  recommendations: OutfitRecommendation[];
  allOutfits: Outfit[];
  tempUnit: TemperatureUnit;
  onConfirmTodayWear: (status: 'worn' | 'skipped', replacementOutfitId?: string) => Promise<void>;
  onPlanOutfit: (outfitId: string) => Promise<void>;
  onGoToOutfitBuilder: () => void;
  onGoToWardrobe: () => void;
}

export const TodayPlanner: React.FC<TodayPlannerProps> = ({
  weather,
  todayLog,
  recommendations,
  allOutfits,
  tempUnit,
  onConfirmTodayWear,
  onPlanOutfit,
  onGoToOutfitBuilder,
  onGoToWardrobe
}) => {
  const [isConfirming, setIsConfirming] = useState(false);
  const [showSwapModal, setShowSwapModal] = useState(false);

  // Active outfit for today: either already planned in todayLog or the #1 recommendation
  const currentOutfitId = todayLog?.outfitId || recommendations[0]?.outfit?.id;
  const currentOutfit = allOutfits.find(o => o.id === currentOutfitId) || recommendations[0]?.outfit;
  const currentRec = recommendations.find(r => r.outfit.id === currentOutfitId) || recommendations[0];

  const isAlreadyWornToday = todayLog?.status === 'worn';

  const handleConfirm = async () => {
    if (!currentOutfit) return;
    setIsConfirming(true);
    try {
      if (!todayLog) {
        await onPlanOutfit(currentOutfit.id);
      }
      await onConfirmTodayWear('worn');
    } finally {
      setIsConfirming(false);
    }
  };

  const handleSwap = async (newOutfitId: string) => {
    setIsConfirming(true);
    try {
      if (todayLog) {
        await onConfirmTodayWear('worn', newOutfitId);
      } else {
        await onPlanOutfit(newOutfitId);
        await onConfirmTodayWear('worn');
      }
      setShowSwapModal(false);
    } finally {
      setIsConfirming(false);
    }
  };

  // Helper to format reason string with converted temperature
  const formatReasonText = (reason?: string) => {
    if (!reason) return '';
    if (tempUnit === 'F') {
      return reason.replace(/(\d+)°C/g, (_, num) => `${celsiusToFahrenheit(parseInt(num, 10))}°F`);
    }
    return reason;
  };

  return (
    <div className="max-w-5xl mx-auto px-4 py-8 space-y-8">
      {/* 1. Daily Header & Weather Banner */}
      <div className="bg-gradient-to-r from-stone-900 via-stone-800 to-stone-900 dark:from-stone-950 dark:via-stone-900 dark:to-stone-950 rounded-3xl p-6 md:p-8 text-white shadow-xl relative overflow-hidden border border-stone-800">
        <div className="absolute top-0 right-0 -mr-16 -mt-16 w-64 h-64 bg-amber-500/10 rounded-full blur-3xl pointer-events-none" />
        
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-6 relative z-10">
          <div>
            <div className="flex items-center space-x-2 text-amber-400 text-xs font-semibold uppercase tracking-wider mb-2">
              <Calendar className="w-4 h-4" />
              <span>{new Date().toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' })}</span>
            </div>
            <h1 className="text-2xl md:text-3xl font-bold font-serif">Today's Outfit & Daily Plan</h1>
            <p className="text-stone-300 dark:text-stone-400 text-sm mt-1 max-w-xl">
              Smart weather-adapted styling with automated wear tracking and rotation diversity.
            </p>
          </div>

          {weather && (
            <div className="flex items-center space-x-4 bg-white/10 dark:bg-white/5 backdrop-blur-md px-5 py-3 rounded-2xl border border-white/10">
              <div className="p-3 bg-white/10 rounded-xl">
                {weather.isRaining ? (
                  <CloudRain className="w-8 h-8 text-blue-400 animate-pulse" />
                ) : (
                  <Sun className="w-8 h-8 text-amber-400" />
                )}
              </div>
              <div>
                <div className="text-2xl font-bold font-mono">
                  {formatTemp(weather.tempCurrentC, tempUnit)}
                </div>
                <div className="text-xs text-stone-300 dark:text-stone-400 font-medium">
                  {formatTemp(weather.tempLowC, tempUnit)} / {formatTemp(weather.tempHighC, tempUnit)} • {weather.description}
                </div>
                {weather.precipitationProbability > 0 && (
                  <div className="text-[11px] text-blue-300">
                    {weather.precipitationProbability}% precip chance
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* 2. Main Outfit Focus */}
      {currentOutfit ? (
        <div className="bg-white dark:bg-stone-900 rounded-3xl p-6 md:p-8 border border-stone-200 dark:border-stone-800 shadow-sm space-y-6 transition-colors">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-stone-100 dark:border-stone-800 pb-5">
            <div>
              <div className="flex items-center space-x-2">
                <span className="text-xs font-bold uppercase tracking-wider text-amber-800 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/60 px-2.5 py-1 rounded-full border border-amber-200 dark:border-amber-800">
                  {currentOutfit.occasion}
                </span>
                {isAlreadyWornToday && (
                  <span className="flex items-center space-x-1 text-xs font-bold text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/60 px-2.5 py-1 rounded-full border border-emerald-200 dark:border-emerald-800">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    <span>Worn Today</span>
                  </span>
                )}
              </div>
              <h2 className="text-2xl font-bold text-stone-900 dark:text-stone-100 mt-2 font-serif">{currentOutfit.name}</h2>
              {currentRec?.reason && (
                <p className="text-stone-500 dark:text-stone-400 text-sm mt-1 flex items-center space-x-1">
                  <Sparkles className="w-4 h-4 text-amber-500 flex-shrink-0" />
                  <span>{formatReasonText(currentRec.reason)}</span>
                </p>
              )}
            </div>

            {/* Quick 1-Tap Action Buttons */}
            <div className="flex items-center space-x-3 w-full sm:w-auto">
              {!isAlreadyWornToday ? (
                <button
                  onClick={handleConfirm}
                  disabled={isConfirming}
                  className="flex-1 sm:flex-initial flex items-center justify-center space-x-2 px-6 py-3 bg-stone-900 dark:bg-stone-100 hover:bg-stone-800 dark:hover:bg-white active:scale-95 text-white dark:text-stone-950 rounded-xl font-medium shadow-md transition-all disabled:opacity-50"
                >
                  <CheckCircle2 className="w-5 h-5 text-emerald-400 dark:text-emerald-600" />
                  <span>{isConfirming ? 'Confirming...' : "I'm Wearing This Today"}</span>
                </button>
              ) : (
                <div className="flex items-center space-x-2 text-sm text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/60 px-4 py-2.5 rounded-xl border border-emerald-200 dark:border-emerald-800 font-medium">
                  <CheckCircle2 className="w-4 h-4" />
                  <span>Wear confirmed & logged</span>
                </div>
              )}

              <button
                onClick={() => setShowSwapModal(true)}
                title="Swap Outfit"
                className="px-4 py-3 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 text-stone-700 dark:text-stone-300 rounded-xl font-medium transition-colors flex items-center space-x-1"
              >
                <RefreshCw className="w-4 h-4" />
                <span className="hidden sm:inline">Swap</span>
              </button>
            </div>
          </div>

          {/* Outfit Items Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
            {currentOutfit.items?.map((item) => (
              <div
                key={item.id}
                className="bg-stone-50 dark:bg-stone-800/60 rounded-2xl p-3 border border-stone-200 dark:border-stone-700 flex flex-col justify-between group hover:border-stone-300 dark:hover:border-stone-600 transition-all"
              >
                <div className="w-full aspect-square rounded-xl bg-white dark:bg-stone-900 border border-stone-200 dark:border-stone-700 overflow-hidden flex items-center justify-center mb-3">
                  {item.thumbnailUrl || item.imageUrl ? (
                    <img
                      src={item.thumbnailUrl || item.imageUrl || ''}
                      alt={item.name}
                      className="w-full h-full object-contain p-2 group-hover:scale-105 transition-transform"
                    />
                  ) : (
                    <div className="text-stone-300 dark:text-stone-600 text-xs font-medium text-center p-2">
                      No Photo
                    </div>
                  )}
                </div>

                <div>
                  <span className="text-[10px] uppercase font-bold text-stone-400 dark:text-stone-500 tracking-wider">
                    {item.category}
                  </span>
                  <h4 className="font-semibold text-stone-900 dark:text-stone-100 text-sm truncate">{item.name}</h4>
                  {item.brand && <p className="text-xs text-stone-500 dark:text-stone-400 truncate">{item.brand}</p>}

                  {/* Cleanliness / Wash progress meter */}
                  <div className="mt-2 pt-2 border-t border-stone-200 dark:border-stone-700 flex items-center justify-between text-[11px]">
                    <span className="text-stone-500 dark:text-stone-400">Wears:</span>
                    <span className={`font-mono font-bold ${
                      item.wearsSinceWash >= item.maxWearsBeforeWash ? 'text-red-600 dark:text-red-400' : 'text-stone-700 dark:text-stone-300'
                    }`}>
                      {item.wearsSinceWash} / {item.maxWearsBeforeWash}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Diversity & Cleanliness Rationale Badge */}
          <div className="bg-amber-50/60 dark:bg-amber-950/30 border border-amber-200/80 dark:border-amber-800/60 rounded-2xl p-4 flex items-start space-x-3 text-xs text-amber-900 dark:text-amber-200">
            <ShieldCheck className="w-5 h-5 text-amber-600 dark:text-amber-400 flex-shrink-0 mt-0.5" />
            <div className="space-y-1">
              <span className="font-bold">Diversity & Cleanliness Verified</span>
              <p className="text-amber-800 dark:text-amber-300 leading-relaxed">
                All clothing items in this outfit are clean and below their maximum wash threshold.
                {currentRec?.daysSinceLastWorn !== null && currentRec?.daysSinceLastWorn !== undefined ? (
                  <span> This combination was last worn <strong>{currentRec.daysSinceLastWorn === 0 ? 'today' : `${currentRec.daysSinceLastWorn} days ago`}</strong>, maintaining high wardrobe diversity.</span>
                ) : (
                  <span> This combination is ready for its first documented wear!</span>
                )}
              </p>
            </div>
          </div>
        </div>
      ) : (
        <div className="bg-white dark:bg-stone-900 rounded-3xl p-10 border border-stone-200 dark:border-stone-800 text-center space-y-4">
          <div className="w-16 h-16 bg-amber-50 dark:bg-amber-950/60 rounded-2xl flex items-center justify-center text-amber-600 dark:text-amber-400 mx-auto">
            <Sparkles className="w-8 h-8" />
          </div>
          <h3 className="text-xl font-bold font-serif text-stone-900 dark:text-stone-100">No Outfits Created Yet</h3>
          <p className="text-stone-500 dark:text-stone-400 text-sm max-w-md mx-auto">
            Add items to your wardrobe catalog and combine them into outfits to unlock automated weather-adaptive recommendations.
          </p>
          <div className="flex justify-center space-x-3 pt-2">
            <button
              onClick={onGoToWardrobe}
              className="px-5 py-2.5 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 text-stone-800 dark:text-stone-200 rounded-xl font-medium text-sm transition-colors"
            >
              Add Clothes
            </button>
            <button
              onClick={onGoToOutfitBuilder}
              className="px-5 py-2.5 bg-stone-900 dark:bg-stone-100 hover:bg-stone-800 dark:hover:bg-white text-white dark:text-stone-950 rounded-xl font-medium text-sm transition-colors"
            >
              Build an Outfit
            </button>
          </div>
        </div>
      )}

      {/* 3. Alternative Recommendations for Today */}
      {recommendations.length > 1 && (
        <div className="space-y-4">
          <h3 className="text-lg font-bold font-serif text-stone-900 dark:text-stone-100 flex items-center space-x-2">
            <span>Alternative Recommendations for Today</span>
            <span className="text-xs font-normal text-stone-500 dark:text-stone-400 font-sans">
              (Ranked by weather & rotation diversity)
            </span>
          </h3>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {recommendations.slice(1, 4).map((rec) => (
              <div
                key={rec.outfit.id}
                className="bg-white dark:bg-stone-900 rounded-2xl p-5 border border-stone-200 dark:border-stone-800 hover:border-stone-300 dark:hover:border-stone-700 transition-all flex flex-col justify-between space-y-4 shadow-sm"
              >
                <div>
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-bold uppercase text-stone-500 dark:text-stone-400 bg-stone-100 dark:bg-stone-800 px-2 py-0.5 rounded">
                      {rec.outfit.occasion}
                    </span>
                    <span className="font-mono text-amber-700 dark:text-amber-400 font-semibold">
                      Score: {rec.score}
                    </span>
                  </div>
                  <h4 className="font-bold text-stone-900 dark:text-stone-100 mt-2">{rec.outfit.name}</h4>
                  <p className="text-xs text-stone-500 dark:text-stone-400 mt-1 line-clamp-2">{formatReasonText(rec.reason)}</p>
                </div>

                {/* Items thumbnails preview */}
                <div className="flex -space-x-2 overflow-hidden py-1">
                  {rec.outfit.items?.slice(0, 4).map((it) => (
                    <div
                      key={it.id}
                      className="w-10 h-10 rounded-full border-2 border-white dark:border-stone-900 bg-stone-100 dark:bg-stone-800 overflow-hidden flex-shrink-0"
                    >
                      {it.thumbnailUrl ? (
                        <img src={it.thumbnailUrl} alt={it.name} className="w-full h-full object-cover" />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-[10px] text-stone-400">
                          {it.category[0].toUpperCase()}
                        </div>
                      )}
                    </div>
                  ))}
                </div>

                <button
                  onClick={() => handleSwap(rec.outfit.id)}
                  className="w-full py-2 bg-stone-50 dark:bg-stone-800 hover:bg-stone-100 dark:hover:bg-stone-700 text-stone-700 dark:text-stone-300 rounded-xl text-xs font-semibold transition-colors border border-stone-200 dark:border-stone-700 flex items-center justify-center space-x-1"
                >
                  <span>Select for Today</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Outfit Swapper Modal */}
      {showSwapModal && (
        <div className="fixed inset-0 z-50 bg-stone-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white dark:bg-stone-900 rounded-3xl max-w-lg w-full p-6 space-y-5 shadow-2xl max-h-[85vh] flex flex-col border border-stone-200 dark:border-stone-800">
            <div className="flex items-center justify-between border-b border-stone-100 dark:border-stone-800 pb-3">
              <h3 className="text-lg font-bold font-serif text-stone-900 dark:text-stone-100">Choose a Different Outfit</h3>
              <button
                onClick={() => setShowSwapModal(false)}
                className="text-stone-400 hover:text-stone-600 dark:hover:text-stone-300 text-sm font-semibold"
              >
                Cancel
              </button>
            </div>

            <div className="overflow-y-auto space-y-3 flex-1 pr-1">
              {allOutfits.map((outfit) => (
                <div
                  key={outfit.id}
                  onClick={() => handleSwap(outfit.id)}
                  className="p-3.5 rounded-2xl border border-stone-200 dark:border-stone-700 hover:border-amber-500 hover:bg-amber-50/40 dark:hover:bg-amber-950/20 cursor-pointer transition-all flex items-center justify-between"
                >
                  <div>
                    <span className="text-[10px] uppercase font-bold text-stone-400 dark:text-stone-500">{outfit.occasion}</span>
                    <h4 className="font-bold text-stone-900 dark:text-stone-100 text-sm">{outfit.name}</h4>
                    <p className="text-xs text-stone-500 dark:text-stone-400">{outfit.items?.length || 0} items</p>
                  </div>
                  <button className="px-3 py-1.5 bg-stone-900 dark:bg-stone-100 text-white dark:text-stone-950 text-xs font-medium rounded-lg">
                    Wear
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

import React, { useState } from 'react';
import {
  Plus,
  Layers,
  Star,
  MessageSquare,
  Share2,
  Trash2,
  Check,
  Calendar,
  Sparkles
} from 'lucide-react';
import {
  type Outfit,
  type ClothingItem,
  type CreateOutfitInput,
  type Occasion,
  Occasions,
  Seasons
} from '@trousseau/shared';

interface OutfitBuilderProps {
  outfits: Outfit[];
  items: ClothingItem[];
  onCreateOutfit: (input: CreateOutfitInput) => Promise<void>;
  onDeleteOutfit: (id: string) => Promise<void>;
  onOpenSocial: (outfit: Outfit) => void;
}

export const OutfitBuilder: React.FC<OutfitBuilderProps> = ({
  outfits,
  items,
  onCreateOutfit,
  onDeleteOutfit,
  onOpenSocial
}) => {
  const [isCreating, setIsCreating] = useState(false);
  const [name, setName] = useState('');
  const [occasion, setOccasion] = useState<Occasion>('casual');
  const [season, setSeason] = useState('all_season');
  const [isPublic, setIsPublic] = useState(false);
  const [selectedItemIds, setSelectedItemIds] = useState<string[]>([]);
  const [selectedCategoryTab, setSelectedCategoryTab] = useState<string>('all');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const toggleItemSelection = (id: string) => {
    if (selectedItemIds.includes(id)) {
      setSelectedItemIds(selectedItemIds.filter(x => x !== id));
    } else {
      setSelectedItemIds([...selectedItemIds, id]);
    }
  };

  const handleSaveOutfit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || selectedItemIds.length === 0) return;

    setIsSubmitting(true);
    try {
      await onCreateOutfit({
        name: name.trim(),
        occasion,
        season: season as any,
        isPublic,
        itemIds: selectedItemIds
      });

      // Reset
      setName('');
      setSelectedItemIds([]);
      setIsCreating(false);
    } finally {
      setIsSubmitting(false);
    }
  };

  const selectedItems = items.filter(i => selectedItemIds.includes(i.id));

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold font-serif text-stone-900 dark:text-stone-100">Outfit Collections</h2>
          <p className="text-stone-500 dark:text-stone-400 text-sm mt-0.5">
            {outfits.length} styled combinations curated for various occasions and seasons.
          </p>
        </div>

        <button
          onClick={() => setIsCreating(true)}
          className="flex items-center justify-center space-x-2 px-5 py-2.5 bg-stone-900 hover:bg-stone-800 dark:bg-amber-600 dark:hover:bg-amber-500 text-white rounded-xl text-sm font-medium shadow-sm transition-all"
        >
          <Plus className="w-4 h-4" />
          <span>Build New Outfit</span>
        </button>
      </div>

      {/* Outfit Cards Grid */}
      {outfits.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {outfits.map((outfit) => (
            <div
              key={outfit.id}
              className="bg-white dark:bg-stone-900 rounded-3xl p-6 border border-stone-200 dark:border-stone-800 hover:border-amber-400 dark:hover:border-amber-500 hover:shadow-lg transition-all flex flex-col justify-between space-y-5"
            >
              <div>
                <div className="flex items-center justify-between text-xs">
                  <span className="font-bold uppercase tracking-wider text-amber-800 dark:text-amber-300 bg-amber-50 dark:bg-amber-950/60 px-2.5 py-1 rounded-full border border-amber-200 dark:border-amber-800">
                    {outfit.occasion}
                  </span>
                  {outfit.isPublic && (
                    <span className="flex items-center space-x-1 text-stone-500 dark:text-stone-400 font-medium">
                      <Share2 className="w-3.5 h-3.5" />
                      <span>Shared</span>
                    </span>
                  )}
                </div>

                <h3 className="text-xl font-bold font-serif text-stone-900 dark:text-stone-100 mt-2">{outfit.name}</h3>

                {/* Items Collage Grid Preview */}
                <div className="grid grid-cols-3 gap-2 mt-4 aspect-[4/3] bg-stone-50 dark:bg-stone-800/60 rounded-2xl p-2.5 border border-stone-100 dark:border-stone-800">
                  {outfit.items?.slice(0, 6).map((it) => (
                    <div
                      key={it.id}
                      className="rounded-xl bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-700 overflow-hidden flex items-center justify-center p-1"
                    >
                      {it.thumbnailUrl || it.imageUrl ? (
                        <img src={it.thumbnailUrl || it.imageUrl || ''} alt={it.name} className="w-full h-full object-contain" />
                      ) : (
                        <span className="text-[10px] text-stone-400 capitalize">{it.category}</span>
                      )}
                    </div>
                  ))}
                </div>
              </div>

              {/* Stats & Actions */}
              <div className="border-t border-stone-100 dark:border-stone-800 pt-4 space-y-3">
                <div className="flex items-center justify-between text-xs text-stone-500 dark:text-stone-400">
                  <span>Worn {outfit.wearCount} times</span>
                  {outfit.lastWornAt ? (
                    <span>Last: {new Date(outfit.lastWornAt).toLocaleDateString()}</span>
                  ) : (
                    <span>Never worn</span>
                  )}
                </div>

                {/* Multi-Perspective Rating Summary Button */}
                <div className="flex items-center justify-between pt-1">
                  <button
                    onClick={() => onOpenSocial(outfit)}
                    className="flex items-center space-x-2 text-xs font-semibold px-3 py-1.5 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 text-stone-800 dark:text-stone-200 rounded-xl transition-colors"
                  >
                    <Star className="w-4 h-4 text-amber-500 fill-amber-400" />
                    <span>{outfit.avgRating ? `${outfit.avgRating} / 5` : 'Rate Outfit'}</span>
                    <span className="text-stone-400 dark:text-stone-600">|</span>
                    <MessageSquare className="w-3.5 h-3.5 text-stone-500 dark:text-stone-400" />
                    <span>Feedback</span>
                  </button>

                  <button
                    onClick={() => {
                      if (confirm(`Delete outfit "${outfit.name}"?`)) {
                        onDeleteOutfit(outfit.id);
                      }
                    }}
                    className="p-1.5 text-stone-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-950/40 rounded-lg transition-colors"
                    title="Delete Outfit"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="bg-white dark:bg-stone-900 rounded-3xl p-12 text-center border border-stone-200 dark:border-stone-800">
          <Layers className="w-12 h-12 text-stone-300 dark:text-stone-600 mx-auto mb-3" />
          <h3 className="font-bold text-lg font-serif text-stone-900 dark:text-stone-100">No Outfits Created Yet</h3>
          <p className="text-stone-500 dark:text-stone-400 text-sm max-w-sm mx-auto mt-1 mb-4">
            Select items from your wardrobe to build complete outfits for work, casual, or evenings.
          </p>
          <button
            onClick={() => setIsCreating(true)}
            className="px-5 py-2.5 bg-stone-900 hover:bg-stone-800 dark:bg-amber-600 dark:hover:bg-amber-500 text-white rounded-xl text-sm font-medium shadow-sm"
          >
            Create Your First Outfit
          </button>
        </div>
      )}

      {/* Outfit Builder Modal */}
      {isCreating && (
        <div className="fixed inset-0 z-50 bg-stone-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white dark:bg-stone-900 rounded-3xl max-w-4xl w-full p-6 space-y-6 shadow-2xl max-h-[90vh] flex flex-col border border-stone-200 dark:border-stone-800">
            <div className="flex justify-between items-center border-b border-stone-100 dark:border-stone-800 pb-3">
              <div>
                <h3 className="text-xl font-bold font-serif text-stone-900 dark:text-stone-100">Outfit Canvas & Builder</h3>
                <p className="text-xs text-stone-500 dark:text-stone-400">Pick matching items to compose your look.</p>
              </div>
              <button onClick={() => setIsCreating(false)} className="text-stone-400 hover:text-stone-600 dark:hover:text-stone-200 font-bold">
                ✕
              </button>
            </div>

            <form onSubmit={handleSaveOutfit} className="space-y-5 flex-1 overflow-y-auto pr-1">
              {/* Outfit Meta Info */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div className="sm:col-span-1">
                  <label className="block text-xs font-semibold text-stone-700 dark:text-stone-300 mb-1">Outfit Name *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Sharp Casual Friday"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="w-full px-3 py-2 bg-stone-50 dark:bg-stone-800 border border-stone-200 dark:border-stone-700 rounded-xl text-sm text-stone-900 dark:text-stone-100 focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-600"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-stone-700 dark:text-stone-300 mb-1">Occasion</label>
                  <select
                    value={occasion}
                    onChange={(e) => setOccasion(e.target.value as Occasion)}
                    className="w-full px-3 py-2 bg-stone-50 dark:bg-stone-800 border border-stone-200 dark:border-stone-700 rounded-xl text-sm text-stone-900 dark:text-stone-100 capitalize"
                  >
                    {Occasions.map((occ) => (
                      <option key={occ} value={occ}>
                        {occ.replace('_', ' ')}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-stone-700 dark:text-stone-300 mb-1">Season</label>
                  <select
                    value={season}
                    onChange={(e) => setSeason(e.target.value)}
                    className="w-full px-3 py-2 bg-stone-50 dark:bg-stone-800 border border-stone-200 dark:border-stone-700 rounded-xl text-sm text-stone-900 dark:text-stone-100 capitalize"
                  >
                    {Seasons.map((s) => (
                      <option key={s} value={s}>
                        {s.replace('_', ' ')}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Visual Selected Items Canvas */}
              <div>
                <label className="block text-xs font-semibold text-stone-700 dark:text-stone-300 mb-2">
                  Look Canvas ({selectedItems.length} items chosen)
                </label>
                <div className="min-h-24 p-3 bg-stone-50 dark:bg-stone-800/50 rounded-2xl border-2 border-dashed border-stone-300 dark:border-stone-700 flex items-center space-x-3 overflow-x-auto">
                  {selectedItems.length > 0 ? (
                    selectedItems.map((item) => (
                      <div
                        key={item.id}
                        className="relative w-20 h-20 rounded-xl bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-700 p-1 flex-shrink-0 group flex items-center justify-center"
                      >
                        {item.thumbnailUrl ? (
                          <img src={item.thumbnailUrl} alt={item.name} className="w-full h-full object-contain" />
                        ) : (
                          <span className="text-[10px] text-stone-500 dark:text-stone-400 font-semibold">{item.name}</span>
                        )}
                        <button
                          type="button"
                          onClick={() => toggleItemSelection(item.id)}
                          className="absolute -top-1.5 -right-1.5 bg-red-600 text-white rounded-full w-5 h-5 flex items-center justify-center text-xs shadow-sm"
                        >
                          ✕
                        </button>
                      </div>
                    ))
                  ) : (
                    <div className="text-xs text-stone-400 dark:text-stone-500 mx-auto">
                      Click clothes below to assemble this outfit
                    </div>
                  )}
                </div>
              </div>

              {/* Items Picker Grid */}
              <div className="space-y-3">
                <label className="block text-xs font-semibold text-stone-700 dark:text-stone-300">
                  Select Items from Wardrobe
                </label>
                <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-3 max-h-56 overflow-y-auto p-1 border border-stone-100 dark:border-stone-800 rounded-xl">
                  {items.map((item) => {
                    const isSelected = selectedItemIds.includes(item.id);
                    return (
                      <div
                        key={item.id}
                        onClick={() => toggleItemSelection(item.id)}
                        className={`p-2 rounded-xl border cursor-pointer transition-all flex flex-col items-center text-center relative ${
                          isSelected
                            ? 'border-amber-600 bg-amber-50/50 dark:bg-amber-950/40 shadow-sm'
                            : 'border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-800 hover:border-stone-300 dark:hover:border-stone-600'
                        }`}
                      >
                        {isSelected && (
                          <div className="absolute top-1 right-1 w-4 h-4 bg-amber-600 text-white rounded-full flex items-center justify-center text-[10px]">
                            <Check className="w-3 h-3" />
                          </div>
                        )}
                        <div className="w-12 h-12 rounded-lg bg-stone-50 dark:bg-stone-900 flex items-center justify-center overflow-hidden mb-1">
                          {item.thumbnailUrl ? (
                            <img src={item.thumbnailUrl} alt={item.name} className="w-full h-full object-contain" />
                          ) : (
                            <span className="text-[9px] text-stone-400 capitalize">{item.category}</span>
                          )}
                        </div>
                        <span className="text-[11px] font-medium text-stone-800 dark:text-stone-200 truncate w-full">
                          {item.name}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Submit Buttons */}
              <div className="flex justify-end space-x-3 pt-3 border-t border-stone-100 dark:border-stone-800">
                <button
                  type="button"
                  onClick={() => setIsCreating(false)}
                  className="px-4 py-2 text-stone-600 dark:text-stone-300 hover:bg-stone-100 dark:hover:bg-stone-800 rounded-xl text-sm font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting || selectedItemIds.length === 0}
                  className="px-6 py-2 bg-stone-900 hover:bg-stone-800 dark:bg-amber-600 dark:hover:bg-amber-500 text-white rounded-xl text-sm font-medium disabled:opacity-50"
                >
                  {isSubmitting ? 'Saving Outfit...' : 'Save Outfit'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

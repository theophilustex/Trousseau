import React, { useState } from 'react';
import {
  Plus,
  Search,
  Filter,
  WashingMachine,
  Sparkles,
  Trash2,
  Edit2,
  Tag,
  CheckCircle2,
  AlertTriangle,
  Upload,
  Layers,
  ThermometerSun,
  CloudRain
} from 'lucide-react';
import {
  type ClothingItem,
  type ClothingCategory,
  type CreateClothingItemInput,
  ClothingCategories,
  Seasons,
  DEFAULT_MAX_WEARS
} from '@trousseau/shared';

interface WardrobeCatalogProps {
  items: ClothingItem[];
  onAddItem: (input: CreateClothingItemInput, file?: File, removeBg?: boolean) => Promise<void>;
  onDeleteItem: (id: string) => Promise<void>;
  onWashItem: (id: string) => Promise<void>;
  onRefresh: () => Promise<void>;
}

export const WardrobeCatalog: React.FC<WardrobeCatalogProps> = ({
  items,
  onAddItem,
  onDeleteItem,
  onWashItem
}) => {
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [selectedStatus, setSelectedStatus] = useState<string>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [activeItemDetail, setActiveItemDetail] = useState<ClothingItem | null>(null);

  // Form states for adding item
  const [name, setName] = useState('');
  const [category, setCategory] = useState<ClothingCategory>('tops');
  const [brand, setBrand] = useState('');
  const [size, setSize] = useState('');
  const [colorInput, setColorInput] = useState('#333333');
  const [colors, setColors] = useState<string[]>([]);
  const [tagInput, setTagInput] = useState('');
  const [tags, setTags] = useState<string[]>([]);
  const [maxWears, setMaxWears] = useState(DEFAULT_MAX_WEARS['tops']);
  const [rainFriendly, setRainFriendly] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [removeBackground, setRemoveBackground] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleCategoryChange = (cat: ClothingCategory) => {
    setCategory(cat);
    setMaxWears(DEFAULT_MAX_WEARS[cat]);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      setPreviewUrl(URL.createObjectURL(file));
    }
  };

  const handleAddColor = () => {
    if (colorInput && !colors.includes(colorInput)) {
      setColors([...colors, colorInput]);
    }
  };

  const handleAddTag = () => {
    if (tagInput.trim() && !tags.includes(tagInput.trim())) {
      setTags([...tags, tagInput.trim()]);
      setTagInput('');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    setIsSubmitting(true);
    try {
      await onAddItem(
        {
          name: name.trim(),
          category,
          brand: brand.trim() || undefined,
          size: size.trim() || undefined,
          colors,
          tags,
          maxWearsBeforeWash: maxWears,
          rainFriendly
        },
        selectedFile || undefined,
        removeBackground
      );

      // Reset form
      setName('');
      setBrand('');
      setSize('');
      setColors([]);
      setTags([]);
      setSelectedFile(null);
      setPreviewUrl(null);
      setRemoveBackground(false);
      setIsAddModalOpen(false);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Filter items
  const filteredItems = items.filter((item) => {
    if (selectedCategory !== 'all' && item.category !== selectedCategory) return false;
    if (selectedStatus === 'clean' && item.status !== 'clean') return false;
    if (selectedStatus === 'needs_wash' && item.status !== 'needs_wash') return false;
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchName = item.name.toLowerCase().includes(q);
      const matchBrand = item.brand?.toLowerCase().includes(q);
      const matchTags = item.tags?.some(t => t.toLowerCase().includes(q));
      if (!matchName && !matchBrand && !matchTags) return false;
    }
    return true;
  });

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6">
      {/* Top Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold font-serif text-stone-900">Wardrobe Inventory</h2>
          <p className="text-stone-500 text-sm mt-0.5">
            {items.length} clothing items cataloged with wear and wash tracking.
          </p>
        </div>

        <button
          onClick={() => setIsAddModalOpen(true)}
          className="flex items-center justify-center space-x-2 px-5 py-2.5 bg-stone-900 hover:bg-stone-800 text-white rounded-xl text-sm font-medium shadow-sm transition-all"
        >
          <Plus className="w-4 h-4" />
          <span>Add Clothing Item</span>
        </button>
      </div>

      {/* Filter Toolbar */}
      <div className="bg-white rounded-2xl p-4 border border-stone-200 shadow-sm space-y-4">
        <div className="flex flex-col sm:flex-row gap-3">
          {/* Search bar */}
          <div className="relative flex-1">
            <Search className="w-4 h-4 absolute left-3.5 top-3 text-stone-400" />
            <input
              type="text"
              placeholder="Search by item name, brand, or tag..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-stone-50 border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-stone-900/10 focus:border-stone-900"
            />
          </div>

          {/* Cleanliness Status filter */}
          <div className="flex items-center space-x-2">
            <button
              onClick={() => setSelectedStatus('all')}
              className={`px-3 py-2 rounded-xl text-xs font-semibold transition-colors ${
                selectedStatus === 'all' ? 'bg-stone-900 text-white' : 'bg-stone-100 text-stone-600 hover:bg-stone-200'
              }`}
            >
              All Status
            </button>
            <button
              onClick={() => setSelectedStatus('clean')}
              className={`px-3 py-2 rounded-xl text-xs font-semibold transition-colors ${
                selectedStatus === 'clean' ? 'bg-emerald-600 text-white' : 'bg-emerald-50 text-emerald-800 hover:bg-emerald-100'
              }`}
            >
              Clean Only
            </button>
            <button
              onClick={() => setSelectedStatus('needs_wash')}
              className={`px-3 py-2 rounded-xl text-xs font-semibold transition-colors ${
                selectedStatus === 'needs_wash' ? 'bg-red-600 text-white' : 'bg-red-50 text-red-800 hover:bg-red-100'
              }`}
            >
              Needs Wash
            </button>
          </div>
        </div>

        {/* Categories Bar */}
        <div className="flex items-center space-x-2 overflow-x-auto pb-1 scrollbar-none">
          <button
            onClick={() => setSelectedCategory('all')}
            className={`px-3.5 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-colors ${
              selectedCategory === 'all'
                ? 'bg-amber-100 text-amber-900 border border-amber-300 font-bold'
                : 'bg-stone-100 text-stone-600 hover:bg-stone-200'
            }`}
          >
            All Items ({items.length})
          </button>
          {ClothingCategories.map((cat) => {
            const count = items.filter(i => i.category === cat).length;
            return (
              <button
                key={cat}
                onClick={() => setSelectedCategory(cat)}
                className={`px-3.5 py-1.5 rounded-full text-xs font-medium whitespace-nowrap capitalize transition-colors ${
                  selectedCategory === cat
                    ? 'bg-amber-100 text-amber-900 border border-amber-300 font-bold'
                    : 'bg-stone-100 text-stone-600 hover:bg-stone-200'
                }`}
              >
                {cat.replace('_', ' ')} ({count})
              </button>
            );
          })}
        </div>
      </div>

      {/* Grid of Items */}
      {filteredItems.length > 0 ? (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
          {filteredItems.map((item) => {
            const isDirty = item.status === 'needs_wash' || item.wearsSinceWash >= item.maxWearsBeforeWash;
            return (
              <div
                key={item.id}
                onClick={() => setActiveItemDetail(item)}
                className="bg-white rounded-2xl p-3 border border-stone-200 hover:border-amber-400 hover:shadow-md cursor-pointer transition-all flex flex-col justify-between group"
              >
                {/* Photo container */}
                <div className="relative w-full aspect-square rounded-xl bg-stone-50 border border-stone-100 overflow-hidden flex items-center justify-center mb-3">
                  {item.thumbnailUrl || item.imageUrl ? (
                    <img
                      src={item.thumbnailUrl || item.imageUrl || ''}
                      alt={item.name}
                      className="w-full h-full object-contain p-2 group-hover:scale-105 transition-transform"
                    />
                  ) : (
                    <div className="text-stone-300 text-xs font-medium">No Image</div>
                  )}

                  {/* Status Badge overlay */}
                  <div className="absolute top-2 left-2">
                    {isDirty ? (
                      <span className="flex items-center space-x-1 text-[10px] font-bold bg-red-600 text-white px-2 py-0.5 rounded-full shadow-sm">
                        <AlertTriangle className="w-2.5 h-2.5" />
                        <span>Needs Wash</span>
                      </span>
                    ) : (
                      <span className="text-[10px] font-semibold bg-emerald-600 text-white px-2 py-0.5 rounded-full shadow-sm">
                        Clean
                      </span>
                    )}
                  </div>

                  {item.hasTransparentBg && (
                    <span className="absolute bottom-2 right-2 text-[9px] bg-stone-900/70 text-white px-1.5 py-0.5 rounded backdrop-blur-sm">
                      Transparent
                    </span>
                  )}
                </div>

                {/* Details */}
                <div>
                  <span className="text-[10px] uppercase font-bold text-stone-400 tracking-wider">
                    {item.category}
                  </span>
                  <h4 className="font-semibold text-stone-900 text-sm truncate">{item.name}</h4>
                  {item.brand && <p className="text-xs text-stone-500 truncate">{item.brand}</p>}

                  {/* Wears counter meter */}
                  <div className="mt-2 pt-2 border-t border-stone-100 flex items-center justify-between text-xs">
                    <span className="text-stone-500 text-[11px]">Wears:</span>
                    <span className={`font-mono font-bold text-xs ${isDirty ? 'text-red-600' : 'text-stone-800'}`}>
                      {item.wearsSinceWash} / {item.maxWearsBeforeWash}
                    </span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="bg-white rounded-3xl p-12 text-center border border-stone-200">
          <Layers className="w-10 h-10 text-stone-300 mx-auto mb-3" />
          <h3 className="font-bold text-stone-800 font-serif">No clothes match your criteria</h3>
          <p className="text-stone-500 text-xs mt-1">Try resetting the filters or add a new piece to your wardrobe.</p>
        </div>
      )}

      {/* Item Detail Modal */}
      {activeItemDetail && (
        <div className="fixed inset-0 z-50 bg-stone-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 space-y-5 shadow-2xl">
            <div className="flex justify-between items-start border-b border-stone-100 pb-3">
              <div>
                <span className="text-xs font-bold uppercase tracking-wider text-amber-700">
                  {activeItemDetail.category}
                </span>
                <h3 className="text-xl font-bold font-serif text-stone-900">{activeItemDetail.name}</h3>
                {activeItemDetail.brand && <p className="text-xs text-stone-500">{activeItemDetail.brand}</p>}
              </div>
              <button
                onClick={() => setActiveItemDetail(null)}
                className="text-stone-400 hover:text-stone-600 font-bold"
              >
                ✕
              </button>
            </div>

            {/* Image Preview */}
            <div className="w-full h-56 rounded-2xl bg-stone-50 border border-stone-200 overflow-hidden flex items-center justify-center">
              {activeItemDetail.imageUrl ? (
                <img
                  src={activeItemDetail.imageUrl}
                  alt={activeItemDetail.name}
                  className="w-full h-full object-contain p-2"
                />
              ) : (
                <div className="text-stone-300 text-sm">No photo available</div>
              )}
            </div>

            {/* Stats */}
            <div className="grid grid-cols-2 gap-3 text-xs bg-stone-50 p-4 rounded-2xl border border-stone-200">
              <div>
                <span className="text-stone-500">Wears Since Wash:</span>
                <div className="font-mono font-bold text-sm text-stone-900">
                  {activeItemDetail.wearsSinceWash} / {activeItemDetail.maxWearsBeforeWash}
                </div>
              </div>
              <div>
                <span className="text-stone-500">Lifetime Wears:</span>
                <div className="font-mono font-bold text-sm text-stone-900">
                  {activeItemDetail.totalLifetimeWears}
                </div>
              </div>
              <div>
                <span className="text-stone-500">Total Washes:</span>
                <div className="font-mono font-bold text-sm text-stone-900">
                  {activeItemDetail.totalWashes}
                </div>
              </div>
              <div>
                <span className="text-stone-500">Rain Friendly:</span>
                <div className="font-semibold text-stone-900">
                  {activeItemDetail.rainFriendly ? 'Yes ☔' : 'No'}
                </div>
              </div>
            </div>

            {/* Actions */}
            <div className="flex space-x-3 pt-2">
              <button
                onClick={async () => {
                  await onWashItem(activeItemDetail.id);
                  setActiveItemDetail(null);
                }}
                className="flex-1 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-semibold flex items-center justify-center space-x-1 shadow-sm"
              >
                <WashingMachine className="w-4 h-4" />
                <span>Mark Clean / Washed</span>
              </button>

              <button
                onClick={async () => {
                  if (confirm(`Delete "${activeItemDetail.name}" from your wardrobe?`)) {
                    await onDeleteItem(activeItemDetail.id);
                    setActiveItemDetail(null);
                  }
                }}
                className="p-2.5 text-stone-400 hover:text-red-600 hover:bg-red-50 border border-stone-200 rounded-xl transition-colors"
                title="Delete Item"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Add Item Modal */}
      {isAddModalOpen && (
        <div className="fixed inset-0 z-50 bg-stone-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 space-y-5 shadow-2xl max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center border-b border-stone-100 pb-3">
              <h3 className="text-xl font-bold font-serif text-stone-900">Add Clothing Item</h3>
              <button
                onClick={() => setIsAddModalOpen(false)}
                className="text-stone-400 hover:text-stone-600 font-bold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Photo Upload with Local Background Removal Option */}
              <div>
                <label className="block text-xs font-semibold text-stone-700 mb-1">
                  Item Photo
                </label>
                <div className="border-2 border-dashed border-stone-300 hover:border-amber-500 rounded-2xl p-4 text-center cursor-pointer transition-colors relative">
                  <input
                    type="file"
                    accept="image/*"
                    onChange={handleFileChange}
                    className="absolute inset-0 opacity-0 cursor-pointer w-full h-full"
                  />
                  {previewUrl ? (
                    <div className="h-32 flex items-center justify-center">
                      <img src={previewUrl} alt="Preview" className="h-full object-contain" />
                    </div>
                  ) : (
                    <div className="space-y-1">
                      <Upload className="w-7 h-7 text-stone-400 mx-auto" />
                      <div className="text-xs font-medium text-stone-700">Click or drag photo here</div>
                      <div className="text-[10px] text-stone-400">JPG, PNG, WebP</div>
                    </div>
                  )}
                </div>

                {/* Local Background Removal Toggle */}
                <div className="mt-2.5 flex items-center justify-between p-2.5 bg-amber-50/70 rounded-xl border border-amber-200">
                  <div className="flex items-center space-x-2">
                    <Sparkles className="w-4 h-4 text-amber-600" />
                    <div>
                      <div className="text-xs font-bold text-amber-950">Remove Background (Local AI)</div>
                      <div className="text-[10px] text-amber-800">100% private, processed directly on your host</div>
                    </div>
                  </div>
                  <input
                    type="checkbox"
                    checked={removeBackground}
                    onChange={(e) => setRemoveBackground(e.target.checked)}
                    className="w-4 h-4 rounded text-amber-600 focus:ring-amber-500"
                  />
                </div>
              </div>

              {/* Name & Category */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-stone-700 mb-1">Item Name *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Oxford Cotton Shirt"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="w-full px-3 py-2 bg-stone-50 border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-stone-900/10 focus:border-stone-900"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-stone-700 mb-1">Category *</label>
                  <select
                    value={category}
                    onChange={(e) => handleCategoryChange(e.target.value as ClothingCategory)}
                    className="w-full px-3 py-2 bg-stone-50 border border-stone-200 rounded-xl text-sm capitalize focus:outline-none focus:ring-2 focus:ring-stone-900/10 focus:border-stone-900"
                  >
                    {ClothingCategories.map((cat) => (
                      <option key={cat} value={cat}>
                        {cat.replace('_', ' ')}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Brand & Size */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-stone-700 mb-1">Brand</label>
                  <input
                    type="text"
                    placeholder="e.g. Uniqlo, Levi's"
                    value={brand}
                    onChange={(e) => setBrand(e.target.value)}
                    className="w-full px-3 py-2 bg-stone-50 border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-stone-900/10 focus:border-stone-900"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-stone-700 mb-1">Size</label>
                  <input
                    type="text"
                    placeholder="e.g. M, 32/30, 10.5"
                    value={size}
                    onChange={(e) => setSize(e.target.value)}
                    className="w-full px-3 py-2 bg-stone-50 border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-stone-900/10 focus:border-stone-900"
                  />
                </div>
              </div>

              {/* Wash Limit Threshold */}
              <div>
                <label className="block text-xs font-semibold text-stone-700 mb-1">
                  Wears Before Wash (Automatic laundry trigger)
                </label>
                <div className="flex items-center space-x-3">
                  <input
                    type="number"
                    min="1"
                    max="50"
                    value={maxWears}
                    onChange={(e) => setMaxWears(Number(e.target.value))}
                    className="w-24 px-3 py-2 bg-stone-50 border border-stone-200 rounded-xl text-sm font-mono focus:outline-none focus:ring-2 focus:ring-stone-900/10 focus:border-stone-900"
                  />
                  <span className="text-xs text-stone-500">
                    Category default for {category}: {DEFAULT_MAX_WEARS[category]} wears
                  </span>
                </div>
              </div>

              {/* Rain Friendly */}
              <div className="flex items-center space-x-2">
                <input
                  type="checkbox"
                  id="rainFriendly"
                  checked={rainFriendly}
                  onChange={(e) => setRainFriendly(e.target.checked)}
                  className="w-4 h-4 rounded text-stone-900 focus:ring-stone-900"
                />
                <label htmlFor="rainFriendly" className="text-xs font-medium text-stone-700">
                  Rain Friendly (water-resistant footwear/jacket)
                </label>
              </div>

              {/* Tags */}
              <div>
                <label className="block text-xs font-semibold text-stone-700 mb-1">Custom Tags</label>
                <div className="flex space-x-2">
                  <input
                    type="text"
                    placeholder="e.g. linen, casual, office"
                    value={tagInput}
                    onChange={(e) => setTagInput(e.target.value)}
                    onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); handleAddTag(); }}}
                    className="flex-1 px-3 py-2 bg-stone-50 border border-stone-200 rounded-xl text-sm"
                  />
                  <button
                    type="button"
                    onClick={handleAddTag}
                    className="px-3 py-2 bg-stone-200 hover:bg-stone-300 text-stone-800 rounded-xl text-xs font-semibold"
                  >
                    Add
                  </button>
                </div>
                {tags.length > 0 && (
                  <div className="flex flex-wrap gap-1.5 mt-2">
                    {tags.map((t) => (
                      <span key={t} className="text-xs bg-stone-100 text-stone-700 px-2 py-0.5 rounded-full flex items-center space-x-1">
                        <span>#{t}</span>
                        <button type="button" onClick={() => setTags(tags.filter(x => x !== t))} className="text-stone-400 hover:text-stone-700">✕</button>
                      </span>
                    ))}
                  </div>
                )}
              </div>

              {/* Buttons */}
              <div className="flex justify-end space-x-3 pt-4 border-t border-stone-100">
                <button
                  type="button"
                  onClick={() => setIsAddModalOpen(false)}
                  className="px-4 py-2 text-stone-600 hover:bg-stone-100 rounded-xl text-sm font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="px-6 py-2 bg-stone-900 hover:bg-stone-800 text-white rounded-xl text-sm font-medium disabled:opacity-50"
                >
                  {isSubmitting ? 'Saving Item...' : 'Save Clothing Item'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

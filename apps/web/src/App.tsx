import React, { useState, useEffect } from 'react';
import {
  type User,
  type ClothingItem,
  type Outfit,
  type WearLog,
  type WeatherForecast,
  type OutfitRecommendation,
  type CreateClothingItemInput,
  type CreateOutfitInput
} from '@trousseau/shared';
import { ApiClient } from './api/client.js';
import { Navbar } from './components/Navbar.js';
import { TodayPlanner } from './components/TodayPlanner.js';
import { WardrobeCatalog } from './components/WardrobeCatalog.js';
import { OutfitBuilder } from './components/OutfitBuilder.js';
import { LaundryManager } from './components/LaundryManager.js';
import { SocialFeedbackModal } from './components/SocialFeedbackModal.js';
import { BackupModal } from './components/BackupModal.js';
import { AuthModal } from './components/AuthModal.js';

export const App: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [isAuthChecking, setIsAuthChecking] = useState(true);

  // App data
  const [currentTab, setCurrentTab] = useState<string>('today');
  const [items, setItems] = useState<ClothingItem[]>([]);
  const [outfits, setOutfits] = useState<Outfit[]>([]);
  const [todayLog, setTodayLog] = useState<WearLog | null>(null);
  const [weather, setWeather] = useState<WeatherForecast | null>(null);
  const [recommendations, setRecommendations] = useState<OutfitRecommendation[]>([]);

  // Modals
  const [activeSocialOutfit, setActiveSocialOutfit] = useState<Outfit | null>(null);
  const [isBackupOpen, setIsBackupOpen] = useState(false);

  // User & preferences
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('trousseau_theme');
      if (saved === 'dark' || saved === 'light') return saved;
      if (window.matchMedia('(prefers-color-scheme: dark)').matches) return 'dark';
    }
    return 'light';
  });

  const [tempUnit, setTempUnit] = useState<import('@trousseau/shared').TemperatureUnit>(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('trousseau_temp_unit');
      if (saved === 'C' || saved === 'F') return saved;
    }
    return 'C';
  });

  useEffect(() => {
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
    localStorage.setItem('trousseau_theme', theme);
  }, [theme]);

  useEffect(() => {
    localStorage.setItem('trousseau_temp_unit', tempUnit);
  }, [tempUnit]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === 'light' ? 'dark' : 'light'));
  };

  const toggleTempUnit = () => {
    setTempUnit((prev) => (prev === 'C' ? 'F' : 'C'));
  };

  // Initial session check
  useEffect(() => {
    checkAuth();
  }, []);

  const checkAuth = async () => {
    setIsAuthChecking(true);
    try {
      if (ApiClient.getToken()) {
        const res = await ApiClient.getMe();
        setUser(res.user);
        await loadAllData();
      }
    } catch {
      setUser(null);
    } finally {
      setIsAuthChecking(false);
    }
  };

  const loadAllData = async () => {
    try {
      const [itemsRes, outfitsRes, todayRes, weatherRes, recsRes] = await Promise.all([
        ApiClient.getItems(),
        ApiClient.getOutfits(),
        ApiClient.getTodayLog(),
        ApiClient.getWeather().catch(() => ({
          forecast: {
            date: new Date().toISOString().split('T')[0],
            tempCurrentC: 20,
            tempHighC: 23,
            tempLowC: 16,
            precipitationProbability: 0,
            weatherCode: 1,
            description: 'Pleasant & clear',
            isRaining: false
          }
        })),
        ApiClient.getRecommendations()
      ]);

      setItems(itemsRes.items);
      setOutfits(outfitsRes.outfits);
      setTodayLog(todayRes.log);
      setWeather(weatherRes.forecast);
      setRecommendations(recsRes.recommendations);
    } catch (err) {
      console.error('Error loading data:', err);
    }
  };

  const handleAddItem = async (input: CreateClothingItemInput, file?: File, removeBg?: boolean) => {
    let imageUrl = input.imageUrl;
    let thumbnailUrl = input.thumbnailUrl;
    let hasTransparentBg = false;

    if (file) {
      const uploadRes = await ApiClient.uploadPhoto(file, removeBg);
      imageUrl = uploadRes.imageUrl;
      thumbnailUrl = uploadRes.thumbnailUrl;
      hasTransparentBg = uploadRes.hasTransparentBg;
    }

    await ApiClient.createItem({
      ...input,
      imageUrl,
      thumbnailUrl,
      hasTransparentBg
    });

    await loadAllData();
  };

  const handleDeleteItem = async (id: string) => {
    await ApiClient.deleteItem(id);
    await loadAllData();
  };

  const handleWashItem = async (id: string) => {
    await ApiClient.washItem(id);
    await loadAllData();
  };

  const handleWashAllDirty = async () => {
    await ApiClient.washAllDirty();
    await loadAllData();
  };

  const handleCreateOutfit = async (input: CreateOutfitInput) => {
    await ApiClient.createOutfit(input);
    await loadAllData();
  };

  const handleDeleteOutfit = async (id: string) => {
    await ApiClient.deleteOutfit(id);
    await loadAllData();
  };

  const handleConfirmTodayWear = async (status: 'worn' | 'skipped', replacementOutfitId?: string) => {
    if (todayLog) {
      await ApiClient.confirmWear(todayLog.id, {
        status,
        replacementOutfitId
      });
    } else {
      // Find default or replacement outfit
      const targetOutfitId = replacementOutfitId || outfits[0]?.id;
      if (targetOutfitId) {
        const targetOutfit = outfits.find(o => o.id === targetOutfitId);
        if (targetOutfit) {
          const newLog = await ApiClient.logWear({
            outfitId: targetOutfit.id,
            itemIds: targetOutfit.itemIds,
            date: new Date().toISOString().split('T')[0],
            status: 'worn'
          });
          setTodayLog(newLog.log);
        }
      }
    }
    await loadAllData();
  };

  const handlePlanOutfit = async (outfitId: string) => {
    const outfit = outfits.find(o => o.id === outfitId);
    if (!outfit) return;

    const res = await ApiClient.logWear({
      outfitId,
      itemIds: outfit.itemIds,
      date: new Date().toISOString().split('T')[0],
      status: 'planned'
    });
    setTodayLog(res.log);
  };

  const handleLogout = () => {
    ApiClient.clearToken();
    setUser(null);
  };

  if (isAuthChecking) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-stone-50 dark:bg-stone-950 transition-colors">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-stone-900 dark:border-stone-100" />
      </div>
    );
  }

  if (!user) {
    return (
      <AuthModal
        onSuccess={(loggedUser) => {
          setUser(loggedUser);
          loadAllData();
        }}
      />
    );
  }

  const dirtyCount = items.filter(
    (i) => i.status === 'needs_wash' || i.wearsSinceWash >= i.maxWearsBeforeWash
  ).length;

  return (
    <div className="min-h-screen bg-stone-50 dark:bg-stone-950 text-stone-900 dark:text-stone-100 flex flex-col transition-colors duration-200">
      <Navbar
        currentTab={currentTab}
        onTabChange={setCurrentTab}
        user={user}
        weather={weather}
        laundryCount={dirtyCount}
        theme={theme}
        onToggleTheme={toggleTheme}
        tempUnit={tempUnit}
        onToggleTempUnit={toggleTempUnit}
        onOpenBackup={() => setIsBackupOpen(true)}
        onLogout={handleLogout}
      />

      <main className="flex-1 pb-16">
        {currentTab === 'today' && (
          <TodayPlanner
            weather={weather}
            todayLog={todayLog}
            recommendations={recommendations}
            allOutfits={outfits}
            tempUnit={tempUnit}
            onConfirmTodayWear={handleConfirmTodayWear}
            onPlanOutfit={handlePlanOutfit}
            onGoToOutfitBuilder={() => setCurrentTab('outfits')}
            onGoToWardrobe={() => setCurrentTab('wardrobe')}
          />
        )}

        {currentTab === 'wardrobe' && (
          <WardrobeCatalog
            items={items}
            tempUnit={tempUnit}
            onAddItem={handleAddItem}
            onDeleteItem={handleDeleteItem}
            onWashItem={handleWashItem}
            onRefresh={loadAllData}
          />
        )}

        {currentTab === 'outfits' && (
          <OutfitBuilder
            outfits={outfits}
            items={items}
            onCreateOutfit={handleCreateOutfit}
            onDeleteOutfit={handleDeleteOutfit}
            onOpenSocial={(outfit) => setActiveSocialOutfit(outfit)}
          />
        )}

        {currentTab === 'laundry' && (
          <LaundryManager
            items={items}
            onWashItem={handleWashItem}
            onWashAll={handleWashAllDirty}
          />
        )}
      </main>

      {/* Social Feedback / Rating Modal */}
      {activeSocialOutfit && (
        <SocialFeedbackModal
          outfit={activeSocialOutfit}
          onClose={() => {
            setActiveSocialOutfit(null);
            loadAllData();
          }}
        />
      )}

      {/* Backup & Restore Modal */}
      <BackupModal
        isOpen={isBackupOpen}
        onClose={() => setIsBackupOpen(false)}
        onRestored={loadAllData}
      />
    </div>
  );
};

export default App;

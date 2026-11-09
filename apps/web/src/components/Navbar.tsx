import React from 'react';
import { Shirt, Sparkles, WashingMachine, Layers, DownloadCloud, LogOut, Sun, CloudRain } from 'lucide-react';
import type { User, WeatherForecast } from '@trousseau/shared';

interface NavbarProps {
  currentTab: string;
  onTabChange: (tab: string) => void;
  user: User | null;
  weather: WeatherForecast | null;
  laundryCount: number;
  onOpenBackup: () => void;
  onLogout: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  currentTab,
  onTabChange,
  user,
  weather,
  laundryCount,
  onOpenBackup,
  onLogout
}) => {
  const tabs = [
    { id: 'today', label: "Today's Outfit", icon: Sparkles },
    { id: 'wardrobe', label: 'Wardrobe', icon: Shirt },
    { id: 'outfits', label: 'Outfits', icon: Layers },
    {
      id: 'laundry',
      label: 'Laundry Bin',
      icon: WashingMachine,
      badge: laundryCount > 0 ? laundryCount : undefined
    }
  ];

  return (
    <header className="bg-white border-b border-stone-200 sticky top-0 z-30 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo & Brand */}
          <div className="flex items-center space-x-3 cursor-pointer" onClick={() => onTabChange('today')}>
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-amber-700 via-amber-600 to-amber-500 flex items-center justify-center text-white shadow-md shadow-amber-600/20">
              <Shirt className="w-5 h-5" />
            </div>
            <div>
              <span className="text-xl font-bold tracking-tight text-stone-900 font-serif">Trousseau</span>
              <span className="hidden sm:inline-block ml-2 text-xs font-medium text-amber-700 bg-amber-50 px-2 py-0.5 rounded-full border border-amber-200">
                Self-Hosted
              </span>
            </div>
          </div>

          {/* Navigation Tabs */}
          <nav className="hidden md:flex space-x-1">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              const isActive = currentTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => onTabChange(tab.id)}
                  className={`relative flex items-center space-x-2 px-3.5 py-2 rounded-lg text-sm font-medium transition-all ${
                    isActive
                      ? 'bg-stone-900 text-white shadow-sm'
                      : 'text-stone-600 hover:text-stone-900 hover:bg-stone-100'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  <span>{tab.label}</span>
                  {tab.badge !== undefined && (
                    <span className={`text-xs px-1.5 py-0.2 rounded-full font-bold ${
                      isActive ? 'bg-amber-500 text-stone-950' : 'bg-red-500 text-white'
                    }`}>
                      {tab.badge}
                    </span>
                  )}
                </button>
              );
            })}
          </nav>

          {/* Right Action Bar: Weather + Backup + User */}
          <div className="flex items-center space-x-3">
            {weather && (
              <div className="hidden lg:flex items-center space-x-2 px-3 py-1.5 bg-stone-50 border border-stone-200 rounded-full text-xs font-medium text-stone-700">
                {weather.isRaining ? (
                  <CloudRain className="w-4 h-4 text-blue-500" />
                ) : (
                  <Sun className="w-4 h-4 text-amber-500" />
                )}
                <span>{weather.tempCurrentC}°C</span>
                <span className="text-stone-400">|</span>
                <span className="truncate max-w-[120px]">{weather.description}</span>
              </div>
            )}

            <button
              onClick={onOpenBackup}
              title="Backup & Restore Data"
              className="p-2 text-stone-600 hover:text-stone-900 hover:bg-stone-100 rounded-lg transition-colors border border-stone-200"
            >
              <DownloadCloud className="w-4 h-4" />
            </button>

            {user && (
              <div className="flex items-center space-x-2 pl-2 border-l border-stone-200">
                <span className="hidden sm:inline text-xs font-medium text-stone-700">
                  {user.displayName}
                </span>
                <button
                  onClick={onLogout}
                  title="Sign Out"
                  className="p-1.5 text-stone-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                >
                  <LogOut className="w-4 h-4" />
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Mobile Navigation bar */}
        <div className="flex md:hidden justify-around py-2 border-t border-stone-100">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            const isActive = currentTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => onTabChange(tab.id)}
                className={`flex flex-col items-center py-1 px-3 rounded-lg text-xs font-medium ${
                  isActive ? 'text-amber-800' : 'text-stone-500'
                }`}
              >
                <div className="relative">
                  <Icon className="w-5 h-5 mb-0.5" />
                  {tab.badge !== undefined && (
                    <span className="absolute -top-1 -right-2 text-[10px] bg-red-500 text-white px-1 rounded-full font-bold">
                      {tab.badge}
                    </span>
                  )}
                </div>
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>
      </div>
    </header>
  );
};

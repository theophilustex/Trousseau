import React from 'react';
import {
  Shirt,
  Sparkles,
  WashingMachine,
  Layers,
  DownloadCloud,
  LogOut,
  Sun,
  Moon,
  CloudRain
} from 'lucide-react';
import type { User, WeatherForecast, TemperatureUnit } from '@trousseau/shared';
import { formatTemp } from '@trousseau/shared';

interface NavbarProps {
  currentTab: string;
  onTabChange: (tab: string) => void;
  user: User | null;
  weather: WeatherForecast | null;
  laundryCount: number;
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
  tempUnit: TemperatureUnit;
  onToggleTempUnit: () => void;
  onOpenBackup: () => void;
  onLogout: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  currentTab,
  onTabChange,
  user,
  weather,
  laundryCount,
  theme,
  onToggleTheme,
  tempUnit,
  onToggleTempUnit,
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
    <header className="bg-white dark:bg-stone-900 border-b border-stone-200 dark:border-stone-800 sticky top-0 z-30 shadow-sm transition-colors duration-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo & Brand */}
          <div className="flex items-center space-x-3 cursor-pointer" onClick={() => onTabChange('today')}>
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-amber-700 via-amber-600 to-amber-500 flex items-center justify-center text-white shadow-md shadow-amber-600/20">
              <Shirt className="w-5 h-5" />
            </div>
            <div>
              <span className="text-xl font-bold tracking-tight text-stone-900 dark:text-stone-100 font-serif">Trousseau</span>
              <span className="hidden sm:inline-block ml-2 text-xs font-medium text-amber-700 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/60 px-2 py-0.5 rounded-full border border-amber-200 dark:border-amber-800">
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
                      ? 'bg-stone-900 dark:bg-stone-100 text-white dark:text-stone-950 shadow-sm'
                      : 'text-stone-600 dark:text-stone-300 hover:text-stone-900 dark:hover:text-white hover:bg-stone-100 dark:hover:bg-stone-800'
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

          {/* Right Action Bar: Weather + Temp Toggle + Theme Toggle + Backup + User */}
          <div className="flex items-center space-x-2 sm:space-x-3">
            {/* Weather & Unit Switcher */}
            {weather && (
              <div className="hidden lg:flex items-center space-x-2 px-3 py-1.5 bg-stone-50 dark:bg-stone-800/70 border border-stone-200 dark:border-stone-700 rounded-full text-xs font-medium text-stone-700 dark:text-stone-300">
                {weather.isRaining ? (
                  <CloudRain className="w-4 h-4 text-blue-500" />
                ) : (
                  <Sun className="w-4 h-4 text-amber-500" />
                )}
                <span className="font-mono font-bold">{formatTemp(weather.tempCurrentC, tempUnit)}</span>
                <span className="text-stone-300 dark:text-stone-600">|</span>
                <span className="truncate max-w-[110px]">{weather.description}</span>
              </div>
            )}

            {/* Fahrenheit / Celsius Toggle Button */}
            <button
              onClick={onToggleTempUnit}
              title={`Switch to ${tempUnit === 'C' ? 'Fahrenheit (°F)' : 'Celsius (°C)'}`}
              className="px-2.5 py-1.5 bg-stone-100 dark:bg-stone-800 hover:bg-stone-200 dark:hover:bg-stone-700 text-stone-700 dark:text-stone-300 text-xs font-mono font-bold rounded-lg border border-stone-200 dark:border-stone-700 transition-colors"
            >
              °{tempUnit}
            </button>

            {/* Dark / Light Theme Toggle Button */}
            <button
              onClick={onToggleTheme}
              title={`Switch to ${theme === 'light' ? 'Dark' : 'Light'} Mode`}
              className="p-2 text-stone-600 dark:text-stone-300 hover:text-stone-900 dark:hover:text-white hover:bg-stone-100 dark:hover:bg-stone-800 rounded-lg transition-colors border border-stone-200 dark:border-stone-700"
            >
              {theme === 'dark' ? (
                <Sun className="w-4 h-4 text-amber-400" />
              ) : (
                <Moon className="w-4 h-4 text-stone-600" />
              )}
            </button>

            {/* Backup Button */}
            <button
              onClick={onOpenBackup}
              title="Backup & Restore Data"
              className="p-2 text-stone-600 dark:text-stone-300 hover:text-stone-900 dark:hover:text-white hover:bg-stone-100 dark:hover:bg-stone-800 rounded-lg transition-colors border border-stone-200 dark:border-stone-700"
            >
              <DownloadCloud className="w-4 h-4" />
            </button>

            {/* User & Sign Out */}
            {user && (
              <div className="flex items-center space-x-2 pl-2 border-l border-stone-200 dark:border-stone-700">
                <span className="hidden sm:inline text-xs font-medium text-stone-700 dark:text-stone-300">
                  {user.displayName}
                </span>
                <button
                  onClick={onLogout}
                  title="Sign Out"
                  className="p-1.5 text-stone-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-950/40 rounded-lg transition-colors"
                >
                  <LogOut className="w-4 h-4" />
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Mobile Navigation bar */}
        <div className="flex md:hidden justify-around py-2 border-t border-stone-100 dark:border-stone-800">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            const isActive = currentTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => onTabChange(tab.id)}
                className={`flex flex-col items-center py-1 px-3 rounded-lg text-xs font-medium transition-colors ${
                  isActive ? 'text-amber-800 dark:text-amber-400' : 'text-stone-500 dark:text-stone-400'
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

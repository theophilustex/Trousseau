import { eq } from 'drizzle-orm';
import { db } from '../db/index.js';
import { weatherCache } from '../db/schema.js';
import { type WeatherForecast } from '@trousseau/shared';

// WMO Weather interpretation codes
function interpretWmoCode(code: number): { description: string; isRaining: boolean } {
  if (code === 0) return { description: 'Clear sky', isRaining: false };
  if (code === 1) return { description: 'Mainly clear', isRaining: false };
  if (code === 2) return { description: 'Partly cloudy', isRaining: false };
  if (code === 3) return { description: 'Overcast', isRaining: false };
  if (code === 45 || code === 48) return { description: 'Foggy', isRaining: false };
  if ([51, 53, 55, 56, 57].includes(code)) return { description: 'Drizzle', isRaining: true };
  if ([61, 63, 65, 66, 67].includes(code)) return { description: 'Rain', isRaining: true };
  if ([71, 73, 75, 77].includes(code)) return { description: 'Snow', isRaining: false };
  if ([80, 81, 82].includes(code)) return { description: 'Rain showers', isRaining: true };
  if ([85, 86].includes(code)) return { description: 'Snow showers', isRaining: false };
  if ([95, 96, 99].includes(code)) return { description: 'Thunderstorm', isRaining: true };
  return { description: 'Partly cloudy', isRaining: false };
}

export class WeatherService {
  /**
   * Get weather for a location with local caching to minimize outside calls.
   */
  static async getForecast(
    lat: number = 40.7128,
    lon: number = -74.0060,
    city: string = 'Local'
  ): Promise<WeatherForecast> {
    const today = new Date().toISOString().split('T')[0];
    const roundedLat = Number(lat.toFixed(2));
    const roundedLon = Number(lon.toFixed(2));
    const cacheId = `${roundedLat}_${roundedLon}_${today}`;

    // 1. Check local cache first
    const cached = db.select().from(weatherCache).where(eq(weatherCache.id, cacheId)).get();
    if (cached) {
      return {
        date: cached.date,
        tempCurrentC: cached.tempCurrentC,
        tempHighC: cached.tempHighC,
        tempLowC: cached.tempLowC,
        precipitationProbability: cached.precipitationProbability,
        weatherCode: cached.weatherCode,
        description: cached.description,
        isRaining: Boolean(cached.isRaining),
        city: cached.city || city
      };
    }

    // 2. Fetch from Open-Meteo (No API key needed, open-source friendly)
    try {
      const url = `https://api.open-meteo.com/v1/forecast?latitude=${roundedLat}&longitude=${roundedLon}&current=temperature_2m,precipitation,weather_code&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto`;
      const response = await fetch(url, { headers: { 'User-Agent': 'Trousseau-SelfHosted-Wardrobe/1.0' } });

      if (!response.ok) {
        throw new Error(`Open-Meteo returned status ${response.status}`);
      }

      const data: any = await response.json();
      const current = data.current || {};
      const daily = data.daily || {};

      const tempCurrentC = Math.round(current.temperature_2m ?? 20);
      const tempHighC = Math.round(daily.temperature_2m_max?.[0] ?? tempCurrentC + 3);
      const tempLowC = Math.round(daily.temperature_2m_min?.[0] ?? tempCurrentC - 3);
      const precipProb = Math.round(daily.precipitation_probability_max?.[0] ?? 0);
      const weatherCode = current.weather_code ?? 1;
      const { description, isRaining } = interpretWmoCode(weatherCode);

      const forecast: WeatherForecast = {
        date: today,
        tempCurrentC,
        tempHighC,
        tempLowC,
        precipitationProbability: precipProb,
        weatherCode,
        description,
        isRaining: isRaining || (current.precipitation ?? 0) > 0.5,
        city
      };

      // Store in local cache
      const now = new Date().toISOString();
      db.insert(weatherCache)
        .values({
          id: cacheId,
          city,
          lat: roundedLat,
          lon: roundedLon,
          date: today,
          tempCurrentC: forecast.tempCurrentC,
          tempHighC: forecast.tempHighC,
          tempLowC: forecast.tempLowC,
          precipitationProbability: forecast.precipitationProbability,
          weatherCode: forecast.weatherCode,
          description: forecast.description,
          isRaining: forecast.isRaining ? 1 : 0,
          fetchedAt: now
        })
        .onConflictDoUpdate({
          target: weatherCache.id,
          set: {
            tempCurrentC: forecast.tempCurrentC,
            tempHighC: forecast.tempHighC,
            tempLowC: forecast.tempLowC,
            precipitationProbability: forecast.precipitationProbability,
            weatherCode: forecast.weatherCode,
            description: forecast.description,
            isRaining: forecast.isRaining ? 1 : 0,
            fetchedAt: now
          }
        })
        .run();

      return forecast;
    } catch (err) {
      console.warn('Weather fetch fallback to mild defaults:', err);
      return {
        date: today,
        tempCurrentC: 21,
        tempHighC: 24,
        tempLowC: 17,
        precipitationProbability: 10,
        weatherCode: 1,
        description: 'Pleasant & clear',
        isRaining: false,
        city
      };
    }
  }
}

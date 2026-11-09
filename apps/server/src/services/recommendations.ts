import {
  type Outfit,
  type OutfitRecommendation,
  type WeatherForecast,
  type Occasion
} from '@trousseau/shared';
import { OutfitService } from './outfits.js';
import { ClothingItemService } from './items.js';
import { WeatherService } from './weather.js';
import { AuthService } from './auth.js';

export class RecommendationService {
  /**
   * Generates weather-aware, diversity-optimized outfit recommendations.
   */
  static async getRecommendations(
    userId: string,
    options: {
      occasion?: Occasion;
      targetDate?: string;
    } = {}
  ): Promise<OutfitRecommendation[]> {
    const user = await AuthService.getUserById(userId);
    const targetDate = options.targetDate || new Date().toISOString().split('T')[0];

    // 1. Get weather forecast
    const forecast = await WeatherService.getForecast(
      user?.locationLat ?? 40.7128,
      user?.locationLon ?? -74.0060,
      user?.locationCity ?? 'Local'
    );

    // 2. Fetch all user outfits with populated items
    const allOutfits = await OutfitService.list(userId);
    if (allOutfits.length === 0) {
      return [];
    }

    const scoredOutfits: OutfitRecommendation[] = [];
    const nowMs = new Date(targetDate).getTime();

    for (const outfit of allOutfits) {
      const items = outfit.items || [];
      if (items.length === 0) continue;

      // 3. Cleanliness Gate: Strictly check if any item is dirty
      let isClean = true;
      let dirtyItemName = '';
      for (const item of items) {
        if (
          item.status === 'needs_wash' ||
          item.status === 'in_laundry' ||
          item.status === 'dry_cleaning' ||
          item.wearsSinceWash >= item.maxWearsBeforeWash
        ) {
          isClean = false;
          dirtyItemName = item.name;
          break;
        }
      }

      if (!isClean) {
        // Skip dirty outfits entirely from active recommendation
        continue;
      }

      let weatherScore = 50; // base score
      let weatherReasons: string[] = [];

      // 4. Weather Analysis
      const temp = forecast.tempCurrentC;
      const isRaining = forecast.isRaining;

      let hasOuterwear = items.some(i => i.category === 'outerwear');

      // Temperature guidelines
      if (temp < 12) {
        if (hasOuterwear) {
          weatherScore += 25;
          weatherReasons.push('includes warm outerwear for cold weather');
        } else {
          weatherScore -= 30;
          weatherReasons.push('might be too cold without outerwear');
        }
      } else if (temp > 24) {
        if (hasOuterwear) {
          weatherScore -= 25;
          weatherReasons.push('outerwear might be too warm');
        } else {
          weatherScore += 15;
          weatherReasons.push('breathable for warm weather');
        }
      }

      // Check item temperature boundaries
      for (const item of items) {
        if (item.minTempC !== null && item.minTempC !== undefined && temp < item.minTempC) {
          weatherScore -= (item.minTempC - temp) * 2;
        }
        if (item.maxTempC !== null && item.maxTempC !== undefined && temp > item.maxTempC) {
          weatherScore -= (temp - item.maxTempC) * 2;
        }
        if (isRaining) {
          if (item.rainFriendly) {
            weatherScore += 15;
            weatherReasons.push(`${item.name} is rain-friendly`);
          } else if (item.category === 'footwear') {
            weatherScore -= 10;
          }
        }
      }

      // 5. Diversity & Recency Penalty
      let recencyPenalty = 0;
      let daysSinceLastWorn: number | null = null;

      if (outfit.lastWornAt) {
        const lastWornMs = new Date(outfit.lastWornAt).getTime();
        const diffDays = Math.max(0, Math.floor((nowMs - lastWornMs) / (1000 * 60 * 60 * 24)));
        daysSinceLastWorn = diffDays;

        if (diffDays === 0) {
          recencyPenalty += 80; // Worn today!
        } else if (diffDays <= 2) {
          recencyPenalty += 60; // Worn 1-2 days ago
        } else if (diffDays <= 4) {
          recencyPenalty += 35; // Worn within 4 days
        } else if (diffDays <= 7) {
          recencyPenalty += 15; // Worn within a week
        }
      } else {
        // Never worn yet: boost diversity!
        weatherScore += 15;
        weatherReasons.push('fresh addition to your wardrobe rotation');
      }

      // Item-level recency penalty (prevent wearing the exact same top 2 days in a row)
      for (const item of items) {
        if (item.lastWornAt) {
          const itemWornMs = new Date(item.lastWornAt).getTime();
          const itemDiffDays = Math.floor((nowMs - itemWornMs) / (1000 * 60 * 60 * 24));
          if (itemDiffDays <= 1) {
            recencyPenalty += 20;
          }
        }
      }

      // 6. Occasion Match
      let occasionBonus = 0;
      if (options.occasion) {
        if (outfit.occasion === options.occasion) {
          occasionBonus += 30;
        } else {
          occasionBonus -= 20;
        }
      }

      // 7. Rating Boost
      const ratingBonus = (outfit.avgRating ?? 3) * 3;

      // 8. Total Score Calculation
      const totalScore = Math.max(
        0,
        Math.round(weatherScore + occasionBonus + ratingBonus - recencyPenalty)
      );

      // Construct readable explanation
      let reason = `Recommended for ${forecast.description.toLowerCase()} (${temp}°C). All items clean.`;
      if (daysSinceLastWorn !== null) {
        reason += ` Last worn ${daysSinceLastWorn === 0 ? 'today' : daysSinceLastWorn + ' days ago'}.`;
      } else {
        reason += ` Ready for its first wear.`;
      }
      if (weatherReasons.length > 0) {
        reason += ` (${weatherReasons.join(', ')})`;
      }

      scoredOutfits.push({
        outfit,
        score: totalScore,
        reason,
        weatherScore,
        recencyPenalty,
        cleanlinessScore: 100,
        daysSinceLastWorn
      });
    }

    // Sort descending by score
    scoredOutfits.sort((a, b) => b.score - a.score);

    return scoredOutfits;
  }
}

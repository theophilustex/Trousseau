import type {
  User,
  ClothingItem,
  CreateClothingItemInput,
  UpdateClothingItemInput,
  Outfit,
  CreateOutfitInput,
  UpdateOutfitInput,
  WearLog,
  CreateWearLogInput,
  ConfirmWearLogInput,
  Rating,
  CreateRatingInput,
  Comment,
  CreateCommentInput,
  WeatherForecast,
  OutfitRecommendation,
  LoginInput,
  RegisterInput
} from '@trousseau/shared';

const TOKEN_KEY = 'trousseau_auth_token';

export const ApiClient = {
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  },

  setToken(token: string) {
    localStorage.setItem(TOKEN_KEY, token);
  },

  clearToken() {
    localStorage.removeItem(TOKEN_KEY);
  },

  async request<T>(path: string, options: RequestInit = {}): Promise<T> {
    const token = this.getToken();
    const headers = new Headers(options.headers || {});

    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }

    if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json');
    }

    const response = await fetch(path, { ...options, headers });

    if (response.status === 401) {
      this.clearToken();
    }

    if (!response.ok) {
      const err = await response.json().catch(() => ({ error: response.statusText }));
      throw new Error(err.error || `HTTP error ${response.status}`);
    }

    return response.json();
  },

  // Auth
  async login(input: LoginInput): Promise<{ token: string; user: User }> {
    const res = await this.request<{ token: string; user: User }>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(input)
    });
    this.setToken(res.token);
    return res;
  },

  async register(input: RegisterInput): Promise<{ token: string; user: User }> {
    const res = await this.request<{ token: string; user: User }>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(input)
    });
    this.setToken(res.token);
    return res;
  },

  async getMe(): Promise<{ user: User }> {
    return this.request<{ user: User }>('/api/auth/me');
  },

  async updateLocation(city?: string, lat?: number, lon?: number): Promise<{ user: User }> {
    return this.request<{ user: User }>('/api/auth/location', {
      method: 'PATCH',
      body: JSON.stringify({ city, lat, lon })
    });
  },

  // Clothing Items
  async getItems(filters: Record<string, string | boolean> = {}): Promise<{ items: ClothingItem[] }> {
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([k, v]) => {
      if (v !== undefined && v !== '') params.append(k, String(v));
    });
    return this.request<{ items: ClothingItem[] }>(`/api/items?${params.toString()}`);
  },

  async getItem(id: string): Promise<{ item: ClothingItem }> {
    return this.request<{ item: ClothingItem }>(`/api/items/${id}`);
  },

  async createItem(input: CreateClothingItemInput): Promise<{ item: ClothingItem }> {
    return this.request<{ item: ClothingItem }>('/api/items', {
      method: 'POST',
      body: JSON.stringify(input)
    });
  },

  async updateItem(id: string, input: UpdateClothingItemInput): Promise<{ item: ClothingItem }> {
    return this.request<{ item: ClothingItem }>(`/api/items/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(input)
    });
  },

  async deleteItem(id: string): Promise<{ success: boolean }> {
    return this.request<{ success: boolean }>(`/api/items/${id}`, {
      method: 'DELETE'
    });
  },

  async washItem(id: string): Promise<{ item: ClothingItem }> {
    return this.request<{ item: ClothingItem }>(`/api/items/${id}/wash`, {
      method: 'POST'
    });
  },

  async washAllDirty(): Promise<{ washedCount: number }> {
    return this.request<{ washedCount: number }>('/api/items/wash-all', {
      method: 'POST'
    });
  },

  async uploadPhoto(file: File, removeBackground: boolean = false): Promise<{ imageUrl: string; thumbnailUrl: string; hasTransparentBg: boolean }> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('removeBackground', String(removeBackground));

    return this.request('/api/items/upload', {
      method: 'POST',
      body: formData
    });
  },

  // Outfits
  async getOutfits(filters?: { occasion?: string; season?: string }): Promise<{ outfits: Outfit[] }> {
    const params = new URLSearchParams(filters as any);
    return this.request<{ outfits: Outfit[] }>(`/api/outfits?${params.toString()}`);
  },

  async getPublicOutfits(): Promise<{ outfits: Outfit[] }> {
    return this.request<{ outfits: Outfit[] }>('/api/outfits/public');
  },

  async createOutfit(input: CreateOutfitInput): Promise<{ outfit: Outfit }> {
    return this.request<{ outfit: Outfit }>('/api/outfits', {
      method: 'POST',
      body: JSON.stringify(input)
    });
  },

  async updateOutfit(id: string, input: UpdateOutfitInput): Promise<{ outfit: Outfit }> {
    return this.request<{ outfit: Outfit }>(`/api/outfits/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(input)
    });
  },

  async deleteOutfit(id: string): Promise<{ success: boolean }> {
    return this.request<{ success: boolean }>(`/api/outfits/${id}`, {
      method: 'DELETE'
    });
  },

  // Daily Wear Logs & Check-in
  async getTodayLog(): Promise<{ log: WearLog | null; date: string }> {
    return this.request<{ log: WearLog | null; date: string }>('/api/wear-logs/today');
  },

  async logWear(input: CreateWearLogInput): Promise<{ log: WearLog }> {
    return this.request<{ log: WearLog }>('/api/wear-logs', {
      method: 'POST',
      body: JSON.stringify(input)
    });
  },

  async confirmWear(logId: string, input: ConfirmWearLogInput): Promise<{ log: WearLog }> {
    return this.request<{ log: WearLog }>(`/api/wear-logs/${logId}/confirm`, {
      method: 'POST',
      body: JSON.stringify(input)
    });
  },

  async getWearHistory(): Promise<{ logs: WearLog[] }> {
    return this.request<{ logs: WearLog[] }>('/api/wear-logs');
  },

  // Weather & Recommendations
  async getWeather(): Promise<{ forecast: WeatherForecast }> {
    return this.request<{ forecast: WeatherForecast }>('/api/weather/current');
  },

  async getRecommendations(occasion?: string): Promise<{ recommendations: OutfitRecommendation[] }> {
    const path = occasion ? `/api/recommendations?occasion=${occasion}` : '/api/recommendations';
    return this.request<{ recommendations: OutfitRecommendation[] }>(path);
  },

  // Social Ratings & Comments
  async getRatings(outfitId: string): Promise<{ ratings: Rating[] }> {
    return this.request<{ ratings: Rating[] }>(`/api/social/outfits/${outfitId}/ratings`);
  },

  async addRating(outfitId: string, input: CreateRatingInput): Promise<{ rating: Rating }> {
    return this.request<{ rating: Rating }>(`/api/social/outfits/${outfitId}/ratings`, {
      method: 'POST',
      body: JSON.stringify(input)
    });
  },

  async getComments(outfitId: string): Promise<{ comments: Comment[] }> {
    return this.request<{ comments: Comment[] }>(`/api/social/outfits/${outfitId}/comments`);
  },

  async addComment(outfitId: string, input: CreateCommentInput): Promise<{ comment: Comment }> {
    return this.request<{ comment: Comment }>(`/api/social/outfits/${outfitId}/comments`, {
      method: 'POST',
      body: JSON.stringify(input)
    });
  },

  // Backup & Import/Export
  async downloadBackup() {
    const token = this.getToken();
    const res = await fetch('/api/backup/export', {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    });
    if (!res.ok) throw new Error('Failed to generate backup export');
    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `trousseau-backup-${new Date().toISOString().split('T')[0]}.zip`;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    a.remove();
  },

  async uploadBackup(file: File): Promise<{ success: boolean; restoredItems: number; restoredOutfits: number }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.request('/api/backup/import', {
      method: 'POST',
      body: formData
    });
  }
};

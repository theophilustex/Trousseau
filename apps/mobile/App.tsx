import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  TouchableOpacity,
  TextInput,
  Image,
  ActivityIndicator,
  Alert,
  Modal,
  SafeAreaView,
  StatusBar
} from 'react-native';

interface WeatherData {
  tempCurrentC: number;
  tempHighC: number;
  tempLowC: number;
  description: string;
  isRaining: boolean;
}

interface ClothingItem {
  id: string;
  name: string;
  category: string;
  thumbnailUrl?: string;
  imageUrl?: string;
  wearsSinceWash: number;
  maxWearsBeforeWash: number;
  status: string;
}

interface Outfit {
  id: string;
  name: string;
  occasion: string;
  items?: ClothingItem[];
  itemIds: string[];
}

export default function App() {
  const [serverUrl, setServerUrl] = useState('http://10.0.2.2:4000'); // Default emulator localhost or LAN IP
  const [token, setToken] = useState('');
  const [isConnected, setIsConnected] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  // Auth inputs
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  // Daily State
  const [weather, setWeather] = useState<WeatherData | null>(null);
  const [todayOutfit, setTodayOutfit] = useState<Outfit | null>(null);
  const [todayLogId, setTodayLogId] = useState<string | null>(null);
  const [isWornToday, setIsWornToday] = useState(false);
  const [allOutfits, setAllOutfits] = useState<Outfit[]>([]);

  // Swap modal
  const [isSwapModalVisible, setIsSwapModalVisible] = useState(false);

  const apiRequest = async (path: string, options: any = {}) => {
    const cleanUrl = serverUrl.replace(/\/+$/, '');
    const headers = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {})
    };

    const res = await fetch(`${cleanUrl}${path}`, {
      ...options,
      headers
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({ error: res.statusText }));
      throw new Error(err.error || `HTTP ${res.status}`);
    }

    return res.json();
  };

  const handleLogin = async () => {
    if (!username.trim() || !password.trim()) {
      Alert.alert('Required', 'Please enter your username and password.');
      return;
    }

    setIsLoading(true);
    try {
      const data = await apiRequest('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({
          usernameOrEmail: username.trim(),
          password: password.trim()
        })
      });

      setToken(data.token);
      setIsConnected(true);
      await loadDashboardData(data.token);
    } catch (err: any) {
      Alert.alert('Connection Failed', err.message || 'Unable to connect to your Trousseau server.');
    } finally {
      setIsLoading(false);
    }
  };

  const loadDashboardData = async (authToken?: string) => {
    setIsLoading(true);
    try {
      // 1. Fetch weather
      const weatherRes = await apiRequest('/api/weather/current').catch(() => ({
        forecast: {
          tempCurrentC: 21,
          tempHighC: 24,
          tempLowC: 17,
          description: 'Mild & pleasant',
          isRaining: false
        }
      }));
      setWeather(weatherRes.forecast);

      // 2. Fetch today's plan
      const todayRes = await apiRequest('/api/wear-logs/today');
      setTodayLogId(todayRes.log?.id || null);
      setIsWornToday(todayRes.log?.status === 'worn');

      // 3. Fetch recommendations & all outfits
      const [recsRes, outfitsRes] = await Promise.all([
        apiRequest('/api/recommendations'),
        apiRequest('/api/outfits')
      ]);

      setAllOutfits(outfitsRes.outfits || []);

      if (todayRes.log?.outfitId) {
        const planned = outfitsRes.outfits.find((o: Outfit) => o.id === todayRes.log.outfitId);
        setTodayOutfit(planned || recsRes.recommendations?.[0]?.outfit || null);
      } else {
        setTodayOutfit(recsRes.recommendations?.[0]?.outfit || outfitsRes.outfits?.[0] || null);
      }
    } catch (err: any) {
      Alert.alert('Data Error', err.message || 'Failed to load today data');
    } finally {
      setIsLoading(false);
    }
  };

  // Crucial 1-tap wear confirmation
  const handleConfirmWorn = async () => {
    if (!todayOutfit) return;

    setIsLoading(true);
    try {
      let logId = todayLogId;
      if (!logId) {
        // Create today's log if not exists
        const createRes = await apiRequest('/api/wear-logs', {
          method: 'POST',
          body: JSON.stringify({
            outfitId: todayOutfit.id,
            itemIds: todayOutfit.itemIds,
            date: new Date().toISOString().split('T')[0],
            status: 'worn'
          })
        });
        logId = createRes.log.id;
        setTodayLogId(logId);
      } else {
        await apiRequest(`/api/wear-logs/${logId}/confirm`, {
          method: 'POST',
          body: JSON.stringify({
            status: 'worn'
          })
        });
      }

      setIsWornToday(true);
      Alert.alert('Wear Confirmed!', "Your wear count has been updated and laundry thresholds recalculated.");
      await loadDashboardData();
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to confirm wear');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSwapOutfit = async (newOutfit: Outfit) => {
    setIsLoading(true);
    try {
      if (todayLogId) {
        await apiRequest(`/api/wear-logs/${todayLogId}/confirm`, {
          method: 'POST',
          body: JSON.stringify({
            status: 'worn',
            replacementOutfitId: newOutfit.id
          })
        });
      } else {
        await apiRequest('/api/wear-logs', {
          method: 'POST',
          body: JSON.stringify({
            outfitId: newOutfit.id,
            itemIds: newOutfit.itemIds,
            date: new Date().toISOString().split('T')[0],
            status: 'worn'
          })
        });
      }

      setTodayOutfit(newOutfit);
      setIsWornToday(true);
      setIsSwapModalVisible(false);
      Alert.alert('Swapped & Confirmed!', `Switched to "${newOutfit.name}". Wear logged successfully.`);
      await loadDashboardData();
    } catch (err: any) {
      Alert.alert('Swap Error', err.message || 'Failed to swap outfit');
    } finally {
      setIsLoading(false);
    }
  };

  // Render Login screen if not connected
  if (!isConnected) {
    return (
      <SafeAreaView style={styles.container}>
        <StatusBar barStyle="dark-content" />
        <View style={styles.loginCard}>
          <Text style={styles.brandTitle}>Trousseau</Text>
          <Text style={styles.brandSubtitle}>Companion Mobile App</Text>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>Server URL (IP or Hostname)</Text>
            <TextInput
              style={styles.input}
              value={serverUrl}
              onChangeText={setServerUrl}
              placeholder="http://192.168.1.50:4000"
              autoCapitalize="none"
              autoCorrect={false}
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>Username</Text>
            <TextInput
              style={styles.input}
              value={username}
              onChangeText={setUsername}
              placeholder="Username"
              autoCapitalize="none"
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>Password</Text>
            <TextInput
              style={styles.input}
              value={password}
              onChangeText={setPassword}
              placeholder="Password"
              secureTextEntry
            />
          </View>

          <TouchableOpacity
            style={styles.primaryButton}
            onPress={handleLogin}
            disabled={isLoading}
          >
            {isLoading ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <Text style={styles.primaryButtonText}>Connect to Server</Text>
            )}
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  const cleanBase = serverUrl.replace(/\/+$/, '');

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />

      {/* Header */}
      <View style={styles.header}>
        <View>
          <Text style={styles.headerTitle}>Trousseau</Text>
          <Text style={styles.headerDate}>
            {new Date().toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })}
          </Text>
        </View>

        {weather && (
          <View style={styles.weatherBadge}>
            <Text style={styles.weatherTemp}>{weather.tempCurrentC}°C</Text>
            <Text style={styles.weatherDesc}>{weather.description}</Text>
          </View>
        )}
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* Main Outfit Card */}
        {todayOutfit ? (
          <View style={styles.outfitCard}>
            <View style={styles.outfitHeader}>
              <View style={styles.occasionBadge}>
                <Text style={styles.occasionText}>{todayOutfit.occasion}</Text>
              </View>
              {isWornToday && (
                <View style={styles.wornBadge}>
                  <Text style={styles.wornBadgeText}>✓ Worn Today</Text>
                </View>
              )}
            </View>

            <Text style={styles.outfitName}>{todayOutfit.name}</Text>

            {/* Items Grid */}
            <View style={styles.itemsGrid}>
              {todayOutfit.items?.map((item) => (
                <View key={item.id} style={styles.itemBox}>
                  {item.thumbnailUrl ? (
                    <Image
                      source={{ uri: `${cleanBase}${item.thumbnailUrl}` }}
                      style={styles.itemImage}
                      resizeMode="contain"
                    />
                  ) : (
                    <View style={styles.itemPlaceholder}>
                      <Text style={styles.placeholderText}>{item.category}</Text>
                    </View>
                  )}
                  <Text style={styles.itemName} numberOfLines={1}>{item.name}</Text>
                  <Text style={styles.itemWears}>
                    {item.wearsSinceWash}/{item.maxWearsBeforeWash} wears
                  </Text>
                </View>
              ))}
            </View>

            {/* Crucial 1-Tap Confirmation Buttons */}
            <View style={styles.actionsRow}>
              {!isWornToday ? (
                <TouchableOpacity
                  style={styles.confirmButton}
                  onPress={handleConfirmWorn}
                  disabled={isLoading}
                >
                  <Text style={styles.confirmButtonText}>✓ I Wore This Today</Text>
                </TouchableOpacity>
              ) : (
                <View style={styles.confirmedBox}>
                  <Text style={styles.confirmedText}>✓ Logged & Wear Count Updated</Text>
                </View>
              )}

              <TouchableOpacity
                style={styles.swapButton}
                onPress={() => setIsSwapModalVisible(true)}
              >
                <Text style={styles.swapButtonText}>Swap Outfit</Text>
              </TouchableOpacity>
            </View>
          </View>
        ) : (
          <View style={styles.emptyCard}>
            <Text style={styles.emptyText}>No outfit scheduled for today.</Text>
          </View>
        )}

        <TouchableOpacity
          style={styles.refreshButton}
          onPress={() => loadDashboardData()}
        >
          <Text style={styles.refreshText}>↻ Sync with Server</Text>
        </TouchableOpacity>
      </ScrollView>

      {/* Outfit Swapper Modal */}
      <Modal
        visible={isSwapModalVisible}
        animationType="slide"
        transparent={true}
        onRequestClose={() => setIsSwapModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Select Today's Outfit</Text>
              <TouchableOpacity onPress={() => setIsSwapModalVisible(false)}>
                <Text style={styles.closeText}>Close</Text>
              </TouchableOpacity>
            </View>

            <ScrollView style={styles.modalList}>
              {allOutfits.map((outfit) => (
                <TouchableOpacity
                  key={outfit.id}
                  style={styles.modalOutfitItem}
                  onPress={() => handleSwapOutfit(outfit)}
                >
                  <Text style={styles.modalOutfitOccasion}>{outfit.occasion.toUpperCase()}</Text>
                  <Text style={styles.modalOutfitName}>{outfit.name}</Text>
                  <Text style={styles.modalOutfitSub}>{outfit.items?.length || 0} items</Text>
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fafaf9'
  },
  loginCard: {
    padding: 24,
    margin: 20,
    backgroundColor: '#fff',
    borderRadius: 24,
    borderWidth: 1,
    borderColor: '#e7e5e4',
    shadowColor: '#000',
    shadowOpacity: 0.05,
    shadowRadius: 10,
    elevation: 3
  },
  brandTitle: {
    fontSize: 28,
    fontWeight: 'bold',
    fontFamily: 'serif',
    color: '#1c1917',
    textAlign: 'center'
  },
  brandSubtitle: {
    fontSize: 13,
    color: '#78716c',
    textAlign: 'center',
    marginBottom: 20
  },
  inputGroup: {
    marginBottom: 14
  },
  label: {
    fontSize: 12,
    fontWeight: '600',
    color: '#44403c',
    marginBottom: 6
  },
  input: {
    backgroundColor: '#f5f5f4',
    borderWidth: 1,
    borderColor: '#e7e5e4',
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 14,
    color: '#1c1917'
  },
  primaryButton: {
    backgroundColor: '#1c1917',
    borderRadius: 14,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 10
  },
  primaryButtonText: {
    color: '#fff',
    fontWeight: 'bold',
    fontSize: 15
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 14,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e7e5e4'
  },
  headerTitle: {
    fontSize: 22,
    fontWeight: 'bold',
    fontFamily: 'serif',
    color: '#1c1917'
  },
  headerDate: {
    fontSize: 12,
    color: '#78716c',
    fontWeight: '500'
  },
  weatherBadge: {
    backgroundColor: '#fef3c7',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 20,
    alignItems: 'flex-end',
    borderWidth: 1,
    borderColor: '#fde68a'
  },
  weatherTemp: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#92400e'
  },
  weatherDesc: {
    fontSize: 10,
    color: '#b45309'
  },
  scrollContent: {
    padding: 16
  },
  outfitCard: {
    backgroundColor: '#fff',
    borderRadius: 24,
    padding: 18,
    borderWidth: 1,
    borderColor: '#e7e5e4',
    shadowColor: '#000',
    shadowOpacity: 0.05,
    shadowRadius: 8,
    elevation: 2
  },
  outfitHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8
  },
  occasionBadge: {
    backgroundColor: '#fef3c7',
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12
  },
  occasionText: {
    fontSize: 11,
    fontWeight: 'bold',
    color: '#92400e',
    textTransform: 'uppercase'
  },
  wornBadge: {
    backgroundColor: '#ecfdf5',
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#a7f3d0'
  },
  wornBadgeText: {
    fontSize: 11,
    fontWeight: 'bold',
    color: '#047857'
  },
  outfitName: {
    fontSize: 22,
    fontWeight: 'bold',
    fontFamily: 'serif',
    color: '#1c1917',
    marginBottom: 16
  },
  itemsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
    marginBottom: 20
  },
  itemBox: {
    width: '48%',
    backgroundColor: '#f5f5f4',
    borderRadius: 16,
    padding: 10,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#e7e5e4'
  },
  itemImage: {
    width: 80,
    height: 80,
    marginBottom: 6
  },
  itemPlaceholder: {
    width: 80,
    height: 80,
    backgroundColor: '#e7e5e4',
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 6
  },
  placeholderText: {
    fontSize: 10,
    color: '#78716c',
    textTransform: 'capitalize'
  },
  itemName: {
    fontSize: 13,
    fontWeight: '600',
    color: '#1c1917',
    textAlign: 'center'
  },
  itemWears: {
    fontSize: 11,
    color: '#78716c',
    marginTop: 2
  },
  actionsRow: {
    gap: 10
  },
  confirmButton: {
    backgroundColor: '#059669',
    paddingVertical: 14,
    borderRadius: 14,
    alignItems: 'center',
    shadowColor: '#059669',
    shadowOpacity: 0.25,
    shadowRadius: 5,
    elevation: 3
  },
  confirmButtonText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: 'bold'
  },
  confirmedBox: {
    backgroundColor: '#ecfdf5',
    borderWidth: 1,
    borderColor: '#a7f3d0',
    paddingVertical: 12,
    borderRadius: 14,
    alignItems: 'center'
  },
  confirmedText: {
    color: '#065f46',
    fontWeight: 'bold',
    fontSize: 14
  },
  swapButton: {
    backgroundColor: '#f5f5f4',
    paddingVertical: 12,
    borderRadius: 14,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#e7e5e4'
  },
  swapButtonText: {
    color: '#44403c',
    fontSize: 13,
    fontWeight: '600'
  },
  emptyCard: {
    padding: 30,
    alignItems: 'center',
    backgroundColor: '#fff',
    borderRadius: 20
  },
  emptyText: {
    color: '#78716c'
  },
  refreshButton: {
    padding: 16,
    alignItems: 'center'
  },
  refreshText: {
    color: '#78716c',
    fontSize: 12,
    fontWeight: '500'
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'flex-end'
  },
  modalContent: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    padding: 20,
    maxHeight: '75%'
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingBottom: 14,
    borderBottomWidth: 1,
    borderBottomColor: '#f5f5f4'
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    fontFamily: 'serif',
    color: '#1c1917'
  },
  closeText: {
    color: '#78716c',
    fontSize: 14,
    fontWeight: '600'
  },
  modalList: {
    paddingVertical: 10
  },
  modalOutfitItem: {
    padding: 14,
    backgroundColor: '#f5f5f4',
    borderRadius: 16,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: '#e7e5e4'
  },
  modalOutfitOccasion: {
    fontSize: 10,
    color: '#b45309',
    fontWeight: 'bold'
  },
  modalOutfitName: {
    fontSize: 15,
    fontWeight: 'bold',
    color: '#1c1917',
    marginTop: 2
  },
  modalOutfitSub: {
    fontSize: 12,
    color: '#78716c',
    marginTop: 2
  }
});

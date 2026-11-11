import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Ionicons } from '@expo/vector-icons';
import { SafeAreaProvider, useSafeAreaInsets } from 'react-native-safe-area-context';
import { useWindowDimensions, View, Button } from 'react-native';
import PlatformWorkspaceScreen from '../screens/PlatformWorkspaceScreen';
import useAuthStore from '../store/authStore';
import { colors } from '../config/theme';
import { getTabBarHeight, getBottomPadding, getDeviceType } from '../utils/responsive';

// Screens
import LoginScreen from '../screens/LoginScreen';
import DashboardScreen from '../screens/DashboardScreen';
import StudentsScreen from '../screens/StudentsScreen';
import AttendanceScreen from '../screens/AttendanceScreen';
import FinanceScreen from '../screens/FinanceScreen';
import SettingsScreen from '../screens/SettingsScreen';
import ClassesScreen from '../screens/ClassesScreen';
import CommunicationsScreen from '../screens/CommunicationsScreen';
import ReportsScreen from '../screens/ReportsScreen';
import ResourcesScreen from '../screens/ResourcesScreen';
import RolesScreen from '../screens/RolesScreen';
import UsersScreen from '../screens/UsersScreen';
import MoreScreen from '../screens/MoreScreen';

const Stack = createNativeStackNavigator();
const Tab = createBottomTabNavigator();
const MoreStack = createNativeStackNavigator();

// Tab Bar'ın alt sayfalarda da görünmesini sağlayan İÇ STACK
function MoreStackScreen() {
  return (
    <MoreStack.Navigator screenOptions={{ headerShown: false }}>
      <MoreStack.Screen name="MoreMain" component={MoreScreen} />
      <MoreStack.Screen name="Attendance" component={AttendanceScreen} />
      <MoreStack.Screen name="Classes" component={ClassesScreen} />
      <MoreStack.Screen name="Communications" component={CommunicationsScreen} />
      <MoreStack.Screen name="Reports" component={ReportsScreen} />
      <MoreStack.Screen name="Resources" component={ResourcesScreen} />
      <MoreStack.Screen name="Roles" component={RolesScreen} />
      <MoreStack.Screen name="Users" component={UsersScreen} />
    </MoreStack.Navigator>
  );
}

function MainTabs() {
  const insets = useSafeAreaInsets();
  const { width } = useWindowDimensions();
  const deviceType = getDeviceType();
  const tabBarHeight = getTabBarHeight();

  const getResponsiveTabBarHeight = () => {
    switch (deviceType) {
      case 'phone': return 80;
      case 'phoneLarge': return 90;
      case 'tablet': return 100;
      case 'tabletLarge': return 110;
      default: return 80;
    }
  };

  const getResponsiveIconSize = () => {
    switch (deviceType) {
      case 'phone': return 28;
      case 'phoneLarge': return 32;
      case 'tablet': return 36;
      case 'tabletLarge': return 40;
      default: return 28;
    }
  };

  const getResponsiveFontSize = () => {
    switch (deviceType) {
      case 'phone': return 13;
      case 'phoneLarge': return 14;
      case 'tablet': return 15;
      case 'tabletLarge': return 16;
      default: return 13;
    }
  };

  const getResponsivePadding = () => {
    switch (deviceType) {
      case 'phone': return 12;
      case 'phoneLarge': return 14;
      case 'tablet': return 16;
      case 'tabletLarge': return 18;
      default: return 12;
    }
  };

  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        tabBarIcon: ({ focused, color }) => {
          let iconName;
          if (route.name === 'Dashboard') iconName = focused ? 'home' : 'home-outline';
          else if (route.name === 'Students') iconName = focused ? 'people' : 'people-outline';
          else if (route.name === 'Finance') iconName = focused ? 'cash' : 'cash-outline';
          else if (route.name === 'More') iconName = focused ? 'grid' : 'grid-outline';
          else if (route.name === 'Settings') iconName = focused ? 'settings' : 'settings-outline';

          return <Ionicons name={iconName} size={getResponsiveIconSize()} color={color} />;
        },
        tabBarActiveTintColor: colors.brand,
        tabBarInactiveTintColor: colors.muted,
        headerShown: false,
        tabBarStyle: {
          backgroundColor: colors.surface,
          borderTopWidth: 1,
          borderTopColor: colors.line,
          height: getResponsiveTabBarHeight() + insets.bottom,
          paddingBottom: insets.bottom,
          paddingTop: getResponsivePadding(),
          width: '100%',
        },
        tabBarItemStyle: { flex: 1 },
        tabBarLabelStyle: { fontSize: getResponsiveFontSize(), fontWeight: '500' },
      })}
    >
      <Tab.Screen name="Dashboard" component={DashboardScreen} options={{ tabBarLabel: 'Ana Sayfa' }} />
      <Tab.Screen name="Students" component={StudentsScreen} options={{ tabBarLabel: 'Öğrenciler' }} />
      <Tab.Screen name="Finance" component={FinanceScreen} options={{ tabBarLabel: 'Finans' }} />
      <Tab.Screen name="More" component={MoreStackScreen} options={{ tabBarLabel: 'Diğer' }} />
      <Tab.Screen name="Settings" component={SettingsScreen} options={{ tabBarLabel: 'Ayarlar' }} />
    </Tab.Navigator>
  );
}

export default function AppNavigator() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const loadUser = useAuthStore((state) => state.loadUser);
  const user = useAuthStore((state) => state.user);
  const workspace = useAuthStore((state) => state.workspace);
  const setWorkspace = useAuthStore((state) => state.setWorkspace);

  React.useEffect(() => {
    loadUser();
  }, []);

  return (
    <SafeAreaProvider>
      {isAuthenticated && user?.is_super_admin && workspace && <View><Button title={`${workspace.name} · SaaS yönetimine dön`} onPress={() => setWorkspace(null)} /></View>}
      <NavigationContainer>
        <Stack.Navigator screenOptions={{ headerShown: false }}>
          {!isAuthenticated ? (
            <Stack.Screen name="Login" component={LoginScreen} />
          ) : user?.is_super_admin && !workspace ? (
            <Stack.Screen name="SaaSWorkspace" component={PlatformWorkspaceScreen} />
          ) : (
            <Stack.Screen name="MainTabs" component={MainTabs} />
          )}
        </Stack.Navigator>
      </NavigationContainer>
    </SafeAreaProvider>
  );
}

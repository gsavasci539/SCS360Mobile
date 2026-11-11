import React from 'react';
import { Dimensions, Platform, PixelRatio } from 'react-native';
import { moderateScale, verticalScale, scale, moderateVerticalScale } from 'react-native-size-matters';

const { width, height } = Dimensions.get('window');

// Breakpoints for different screen sizes
export const breakpoints = {
  phoneSmall: 320,
  phone: 375,
  phoneLarge: 414,
  tablet: 768,
  tabletLarge: 1024,
  desktop: 1280,
};

// Device type detection
export const getDeviceType = () => {
  if (width >= breakpoints.tablet) {
    return width >= breakpoints.tabletLarge ? 'tabletLarge' : 'tablet';
  }
  if (width >= breakpoints.phoneLarge) return 'phoneLarge';
  if (width >= breakpoints.phone) return 'phone';
  return 'phoneSmall';
};

// Column count based on screen size
export const getColumnCount = () => {
  const deviceType = getDeviceType();
  const isLandscape = width > height;
  
  if (deviceType === 'tabletLarge') return isLandscape ? 4 : 3;
  if (deviceType === 'tablet') return isLandscape ? 3 : 2;
  return 2;
};

// Responsive spacing
export const responsiveSpacing = {
  xs: (value) => moderateScale(value, 0.3),
  sm: (value) => moderateScale(value, 0.5),
  md: (value) => moderateScale(value, 0.7),
  lg: (value) => moderateScale(value, 0.9),
  xl: (value) => moderateScale(value, 1.1),
};

// Responsive font size
export const responsiveFontSize = (size) => {
  const deviceType = getDeviceType();
  const scaleMultiplier = {
    phoneSmall: 0.9,
    phone: 1,
    phoneLarge: 1.05,
    tablet: 1.15,
    tabletLarge: 1.25,
  };
  return moderateScale(size * (scaleMultiplier[deviceType] || 1));
};

// Responsive icon size
export const responsiveIconSize = (size) => {
  const deviceType = getDeviceType();
  const scaleMultiplier = {
    phoneSmall: 0.9,
    phone: 1,
    phoneLarge: 1.05,
    tablet: 1.2,
    tabletLarge: 1.3,
  };
  return moderateScale(size * (scaleMultiplier[deviceType] || 1));
};

// Responsive border radius
export const responsiveRadius = (radius) => {
  const deviceType = getDeviceType();
  const scaleMultiplier = {
    phoneSmall: 0.9,
    phone: 1,
    phoneLarge: 1.05,
    tablet: 1.1,
    tabletLarge: 1.15,
  };
  return moderateScale(radius * (scaleMultiplier[deviceType] || 1));
};

// Calculate card width based on screen size and column count
export const getCardWidth = (columnCount, padding = 16) => {
  const totalPadding = padding * (columnCount + 1);
  const availableWidth = width - totalPadding;
  return availableWidth / columnCount;
};

// Max width for tablet content
export const getMaxContentWidth = () => {
  const deviceType = getDeviceType();
  if (deviceType === 'tabletLarge') return 1400;
  if (deviceType === 'tablet') return 1200;
  return width;
};

// Tab bar height calculation
export const getTabBarHeight = () => {
  const deviceType = getDeviceType();
  const baseHeight = 60;
  const scaleMultiplier = {
    phoneSmall: 0.9,
    phone: 1,
    phoneLarge: 1.05,
    tablet: 1.15,
    tabletLarge: 1.2,
  };
  return moderateVerticalScale(baseHeight * (scaleMultiplier[deviceType] || 1));
};

// Bottom padding for scroll views to avoid tab bar overlap
export const getBottomPadding = (tabBarHeight) => {
  const safeAreaBottom = Platform.OS === 'ios' ? 34 : 0;
  return tabBarHeight + safeAreaBottom + moderateScale(16);
};

// Safe area padding for different platforms
export const getSafeAreaPadding = () => {
  const isIOS = Platform.OS === 'ios';
  return {
    top: isIOS ? moderateVerticalScale(44) : moderateVerticalScale(16),
    bottom: isIOS ? moderateVerticalScale(34) : moderateVerticalScale(16),
  };
};

// Responsive container style
export const getResponsiveContainerStyle = () => {
  const deviceType = getDeviceType();
  const maxWidth = getMaxContentWidth();
  
  return {
    flex: 1,
    maxWidth,
    alignSelf: deviceType === 'tablet' || deviceType === 'tabletLarge' ? 'center' : 'stretch',
    width: deviceType === 'tablet' || deviceType === 'tabletLarge' ? '100%' : '100%',
  };
};

// Export all size-matters functions for convenience
export { scale, verticalScale, moderateScale, moderateVerticalScale };

// Hook for responsive values
export const useResponsive = () => {
  const [dimensions, setDimensions] = React.useState(Dimensions.get('window'));

  React.useEffect(() => {
    const subscription = Dimensions.addEventListener('change', ({ window }) => {
      setDimensions(window);
    });
    return () => subscription?.remove();
  }, []);

  return {
    width: dimensions.width,
    height: dimensions.height,
    isLandscape: dimensions.width > dimensions.height,
    isPortrait: dimensions.width <= dimensions.height,
    deviceType: getDeviceType(),
    columnCount: getColumnCount(),
    tabBarHeight: getTabBarHeight(),
    maxContentWidth: getMaxContentWidth(),
  };
};

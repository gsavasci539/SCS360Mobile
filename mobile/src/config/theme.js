// Design system matching SchoolManagement-master frontend
import { moderateScale, verticalScale as moderateVerticalScale } from 'react-native-size-matters';

export const colors = {
  // Primary colors
  brand: '#176b5b',
  brandDark: '#114c43',
  brandLight: '#24947c',
  
  // Navy (sidebar)
  navy: '#142a2d',
  
  // Neutral colors
  ink: '#17282b',
  muted: '#6e7b7d',
  canvas: '#f4f6f3',
  surface: '#ffffff',
  surfaceSoft: '#eef2ee',
  line: '#dfe6e1',
  
  // Accent colors
  accent: '#e9a74d',
  
  // Status colors
  danger: '#c34b4b',
  success: '#2c8a6f',
  warning: '#b47626',
  info: '#397c92',
  
  // Text colors
  text: {
    primary: '#17282b',
    secondary: '#6e7b7d',
    light: '#95aaa7',
    inverse: '#edf5f1',
  },
  
  // Background colors
  background: {
    primary: '#f4f6f3',
    secondary: '#ffffff',
    navy: '#142a2d',
  },
};

export const spacing = {
  xs: moderateScale(4),
  sm: moderateScale(8),
  md: moderateScale(12),
  lg: moderateScale(16),
  xl: moderateScale(20),
  xxl: moderateScale(24),
  xxxl: moderateScale(30),
};

export const borderRadius = {
  sm: moderateScale(9),
  md: moderateScale(11),
  lg: moderateScale(12),
  xl: moderateScale(13),
  xxl: moderateScale(16),
  xxxl: moderateScale(18),
  round: 99,
};

export const fontSize = {
  xs: moderateScale(10),
  sm: moderateScale(11),
  base: moderateScale(12),
  md: moderateScale(13),
  lg: moderateScale(14),
  xl: moderateScale(15),
  xxl: moderateScale(16),
  xxxl: moderateScale(18),
  huge: moderateScale(20),
  massive: moderateScale(25),
  giant: moderateScale(27),
  colossal: moderateScale(34),
};

export const fontWeight = {
  normal: '400',
  medium: '500',
  semibold: '600',
  bold: '700',
  extrabold: '800',
};

export const shadows = {
  small: {
    shadowColor: '#000',
    shadowOffset: { width: 0, height: moderateScale(2) },
    shadowOpacity: 0.05,
    shadowRadius: moderateScale(4),
    elevation: 2,
  },
  medium: {
    shadowColor: '#000',
    shadowOffset: { width: 0, height: moderateScale(4) },
    shadowOpacity: 0.08,
    shadowRadius: moderateScale(16),
    elevation: 4,
  },
  large: {
    shadowColor: '#000',
    shadowOffset: { width: 0, height: moderateScale(16) },
    shadowOpacity: 0.08,
    shadowRadius: moderateScale(50),
    elevation: 8,
  },
};

export const commonStyles = {
  card: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.xxxl,
    borderWidth: 1,
    borderColor: colors.line,
    ...shadows.small,
  },
  button: {
    borderRadius: borderRadius.lg,
    minHeight: moderateVerticalScale(42),
    paddingHorizontal: spacing.lg,
    justifyContent: 'center',
    alignItems: 'center',
  },
  buttonPrimary: {
    backgroundColor: colors.brand,
    ...shadows.small,
  },
  buttonSecondary: {
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.line,
  },
  input: {
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: borderRadius.lg,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
    minHeight: moderateVerticalScale(43),
  },
  statCard: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.xxxl,
    padding: spacing.xxl,
    ...shadows.small,
  },
};

export default {
  colors,
  spacing,
  borderRadius,
  fontSize,
  fontWeight,
  shadows,
  commonStyles,
};

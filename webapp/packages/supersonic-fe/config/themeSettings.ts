// import defaultSettings from './defaultSettings';

const constants = {
  black85: 'rgba(0,10,36,0.85)',
  black65: 'rgba(0,10,36,0.65)',
  black45: 'rgba(0,10,36,0.45)',
  black25: 'rgba(0,10,36,0.25)',
};

const settings = {
  // 'primary-color': defaultSettings.primaryColor,
  // Colors
  'blue-6': '#296DF3',
  'primary-color': '#296DF3',
  'green-6': '#26C992',
  'success-color': '#26C992',
  'red-5': '#EF4872',
  'error-color': '#EF4872',
  'gold-6': '#FFB924',
  'warning-color': '#FFB924',

  // Color used by default to control hover and active backgrounds and for
  // alert info backgrounds.
  'primary-1': '#E3ECFD',
  'primary-2': '#BED2FB',
  'primary-3': '#86ACF8',
  'primary-4': '#6193F6',
  'primary-5': '#4E86F5',
  'primary-6': '#296DF3',
  'primary-7': '#0D57E8',
  'primary-8': '#0B49C3',
  'primary-9': '#093B9D',
  'primary-10': '#062666',

  // Base Scaffolding Variables
  'heading-color': constants.black85,
  'text-color': constants.black85,
  'text-color-secondary': constants.black65,
  'border-radius-base': '4px',

  // Buttons
  'btn-padding-horizontal-sm': '8px',
  'btn-padding-horizontal-base': '16px',
  'btn-padding-horizontal-lg': '16px',
  'btn-default-color': constants.black65,
  'btn-default-border': 'rgba(0,0,0,0.15)',
  'btn-disable-color': constants.black25,
  'btn-disable-border': 'rgba(0,10,36,0.15)',
  'btn-disable-bg': 'rgba(0,10,36,0.04)',
};

export const configProviderTheme = {
  components: {
    Button: {
      colorPrimary: '#3182ce',
    },
    Radio: {
      colorPrimary: '#3182ce',
    },
    Checkbox: {
      colorPrimary: '#3182ce',
      borderRadiusSM: 0,
      checkboxSize: 24,
    },
    Table: {
      headerBg: '#f9fafb',
      headerColor: '#667085',
      headerLineHeight: '38px',
      headerSplitColor: '#f9fafb',
    },
  },
};

export default settings;

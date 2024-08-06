module.exports = {
  presets: (() => {
    try {
      return [require.resolve('@react-native/babel-preset')];
    } catch (_) {
      return ['module:metro-react-native-babel-preset'];
    }
  })(),
  plugins: [[require('@rnx-kit/polyfills')]],
};

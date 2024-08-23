module.exports = {
  extends: ['plugin:@typescript-eslint/recommended', '@react-native'],
  root: true,
  parser: '@typescript-eslint/parser',
  plugins: ['@typescript-eslint'],
  rules: {
    'prettier/prettier': 'error',
    '@typescript-eslint/no-var-requires': 'off',
    'comma-dangle': 'off', // prettier already detects this
  },
};

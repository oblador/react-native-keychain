module.exports = {
  extends: ['plugin:@typescript-eslint/recommended', '@react-native'],
  root: true,
  parser: '@typescript-eslint/parser',
  plugins: ['@typescript-eslint', 'prettier'],
  rules: {
    'prettier/prettier': 'error',
    '@typescript-eslint/no-require-imports': 'off',
    'comma-dangle': 'off', // prettier already detects this
  },
};

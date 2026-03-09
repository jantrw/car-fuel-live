import pluginVue from "eslint-plugin-vue"
import tseslint from "@typescript-eslint/eslint-plugin"
import tsParser from "@typescript-eslint/parser"

export default [
  {
    files: ["src/**/*.vue"],
    languageOptions: {
      parser: (await import("vue-eslint-parser")).default,
      parserOptions: {
        parser: tsParser
      }
    },
    plugins: {
      vue: pluginVue,
      "@typescript-eslint": tseslint
    },
    rules: {
      ...pluginVue.configs["flat/recommended"].rules
    }
  },
  {
    files: ["src/**/*.ts"],
    languageOptions: { parser: tsParser },
    plugins: { "@typescript-eslint": tseslint },
    rules: { ...tseslint.configs["recommended"].rules }
  }
]

import basicConfig from './rollup.config.mjs'
import excludeDependenciesFromBundle from "rollup-plugin-exclude-dependencies-from-bundle"
import terser from '@rollup/plugin-terser'

const config = {
  ...basicConfig,
  output: [
    {
      file: 'dist/index.es.js',
      format: 'es',
    },
  ],
  plugins: [
    ...basicConfig.plugins,
    excludeDependenciesFromBundle(),
    terser()
  ]
}

export default config


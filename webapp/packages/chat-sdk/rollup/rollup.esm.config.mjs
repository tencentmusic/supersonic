import basicConfig from './rollup.config.mjs'
// import { terser } from "rollup-plugin-terser"
import excludeDependenciesFromBundle from "rollup-plugin-exclude-dependencies-from-bundle"

const config = {
  ...basicConfig,
  output: [
    {
      file: 'dist/index.es.js',
      format: 'es',
      // plugins: [
      //   terser()
      // ],
    },
  ],
  plugins: [
    ...basicConfig.plugins,
    excludeDependenciesFromBundle(),
  ]
}

export default config


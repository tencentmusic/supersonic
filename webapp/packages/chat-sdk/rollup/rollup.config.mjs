import { nodeResolve } from '@rollup/plugin-node-resolve'
import commonjs from '@rollup/plugin-commonjs'
import json from '@rollup/plugin-json'
import styles from "rollup-plugin-styles";
// This package's Rollup setup does not reliably strip TS syntax with rpt2.
// Babel handles TS/TSX transpilation here so Rollup never parses raw TS.
import { babel } from '../../../node_modules/.pnpm/@rollup+plugin-babel@5.3.1_@babel+core@7.29.0_@types+babel__core@7.20.5_rollup@2.80.0/node_modules/@rollup/plugin-babel/dist/index.js'

process.env.BABEL_ENV = 'production'
process.env.NODE_ENV = 'production'

const presetTypescript = new URL(
  '../../../node_modules/.pnpm/@babel+preset-typescript@7.28.5_@babel+core@7.29.0/node_modules/@babel/preset-typescript/lib/index.js',
  import.meta.url,
).pathname

const presetReact = new URL(
  '../../../node_modules/.pnpm/@babel+preset-react@7.28.5_@babel+core@7.29.0/node_modules/@babel/preset-react/lib/index.js',
  import.meta.url,
).pathname

const config = {
  input: 'src/index.tsx',
  plugins: [
    nodeResolve({
      extensions: ['.mjs', '.js', '.jsx', '.json', '.node', '.ts', '.tsx'],
    }),
    commonjs(),
    json(),
    babel({
      babelHelpers: 'bundled',
      babelrc: false,
      configFile: false,
      exclude: /node_modules/,
      extensions: ['.js', '.jsx', '.ts', '.tsx'],
      presets: [
        presetTypescript,
        [
          presetReact,
          { runtime: 'automatic' },
        ],
      ],
    }),
    styles({
      // mode: ["extract"],
      // modules: true,
      autoModules: id => id.includes(".module."),
    }),
    // less({ output: 'dist/index.css' }),
    // postcss({
    //   plugins: [  
    //     cssnano()  
    //   ]
    // })
  ],
}

export default config

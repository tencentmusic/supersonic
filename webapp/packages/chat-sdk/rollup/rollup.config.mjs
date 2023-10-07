import typescript from 'rollup-plugin-typescript2'
import { nodeResolve } from '@rollup/plugin-node-resolve'
import commonjs from '@rollup/plugin-commonjs'
import json from '@rollup/plugin-json'
import less from 'rollup-plugin-less'
import styles from "rollup-plugin-styles";
import postcss from 'rollup-plugin-postcss'
import cssnano from 'cssnano'

const overrides = {
  compilerOptions: { declaration: true },
  exclude: ["src/**/*.test.tsx", "src/**/*.stories.tsx", "src/**/*.stories.mdx", "src/setupTests.ts"]
}

const config = {
  input: 'src/index.tsx',
  plugins: [
    nodeResolve(),
    commonjs(),
    json(),
    typescript({ tsconfigOverride: overrides }),
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


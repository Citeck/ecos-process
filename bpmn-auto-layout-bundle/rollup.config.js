const resolve = require('@rollup/plugin-node-resolve');
const commonjs = require('@rollup/plugin-commonjs');

module.exports = {
  input: 'src/index.js',
  output: [
    {
      file: 'dist/bpmn-auto-layout-bundle.umd.js',
      format: 'umd',
      name: 'BpmnAutoLayout',
      globals: {}
    }
  ],
  plugins: [
    resolve({
      browser: true,
      preferBuiltins: false
    }),
    commonjs()
  ]
};

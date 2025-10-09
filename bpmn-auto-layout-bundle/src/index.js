import { layoutProcess } from 'bpmn-auto-layout';

// Make the function available globally for GraalVM
if (typeof globalThis !== 'undefined') {
  globalThis.layoutProcess = layoutProcess;
} else if (typeof window !== 'undefined') {
  window.layoutProcess = layoutProcess;
} else if (typeof global !== 'undefined') {
  global.layoutProcess = layoutProcess;
}

export { layoutProcess };

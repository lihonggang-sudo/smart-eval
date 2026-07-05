import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

const projectRoot = resolve(__dirname, '../..');

describe('global layout baseline', () => {
  it('loads a mobile-safe global layout stylesheet from the app entry', () => {
    const mainSource = readFileSync(resolve(projectRoot, 'src/main.tsx'), 'utf8');

    expect(mainSource).toContain("import './styles/global.css';");
  });

  it('keeps the document roots constrained to the viewport', () => {
    const globalStyles = readFileSync(resolve(projectRoot, 'src/styles/global.css'), 'utf8');

    expect(globalStyles).toContain('html,');
    expect(globalStyles).toContain('body,');
    expect(globalStyles).toContain('#root');
    expect(globalStyles).toContain('margin: 0;');
    expect(globalStyles).toContain('width: 100%;');
    expect(globalStyles).toContain('min-height: 100%;');
    expect(globalStyles).toContain('box-sizing: border-box;');
    expect(globalStyles).toContain('overflow-x: hidden;');
  });
});

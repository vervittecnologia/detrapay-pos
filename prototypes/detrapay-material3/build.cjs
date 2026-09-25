const fs = require('node:fs');
const path = require('node:path');
const esbuild = require('esbuild');
process.chdir(__dirname);
fs.mkdirSync('assets', {recursive:true});
esbuild.buildSync({entryPoints:['material.js'],bundle:true,minify:true,format:'iife',target:'chrome100',outfile:'assets/material.bundle.js',legalComments:'linked'});
for (const weight of [400,500,700]) {
  const filename=`roboto-latin-${weight}-normal.woff2`;
  fs.copyFileSync(path.join('node_modules/@fontsource/roboto/files',filename),path.join('assets',filename));
}
fs.copyFileSync('node_modules/@material/web/LICENSE','assets/MATERIAL-LICENSE.txt');
fs.copyFileSync('node_modules/@fontsource/roboto/LICENSE','assets/ROBOTO-LICENSE.txt');

const safeScript = source => source.replace(/<\/script/gi, '<\\/script');
let css = fs.readFileSync('styles.css', 'utf8');
for (const weight of [400,500,700]) {
  const filename = `roboto-latin-${weight}-normal.woff2`;
  const data = fs.readFileSync(path.join('assets', filename)).toString('base64');
  css = css.replaceAll(`url('assets/${filename}')`, `url('data:font/woff2;base64,${data}')`);
}
const standalone = fs.readFileSync('index.shell.html', 'utf8')
  .replace('<link rel="stylesheet" href="styles.css">', () => `<style>\n${css}\n</style>`)
  .replace('<script src="assets/material.bundle.js"></script>', () => `<script>\n${safeScript(fs.readFileSync('assets/material.bundle.js', 'utf8'))}\n</script>`)
  .replace('<script src="model.js"></script>', () => `<script>\n${safeScript(fs.readFileSync('model.js', 'utf8'))}\n</script>`)
  .replace('<script src="app.js"></script>', () => `<script>\n${safeScript(fs.readFileSync('app.js', 'utf8'))}\n</script>`);

if (/<script\s+src=|<link\s+rel=["']stylesheet["']/i.test(standalone)) {
  throw new Error('O HTML autônomo ainda contém dependências externas.');
}
fs.writeFileSync('index.html', standalone);

const conceptCss = fs.readFileSync('concept3.css', 'utf8');
const conceptStandalone = fs.readFileSync('concept3.shell.html', 'utf8')
  .replace('<link rel="stylesheet" href="styles.css">', () => `<style>\n${css}\n</style>`)
  .replace('<link rel="stylesheet" href="concept3.css">', () => `<style>\n${conceptCss}\n</style>`)
  .replace('<script src="assets/material.bundle.js"></script>', () => `<script>\n${safeScript(fs.readFileSync('assets/material.bundle.js', 'utf8'))}\n</script>`)
  .replace('<script src="model.js"></script>', () => `<script>\n${safeScript(fs.readFileSync('model.js', 'utf8'))}\n</script>`)
  .replace('<script src="concept3.js"></script>', () => `<script>\n${safeScript(fs.readFileSync('concept3.js', 'utf8'))}\n</script>`);

if (/<script\s+src=|<link\s+rel=["']stylesheet["']/i.test(conceptStandalone)) {
  throw new Error('A proposta 03 ainda contém dependências externas.');
}
fs.writeFileSync('concept3.html', conceptStandalone);

console.log('Protótipos 02 e 03 gerados com Material 3, Roboto, estilos e lógica incorporados.');

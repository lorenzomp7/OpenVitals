import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile,access} from 'node:fs/promises';
import vm from 'node:vm';
import {ALL,METRICS,DEFAULTS} from '../metrics.js';
import {defaults,validateState} from '../state.js';
test('Android catalog supplies a color, icon and translated title for every widget',()=>{
 assert.equal(ALL.length,40);assert.equal(new Set(ALL.map(m=>m.id)).size,ALL.length);
 for(const m of ALL){assert.match(m.color,/^#[0-9a-f]{6}$/i);assert.ok(m.icon);assert.ok(m.label);assert.ok(m.en);}
 for(const id of DEFAULTS)assert.ok(ALL.some(m=>m.id===id));
 assert.equal(METRICS.find(m=>m.id==='STEPS').color,'#3F9A63');
});
test('PWA backup validates layouts, photos, workouts and full body composition',()=>{
 const s=defaults();s.records=[{date:'2024-01-01',weight:72,leanMass:60,bodyWater:44}];s.workouts=[{id:'demo',date:'2024-01-01',type:'Corsa',duration:50,distance:10}];
 assert.equal(validateState(s).records[0].leanMass,60);
 const bad=structuredClone(s);bad.profile.photo='javascript:alert(1)';assert.throws(()=>validateState(bad));
 bad.profile.photo='';bad.layout.order=['STEPS','STEPS'];assert.throws(()=>validateState(bad));
 bad.layout.order=['STEPS'];bad.workouts[0].duration=-1;assert.throws(()=>validateState(bad));
});
test('install manifest and generated service worker can serve a navigation offline',async()=>{
 const manifest=JSON.parse(await readFile(new URL('../dist/manifest.webmanifest',import.meta.url),'utf8'));
 assert.equal(manifest.display,'standalone');assert.equal(manifest.scope,'./');
 for(const icon of manifest.icons)await access(new URL('../dist/'+icon.src,import.meta.url));
 const source=await readFile(new URL('../dist/sw.js',import.meta.url),'utf8');
 const handlers={},cached=[];let fallback=false;
 const cache={addAll:async assets=>{for(const asset of assets){await access(new URL('../dist/'+(asset==='./'?'index.html':asset),import.meta.url));cached.push(asset);}},put:async()=>{}};
 const context={URL,self:{location:{origin:'https://example.test'},addEventListener:(name,handler)=>handlers[name]=handler,skipWaiting:async()=>{},clients:{claim:async()=>{}}},caches:{open:async()=>cache,keys:async()=>[],match:async key=>{fallback=key==='./';return 'offline-shell';}},fetch:async()=>{throw Error('offline');}};
 vm.runInNewContext(source,context);
 let install;handlers.install({waitUntil:p=>install=p});await install;assert.ok(cached.includes('./app.js'));assert.ok(cached.includes('./manifest.webmanifest'));
 let response;handlers.fetch({request:{method:'GET',url:'https://example.test/',mode:'navigate'},respondWith:p=>response=p});assert.equal(await response,'offline-shell');assert.equal(fallback,true);
});

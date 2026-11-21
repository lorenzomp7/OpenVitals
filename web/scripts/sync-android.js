import {readFile,writeFile} from 'node:fs/promises';
const root=new URL('../../',import.meta.url);
const read=p=>readFile(new URL(p,root),'utf8');
const prefix='app/src/main/kotlin/tech/mmarca/openvitals/';
const [specs,ids,colors,en,it]=await Promise.all([read(prefix+'features/dashboard/DashboardWidgetSpecs.kt'),read(prefix+'features/dashboard/DashboardWidgetId.kt'),read(prefix+'ui/theme/Color.kt'),read('app/src/main/res/values/strings.xml'),read('app/src/main/res/values-it/strings.xml')]);
function strings(xml){return Object.fromEntries([...xml.matchAll(/<string name="([^"]+)"[^>]*>([\s\S]*?)<\/string>/g)].map(m=>[m[1],m[2].replace(/<[^>]+>/g,'').replace(/\\'/g,"'").replace(/&amp;/g,'&').replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&quot;/g,'"')]));}
const english=strings(en),italian=strings(it),palette=Object.fromEntries([...colors.matchAll(/val (\w+) = Color\(0xFF([\da-fA-F]{6})\)/g)].map(m=>[m[1],'#'+m[2]]));
for(const match of colors.matchAll(/val (\w+) = (\w+)Color/g))palette[match[1]]=palette[match[2]+'Color'];
function extract(text,kind){const output={};let pending=[];for(const line of text.split('\n')){pending.push(...[...line.matchAll(/DashboardWidgetId\.(\w+)/g)].map(m=>m[1]));if(line.includes('->')){const found=kind==='label'?line.match(/R.string\.(\w+)/):line.match(/DashboardWidgetMeta\(Icons\.[\w.]+\.(\w+), (\w+)\)/);if(found)for(const id of pending)output[id]=kind==='label'?found[1]:{icon:found[1],color:palette[found[2]]};pending=[];}}return output;}
const labelSection=specs.split('internal fun dashboardWidgetTitle')[1].split('private data class')[0];
const metaSection=specs.split('private fun dashboardWidgetMeta')[1].split('internal fun MutableList')[0];
const labels=extract(labelSection,'label'),meta=extract(metaSection,'meta');
const order=[...ids.split('val DefaultDashboardWidgetIds')[1].split('fun customizable')[0].matchAll(/DashboardWidgetId\.(\w+)/g)].map(m=>m[1]);
const metrics=Object.keys(labels).filter(id=>id!=='CARDIO_LOAD').map(id=>({id,label:italian[labels[id]]??english[labels[id]]??id,en:english[labels[id]]??id,...meta[id]}));
await writeFile(new URL('../android-catalog.js',import.meta.url),`// Generated from Android widget metadata, strings and palette. Do not edit.\nexport const ANDROID_METRICS = ${JSON.stringify(metrics,null,2)};\nexport const DEFAULT_ORDER = ${JSON.stringify(order)};\n`);
console.log(`Synced ${metrics.length} Android widgets.`);

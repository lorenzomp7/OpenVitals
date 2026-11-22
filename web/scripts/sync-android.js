import {readFile,writeFile} from 'node:fs/promises';
const root=new URL('../../',import.meta.url);
const read=p=>readFile(new URL(p,root),'utf8');
const prefix='app/src/main/kotlin/tech/mmarca/openvitals/';
const [specs,ids,categories,colors,en,it]=await Promise.all([read(prefix+'features/dashboard/DashboardWidgetSpecs.kt'),read(prefix+'features/dashboard/DashboardWidgetId.kt'),read(prefix+'features/dashboard/DashboardWidgetCategory.kt'),read(prefix+'ui/theme/Color.kt'),read('app/src/main/res/values/strings.xml'),read('app/src/main/res/values-it/strings.xml')]);
function strings(xml){return Object.fromEntries([...xml.matchAll(/<string name="([^"]+)"[^>]*>([\s\S]*?)<\/string>/g)].map(m=>[m[1],m[2].replace(/<[^>]+>/g,'').replace(/\\'/g,"'").replace(/&amp;/g,'&').replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&quot;/g,'"')]));}
const english=strings(en),italian=strings(it),palette=Object.fromEntries([...colors.matchAll(/val (\w+) = Color\(0xFF([\da-fA-F]{6})\)/g)].map(m=>[m[1],'#'+m[2]]));
for(const match of colors.matchAll(/val (\w+) = (\w+)Color/g))palette[match[1]]=palette[match[2]+'Color'];
// Each widget's icon is its own; its colour is its category's (DashboardWidgetCategory.kt).
function extract(text,pattern){const output={};let pending=[];for(const line of text.split('\n')){pending.push(...[...line.matchAll(/DashboardWidgetId\.(\w+)/g)].map(m=>m[1]));if(line.includes('->')){const found=line.match(pattern);if(found)for(const id of pending)output[id]=found[1];pending=[];}}return output;}
const categoryColor=Object.fromEntries([...categories.split('enum class DashboardWidgetCategory')[1].split('fun dashboardWidgetCategory')[0].matchAll(/^\s*(\w+)\((\w+)\)/gm)].map(m=>[m[1],palette[m[2]]]));
const labelSection=specs.split('internal fun dashboardWidgetTitle')[1].split('private data class')[0];
const iconSection=specs.split('private fun dashboardWidgetIconOf')[1].split('internal fun MutableList')[0];
const categorySection=categories.split('fun dashboardWidgetCategory')[1];
const labels=extract(labelSection,/R.string\.(\w+)/),icons=extract(iconSection,/Icons\.[\w.]+\.(\w+)/),category=extract(categorySection,/DashboardWidgetCategory\.(\w+)/);
const meta=Object.fromEntries(Object.keys(icons).map(id=>[id,{icon:icons[id],color:categoryColor[category[id]]}]));
const order=[...ids.split('val DefaultDashboardWidgetIds')[1].split('fun customizable')[0].matchAll(/DashboardWidgetId\.(\w+)/g)].map(m=>m[1]);
const metrics=Object.keys(labels).filter(id=>id!=='CARDIO_LOAD').map(id=>({id,label:italian[labels[id]]??english[labels[id]]??id,en:english[labels[id]]??id,...meta[id]}));
await writeFile(new URL('../android-catalog.js',import.meta.url),`// Generated from Android widget metadata, strings and palette. Do not edit.\nexport const ANDROID_METRICS = ${JSON.stringify(metrics,null,2)};\nexport const DEFAULT_ORDER = ${JSON.stringify(order)};\n`);
console.log(`Synced ${metrics.length} Android widgets.`);

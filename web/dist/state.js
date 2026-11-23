import {DEFAULTS,ALL,METRICS} from './metrics.js';
import {dateKey,validDate,validateRecords,selectedRecords,aggregate} from './model.js';
export const STORAGE='openvitals-pwa-v2';
export const defaults=()=>({version:2,records:[],workouts:[],profile:{name:'',birth:'',sex:'',height:'',photo:''},preferences:{theme:'light',language:'it',paged:false},layout:{order:[...DEFAULTS],sizes:{},details:{},goals:{}}});
export function validateState(input){
 if(!input||input.version!==2)throw Error('Formato del backup non riconosciuto.');
 const s=defaults();s.records=validateRecords(input.records);
 if(!Array.isArray(input.workouts)||input.workouts.length>10000)throw Error('Elenco attività non valido.');
 const seen=new Set();s.workouts=input.workouts.map(w=>{
  if(!w||typeof w.id!=='string'||w.id.length>100||seen.has(w.id)||!validDate(w.date)||w.date>dateKey()||typeof w.type!=='string'||w.type.length>80||!Number.isFinite(w.duration)||w.duration<=0||w.duration>1440)throw Error('Attività non valida.');seen.add(w.id);
  const clean={id:w.id,date:w.date,type:w.type,duration:w.duration};
  for(const [key,max]of [['distance',2000],['calories',30000],['heart',300],['elevation',30000]])if(w[key]!==undefined){if(!Number.isFinite(w[key])||w[key]<0||w[key]>max)throw Error('Valore attività non valido.');clean[key]=w[key];}
  if(w.time!==undefined){if(!/^\d{2}:\d{2}$/.test(w.time))throw Error('Ora non valida.');clean.time=w.time;}
  return clean;
 });
 const p=input.profile??{};
 for(const key of ['name','birth','sex','height','photo']){if(typeof p[key]==='string')s.profile[key]=p[key];}
 if(s.profile.name.length>100||s.profile.height.length>6||s.profile.photo.length>1000000||s.profile.photo&&!/^data:image\/(png|jpeg|webp);base64,[A-Za-z0-9+/=]+$/.test(s.profile.photo)||s.profile.birth&&(!validDate(s.profile.birth)||s.profile.birth>dateKey()))throw Error('Profilo non valido.');
 const pref=input.preferences??{};s.preferences.theme=['light','dark','amoled','peach','system'].includes(pref.theme)?pref.theme:'light';s.preferences.language=pref.language==='en'?'en':'it';s.preferences.paged=Boolean(pref.paged);
 const l=input.layout??{};if(!Array.isArray(l.order)||l.order.length>ALL.length||new Set(l.order).size!==l.order.length||l.order.some(id=>!ALL.some(m=>m.id===id)))throw Error('Layout non valido.');s.layout.order=[...l.order];
 for(const m of ALL){if([1,2,4].includes(l.sizes?.[m.id]))s.layout.sizes[m.id]=l.sizes[m.id];if([1,2,3].includes(l.details?.[m.id]))s.layout.details[m.id]=l.details[m.id];if(Number.isFinite(l.goals?.[m.id])&&l.goals[m.id]>0&&l.goals[m.id]<=1000000)s.layout.goals[m.id]=l.goals[m.id];}
 return s;
}
export let state=defaults();
export let loadError='';
try{const saved=localStorage.getItem(STORAGE);if(saved)state=validateState(JSON.parse(saved));else{const legacy=localStorage.getItem('openvitals-web-demo-v1');if(legacy)state.records=validateRecords(JSON.parse(legacy));}}catch{loadError='Impossibile leggere i dati locali. Il backup originale non viene cancellato.';}
export function persist(next){const clean=validateState(next);localStorage.setItem(STORAGE,JSON.stringify(clean));state=clean;return state;}
export const label=m=>state.preferences.language==='en'?m.en:m.label;
export const fmt=n=>n===null||n===undefined?'—':new Intl.NumberFormat(state.preferences.language==='en'?'en-GB':'it-IT',{maximumFractionDigits:1}).format(n);
export function dataFor(anchor,range){return selectedRecords(state.records,anchor,range);}
export function valueFor(m,anchor,range){
 if(m.id==='WORKOUT'){return aggregate(dataFor(anchor,range),m)??(workoutsFor(anchor,range).length?workoutsFor(anchor,range).reduce((a,w)=>a+w.duration,0):null);}
 if(m.id==='BMI'){const weight=aggregate(dataFor(anchor,range),METRICS.find(m=>m.id==='WEIGHT'));const h=Number(state.profile.height)/100;return weight&&h?weight/(h*h):aggregate(dataFor(anchor,range),m);}
 return aggregate(dataFor(anchor,range),m);
}
export function workoutsFor(anchor,range){return selectedRecords(state.workouts,anchor,range).sort((a,b)=>b.date.localeCompare(a.date));}
export function escape(s){return String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
export const age=()=>{if(!state.profile.birth)return '';const now=new Date(),b=new Date(state.profile.birth+'T12:00:00');return now.getFullYear()-b.getFullYear()-(now.getMonth()<b.getMonth()||now.getMonth()===b.getMonth()&&now.getDate()<b.getDate()?1:0);};

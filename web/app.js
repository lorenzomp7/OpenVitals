import {METRICS,dateKey,validDate,bounds,shiftDate,validateRecords,selectedRecords,aggregate} from './model.js';
const $ = id => document.getElementById(id);
const KEY = 'openvitals-web-demo-v1';
let records = [], range = 'day', chartMetric = METRICS[0];
const fmt = value => value === null ? '—' : new Intl.NumberFormat('it-IT',{maximumFractionDigits:1}).format(value);
const prettyDate = value => new Date(`${value}T12:00:00`).toLocaleDateString('it-IT',{day:'numeric',month:'short',year:'numeric'});
function status(message) { $('status').textContent = message; }
try { const saved = localStorage.getItem(KEY); if(saved) records=validateRecords(JSON.parse(saved)); }
catch { status('Impossibile leggere i dati salvati. Esporta o conserva il vecchio backup prima di salvare nuovi dati.'); }
$('anchor').value=dateKey(); $('anchor').max=dateKey();
function save(next) {
  try { const clean=validateRecords(next); localStorage.setItem(KEY,JSON.stringify(clean)); records=clean; render(); return true; }
  catch(error) { status(`Salvataggio non riuscito: ${error.message}`); return false; }
}
function render() {
  const anchor=$('anchor').value;
  const selected=selectedRecords(records,anchor,range);
  const [start,end]=bounds(anchor,range);
  $('period-title').textContent=range==='day'?prettyDate(anchor):`${prettyDate(start)} – ${prettyDate(end)}`;
  $('record-count').textContent=`${selected.length} giornat${selected.length===1?'a':'e'} registrat${selected.length===1?'a':'e'}`;
  $('next').disabled=bounds(dateKey(),range)[0] <= start;
  document.querySelectorAll('[data-range]').forEach(button=>button.setAttribute('aria-pressed',String(button.dataset.range===range)));
  const steps=aggregate(selected,METRICS[0]);
  const days = Math.round((new Date(`${end}T12:00:00`)-new Date(`${start}T12:00:00`))/86400000)+1;
  const goal=8000*days;
  $('steps-value').textContent=fmt(steps);
  $('steps-ring').style.setProperty('--progress',`${Math.min(100,(steps??0)/goal*100)}%`);
  $('steps-caption').textContent=steps===null?'Inserisci i tuoi primi passi.':`${fmt(steps)} passi · obiettivo demo ${fmt(goal)}`;
  $('metrics').replaceChildren(...METRICS.slice(1).map(metric=>{
    const card=document.createElement('button'); card.className='metric'; card.style.setProperty('--accent',metric.color);
    card.setAttribute('aria-pressed',String(chartMetric.key===metric.key));
    const icon=document.createElement('span'); icon.className='metric-icon'; icon.textContent=metric.icon; icon.setAttribute('aria-hidden','true');
    const label=document.createElement('span'); label.className='metric-label'; label.textContent=metric.label;
    const value=document.createElement('strong'); value.textContent=`${fmt(aggregate(selected,metric))} ${metric.unit}`;
    const note=document.createElement('small'); note.textContent=metric.mode==='last'?'Ultimo valore nel periodo':metric.mode==='mean'?'Media dei giorni registrati':'Totale del periodo';
    card.append(icon,label,value,note); card.onclick=()=>{chartMetric=metric;render();}; return card;
  }));
  renderChart(selected);
  $('entries').replaceChildren();
  if(!selected.length) { const p=document.createElement('p'); p.className='empty'; p.textContent='Ancora nessun dato in questo periodo. Aggiungi una giornata o prova i dati di esempio.'; $('entries').append(p); }
  selected.slice().reverse().forEach(record=>{
    const row=document.createElement('button'); row.className='entry-row';
    const date=document.createElement('strong'); date.textContent=prettyDate(record.date);
    const values=document.createElement('span'); values.textContent=METRICS.filter(m=>record[m.key]!==undefined).map(m=>`${m.label}: ${fmt(record[m.key])} ${m.unit}`).join(' · ');
    row.append(date,values); row.onclick=()=>openEntry(record.date); $('entries').append(row);
  });
}
function renderChart(selected) {
  const metric=chartMetric, values=selected.filter(r=>r[metric.key]!==undefined);
  $('chart-title').textContent=`${metric.label} · storico`;
  $('chart-description').textContent=`${fmt(aggregate(selected,metric))} ${metric.unit}`;
  $('chart').replaceChildren();
  $('chart-empty').textContent=values.length?'Ogni barra rappresenta una giornata registrata. Le giornate senza dati sono escluse.':'Nessun valore disponibile per questo parametro.';
  if(!values.length) return;
  const chart=document.createElementNS('http://www.w3.org/2000/svg','svg');
  chart.setAttribute('viewBox','0 0 900 190'); chart.setAttribute('role','img'); chart.setAttribute('aria-label',`${metric.label}: ${values.map(r=>`${prettyDate(r.date)}, ${fmt(r[metric.key])} ${metric.unit}`).join('; ')}`);
  const max=Math.max(1,...values.map(r=>r[metric.key]));
  values.forEach((record,i)=>{
    const rect=document.createElementNS(chart.namespaceURI,'rect');
    rect.setAttribute('x',String(i*900/values.length+2)); rect.setAttribute('width',String(Math.max(1,900/values.length-4)));
    const h=Math.max(1,record[metric.key]/max*165); rect.setAttribute('y',String(180-h)); rect.setAttribute('height',String(h)); rect.setAttribute('rx','3'); rect.setAttribute('fill',metric.color);
    const title=document.createElementNS(chart.namespaceURI,'title'); title.textContent=`${prettyDate(record.date)}: ${fmt(record[metric.key])} ${metric.unit}`; rect.append(title); chart.append(rect);
  }); $('chart').append(chart);
}
function openEntry(date=$('anchor').value) {
  $('entry-form').reset(); $('entry-form').elements.date.value=date; $('entry-form').elements.date.max=dateKey();
  const record=records.find(r=>r.date===date)??{};
  $('fields').replaceChildren(...METRICS.map(metric=>{
    const label=document.createElement('label'); label.textContent=`${metric.label} (${metric.unit})`;
    const input=document.createElement('input'); input.type='number'; input.name=metric.key; input.min='0'; input.max=String(metric.max); input.step=String(metric.step); input.placeholder='Non registrato'; input.value=record[metric.key]??'';
    label.append(input); return label;
  }));
  $('form-error').textContent=''; $('entry-dialog').showModal();
}
$('add').onclick=()=>openEntry(); $('edit').onclick=()=>openEntry();
$('close-dialog').onclick=()=>$('entry-dialog').close();
$('entry-form').elements.date.onchange=event=>{ const date=event.target.value; if(validDate(date)&&date<=dateKey()){ $('entry-dialog').close(); openEntry(date); } };
$('entry-form').onsubmit=event=>{
  event.preventDefault(); const data=new FormData(event.target), record={date:data.get('date')};
  METRICS.forEach(metric=>{const v=data.get(metric.key);if(v!=='')record[metric.key]=Number(v);});
  try { validateRecords([record]); } catch(error) { $('form-error').textContent=error.message; return; }
  if(save([...records.filter(r=>r.date!==record.date),record])) { $('anchor').value=record.date; render(); $('entry-dialog').close(); status('Giornata salvata in questo browser.'); }
  else $('form-error').textContent='Impossibile salvare. Controlla che il browser consenta la memorizzazione locale.';
};
$('delete-entry').onclick=()=>{ const date=$('entry-form').elements.date.value;if(records.some(r=>r.date===date)&&confirm(`Eliminare i dati del ${prettyDate(date)}?`)&&save(records.filter(r=>r.date!==date))) { $('entry-dialog').close();status('Giornata eliminata.'); } };
$('ranges').onclick=event=>{if(event.target.dataset.range){range=event.target.dataset.range;render();}};
$('anchor').onchange=()=>{if(!validDate($('anchor').value)||$('anchor').value>dateKey())$('anchor').value=dateKey();render();};
for(const [id,direction] of [['previous',-1],['next',1]]) $(id).onclick=()=>{ $('anchor').value=direction>0?[shiftDate($('anchor').value,range,direction),dateKey()].sort()[0]:shiftDate($('anchor').value,range,direction);render(); };
$('steps-ring').onclick=()=>{chartMetric=METRICS[0];render();};
$('sample').onclick=()=>{
  if(!confirm('Aggiungere 7 giornate di esempio? Le giornate già registrate saranno conservate.'))return;
  const next=[...records];
  for(let i=6;i>=0;i--){const d=new Date();d.setDate(d.getDate()-i);const date=dateKey(d);if(!next.some(r=>r.date===date))next.push({date,steps:6500+i*850,distance:4.8+i*0.6,calories:340+i*45,sleep:7+i*0.1,weight:72+i*0.1,heart:58+i,water:1.8+i*0.1});}
  if(save(next)) {range='week';$('anchor').value=dateKey();render();status('Dati di esempio aggiunti: sono valori fittizi modificabili.');}
};
$('export').onclick=()=>{const url=URL.createObjectURL(new Blob([JSON.stringify({version:1,records},null,2)],{type:'application/json'}));const link=document.createElement('a');link.href=url;link.download=`openvitals-demo-${dateKey()}.json`;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);};
$('import').onchange=async event=>{
  const file=event.target.files[0];if(!file)return;
  try {if(file.size>5*1024*1024)throw new Error('Il file supera 5 MB.');const data=JSON.parse(await file.text());if(data.version!==1)throw new Error('Versione backup non supportata.');const clean=validateRecords(data.records);if(confirm(`Sostituire tutte le giornate con le ${clean.length} del backup?`)&&save(clean))status('Backup importato.');}
  catch(error){status(`Importazione non riuscita: ${error.message}`);}finally{event.target.value='';}
};
$('clear').onclick=()=>{if(confirm('Cancellare tutte le giornate di questa demo? Esporta prima un backup se vuoi conservarle.')&&save([]))status('Tutti i dati demo sono stati cancellati.');};
render();

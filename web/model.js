import {METRICS} from './metrics.js';
export {METRICS};
export function dateKey(date = new Date()) {
  return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
}
export function validDate(value) {
  if(typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const d = new Date(`${value}T12:00:00`);
  return Number.isFinite(d.getTime()) && dateKey(d) === value && value >= '1900-01-01';
}
export function bounds(anchor, range) {
  const start = new Date(`${anchor}T12:00:00`), end = new Date(start);
  if(range === 'week') { start.setDate(start.getDate() - (start.getDay()+6)%7); end.setTime(start.getTime()); end.setDate(end.getDate()+6); }
  if(range === 'month') { start.setDate(1); end.setMonth(end.getMonth()+1,0); }
  if(range === 'year') { start.setMonth(0,1); end.setMonth(11,31); }
  return [dateKey(start), dateKey(end)];
}
export function shiftDate(anchor, range, direction) {
  const d = new Date(`${anchor}T12:00:00`);
  if(range === 'day' || range === 'week') d.setDate(d.getDate()+direction*(range==='week'?7:1));
  else if(range === 'month') { d.setDate(1); d.setMonth(d.getMonth()+direction); }
  else { d.setMonth(0,1); d.setFullYear(d.getFullYear()+direction); }
  return dateKey(d);
}
export function validateRecords(records) {
  if(!Array.isArray(records) || records.length > 20000) throw new Error('Il backup deve contenere al massimo 20.000 giornate.');
  const dates = new Set();
  return records.map(record => {
    if(!record || !validDate(record.date) || record.date > dateKey() || dates.has(record.date)) throw new Error('Data non valida, futura o duplicata nel backup.');
    dates.add(record.date);
    const clean = {date:record.date};
    for(const metric of METRICS) {
      const value = record[metric.key];
      if(value === undefined || value === null) continue;
      if(typeof value !== 'number' || !Number.isFinite(value) || value < 0 || value > metric.max || (metric.step === 1 && !Number.isInteger(value))) throw new Error(`Valore non valido: ${metric.label}.`);
      clean[metric.key] = value;
    }
    if(Object.keys(clean).length === 1) throw new Error('Ogni giornata deve contenere almeno un valore.');
    return clean;
  }).sort((a,b)=>a.date.localeCompare(b.date));
}
export function selectedRecords(records, anchor, range) {
  const [start,end] = bounds(anchor,range);
  return records.filter(r => r.date >= start && r.date <= end).sort((a,b)=>a.date.localeCompare(b.date));
}
export function aggregate(records, metric) {
  const values = records.filter(r=>Number.isFinite(r[metric.key])).sort((a,b)=>a.date.localeCompare(b.date)).map(r=>r[metric.key]);
  if(!values.length) return null;
  if(metric.mode === 'last') return values.at(-1);
  const sum = values.reduce((a,b)=>a+b,0);
  return metric.mode === 'mean' ? sum/values.length : sum;
}

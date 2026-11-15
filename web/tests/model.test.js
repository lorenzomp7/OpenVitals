import test from 'node:test';
import assert from 'node:assert/strict';
import {METRICS,validDate,bounds,shiftDate,validateRecords,selectedRecords,aggregate} from '../model.js';
test('periods handle Monday weeks, leap years and month boundaries',()=>{
  assert.deepEqual(bounds('2024-03-03','week'),['2024-02-26','2024-03-03']);
  assert.deepEqual(bounds('2024-02-15','month'),['2024-02-01','2024-02-29']);
  assert.deepEqual(bounds('2024-06-12','year'),['2024-01-01','2024-12-31']);
  assert.equal(shiftDate('2024-03-31','month',-1),'2024-02-01');
  assert.equal(validDate('2024-02-30'),false);
});
test('missing measurements stay missing while zero is preserved',()=>{
  const records=validateRecords([{date:'2024-02-01',steps:0},{date:'2024-02-02',weight:70},{date:'2024-02-03',weight:71}]);
  assert.equal(aggregate(records,METRICS[0]),0);
  assert.equal(aggregate(records,METRICS.find(m=>m.key==='sleep')),null);
  assert.equal(aggregate(records,METRICS.find(m=>m.key==='weight')),71);
  assert.equal(selectedRecords(records,'2024-02-02','day').length,1);
});
test('import rejects corrupt, duplicate, future and hostile data',()=>{
  for(const input of [[{date:'2024-01-01',steps:-1}],[{date:'2024-01-01',steps:1.5}],[{date:'2024-01-01',sleep:25}],[{date:'2024-01-01',weight:'<script>'}],[{date:'2024-01-01'}],[{date:'2999-01-01',steps:1}],[{date:'2024-01-01',steps:1},{date:'2024-01-01',steps:2}]]) assert.throws(()=>validateRecords(input));
  assert.deepEqual(validateRecords([{date:'2024-01-01',steps:1,unknown:'ignored'}]),[{date:'2024-01-01',steps:1}]);
});
test('totals and means use only the selected days with recorded values',()=>{
  const records=validateRecords([{date:'2024-01-01',steps:1000,sleep:6},{date:'2024-01-02',steps:2000},{date:'2024-01-03',sleep:8},{date:'2024-02-01',steps:9000}]);
  const selected=selectedRecords(records,'2024-01-15','month');
  assert.equal(aggregate(selected,METRICS[0]),3000);
  assert.equal(aggregate(selected,METRICS.find(m=>m.key==='sleep')),7);
});

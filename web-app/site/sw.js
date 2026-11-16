// Keeps the web copy usable offline. A new build has a new version and replaces the old cache.
const VERSION = 'openvitals-web-__VERSION__';
const SHELL = ['./', 'index.html', 'style.css', 'app.js', 'app.json', 'manifest.webmanifest', 'icon-192.png', 'icon-512.png'];
const IMAGES = __IMAGES__;

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(VERSION).then(async (cache) => {
    await cache.addAll(SHELL);
    // Pictures are many: fetched in the background, a failure does not stop the install.
    IMAGES.forEach((url) => cache.add(url).catch(() => {}));
  }));
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== VERSION).map((k) => caches.delete(k)))));
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);
  if (event.request.method !== 'GET' || url.origin !== location.origin) return;
  // The list of screens comes from the network first, so a new build shows at once.
  if (url.pathname.endsWith('app.json')) {
    event.respondWith(fetch(event.request).then((response) => {
      const copy = response.clone();
      caches.open(VERSION).then((cache) => cache.put(event.request, copy));
      return response;
    }).catch(() => caches.match(event.request)));
    return;
  }
  event.respondWith(caches.match(event.request).then((hit) => hit || fetch(event.request)));
});

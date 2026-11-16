// The web copy of OpenVitals: pictures of the real app, taken from the APK of the commit
// named below, with every recorded tap a link. Browser back is the app's back.
(async () => {
  const app = await (await fetch('app.json', { cache: 'no-cache' })).json();
  const content = document.getElementById('content');
  const bar = document.getElementById('bar');
  const scroller = document.getElementById('screen');
  const backButton = document.getElementById('back');
  document.getElementById('info').textContent = `Dati di esempio · build ${app.commit}`;

  const area = (link, height) => {
    const [x1, y1, x2, y2] = link.b;
    const hot = document.createElement('button');
    hot.className = 'hot';
    hot.type = 'button';
    hot.title = link.l || '';
    hot.setAttribute('aria-label', link.l || 'Apri');
    hot.style.left = `${(x1 / app.width) * 100}%`;
    hot.style.width = `${((x2 - x1) / app.width) * 100}%`;
    hot.style.top = `${(y1 / height) * 100}%`;
    hot.style.height = `${((y2 - y1) / height) * 100}%`;
    hot.addEventListener('click', () => go(link.to));
    return hot;
  };

  const layer = (target, src, height, links, alt) => {
    target.replaceChildren();
    if (!src) return;
    const img = new Image();
    img.src = src;
    img.alt = alt;
    img.width = app.width;
    img.height = height;
    target.append(img, ...links.map((link) => area(link, height)));
  };

  const show = (id) => {
    const screen = app.screens[id] || app.screens[app.start];
    layer(content, `screens/${id}.webp`, screen.h, screen.links, 'Schermata di OpenVitals');
    layer(bar, screen.bar ? `screens/${id}-bar.webp` : null, screen.bar, screen.barLinks, '');
    scroller.scrollTop = 0;
    backButton.disabled = id === app.start;
  };

  const go = (id) => { history.pushState({ id }, '', `#${id}`); show(id); };
  window.addEventListener('popstate', (event) => show(event.state?.id || location.hash.slice(1) || app.start));
  backButton.addEventListener('click', () => history.back());
  document.getElementById('home').addEventListener('click', () => go(app.start));
  document.addEventListener('keydown', (event) => { if (event.key === 'Escape') history.back(); });

  const first = app.screens[location.hash.slice(1)] ? location.hash.slice(1) : app.start;
  history.replaceState({ id: first }, '', `#${first}`);
  show(first);

  if ('serviceWorker' in navigator) navigator.serviceWorker.register('sw.js');
})();

// The web copy of OpenVitals: pictures of the real app, taken from the APK of the commit
// named below, with every recorded tap a link. Browser back is the app's back.
(async () => {
  const content = document.getElementById('content');
  const bar = document.getElementById('bar');
  const scroller = document.getElementById('screen');
  const backButton = document.getElementById('back');
  const info = document.getElementById('info');

  let app;
  try {
    const response = await fetch('app.json', { cache: 'no-cache' });
    if (!response.ok) throw new Error(String(response.status));
    app = await response.json();
  } catch (error) {
    // 86: offline with no copy, or a build without screens: say so instead of a black page.
    content.innerHTML = '<p class="empty">La copia web non è disponibile ora. Riprova più tardi.</p>';
    backButton.disabled = true;
    return;
  }
  info.textContent = `Dati di esempio · build ${app.commit}`;

  // 86: how deep the visitor is inside this visit. A link opened straight to a screen has no
  // page behind it in this site, and the browser's back would leave it: go to the start instead.
  let depth = history.state?.depth ?? 0;

  const goBack = () => {
    if (depth > 0) history.back();
    else if (current !== app.start) { history.replaceState({ id: app.start, depth: 0 }, '', `#${app.start}`); show(app.start); }
  };

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
    hot.addEventListener('click', (event) => {
      event.preventDefault();
      // 86: the app's own back arrow goes back, as it does in the app.
      if (link.to === 'back') goBack();
      else if (app.screens[link.to]) go(link.to);
    });
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
    img.decoding = 'async';
    // 86: the larger areas first, so a card that holds buttons never covers them: the last
    // drawn is the one a tap reaches.
    const ordered = (links || [])
      .filter((link) => link.to === 'back' || app.screens[link.to])
      .slice()
      .sort((a, b) => ((b.b[2] - b.b[0]) * (b.b[3] - b.b[1])) - ((a.b[2] - a.b[0]) * (a.b[3] - a.b[1])));
    target.append(img, ...ordered.map((link) => area(link, height)));
  };

  let current = null;
  const show = (id) => {
    const known = app.screens[id] ? id : app.start;
    const screen = app.screens[known];
    current = known;
    layer(content, `screens/${known}.webp`, screen.h, screen.links, 'Schermata di OpenVitals');
    layer(bar, screen.bar ? `screens/${known}-bar.webp` : null, screen.bar, screen.barLinks, '');
    scroller.scrollTop = 0;
    backButton.disabled = known === app.start && depth === 0;
  };

  const go = (id) => {
    if (id === current) return;
    depth += 1;
    history.pushState({ id, depth }, '', `#${id}`);
    show(id);
  };
  window.addEventListener('popstate', (event) => {
    depth = event.state?.depth ?? 0;
    show(event.state?.id || location.hash.slice(1) || app.start);
  });
  backButton.addEventListener('click', goBack);
  document.getElementById('home').addEventListener('click', () => { if (current !== app.start) go(app.start); });
  document.addEventListener('keydown', (event) => { if (event.key === 'Escape') goBack(); });

  const hash = location.hash.slice(1);
  const first = app.screens[hash] ? hash : app.start;
  history.replaceState({ id: first, depth }, '', `#${first}`);
  show(first);

  if ('serviceWorker' in navigator) navigator.serviceWorker.register('sw.js').catch(() => {});
})();

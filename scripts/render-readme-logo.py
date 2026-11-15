#!/usr/bin/env python3
"""Renders the launcher icon (white round background + ic_launcher_foreground.xml) to a PNG.

Usage: python3 scripts/render-readme-logo.py app/src/main/res/drawable/ic_launcher_foreground.xml docs/images/readme-logo.png 512
Needs numpy, Pillow and svgpathtools. Run it whenever the icon changes (see AGENTS.md).
The sweep gradient stops mirror the ones in the foreground vector.
"""
import re, sys, math
import numpy as np
from PIL import Image, ImageDraw
from svgpathtools import parse_path
src, out, size = sys.argv[1], sys.argv[2], int(sys.argv[3])
xml = open(src).read()
paths = re.findall(r'android:pathData="([^"]+)"', xml)
stops = [(0.0,(0x42,0x85,0xF4)),(0.25,(0x34,0xA8,0x53)),(0.5,(0xFB,0xBC,0x04)),(0.75,(0xEA,0x43,0x35)),(1.0,(0x42,0x85,0xF4))]
S = size*4  # supersample
scale = S/108.0
mask = Image.new("L", (S,S), 0)
d = ImageDraw.Draw(mask)
for pd in paths:
    p = parse_path(pd)
    # split into closed subpaths
    for sub in p.continuous_subpaths():
        pts=[]
        for seg in sub:
            n = max(8, int(seg.length()*4))
            for k in range(n):
                z = seg.point(k/n); pts.append((z.real*scale, z.imag*scale))
        z = sub[-1].end; pts.append((z.real*scale, z.imag*scale))
        if len(pts)>2: d.polygon(pts, fill=255)
yy, xx = np.mgrid[0:S,0:S].astype(float)
ang = (np.degrees(np.arctan2(yy-54*scale, xx-54*scale)) % 360)/360.0
col = np.zeros((S,S,3))
for (a,ca),(b,cb) in zip(stops, stops[1:]):
    m = (ang>=a)&(ang<=b); t=((ang-a)/(b-a))[...,None]
    col = np.where(m[...,None], np.array(ca)*(1-t)+np.array(cb)*t, col)
# white circle background (adaptive icon, round mask)
bg = np.hypot(xx-S/2, yy-S/2) <= S/2
img = np.zeros((S,S,4))
img[...,:3] = 255; img[...,3] = bg*255
m = np.array(mask)/255.0
img[...,:3] = img[...,:3]*(1-m[...,None]) + col*m[...,None]
img[...,3] = np.maximum(img[...,3], m*255)
Image.fromarray(img.astype(np.uint8),"RGBA").resize((size,size), Image.LANCZOS).save(out)

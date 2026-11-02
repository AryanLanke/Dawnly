"""Generates every Dawnly icon concept: SVG masters, monochrome SVGs, 512px PNGs,
review sheets and the index.html contact sheet.

Canvas: 1024x1024 = 108dp adaptive canvas. Safe circle (66dp) = radius 313 around 512,512.
Usage:  python build.py            write SVGs + concept.md for all concepts
        python build.py --png      also render day/night PNGs (headless Chrome)
        python build.py --review A B   render a review sheet for concepts A..B
        python build.py --index    write index.html
        python build.py --android day|night   export that state as the app's launcher icon layers
"""
import math, os, re, shutil, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)  # design/logo
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
from kit import CONCEPTS, _n
import concepts  # noqa: F401  (registers concepts in order)

# ---------------------------------------------------------------- output
def doc(bg, fg):
    out = ['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="1024" height="1024">']
    if bg is None:
        out.append(f'<g id="monochrome">{fg}</g>')
    else:
        b = f'<rect width="1024" height="1024" fill="{bg}"/>' if bg.startswith('#') else bg
        out.append(f'<g id="background">{b}</g><g id="foreground">{fg}</g>')
    out.append('</svg>\n')
    return ''.join(out)

def folder(i, c): return f'{i + 1:02d}-{c["slug"]}'

def write_all():
    keep = {folder(i, c) for i, c in enumerate(CONCEPTS)}
    for name in os.listdir(ROOT):  # drop folders of cut/renumbered concepts
        if re.fullmatch(r'\d\d-[a-z0-9-]+', name) and name not in keep:
            shutil.rmtree(os.path.join(ROOT, name))
    for i, c in enumerate(CONCEPTS):
        d = os.path.join(ROOT, folder(i, c))
        os.makedirs(d, exist_ok=True)
        _n[0] = 0
        r = c['fn']()
        names = {'lday': 'day.svg', 'lnight': 'night.svg'}
        files = {names[k]: r[k] for k in names if k in r}
        for old in os.listdir(d):  # drop anything that is no longer generated
            if old.replace('-512.png', '.svg') not in files:
                os.remove(os.path.join(d, old))
        for fn, s in files.items():
            with open(os.path.join(d, fn), 'w', encoding='utf-8', newline='\n') as fh:
                fh.write(s)

def chrome_shot(src, out, w, h, scale=1.0):
    subprocess.run([CHROME, '--headless=new', '--disable-gpu', '--hide-scrollbars',
                    f'--force-device-scale-factor={scale}', f'--window-size={w},{h}',
                    '--default-background-color=00000000', f'--screenshot={out}',
                    'file:///' + src.replace('\\', '/')],
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)

def render_pngs(sel):
    for i, c in sel:
        d = os.path.join(ROOT, folder(i, c))
        for st in ('day', 'night'):
            if not os.path.exists(os.path.join(d, f'{st}.svg')):
                continue
            chrome_shot(os.path.join(d, f'{st}.svg'), os.path.join(d, f'{st}-512.png'), 1024, 1024, 0.5)

# ---------------------------------------------------------------- android
APP_RES = os.path.join(os.path.dirname(os.path.dirname(ROOT)), 'app', 'src', 'main', 'res')
DENSITIES = {'mdpi': 108, 'hdpi': 162, 'xhdpi': 216, 'xxhdpi': 324, 'xxxhdpi': 432}

def export_android(state):
    """Renders the chosen state's adaptive-icon layers into the app's mipmap folders as
    ic_launcher_background/foreground/monochrome.png (108dp at every density)."""
    import tempfile
    back, fore, mono = CONCEPTS[0]['fn']()[f'android_{state}']
    with tempfile.TemporaryDirectory() as tmp:
        for name, svg in (('background', back), ('foreground', fore), ('monochrome', mono)):
            src = os.path.join(tmp, f'{name}.svg')
            with open(src, 'w', encoding='utf-8') as fh:
                fh.write(svg)
            for dens, px in DENSITIES.items():
                out_dir = os.path.join(APP_RES, f'mipmap-{dens}')
                os.makedirs(out_dir, exist_ok=True)
                chrome_shot(src, os.path.join(out_dir, f'ic_launcher_{name}.png'), 1024, 1024, px / 1024)

# ---------------------------------------------------------------- sheets
CSS = """
*{box-sizing:border-box;margin:0;padding:0}
body{font:14px/1.4 system-ui,'Segoe UI',sans-serif;color:#1d2433;background:#fff}
.ic{position:relative;overflow:hidden;flex:none}
.ic img{position:absolute;left:-25%;top:-25%;width:150%;height:150%}
.sq{border-radius:23%}
.mono{background:#DCE4FA}
.mono img{opacity:.82}
.dk .mono{background:#27304A}
.dk .mono img{filter:invert(1);opacity:.88}
"""

def icon(rel, size, cls='sq'):
    return f'<div class="ic {cls}" style="width:{size}px;height:{size}px"><img src="{rel}"></div>'

def review(a, b):
    sel = [(i, c) for i, c in enumerate(CONCEPTS) if a <= i + 1 <= b]
    cells = []
    for i, c in sel:
        p = folder(i, c) + '/'
        small = lambda bgc, cls='': (
            f'<div class="{cls}" style="background:{bgc};padding:10px;display:flex;gap:8px;border-radius:8px">'
            f'{icon(p + "day.svg", 48)}{icon(p + "night.svg", 48)}'
            f'{icon(p + "day-monochrome.svg", 48, "sq mono")}{icon(p + "night-monochrome.svg", 48, "sq mono")}</div>')
        cells.append(
            f'<div style="padding:14px;border:1px solid #e3e6ee;border-radius:12px">'
            f'<div style="font-weight:600;margin-bottom:8px">{i + 1:02d} {c["name"]}</div>'
            f'<div style="display:flex;gap:12px;align-items:flex-start">'
            f'<img src="{p}day.svg" width="200" height="200" style="border-radius:6px">'
            f'<img src="{p}night.svg" width="200" height="200" style="border-radius:6px">'
            f'<div style="display:flex;flex-direction:column;gap:8px">{small("#F1F2F5")}{small("#14161c", "dk")}'
            f'<div style="display:flex;gap:8px">{icon(p + "day.svg", 96)}{icon(p + "night.svg", 96)}</div></div>'
            f'</div></div>')
    page = (f'<!doctype html><meta charset="utf-8"><style>{CSS}</style>'
            f'<div style="display:grid;grid-template-columns:1fr 1fr;gap:14px;padding:14px;width:1500px">'
            f'{"".join(cells)}</div>')
    src = os.path.join(ROOT, f'_review-{a:02d}-{b:02d}.html')
    with open(src, 'w', encoding='utf-8') as fh:
        fh.write(page)
    rows = (len(sel) + 1) // 2
    out = os.path.join(HERE, f'review-{a:02d}-{b:02d}.png')
    chrome_shot(src, out, 1530, 40 + rows * 322)
    os.remove(src)
    print(out)

if __name__ == '__main__':
    args = sys.argv[1:]
    write_all()
    if '--png' in args:
        render_pngs(list(enumerate(CONCEPTS)))
    if '--review' in args:
        k = args.index('--review')
        review(int(args[k + 1]), int(args[k + 2]))
    if '--index' in args:
        from index import write_index
        write_index(CONCEPTS, folder, ROOT)
    if '--android' in args:
        export_android(args[args.index('--android') + 1])
    print(len(CONCEPTS), 'concepts')

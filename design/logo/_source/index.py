"""Writes design/logo/index.html, the contact sheet for every concept."""
import os, re, html


CSS = """
*{box-sizing:border-box;margin:0;padding:0}
body{font:14px/1.45 system-ui,'Segoe UI',Roboto,sans-serif;color:#1d2433;background:#fafafa;padding:32px}
h1{font-size:22px;font-weight:650;margin-bottom:6px}
.intro{color:#5a6377;max-width:900px;margin-bottom:24px}
.intro a{color:#1A4FD6}
.grid{display:grid;grid-template-columns:minmax(460px,620px);gap:18px}
.card{background:#fff;border:1px solid #e3e6ee;border-radius:14px;padding:16px}
.head{display:flex;align-items:baseline;gap:8px;margin-bottom:10px}
.num{font-variant-numeric:tabular-nums;color:#8a93a6;font-weight:600}
.name{font-weight:650;font-size:16px}
.dir{color:#8a93a6;font-size:12px;margin-left:auto}
.badge{background:#1A4FD6;color:#fff;border-radius:99px;font-size:11px;font-weight:650;padding:1px 8px}
.row{display:flex;gap:10px;margin-bottom:10px}
.panel{flex:1;border-radius:10px;padding:12px;display:flex;align-items:flex-end;gap:10px}
.light{background:#ECEEF3}.dark{background:#111318}
.pair{display:flex;flex-direction:column;align-items:center;gap:4px}
.lbl{font-size:10px;letter-spacing:.04em;text-transform:uppercase;color:#8a93a6}
.ic{position:relative;overflow:hidden;flex:none;border-radius:23%}
.ic img{position:absolute;left:-25%;top:-25%;width:150%;height:150%}
.mono{background:#D9E2FF}.mono img{opacity:.8}
.dark .mono{background:#2A3350}.dark .mono img{filter:invert(1);opacity:.9}
.meta{color:#5a6377;font-size:13px}
.meta a{color:#1A4FD6;text-decoration:none}
.sub{font-size:11px;font-weight:650;letter-spacing:.06em;text-transform:uppercase;color:#5a6377;margin:4px 0 6px}
"""


def icon(src, size, mono=False):
    return (f'<div class="ic{" mono" if mono else ""}" style="width:{size}px;height:{size}px">'
            f'<img src="{src}" alt=""></div>')


def panel(p, cls, day, night):
    return (f'<div class="panel {cls}">'
            f'<div class="pair">{icon(p + day, 108)}<span class="lbl">day</span></div>'
            f'<div class="pair">{icon(p + night, 108)}<span class="lbl">night</span></div>'
            f'<div class="pair">{icon(p + day, 48)}{icon(p + night, 48)}<span class="lbl">48px</span></div></div>')


PAIRS = [('Day + night', 'day.svg', 'night.svg')]


def write_index(concepts, folder, root):
    cards = []
    for i, c in enumerate(concepts):
        p = folder(i, c) + '/'
        rows = ''.join(f'<div class="sub">{name}</div><div class="row">{panel(p, "light", d, n)}</div>'
                       f'<div class="row">{panel(p, "dark", d, n)}</div>' for name, d, n in PAIRS)
        cards.append(f'<div class="card"><div class="head"><span class="name">{html.escape(c["name"])}</span></div>'
                     f'{rows}<div class="meta">Files: <a href="{p}day.svg">day.svg</a> · <a href="{p}night.svg">night.svg</a>'
                     f' (and a 512px PNG of each)</div></div>')
    page = (
        '<!doctype html><html lang="en"><head><meta charset="utf-8">'
        '<meta name="viewport" content="width=device-width,initial-scale=1">'
        f'<title>Dawnly icon: Floor Light</title><style>{CSS}</style></head><body>'
        '<h1>Dawnly icon: Floor Light</h1>'
        '<p class="intro">Liquid glass, in the style of Apple’s iOS 26 icons. Day and night are the same '
        'scene at two light levels: same window, same glass, same light on the floor; at night the blue deepens '
        'toward navy and the glass gets quieter. Icons are shown the way an Android launcher shows them: the '
        'central 72dp of the 108dp adaptive canvas, in a rounded-square mask, at 108px and 48px, on light and '
        'dark home screens.</p>'
        f'<div class="grid">{"".join(cards)}</div></body></html>\n')
    with open(os.path.join(root, 'index.html'), 'w', encoding='utf-8', newline='\n') as fh:
        fh.write(page)

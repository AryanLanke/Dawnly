"""Builds the Dawnly app icon from icon.py. Renders with headless Chrome (no Python packages needed).

    python build.py                  egg.svg, egg-512.png and preview.html
    python build.py --android egg    also export it as the app's launcher icon layers

The launcher icon ships as bitmap layers (mipmap-*/ic_launcher_{background,foreground,monochrome}.png)
because the glass needs blur, which Android vector drawables can't draw.
"""
import os
import subprocess
import sys
import tempfile

import icon

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.dirname(HERE)                                   # design/app-icon
REPO = os.path.dirname(os.path.dirname(OUT))
APP_RES = os.path.join(REPO, 'app', 'src', 'main', 'res')
CHROME = r'C:\Program Files\Google\Chrome\Application\chrome.exe'
DENSITIES = {'mdpi': 108, 'hdpi': 162, 'xhdpi': 216, 'xxhdpi': 324, 'xxxhdpi': 432}  # 108dp each


def render(svg_path, png_path, px):
    """Screenshots a 1024px SVG at px x px, keeping transparency."""
    subprocess.run([CHROME, '--headless=new', '--disable-gpu', '--hide-scrollbars',
                    f'--force-device-scale-factor={px / 1024}', '--window-size=1024,1024',
                    '--default-background-color=00000000', f'--screenshot={png_path}',
                    'file:///' + svg_path.replace('\\', '/')],
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)


def write(path, text):
    with open(path, 'w', encoding='utf-8', newline='\n') as fh:
        fh.write(text)


def masters():
    for name, make in icon.STATES.items():
        svg = os.path.join(OUT, f'{name}.svg')
        write(svg, make().svg())
        render(svg, os.path.join(OUT, f'{name}-512.png'), 512)
    write(os.path.join(OUT, 'preview.html'), PREVIEW)


def export_android(state):
    back, fore, mono = icon.STATES[state]().layers()
    with tempfile.TemporaryDirectory() as tmp:
        for layer, svg in (('background', back), ('foreground', fore), ('monochrome', mono)):
            src = os.path.join(tmp, f'{layer}.svg')
            write(src, svg)
            for density, px in DENSITIES.items():
                folder = os.path.join(APP_RES, f'mipmap-{density}')
                os.makedirs(folder, exist_ok=True)
                render(src, os.path.join(folder, f'ic_launcher_{layer}.png'), px)


# Shows the icon the way a launcher does: the central 72dp of the 108dp canvas, in a rounded mask.
PREVIEW = """<!doctype html><html lang="en"><head><meta charset="utf-8"><title>Dawnly app icon</title><style>
body{margin:0;padding:32px;font:14px/1.45 system-ui,'Segoe UI',sans-serif;color:#1d2433;background:#fafafa}
h1{font-size:20px;margin:0 0 6px}p{color:#5a6377;max-width:640px;margin:0 0 20px}
.panel{display:flex;gap:14px;align-items:flex-end;padding:16px;border-radius:12px;margin-bottom:12px;width:max-content}
.light{background:#ECEEF3}.dark{background:#111318;color:#8a93a6}
.ic{position:relative;overflow:hidden;border-radius:23%}
.ic img{position:absolute;left:-25%;top:-25%;width:150%;height:150%}
.col{display:flex;flex-direction:column;gap:8px;align-items:center;font-size:11px;color:#8a93a6}
</style></head><body>
<h1>Dawnly app icon: Sunny Egg</h1>
<p>A fried egg: a white of soft blue glass and a glossy yolk. Shown as an Android launcher shows it,
at 108px and 48px, round and squircle, on light and dark home screens.</p>
""" + ''.join(f"""<div class="panel {bg}">
<div class="col"><div class="ic" style="width:108px;height:108px"><img src="egg.svg"></div>squircle</div>
<div class="col"><div class="ic" style="width:108px;height:108px;border-radius:50%"><img src="egg.svg"></div>round</div>
<div class="col"><div class="ic" style="width:48px;height:48px"><img src="egg.svg"></div>
<div class="ic" style="width:48px;height:48px;border-radius:50%"><img src="egg.svg"></div>48px</div></div>
""" for bg in ('light', 'dark')) + '</body></html>\n'


if __name__ == '__main__':
    args = sys.argv[1:]
    masters()
    if '--android' in args:
        export_android(args[args.index('--android') + 1])

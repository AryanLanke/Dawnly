"""Palette, SVG primitives and the concept registry."""
import math

M = '#000000'  # monochrome ink

# ---------------------------------------------------------------- palette
NAVY = '#0E1E45'; BLUE = '#1A4FD6'; PALE = '#F7F9FE'; DUSK = '#22386F'
MOON = '#E8EEFA'; CREAM = '#F6EEE3'; BONE = '#F2E8D8'
ORANGE = '#EF7F45'; APRICOT = '#FFB36B'; AMBER = '#F3A03C'; BUTTER = '#FFD27F'
RUST = '#B9502E'; CHAR = '#1E1B1A'

# ---------------------------------------------------------------- primitives
_n = [0]
def uid(p='m'):
    _n[0] += 1
    return f'{p}{_n[0]}'

def f(v): return f'{v:.1f}'.rstrip('0').rstrip('.')

def circ(cx, cy, r, fill): return f'<circle cx="{f(cx)}" cy="{f(cy)}" r="{f(r)}" fill="{fill}"/>'
def ell(cx, cy, rx, ry, fill): return f'<ellipse cx="{f(cx)}" cy="{f(cy)}" rx="{f(rx)}" ry="{f(ry)}" fill="{fill}"/>'
def rect(x, y, w, h, fill, rx=0, extra=''):
    return f'<rect x="{f(x)}" y="{f(y)}" width="{f(w)}" height="{f(h)}" rx="{f(rx)}" fill="{fill}"{extra}/>'
def path(d, fill, extra=''): return f'<path d="{d}" fill="{fill}"{extra}/>'
def line(d, color, w, cap='round'):
    return (f'<path d="{d}" fill="none" stroke="{color}" stroke-width="{f(w)}" '
            f'stroke-linecap="{cap}" stroke-linejoin="round"/>')
def g(*parts, extra=''): return f'<g{extra}>{"".join(parts)}</g>'

def cut(content, *holes):
    """content with holes punched out (holes drawn in black)."""
    i = uid()
    return (f'<mask id="{i}" maskUnits="userSpaceOnUse" x="-512" y="-512" width="2048" height="2048">'
            f'<rect x="-512" y="-512" width="2048" height="2048" fill="#fff"/>{"".join(holes)}</mask>'
            f'<g mask="url(#{i})">{content}</g>')

def clip(content, shape):
    i = uid('c')
    return f'<clipPath id="{i}">{shape}</clipPath><g clip-path="url(#{i})">{content}</g>'

def crescent_d(cx, cy, r, ox, oy, r2):
    """Path for circle (cx,cy,r) minus circle (cx+ox,cy+oy,r2), as two arcs."""
    x2, y2 = cx + ox, cy + oy
    d = math.hypot(ox, oy)
    ux, uy = ox / d, oy / d
    a = (r * r - r2 * r2 + d * d) / (2 * d)
    h = math.sqrt(r * r - a * a)
    px, py = cx + a * ux, cy + a * uy
    i1 = (px - h * uy, py + h * ux); i2 = (px + h * uy, py - h * ux)
    big = 1 if a > 0 else 0
    return (f'M{f(i1[0])} {f(i1[1])} A{f(r)} {f(r)} 0 {big} 1 {f(i2[0])} {f(i2[1])} '
            f'A{f(r2)} {f(r2)} 0 0 0 {f(i1[0])} {f(i1[1])}Z')

def crescent(cx, cy, r, ox, oy, r2, fill):
    return path(crescent_d(cx, cy, r, ox, oy, r2), fill)

def smooth(pts, t=1 / 6):
    """Closed Catmull-Rom spline through pts."""
    n = len(pts)
    d = f'M{f(pts[0][0])} {f(pts[0][1])}'
    for i in range(n):
        p0, p1, p2, p3 = pts[i - 1], pts[i], pts[(i + 1) % n], pts[(i + 2) % n]
        c1 = (p1[0] + (p2[0] - p0[0]) * t, p1[1] + (p2[1] - p0[1]) * t)
        c2 = (p2[0] - (p3[0] - p1[0]) * t, p2[1] - (p3[1] - p1[1]) * t)
        d += f' C{f(c1[0])} {f(c1[1])} {f(c2[0])} {f(c2[1])} {f(p2[0])} {f(p2[1])}'
    return d + 'Z'

def polar(cx, cy, r, deg):
    a = math.radians(deg)
    return cx + r * math.cos(a), cy + r * math.sin(a)

def dome(cx, cy, r):  # upper half-disc sitting on y=cy
    return f'M{f(cx - r)} {f(cy)} A{f(r)} {f(r)} 0 0 1 {f(cx + r)} {f(cy)}Z'

def bowl(cx, cy, r):  # lower half-disc hanging from y=cy
    return f'M{f(cx - r)} {f(cy)} A{f(r)} {f(r)} 0 0 0 {f(cx + r)} {f(cy)}Z'

# ---------------------------------------------------------------- registry
CONCEPTS = []
def concept(slug, name, direction, md):
    def deco(fn):
        CONCEPTS.append(dict(slug=slug, name=name, direction=direction, md=md.strip(), fn=fn))
        return fn
    return deco



"""Liquid-glass versions, in the spirit of Apple's iOS 26 icons: thick soft-cornered glass, a bright
specular edge where the light hits (top-left) and a fainter one opposite, frosting that lightens toward
the top, a soft lift shadow, and coloured light passing through the glass onto the background.

Each piece is given as an inset path P; the rounded piece is P grown by R (fill + round-joined stroke),
so every band (rim, glow) is the same path with a different stroke width.
"""
from kit import f

R = 14   # corner radius of every glass piece, in local units


def inverse(m):
    a, b, c, d, e, f_ = m
    return (1 / a, 0, -c / (a * d), 1 / d, -e / a + c * f_ / (a * d), -f_ / d)


def mstr(m): return 'matrix(' + ' '.join(f(v) if abs(v) > 1e-9 else '0' for v in m) + ')'


class Liquid:
    def __init__(self, background, theme):
        """background: svg drawn into the background layer (also what the glass frosts).
        theme: tint_top, tint_bottom (colour, alpha), spec (hi, mid, lo), shadow (colour, alpha),
        glow alpha, caustic (colour, alpha, (dx, dy)) or None."""
        self.t = theme
        self.bg = background
        self.defs = [
            '<filter id="frost" x="-300" y="-300" width="1624" height="1624" filterUnits="userSpaceOnUse">'
            f'<feGaussianBlur stdDeviation="22"/><feColorMatrix type="saturate" values="{theme["sat"]}"/></filter>',
            '<filter id="b8" x="-300" y="-300" width="1624" height="1624" filterUnits="userSpaceOnUse">'
            '<feGaussianBlur stdDeviation="8"/></filter>',
            '<filter id="b24" x="-300" y="-300" width="1624" height="1624" filterUnits="userSpaceOnUse">'
            '<feGaussianBlur stdDeviation="24"/></filter>',
            '<filter id="b34" x="-300" y="-300" width="1624" height="1624" filterUnits="userSpaceOnUse">'
            '<feGaussianBlur stdDeviation="24"/></filter>',
            '<filter id="b40" x="-300" y="-300" width="1624" height="1624" filterUnits="userSpaceOnUse">'
            '<feGaussianBlur stdDeviation="40"/></filter>',
        ]
        self.under, self.over, self.mono = [], [], []
        self.n = 0

    def id(self, p):
        self.n += 1
        return f'{p}{self.n}'

    @staticmethod
    def grown(p, ext, color, extra=''):
        ext = max(ext, 0.01)
        return (f'<path d="{p}" fill="{color}" stroke="{color}" stroke-width="{f(2 * ext)}" '
                f'stroke-linejoin="round"{extra}/>')

    def mask(self, *parts):
        i = self.id('m')
        self.defs.append(f'<mask id="{i}" maskUnits="userSpaceOnUse" x="-300" y="-300" width="1624" height="1624">'
                         f'{"".join(parts)}</mask>')
        return i

    def lin(self, x1, y1, x2, y2, stops):
        i = self.id('g')
        st = ''.join(f'<stop offset="{o}" stop-color="{c}" stop-opacity="{a}"/>' for o, c, a in stops)
        self.defs.append(f'<linearGradient id="{i}" gradientUnits="userSpaceOnUse" x1="{f(x1)}" y1="{f(y1)}" '
                         f'x2="{f(x2)}" y2="{f(y2)}">{st}</linearGradient>')
        return i

    def piece(self, p, bbox, m):
        """p: inset path in local coords, bbox: (x0, y0, x1, y1) of the rounded piece, m: local->canvas matrix."""
        t = self.t
        x0, y0, x1, y1 = bbox
        T, Ti = mstr(m), mstr(inverse(m))
        full = self.mask(self.grown(p, R, '#fff'))
        rim = self.mask(self.grown(p, R, '#fff'), self.grown(p, R - 5, '#000'))
        glow = self.mask(f'<g filter="url(#b8)">{self.grown(p, R, "#fff")}{self.grown(p, R - 16, "#000")}</g>')
        tint = self.lin(0, y0, 0, y1, [(0, t['tint_top'][0], t['tint_top'][1]),
                                      (1, t['tint_bottom'][0], t['tint_bottom'][1])])
        sh, sm, sl = t['spec']
        spec = self.lin(x0, y0, x1, y1, [(0, '#fff', sh), (0.45, '#fff', sm), (1, '#fff', sl)])
        box = f'<rect x="{x0 - 40}" y="{y0 - 40}" width="{x1 - x0 + 80}" height="{y1 - y0 + 80}"'
        # light thrown through the glass onto the background, then the lift shadow
        if t['caustic']:
            cc, ca, (dx, dy) = t['caustic']
            self.under.append(f'<g transform="translate({dx} {dy})" opacity="{ca}" filter="url(#b34)">'
                              f'<g transform="{T}">{self.grown(p, R, cc)}</g></g>')
        sc, sa = t['shadow']
        self.under.append(f'<g transform="translate(0 18)" opacity="{sa}" filter="url(#b40)">'
                          f'<g transform="{T}">{self.grown(p, R, sc)}</g></g>')
        self.mono.append(f'<g transform="{T}">{self.grown(p, R, "#000")}</g>')
        self.over.append(
            f'<g transform="{T}">'
            f'<g mask="url(#{full})"><use href="#bg" transform="{Ti}" filter="url(#frost)"/>'
            f'{box} fill="url(#{tint})"/></g>'
            f'<g mask="url(#{glow})">{box} fill="#fff" opacity="{t["glow"]}"/></g>'
            f'<g mask="url(#{rim})">{box} fill="url(#{spec})"/></g></g>')

    def svg(self):
        return ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="1024" height="1024">'
                f'<defs>{"".join(self.defs)}</defs>'
                f'<g id="background"><g id="bg">{self.bg}</g>{"".join(self.under)}</g>'
                f'<g id="foreground">{"".join(self.over)}</g></svg>\n')

    def layers(self):
        """The three adaptive-icon layers as separate SVGs: background (sky, glow, thrown light, shadow),
        foreground (the glass, frosting what's behind it), and the flat monochrome silhouette."""
        head = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="1024" height="1024">'
        defs = "".join(self.defs)
        back = f'{head}<defs>{defs}</defs><g id="bg">{self.bg}</g>{"".join(self.under)}</svg>\n'
        fore = f'{head}<defs>{defs}<g id="bg">{self.bg}</g></defs>{"".join(self.over)}</svg>\n'
        mono = f'{head}{"".join(self.mono)}</svg>\n'
        return back, fore, mono

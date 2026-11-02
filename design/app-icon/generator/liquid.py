"""Liquid-glass renderer: draws glass pieces over a background as SVG.

The look follows Apple's iOS 26 icons: thick soft-cornered glass, a bright specular edge where the
light hits (top-left) and a fainter one opposite, frosting that lightens toward the top, a soft lift
shadow, and the light that passes through the glass landing on the background below it.

Each piece is given as an inset path P; the rounded piece is P grown by R (fill plus a round-joined
stroke), so every band (rim, inner glow) is the same path with a different stroke width.
Coordinates are on a 1024 canvas, which is the 108dp adaptive icon canvas.
"""

R = 14  # corner radius of every glass piece, in local units

_FILTER_BOX = 'x="-300" y="-300" width="1624" height="1624" filterUnits="userSpaceOnUse"'
_MASK_BOX = 'maskUnits="userSpaceOnUse" x="-300" y="-300" width="1624" height="1624"'
_SVG_HEAD = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="1024" height="1024">'


def num(v):
    """Short number for SVG output: 12.5, 12, never 12.0."""
    return f'{v:.1f}'.rstrip('0').rstrip('.')


def matrix(m):
    return 'matrix(' + ' '.join(num(v) if abs(v) > 1e-9 else '0' for v in m) + ')'


def inverse(m):
    """Inverse of an SVG matrix (a, b, c, d, e, f) with b == 0."""
    a, _, c, d, e, f = m
    return (1 / a, 0, -c / (a * d), 1 / d, -e / a + c * f / (a * d), -f / d)


class Liquid:
    def __init__(self, background, look):
        """background: SVG for the background layer, which is also what the glass frosts.
        look: tint_top / tint_bottom (colour, alpha), spec (top-left, middle, bottom-right alpha),
        shadow (colour, alpha), glow (inner glow alpha), light (colour, alpha, (dx, dy)) thrown onto
        the background, sat (saturation boost of the frosted background)."""
        self.look = look
        self.bg = background
        self.defs = [
            f'<filter id="frost" {_FILTER_BOX}><feGaussianBlur stdDeviation="22"/>'
            f'<feColorMatrix type="saturate" values="{look["sat"]}"/></filter>',
            f'<filter id="soft" {_FILTER_BOX}><feGaussianBlur stdDeviation="8"/></filter>',
            f'<filter id="light" {_FILTER_BOX}><feGaussianBlur stdDeviation="24"/></filter>',
            f'<filter id="shadow" {_FILTER_BOX}><feGaussianBlur stdDeviation="40"/></filter>',
        ]
        self.under, self.over, self.mono = [], [], []
        self.n = 0

    def _id(self, prefix):
        self.n += 1
        return f'{prefix}{self.n}'

    @staticmethod
    def _grown(p, ext, color):
        ext = max(ext, 0.01)
        return (f'<path d="{p}" fill="{color}" stroke="{color}" stroke-width="{num(2 * ext)}" '
                f'stroke-linejoin="round"/>')

    def _mask(self, *parts):
        i = self._id('m')
        self.defs.append(f'<mask id="{i}" {_MASK_BOX}>{"".join(parts)}</mask>')
        return i

    def _linear(self, x1, y1, x2, y2, stops):
        i = self._id('g')
        st = ''.join(f'<stop offset="{o}" stop-color="{c}" stop-opacity="{a}"/>' for o, c, a in stops)
        self.defs.append(f'<linearGradient id="{i}" gradientUnits="userSpaceOnUse" x1="{num(x1)}" '
                         f'y1="{num(y1)}" x2="{num(x2)}" y2="{num(y2)}">{st}</linearGradient>')
        return i

    def piece(self, p, bbox, m):
        """p: inset path in local coordinates; bbox: (x0, y0, x1, y1) of the rounded piece;
        m: local-to-canvas matrix (a, b, c, d, e, f)."""
        look = self.look
        x0, y0, x1, y1 = bbox
        to_canvas, to_local = matrix(m), matrix(inverse(m))
        full = self._mask(self._grown(p, R, '#fff'))
        rim = self._mask(self._grown(p, R, '#fff'), self._grown(p, R - 5, '#000'))
        glow = self._mask(f'<g filter="url(#soft)">{self._grown(p, R, "#fff")}{self._grown(p, R - 16, "#000")}</g>')
        tint = self._linear(0, y0, 0, y1, [(0, *look['tint_top']), (1, *look['tint_bottom'])])
        hi, mid, lo = look['spec']
        spec = self._linear(x0, y0, x1, y1, [(0, '#fff', hi), (0.45, '#fff', mid), (1, '#fff', lo)])
        box = f'<rect x="{x0 - 40}" y="{y0 - 40}" width="{x1 - x0 + 80}" height="{y1 - y0 + 80}"'

        # Under the glass: the light it throws onto the background, then its lift shadow.
        lc, la, (dx, dy) = look['light']
        self.under.append(f'<g transform="translate({dx} {dy})" opacity="{la}" filter="url(#light)">'
                          f'<g transform="{to_canvas}">{self._grown(p, R, lc)}</g></g>')
        sc, sa = look['shadow']
        self.under.append(f'<g transform="translate(0 18)" opacity="{sa}" filter="url(#shadow)">'
                          f'<g transform="{to_canvas}">{self._grown(p, R, sc)}</g></g>')
        # The glass: frosted background, tint, inner glow, specular rim.
        self.over.append(
            f'<g transform="{to_canvas}">'
            f'<g mask="url(#{full})"><use href="#bg" transform="{to_local}" filter="url(#frost)"/>'
            f'{box} fill="url(#{tint})"/></g>'
            f'<g mask="url(#{glow})">{box} fill="#fff" opacity="{look["glow"]}"/></g>'
            f'<g mask="url(#{rim})">{box} fill="url(#{spec})"/></g></g>')
        self.mono.append(f'<g transform="{to_canvas}">{self._grown(p, R, "#000")}</g>')

    def svg(self):
        """The whole icon, with background and foreground groups (the adaptive icon's two layers)."""
        return (f'{_SVG_HEAD}<defs>{"".join(self.defs)}</defs>'
                f'<g id="background"><g id="bg">{self.bg}</g>{"".join(self.under)}</g>'
                f'<g id="foreground">{"".join(self.over)}</g></svg>\n')

    def layers(self):
        """The adaptive icon layers as separate SVGs: background (sky, glow, thrown light, shadow),
        foreground (the glass, frosting what's behind it), and the flat monochrome silhouette."""
        defs = ''.join(self.defs)
        back = f'{_SVG_HEAD}<defs>{defs}</defs><g id="bg">{self.bg}</g>{"".join(self.under)}</svg>\n'
        fore = f'{_SVG_HEAD}<defs>{defs}<g id="bg">{self.bg}</g></defs>{"".join(self.over)}</svg>\n'
        mono = f'{_SVG_HEAD}{"".join(self.mono)}</svg>\n'
        return back, fore, mono

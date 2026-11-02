"""The Dawnly app icon, "Sunny Egg": a fried egg with a white of soft blue glass and a glossy yolk,
like a sun coming up. Drawn on the 1024px adaptive-icon canvas (108dp), the whole egg inside the
66dp safe circle so no launcher mask clips it.
"""

_SVG_HEAD = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="1024" height="1024">'

# The wobbly outline of the egg white, clockwise from the top. Smoothed into curves below.
_WHITE = [
    (497, 260), (587, 274), (637, 296), (692, 292), (739, 334), (745, 394), (752, 464), (769, 529),
    (742, 604), (719, 654), (722, 719), (667, 758), (602, 749), (547, 746), (492, 770), (417, 746),
    (352, 710), (292, 710), (255, 644), (272, 579), (259, 484), (302, 419), (345, 369), (372, 306),
    (427, 274),
]
YOLK = (494, 514, 104)                                        # centre x, centre y, radius


def _smooth(points):
    """A closed path through every point (Catmull-Rom turned into cubic Béziers)."""
    n = len(points)
    d = f'M{points[0][0]} {points[0][1]}'
    for i in range(n):
        p0, p1, p2, p3 = points[i - 1], points[i], points[(i + 1) % n], points[(i + 2) % n]
        c1 = (p1[0] + (p2[0] - p0[0]) / 6, p1[1] + (p2[1] - p0[1]) / 6)
        c2 = (p2[0] - (p3[0] - p1[0]) / 6, p2[1] - (p3[1] - p1[1]) / 6)
        d += f' C{c1[0]:.1f} {c1[1]:.1f} {c2[0]:.1f} {c2[1]:.1f} {p2[0]} {p2[1]}'
    return d + 'Z'


WHITE_PATH = _smooth(_WHITE)

_DEFS = (
    '<linearGradient id="white" x1=".2" y1=".1" x2=".85" y2=".95">'
    '<stop offset="0" stop-color="#A3C4FF"/><stop offset=".5" stop-color="#88AEFB"/>'
    '<stop offset="1" stop-color="#6990EC"/></linearGradient>'
    '<radialGradient id="yolk" cx=".38" cy=".34" r=".75">'
    '<stop offset="0" stop-color="#FFE68A"/><stop offset=".55" stop-color="#FFC21F"/>'
    '<stop offset="1" stop-color="#F09E00"/></radialGradient>'
    '<radialGradient id="sheen" cx=".3" cy=".22" r=".6">'
    '<stop offset="0" stop-color="#FFFFFF" stop-opacity=".35"/>'
    '<stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/></radialGradient>'
    '<radialGradient id="room" cx=".5" cy=".5" r=".6">'
    '<stop offset="0" stop-color="#EEF4FF"/><stop offset="1" stop-color="#FFFFFF"/></radialGradient>'
    f'<clipPath id="inside"><path d="{WHITE_PATH}"/></clipPath>'
    '<filter id="b8" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="8"/></filter>'
    '<filter id="b14" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="14"/></filter>'
    '<filter id="b34" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="34"/></filter>'
)


def _background():
    return '<rect width="1024" height="1024" fill="url(#room)"/>'


def _egg():
    x, y, r = YOLK
    return (
        # A soft blue glow around the white, as if light passes through it.
        f'<path d="{WHITE_PATH}" fill="#7DA6FF" opacity=".45" filter="url(#b34)" transform="translate(0 10)"/>'
        # The glass white: blue gradient, frosted lighter edge inside, a sheen, a thin bright rim.
        f'<path d="{WHITE_PATH}" fill="url(#white)"/>'
        f'<g clip-path="url(#inside)">'
        f'<path d="{WHITE_PATH}" fill="none" stroke="#FFFFFF" stroke-width="44" opacity=".38" filter="url(#b14)"/>'
        f'<rect width="1024" height="1024" fill="url(#sheen)"/>'
        f'<path d="{WHITE_PATH}" fill="none" stroke="#3E63C8" stroke-width="70" opacity=".12" filter="url(#b34)"'
        ' transform="translate(-12 -14)"/></g>'
        f'<path d="{WHITE_PATH}" fill="none" stroke="#FFFFFF" stroke-width="4" opacity=".6"/>'
        # The yolk: a shadow on the white, a pale rim, a glossy dome, a big and a small highlight.
        f'<circle cx="{x + 4}" cy="{y + 14}" r="{r}" fill="#2F4FB0" opacity=".35" filter="url(#b14)"/>'
        f'<circle cx="{x}" cy="{y}" r="{r}" fill="#FFD660"/>'
        f'<circle cx="{x}" cy="{y}" r="{r - 9}" fill="url(#yolk)"/>'
        f'<ellipse cx="{x - 33}" cy="{y - 38}" rx="34" ry="20" transform="rotate(-22 {x - 33} {y - 38})"'
        ' fill="#FFF8E2" opacity=".9" filter="url(#b8)"/>'
        f'<ellipse cx="{x - 33}" cy="{y - 38}" rx="30" ry="17" transform="rotate(-22 {x - 33} {y - 38})"'
        ' fill="#FFF8E2" opacity=".85"/>'
        f'<circle cx="{x + 45}" cy="{y + 50}" r="10" fill="#FFE59A" opacity=".7"/>'
    )


def _mono():
    """Themed-icon silhouette: the white with a ring cut around the yolk."""
    x, y, r = YOLK
    return (f'<mask id="m"><rect width="1024" height="1024" fill="#000"/><path d="{WHITE_PATH}" fill="#fff"/>'
            f'<circle cx="{x}" cy="{y}" r="{r + 14}" fill="#000"/><circle cx="{x}" cy="{y}" r="{r - 6}" fill="#fff"/>'
            '</mask><rect width="1024" height="1024" fill="#000" mask="url(#m)"/>')


class Egg:
    def svg(self):
        """The whole icon in one SVG."""
        return f'{_SVG_HEAD}<defs>{_DEFS}</defs>{_background()}{_egg()}</svg>\n'

    def layers(self):
        """The adaptive icon layers: background (a white, faintly blue room), foreground (the egg)
        and the flat monochrome silhouette."""
        back = f'{_SVG_HEAD}<defs>{_DEFS}</defs>{_background()}</svg>\n'
        fore = f'{_SVG_HEAD}<defs>{_DEFS}</defs>{_egg()}</svg>\n'
        mono = f'{_SVG_HEAD}{_mono()}</svg>\n'
        return back, fore, mono


STATES = {'egg': Egg}

"""The Dawnly icon. Coordinates are on the 1024 canvas (= the 108dp adaptive icon canvas)."""
from kit import *  # noqa: F401,F403
from liquid import Liquid


@concept('floor-light', 'Floor Light', 'Window', """
The slanted patch of light a window throws on the floor, drawn as liquid glass. Day and night are
the same scene at two light levels; only the colour values change.
""")
def _():
    # rounded panes of an arched window with a cross mullion: inset paths (grown by liquid.R) and the
    # bounding box of each rounded pane
    panes = [('M406 542 V452 A106 106 0 0 1 486 349.2 V542Z', (392, 332, 500, 556)),
             ('M538 542 V349.2 A106 106 0 0 1 618 452 V542Z', (524, 332, 632, 556)),
             ('M406 594 H486 V698 H406Z', (392, 580, 500, 712)),
             ('M538 594 H618 V698 H538Z', (524, 580, 632, 712))]
    # leaned like light on a floor, scaled 1.13 about the centre, nudged up to sit optically centred
    s, skew, up = 1.13, 0.42, 10
    lean = (s, 0, s * skew, s, 512 - s * 512 - s * skew * 512, 512 - s * 512 - up)

    def scene(sky_top, sky_bottom, glow, glow_a, glass):
        bg = (f'<defs><linearGradient id="sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{sky_top}"/>'
              f'<stop offset="1" stop-color="{sky_bottom}"/></linearGradient>'
              f'<radialGradient id="glow"><stop offset="0" stop-color="{glow}" stop-opacity="{glow_a}"/>'
              f'<stop offset=".7" stop-color="{glow}" stop-opacity="0"/></radialGradient></defs>'
              '<rect width="1024" height="1024" fill="url(#sky)"/><circle cx="520" cy="500" r="600" fill="url(#glow)"/>')
        icon = Liquid(bg, glass)
        for p, bb in panes:
            icon.piece(p, bb, lean)
        return icon

    day = scene('#4C86FF', '#1A4FD6', '#BFD8FF', .6,
                dict(tint_top=('#FFFFFF', .64), tint_bottom=('#FFFFFF', .24), spec=(1, .38, .7),
                     shadow=('#0A1E5A', .45), glow=.32, caustic=('#E4F0FF', .6, (40, 100)), sat=1.2))
    # the same scene after sunset: sky deepens to navy, the glow behind dims to the app's dark-mode
    # blue, and the glass and the light it throws get quieter
    night = scene('#1A3A8C', '#0A1A4A', '#5E95FF', .45,
                  dict(tint_top=('#DCEBFF', .46), tint_bottom=('#DCEBFF', .15), spec=(1, .32, .6),
                       shadow=('#030A24', .55), glow=.24, caustic=('#A9CCFF', .5, (40, 100)), sat=1.15))
    return dict(lday=day.svg(), lnight=night.svg(), android_day=day.layers(), android_night=night.layers())

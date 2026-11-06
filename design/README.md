# Design

| Path | What it is |
|------|------------|
| `mockups.html` | The original UI mockups: clear glass over a morning sky (light) or a night sky (dark). The app has moved on since (reminders, learning and more options are not in them). Open in a browser; it has a light/dark toggle. |
| `app-icon/` | The app icon, "Sunny Egg": a fried egg with a white of blue glass. `egg.svg`, a 512px PNG, and `preview.html`, which shows it the way a launcher does. |
| `app-icon/generator/` | Python that draws the icon (`icon.py`) and renders it with headless Chrome (`build.py`). No packages needed. |
| `sounds/` | The notification sound masters and the script that makes the app's sounds from them (below). |

To change the icon, edit `app-icon/generator/icon.py`, then run from that folder:

```
python build.py                # egg.svg, its PNG and preview.html
python build.py --android egg  # also export the launcher icon layers into app/src/main/res
```

## Sounds

`sounds/source/` holds the notification sound masters (Mixkit, see `app/src/main/assets/licenses/mixkit_sounds.txt`). The app doesn't ship them as they are: `sounds/make_sounds.sh` turns each one into `sound_<name>.ogg` (the prompt) and `sound_<name>_reversed.ogg` ("Alarm dismissed", played backwards; with Right away the forwards sound is used), both with a soft echo that fades out over about a second. To change them, edit the script and run it from the repo root with ffmpeg's `bin` folder:

```
sh design/sounds/make_sounds.sh "$LOCALAPPDATA/Microsoft/WinGet/Packages/Gyan.FFmpeg_<...>/ffmpeg-<version>-full_build/bin"
```

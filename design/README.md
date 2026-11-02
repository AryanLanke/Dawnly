# Design

| Path | What it is |
|------|------------|
| `mockups.html` | The approved UI mockups (PLAN.md, Appendix A). Open in a browser; it has a light/dark toggle. |
| `app-icon/` | The app icon, "Floor Light": `day.svg` and `night.svg` masters, 512px PNGs, and `preview.html`, which shows them the way a launcher does. The app ships the night version. |
| `app-icon/generator/` | Python that draws the icon (`icon.py`, `liquid.py`) and renders it with headless Chrome (`build.py`). No packages needed. |
| `sounds/` | The notification sound masters and the script that makes the app's sounds from them (below). |

To change the icon, edit `app-icon/generator/icon.py`, then run from that folder:

```
python build.py                  # day/night masters, PNGs and preview.html
python build.py --android night  # also export the launcher icon layers into app/src/main/res
```

## Sounds

`sounds/source/` holds the notification sound masters (Mixkit, see `app/src/main/assets/licenses/mixkit_sounds.txt`). The app doesn't ship them as they are: `sounds/make_sounds.sh` turns each one into `sound_<name>.ogg` (the prompt) and `sound_<name>_reversed.ogg` ("Alarm dismissed", played backwards), both with a soft echo that fades out over about a second. To change them, edit the script and run it from the repo root with ffmpeg's `bin` folder:

```
sh design/sounds/make_sounds.sh "$LOCALAPPDATA/Microsoft/WinGet/Packages/Gyan.FFmpeg_<...>/ffmpeg-<version>-full_build/bin"
```

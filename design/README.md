# Design

| Path | What it is |
|------|------------|
| `mockups.html` | The approved UI mockups (PLAN.md, Appendix A). Open in a browser; it has a light/dark toggle. |
| `app-icon/` | The app icon, "Floor Light": `day.svg` and `night.svg` masters, 512px PNGs, and `preview.html`, which shows them the way a launcher does. The app ships the night version. |
| `app-icon/generator/` | Python that draws the icon (`icon.py`, `liquid.py`) and renders it with headless Chrome (`build.py`). No packages needed. |

To change the icon, edit `app-icon/generator/icon.py`, then run from that folder:

```
python build.py                  # day/night masters, PNGs and preview.html
python build.py --android night  # also export the launcher icon layers into app/src/main/res
```

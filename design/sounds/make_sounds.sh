#!/bin/sh
# Makes Dawnly's notification sounds from the masters in source/ (Mixkit, see
# app/src/main/assets/licenses/mixkit_sounds.txt). For each master it writes two files into
# app/src/main/res/raw:
#   sound_<name>.ogg           the prompt: the sound as is
#   sound_<name>_reversed.ogg  "Alarm dismissed": the sound played backwards
# Both get a soft room-like tail that fades out over about a second instead of stopping dead
# (the user's choice, 2026-11-03), and keep the master's peak level.
#
# Usage, from the repo root: sh design/sounds/make_sounds.sh <ffmpeg bin dir>
set -e
FF="$1"
SRC="$(dirname "$0")/source"
RAW="app/src/main/res/raw"
TAIL=1.4
mkdir -p "$RAW"

peak() {
  "$FF/ffmpeg" -hide_banner -i "$1" -af volumedetect -f null - 2>&1 | sed -n 's/.*max_volume: \(-*[0-9.]*\) dB/\1/p'
}

# make <master> <output> <"" or "areverse,">
make() {
  target=$(peak "$1")
  tmp="$2.tmp.wav"
  "$FF/ffmpeg" -hide_banner -loglevel error -y -i "$1" \
    -f lavfi -i "anoisesrc=d=$TAIL:c=pink:a=0.6:r=44100:seed=7,afade=t=out:st=0:d=$TAIL:curve=qua,aformat=channel_layouts=stereo" \
    -filter_complex "[0]aformat=sample_rates=44100:channel_layouts=stereo,$3silenceremove=start_periods=1:start_threshold=-50dB,areverse,silenceremove=start_periods=1:start_threshold=-50dB,areverse,afade=t=in:d=0.01,apad=pad_dur=$TAIL,asplit[dry][toverb];[toverb][1]afir=irnorm=2[wet];[dry][wet]amix=inputs=2:weights='1 0.5':normalize=0,areverse,afade=t=in:d=0.6:curve=qsin,areverse,alimiter=limit=0.98[o]" \
    -map "[o]" "$tmp"
  gain=$(awk -v a="$target" -v b="$(peak "$tmp")" 'BEGIN { print a - b }')
  "$FF/ffmpeg" -hide_banner -loglevel error -y -i "$tmp" -af "volume=${gain}dB" -c:a libvorbis -q:a 5 "$2"
  rm "$tmp"
}

for f in "$SRC"/sound_*; do
  name=$(basename "${f%.*}")
  make "$f" "$RAW/$name.ogg" ""
  make "$f" "$RAW/${name}_reversed.ogg" "areverse,"
  echo "$name"
done

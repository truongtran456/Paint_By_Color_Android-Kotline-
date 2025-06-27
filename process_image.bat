@echo off
python tools/image_processor.py ^
app/src/main/res/drawable-xxxhdpi/paint_3_outline.jpg ^
app/src/main/res/drawable-xxxhdpi/paint_3_preview.png ^
app/src/main/res/raw/image_3.svg ^
--output-line-art app/src/main/res/raw/paint_3_line_art.png 
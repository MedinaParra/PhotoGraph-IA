from pathlib import Path

source = Path("app/src/main/java/cl/medina/atatis35/AtatisGame.java")
text = source.read_text(encoding="utf-8")
text = text.replace(
    "private TextureRegionDrawable drawable(Color c) {",
    "private com.badlogic.gdx.scenes.scene2d.utils.Drawable drawable(Color c) {",
)
marker = "    @Override public boolean touchDragged(int screenX, int screenY, int pointer) {"
if "touchCancelled(" not in text:
    text = text.replace(
        marker,
        "    @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {\n"
        "        cameraDragging = false;\n"
        "        return true;\n"
        "    }\n\n" + marker,
    )
source.write_text(text, encoding="utf-8")

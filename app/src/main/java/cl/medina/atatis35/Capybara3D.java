package cl.medina.atatis35;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

final class Capybara3D {
    static final int IDLE = 0;
    static final int WALK = 1;
    static final int EAT = 2;
    static final int WASH = 3;
    static final int SLEEP = 4;
    static final int JUMP = 5;
    static final int HAPPY = 6;
    static final int SKATE = 7;

    private static final Color[] FUR = {
            Color.valueOf("A97855"), Color.valueOf("E4A7B9"), Color.valueOf("B8A1D8"),
            Color.valueOf("8FC8B5"), Color.valueOf("91B8D8"), Color.valueOf("D8B67C")
    };
    private static final Color[] MAGIC = {
            Color.valueOf("FF70A6"), Color.valueOf("FFD166"), Color.valueOf("67D5FF"),
            Color.valueOf("7EE2A8"), Color.valueOf("B28DFF"), Color.valueOf("FF9A5A")
    };

    private final Part body, head, snout, nose, tail;
    private final Part[] ears = new Part[2];
    private final Part[] eyes = new Part[2];
    private final Part[] cheeks = new Part[2];
    private final Part[] legs = new Part[4];
    private final Part[] hooves = new Part[4];
    private final Part horn;
    private final Part[] mane = new Part[6];
    private final Part[] accessory = new Part[8];
    private final Matrix4 root = new Matrix4();
    private final Matrix4 temp = new Matrix4();

    float x = 0f;
    float z = 0f;
    float targetX = 0f;
    float targetZ = 0f;
    float yOffset = 0f;
    float yaw = 0f;
    float externalScale = 1f;
    int furIndex = 0;
    int styleIndex = 0;
    int animation = IDLE;
    float animationTime = 0f;
    float animationDuration = 0f;
    boolean enabled = true;

    Capybara3D(Models3D models) {
        body = new Part(models.newSphere(Color.WHITE));
        head = new Part(models.newSphere(Color.WHITE));
        snout = new Part(models.newSphere(Color.valueOf("D6AD8B")));
        nose = new Part(models.newSphere(Color.valueOf("4C342E")));
        tail = new Part(models.newSphere(Color.WHITE));
        for (int i = 0; i < 2; i++) {
            ears[i] = new Part(models.newSphere(Color.WHITE));
            eyes[i] = new Part(models.newSphere(Color.valueOf("2D2233")));
            cheeks[i] = new Part(models.newSphere(Color.valueOf("FF8FAD")));
        }
        for (int i = 0; i < 4; i++) {
            legs[i] = new Part(models.newCylinder(Color.WHITE));
            hooves[i] = new Part(models.newSphere(Color.valueOf("66463B")));
        }
        horn = new Part(models.newCone(Color.valueOf("FFF7B2")));
        for (int i = 0; i < mane.length; i++) mane[i] = new Part(models.newSphere(MAGIC[i % MAGIC.length]));
        for (int i = 0; i < accessory.length; i++) accessory[i] = new Part(models.newCube(MAGIC[i % MAGIC.length]));
        recolor();
    }

    void setLook(int fur, int style) {
        furIndex = MathUtils.clamp(fur, 0, FUR.length - 1);
        styleIndex = MathUtils.clamp(style, 0, 5);
        recolor();
    }

    private void recolor() {
        Color base = FUR[furIndex];
        setColor(body.instance, base);
        setColor(head.instance, base);
        setColor(tail.instance, base);
        for (Part p : ears) setColor(p.instance, base);
        for (Part p : legs) setColor(p.instance, base);
    }

    private static void setColor(ModelInstance instance, Color color) {
        instance.materials.forEach(m -> m.set(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.createDiffuse(new Color(color))));
    }

    void moveTo(float nx, float nz) {
        targetX = MathUtils.clamp(nx, -3.8f, 3.8f);
        targetZ = MathUtils.clamp(nz, -2.8f, 2.8f);
        if (animation != EAT && animation != WASH && animation != SLEEP) animation = WALK;
    }

    void trigger(int state, float duration) {
        animation = state;
        animationTime = 0f;
        animationDuration = Math.max(.1f, duration);
    }

    void update(float dt) {
        animationTime += dt;
        float dx = targetX - x;
        float dz = targetZ - z;
        float dist = (float)Math.sqrt(dx * dx + dz * dz);
        if (animation != SLEEP && animation != EAT && animation != WASH && animation != HAPPY && dist > .04f) {
            float speed = animation == SKATE ? 4.5f : 2.1f;
            float step = Math.min(dist, speed * dt);
            x += dx / dist * step;
            z += dz / dist * step;
            yaw = MathUtils.atan2(dx, dz) * MathUtils.radiansToDegrees;
            if (animation != SKATE) animation = WALK;
        } else if (dist <= .04f && animation == WALK) {
            animation = IDLE;
            animationTime = 0f;
        }
        if (animationDuration > 0f && animationTime >= animationDuration && animation != SLEEP) {
            animation = dist > .04f ? WALK : IDLE;
            animationTime = 0f;
            animationDuration = 0f;
        }
    }

    void render(ModelBatch batch, Environment environment, float time) {
        if (!enabled) return;
        float bob = animation == SLEEP ? MathUtils.sin(time * 1.4f) * .018f : MathUtils.sin(time * 2.2f) * .035f;
        float jump = 0f;
        if (animation == JUMP) jump = MathUtils.sin(MathUtils.clamp(animationTime / animationDuration, 0f, 1f) * MathUtils.PI) * 1.15f;
        if (animation == HAPPY) jump = Math.abs(MathUtils.sin(animationTime * 7f)) * .35f;
        if (animation == SKATE) jump += yOffset;
        float rootYaw = yaw + (animation == HAPPY ? animationTime * 180f : 0f);
        root.idt().translate(x, jump + yOffset + bob, z).rotate(Vector3.Y, rootYaw).scale(externalScale, externalScale, externalScale);

        float walk = animation == WALK || animation == SKATE ? MathUtils.sin(animationTime * (animation == SKATE ? 10f : 7f)) : 0f;
        float eat = animation == EAT ? MathUtils.sin(animationTime * 12f) : 0f;
        float wash = animation == WASH ? MathUtils.sin(animationTime * 9f) : 0f;
        float sleepSquash = animation == SLEEP ? .9f + MathUtils.sin(animationTime * 1.5f) * .02f : 1f;

        place(body, 0f, .92f, 0f, 1.45f, .78f * sleepSquash, .78f, wash * 4f, 0f, 0f);
        place(head, 0f, 1.42f + eat * .05f, .66f, .72f, .64f, .64f, eat * 5f, 0f, 0f);
        place(snout, 0f, 1.24f + eat * .08f, 1.17f, .52f, .29f + Math.abs(eat) * .04f, .30f, eat * 8f, 0f, 0f);
        place(nose, 0f, 1.30f + eat * .08f, 1.43f, .15f, .10f, .10f, 0f, 0f, 0f);
        place(tail, 0f, 1.02f, -.78f, .28f, .25f, .25f, 0f, 0f, 0f);

        float earDrop = animation == SLEEP ? -.10f : 0f;
        place(ears[0], -.43f, 1.83f + earDrop, .49f, .24f, .20f, .18f, 0f, 0f, -15f);
        place(ears[1], .43f, 1.83f + earDrop, .49f, .24f, .20f, .18f, 0f, 0f, 15f);

        float eyeY = animation == SLEEP ? .025f : .105f;
        place(eyes[0], -.23f, 1.52f, 1.19f, .09f, eyeY, .055f, 0f, 0f, 0f);
        place(eyes[1], .23f, 1.52f, 1.19f, .09f, eyeY, .055f, 0f, 0f, 0f);
        place(cheeks[0], -.40f, 1.31f, 1.20f, .12f, .07f, .045f, 0f, 0f, 0f);
        place(cheeks[1], .40f, 1.31f, 1.20f, .12f, .07f, .045f, 0f, 0f, 0f);

        float[] lx = {-0.72f, -0.28f, .28f, .72f};
        float[] lz = {-.28f, .25f, -.28f, .25f};
        for (int i = 0; i < 4; i++) {
            float phase = (i % 2 == 0 ? walk : -walk);
            place(legs[i], lx[i], .39f, lz[i], .22f, .60f, .22f, phase * 22f, 0f, 0f);
            place(hooves[i], lx[i], .08f, lz[i] + phase * .04f, .24f, .13f, .28f, 0f, 0f, 0f);
        }

        place(horn, 0f, 2.28f, .50f, .25f, .70f, .25f, 0f, 0f, 0f);
        for (int i = 0; i < mane.length; i++) {
            float mz = .18f - i * .23f;
            float my = 1.98f - i * .075f;
            place(mane[i], 0f, my, mz, .18f, .18f, .18f, 0f, 0f, 0f);
        }
        renderAccessory();

        render(batch, environment, body, head, snout, nose, tail, horn);
        render(batch, environment, ears);
        render(batch, environment, eyes);
        render(batch, environment, cheeks);
        render(batch, environment, legs);
        render(batch, environment, hooves);
        render(batch, environment, mane);
        for (Part p : accessory) if (p.visible) batch.render(p.instance, environment);
    }

    private void renderAccessory() {
        for (Part p : accessory) p.visible = false;
        if (styleIndex == 0) return;
        if (styleIndex == 1) {
            for (int i = 0; i < 3; i++) {
                accessory[i].visible = true;
                place(accessory[i], (i - 1) * .20f, 2.06f, .67f, .16f, .28f + (i == 1 ? .12f : 0f), .11f, 0f, 0f, 0f);
            }
        } else if (styleIndex == 2) {
            accessory[0].visible = true; accessory[1].visible = true;
            place(accessory[0], 0f, 2.03f, .56f, .90f, .10f, .75f, 0f, 0f, 0f);
            place(accessory[1], 0f, 2.18f, .48f, .58f, .28f, .52f, 0f, 0f, 0f);
        } else if (styleIndex == 3) {
            accessory[0].visible = true; accessory[1].visible = true;
            place(accessory[0], -.08f, 2.12f, .52f, .58f, .28f, .50f, 0f, 0f, -15f);
            place(accessory[1], -.37f, 2.32f, .50f, .13f, .13f, .13f, 0f, 0f, 0f);
        } else if (styleIndex == 4) {
            for (int i = 0; i < 3; i++) accessory[i].visible = true;
            place(accessory[0], -.24f, 1.52f, 1.28f, .26f, .18f, .06f, 0f, 0f, 0f);
            place(accessory[1], .24f, 1.52f, 1.28f, .26f, .18f, .06f, 0f, 0f, 0f);
            place(accessory[2], 0f, 1.52f, 1.27f, .16f, .05f, .05f, 0f, 0f, 0f);
        } else {
            accessory[0].visible = true; accessory[1].visible = true;
            place(accessory[0], 0f, 1.98f, .52f, .72f, .35f, .62f, 0f, 0f, 0f);
            place(accessory[1], 0f, 1.79f, 1.03f, .56f, .08f, .10f, 0f, 0f, 0f);
        }
    }

    private void place(Part p, float px, float py, float pz,
                       float sx, float sy, float sz, float rx, float ry, float rz) {
        temp.set(root).translate(px, py, pz);
        if (rx != 0f) temp.rotate(Vector3.X, rx);
        if (ry != 0f) temp.rotate(Vector3.Y, ry);
        if (rz != 0f) temp.rotate(Vector3.Z, rz);
        temp.scale(sx, sy, sz);
        p.instance.transform.set(temp);
    }

    private static void render(ModelBatch batch, Environment env, Part... parts) {
        for (Part p : parts) batch.render(p.instance, env);
    }

    private static final class Part {
        final ModelInstance instance;
        boolean visible = true;
        Part(ModelInstance instance) { this.instance = instance; }
    }
}

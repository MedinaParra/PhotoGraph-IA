package cl.medina.atatis35;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

import java.util.HashMap;
import java.util.Map;

final class Models3D {
    final Model cube;
    final Model sphere;
    final Model cylinder;
    final Model cone;
    private final Map<Integer, ModelInstance> cubeColors = new HashMap<>();
    private final Map<Integer, ModelInstance> sphereColors = new HashMap<>();
    private final Map<Integer, ModelInstance> cylinderColors = new HashMap<>();
    private final Map<Integer, ModelInstance> coneColors = new HashMap<>();

    Models3D() {
        ModelBuilder b = new ModelBuilder();
        long attrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        Material white = new Material(ColorAttribute.createDiffuse(Color.WHITE));
        cube = b.createBox(1f, 1f, 1f, white, attrs);
        sphere = b.createSphere(1f, 1f, 1f, 20, 16, white, attrs);
        cylinder = b.createCylinder(1f, 1f, 1f, 20, white, attrs);
        cone = b.createCone(1f, 1f, 1f, 20, white, attrs);
    }

    ModelInstance cube(Color c) { return colored(cubeColors, cube, c); }
    ModelInstance sphere(Color c) { return colored(sphereColors, sphere, c); }
    ModelInstance cylinder(Color c) { return colored(cylinderColors, cylinder, c); }
    ModelInstance cone(Color c) { return colored(coneColors, cone, c); }

    ModelInstance newCube(Color c) { return unique(cube, c); }
    ModelInstance newSphere(Color c) { return unique(sphere, c); }
    ModelInstance newCylinder(Color c) { return unique(cylinder, c); }
    ModelInstance newCone(Color c) { return unique(cone, c); }

    private ModelInstance unique(Model model, Color color) {
        ModelInstance instance = new ModelInstance(model);
        for (Material m : instance.materials) {
            m.set(ColorAttribute.createDiffuse(new Color(color)));
        }
        return instance;
    }

    private ModelInstance colored(Map<Integer, ModelInstance> cache, Model model, Color color) {
        int key = Color.rgba8888(color);
        ModelInstance instance = cache.get(key);
        if (instance == null) {
            instance = new ModelInstance(model);
            for (Material m : instance.materials) {
                m.set(ColorAttribute.createDiffuse(new Color(color)));
            }
            cache.put(key, instance);
        }
        return instance;
    }

    void dispose() {
        cube.dispose();
        sphere.dispose();
        cylinder.dispose();
        cone.dispose();
        cubeColors.clear();
        sphereColors.clear();
        cylinderColors.clear();
        coneColors.clear();
    }
}

package fr.com.jellyfish.sleepersrv.assets.entities;

import fr.com.jellyfish.sleepersrv.assets.AbstractAsset;
import fr.com.jellyfish.sleepersrv.assets.camera.Camera;
import fr.com.jellyfish.sleepersrv.constants.GameConst;
import fr.com.jellyfish.sleepersrv.opengl.util.WavefrontMeshLoader;
import fr.com.jellyfish.sleepersrv.opengl.util.WavefrontMeshLoader.Mesh;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NORMAL_ARRAY;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDisableClientState;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnableClientState;
import static org.lwjgl.opengl.GL11.glNormalPointer;
import static org.lwjgl.opengl.GL11.glVertexPointer;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;

/**
 *
 * @author thw
 */
public class Asteroid extends AbstractAsset {
    
    private static final float MAX_RADIUS = 30.0f;
    private double x, y, z;
    private float scale;
    private final int positionVbo;
    private final int normalsVbo;
    private Mesh mesh;
    private final Camera camera;
    private final FrustumIntersection frustumIntersection;
    private final Matrix4f modelMatrix;
    private final FloatBuffer matrixBuffer;
    private final int default_modelUniform;
    private final int defaultProg;
    
    public Asteroid(final Camera camera, final FrustumIntersection frustumIntersection, 
        final Matrix4f modelMatrix, final FloatBuffer matrixBuffer, final int default_modelUniform,
        final int defaultProg) {
        
        this.camera = camera;
        this.frustumIntersection = frustumIntersection;
        this.modelMatrix = modelMatrix;
        this.matrixBuffer = matrixBuffer;
        this.default_modelUniform = default_modelUniform;
        this.defaultProg = defaultProg;
        
        this.x = (Math.random() - 0.5) * GameConst.SPREADOUT_3000;
        this.y = (Math.random() - 0.5) * GameConst.SPREADOUT_3000;
        this.z = ((Math.random() - 0.5) * GameConst.SPREADOUT_3000) - 5800f;
        this.scale = (float) ((Math.random() * 0.5 + 0.5) * Asteroid.MAX_RADIUS);
        
        final WavefrontMeshLoader loader = new WavefrontMeshLoader();
        
        try {
            this.mesh = loader.loadMesh("fr/com/jellyfish/game/asteroid.obj.zip");
        } catch (final IOException iOEx) {
            Logger.getLogger(Asteroid.class.getName()).log(Level.SEVERE, null, iOEx);
        }
        
        this.positionVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, positionVbo);
        glBufferData(GL_ARRAY_BUFFER, this.mesh.positions, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        this.normalsVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, this.normalsVbo);
        glBufferData(GL_ARRAY_BUFFER, this.mesh.normals, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
    
    @Override
    public void draw() {
    
        glUseProgram(defaultProg);
        glBindBuffer(GL_ARRAY_BUFFER, positionVbo);
        glVertexPointer(3, GL_FLOAT, 0, 0);
        glEnableClientState(GL_NORMAL_ARRAY);
        glBindBuffer(GL_ARRAY_BUFFER, normalsVbo);
        glNormalPointer(GL_FLOAT, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
  
        float tmpx = (float) (x - camera.position.x);
        float tmpy = (float) (y - camera.position.y);
        float tmpz = (float) (z - camera.position.z);
        if (frustumIntersection.testSphere(tmpx, tmpy, tmpz, scale)) {
            modelMatrix.translation(tmpx, tmpy, tmpz);
            modelMatrix.scale(scale);
            glUniformMatrix4fv(default_modelUniform, false, modelMatrix.get(matrixBuffer));
            glDrawArrays(GL_TRIANGLES, 0, mesh.numVertices);
        }
        
        glDisableClientState(GL_NORMAL_ARRAY);
    }
    
}

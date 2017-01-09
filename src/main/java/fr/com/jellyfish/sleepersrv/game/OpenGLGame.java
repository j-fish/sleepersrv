package fr.com.jellyfish.sleepersrv.game;

import fr.com.jellyfish.sleepersrv.assets.AbstractAsset;
import fr.com.jellyfish.sleepersrv.assets.camera.Camera;
import fr.com.jellyfish.sleepersrv.assets.entities.Asteroid;
import fr.com.jellyfish.sleepersrv.assets.entities.Sphere;
import fr.com.jellyfish.sleepersrv.assets.globals.Cubemap;
import fr.com.jellyfish.sleepersrv.assets.globals.VCompass;
import fr.com.jellyfish.sleepersrv.constants.FrameVars;
import fr.com.jellyfish.sleepersrv.iomanagers.keyboard.KeyBoardManager;
import fr.com.jellyfish.sleepersrv.iomanagers.mouse.MouseButtonManager;
import fr.com.jellyfish.sleepersrv.iomanagers.mouse.MousePositionManager;
import fr.com.jellyfish.sleepersrv.opengl.util.ProgUtils;
import fr.com.jellyfish.sleepersrv.opengl.util.ShaderUtils;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import org.lwjgl.system.Callback;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * @author thw
 */
public class OpenGLGame {

    private Sphere gball;
    
    private GLCapabilities glCapabilities;
    private final Camera camera = new Camera();
    private final Cubemap cubeMap = new Cubemap();
    private final VCompass vCompass = new VCompass();
    private final List<AbstractAsset> assets = new ArrayList<>();
    private long window;

    private int defaultProg;
    private int default_viewUniform;
    private int default_projUniform;
    private int default_modelUniform;
    
    private int sphereProg;
    private int sphere_viewUniform;
    private int sphere_projUniform;
    private int sphere_modelUniform;
    
    private final float straveThrusterAccFactor = 20.0f;
    private final float mainThrusterAccFactor = 50.0f;
    private final float maxLinearVel = 200.0f;

    public boolean leftMouseDown = false;
    public boolean rightMouseDown = false;

    public float mouseX = 0.0f;
    public float mouseY = 0.0f;
    private long lastTime = System.nanoTime();

    private final Vector3f tempVect = new Vector3f();
    private final Matrix4f projMatrix = new Matrix4f();

    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f viewProjMatrix = new Matrix4f();
    private final Matrix4f invViewMatrix = new Matrix4f();
    private final Matrix4f invViewProjMatrix = new Matrix4f();
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    private final FrustumIntersection frustumIntersection = new FrustumIntersection();

    private KeyBoardManager keyCallback;
    private MousePositionManager mousePosCallBack;    
    private GLFWMouseButtonCallback buttonCallback;
    private Callback debugProc;
    
    private float dt;

    private void init() throws IOException {

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        window = glfwCreateWindow(FrameVars.V_WIDTH, FrameVars.V_HEIGHT, "SleeperSrv", 0L, NULL);
        if (window == NULL) {
            throw new AssertionError("Failed to create the GLFW window");
        }

        // Get the resolution of the primary monitor
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        // Center our window
        glfwSetWindowPos(
            window,
            (vidmode.width() - FrameVars.V_WIDTH) / 2,
            (vidmode.height() - FrameVars.V_HEIGHT) / 2
        );
        
        glfwSetCursor(window, glfwCreateStandardCursor(GLFW_IBEAM_CURSOR));

        glfwSetKeyCallback(window, keyCallback = new KeyBoardManager());
        glfwSetCursorPosCallback(window, mousePosCallBack = new MousePositionManager(this));        
        glfwSetMouseButtonCallback(window, buttonCallback = new MouseButtonManager(this));

        glfwMakeContextCurrent(window);
        glfwSwapInterval(0);
        glfwShowWindow(window);

        IntBuffer framebufferSize = BufferUtils.createIntBuffer(2);
        nglfwGetFramebufferSize(window, memAddress(framebufferSize), memAddress(framebufferSize) + 4);

        glCapabilities = GL.createCapabilities();
        if (!glCapabilities.OpenGL20) {
            throw new AssertionError("This demo requires OpenGL 2.0.");
        }

        debugProc = GLUtil.setupDebugMessageCallback();

        this.cubeMap.createCubemapTexture(glCapabilities);
        this.cubeMap.createFullScreenQuad();
        this.cubeMap.createCubemapProg();
        this.defaultProg = this.createDefaultProg();
        this.sphereProg = this.createSphereProg();
        this.vCompass.createMesh();
        
        for (int i = 0; i < 200 ; ++i) {
            this.assets.add(new Asteroid(camera, frustumIntersection, viewMatrix, matrixBuffer, 
                default_modelUniform, defaultProg));
        }
        
        this.assets.add(new Sphere(camera, frustumIntersection, viewMatrix, matrixBuffer, 
            sphere_modelUniform, sphereProg, this));
        
        glEnableClientState(GL_VERTEX_ARRAY);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
    }
    
    private int createDefaultProg() throws IOException {
        
        int vshader = ShaderUtils.createShader("fr/com/jellyfish/game/default_shader/default.vs", GL_VERTEX_SHADER);
        int fshader = ShaderUtils.createShader("fr/com/jellyfish/game/default_shader/default.fs", GL_FRAGMENT_SHADER);
        int prog = ProgUtils.createProgram(vshader, fshader);
        glUseProgram(prog);
        default_viewUniform = glGetUniformLocation(prog, "view");
        default_projUniform = glGetUniformLocation(prog, "proj");
        default_modelUniform = glGetUniformLocation(prog, "model");
        glUseProgram(0); 
        
        return prog;
    }
    
    private int createSphereProg() throws IOException {
        
        int vshader = ShaderUtils.createShader("fr/com/jellyfish/game/sphere_shader/sphere.vs", GL_VERTEX_SHADER);
        int fshader = ShaderUtils.createShader("fr/com/jellyfish/game/sphere_shader/sphere.fs", GL_FRAGMENT_SHADER);
        int prog = ProgUtils.createProgram(vshader, fshader);
        glUseProgram(prog);
        sphere_viewUniform = glGetUniformLocation(prog, "view");
        sphere_projUniform = glGetUniformLocation(prog, "proj");
        sphere_modelUniform = glGetUniformLocation(prog, "model");
        glUseProgram(0); 
        
        return prog;
    }

    private void update() {

        long thisTime = System.nanoTime();
        dt = (thisTime - lastTime) / 1E9f;
        lastTime = thisTime;
        camera.update(dt);

        projMatrix.setPerspective((float) Math.toRadians(40.0f),
                (float) FrameVars.V_WIDTH / FrameVars.V_HEIGHT, 0.1f, 5000.0f);
        viewMatrix.set(camera.rotation).invert(invViewMatrix);
        viewProjMatrix.set(projMatrix).mul(viewMatrix).invert(invViewProjMatrix);
        frustumIntersection.set(viewProjMatrix);

        /* Update the background shader */
        glUseProgram(this.cubeMap.cubemapProgram);
        glUniformMatrix4fv(this.cubeMap.cubemap_invViewProjUniform, false, invViewProjMatrix.get(matrixBuffer));

        /* Update the default shader */
        glUseProgram(defaultProg);
        glUniformMatrix4fv(default_viewUniform, false, viewMatrix.get(matrixBuffer));
        glUniformMatrix4fv(default_projUniform, false, projMatrix.get(matrixBuffer));
        
        /* Update the sphere shader */
        glUseProgram(sphereProg);
        glUniformMatrix4fv(sphere_viewUniform, false, viewMatrix.get(matrixBuffer));
        glUniformMatrix4fv(sphere_projUniform, false, projMatrix.get(matrixBuffer));
        
        updateControls();
    }

    private void updateControls() {

        camera.linearAcc.zero();
        float rotZ = 0.0f;
        
        if (keyCallback.kDown[GLFW_KEY_O]) {
            camera.linearAcc.fma(mainThrusterAccFactor, camera.forward(tempVect));
        }
        
        if (keyCallback.kDown[GLFW_KEY_L]) {
            camera.linearAcc.fma(-mainThrusterAccFactor, camera.forward(tempVect));
        }
        
        if (keyCallback.kDown[GLFW_KEY_RIGHT]) {
            camera.linearAcc.fma(straveThrusterAccFactor, camera.right(tempVect));
        }
        
        if (keyCallback.kDown[GLFW_KEY_LEFT]) {
            camera.linearAcc.fma(-straveThrusterAccFactor, camera.right(tempVect));
        }
        
        if (keyCallback.kDown[GLFW_KEY_K]) {
            rotZ = -1.0f;
        }
        
        if (keyCallback.kDown[GLFW_KEY_J]) {
            rotZ = +1.0f;
        }
        
        if (keyCallback.kDown[GLFW_KEY_UP]) {
            camera.linearAcc.fma(straveThrusterAccFactor, camera.up(tempVect));
        }
        
        if (keyCallback.kDown[GLFW_KEY_DOWN]) {
            camera.linearAcc.fma(-straveThrusterAccFactor, camera.up(tempVect));
        }

        if (rightMouseDown) {
            camera.angularAcc.set(2.0f * mouseY * mouseY * mouseY, 2.0f * mouseX * mouseX * mouseX, rotZ);
        } else if (!rightMouseDown) {
            camera.angularAcc.set(0, 0, rotZ);
        }
        
        double linearVelAbs = camera.linearVel.length();
        if (linearVelAbs > maxLinearVel) {
            camera.linearVel.normalize().mul(maxLinearVel);
        }
    }

    private void render() {
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
        this.cubeMap.draw();
        this.vCompass.drawCompass(projMatrix, matrixBuffer, viewMatrix, maxLinearVel, camera);
        for (AbstractAsset asset : assets) asset.draw();
        
    }

    private void loop() {

        while (!glfwWindowShouldClose(window)) {

            glfwPollEvents();
            glViewport(0, 0, FrameVars.V_WIDTH, FrameVars.V_HEIGHT);
            update();
            render();
            glfwSwapBuffers(window);
        }
    }

    void run() {

        try {

            init();
            loop();
            if (debugProc != null) debugProc.free();
            mousePosCallBack.free();            
            keyCallback.free();           
            buttonCallback.free();            
            glfwDestroyWindow(window);

        } catch (final Exception eX) {
            Logger.getLogger(OpenGLGame.class.getName()).log(Level.SEVERE, null, eX);
        } finally {
            glfwTerminate();
        }
    }

    /* *********************************************************************** */
    /* ACCESSORS */
    
    public Camera getCamera() {
        return camera;
    }
    
    public Matrix4f getProjMatrix() {
        return projMatrix;
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }
    
    public float getDt() {
        return dt;
    }

    
}
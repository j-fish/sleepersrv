/**
 * # sleepersrv, openGl & LWJGL3 + joml fun.
 * LWJGL 3 3d workshop
 * Maven project with joml and LWJGL3
 * All credits www.lwjgl.org & https://github.com/LWJGL/lwjgl3
 * All model credits https://nasa3d.arc.nasa.gov/
 */

package fr.com.jfish.sleepersrv.opengl.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glShaderSource;

/**
 *
 * @author thw
 */
public class ShaderUtils {
    
    public static int createShader(String resource, int type) throws IOException {
        
        int shader = glCreateShader(type);
        ByteBuffer source = OpenGLUtils.ioResourceToByteBuffer(resource, 1024);
        PointerBuffer strings = BufferUtils.createPointerBuffer(1);
        IntBuffer lengths = BufferUtils.createIntBuffer(1);
        strings.put(0, source);
        lengths.put(0, source.remaining());
        glShaderSource(shader, strings, lengths);
        glCompileShader(shader);
        int compiled = glGetShaderi(shader, GL_COMPILE_STATUS);
        String shaderLog = glGetShaderInfoLog(shader);
        
        if (shaderLog.trim().length() > 0) {
            System.err.println(shaderLog);
        }
        
        if (compiled == 0) {
            throw new AssertionError("Could not compile shader");
        }
        
        return shader;
    }
    
}

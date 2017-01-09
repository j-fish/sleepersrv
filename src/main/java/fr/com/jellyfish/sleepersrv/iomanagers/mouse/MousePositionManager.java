package fr.com.jellyfish.sleepersrv.iomanagers.mouse;

import fr.com.jellyfish.sleepersrv.constants.FrameVars;
import fr.com.jellyfish.sleepersrv.game.OpenGLGame;
import org.lwjgl.glfw.GLFWCursorPosCallback;

/**
 * @author thw
 */
public class MousePositionManager extends GLFWCursorPosCallback {

    final OpenGLGame game;
    
    public MousePositionManager(final OpenGLGame game) {
        this.game = game;
    }
    
    @Override
    public void invoke(long window, double xpos, double ypos) {
        float normX = (float) ((xpos - FrameVars.V_WIDTH / 2.0) / FrameVars.V_WIDTH * 2.0);
        float normY = (float) ((ypos - FrameVars.V_HEIGHT / 2.0) / FrameVars.V_HEIGHT * 2.0);
        game.mouseX = Math.max(-FrameVars.V_WIDTH / 2.0f, Math.min(FrameVars.V_WIDTH / 2.0f, normX));
        game.mouseY = Math.max(-FrameVars.V_HEIGHT / 2.0f, Math.min(FrameVars.V_HEIGHT / 2.0f, normY));
    }  
    
}
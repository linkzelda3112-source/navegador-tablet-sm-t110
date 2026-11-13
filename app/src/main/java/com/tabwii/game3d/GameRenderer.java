package com.tabwii.game3d;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * El motor de render. Dibuja todos los {@link SceneObject} de la escena con una luz direccional
 * sencilla, y maneja la cámara en órbita (das vueltas alrededor del centro de la escena con el
 * dedo). También es el único lugar donde se puede llamar a OpenGL de verdad (tiene que ser en su
 * propio hilo, por eso los modelos nuevos se piden con {@link #requestAddModel} y se suben aquí
 * cuando toca dibujar el siguiente cuadro).
 */
public class GameRenderer implements GLSurfaceView.Renderer {

    public interface ModelLoadCallback {
        void onLoaded(SceneObject object);
        void onError(Exception e);
    }

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uRotationMatrix;\n" +
            "attribute vec3 aPosition;\n" +
            "attribute vec3 aNormal;\n" +
            "attribute vec2 aTexCoord;\n" +
            "varying vec3 vNormal;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "  gl_Position = uMVPMatrix * vec4(aPosition, 1.0);\n" +
            "  vNormal = (uRotationMatrix * vec4(aNormal, 0.0)).xyz;\n" +
            "  vTexCoord = aTexCoord;\n" +
            "}\n";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform vec3 uColor;\n" +
            "uniform float uHasTexture;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform vec3 uLightDir;\n" +
            "varying vec3 vNormal;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "  vec3 base = mix(uColor, texture2D(uTexture, vTexCoord).rgb, uHasTexture);\n" +
            "  float diffuse = max(dot(normalize(vNormal), uLightDir), 0.0);\n" +
            "  vec3 lit = base * (0.45 + diffuse * 0.6);\n" +
            "  gl_FragColor = vec4(lit, 1.0);\n" +
            "}\n";

    private int program;
    private int aPositionLoc, aNormalLoc, aTexCoordLoc;
    private int uMVPMatrixLoc, uRotationMatrixLoc, uColorLoc, uHasTextureLoc, uTextureLoc, uLightDirLoc;

    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] rotationMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private int viewportWidth = 1, viewportHeight = 1;

    // --- Cámara en órbita alrededor del origen (0,0,0) ---
    public volatile float cameraYaw = 45f;
    public volatile float cameraPitch = 25f;
    public volatile float cameraDistance = 8f;

    private final List<SceneObject> sceneObjects = new ArrayList<>();
    private final Object sceneLock = new Object();

    private static class PendingLoad {
        RawMesh mesh;
        String label;
        ModelLoadCallback callback;
    }
    private final ConcurrentLinkedQueue<PendingLoad> pendingLoads = new ConcurrentLinkedQueue<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void requestAddModel(RawMesh mesh, String label, ModelLoadCallback callback) {
        PendingLoad p = new PendingLoad();
        p.mesh = mesh;
        p.label = label;
        p.callback = callback;
        pendingLoads.add(p);
    }

    public List<SceneObject> getSceneObjectsSnapshot() {
        synchronized (sceneLock) {
            return new ArrayList<>(sceneObjects);
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.12f, 0.14f, 0.18f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        aPositionLoc = GLES20.glGetAttribLocation(program, "aPosition");
        aNormalLoc = GLES20.glGetAttribLocation(program, "aNormal");
        aTexCoordLoc = GLES20.glGetAttribLocation(program, "aTexCoord");
        uMVPMatrixLoc = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        uRotationMatrixLoc = GLES20.glGetUniformLocation(program, "uRotationMatrix");
        uColorLoc = GLES20.glGetUniformLocation(program, "uColor");
        uHasTextureLoc = GLES20.glGetUniformLocation(program, "uHasTexture");
        uTextureLoc = GLES20.glGetUniformLocation(program, "uTexture");
        uLightDirLoc = GLES20.glGetUniformLocation(program, "uLightDir");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        viewportWidth = width;
        viewportHeight = height;
        GLES20.glViewport(0, 0, width, height);
        float aspect = (float) width / (float) Math.max(1, height);
        Matrix.perspectiveM(projectionMatrix, 0, 55f, aspect, 0.1f, 100f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        drainPendingLoads();

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);

        updateViewMatrix();

        float[] lightDir = normalize(0.5f, 1f, 0.4f);
        GLES20.glUniform3f(uLightDirLoc, lightDir[0], lightDir[1], lightDir[2]);

        List<SceneObject> snapshot = getSceneObjectsSnapshot();
        for (SceneObject obj : snapshot) {
            drawObject(obj);
        }
    }

    private void drawObject(SceneObject obj) {
        GpuModel model = obj.model;

        Matrix.setIdentityM(rotationMatrix, 0);
        Matrix.rotateM(rotationMatrix, 0, obj.rotationY, 0f, 1f, 0f);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, obj.x, obj.y, obj.z);
        Matrix.multiplyMM(modelMatrix, 0, modelMatrix, 0, rotationMatrix, 0);
        Matrix.scaleM(modelMatrix, 0, obj.scale, obj.scale, obj.scale);

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);

        GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(uRotationMatrixLoc, 1, false, rotationMatrix, 0);
        GLES20.glUniform3f(uColorLoc, model.colorR, model.colorG, model.colorB);
        GLES20.glUniform1f(uHasTextureLoc, model.hasTexture ? 1f : 0f);

        if (model.hasTexture) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, model.textureId);
            GLES20.glUniform1i(uTextureLoc, 0);
        }

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, model.vertexBufferId);
        int stride = RawMesh.FLOATS_PER_VERTEX * 4;
        GLES20.glEnableVertexAttribArray(aPositionLoc);
        GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, stride, 0);
        GLES20.glEnableVertexAttribArray(aNormalLoc);
        GLES20.glVertexAttribPointer(aNormalLoc, 3, GLES20.GL_FLOAT, false, stride, 3 * 4);
        GLES20.glEnableVertexAttribArray(aTexCoordLoc);
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, stride, 6 * 4);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, model.vertexCount);

        GLES20.glDisableVertexAttribArray(aPositionLoc);
        GLES20.glDisableVertexAttribArray(aNormalLoc);
        GLES20.glDisableVertexAttribArray(aTexCoordLoc);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    private void updateViewMatrix() {
        double yawRad = Math.toRadians(cameraYaw);
        double pitchRad = Math.toRadians(cameraPitch);
        float cx = (float) (cameraDistance * Math.cos(pitchRad) * Math.sin(yawRad));
        float cy = (float) (cameraDistance * Math.sin(pitchRad));
        float cz = (float) (cameraDistance * Math.cos(pitchRad) * Math.cos(yawRad));
        Matrix.setLookAtM(viewMatrix, 0, cx, cy, cz, 0f, 0f, 0f, 0f, 1f, 0f);
    }

    private void drainPendingLoads() {
        PendingLoad p;
        while ((p = pendingLoads.poll()) != null) {
            try {
                GpuModel gpuModel = uploadToGpu(p.mesh);
                SceneObject obj = new SceneObject(gpuModel, p.label);
                synchronized (sceneLock) {
                    sceneObjects.add(obj);
                }
                notifySuccess(p.callback, obj);
            } catch (Exception e) {
                notifyError(p.callback, e);
            }
        }
    }

    private GpuModel uploadToGpu(RawMesh mesh) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(mesh.interleavedVertexData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        buffer.put(mesh.interleavedVertexData).position(0);

        int[] vboHandle = new int[1];
        GLES20.glGenBuffers(1, vboHandle, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboHandle[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, buffer.capacity() * 4, buffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        int textureId = 0;
        boolean hasTexture = mesh.texture != null;
        if (hasTexture) {
            int[] texHandle = new int[1];
            GLES20.glGenTextures(1, texHandle, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texHandle[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mesh.texture, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            textureId = texHandle[0];
        }

        return new GpuModel(vboHandle[0], mesh.vertexCount, textureId, hasTexture,
                mesh.colorR, mesh.colorG, mesh.colorB, mesh.sourceName);
    }

    private void notifySuccess(final ModelLoadCallback callback, final SceneObject obj) {
        if (callback == null) return;
        mainHandler.post(new Runnable() {
            @Override public void run() { callback.onLoaded(obj); }
        });
    }

    private void notifyError(final ModelLoadCallback callback, final Exception e) {
        if (callback == null) return;
        mainHandler.post(new Runnable() {
            @Override public void run() { callback.onError(e); }
        });
    }

    private int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private float[] normalize(float x, float y, float z) {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        return new float[]{x / len, y / len, z / len};
    }
}

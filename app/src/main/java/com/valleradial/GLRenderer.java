package com.valleradial;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Dibuja el mundo esferico: terreno "cube-sphere" por trozos (shader toon con pinceladas 3D),
 * casas, torres, arcos, molinos, ruinas, arboles, rocas y enemigos.
 */
final class GLRenderer implements GLSurfaceView.Renderer {
    private static final float FOG_R = 0.97f, FOG_G = 0.88f, FOG_B = 0.62f;
    private static final float FOG_START = 120f, FOG_END = 420f, FAR = 490f;
    private static final float FOV = 72f;

    private final Game game;

    private int progTerrain, progProp;
    // terreno: 6 caras x 7x7 trozos de 16x16 celdas
    private static final int FACE_N = 112, CH = 16, CPF = FACE_N / CH;
    private int[] chunkVbo;
    private float[] chunkCx, chunkCy, chunkCz;
    private int ibo, idxCount;
    // mallas de objetos: indice = tipo*4 + variante
    private final Mesh[] meshes = new Mesh[8 * 4];
    @SuppressWarnings("unchecked")
    private final ArrayList<World.Prop>[] byKey = new ArrayList[8 * 4];
    private Mesh enemyM;

    private final float[] proj = new float[16], view = new float[16], vp = new float[16];
    private int width = 1, height = 1;
    private long last;
    private float halfDiag;

    private static final class Mesh {
        int vbo, count;
    }

    GLRenderer(Game g) {
        game = g;
    }

    // ------------------------------------------------------------------ shaders
    private static final String TERRAIN_VS =
            "uniform mat4 uVP; uniform vec3 uCam;\n" +
            "attribute vec3 aPos; attribute vec3 aNormal; attribute float aPath;\n" +
            "varying vec3 vN; varying vec3 vW; varying float vPath; varying float vDist;\n" +
            "void main(){ vN=aNormal; vW=aPos; vPath=aPath; vDist=length(uCam-aPos);\n" +
            " gl_Position=uVP*vec4(aPos,1.0); }";

    private static final String TERRAIN_FS =
            "#ifdef GL_FRAGMENT_PRECISION_HIGH\nprecision highp float;\n#else\nprecision mediump float;\n#endif\n" +
            "uniform vec3 uFog; uniform vec2 uFogRange; uniform vec3 uLight;\n" +
            "varying vec3 vN; varying vec3 vW; varying float vPath; varying float vDist;\n" +
            "float h31(vec3 p){ return fract(sin(dot(p, vec3(127.1,311.7,74.7)))*43758.5453); }\n" +
            "float vn(vec3 p){ vec3 i=floor(p); vec3 f=fract(p); f=f*f*(3.0-2.0*f);\n" +
            " float a=h31(i), b=h31(i+vec3(1.,0.,0.)), c=h31(i+vec3(0.,1.,0.)), d=h31(i+vec3(1.,1.,0.));\n" +
            " float e=h31(i+vec3(0.,0.,1.)), g=h31(i+vec3(1.,0.,1.)), h=h31(i+vec3(0.,1.,1.)), k=h31(i+vec3(1.,1.,1.));\n" +
            " return mix(mix(mix(a,b,f.x), mix(c,d,f.x), f.y), mix(mix(e,g,f.x), mix(h,k,f.x), f.y), f.z); }\n" +
            "void main(){\n" +
            " float big=vn(vW/16.0);\n" +
            " float mid=vn(vW/5.5+7.3);\n" +
            " float fine=vn(vW/2.1+3.1);\n" +
            " vec3 c=mix(vec3(0.93,0.78,0.30), vec3(0.84,0.66,0.22), step(0.5, fine*0.6+mid*0.4));\n" +
            " float g=step(0.62, big*0.85+mid*0.25);\n" +
            " vec3 green=mix(vec3(0.60,0.68,0.22), vec3(0.50,0.60,0.20), step(0.5,fine));\n" +
            " c=mix(c, green, g);\n" +
            " float pk=step(0.74, mid*0.6+fine*0.5)*(1.0-g);\n" +
            " c=mix(c, vec3(0.92,0.40,0.46), pk);\n" +
            " float pv=vPath+(fine-0.5)*0.35;\n" +
            " c=mix(c, vec3(0.97,0.90,0.78), step(0.5,pv));\n" +
            " float ndl=dot(normalize(vN), normalize(uLight));\n" +
            " float band=ndl>-0.2 ? 1.0 : 0.88;\n" +
            " float f=smoothstep(uFogRange.x, uFogRange.y, vDist);\n" +
            " gl_FragColor=vec4(mix(c*band, uFog, f), 1.0); }";

    private static final String PROP_VS =
            "uniform mat4 uVP; uniform mat4 uModel; uniform vec3 uCam; uniform vec3 uOutScale; uniform float uOutline;\n" +
            "attribute vec3 aPos; attribute vec3 aNormal; attribute vec3 aColor;\n" +
            "varying vec3 vN; varying vec3 vC; varying float vDist;\n" +
            "void main(){ vec3 lp = (uOutline>0.5) ? aPos*uOutScale : aPos;\n" +
            " vec4 w = uModel*vec4(lp,1.0); vN=mat3(uModel)*aNormal; vC=aColor; vDist=length(uCam-w.xyz);\n" +
            " gl_Position=uVP*w; }";

    private static final String PROP_FS =
            "precision mediump float;\n" +
            "uniform vec3 uFog; uniform vec2 uFogRange; uniform vec3 uLight; uniform vec3 uTint; uniform float uOutline; uniform float uFlash;\n" +
            "varying vec3 vN; varying vec3 vC; varying float vDist;\n" +
            "void main(){\n" +
            " vec3 c;\n" +
            " if(uOutline>0.5){ c=vec3(0.07,0.05,0.06); }\n" +
            " else { float ndl=dot(normalize(vN), normalize(uLight));\n" +
            "  float band = ndl>0.3 ? 1.0 : (ndl>-0.2 ? 0.86 : 0.72);\n" +
            "  c = vC*uTint*band; c=mix(c, vec3(1.0), uFlash); }\n" +
            " float f=smoothstep(uFogRange.x, uFogRange.y, vDist);\n" +
            " gl_FragColor=vec4(mix(c, uFog, f), 1.0); }";

    private static int compile(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        int[] ok = new int[1];
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(s);
            GLES20.glDeleteShader(s);
            throw new RuntimeException("Shader: " + log);
        }
        return s;
    }

    private static int program(String vs, String fs) {
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, compile(GLES20.GL_VERTEX_SHADER, vs));
        GLES20.glAttachShader(p, compile(GLES20.GL_FRAGMENT_SHADER, fs));
        GLES20.glLinkProgram(p);
        int[] ok = new int[1];
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0);
        if (ok[0] == 0) throw new RuntimeException("Link: " + GLES20.glGetProgramInfoLog(p));
        return p;
    }

    private static FloatBuffer fb(float[] a) {
        FloatBuffer b = ByteBuffer.allocateDirect(a.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        b.put(a).position(0);
        return b;
    }

    private static int vbo(float[] data) {
        int[] id = new int[1];
        GLES20.glGenBuffers(1, id, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, id[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, data.length * 4, fb(data), GLES20.GL_STATIC_DRAW);
        return id[0];
    }

    private static Mesh mesh(MeshBuilder b) {
        Mesh m = new Mesh();
        float[] a = b.toArray();
        m.vbo = vbo(a);
        m.count = a.length / 9;
        return m;
    }

    // ------------------------------------------------------------------ GL callbacks
    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        progTerrain = program(TERRAIN_VS, TERRAIN_FS);
        progProp = program(PROP_VS, PROP_FS);
        buildTerrain();

        meshes[World.HOUSE * 4] = mesh(MeshBuilder.house());
        meshes[World.ROCK * 4] = mesh(MeshBuilder.rock());
        meshes[World.TOWER * 4] = mesh(MeshBuilder.tower());
        meshes[World.ARCH * 4] = mesh(MeshBuilder.arch());
        meshes[World.WINDMILL * 4] = mesh(MeshBuilder.windmill());
        meshes[World.RUIN * 4] = mesh(MeshBuilder.ruin());
        for (int i = 0; i < 4; i++) meshes[World.TREE * 4 + i] = mesh(MeshBuilder.tree(i));
        for (int i = 0; i < 2; i++) meshes[World.BUSH * 4 + i] = mesh(MeshBuilder.bush(i));
        enemyM = mesh(MeshBuilder.enemy());

        for (int i = 0; i < byKey.length; i++) byKey[i] = new ArrayList<>();
        for (World.Prop p : game.world.props) {
            int key = p.kind * 4 + (p.variant & 3);
            if (meshes[key] != null) byKey[key].add(p);
        }

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        last = System.nanoTime();
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int w, int h) {
        width = Math.max(1, w);
        height = Math.max(1, h);
        GLES20.glViewport(0, 0, width, height);
        float aspect = (float) width / height;
        Matrix.perspectiveM(proj, 0, FOV, aspect, 0.3f, FAR);
        double th = Math.tan(Math.toRadians(FOV / 2.0));
        halfDiag = (float) Math.atan(Math.sqrt(th * th + Math.pow(th * aspect, 2)));
        last = System.nanoTime();
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        long now = System.nanoTime();
        float dt = (now - last) * 1e-9f;
        last = now;
        game.update(dt);

        Matrix.setLookAtM(view, 0, game.ex, game.ey, game.ez,
                game.ex + game.dx, game.ey + game.dy, game.ez + game.dz,
                game.ux, game.uy, game.uz);
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0);
        System.arraycopy(vp, 0, game.vp, 0, 16);

        GLES20.glClearColor(FOG_R, FOG_G, FOG_B, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        drawTerrain();
        drawProps();
    }

    // ------------------------------------------------------------------ terreno (cube-sphere)
    /** Direccion unitaria del vertice (i,j) de la cara f. i,j pueden salirse de [0,FACE_N] (para normales). */
    private static void dirAt(int face, int i, int j, float[] out) {
        double a = Math.tan((-1.0 + 2.0 * i / FACE_N) * Math.PI / 4.0);
        double b = Math.tan((-1.0 + 2.0 * j / FACE_N) * Math.PI / 4.0);
        double x, y, z;
        switch (face) {
            case 0: x = 1; y = a; z = b; break;
            case 1: x = -1; y = a; z = b; break;
            case 2: x = a; y = 1; z = b; break;
            case 3: x = a; y = -1; z = b; break;
            case 4: x = a; y = b; z = 1; break;
            default: x = a; y = b; z = -1; break;
        }
        double l = Math.sqrt(x * x + y * y + z * z);
        out[0] = (float) (x / l); out[1] = (float) (y / l); out[2] = (float) (z / l);
    }

    private static void posAt(int face, int i, int j, float[] out) {
        dirAt(face, i, j, out);
        float rg = World.groundRadius(out[0], out[1], out[2]);
        out[0] *= rg; out[1] *= rg; out[2] *= rg;
    }

    private void buildTerrain() {
        int n = 6 * CPF * CPF;
        chunkVbo = new int[n];
        chunkCx = new float[n]; chunkCy = new float[n]; chunkCz = new float[n];
        final int v = CH + 1;
        float[] buf = new float[v * v * 7];
        float[] d = new float[3], p0 = new float[3], pa = new float[3], pb = new float[3], pc = new float[3], pd = new float[3];
        int id = 0;
        for (int face = 0; face < 6; face++) {
            for (int cj = 0; cj < CPF; cj++) {
                for (int ci = 0; ci < CPF; ci++) {
                    int k = 0;
                    float sx = 0, sy = 0, sz = 0;
                    for (int b = 0; b < v; b++) {
                        for (int a = 0; a < v; a++) {
                            int gi = ci * CH + a, gj = cj * CH + b;
                            posAt(face, gi, gj, p0);
                            posAt(face, gi + 1, gj, pa); posAt(face, gi - 1, gj, pb);
                            posAt(face, gi, gj + 1, pc); posAt(face, gi, gj - 1, pd);
                            float t1x = pa[0] - pb[0], t1y = pa[1] - pb[1], t1z = pa[2] - pb[2];
                            float t2x = pc[0] - pd[0], t2y = pc[1] - pd[1], t2z = pc[2] - pd[2];
                            float nx = t1y * t2z - t1z * t2y, ny = t1z * t2x - t1x * t2z, nz = t1x * t2y - t1y * t2x;
                            float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                            nx /= l; ny /= l; nz /= l;
                            // la normal apunta al centro (hacia donde esta "arriba")
                            if (nx * p0[0] + ny * p0[1] + nz * p0[2] > 0f) { nx = -nx; ny = -ny; nz = -nz; }
                            dirAt(face, gi, gj, d);
                            buf[k++] = p0[0]; buf[k++] = p0[1]; buf[k++] = p0[2];
                            buf[k++] = nx; buf[k++] = ny; buf[k++] = nz;
                            buf[k++] = World.pathAttr(d[0], d[1], d[2]);
                            sx += p0[0]; sy += p0[1]; sz += p0[2];
                        }
                    }
                    chunkVbo[id] = vbo(buf);
                    chunkCx[id] = sx / (v * v); chunkCy[id] = sy / (v * v); chunkCz[id] = sz / (v * v);
                    id++;
                }
            }
        }
        short[] idx = new short[CH * CH * 6];
        int q = 0;
        for (int b = 0; b < CH; b++) {
            for (int a = 0; a < CH; a++) {
                int i0 = b * v + a, i1 = i0 + 1, i2 = i0 + v, i3 = i2 + 1;
                idx[q++] = (short) i0; idx[q++] = (short) i1; idx[q++] = (short) i2;
                idx[q++] = (short) i1; idx[q++] = (short) i3; idx[q++] = (short) i2;
            }
        }
        idxCount = idx.length;
        ShortBuffer sb = ByteBuffer.allocateDirect(idx.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        sb.put(idx).position(0);
        int[] ib = new int[1];
        GLES20.glGenBuffers(1, ib, 0);
        ibo = ib[0];
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, idx.length * 2, sb, GLES20.GL_STATIC_DRAW);
    }

    private boolean visible(float cx, float cy, float cz, float radius, float maxDist) {
        float rx = cx - game.ex, ry = cy - game.ey, rz = cz - game.ez;
        float d = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (d - radius > maxDist) return false;
        if (d < radius + 1f) return true;
        float dot = (rx * game.dx + ry * game.dy + rz * game.dz) / d;
        double ang = Math.acos(Math.max(-1f, Math.min(1f, dot)));
        double lim = halfDiag + Math.asin(Math.min(1f, radius / d));
        return ang < lim;
    }

    private void drawTerrain() {
        GLES20.glUseProgram(progTerrain);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(progTerrain, "uVP"), 1, false, vp, 0);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(progTerrain, "uCam"), game.ex, game.ey, game.ez);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(progTerrain, "uFog"), FOG_R, FOG_G, FOG_B);
        GLES20.glUniform2f(GLES20.glGetUniformLocation(progTerrain, "uFogRange"), FOG_START, FOG_END);
        // la normal del terreno apunta al centro; la luz tambien "hacia el centro" -> normal . (-luz)
        GLES20.glUniform3f(GLES20.glGetUniformLocation(progTerrain, "uLight"),
                game.ux * 0.6f + game.dx * 0.3f, game.uy * 0.6f + game.dy * 0.3f, game.uz * 0.6f + game.dz * 0.3f);
        int aPos = GLES20.glGetAttribLocation(progTerrain, "aPos");
        int aN = GLES20.glGetAttribLocation(progTerrain, "aNormal");
        int aPath = GLES20.glGetAttribLocation(progTerrain, "aPath");
        GLES20.glEnableVertexAttribArray(aPos);
        GLES20.glEnableVertexAttribArray(aN);
        GLES20.glEnableVertexAttribArray(aPath);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo);
        for (int i = 0; i < chunkVbo.length; i++) {
            if (!visible(chunkCx[i], chunkCy[i], chunkCz[i], 50f, FAR)) continue;
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, chunkVbo[i]);
            GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 28, 0);
            GLES20.glVertexAttribPointer(aN, 3, GLES20.GL_FLOAT, false, 28, 12);
            GLES20.glVertexAttribPointer(aPath, 1, GLES20.GL_FLOAT, false, 28, 24);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, idxCount, GLES20.GL_UNSIGNED_SHORT, 0);
        }
        GLES20.glDisableVertexAttribArray(aPos);
        GLES20.glDisableVertexAttribArray(aN);
        GLES20.glDisableVertexAttribArray(aPath);
    }

    // ------------------------------------------------------------------ objetos
    private int uModel, uOutline, uOutScale, uTint, uFlash, aPos, aNormal, aColor;

    private void bindMesh(Mesh m) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, m.vbo);
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 36, 0);
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 36, 12);
        GLES20.glVertexAttribPointer(aColor, 3, GLES20.GL_FLOAT, false, 36, 24);
    }

    private void drawInstance(Mesh m, float[] model, float flash, boolean outline) {
        GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0);
        GLES20.glUniform3f(uTint, 1f, 1f, 1f);
        GLES20.glUniform1f(uFlash, flash);
        if (outline) {
            // contorno: casco invertido (caras delanteras fuera) un poco mas grande
            GLES20.glUniform1f(uOutline, 1f);
            GLES20.glUniform3f(uOutScale, 1.07f, 1.04f, 1.07f);
            GLES20.glCullFace(GLES20.GL_FRONT);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, m.count);
        }
        GLES20.glUniform1f(uOutline, 0f);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, m.count);
    }

    private final float[] enemyModel = new float[16];

    private static float radiusOf(World.Prop p) {
        switch (p.kind) {
            case World.TOWER: return 14f * p.scale;
            case World.WINDMILL: return 12f * p.scale;
            case World.HOUSE: return 9f * p.scale;
            case World.ARCH: return 9f * p.scale;
            case World.RUIN: return 6f * p.scale;
            case World.TREE: return 7f * p.scale;
            case World.ROCK: return 3f * p.scale;
            default: return 2.5f * p.scale;
        }
    }

    private static float maxDistOf(int kind) {
        switch (kind) {
            case World.BUSH: return 130f;
            case World.ROCK: return 230f;
            case World.RUIN: return 260f;
            case World.TREE: return 300f;
            default: return FAR;
        }
    }

    private void drawProps() {
        GLES20.glUseProgram(progProp);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glFrontFace(GLES20.GL_CCW);
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(progProp, "uVP"), 1, false, vp, 0);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(progProp, "uCam"), game.ex, game.ey, game.ez);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(progProp, "uFog"), FOG_R, FOG_G, FOG_B);
        GLES20.glUniform2f(GLES20.glGetUniformLocation(progProp, "uFogRange"), FOG_START, FOG_END);
        // luz relativa a la camara
        GLES20.glUniform3f(GLES20.glGetUniformLocation(progProp, "uLight"),
                game.ux * 0.6f + game.dx * 0.3f + 0.1f, game.uy * 0.6f + game.dy * 0.3f + 0.1f,
                game.uz * 0.6f + game.dz * 0.3f + 0.1f);
        uModel = GLES20.glGetUniformLocation(progProp, "uModel");
        uOutline = GLES20.glGetUniformLocation(progProp, "uOutline");
        uOutScale = GLES20.glGetUniformLocation(progProp, "uOutScale");
        uTint = GLES20.glGetUniformLocation(progProp, "uTint");
        uFlash = GLES20.glGetUniformLocation(progProp, "uFlash");
        aPos = GLES20.glGetAttribLocation(progProp, "aPos");
        aNormal = GLES20.glGetAttribLocation(progProp, "aNormal");
        aColor = GLES20.glGetAttribLocation(progProp, "aColor");
        GLES20.glEnableVertexAttribArray(aPos);
        GLES20.glEnableVertexAttribArray(aNormal);
        GLES20.glEnableVertexAttribArray(aColor);

        for (int key = 0; key < byKey.length; key++) {
            ArrayList<World.Prop> list = byKey[key];
            if (list.isEmpty()) continue;
            Mesh m = meshes[key];
            bindMesh(m);
            float maxD = maxDistOf(key / 4);
            for (int i = 0; i < list.size(); i++) {
                World.Prop p = list.get(i);
                if (!visible(p.x, p.y, p.z, radiusOf(p), maxD)) continue;
                float rx = p.x - game.ex, ry = p.y - game.ey, rz = p.z - game.ez;
                boolean outline = rx * rx + ry * ry + rz * rz < 150f * 150f;
                drawInstance(m, p.model, 0f, outline);
            }
        }

        // enemigos
        bindMesh(enemyM);
        for (Game.Enemy e : game.enemies) {
            if (!e.alive) continue;
            if (!visible(e.x, e.y, e.z, 4f, FAR - 20f)) continue;
            float d = (float) Math.sqrt(e.x * e.x + e.y * e.y + e.z * e.z);
            float ux = -e.x / d, uy = -e.y / d, uz = -e.z / d;
            // base: Y=arriba, Z=hacia el jugador, X = Y x Z
            float zx = e.fx, zy = e.fy, zz = e.fz;
            float xx = uy * zz - uz * zy, xy = uz * zx - ux * zz, xz = ux * zy - uy * zx;
            float xl = (float) Math.sqrt(xx * xx + xy * xy + xz * xz);
            if (xl < 1e-4f) continue;
            xx /= xl; xy /= xl; xz /= xl;
            float bob = (float) Math.sin(e.bob) * 0.15f + 0.15f;
            float[] m = enemyModel;
            m[0] = xx; m[1] = xy; m[2] = xz; m[3] = 0;
            m[4] = ux; m[5] = uy; m[6] = uz; m[7] = 0;
            m[8] = zx; m[9] = zy; m[10] = zz; m[11] = 0;
            m[12] = e.x + ux * bob; m[13] = e.y + uy * bob; m[14] = e.z + uz * bob; m[15] = 1;
            drawInstance(enemyM, m, e.hurt > 0 ? 0.7f : 0f, true);
        }

        GLES20.glDisableVertexAttribArray(aPos);
        GLES20.glDisableVertexAttribArray(aNormal);
        GLES20.glDisableVertexAttribArray(aColor);
    }
}

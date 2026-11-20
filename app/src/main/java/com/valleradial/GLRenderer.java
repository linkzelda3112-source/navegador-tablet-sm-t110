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

/** Dibuja el valle: terreno por trozos (shader toon con pinceladas), casas, arboles, rocas y enemigos. */
final class GLRenderer implements GLSurfaceView.Renderer {
    private static final float FOG_R = 0.97f, FOG_G = 0.88f, FOG_B = 0.62f;
    private static final float FOG_START = 130f, FOG_END = 400f, FAR = 430f;
    private static final float FOV = 72f;

    private final Game game;

    private int progTerrain, progProp;
    // terreno
    private static final int CH_A = 26, CH_B = 12;
    private int chunksA, chunksB;
    private int[] chunkVbo;
    private float[] chunkCx, chunkCy, chunkCz;
    private int ibo, idxCount;
    // mallas de objetos
    private Mesh houseM, rockM, enemyM;
    private final Mesh[] treeM = new Mesh[4];
    private final Mesh[] bushM = new Mesh[2];

    private final float[] proj = new float[16], view = new float[16], vp = new float[16];
    private int width = 1, height = 1;
    private long last;
    private float halfDiagCos;

    private static final class Mesh {
        int vbo, count;
    }

    GLRenderer(Game g) {
        game = g;
    }

    // ------------------------------------------------------------------ shaders
    private static final String TERRAIN_VS =
            "uniform mat4 uVP; uniform vec3 uCam;\n" +
            "attribute vec3 aPos; attribute vec3 aNormal; attribute vec2 aUV; attribute float aPath;\n" +
            "varying vec3 vN; varying vec2 vUV; varying float vPath; varying float vDist;\n" +
            "void main(){ vN=aNormal; vUV=aUV; vPath=aPath; vDist=length(uCam-aPos);\n" +
            " gl_Position=uVP*vec4(aPos,1.0); }";

    private static final String TERRAIN_FS =
            "#ifdef GL_FRAGMENT_PRECISION_HIGH\nprecision highp float;\n#else\nprecision mediump float;\n#endif\n" +
            "uniform vec3 uFog; uniform vec2 uFogRange; uniform vec3 uLight;\n" +
            "varying vec3 vN; varying vec2 vUV; varying float vPath; varying float vDist;\n" +
            "float h21(vec2 p){ return fract(sin(dot(p, vec2(127.1,311.7)))*43758.5453); }\n" +
            "float vn(vec2 p, float per){ vec2 i=floor(p); vec2 f=fract(p); f=f*f*(3.0-2.0*f);\n" +
            " float x0=mod(i.x,per); float x1=mod(i.x+1.0,per);\n" +
            " float a=h21(vec2(x0,i.y)); float b=h21(vec2(x1,i.y));\n" +
            " float c=h21(vec2(x0,i.y+1.0)); float d=h21(vec2(x1,i.y+1.0));\n" +
            " return mix(mix(a,b,f.x), mix(c,d,f.x), f.y); }\n" +
            "void main(){\n" +
            " float big=vn(vUV/12.0, 26.0);\n" +
            " float mid=vn(vUV/4.0, 78.0);\n" +
            " float fine=vn(vec2(vUV.x/1.5, vUV.y/9.0), 208.0);\n" +
            " float fine2=vn(vec2(vUV.x, vUV.y/6.0), 312.0);\n" +
            " vec3 c=mix(vec3(0.93,0.78,0.30), vec3(0.84,0.66,0.22), step(0.5, fine*0.6+fine2*0.5));\n" +
            " float g=step(0.63, big*0.8+mid*0.3);\n" +
            " vec3 green=mix(vec3(0.60,0.68,0.22), vec3(0.50,0.60,0.20), step(0.5,fine));\n" +
            " c=mix(c, green, g);\n" +
            " float pk=step(0.76, mid*0.7+fine2*0.45)*(1.0-g);\n" +
            " c=mix(c, vec3(0.92,0.40,0.46), pk);\n" +
            " float pv=vPath+(fine-0.5)*0.35;\n" +
            " c=mix(c, vec3(0.97,0.90,0.78), step(0.5,pv));\n" +
            " float ndl=dot(normalize(vN), normalize(uLight));\n" +
            " float band=ndl>0.15 ? 1.0 : 0.88;\n" +
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
            "  float band = ndl>0.35 ? 1.0 : (ndl>-0.15 ? 0.84 : 0.68);\n" +
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
        houseM = mesh(MeshBuilder.house());
        rockM = mesh(MeshBuilder.rock());
        enemyM = mesh(MeshBuilder.enemy());
        for (int i = 0; i < 4; i++) treeM[i] = mesh(MeshBuilder.tree(i));
        for (int i = 0; i < 2; i++) bushM[i] = mesh(MeshBuilder.bush(i));
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
        double hh = Math.atan(th * aspect);
        double diag = Math.atan(Math.sqrt(th * th + Math.pow(th * aspect, 2)));
        halfDiagCos = (float) Math.cos(diag);
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

    // ------------------------------------------------------------------ terreno
    private void buildTerrain() {
        chunksA = World.NT / CH_A;   // 12
        chunksB = World.NZ / CH_B;   // 22
        int n = chunksA * chunksB;
        chunkVbo = new int[n];
        chunkCx = new float[n]; chunkCy = new float[n]; chunkCz = new float[n];
        final int va = CH_A + 1, vb = CH_B + 1;
        float[] buf = new float[va * vb * 9];
        for (int ca = 0; ca < chunksA; ca++) {
            for (int cb = 0; cb < chunksB; cb++) {
                int k = 0;
                float sx = 0, sy = 0, sz = 0;
                for (int b = 0; b < vb; b++) {
                    for (int a = 0; a < va; a++) {
                        int gi = ca * CH_A + a, gj = cb * CH_B + b;
                        float[] pos = posAt(gi, gj);
                        float[] pa = posAt(gi + 1, gj), pb = posAt(gi - 1, gj);
                        float[] pc = posAt(gi, gj + 1), pd = posAt(gi, gj - 1);
                        float t1x = pa[0] - pb[0], t1y = pa[1] - pb[1], t1z = pa[2] - pb[2];
                        float t2x = pc[0] - pd[0], t2y = pc[1] - pd[1], t2z = pc[2] - pd[2];
                        float nx = t1y * t2z - t1z * t2y, ny = t1z * t2x - t1x * t2z, nz = t1x * t2y - t1y * t2x;
                        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                        nx /= l; ny /= l; nz /= l;
                        if (nx * pos[0] + ny * pos[1] > 0f) { nx = -nx; ny = -ny; nz = -nz; }
                        float th = gi * World.TWO_PI / World.NT;
                        float z = gj * World.L / World.NZ;
                        buf[k++] = pos[0]; buf[k++] = pos[1]; buf[k++] = pos[2];
                        buf[k++] = nx; buf[k++] = ny; buf[k++] = nz;
                        buf[k++] = gi; buf[k++] = gj;
                        buf[k++] = World.pathAttr(th, z);
                        sx += pos[0]; sy += pos[1]; sz += pos[2];
                    }
                }
                int id = ca * chunksB + cb;
                chunkVbo[id] = vbo(buf);
                chunkCx[id] = sx / (va * vb); chunkCy[id] = sy / (va * vb); chunkCz[id] = sz / (va * vb);
            }
        }
        // indices compartidos
        short[] idx = new short[CH_A * CH_B * 6];
        int q = 0;
        for (int b = 0; b < CH_B; b++) {
            for (int a = 0; a < CH_A; a++) {
                int i0 = b * va + a, i1 = i0 + 1, i2 = i0 + va, i3 = i2 + 1;
                idx[q++] = (short) i0; idx[q++] = (short) i1; idx[q++] = (short) i2;
                idx[q++] = (short) i1; idx[q++] = (short) i3; idx[q++] = (short) i2;
            }
        }
        idxCount = idx.length;
        ShortBuffer sb = ByteBuffer.allocateDirect(idx.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        sb.put(idx).position(0);
        int[] id = new int[1];
        GLES20.glGenBuffers(1, id, 0);
        ibo = id[0];
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, idx.length * 2, sb, GLES20.GL_STATIC_DRAW);
    }

    private static float[] posAt(int gi, int gj) {
        int j = Math.max(0, Math.min(World.NZ, gj));
        float th = gi * World.TWO_PI / World.NT;
        float z = j * World.L / World.NZ;
        float rg = World.groundRadius(th, z);
        return new float[]{rg * (float) Math.cos(th), rg * (float) Math.sin(th), z};
    }

    private boolean visible(float cx, float cy, float cz, float radius, float maxDist) {
        float rx = cx - game.ex, ry = cy - game.ey, rz = cz - game.ez;
        float d = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (d - radius > maxDist) return false;
        if (d < radius + 1f) return true;
        float dot = (rx * game.dx + ry * game.dy + rz * game.dz) / d;
        double ang = Math.acos(Math.max(-1f, Math.min(1f, dot)));
        double lim = Math.acos(halfDiagCos) + Math.asin(Math.min(1f, radius / d));
        return ang < lim;
    }

    private void drawTerrain() {
        GLES20.glUseProgram(progTerrain);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(progTerrain, "uVP"), 1, false, vp, 0);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(progTerrain, "uCam"), game.ex, game.ey, game.ez);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(progTerrain, "uFog"), FOG_R, FOG_G, FOG_B);
        GLES20.glUniform2f(GLES20.glGetUniformLocation(progTerrain, "uFogRange"), FOG_START, FOG_END);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(progTerrain, "uLight"), 0.3f, 0.6f, 0.5f);
        int aPos = GLES20.glGetAttribLocation(progTerrain, "aPos");
        int aN = GLES20.glGetAttribLocation(progTerrain, "aNormal");
        int aUV = GLES20.glGetAttribLocation(progTerrain, "aUV");
        int aPath = GLES20.glGetAttribLocation(progTerrain, "aPath");
        GLES20.glEnableVertexAttribArray(aPos);
        GLES20.glEnableVertexAttribArray(aN);
        GLES20.glEnableVertexAttribArray(aUV);
        GLES20.glEnableVertexAttribArray(aPath);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo);
        for (int i = 0; i < chunkVbo.length; i++) {
            if (!visible(chunkCx[i], chunkCy[i], chunkCz[i], 52f, FAR)) continue;
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, chunkVbo[i]);
            GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 36, 0);
            GLES20.glVertexAttribPointer(aN, 3, GLES20.GL_FLOAT, false, 36, 12);
            GLES20.glVertexAttribPointer(aUV, 2, GLES20.GL_FLOAT, false, 36, 24);
            GLES20.glVertexAttribPointer(aPath, 1, GLES20.GL_FLOAT, false, 36, 32);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, idxCount, GLES20.GL_UNSIGNED_SHORT, 0);
        }
        GLES20.glDisableVertexAttribArray(aPos);
        GLES20.glDisableVertexAttribArray(aN);
        GLES20.glDisableVertexAttribArray(aUV);
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

    private void drawInstance(Mesh m, float[] model, float tint, float flash, float sxz, float sy) {
        GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0);
        GLES20.glUniform3f(uTint, tint, tint, tint);
        GLES20.glUniform1f(uFlash, flash);
        // contorno: casco invertido (caras delanteras fuera) un poco mas grande
        GLES20.glUniform1f(uOutline, 1f);
        GLES20.glUniform3f(uOutScale, sxz, sy, sxz);
        GLES20.glCullFace(GLES20.GL_FRONT);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, m.count);
        // relleno
        GLES20.glUniform1f(uOutline, 0f);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, m.count);
    }

    private final float[] enemyModel = new float[16];

    private void drawProps() {
        GLES20.glUseProgram(progProp);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glFrontFace(GLES20.GL_CCW);
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(progProp, "uVP"), 1, false, vp, 0);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(progProp, "uCam"), game.ex, game.ey, game.ez);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(progProp, "uFog"), FOG_R, FOG_G, FOG_B);
        GLES20.glUniform2f(GLES20.glGetUniformLocation(progProp, "uFogRange"), FOG_START, FOG_END);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(progProp, "uLight"), 0.3f, 0.6f, 0.5f);
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

        ArrayList<World.Prop> ps = game.world.props;
        // un tipo de malla a la vez para no cambiar de VBO a cada rato
        for (int pass = 0; pass < 8; pass++) {
            Mesh m;
            switch (pass) {
                case 0: m = houseM; break;
                case 1: case 2: case 3: case 4: m = treeM[pass - 1]; break;
                case 5: m = rockM; break;
                case 6: m = bushM[0]; break;
                default: m = bushM[1]; break;
            }
            bindMesh(m);
            for (int i = 0; i < ps.size(); i++) {
                World.Prop p = ps.get(i);
                boolean match;
                switch (pass) {
                    case 0: match = p.kind == World.HOUSE; break;
                    case 1: case 2: case 3: case 4: match = p.kind == World.TREE && p.variant == pass - 1; break;
                    case 5: match = p.kind == World.ROCK; break;
                    case 6: match = p.kind == World.BUSH && p.variant == 0; break;
                    default: match = p.kind == World.BUSH && p.variant == 1; break;
                }
                if (!match) continue;
                float rad = p.kind == World.HOUSE ? 8f * p.scale : 6f * p.scale;
                float cy = p.y, cx = p.x, cz = p.z;
                if (!visible(cx, cy, cz, rad, p.kind == World.BUSH ? 200f : FAR - 20f)) continue;
                float sxz = 1.07f, sy = 1.04f;
                drawInstance(m, p.model, 1f, 0f, sxz, sy);
            }
        }

        // enemigos
        bindMesh(enemyM);
        for (Game.Enemy e : game.enemies) {
            if (!e.alive) continue;
            if (!visible(e.x, e.y, e.z, 4f, FAR - 20f)) continue;
            float d = (float) Math.sqrt(e.x * e.x + e.y * e.y);
            float ux = -e.x / d, uy = -e.y / d;
            // base: Y=arriba, Z=hacia el jugador, X = Y x Z
            float zx = e.fx, zy = e.fy, zz = e.fz;
            float xx = uy * zz, xy = -ux * zz, xz = ux * zy - uy * zx;
            float xl = (float) Math.sqrt(xx * xx + xy * xy + xz * xz);
            if (xl < 1e-4f) continue;
            xx /= xl; xy /= xl; xz /= xl;
            float bob = (float) Math.sin(e.bob) * 0.15f + 0.15f;
            float[] m = enemyModel;
            m[0] = xx; m[1] = xy; m[2] = xz; m[3] = 0;
            m[4] = ux; m[5] = uy; m[6] = 0; m[7] = 0;
            m[8] = zx; m[9] = zy; m[10] = zz; m[11] = 0;
            m[12] = e.x + ux * bob; m[13] = e.y + uy * bob; m[14] = e.z; m[15] = 1;
            drawInstance(enemyM, m, 1f, e.hurt > 0 ? 0.7f : 0f, 1.08f, 1.06f);
        }

        GLES20.glDisableVertexAttribArray(aPos);
        GLES20.glDisableVertexAttribArray(aNormal);
        GLES20.glDisableVertexAttribArray(aColor);
    }
}

package com.valleradial;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Dibuja el mundo esferico: terreno "cube-sphere" por trozos (shader toon con pinceladas 3D y sombra de contacto),
 * matas de pasto con flores, casas, torres, arcos, molinos con aspas que giran, ruinas, arboles que se mecen,
 * rocas y enemigos. Los objetos tienen version detallada (cerca) y simple (lejos).
 */
final class GLRenderer implements GLSurfaceView.Renderer {
    private static final float FOG_R = 0.97f, FOG_G = 0.88f, FOG_B = 0.62f;
    private static final float FOG_START = 120f, FOG_END = 420f, FAR = 490f;
    private static final float FOV = 72f;
    private static final float LOD_DIST = 100f;      // detallado hasta aqui
    private static final float GRASS_DIST = 135f;

    private final Game game;

    private int progTerrain, progProp;
    // terreno: 6 caras x 7x7 trozos de 16x16 celdas
    private static final int FACE_N = 112, CH = 16, CPF = FACE_N / CH;
    private int[] chunkVbo, grassVbo, grassCount;
    private float[] chunkCx, chunkCy, chunkCz;
    private int ibo, idxCount;
    // mallas de objetos: indice = tipo*4 + variante; [0]=detallada, [1]=simple
    private final Mesh[][] lod = new Mesh[8 * 4][2];
    @SuppressWarnings("unchecked")
    private final ArrayList<World.Prop>[] byKey = new ArrayList[8 * 4];
    private Mesh enemyM, bladesM;

    private final float[] proj = new float[16], view = new float[16], vp = new float[16];
    private final float[] tmpModel = new float[16];
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
            "attribute vec3 aPos; attribute vec3 aNormal; attribute vec2 aPA;\n" +
            "varying vec3 vN; varying vec3 vW; varying vec2 vPA; varying float vDist;\n" +
            "void main(){ vN=aNormal; vW=aPos; vPA=aPA; vDist=length(uCam-aPos);\n" +
            " gl_Position=uVP*vec4(aPos,1.0); }";

    private static final String TERRAIN_FS =
            "#ifdef GL_FRAGMENT_PRECISION_HIGH\nprecision highp float;\n#else\nprecision mediump float;\n#endif\n" +
            "uniform vec3 uFog; uniform vec2 uFogRange; uniform vec3 uLight;\n" +
            "varying vec3 vN; varying vec3 vW; varying vec2 vPA; varying float vDist;\n" +
            "float h31(vec3 p){ return fract(sin(dot(p, vec3(127.1,311.7,74.7)))*43758.5453); }\n" +
            "float vn(vec3 p){ vec3 i=floor(p); vec3 f=fract(p); f=f*f*(3.0-2.0*f);\n" +
            " float a=h31(i), b=h31(i+vec3(1.,0.,0.)), c=h31(i+vec3(0.,1.,0.)), d=h31(i+vec3(1.,1.,0.));\n" +
            " float e=h31(i+vec3(0.,0.,1.)), g=h31(i+vec3(1.,0.,1.)), h=h31(i+vec3(0.,1.,1.)), k=h31(i+vec3(1.,1.,1.));\n" +
            " return mix(mix(mix(a,b,f.x), mix(c,d,f.x), f.y), mix(mix(e,g,f.x), mix(h,k,f.x), f.y), f.z); }\n" +
            "void main(){\n" +
            " float big=vn(vW/16.0);\n" +
            " float mid=vn(vW/5.5+7.3);\n" +
            " float fine=vn(vW/2.1+3.1);\n" +
            " float tiny=vn(vW*1.35+17.0);\n" +
            " vec3 c=mix(vec3(0.93,0.78,0.30), vec3(0.84,0.66,0.22), step(0.5, fine*0.6+mid*0.4));\n" +
            " c=mix(c, vec3(0.99,0.88,0.45), step(0.74, tiny)*0.55);\n" +          // briznas claras
            " float g=step(0.62, big*0.85+mid*0.25);\n" +
            " vec3 green=mix(vec3(0.60,0.68,0.22), vec3(0.50,0.60,0.20), step(0.5,fine));\n" +
            " green=mix(green, vec3(0.40,0.52,0.16), step(0.7,tiny)*0.7);\n" +
            " c=mix(c, green, g);\n" +
            " float pk=step(0.74, mid*0.6+fine*0.5)*(1.0-g);\n" +
            " c=mix(c, vec3(0.92,0.40,0.46), pk);\n" +
            // florecitas redondas sobre el pasto
            " vec3 cell=floor(vW*2.0); vec3 fp=fract(vW*2.0)-0.5;\n" +
            " float fh=h31(cell+5.0);\n" +
            " float flower=step(0.93,fh)*step(length(fp),0.24)*max(g,0.35);\n" +
            " vec3 fcol=fh>0.975 ? vec3(0.98,0.96,0.88) : (fh>0.955 ? vec3(0.98,0.84,0.32) : vec3(0.95,0.45,0.58));\n" +
            " c=mix(c, fcol, flower);\n" +
            " float pv=vPA.x+(fine-0.5)*0.35;\n" +
            " float onPath=step(0.5,pv);\n" +
            " vec3 pathc=mix(vec3(0.97,0.90,0.78), vec3(0.86,0.76,0.60), step(0.55,tiny));\n" +
            " c=mix(c, pathc, onPath);\n" +
            // bordes oscuros del camino (como tinta) y pedregullo
            " float edge=step(0.5,pv)*(1.0-step(0.62,pv));\n" +
            " c=mix(c, vec3(0.55,0.44,0.28), edge*0.8);\n" +
            // variacion de brillo por pinceladas + sombra de contacto
            " c*=0.90+0.2*fine+0.05*(mid-0.5);\n" +
            " c*=mix(0.58, 1.0, vPA.y);\n" +
            " float ndl=dot(normalize(vN), normalize(uLight));\n" +
            " float band=ndl>0.2 ? 1.0 : (ndl>-0.25 ? 0.93 : 0.85);\n" +
            " float f=smoothstep(uFogRange.x, uFogRange.y, vDist);\n" +
            " gl_FragColor=vec4(mix(c*band, uFog, f), 1.0); }";

    private static final String PROP_VS =
            "uniform mat4 uVP; uniform mat4 uModel; uniform vec3 uCam; uniform vec3 uOutScale; uniform float uOutline;\n" +
            "uniform float uTime; uniform vec3 uWind; uniform float uSwayAmp; uniform float uAO;\n" +
            "attribute vec3 aPos; attribute vec3 aNormal; attribute vec3 aColor; attribute float aSway;\n" +
            "varying vec3 vN; varying vec3 vC; varying float vDist; varying float vH;\n" +
            "void main(){ vec3 lp = (uOutline>0.5) ? aPos*uOutScale : aPos;\n" +
            " vec4 w = uModel*vec4(lp,1.0); vN=mat3(uModel)*aNormal; vC=aColor; vH=(uAO>0.5)?aPos.y:3.0;\n" +
            " float ph = uTime*1.7 + dot(w.xyz, vec3(0.071,0.083,0.059));\n" +
            " float gust = sin(ph) + 0.5*sin(ph*2.3+1.7);\n" +
            " w.xyz += uWind*(gust*aSway*uSwayAmp);\n" +
            " vDist=length(uCam-w.xyz);\n" +
            " gl_Position=uVP*w; }";

    private static final String PROP_FS =
            "precision mediump float;\n" +
            "uniform vec3 uFog; uniform vec2 uFogRange; uniform vec3 uLight; uniform vec3 uTint; uniform float uOutline; uniform float uFlash;\n" +
            "varying vec3 vN; varying vec3 vC; varying float vDist; varying float vH;\n" +
            "void main(){\n" +
            " vec3 c;\n" +
            " if(uOutline>0.5){ c=vec3(0.07,0.05,0.06); }\n" +
            " else { float ndl=dot(normalize(vN), normalize(uLight));\n" +
            "  float band = ndl>0.45 ? 1.0 : (ndl>0.1 ? 0.9 : (ndl>-0.25 ? 0.79 : 0.68));\n" +
            "  float ao = mix(0.72, 1.0, clamp(vH*0.5+0.35, 0.0, 1.0));\n" +   // mas oscuro cerca del suelo
            "  c = vC*uTint*band*ao; c=mix(c, vec3(1.0), uFlash); }\n" +
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
        m.count = a.length / MeshBuilder.STRIDE;
        return m;
    }

    // ------------------------------------------------------------------ GL callbacks
    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        progTerrain = program(TERRAIN_VS, TERRAIN_FS);
        progProp = program(PROP_VS, PROP_FS);
        buildTerrain();

        for (int v = 0; v < 4; v++) {
            lod[World.HOUSE * 4 + v][0] = mesh(MeshBuilder.house(v));
            lod[World.HOUSE * 4 + v][1] = mesh(MeshBuilder.houseSimple(v));
            lod[World.TREE * 4 + v][0] = mesh(MeshBuilder.tree(v));
            lod[World.TREE * 4 + v][1] = mesh(MeshBuilder.treeSimple(v));
        }
        lod[World.ROCK * 4][0] = mesh(MeshBuilder.rock());
        lod[World.ROCK * 4][1] = mesh(MeshBuilder.rockSimple());
        for (int v = 0; v < 2; v++) {
            lod[World.BUSH * 4 + v][0] = mesh(MeshBuilder.bush(v));
            lod[World.BUSH * 4 + v][1] = mesh(MeshBuilder.bushSimple(v));
        }
        lod[World.TOWER * 4][0] = mesh(MeshBuilder.tower());
        lod[World.TOWER * 4][1] = mesh(MeshBuilder.towerSimple());
        lod[World.ARCH * 4][0] = mesh(MeshBuilder.arch());
        lod[World.ARCH * 4][1] = mesh(MeshBuilder.archSimple());
        lod[World.WINDMILL * 4][0] = mesh(MeshBuilder.windmillBody());
        lod[World.WINDMILL * 4][1] = mesh(MeshBuilder.windmillSimple());
        lod[World.RUIN * 4][0] = mesh(MeshBuilder.ruin());
        lod[World.RUIN * 4][1] = mesh(MeshBuilder.ruinSimple());
        bladesM = mesh(MeshBuilder.windmillBlades());
        enemyM = mesh(MeshBuilder.enemy());

        for (int i = 0; i < byKey.length; i++) byKey[i] = new ArrayList<>();
        for (World.Prop p : game.world.props) {
            int key = p.kind * 4 + (p.variant & 3);
            if (lod[key][0] != null) byKey[key].add(p);
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
    private static void dirAtD(int face, double i, double j, float[] out) {
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
        dirAtD(face, i, j, out);
        float rg = World.groundRadius(out[0], out[1], out[2]);
        out[0] *= rg; out[1] *= rg; out[2] *= rg;
    }

    // ----- sombra de contacto: oscurece el suelo bajo copas, casas, rocas...
    private static final float CELL = 24f;

    private static long cellKey(int x, int y, int z) {
        return ((long) (x + 64) << 40) | ((long) (y + 64) << 20) | (long) (z + 64);
    }

    private static float shadowRadius(World.Prop p) {
        switch (p.kind) {
            case World.HOUSE: return 6.5f * p.scale;
            case World.TOWER: return 5.5f * p.scale;
            case World.WINDMILL: return 5.5f * p.scale;
            case World.TREE: return 3.6f * p.scale;
            case World.ROCK: return 2.4f * p.scale;
            case World.ARCH: return 6.5f * p.scale;
            case World.RUIN: return 5.5f * p.scale;
            default: return 1.8f * p.scale;
        }
    }

    private HashMap<Long, ArrayList<World.Prop>> propGrid() {
        HashMap<Long, ArrayList<World.Prop>> grid = new HashMap<>();
        for (World.Prop p : game.world.props) {
            long k = cellKey((int) Math.floor(p.x / CELL), (int) Math.floor(p.y / CELL), (int) Math.floor(p.z / CELL));
            ArrayList<World.Prop> l = grid.get(k);
            if (l == null) { l = new ArrayList<>(); grid.put(k, l); }
            l.add(p);
        }
        return grid;
    }

    private static float aoAt(HashMap<Long, ArrayList<World.Prop>> grid, float x, float y, float z) {
        int cx = (int) Math.floor(x / CELL), cy = (int) Math.floor(y / CELL), cz = (int) Math.floor(z / CELL);
        float ao = 1f;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    ArrayList<World.Prop> l = grid.get(cellKey(cx + dx, cy + dy, cz + dz));
                    if (l == null) continue;
                    for (int i = 0; i < l.size(); i++) {
                        World.Prop p = l.get(i);
                        float r = shadowRadius(p);
                        float ex = p.off > 0f ? p.off : 0f;
                        float qx = x - p.x, qy = y - p.y, qz = z - p.z;
                        float d = (float) Math.sqrt(qx * qx + qy * qy + qz * qz);
                        float lim = (r + ex) * 1.9f;
                        if (d > lim) continue;
                        float dd = Math.max(0f, d - ex);
                        float t = World.smooth(r * 0.35f, r * 1.9f, dd);
                        float a = 0.50f + 0.50f * t;
                        if (a < ao) ao = a;
                    }
                }
            }
        }
        return ao;
    }

    private void buildTerrain() {
        int n = 6 * CPF * CPF;
        chunkVbo = new int[n]; grassVbo = new int[n]; grassCount = new int[n];
        chunkCx = new float[n]; chunkCy = new float[n]; chunkCz = new float[n];
        final int v = CH + 1;
        float[] buf = new float[v * v * 8];
        float[] d = new float[3], p0 = new float[3], pa = new float[3], pb = new float[3], pc = new float[3], pd = new float[3];
        HashMap<Long, ArrayList<World.Prop>> grid = propGrid();
        Random rnd = new Random(2024);
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
                            dirAtD(face, gi, gj, d);
                            buf[k++] = p0[0]; buf[k++] = p0[1]; buf[k++] = p0[2];
                            buf[k++] = nx; buf[k++] = ny; buf[k++] = nz;
                            buf[k++] = World.pathAttr(d[0], d[1], d[2]);
                            buf[k++] = aoAt(grid, p0[0], p0[1], p0[2]);
                            sx += p0[0]; sy += p0[1]; sz += p0[2];
                        }
                    }
                    chunkVbo[id] = vbo(buf);
                    chunkCx[id] = sx / (v * v); chunkCy[id] = sy / (v * v); chunkCz[id] = sz / (v * v);

                    // matas de pasto con flores para este trozo
                    MeshBuilder gb = new MeshBuilder();
                    for (int t = 0; t < 120; t++) {
                        double fi = ci * CH + rnd.nextDouble() * CH, fj = cj * CH + rnd.nextDouble() * CH;
                        dirAtD(face, fi, fj, d);
                        if (World.pathAttr(d[0], d[1], d[2]) > 0.45f) continue;
                        float rg = World.groundRadius(d[0], d[1], d[2]);
                        gb.tuft(d[0] * rg, d[1] * rg, d[2] * rg, -d[0], -d[1], -d[2], rnd);
                    }
                    float[] ga = gb.toArray();
                    grassCount[id] = ga.length / MeshBuilder.STRIDE;
                    grassVbo[id] = grassCount[id] > 0 ? vbo(ga) : 0;
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
        // luz relativa a la camara; la normal del terreno apunta al centro, igual que "arriba"
        GLES20.glUniform3f(GLES20.glGetUniformLocation(progTerrain, "uLight"),
                game.ux * 0.6f + game.dx * 0.3f, game.uy * 0.6f + game.dy * 0.3f, game.uz * 0.6f + game.dz * 0.3f);
        int aPos = GLES20.glGetAttribLocation(progTerrain, "aPos");
        int aN = GLES20.glGetAttribLocation(progTerrain, "aNormal");
        int aPA = GLES20.glGetAttribLocation(progTerrain, "aPA");
        GLES20.glEnableVertexAttribArray(aPos);
        GLES20.glEnableVertexAttribArray(aN);
        GLES20.glEnableVertexAttribArray(aPA);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo);
        for (int i = 0; i < chunkVbo.length; i++) {
            if (!visible(chunkCx[i], chunkCy[i], chunkCz[i], 50f, FAR)) continue;
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, chunkVbo[i]);
            GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 32, 0);
            GLES20.glVertexAttribPointer(aN, 3, GLES20.GL_FLOAT, false, 32, 12);
            GLES20.glVertexAttribPointer(aPA, 2, GLES20.GL_FLOAT, false, 32, 24);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, idxCount, GLES20.GL_UNSIGNED_SHORT, 0);
        }
        GLES20.glDisableVertexAttribArray(aPos);
        GLES20.glDisableVertexAttribArray(aN);
        GLES20.glDisableVertexAttribArray(aPA);
    }

    // ------------------------------------------------------------------ objetos
    private int uModel, uOutline, uOutScale, uTint, uFlash, uSwayAmp, uAO, aPos, aNormal, aColor, aSway;

    private void bindMesh(Mesh m) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, m.vbo);
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 40, 0);
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 40, 12);
        GLES20.glVertexAttribPointer(aColor, 3, GLES20.GL_FLOAT, false, 40, 24);
        GLES20.glVertexAttribPointer(aSway, 1, GLES20.GL_FLOAT, false, 40, 36);
    }

    private void drawInstance(Mesh m, float[] model, float flash, boolean outline, float tint, float amp) {
        GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0);
        GLES20.glUniform3f(uTint, tint, tint, tint * 0.99f);
        GLES20.glUniform1f(uFlash, flash);
        GLES20.glUniform1f(uSwayAmp, amp);
        if (outline) {
            // contorno: casco invertido (caras delanteras fuera) un poco mas grande
            GLES20.glUniform1f(uOutline, 1f);
            GLES20.glUniform3f(uOutScale, 1.05f, 1.03f, 1.05f);
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
            case World.TOWER: return 18f * p.scale;
            case World.WINDMILL: return 13f * p.scale;
            case World.HOUSE: return 10f * p.scale;
            case World.ARCH: return 10f * p.scale;
            case World.RUIN: return 7f * p.scale;
            case World.TREE: return 9f * p.scale;
            case World.ROCK: return 3.5f * p.scale;
            default: return 3f * p.scale;
        }
    }

    private static float maxDistOf(int kind) {
        switch (kind) {
            case World.BUSH: return 140f;
            case World.ROCK: return 230f;
            case World.RUIN: return 260f;
            case World.TREE: return 300f;
            default: return FAR;
        }
    }

    private static float swayOf(int kind) {
        switch (kind) {
            case World.TREE: return 0.55f;
            case World.BUSH: return 0.22f;
            case World.HOUSE: return 0.7f;     // humo
            case World.TOWER: return 1.3f;     // bandera
            case World.ARCH: return 0.4f;      // hiedra
            case World.RUIN: return 0.3f;
            default: return 0f;
        }
    }

    private static float tintOf(World.Prop p) {
        float h = (float) Math.sin(p.x * 0.137f + p.y * 0.291f + p.z * 0.173f) * 43758.55f;
        h = h - (float) Math.floor(h);
        float k = (p.kind == World.TREE || p.kind == World.BUSH) ? 0.26f : 0.12f;
        return 1f - k * 0.5f + k * h;
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
        // viento: sopla hacia la derecha de la camara y un poco hacia adelante
        float rx = game.dy * game.uz - game.dz * game.uy, ry = game.dz * game.ux - game.dx * game.uz,
                rz = game.dx * game.uy - game.dy * game.ux;
        GLES20.glUniform3f(GLES20.glGetUniformLocation(progProp, "uWind"),
                rx * 0.8f + game.dx * 0.25f, ry * 0.8f + game.dy * 0.25f, rz * 0.8f + game.dz * 0.25f);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(progProp, "uTime"), game.time);
        uModel = GLES20.glGetUniformLocation(progProp, "uModel");
        uOutline = GLES20.glGetUniformLocation(progProp, "uOutline");
        uOutScale = GLES20.glGetUniformLocation(progProp, "uOutScale");
        uTint = GLES20.glGetUniformLocation(progProp, "uTint");
        uFlash = GLES20.glGetUniformLocation(progProp, "uFlash");
        uSwayAmp = GLES20.glGetUniformLocation(progProp, "uSwayAmp");
        uAO = GLES20.glGetUniformLocation(progProp, "uAO");
        GLES20.glUniform1f(uAO, 1f);
        aPos = GLES20.glGetAttribLocation(progProp, "aPos");
        aNormal = GLES20.glGetAttribLocation(progProp, "aNormal");
        aColor = GLES20.glGetAttribLocation(progProp, "aColor");
        aSway = GLES20.glGetAttribLocation(progProp, "aSway");
        GLES20.glEnableVertexAttribArray(aPos);
        GLES20.glEnableVertexAttribArray(aNormal);
        GLES20.glEnableVertexAttribArray(aColor);
        GLES20.glEnableVertexAttribArray(aSway);

        float ld2 = LOD_DIST * LOD_DIST;
        for (int key = 0; key < byKey.length; key++) {
            ArrayList<World.Prop> list = byKey[key];
            if (list.isEmpty()) continue;
            int kind = key / 4;
            float maxD = maxDistOf(kind);
            float amp = swayOf(kind);
            for (int pass = 0; pass < 2; pass++) {
                Mesh m = lod[key][pass];
                bindMesh(m);
                for (int i = 0; i < list.size(); i++) {
                    World.Prop p = list.get(i);
                    float qx = p.x - game.ex, qy = p.y - game.ey, qz = p.z - game.ez;
                    float d2 = qx * qx + qy * qy + qz * qz;
                    boolean near = d2 < ld2;
                    if ((pass == 0) != near) continue;
                    if (!visible(p.x, p.y, p.z, radiusOf(p), maxD)) continue;
                    boolean outline = near ? d2 < 75f * 75f : d2 < 160f * 160f;
                    drawInstance(m, p.model, 0f, outline, tintOf(p), near ? amp : amp * 0.8f);
                    if (pass == 0 && kind == World.WINDMILL) {
                        // aspas girando sobre el eje del molino
                        System.arraycopy(p.model, 0, tmpModel, 0, 16);
                        Matrix.translateM(tmpModel, 0, 0f, 8.0f, 3.6f);
                        Matrix.rotateM(tmpModel, 0, -(game.time * 38f + p.yaw * 57f), 0f, 0f, 1f);
                        bindMesh(bladesM);
                        GLES20.glUniform1f(uAO, 0f);
                        drawInstance(bladesM, tmpModel, 0f, true, 1f, 0f);
                        GLES20.glUniform1f(uAO, 1f);
                        bindMesh(m);
                    }
                }
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
            drawInstance(enemyM, m, e.hurt > 0 ? 0.7f : 0f, true, 1f, 0f);
        }

        // pasto y flores (solo cerca, se mecen con el viento)
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glUniform1f(uAO, 0f);
        GLES20.glUniform1f(uOutline, 0f);
        GLES20.glUniform3f(uTint, 1f, 1f, 1f);
        GLES20.glUniform1f(uFlash, 0f);
        GLES20.glUniform1f(uSwayAmp, 0.35f);
        System.arraycopy(IDENTITY, 0, tmpModel, 0, 16);
        GLES20.glUniformMatrix4fv(uModel, 1, false, tmpModel, 0);
        for (int i = 0; i < grassVbo.length; i++) {
            if (grassCount[i] == 0) continue;
            if (!visible(chunkCx[i], chunkCy[i], chunkCz[i], 50f, GRASS_DIST)) continue;
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, grassVbo[i]);
            GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 40, 0);
            GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 40, 12);
            GLES20.glVertexAttribPointer(aColor, 3, GLES20.GL_FLOAT, false, 40, 24);
            GLES20.glVertexAttribPointer(aSway, 1, GLES20.GL_FLOAT, false, 40, 36);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, grassCount[i]);
        }
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        GLES20.glDisableVertexAttribArray(aPos);
        GLES20.glDisableVertexAttribArray(aNormal);
        GLES20.glDisableVertexAttribArray(aColor);
        GLES20.glDisableVertexAttribArray(aSway);
    }

    private static final float[] IDENTITY = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
}

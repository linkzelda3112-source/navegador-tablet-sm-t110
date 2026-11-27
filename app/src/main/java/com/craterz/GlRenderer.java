package com.craterz;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Render 3D con OpenGL ES 2.0: normales, sombras de la luna, niebla, lluvia, partículas y relámpagos. */
public final class GlRenderer implements GLSurfaceView.Renderer {
    private static final int FS = MeshBuilder.STRIDE * 4;

    private static final String MAIN_VS =
            "uniform mat4 uVP; uniform mat4 uModel; uniform mat4 uShadowVP;\n" +
            "attribute vec3 aPos; attribute vec3 aNrm; attribute vec3 aTan; attribute vec2 aUV; attribute vec3 aCol; attribute float aWet;\n" +
            "varying vec3 vWorld; varying vec3 vN; varying vec3 vT; varying vec2 vUV; varying vec3 vCol; varying float vWet; varying vec4 vSh;\n" +
            "void main(){\n" +
            "  vec4 w = uModel * vec4(aPos,1.0);\n" +
            "  vWorld = w.xyz;\n" +
            "  mat3 m3 = mat3(uModel[0].xyz, uModel[1].xyz, uModel[2].xyz);\n" +
            "  vN = m3 * aNrm; vT = m3 * aTan;\n" +
            "  vUV = aUV; vCol = aCol; vWet = aWet;\n" +
            "  vSh = uShadowVP * w;\n" +
            "  gl_Position = uVP * w;\n" +
            "}\n";

    private static final String MAIN_FS =
            "#ifdef GL_FRAGMENT_PRECISION_HIGH\nprecision highp float;\n#else\nprecision mediump float;\n#endif\n" +
            "uniform sampler2D uTex; uniform sampler2D uNrm; uniform sampler2D uShadow;\n" +
            "uniform vec3 uCam; uniform vec3 uSunDir; uniform vec3 uSunCol; uniform vec3 uSkyAmb; uniform vec3 uGndAmb;\n" +
            "uniform vec3 uFogCol; uniform float uFogDen; uniform vec4 uLP[5]; uniform vec3 uLC[5];\n" +
            "uniform float uWetG; uniform vec3 uTint; uniform float uHit; uniform float uShadowOn; uniform float uTexel;\n" +
            "varying vec3 vWorld; varying vec3 vN; varying vec3 vT; varying vec2 vUV; varying vec3 vCol; varying float vWet; varying vec4 vSh;\n" +
            "float unpackD(vec4 c){ return dot(c, vec4(1.0, 1.0/255.0, 1.0/65025.0, 1.0/16581375.0)); }\n" +
            "float shadowAt(float ndl){\n" +
            "  if (uShadowOn < 0.5) return 1.0;\n" +
            "  vec3 p = vSh.xyz / vSh.w * 0.5 + 0.5;\n" +
            "  if (p.x < 0.0 || p.x > 1.0 || p.y < 0.0 || p.y > 1.0 || p.z > 1.0) return 1.0;\n" +
            "  float bias = 0.0016 + 0.0035 * (1.0 - ndl);\n" +
            "  float s = 0.0; float o = uTexel * 0.8;\n" +
            "  s += step(p.z - bias, unpackD(texture2D(uShadow, p.xy + vec2(-o, -o))));\n" +
            "  s += step(p.z - bias, unpackD(texture2D(uShadow, p.xy + vec2( o, -o))));\n" +
            "  s += step(p.z - bias, unpackD(texture2D(uShadow, p.xy + vec2(-o,  o))));\n" +
            "  s += step(p.z - bias, unpackD(texture2D(uShadow, p.xy + vec2( o,  o))));\n" +
            "  s *= 0.25;\n" +
            "  float e = max(abs(p.x - 0.5), abs(p.y - 0.5)) * 2.0;\n" +
            "  return mix(s, 1.0, smoothstep(0.82, 1.0, e));\n" +
            "}\n" +
            "void main(){\n" +
            "  vec4 tc = texture2D(uTex, vUV);\n" +
            "  vec3 emis = max(vCol - 1.0, 0.0);\n" +
            "  vec3 alb = tc.rgb * min(vCol, vec3(1.0)) * uTint; alb = alb * alb;\n" +
            "  vec4 nm = texture2D(uNrm, vUV);\n" +
            "  vec3 tn = nm.xyz * 2.0 - 1.0;\n" +
            "  vec3 Ng = normalize(vN);\n" +
            "  vec3 T = normalize(vT - Ng * dot(Ng, vT));\n" +
            "  vec3 B = cross(Ng, T);\n" +
            "  vec3 N = normalize(T * tn.x + B * tn.y + Ng * tn.z);\n" +
            "  vec3 V = normalize(uCam - vWorld);\n" +
            "  float wet = clamp(vWet + uWetG * 0.3, 0.0, 1.0);\n" +
            "  float specAmt = nm.a * 0.55 + wet * 0.35;\n" +
            "  float gloss = 10.0 + 70.0 * (nm.a + wet * 0.6);\n" +
            "  alb *= (1.0 - wet * 0.28);\n" +
            "  float ndlG = max(dot(Ng, uSunDir), 0.0);\n" +
            "  float ndl = max(dot(N, uSunDir), 0.0);\n" +
            "  float sh = shadowAt(ndlG) * smoothstep(0.0, 0.2, ndlG);\n" +
            "  vec3 col = alb * uSunCol * ndl * sh;\n" +
            "  vec3 H = normalize(uSunDir + V);\n" +
            "  col += uSunCol * sh * specAmt * pow(max(dot(N, H), 0.0), gloss);\n" +
            "  float hemi = N.y * 0.5 + 0.5;\n" +
            "  float ao = 0.55 + 0.45 * clamp(vWorld.y * 0.5 + 0.8, 0.0, 1.0);\n" +
            "  col += alb * mix(uGndAmb, uSkyAmb, hemi) * ao;\n" +
            "  for (int i = 0; i < 5; i++) {\n" +
            "    vec3 L = uLP[i].xyz - vWorld; float d = length(L); L /= max(d, 0.001);\n" +
            "    float att = clamp(1.0 - d / uLP[i].w, 0.0, 1.0); att *= att;\n" +
            "    float nd = max(dot(N, L), 0.0) * 0.85 + 0.15 * max(dot(Ng, L), 0.0);\n" +
            "    vec3 Hl = normalize(L + V);\n" +
            "    col += (alb * nd + specAmt * pow(max(dot(N, Hl), 0.0), gloss) * nd) * uLC[i] * att;\n" +
            "  }\n" +
            "  col += emis * (tc.rgb * tc.rgb * 0.8 + 0.2);\n" +
            "  col += vec3(0.7, 0.05, 0.02) * uHit;\n" +
            "  float dist = length(uCam - vWorld);\n" +
            "  float fd = uFogDen * (1.0 + clamp((2.0 - vWorld.y) * 0.18, 0.0, 1.2));\n" +
            "  float f = 1.0 - exp(-dist * fd);\n" +
            "  col = mix(col, uFogCol, clamp(f, 0.0, 0.97));\n" +
            "  col = 1.0 - exp(-col * 1.35);\n" +
            "  col = sqrt(col);\n" +
            "  gl_FragColor = vec4(col, 1.0);\n" +
            "}\n";

    private static final String DEPTH_VS =
            "uniform mat4 uLVP; uniform mat4 uModel; attribute vec3 aPos;\n" +
            "void main(){ gl_Position = uLVP * (uModel * vec4(aPos, 1.0)); }\n";
    private static final String DEPTH_FS =
            "#ifdef GL_FRAGMENT_PRECISION_HIGH\nprecision highp float;\n#else\nprecision mediump float;\n#endif\n" +
            "void main(){\n" +
            "  float d = gl_FragCoord.z;\n" +
            "  vec4 enc = fract(d * vec4(1.0, 255.0, 65025.0, 16581375.0));\n" +
            "  enc -= enc.yzww * vec4(1.0/255.0, 1.0/255.0, 1.0/255.0, 0.0);\n" +
            "  gl_FragColor = enc;\n" +
            "}\n";

    private static final String SKY_VS =
            "attribute vec2 aP; uniform vec3 uFwd; uniform vec3 uRight; uniform vec3 uUp; uniform float uTanY; uniform float uAspect;\n" +
            "varying vec3 vDir;\n" +
            "void main(){ vDir = uFwd + uRight * (aP.x * uTanY * uAspect) + uUp * (aP.y * uTanY); gl_Position = vec4(aP, 0.9999, 1.0); }\n";
    private static final String SKY_FS =
            "precision highp float;\n" +
            "uniform sampler2D uCloud; uniform vec3 uMoon; uniform float uTime; uniform float uLight; uniform vec3 uTop; uniform vec3 uHor;\n" +
            "varying vec3 vDir;\n" +
            "float hash(vec2 p){ return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }\n" +
            "void main(){\n" +
            "  vec3 d = normalize(vDir);\n" +
            "  float h = clamp(d.y, 0.0, 1.0);\n" +
            "  vec3 col = mix(uHor, uTop, pow(h, 0.5));\n" +
            "  float md = dot(d, uMoon);\n" +
            "  float disc = smoothstep(0.9988, 0.9994, md);\n" +
            "  float cl = 0.0;\n" +
            "  if (d.y > 0.01) {\n" +
            "    vec2 uv = d.xz / (d.y + 0.28) * 0.9 + vec2(uTime * 0.006, uTime * 0.0025);\n" +
            "    float c1 = texture2D(uCloud, uv).r; float c2 = texture2D(uCloud, uv * 2.7 + 0.37).r;\n" +
            "    cl = smoothstep(0.40, 0.78, c1 * 0.65 + c2 * 0.35) * smoothstep(0.01, 0.22, d.y);\n" +
            "    vec3 ccol = vec3(0.045, 0.06, 0.085) + vec3(0.22, 0.27, 0.36) * pow(max(md, 0.0), 6.0) + vec3(0.5, 0.6, 0.9) * uLight * 0.7;\n" +
            "    col = mix(col, ccol, cl * 0.92);\n" +
            "  }\n" +
            "  col += vec3(0.20, 0.27, 0.40) * pow(max(md, 0.0), 40.0) * 0.8 * (1.0 - cl * 0.7);\n" +
            "  col += vec3(0.9, 0.96, 1.0) * disc * 1.6 * (1.0 - cl * 0.85);\n" +
            "  vec2 sp = d.xz / (abs(d.y) + 0.25) * 110.0;\n" +
            "  float st = step(0.9972, hash(floor(sp))) * (1.0 - cl) * smoothstep(0.1, 0.5, d.y);\n" +
            "  col += vec3(0.8, 0.85, 1.0) * st * 0.7;\n" +
            "  col += vec3(0.5, 0.6, 0.9) * uLight * (0.35 + 0.5 * h);\n" +
            "  gl_FragColor = vec4(col, 1.0);\n" +
            "}\n";

    private static final String PART_VS =
            "uniform mat4 uVP; attribute vec3 aPos; attribute vec2 aUV; attribute vec4 aCol;\n" +
            "varying vec2 vUV; varying vec4 vCol; varying float vD;\n" +
            "void main(){ vUV = aUV; vCol = aCol; vec4 p = uVP * vec4(aPos, 1.0); vD = p.w; gl_Position = p; }\n";
    private static final String PART_FS =
            "precision mediump float;\n" +
            "uniform sampler2D uTex; uniform vec3 uLit; uniform float uAdd; uniform float uFogDen;\n" +
            "varying vec2 vUV; varying vec4 vCol; varying float vD;\n" +
            "void main(){\n" +
            "  vec4 t = texture2D(uTex, vUV);\n" +
            "  vec3 c = vCol.rgb * mix(uLit, vec3(1.0), uAdd);\n" +
            "  float fog = exp(-vD * uFogDen * (uAdd > 0.5 ? 0.35 : 0.0));\n" +
            "  gl_FragColor = vec4(c, t.a * vCol.a * fog);\n" +
            "}\n";

    private static final String POST_VS =
            "attribute vec2 aP; varying vec2 vP; void main(){ vP = aP; gl_Position = vec4(aP, 0.0, 1.0); }\n";
    private static final String POST_FS =
            "precision highp float;\n" +
            "uniform float uTime; uniform float uDmg; uniform float uLow; uniform float uFlash; uniform float uAspect;\n" +
            "varying vec2 vP;\n" +
            "float hash(vec2 p){ return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }\n" +
            "void main(){\n" +
            "  vec2 q = vP * vec2(uAspect * 0.62, 1.0);\n" +
            "  float r = length(q);\n" +
            "  float vig = smoothstep(0.62, 1.55, r);\n" +
            "  vec3 col = vec3(0.0);\n" +
            "  float a = vig * 0.62;\n" +
            "  float dm = uDmg * smoothstep(0.2, 1.35, r);\n" +
            "  col = mix(col, vec3(0.55, 0.0, 0.0), clamp(dm * 1.2, 0.0, 1.0));\n" +
            "  a = max(a, dm * 0.9);\n" +
            "  float pulse = uLow * (0.5 + 0.5 * sin(uTime * 5.0)) * smoothstep(0.1, 1.3, r);\n" +
            "  col = mix(col, vec3(0.35, 0.0, 0.0), pulse);\n" +
            "  a = max(a, pulse * 0.7);\n" +
            "  float g = hash(gl_FragCoord.xy + fract(uTime) * 91.0) - 0.5;\n" +
            "  col += vec3(g * 0.10);\n" +
            "  a = clamp(a + abs(g) * 0.07, 0.0, 1.0);\n" +
            "  float lf = uFlash * 0.16;\n" +
            "  gl_FragColor = vec4(col + vec3(0.7, 0.8, 1.0) * lf, clamp(a + lf * 0.2, 0.0, 1.0));\n" +
            "}\n";

    // ------------------------------------------------------------------ estado
    private final Game game;
    private final int quality;
    private int pMain, pDepth, pSky, pPart, pPost, locModelMain, locModelDepth;
    private int[] diffuse = new int[TexGen.COUNT], normal = new int[TexGen.COUNT];
    private int texPart, texCloud;
    private int fbo, fboTex, fboDepth, shadowSize;
    private boolean shadowOk;
    private int[][] chunkVbo = new int[World.NC * World.NC][World.NMAT];
    private int[][] chunkCnt = new int[World.NC * World.NC][World.NMAT];
    private int[] doorVbo, doorCnt;
    private int[][] zVbo = new int[ZombieModel.NPARTS][1];
    private int[] zCnt = new int[ZombieModel.NPARTS];
    private final int[][][] wVbo = new int[Weapons.COUNT][WeaponModels.GROUPS][1];
    private final int[][] wCnt = new int[Weapons.COUNT][WeaponModels.GROUPS];
    private final boolean[] wBuilt = new boolean[Weapons.COUNT];
    private final int[] puVbo = new int[4], puCnt = new int[4];
    private int partVbo, quadVbo;
    private FloatBuffer partBuf;
    private final ZombieModel zmodel = new ZombieModel();

    private int width = 1, height = 1;
    private long lastNs;
    public volatile float fps;
    private float fpsAcc; private int fpsN;

    private final float[] proj = new float[16], view = new float[16], vp = new float[16];
    private final float[] lview = new float[16], lproj = new float[16], lvp = new float[16], model = new float[16], tmpM = new float[16];
    private final float[] camM = new float[16], vmProj = new float[16], vmVP = new float[16];
    private final float[] frustum = new float[24];
    private final float[] fwd = new float[3], rgt = new float[3], upv = new float[3];
    private final float[] lightPos = new float[20], lightCol = new float[15];
    private final float sunX = -0.42f, sunY = 0.80f, sunZ = 0.43f;

    // parámetros por cuadro
    private float lightning, wetG = 0.8f;
    private final float[] fogCol = new float[3];
    private float fogDen;
    private float time;

    public GlRenderer(Game game, int quality) {
        this.game = game;
        this.quality = quality;
    }

    // ---------------------------------------------------------------- ciclo GL
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.03f, 0.05f, 0.07f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        pMain = program(MAIN_VS, MAIN_FS, "aPos", "aNrm", "aTan", "aUV", "aCol", "aWet");
        pDepth = program(DEPTH_VS, DEPTH_FS, "aPos");
        pSky = program(SKY_VS, SKY_FS, "aP");
        pPart = program(PART_VS, PART_FS, "aPos", "aUV", "aCol");
        pPost = program(POST_VS, POST_FS, "aP");
        locModelMain = GLES20.glGetUniformLocation(pMain, "uModel");
        locModelDepth = GLES20.glGetUniformLocation(pDepth, "uModel");

        for (int i = 0; i < TexGen.COUNT; i++) {
            byte[][] t = TexGen.get(i);
            int sz = TexGen.size(i);
            diffuse[i] = texture(t[0], sz, sz, true, true);
            normal[i] = texture(t[1], sz, sz, true, true);
        }
        texPart = texture(TexGen.particles(), 128, 128, false, false);
        texCloud = texture(TexGen.clouds(), 256, 256, false, true);

        float[] quad = {-1, -1, 3, -1, -1, 3};
        quadVbo = buffer(quad, quad.length);
        partBuf = ByteBuffer.allocateDirect((Particles.CAP + 500) * 54 * 4 + 4096).order(ByteOrder.nativeOrder()).asFloatBuffer();
        int[] b = new int[1];
        GLES20.glGenBuffers(1, b, 0);
        partVbo = b[0];

        uploadWorld();
        for (int i = 0; i < ZombieModel.NPARTS; i++) {
            zVbo[i][0] = buffer(zmodel.part[i], zmodel.part[i].length);
            zCnt[i] = zmodel.part[i].length / MeshBuilder.STRIDE;
        }
        for (int t = 0; t < 4; t++) {
            MeshBuilder m = new MeshBuilder();
            float[][] cc = {{1.5f, 2.2f, 3.0f}, {3.0f, 0.8f, 0.7f}, {3.0f, 2.6f, 0.7f}, {2.8f, 1.6f, 0.5f}};
            m.color(cc[t][0], cc[t][1], cc[t][2]);
            m.box(0, 0, 0, 0.28f, 0.28f, 0.28f, 0.5f);
            m.rotY(45); m.rotZ(45);
            m.box(0, 0, 0, 0.2f, 0.2f, 0.2f, 0.5f);
            float[] a = m.toArray();
            puVbo[t] = buffer(a, a.length); puCnt[t] = a.length / MeshBuilder.STRIDE;
        }
        for (int i = 0; i < Weapons.COUNT; i++) wBuilt[i] = false;

        // sombras
        shadowSize = quality == 0 ? 512 : (quality == 1 ? 1024 : 2048);
        buildShadowTarget();
        lastNs = System.nanoTime();
    }

    private void uploadWorld() {
        World w = game.world;
        for (int c = 0; c < World.NC * World.NC; c++) {
            for (int m = 0; m < World.NMAT; m++) {
                MeshBuilder mb = w.chunks[c][m];
                if (mb == null || mb.vertexCount() == 0) { chunkVbo[c][m] = 0; chunkCnt[c][m] = 0; continue; }
                float[] a = mb.toArray();
                chunkVbo[c][m] = buffer(a, a.length);
                chunkCnt[c][m] = a.length / MeshBuilder.STRIDE;
            }
        }
        doorVbo = new int[w.doors.size()]; doorCnt = new int[w.doors.size()];
        for (int i = 0; i < w.doors.size(); i++) {
            World.Door d = w.doors.get(i);
            doorVbo[i] = buffer(d.mesh, d.verts * MeshBuilder.STRIDE);
            doorCnt[i] = d.verts;
        }
    }

    private void buildShadowTarget() {
        int[] t = new int[1];
        GLES20.glGenTextures(1, t, 0);
        fboTex = t[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTex);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, shadowSize, shadowSize, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        int[] rb = new int[1];
        GLES20.glGenRenderbuffers(1, rb, 0);
        fboDepth = rb[0];
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, fboDepth);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, shadowSize, shadowSize);
        int[] fb = new int[1];
        GLES20.glGenFramebuffers(1, fb, 0);
        fbo = fb[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, fboTex, 0);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, fboDepth);
        shadowOk = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) == GLES20.GL_FRAMEBUFFER_COMPLETE;
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        width = w; height = h;
        GLES20.glViewport(0, 0, w, h);
    }

    // ------------------------------------------------------------------ utilidades GL
    private static int shader(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        return s;
    }

    private static int program(String vs, String fs, String... attrs) {
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, shader(GLES20.GL_VERTEX_SHADER, vs));
        GLES20.glAttachShader(p, shader(GLES20.GL_FRAGMENT_SHADER, fs));
        for (int i = 0; i < attrs.length; i++) GLES20.glBindAttribLocation(p, i, attrs[i]);
        GLES20.glLinkProgram(p);
        return p;
    }

    private static void resetAttribs() {
        for (int i = 0; i < 6; i++) GLES20.glDisableVertexAttribArray(i);
    }

    private static int texture(byte[] rgba, int w, int h, boolean mips, boolean repeat) {
        int[] t = new int[1];
        GLES20.glGenTextures(1, t, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t[0]);
        ByteBuffer bb = ByteBuffer.allocateDirect(rgba.length).order(ByteOrder.nativeOrder());
        bb.put(rgba).position(0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bb);
        if (mips) GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, mips ? GLES20.GL_LINEAR_MIPMAP_LINEAR : GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        int wrap = repeat ? GLES20.GL_REPEAT : GLES20.GL_CLAMP_TO_EDGE;
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, wrap);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, wrap);
        return t[0];
    }

    private static int buffer(float[] data, int count) {
        int[] b = new int[1];
        GLES20.glGenBuffers(1, b, 0);
        FloatBuffer fb = ByteBuffer.allocateDirect(count * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        fb.put(data, 0, count).position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, b[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, count * 4, fb, GLES20.GL_STATIC_DRAW);
        return b[0];
    }

    private void bindMesh(int prog, int vbo) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glEnableVertexAttribArray(0);
        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, FS, 0);
        if (prog == pMain) {
            GLES20.glEnableVertexAttribArray(1); GLES20.glVertexAttribPointer(1, 3, GLES20.GL_FLOAT, false, FS, 12);
            GLES20.glEnableVertexAttribArray(2); GLES20.glVertexAttribPointer(2, 3, GLES20.GL_FLOAT, false, FS, 24);
            GLES20.glEnableVertexAttribArray(3); GLES20.glVertexAttribPointer(3, 2, GLES20.GL_FLOAT, false, FS, 36);
            GLES20.glEnableVertexAttribArray(4); GLES20.glVertexAttribPointer(4, 3, GLES20.GL_FLOAT, false, FS, 44);
            GLES20.glEnableVertexAttribArray(5); GLES20.glVertexAttribPointer(5, 1, GLES20.GL_FLOAT, false, FS, 56);
        }
    }

    private void uni1(int p, String n, float v) { GLES20.glUniform1f(GLES20.glGetUniformLocation(p, n), v); }
    private void uni3(int p, String n, float a, float b, float c) { GLES20.glUniform3f(GLES20.glGetUniformLocation(p, n), a, b, c); }
    private void uniM(int p, String n, float[] m) { GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(p, n), 1, false, m, 0); }
    private void uniI(int p, String n, int v) { GLES20.glUniform1i(GLES20.glGetUniformLocation(p, n), v); }

    private void tex(int mat) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, diffuse[mat]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, normal[mat]);
    }

    // ------------------------------------------------------------------ cuadro
    @Override
    public void onDrawFrame(GL10 gl) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastNs) / 1e9f);
        lastNs = now;
        fpsAcc += dt; fpsN++;
        if (fpsAcc > 0.5f) { fps = fpsN / fpsAcc; fpsAcc = 0; fpsN = 0; }
        time += dt;
        game.update(dt);
        draw();
    }

    private float flick(World.Light l) {
        if (l.flicker == 1) {
            float t = time + l.phase;
            return 0.72f + 0.2f * (float) Math.sin(t * 17f) * (float) Math.sin(t * 7.3f) + 0.1f * (float) Math.sin(t * 41f);
        }
        if (l.flicker == 2) return 0.82f + 0.18f * (float) Math.sin(time * 2.2f + l.phase);
        return 1f;
    }

    private void draw() {
        Game g = game;
        World w = g.world;
        lightning = g.lightning;
        float aspect = width / (float) height;
        float fov = 72f - 20f * g.adsT;
        Mat4.perspective(proj, fov, aspect, 0.1f, 230f);
        float fx = g.fwdX(), fy = g.fwdY(), fz = g.fwdZ();
        Mat4.lookAt(view, g.px, g.py, g.pz, g.px + fx, g.py + fy, g.pz + fz, 0, 1, 0);
        Mat4.multiply(vp, proj, view);
        extractFrustum(vp);
        fwd[0] = fx; fwd[1] = fy; fwd[2] = fz;
        rgt[0] = (float) Math.cos(Math.toRadians(g.yaw)); rgt[1] = 0; rgt[2] = (float) Math.sin(Math.toRadians(g.yaw));
        upv[0] = rgt[1] * fz - rgt[2] * fy; upv[1] = rgt[2] * fx - rgt[0] * fz; upv[2] = rgt[0] * fy - rgt[1] * fx;

        // atmósfera
        float lt = lightning;
        boolean night = g.mode == Game.MODE_NIGHT;
        float dark = night ? 0.55f : 1f;
        fogCol[0] = (0.07f + 0.5f * lt) * dark; fogCol[1] = (0.10f + 0.55f * lt) * dark; fogCol[2] = (0.14f + 0.65f * lt) * dark;
        fogDen = night ? 0.034f : 0.021f;

        // luces cercanas
        pickLights();

        // ---------- sombras
        shadowPass();

        // ---------- escena
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glViewport(0, 0, width, height);
        GLES20.glClearColor(fogCol[0], fogCol[1], fogCol[2], 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        drawSky(aspect, fov);

        GLES20.glUseProgram(pMain); resetAttribs();
        setMainUniforms(true);
        drawWorld();
        drawDoors();
        drawZombies(false);
        drawPowerUps();
        drawParticles();

        // viewmodel
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
        drawViewModel(aspect);

        // post
        drawPost(aspect);
    }

    private void pickLights() {
        Game g = game;
        ArrayList<World.Light> ls = g.world.lights;
        int n = ls.size();
        float[] bestD = {1e9f, 1e9f, 1e9f, 1e9f};
        int[] bestI = {-1, -1, -1, -1};
        for (int i = 0; i < n; i++) {
            World.Light l = ls.get(i);
            float dx = l.x - g.px, dy = l.y - g.py, dz = l.z - g.pz;
            float d2 = dx * dx + dy * dy + dz * dz - l.range * l.range * 0.3f;
            if (d2 > 1600f) continue;
            for (int k = 0; k < 4; k++) {
                if (d2 < bestD[k]) {
                    for (int m = 3; m > k; m--) { bestD[m] = bestD[m - 1]; bestI[m] = bestI[m - 1]; }
                    bestD[k] = d2; bestI[k] = i;
                    break;
                }
            }
        }
        for (int k = 0; k < 4; k++) {
            int o = (k + 1) * 4;
            if (bestI[k] < 0) { lightPos[o] = 0; lightPos[o + 1] = -500; lightPos[o + 2] = 0; lightPos[o + 3] = 1; lightCol[(k + 1) * 3] = lightCol[(k + 1) * 3 + 1] = lightCol[(k + 1) * 3 + 2] = 0; continue; }
            World.Light l = ls.get(bestI[k]);
            float f = flick(l) * l.intensity;
            lightPos[o] = l.x; lightPos[o + 1] = l.y; lightPos[o + 2] = l.z; lightPos[o + 3] = l.range;
            lightCol[(k + 1) * 3] = l.r * f; lightCol[(k + 1) * 3 + 1] = l.g * f; lightCol[(k + 1) * 3 + 2] = l.b * f;
        }
        // luz del fogonazo (ranura 0)
        if (g.flashT > 0) {
            float k = g.flashT / 0.07f;
            lightPos[0] = g.px + fwd[0] * 0.8f; lightPos[1] = g.py + fwd[1] * 0.8f; lightPos[2] = g.pz + fwd[2] * 0.8f; lightPos[3] = 14f;
            float st = g.weapon() == Weapons.STORM ? 1f : 0f;
            lightCol[0] = (1.8f * (1 - st) + 0.5f * st) * k; lightCol[1] = (1.3f * (1 - st) + 1.3f * st) * k; lightCol[2] = (0.6f * (1 - st) + 2.2f * st) * k;
        } else {
            lightPos[0] = 0; lightPos[1] = -500; lightPos[2] = 0; lightPos[3] = 1;
            lightCol[0] = lightCol[1] = lightCol[2] = 0;
        }
    }

    private float[] sunColor = new float[3];

    private void setMainUniforms(boolean shadows) {
        Game g = game;
        float lt = lightning;
        boolean night = g.mode == Game.MODE_NIGHT;
        float k = night ? 0.5f : 1f;
        float sr = (0.30f + 1.1f * lt) * k, sg = (0.42f + 1.15f * lt) * k, sb = (0.62f + 1.3f * lt) * k;
        uni3(pMain, "uSunDir", sunX, sunY, sunZ);
        uni3(pMain, "uSunCol", sr, sg, sb);
        uni3(pMain, "uSkyAmb", (0.10f + 0.45f * lt) * k, (0.14f + 0.5f * lt) * k, (0.22f + 0.6f * lt) * k);
        uni3(pMain, "uGndAmb", (0.06f + 0.1f * lt) * k, (0.05f + 0.1f * lt) * k, (0.05f + 0.12f * lt) * k);
        uni3(pMain, "uFogCol", fogCol[0], fogCol[1], fogCol[2]);
        uni1(pMain, "uFogDen", fogDen);
        uni3(pMain, "uCam", g.px, g.py, g.pz);
        GLES20.glUniform4fv(GLES20.glGetUniformLocation(pMain, "uLP[0]"), 5, lightPos, 0);
        GLES20.glUniform3fv(GLES20.glGetUniformLocation(pMain, "uLC[0]"), 5, lightCol, 0);
        uni1(pMain, "uWetG", wetG);
        uni3(pMain, "uTint", 1, 1, 1);
        uni1(pMain, "uHit", 0f);
        uni1(pMain, "uShadowOn", shadows && shadowOk ? 1f : 0f);
        uni1(pMain, "uTexel", 1f / shadowSize);
        uniM(pMain, "uVP", vp);
        uniM(pMain, "uShadowVP", lvp);
        Mat4.identity(model);
        uniM(pMain, "uModel", model);
        uniI(pMain, "uTex", 0); uniI(pMain, "uNrm", 1); uniI(pMain, "uShadow", 2);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTex);
    }

    // ------------------------------------------------------------------ sombras
    private void shadowPass() {
        Game g = game;
        float E = 34f;
        float lx = sunX, ly = sunY, lz = sunZ;
        float ll = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
        lx /= ll; ly /= ll; lz /= ll;
        // base de la luz
        float[] f = {-lx, -ly, -lz};
        float rx = f[1] * 0 - f[2] * 1, ry = f[2] * 0 - f[0] * 0, rz = f[0] * 1 - f[1] * 0;
        float rl = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        rx /= rl; ry /= rl; rz /= rl;
        float ux = ry * f[2] - rz * f[1], uy = rz * f[0] - rx * f[2], uz = rx * f[1] - ry * f[0];
        float texel = 2 * E / shadowSize;
        float cx = g.px + fwd[0] * 14f, cy = 0, cz = g.pz + fwd[2] * 14f;
        float sx = (float) Math.floor((cx * rx + cy * ry + cz * rz) / texel) * texel;
        float sy = (float) Math.floor((cx * ux + cy * uy + cz * uz) / texel) * texel;
        float sz = cx * f[0] + cy * f[1] + cz * f[2];
        float ccx = rx * sx + ux * sy + f[0] * sz, ccy = ry * sx + uy * sy + f[1] * sz, ccz = rz * sx + uz * sy + f[2] * sz;
        Mat4.lookAt(lview, ccx + lx * 70, ccy + ly * 70, ccz + lz * 70, ccx, ccy, ccz, 0, 1, 0);
        Mat4.ortho(lproj, -E, E, -E, E, 5f, 150f);
        Mat4.multiply(lvp, lproj, lview);
        if (!shadowOk) return;
        float[] lvpRaw = new float[16];
        System.arraycopy(lvp, 0, lvpRaw, 0, 16);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        GLES20.glViewport(0, 0, shadowSize, shadowSize);
        GLES20.glClearColor(1f, 1f, 1f, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glCullFace(GLES20.GL_FRONT);
        GLES20.glUseProgram(pDepth); resetAttribs();
        uniM(pDepth, "uLVP", lvpRaw);
        Mat4.identity(model);
        uniM(pDepth, "uModel", model);
        World w = g.world;
        for (int cz2 = 0; cz2 < World.NC; cz2++) {
            for (int cx2 = 0; cx2 < World.NC; cx2++) {
                float mx = World.ORG + (cx2 + 0.5f) * World.CH, mz = World.ORG + (cz2 + 0.5f) * World.CH;
                if (Math.abs(mx - g.px) > E + 20 || Math.abs(mz - g.pz) > E + 20) continue;
                for (int m = 0; m < World.NMAT; m++) {
                    if (m == World.M_MUD) continue; // el terreno no proyecta sombra sobre sí mismo
                    int vb = chunkVbo[cz2 * World.NC + cx2][m];
                    if (vb == 0) continue;
                    bindMesh(pDepth, vb);
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, chunkCnt[cz2 * World.NC + cx2][m]);
                }
            }
        }
        for (int i = 0; i < w.doors.size(); i++) {
            if (w.doors.get(i).open) continue;
            bindMesh(pDepth, doorVbo[i]);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, doorCnt[i]);
        }
        drawZombies(true);
        GLES20.glCullFace(GLES20.GL_BACK);
        // matriz de sombras para el pase principal (misma ortográfica; la conversión a [0,1] ocurre en el shader)
    }

    // ------------------------------------------------------------------ cielo
    private void drawSky(float aspect, float fov) {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glUseProgram(pSky); resetAttribs();
        uni3(pSky, "uFwd", fwd[0], fwd[1], fwd[2]);
        uni3(pSky, "uRight", rgt[0], rgt[1], rgt[2]);
        uni3(pSky, "uUp", upv[0], upv[1], upv[2]);
        uni1(pSky, "uTanY", (float) Math.tan(Math.toRadians(fov) * 0.5));
        uni1(pSky, "uAspect", aspect);
        float ml = (float) Math.sqrt(sunX * sunX + sunY * sunY + sunZ * sunZ);
        uni3(pSky, "uMoon", sunX / ml, sunY / ml, sunZ / ml);
        uni1(pSky, "uTime", time);
        uni1(pSky, "uLight", lightning);
        boolean night = game.mode == Game.MODE_NIGHT;
        float k = night ? 0.5f : 1f;
        uni3(pSky, "uTop", 0.018f * k, 0.035f * k, 0.065f * k);
        uni3(pSky, "uHor", fogCol[0] * 1.05f + 0.01f, fogCol[1] * 1.05f + 0.015f, fogCol[2] * 1.05f + 0.02f);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texCloud);
        uniI(pSky, "uCloud", 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, quadVbo);
        GLES20.glEnableVertexAttribArray(0);
        GLES20.glVertexAttribPointer(0, 2, GLES20.GL_FLOAT, false, 8, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
        GLES20.glDisableVertexAttribArray(0);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
    }

    // ------------------------------------------------------------------ mundo
    private void extractFrustum(float[] m) {
        // planos a partir de la matriz VP (columnas)
        for (int i = 0; i < 6; i++) {
            int row = i >> 1; float sgn = (i & 1) == 0 ? 1f : -1f;
            float a = m[3] + sgn * m[row], b = m[7] + sgn * m[4 + row], c = m[11] + sgn * m[8 + row], d = m[15] + sgn * m[12 + row];
            float l = (float) Math.sqrt(a * a + b * b + c * c);
            frustum[i * 4] = a / l; frustum[i * 4 + 1] = b / l; frustum[i * 4 + 2] = c / l; frustum[i * 4 + 3] = d / l;
        }
    }

    private boolean visible(float x, float y, float z, float r) {
        for (int i = 0; i < 6; i++) {
            if (frustum[i * 4] * x + frustum[i * 4 + 1] * y + frustum[i * 4 + 2] * z + frustum[i * 4 + 3] < -r) return false;
        }
        return true;
    }

    private void drawWorld() {
        Game g = game;
        GLES20.glUseProgram(pMain); resetAttribs();
        float maxD = 1.0f / fogDen * 3.2f;
        for (int mat = 0; mat < World.NMAT; mat++) {
            boolean bound = false;
            for (int cz = 0; cz < World.NC; cz++) {
                for (int cx = 0; cx < World.NC; cx++) {
                    int idx = cz * World.NC + cx;
                    int vb = chunkVbo[idx][mat];
                    if (vb == 0) continue;
                    float mx = World.ORG + (cx + 0.5f) * World.CH, mz = World.ORG + (cz + 0.5f) * World.CH;
                    float dd = (float) Math.hypot(mx - g.px, mz - g.pz);
                    if (dd > maxD + 28f) continue;
                    if (!visible(mx, 4f, mz, 36f)) continue;
                    if (!bound) { tex(mat); bound = true; }
                    bindMesh(pMain, vb);
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, chunkCnt[idx][mat]);
                }
            }
        }
    }

    private void drawDoors() {
        World w = game.world;
        tex(World.M_WOOD);
        for (int i = 0; i < w.doors.size(); i++) {
            World.Door d = w.doors.get(i);
            if (d.open) continue;
            if (!visible(d.x, 2f, d.z, 8f)) continue;
            bindMesh(pMain, doorVbo[i]);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, doorCnt[i]);
        }
    }

    private void drawPowerUps() {
        Game g = game;
        if (g.ups.isEmpty()) return;
        tex(World.M_METAL);
        for (int i = 0; i < g.ups.size(); i++) {
            Game.PowerUp u = g.ups.get(i);
            Mat4.identity(model);
            Mat4.translate(model, u.x, u.y + 0.15f * (float) Math.sin(time * 3f), u.z);
            Mat4.rotateY(model, u.spin);
            uniM(pMain, "uModel", model);
            bindMesh(pMain, puVbo[u.type]);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, puCnt[u.type]);
        }
        Mat4.identity(model);
        uniM(pMain, "uModel", model);
    }

    // ------------------------------------------------------------------ zombis
    private void drawZombies(boolean shadow) {
        Game g = game;
        int prog = shadow ? pDepth : pMain;
        if (!shadow) tex(TexGen.ZOMBIE);
        float maxD = shadow ? 46f : 1.0f / fogDen * 3.2f;
        for (int i = 0; i < g.zs.size(); i++) {
            Game.Zombie z = g.zs.get(i);
            float d = (float) Math.hypot(z.x - g.px, z.z - g.pz);
            if (d > maxD) continue;
            if (!shadow && !visible(z.x, z.y + 1f, z.z, 2.4f)) continue;
            if (!shadow) {
                uni3(pMain, "uTint", z.tr, z.tg, z.tb);
                uni1(pMain, "uHit", z.hitFlash > 0 ? 1f : 0f);
            }
            drawZombie(z, prog);
        }
        if (!shadow) {
            uni3(pMain, "uTint", 1, 1, 1);
            uni1(pMain, "uHit", 0f);
            Mat4.identity(model);
            uniM(pMain, "uModel", model);
        }
    }

    private void part(int prog, int p, float[] m) {
        GLES20.glUniformMatrix4fv(prog == pMain ? locModelMain : locModelDepth, 1, false, m, 0);
        bindMesh(prog, zVbo[p][0]);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, zCnt[p]);
    }

    private final float[] hipM = new float[16], torsoM = new float[16], jointM = new float[16], partM = new float[16], sub = new float[16];

    private static float sn(double v) { return (float) Math.sin(v); }

    private void drawZombie(Game.Zombie z, int prog) {
        float s = z.scale;
        float ph = z.phase;
        int gait = z.gait;           // 0 arrastrado, 1 cojo, 2 tambaleo, 3 abalanzado
        float walk = z.state == 1 ? 1f : 0.2f;
        float swing = (z.tier == 0 ? 24f : (z.tier == 1 ? 38f : 54f)) * walk;
        float lean = z.tier == 0 ? 9f : (z.tier == 1 ? 18f : 30f);
        float armBase = z.tier == 2 ? -38f : (gait == 3 ? -92f : -74f);
        float armPump = z.tier == 2 ? 42f : 8f;
        float headRoll = sn(ph * 0.7f + 1f) * 8f + (gait == 1 ? 14f : 6f);
        float limp = gait == 1 ? 1f : 0f;
        float stag = z.hitFlash > 0 ? Math.min(1f, z.hitFlash * 4f) : 0f;
        float y = z.y + 0.97f * s + (float) Math.abs(sn(ph)) * (gait == 2 ? 0.05f : 0.03f) * s;
        float fall = 0;
        float riseK = 0;
        if (z.state == 0) {
            riseK = 1f - z.riseT;
            y -= 2.1f * riseK * s;
            lean = 60f * riseK; armBase = -105f; swing = 0;
        }
        float dir = z.seed < 0.5f ? 1f : -1f;
        if (z.state == 2) {
            fall = Math.min(1f, z.deathT / (z.seed < 0.33f ? 0.55f : 0.85f));
            fall = fall * fall * (3 - 2 * fall);
            y = z.y + (0.97f - 0.77f * fall) * s - Math.max(0f, z.deathT - 4.5f) * 0.45f;
            swing = 0;
        }
        float atk = z.atkT > 0 ? sn(Math.PI * (1f - z.atkT / 0.45f)) : 0f;
        Mat4.identity(hipM);
        Mat4.translate(hipM, z.x, y, z.z);
        Mat4.rotateY(hipM, 180f - z.yaw);
        Mat4.scale(hipM, s, s, s);
        if (z.state == 2) {
            // caída variada: de espaldas, de lado o de bruces
            if (z.seed < 0.33f) Mat4.rotateX(hipM, -88f * fall);
            else if (z.seed < 0.66f) { Mat4.rotateZ(hipM, 84f * fall * dir); Mat4.rotateX(hipM, -20f * fall); }
            else Mat4.rotateX(hipM, 86f * fall);
        } else if (stag > 0) {
            Mat4.rotateX(hipM, -9f * stag);
        }
        boolean coat = z.variant < 2;
        boolean helmet = z.variant == 0 || z.variant == 3;
        float hipSway = sn(ph) * (gait == 2 ? 7f : 4f) * walk;
        Mat4.rotateZ(hipM, hipSway * 0.5f);
        // torso
        System.arraycopy(hipM, 0, torsoM, 0, 16);
        float sway = sn(ph * 0.5f) * (gait == 2 ? 9f : 5f);
        Mat4.rotateX(torsoM, lean + atk * 28f - stag * 10f);
        Mat4.rotateZ(torsoM, sway - limp * 5f);
        part(prog, coat ? ZombieModel.TORSO_COAT : ZombieModel.TORSO_RAG, torsoM);
        // cabeza
        System.arraycopy(torsoM, 0, jointM, 0, 16);
        Mat4.translate(jointM, 0, 0.585f, 0);
        Mat4.rotateZ(jointM, headRoll + stag * 12f);
        Mat4.rotateX(jointM, -lean * 0.8f + sn(ph * 0.5f) * 5f + stag * 14f);
        part(prog, ZombieModel.HEAD, jointM);
        if (helmet) {
            System.arraycopy(jointM, 0, sub, 0, 16);
            Mat4.translate(sub, 0, 0.165f, 0);
            part(prog, ZombieModel.HELMET, sub);
        }
        // mandíbula: abierta al atacar / gruñir, colgando al morir
        float jawOpen = 10f + 8f * (0.5f + 0.5f * sn(ph * 1.3f)) + atk * 22f + (z.state == 2 ? 24f : 0f) + (z.tier == 2 ? 14f : 0f);
        System.arraycopy(jointM, 0, sub, 0, 16);
        Mat4.translate(sub, 0, 0.095f, -0.035f);
        Mat4.rotateX(sub, jawOpen);
        Mat4.translate(sub, 0, -0.095f, 0.035f);
        part(prog, ZombieModel.JAW, sub);
        // brazos
        for (int side = -1; side <= 1; side += 2) {
            System.arraycopy(torsoM, 0, jointM, 0, 16);
            Mat4.translate(jointM, side * 0.31f, 0.48f, 0);
            float a = armBase + (side > 0 ? 1 : -1) * sn(ph) * armPump - atk * 22f + stag * 25f;
            if (gait == 3) a += (side > 0 ? 6f : -16f);
            if (z.state == 2) a = -20f + side * 18f;
            Mat4.rotateX(jointM, a);
            Mat4.rotateZ(jointM, side * (-5f - sn(ph * 0.5f + side) * 4f));
            part(prog, coat ? ZombieModel.UARM_COAT : ZombieModel.UARM_BARE, jointM);
            System.arraycopy(jointM, 0, sub, 0, 16);
            Mat4.translate(sub, 0, -0.3f, 0);
            Mat4.rotateX(sub, -22f - (z.tier == 2 ? 50f : 0f) + sn(ph + side) * 6f);
            part(prog, side < 0 ? ZombieModel.FARM_NEG : ZombieModel.FARM_POS, sub);
        }
        // piernas
        for (int side = -1; side <= 1; side += 2) {
            System.arraycopy(hipM, 0, jointM, 0, 16);
            Mat4.translate(jointM, side * 0.11f, 0, 0);
            float sw = swing * ((limp > 0 && side > 0) ? 0.45f : 1f);
            float a = sn(ph + (side > 0 ? 0 : Math.PI)) * sw;
            if (gait == 0) a = a * 0.8f - 6f * walk;
            if (z.state == 2) a = 10f * side;
            Mat4.rotateX(jointM, a);
            Mat4.rotateZ(jointM, -side * 2f);
            part(prog, ZombieModel.THIGH, jointM);
            System.arraycopy(jointM, 0, sub, 0, 16);
            Mat4.translate(sub, 0, -0.47f, 0);
            float bend = Math.max(0f, -sn(ph + (side > 0 ? 0 : Math.PI) + 0.9f)) * sw * 1.4f;
            if (limp > 0 && side > 0) bend *= 0.5f;
            Mat4.rotateX(sub, -bend);
            part(prog, ZombieModel.SHIN, sub);
        }
    }

    // ------------------------------------------------------------------ arma en primera persona
    private void buildWeapon(int w) {
        float[][] v = WeaponModels.view(w);
        for (int k = 0; k < WeaponModels.GROUPS; k++) {
            if (v[k].length == 0) { wCnt[w][k] = 0; continue; }
            wVbo[w][k][0] = buffer(v[k], v[k].length);
            wCnt[w][k] = v[k].length / MeshBuilder.STRIDE;
        }
        wBuilt[w] = true;
    }

    private final float[] vmPart = new float[16], vmTmp = new float[16];

    private void drawGroup(int w, int k, float[] base, float dx, float dy, float dz) {
        if (wCnt[w][k] == 0) return;
        System.arraycopy(base, 0, vmTmp, 0, 16);
        if (dx != 0 || dy != 0 || dz != 0) Mat4.translate(vmTmp, dx, dy, dz);
        uniM(pMain, "uModel", vmTmp);
        bindMesh(pMain, wVbo[w][k][0]);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, wCnt[w][k]);
    }

    private void drawViewModel(float aspect) {
        Game g = game;
        int w = g.weapon();
        if (g.over) return;
        if (!wBuilt[w]) buildWeapon(w);
        Mat4.perspective(vmProj, 58f, aspect, 0.03f, 10f);
        // matriz de la cámara en el mundo
        float fx = fwd[0], fy = fwd[1], fz = fwd[2];
        camM[0] = rgt[0]; camM[1] = rgt[1]; camM[2] = rgt[2]; camM[3] = 0;
        camM[4] = upv[0]; camM[5] = upv[1]; camM[6] = upv[2]; camM[7] = 0;
        camM[8] = -fx; camM[9] = -fy; camM[10] = -fz; camM[11] = 0;
        camM[12] = g.px; camM[13] = g.py; camM[14] = g.pz; camM[15] = 1;
        float[] hip = WeaponModels.hip(w);
        float a = g.adsT;
        float ox = hip[0] * (1 - a), oy = hip[1] * (1 - a) + (-0.066f) * a, oz = hip[2] * (1 - a) + (-0.28f) * a;
        float bobx = (float) Math.sin(g.bobT) * 0.006f * (1 - a), boby = (float) Math.abs(Math.cos(g.bobT)) * -0.005f * (1 - a);
        float rec = g.recoil;
        float rotx = rec * 3.5f, rotz = 0;
        float dip = 0;
        if (g.reloadT > 0) {
            float p = 1f - g.reloadT / Math.max(0.1f, g.reloadDur);
            float e = (float) Math.sin(Math.PI * Math.min(1f, p * 1.05f));
            dip = e;
            oy -= 0.12f * e; ox -= 0.03f * e; rotx += 38f * e; rotz = -22f * e;
        }
        if (g.swapT > 0) {
            float p = g.swapT / 0.5f;
            oy -= 0.35f * p; rotx += 40f * p;
        }
        Mat4.identity(model);
        System.arraycopy(camM, 0, model, 0, 16);
        Mat4.translate(model, ox + bobx, oy + boby, oz + rec * 0.03f);
        Mat4.rotateX(model, rotx);
        Mat4.rotateZ(model, rotz);
        // proyección propia: sobreescribimos uVP
        Mat4.multiply(vmVP, vmProj, view);
        GLES20.glUseProgram(pMain); resetAttribs();
        uniM(pMain, "uVP", vmVP);
        uni1(pMain, "uShadowOn", 0f);
        uniM(pMain, "uModel", model);
        uni3(pMain, "uTint", 1, 1, 1);
        float rp = g.reloadT > 0 ? 1f - g.reloadT / Math.max(0.1f, g.reloadDur) : 0f;
        float back = g.swapT > 0 ? 0f : WeaponModels.slideBack(w, g.sinceShot, rp);
        float drop = WeaponModels.magDrop(rp);
        tex(TexGen.GUNMETAL);
        drawGroup(w, 0, model, 0, 0, 0);
        drawGroup(w, 3, model, 0, 0, back);
        drawGroup(w, 4, model, 0, drop, 0);
        tex(TexGen.WALNUT);
        drawGroup(w, 1, model, 0, 0, 0);
        tex(TexGen.ZOMBIE);
        drawGroup(w, 2, model, 0, 0, 0);
        drawGroup(w, 5, model, 0, 0, back * (w == Weapons.SHOTGUN ? 1f : 0f));
        if (w == Weapons.STORM) {
            // luces, arcos eléctricos animados, núcleo de plasma que pulsa y letras
            tex(TexGen.TEMPEST);
            drawGroup(w, WeaponModels.G_GLOW, model, 0, 0, 0);
            drawGroup(w, WeaponModels.G_MAGGLOW, model, 0, drop, 0);
            drawGroup(w, WeaponModels.G_TEXT, model, 0, 0, 0);
            float shotK = Math.max(0f, 1f - g.sinceShot * 3f);
            float pulse = 0.93f + 0.07f * (float) Math.sin(time * 6f) + 0.12f * shotK;
            System.arraycopy(model, 0, vmTmp, 0, 16);
            Mat4.translate(vmTmp, 0, 0.058f, -0.1f);
            Mat4.scale(vmTmp, 1f, pulse, pulse);
            Mat4.translate(vmTmp, 0, -0.058f, 0.1f);
            uniM(pMain, "uModel", vmTmp);
            bindMesh(pMain, wVbo[w][WeaponModels.G_CORE][0]);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, wCnt[w][WeaponModels.G_CORE]);
            int fr = (int) (time * 24f);
            int v1 = fr % WeaponModels.N_ARC;
            float hsh = hash1(fr, 11);
            boolean on = hsh > 0.18f || g.sinceShot < 0.4f;
            if (on) drawGroup(w, WeaponModels.G_ARC0 + v1, model, 0, 0, 0);
            if (g.sinceShot < 0.45f || hsh > 0.7f) drawGroup(w, WeaponModels.G_ARC0 + (v1 + 2 + (fr / 4) % 2) % WeaponModels.N_ARC, model, 0, 0, 0);
        }
        uniM(pMain, "uModel", model);
        // fogonazo
        if (g.flashT > 0) {
            float[] mz = WeaponModels.muzzle(w);
            float[] p = new float[3];
            Mat4.point(model, mz[0], mz[1], mz[2], p);
            float k = g.flashT / 0.07f;
            beginParticles();
            float s = (w == Weapons.SHOTGUN ? 0.5f : 0.34f) * (0.7f + 0.5f * k);
            boolean storm = w == Weapons.STORM;
            quad(p[0], p[1], p[2], s, 1, storm ? 0.4f : 1f, storm ? 0.9f : 0.75f, storm ? 1.6f : 0.35f, k, g.time * 40f, true, true);
            quad(p[0], p[1], p[2], s * 0.5f, 1, 1f, 1f, 0.9f, k, g.time * 77f, true, true);
            flushParticlesViewModel();
        }
        uniM(pMain, "uVP", vp);
    }

    // ------------------------------------------------------------------ partículas
    private int nAlpha, nAdd;
    private final ArrayList<float[]> pend = new ArrayList<float[]>();
    private FloatBuffer alphaBuf, addBuf;

    private float[] pa = new float[(Particles.CAP + 500) * 54];
    private float[] pb = new float[(Particles.CAP + 500) * 54];
    private int ia, ib;
    private boolean curAdd;

    private void beginParticles() { ia = 0; ib = 0; }

    private void v(float[] dst, int off, float x, float y, float z, float u, float vv, float r, float g, float b, float a) {
        dst[off] = x; dst[off + 1] = y; dst[off + 2] = z; dst[off + 3] = u; dst[off + 4] = vv;
        dst[off + 5] = r; dst[off + 6] = g; dst[off + 7] = b; dst[off + 8] = a;
    }

    private void quad(float x, float y, float z, float size, int cell, float r, float g, float b, float a, float rot, boolean add, boolean camera) {
        float h = size * 0.5f;
        float c = (float) Math.cos(rot) * h, s = (float) Math.sin(rot) * h;
        float ax = rgt[0] * c + upv[0] * s, ay = rgt[1] * c + upv[1] * s, az = rgt[2] * c + upv[2] * s;
        float bx = -rgt[0] * s + upv[0] * c, by = -rgt[1] * s + upv[1] * c, bz = -rgt[2] * s + upv[2] * c;
        float u0 = (cell & 1) * 0.5f, v0 = (cell >> 1) * 0.5f, u1 = u0 + 0.5f, v1 = v0 + 0.5f;
        // evita sangrado del atlas
        u0 += 0.004f; v0 += 0.004f; u1 -= 0.004f; v1 -= 0.004f;
        float[] d = add ? pb : pa;
        int o = add ? ib : ia;
        v(d, o, x - ax - bx, y - ay - by, z - az - bz, u0, v1, r, g, b, a);
        v(d, o + 9, x + ax - bx, y + ay - by, z + az - bz, u1, v1, r, g, b, a);
        v(d, o + 18, x + ax + bx, y + ay + by, z + az + bz, u1, v0, r, g, b, a);
        v(d, o + 27, x - ax - bx, y - ay - by, z - az - bz, u0, v1, r, g, b, a);
        v(d, o + 36, x + ax + bx, y + ay + by, z + az + bz, u1, v0, r, g, b, a);
        v(d, o + 45, x - ax + bx, y - ay + by, z - az + bz, u0, v0, r, g, b, a);
        if (add) ib += 54; else ia += 54;
    }

    private void ribbon(float sx, float sy, float sz, float ex, float ey, float ez, float width, float r, float g, float b, float a) {
        float dx = ex - sx, dy = ey - sy, dz = ez - sz;
        float mx = (sx + ex) * 0.5f - game.px, my = (sy + ey) * 0.5f - game.py, mz = (sz + ez) * 0.5f - game.pz;
        float cxp = dy * mz - dz * my, cyp = dz * mx - dx * mz, czp = dx * my - dy * mx;
        float cl = (float) Math.sqrt(cxp * cxp + cyp * cyp + czp * czp);
        if (cl < 1e-6f) return;
        cxp = cxp / cl * width * 0.5f; cyp = cyp / cl * width * 0.5f; czp = czp / cl * width * 0.5f;
        float u0 = 0.0f + 0.004f, v0 = 0.5f + 0.004f, u1 = 0.5f - 0.004f, v1 = 1f - 0.004f;
        float[] d = pb; int o = ib;
        v(d, o, sx - cxp, sy - cyp, sz - czp, u0, v1, r, g, b, a);
        v(d, o + 9, sx + cxp, sy + cyp, sz + czp, u1, v1, r, g, b, a);
        v(d, o + 18, ex + cxp, ey + cyp, ez + czp, u1, v0, r, g, b, a);
        v(d, o + 27, sx - cxp, sy - cyp, sz - czp, u0, v1, r, g, b, a);
        v(d, o + 36, ex + cxp, ey + cyp, ez + czp, u1, v0, r, g, b, a);
        v(d, o + 45, ex - cxp, ey - cyp, ez - czp, u0, v0, r, g, b, a);
        ib += 54;
    }

    private void rainStreak(float x, float y, float z, float len, float width, float a) {
        float mx = x - game.px, mz = z - game.pz;
        float ml = (float) Math.sqrt(mx * mx + mz * mz) + 1e-4f;
        float sx = -mz / ml * width * 0.5f, sz = mx / ml * width * 0.5f;
        float u0 = 0.0f + 0.004f, v0 = 0.5f + 0.004f, u1 = 0.5f - 0.004f, v1 = 1f - 0.004f;
        float tx = 0.25f * len, r = 0.62f, g = 0.7f, b = 0.85f;
        float[] d = pa; int o = ia;
        v(d, o, x - sx, y, z - sz, u0, v1, r, g, b, a);
        v(d, o + 9, x + sx, y, z + sz, u1, v1, r, g, b, a);
        v(d, o + 18, x + sx + tx, y + len, z + sz, u1, v0, r, g, b, a);
        v(d, o + 27, x - sx, y, z - sz, u0, v1, r, g, b, a);
        v(d, o + 36, x + sx + tx, y + len, z + sz, u1, v0, r, g, b, a);
        v(d, o + 45, x - sx + tx, y + len, z - sz, u0, v0, r, g, b, a);
        ia += 54;
    }

    private float hash1(int i, int s) {
        int h = i * 374761393 + s * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177; h ^= h >>> 16;
        return (h & 0xFFFFFF) / 16777216f;
    }

    private void drawParticles() {
        Game g = game;
        Particles ps = g.ps;
        ps.limit = quality == 0 ? 350 : (quality == 1 ? 650 : Particles.CAP);
        beginParticles();
        // lluvia
        int nRain = quality == 0 ? 110 : (quality == 1 ? 170 : 240);
        float box = 34f;
        for (int i = 0; i < nRain; i++) {
            float hx = hash1(i, 1) * box, hz = hash1(i, 2) * box, hy = hash1(i, 3);
            float wx = g.px + mod(hx - g.px + box * 0.5f, box) - box * 0.5f;
            float wz = g.pz + mod(hz - g.pz + box * 0.5f, box) - box * 0.5f;
            float wy = g.py + 9f - mod(hy * 18f + time * (15f + hash1(i, 4) * 5f), 18f);
            rainStreak(wx, wy, wz, 0.75f, 0.014f, 0.26f);
        }
        // llamas, halos
        for (int i = 0; i < g.world.fires.size(); i++) {
            World.Fire f = g.world.fires.get(i);
            float d2 = (f.x - g.px) * (f.x - g.px) + (f.z - g.pz) * (f.z - g.pz);
            if (d2 > 55f * 55f) continue;
            float t = time * 9f + i * 3.1f;
            float fl = 0.8f + 0.2f * (float) Math.sin(t) + 0.1f * (float) Math.sin(t * 2.7f);
            float s = f.size;
            quad(f.x, f.y + 0.35f * s, f.z, 1.15f * s * fl, 1, 1f, 0.45f, 0.12f, 0.75f, t * 0.3f, true, true);
            quad(f.x + 0.05f * (float) Math.sin(t * 1.3f), f.y + 0.6f * s * fl, f.z, 0.8f * s * fl, 1, 1f, 0.7f, 0.25f, 0.8f, -t * 0.4f, true, true);
            quad(f.x, f.y + 0.2f * s, f.z, 0.5f * s, 1, 1f, 0.9f, 0.6f, 0.9f, t, true, true);
            quad(f.x, f.y + 0.7f * s, f.z, 4.5f * s, 1, 1f, 0.45f, 0.12f, 0.14f * fl, 0, true, true);
        }
        for (int i = 0; i < g.world.lights.size(); i++) {
            World.Light l = g.world.lights.get(i);
            if (l.range < 6f || l.flicker == 0) continue;
            float d2 = (l.x - g.px) * (l.x - g.px) + (l.z - g.pz) * (l.z - g.pz);
            if (d2 > 50f * 50f) continue;
            if (!visible(l.x, l.y, l.z, 2f)) continue;
            float f = flick(l);
            quad(l.x, l.y, l.z, Math.min(3.2f, l.range * 0.22f), 1, l.r, l.g, l.b, 0.32f * f, 0, true, true);
        }
        // partículas del juego
        for (int i = 0; i < ps.count; i++) {
            float f = ps.life[i] / ps.maxLife[i];
            float size = ps.s0[i] + (ps.s1[i] - ps.s0[i]) * (1f - f);
            float env = Math.min(1f, (1f - f) * 9f);
            float al = ps.a[i] * env * (ps.add[i] ? f : (float) Math.sqrt(f));
            if (ps.tracer[i]) {
                ribbon(ps.x[i], ps.y[i], ps.z[i], ps.ex[i], ps.ey[i], ps.ez[i], size, ps.r[i], ps.g[i], ps.b[i], ps.a[i] * f);
            } else {
                quad(ps.x[i], ps.y[i], ps.z[i], size, ps.cell[i], ps.r[i], ps.g[i], ps.b[i], al, ps.rot[i], ps.add[i], true);
            }
        }
        flushParticles();
    }

    private float mod(float a, float b) { float r = a % b; return r < 0 ? r + b : r; }

    private void flushParticles() {
        GLES20.glUseProgram(pPart); resetAttribs();
        uniM(pPart, "uVP", vp);
        uni1(pPart, "uFogDen", fogDen);
        float l = lightning;
        uni3(pPart, "uLit", 0.22f + 0.7f * l, 0.27f + 0.7f * l, 0.36f + 0.8f * l);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texPart);
        uniI(pPart, "uTex", 0);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDepthMask(false);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, partVbo);
        if (ia > 0) {
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            uni1(pPart, "uAdd", 0f);
            upload(pa, ia);
            draw9(ia / 9);
        }
        if (ib > 0) {
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);
            uni1(pPart, "uAdd", 1f);
            upload(pb, ib);
            draw9(ib / 9);
        }
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glUseProgram(pMain); resetAttribs();
    }

    private void flushParticlesViewModel() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glUseProgram(pPart); resetAttribs();
        uniM(pPart, "uVP", vmVP);
        uni1(pPart, "uFogDen", 0f);
        uni3(pPart, "uLit", 1, 1, 1);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texPart);
        uniI(pPart, "uTex", 0);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, partVbo);
        uni1(pPart, "uAdd", 1f);
        if (ib > 0) { upload(pb, ib); draw9(ib / 9); }
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glUseProgram(pMain); resetAttribs();
    }

    private void upload(float[] arr, int count) {
        partBuf.clear();
        partBuf.put(arr, 0, count).position(0);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, count * 4, partBuf, GLES20.GL_STREAM_DRAW);
    }

    private void draw9(int verts) {
        GLES20.glEnableVertexAttribArray(0); GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 36, 0);
        GLES20.glEnableVertexAttribArray(1); GLES20.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 36, 12);
        GLES20.glEnableVertexAttribArray(2); GLES20.glVertexAttribPointer(2, 4, GLES20.GL_FLOAT, false, 36, 20);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, verts);
    }

    // ------------------------------------------------------------------ post
    private void drawPost(float aspect) {
        Game g = game;
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glUseProgram(pPost); resetAttribs();
        uni1(pPost, "uTime", time);
        uni1(pPost, "uDmg", Math.max(0f, Math.min(1f, g.dmgFlash)));
        float hp = g.health / g.maxHealth;
        uni1(pPost, "uLow", hp < 0.4f ? (0.4f - hp) / 0.4f : 0f);
        uni1(pPost, "uFlash", g.lightning);
        uni1(pPost, "uAspect", aspect);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, quadVbo);
        GLES20.glEnableVertexAttribArray(0);
        GLES20.glVertexAttribPointer(0, 2, GLES20.GL_FLOAT, false, 8, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
        GLES20.glDisableVertexAttribArray(0);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
    }
}

package com.craterz;

/** Matrices 4x4 en columnas (formato OpenGL). Sin dependencias de Android. */
public final class Mat4 {
    private static final float[] T = new float[16];

    private Mat4() {}

    public static void identity(float[] m) {
        for (int i = 0; i < 16; i++) m[i] = 0;
        m[0] = m[5] = m[10] = m[15] = 1;
    }

    public static synchronized void multiply(float[] r, float[] a, float[] b) {
        for (int c = 0; c < 4; c++) {
            for (int row = 0; row < 4; row++) {
                float s = 0;
                for (int k = 0; k < 4; k++) s += a[k * 4 + row] * b[c * 4 + k];
                T[c * 4 + row] = s;
            }
        }
        System.arraycopy(T, 0, r, 0, 16);
    }

    public static void perspective(float[] m, float fovyDeg, float aspect, float n, float f) {
        float t = (float) (1.0 / Math.tan(Math.toRadians(fovyDeg) * 0.5));
        for (int i = 0; i < 16; i++) m[i] = 0;
        m[0] = t / aspect;
        m[5] = t;
        m[10] = (f + n) / (n - f);
        m[11] = -1;
        m[14] = 2 * f * n / (n - f);
    }

    public static void ortho(float[] m, float l, float r, float b, float t, float n, float f) {
        for (int i = 0; i < 16; i++) m[i] = 0;
        m[0] = 2 / (r - l);
        m[5] = 2 / (t - b);
        m[10] = -2 / (f - n);
        m[12] = -(r + l) / (r - l);
        m[13] = -(t + b) / (t - b);
        m[14] = -(f + n) / (f - n);
        m[15] = 1;
    }

    public static void lookAt(float[] m, float ex, float ey, float ez,
                              float cx, float cy, float cz, float ux, float uy, float uz) {
        float fx = cx - ex, fy = cy - ey, fz = cz - ez;
        float fl = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        fx /= fl; fy /= fl; fz /= fl;
        float sx = fy * uz - fz * uy, sy = fz * ux - fx * uz, sz = fx * uy - fy * ux;
        float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        sx /= sl; sy /= sl; sz /= sl;
        float vx = sy * fz - sz * fy, vy = sz * fx - sx * fz, vz = sx * fy - sy * fx;
        m[0] = sx; m[4] = sy; m[8] = sz; m[12] = 0;
        m[1] = vx; m[5] = vy; m[9] = vz; m[13] = 0;
        m[2] = -fx; m[6] = -fy; m[10] = -fz; m[14] = 0;
        m[3] = 0; m[7] = 0; m[11] = 0; m[15] = 1;
        translate(m, -ex, -ey, -ez);
    }

    /** m = m * T(x,y,z) */
    public static void translate(float[] m, float x, float y, float z) {
        for (int i = 0; i < 4; i++) {
            m[12 + i] += m[i] * x + m[4 + i] * y + m[8 + i] * z;
        }
    }

    public static void scale(float[] m, float x, float y, float z) {
        for (int i = 0; i < 4; i++) {
            m[i] *= x; m[4 + i] *= y; m[8 + i] *= z;
        }
    }

    public static void rotateX(float[] m, float deg) {
        float a = (float) Math.toRadians(deg), c = (float) Math.cos(a), s = (float) Math.sin(a);
        for (int i = 0; i < 4; i++) {
            float y = m[4 + i], z = m[8 + i];
            m[4 + i] = y * c + z * s;
            m[8 + i] = -y * s + z * c;
        }
    }

    public static void rotateY(float[] m, float deg) {
        float a = (float) Math.toRadians(deg), c = (float) Math.cos(a), s = (float) Math.sin(a);
        for (int i = 0; i < 4; i++) {
            float x = m[i], z = m[8 + i];
            m[i] = x * c - z * s;
            m[8 + i] = x * s + z * c;
        }
    }

    public static void rotateZ(float[] m, float deg) {
        float a = (float) Math.toRadians(deg), c = (float) Math.cos(a), s = (float) Math.sin(a);
        for (int i = 0; i < 4; i++) {
            float x = m[i], y = m[4 + i];
            m[i] = x * c + y * s;
            m[4 + i] = -x * s + y * c;
        }
    }

    public static void point(float[] m, float x, float y, float z, float[] out) {
        float ox = m[0] * x + m[4] * y + m[8] * z + m[12];
        float oy = m[1] * x + m[5] * y + m[9] * z + m[13];
        float oz = m[2] * x + m[6] * y + m[10] * z + m[14];
        out[0] = ox; out[1] = oy; out[2] = oz;
    }

    public static void dir(float[] m, float x, float y, float z, float[] out) {
        float ox = m[0] * x + m[4] * y + m[8] * z;
        float oy = m[1] * x + m[5] * y + m[9] * z;
        float oz = m[2] * x + m[6] * y + m[10] * z;
        out[0] = ox; out[1] = oy; out[2] = oz;
    }
}

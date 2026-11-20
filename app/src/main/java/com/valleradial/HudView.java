package com.valleradial;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/** Capa encima del 3D: manos, barra de vida, mira, mensajes y controles tactiles. */
final class HudView extends View {
    private final Game game;
    private final Hands hands = new Hands();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float u = 1f;

    // punteros
    private int stickId = -1, lookId = -1, fireId = -1, hookId = -1, jumpId = -1;
    private float stickCx, stickCy, stickX, stickY, lookLastX, lookLastY;
    private static final float STICK_R = 52f;

    HudView(Context ctx, Game g) {
        super(ctx);
        game = g;
        setWillNotDraw(false);
        setBackgroundColor(0x00000000);
        text.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        text.setColor(0xFF141212);
    }

    // posiciones de botones (en pixeles)
    private float fireX() { return getWidth() - 62f * u; }
    private float fireY() { return 150f * u; }
    private float hookX() { return getWidth() - 140f * u; }
    private float hookY() { return 214f * u; }
    private float jumpX() { return getWidth() - 56f * u; }
    private float jumpY() { return 236f * u + 8f * u; }
    private static final float FIRE_R = 36f, HOOK_R = 31f, JUMP_R = 27f;

    private boolean inCircle(float x, float y, float cx, float cy, float r) {
        float dx = x - cx, dy = y - cy;
        return dx * dx + dy * dy <= r * r;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int act = e.getActionMasked();
        int idx = e.getActionIndex();
        switch (act) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int id = e.getPointerId(idx);
                float x = e.getX(idx), y = e.getY(idx);
                if (inCircle(x, y, fireX(), fireY(), FIRE_R * u * 1.25f) && fireId < 0) {
                    fireId = id; game.fireHeld = true;
                } else if (inCircle(x, y, hookX(), hookY(), HOOK_R * u * 1.25f) && hookId < 0) {
                    hookId = id; game.hookHeld = true;
                } else if (inCircle(x, y, jumpX(), jumpY(), JUMP_R * u * 1.25f) && jumpId < 0) {
                    jumpId = id; game.jumpHeld = true;
                } else if (x < getWidth() * 0.38f && stickId < 0) {
                    stickId = id; stickCx = x; stickCy = y; stickX = x; stickY = y;
                    game.moveX = 0; game.moveY = 0;
                } else if (lookId < 0) {
                    lookId = id; lookLastX = x; lookLastY = y;
                }
                break;
            }
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < e.getPointerCount(); i++) {
                    int id = e.getPointerId(i);
                    float x = e.getX(i), y = e.getY(i);
                    if (id == stickId) {
                        stickX = x; stickY = y;
                        float r = STICK_R * u;
                        float dx = (x - stickCx) / r, dy = (y - stickCy) / r;
                        float l = (float) Math.sqrt(dx * dx + dy * dy);
                        if (l > 1f) { dx /= l; dy /= l; }
                        game.moveX = dx;
                        game.moveY = -dy;
                    } else if (id == lookId) {
                        float k = 3.4f / Math.max(1f, getWidth());
                        game.addLook((x - lookLastX) * k, -(y - lookLastY) * k * 1.1f);
                        lookLastX = x; lookLastY = y;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                int id = e.getPointerId(idx);
                if (act == MotionEvent.ACTION_CANCEL) {
                    releaseAll();
                    break;
                }
                if (id == stickId) { stickId = -1; game.moveX = 0; game.moveY = 0; }
                if (id == lookId) lookId = -1;
                if (id == fireId) { fireId = -1; game.fireHeld = false; }
                if (id == hookId) { hookId = -1; game.hookHeld = false; }
                if (id == jumpId) { jumpId = -1; game.jumpHeld = false; }
                break;
            }
            default:
                break;
        }
        return true;
    }

    private void releaseAll() {
        stickId = lookId = fireId = hookId = jumpId = -1;
        game.moveX = 0; game.moveY = 0;
        game.fireHeld = false; game.hookHeld = false; game.jumpHeld = false;
    }

    @Override
    protected void onDraw(Canvas c) {
        float W = getWidth(), H = getHeight();
        u = H / 360f;
        float time = game.time;

        // si el gancho esta pegado, proyecta el ancla a la pantalla (espacio de referencia)
        if (game.hookState != Game.HOOK_NONE) {
            float[] m = game.vp;
            float x = game.hax, y = game.hay, z = game.haz;
            float cx = m[0] * x + m[4] * y + m[8] * z + m[12];
            float cy = m[1] * x + m[5] * y + m[9] * z + m[13];
            float cw = m[3] * x + m[7] * y + m[11] * z + m[15];
            if (cw > 0.1f) {
                float sx = (cx / cw * 0.5f + 0.5f) * W;
                float sy = (1f - (cy / cw * 0.5f + 0.5f)) * H;
                hands.setAnchor(sx / u, sy / u);
            } else {
                hands.setAnchor(-1f, -1f);
            }
        }

        // destello al recibir dano
        if (game.hurtFlash > 0f) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(((int) (game.hurtFlash / 0.35f * 90f) << 24) | 0xD6477F);
            c.drawRect(0, 0, W, H, p);
        }

        drawSpeedLines(c, W, H);
        hands.drawHealth(c, H, game.hp, Game.MAX_HP, game.hurtFlash);
        hands.draw(c, W, H, game, time);
        drawCrosshair(c, W, H);
        drawControls(c, W, H);

        // marcador y mensajes
        text.setTextSize(15f * u);
        text.setTextAlign(Paint.Align.LEFT);
        text.setColor(0xFF141212);
        c.drawText("Manchas: " + game.score, 14f * u, 24f * u, text);
        if (game.messageT > 0f) {
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(26f * u);
            c.drawText(game.message, W / 2f, 70f * u, text);
            if (game.messageT > 1f && "Valle Radial".equals(game.message)) {
                text.setTextSize(12f * u);
                c.drawText("Izq: mover  |  Der: mirar  |  GANCHO: lanza el kunai (mantén para jalar)", W / 2f, 92f * u, text);
            }
        }
        postInvalidateOnAnimation();
    }

    /** Rayitas radiales al ir muy rapido (como el desenfoque de movimiento del video). */
    private void drawSpeedLines(Canvas c, float W, float H) {
        float sp = game.speed();
        if (sp < 20f) return;
        float k = Math.min(1f, (sp - 20f) / 30f);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(1.6f * u);
        p.setColor(((int) (110 * k) << 24) | 0x5A4A1A);
        float cx = W / 2f, cy = H / 2f;
        float t = game.time;
        for (int i = 0; i < 26; i++) {
            float a = i * 0.2417f + (float) Math.sin(i * 7.1f) * 0.1f;
            float r0 = (0.42f + 0.1f * (float) Math.sin(t * 9f + i)) * H;
            float r1 = r0 + (0.18f + 0.1f * k) * H;
            float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
            c.drawLine(cx + ca * r0 * 1.5f, cy + sa * r0, cx + ca * r1 * 1.5f, cy + sa * r1, p);
        }
        p.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawCrosshair(Canvas c, float W, float H) {
        float cx = W / 2f, cy = H / 2f;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3.2f * u);
        p.setColor(0xFF141212);
        c.drawCircle(cx, cy, 4f * u, p);
        p.setStrokeWidth(1.6f * u);
        p.setColor(0xFFFFFFFF);
        c.drawCircle(cx, cy, 4f * u, p);
        if (game.hitMarker > 0f) {
            p.setColor(0xFF141212);
            p.setStrokeWidth(2.6f * u);
            float a = 7f * u, b = 14f * u;
            c.drawLine(cx - b, cy - b, cx - a, cy - a, p);
            c.drawLine(cx + b, cy - b, cx + a, cy - a, p);
            c.drawLine(cx - b, cy + b, cx - a, cy + a, p);
            c.drawLine(cx + b, cy + b, cx + a, cy + a, p);
        }
    }

    private void drawBtn(Canvas c, float x, float y, float r, boolean on, String label) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(on ? 0x99D6477F : 0x44FFFFFF);
        c.drawCircle(x, y, r, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2.2f * u);
        p.setColor(0xCC141212);
        c.drawCircle(x, y, r, p);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(r * 0.36f);
        text.setColor(0xDD141212);
        c.drawText(label, x, y + r * 0.13f, text);
    }

    private void drawControls(Canvas c, float W, float H) {
        drawBtn(c, fireX(), fireY(), FIRE_R * u, fireId >= 0, "FUEGO");
        drawBtn(c, hookX(), hookY(), HOOK_R * u, hookId >= 0, "GANCHO");
        drawBtn(c, jumpX(), jumpY(), JUMP_R * u, jumpId >= 0, "SALTO");
        float r = STICK_R * u;
        float bx = stickId >= 0 ? stickCx : 92f * u, by = stickId >= 0 ? stickCy : 276f * u;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2.2f * u);
        p.setColor(0x88141212);
        c.drawCircle(bx, by, r, p);
        if (stickId >= 0) {
            float dx = stickX - stickCx, dy = stickY - stickCy;
            float l = (float) Math.sqrt(dx * dx + dy * dy);
            if (l > r) { dx *= r / l; dy *= r / l; }
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x88FFFFFF);
            c.drawCircle(bx + dx, by + dy, r * 0.42f, p);
            p.setStyle(Paint.Style.STROKE);
            p.setColor(0xAA141212);
            c.drawCircle(bx + dx, by + dy, r * 0.42f, p);
        }
    }
}

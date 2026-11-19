package com.lumina;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** Íconos dibujados por código (sin imágenes), centrados en (cx, cy) dentro de un cuadro de lado s. */
final class Icons {
    private Icons() {}

    static final int PLAY = 0, PAUSE = 1, NEXT = 2, PREV = 3, SHUFFLE = 4, REPEAT = 5, REPEAT_ONE = 6,
            SEARCH = 7, FOLDER = 8, CLOSE = 9, CHEVRON_DOWN = 10, NOTE = 11, PLUS = 12, TRASH = 13,
            PHONE = 14, CHECK = 15, BACK = 16, REFRESH = 17, IMAGE = 18, PALETTE = 19,
            HEART = 20, HEART_FILLED = 21, THUMB = 22, THUMB_FILLED = 23, TRENDING = 24, CLOCK = 25;

    private static final Path path = new Path();
    private static final RectF rf = new RectF();

    private static void fill(Paint p, int color) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        p.setShader(null);
    }

    private static void stroke(Paint p, int color, float w) {
        p.setStyle(Paint.Style.STROKE);
        p.setColor(color);
        p.setStrokeWidth(w);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setShader(null);
    }

    private static void arrowHead(Canvas c, Paint p, float x, float y, float dir, float s, int color) {
        fill(p, color);
        path.reset();
        path.moveTo(x + dir * 0.12f * s, y);
        path.lineTo(x - dir * 0.02f * s, y - 0.12f * s);
        path.lineTo(x - dir * 0.02f * s, y + 0.12f * s);
        path.close();
        c.drawPath(path, p);
    }

    static void draw(Canvas c, Paint p, int kind, float cx, float cy, float s, int color) {
        c.save();
        c.translate(cx, cy);
        float sw = Math.max(1.5f, s * 0.085f);
        switch (kind) {
            case PLAY: {
                fill(p, color);
                path.reset();
                path.moveTo(-0.26f * s, -0.36f * s);
                path.lineTo(0.38f * s, 0f);
                path.lineTo(-0.26f * s, 0.36f * s);
                path.close();
                c.drawPath(path, p);
                stroke(p, color, s * 0.1f);
                c.drawPath(path, p);
                break;
            }
            case PAUSE: {
                fill(p, color);
                rf.set(-0.28f * s, -0.34f * s, -0.07f * s, 0.34f * s);
                c.drawRoundRect(rf, s * 0.06f, s * 0.06f, p);
                rf.set(0.07f * s, -0.34f * s, 0.28f * s, 0.34f * s);
                c.drawRoundRect(rf, s * 0.06f, s * 0.06f, p);
                break;
            }
            case NEXT:
            case PREV: {
                float d = kind == NEXT ? 1 : -1;
                fill(p, color);
                path.reset();
                path.moveTo(d * -0.34f * s, -0.3f * s);
                path.lineTo(d * 0.2f * s, 0f);
                path.lineTo(d * -0.34f * s, 0.3f * s);
                path.close();
                c.drawPath(path, p);
                stroke(p, color, s * 0.08f);
                c.drawPath(path, p);
                fill(p, color);
                if (kind == NEXT) rf.set(0.2f * s, -0.3f * s, 0.32f * s, 0.3f * s);
                else rf.set(-0.32f * s, -0.3f * s, -0.2f * s, 0.3f * s);
                c.drawRoundRect(rf, s * 0.04f, s * 0.04f, p);
                break;
            }
            case SHUFFLE: {
                stroke(p, color, sw);
                path.reset();
                path.moveTo(-0.4f * s, -0.24f * s);
                path.cubicTo(-0.05f * s, -0.24f * s, -0.05f * s, 0.24f * s, 0.28f * s, 0.24f * s);
                c.drawPath(path, p);
                path.reset();
                path.moveTo(-0.4f * s, 0.24f * s);
                path.cubicTo(-0.05f * s, 0.24f * s, -0.05f * s, -0.24f * s, 0.28f * s, -0.24f * s);
                c.drawPath(path, p);
                arrowHead(c, p, 0.3f * s, 0.24f * s, 1, s, color);
                arrowHead(c, p, 0.3f * s, -0.24f * s, 1, s, color);
                break;
            }
            case REPEAT:
            case REPEAT_ONE: {
                stroke(p, color, sw);
                path.reset();
                path.moveTo(-0.12f * s, -0.26f * s);
                path.lineTo(0.22f * s, -0.26f * s);
                path.quadTo(0.4f * s, -0.26f * s, 0.4f * s, -0.08f * s);
                path.lineTo(0.4f * s, 0f);
                c.drawPath(path, p);
                path.reset();
                path.moveTo(0.12f * s, 0.26f * s);
                path.lineTo(-0.22f * s, 0.26f * s);
                path.quadTo(-0.4f * s, 0.26f * s, -0.4f * s, 0.08f * s);
                path.lineTo(-0.4f * s, 0f);
                c.drawPath(path, p);
                arrowHead(c, p, -0.1f * s, -0.26f * s, -1, s, color);
                arrowHead(c, p, 0.1f * s, 0.26f * s, 1, s, color);
                if (kind == REPEAT_ONE) {
                    fill(p, color);
                    p.setTextAlign(Paint.Align.CENTER);
                    p.setTextSize(s * 0.34f);
                    p.setFakeBoldText(true);
                    c.drawText("1", 0, s * 0.12f, p);
                    p.setFakeBoldText(false);
                }
                break;
            }
            case SEARCH: {
                stroke(p, color, sw * 1.15f);
                c.drawCircle(-0.08f * s, -0.08f * s, 0.27f * s, p);
                c.drawLine(0.12f * s, 0.12f * s, 0.38f * s, 0.38f * s, p);
                break;
            }
            case FOLDER: {
                stroke(p, color, sw);
                path.reset();
                path.moveTo(-0.4f * s, -0.26f * s);
                path.lineTo(-0.12f * s, -0.26f * s);
                path.lineTo(0f, -0.13f * s);
                path.lineTo(0.4f * s, -0.13f * s);
                path.lineTo(0.4f * s, 0.3f * s);
                path.lineTo(-0.4f * s, 0.3f * s);
                path.close();
                c.drawPath(path, p);
                break;
            }
            case CLOSE: {
                stroke(p, color, sw * 1.1f);
                c.drawLine(-0.24f * s, -0.24f * s, 0.24f * s, 0.24f * s, p);
                c.drawLine(0.24f * s, -0.24f * s, -0.24f * s, 0.24f * s, p);
                break;
            }
            case CHEVRON_DOWN: {
                stroke(p, color, sw * 1.2f);
                path.reset();
                path.moveTo(-0.3f * s, -0.1f * s);
                path.lineTo(0f, 0.2f * s);
                path.lineTo(0.3f * s, -0.1f * s);
                c.drawPath(path, p);
                break;
            }
            case BACK: {
                stroke(p, color, sw * 1.2f);
                path.reset();
                path.moveTo(0.1f * s, -0.3f * s);
                path.lineTo(-0.2f * s, 0f);
                path.lineTo(0.1f * s, 0.3f * s);
                c.drawPath(path, p);
                break;
            }
            case NOTE: {
                fill(p, color);
                rf.set(-0.3f * s, 0.1f * s, 0.02f * s, 0.34f * s);
                c.drawOval(rf, p);
                stroke(p, color, s * 0.075f);
                c.drawLine(0f, 0.22f * s, 0f, -0.34f * s, p);
                path.reset();
                path.moveTo(0f, -0.34f * s);
                path.cubicTo(0.12f * s, -0.28f * s, 0.3f * s, -0.22f * s, 0.26f * s, -0.04f * s);
                c.drawPath(path, p);
                break;
            }
            case PLUS: {
                stroke(p, color, sw * 1.2f);
                c.drawLine(-0.28f * s, 0f, 0.28f * s, 0f, p);
                c.drawLine(0f, -0.28f * s, 0f, 0.28f * s, p);
                break;
            }
            case TRASH: {
                stroke(p, color, sw);
                c.drawLine(-0.3f * s, -0.22f * s, 0.3f * s, -0.22f * s, p);
                c.drawLine(-0.1f * s, -0.22f * s, -0.1f * s, -0.32f * s, p);
                c.drawLine(0.1f * s, -0.22f * s, 0.1f * s, -0.32f * s, p);
                c.drawLine(-0.1f * s, -0.32f * s, 0.1f * s, -0.32f * s, p);
                path.reset();
                path.moveTo(-0.22f * s, -0.22f * s);
                path.lineTo(-0.18f * s, 0.34f * s);
                path.lineTo(0.18f * s, 0.34f * s);
                path.lineTo(0.22f * s, -0.22f * s);
                c.drawPath(path, p);
                break;
            }
            case PHONE: {
                stroke(p, color, sw);
                rf.set(-0.2f * s, -0.36f * s, 0.2f * s, 0.36f * s);
                c.drawRoundRect(rf, s * 0.07f, s * 0.07f, p);
                c.drawLine(-0.06f * s, 0.26f * s, 0.06f * s, 0.26f * s, p);
                break;
            }
            case CHECK: {
                stroke(p, color, sw * 1.4f);
                path.reset();
                path.moveTo(-0.28f * s, 0.02f * s);
                path.lineTo(-0.08f * s, 0.22f * s);
                path.lineTo(0.3f * s, -0.2f * s);
                c.drawPath(path, p);
                break;
            }
            case REFRESH: {
                stroke(p, color, sw);
                rf.set(-0.3f * s, -0.3f * s, 0.3f * s, 0.3f * s);
                c.drawArc(rf, -40, 280, false, p);
                arrowHead(c, p, 0.24f * s, -0.22f * s, 1, s * 1.2f, color);
                break;
            }
            case IMAGE: {
                stroke(p, color, sw);
                rf.set(-0.38f * s, -0.3f * s, 0.38f * s, 0.3f * s);
                c.drawRoundRect(rf, s * 0.08f, s * 0.08f, p);
                fill(p, color);
                c.drawCircle(-0.16f * s, -0.1f * s, 0.07f * s, p);
                stroke(p, color, sw);
                path.reset();
                path.moveTo(-0.38f * s, 0.2f * s);
                path.lineTo(-0.1f * s, -0.02f * s);
                path.lineTo(0.1f * s, 0.14f * s);
                path.lineTo(0.2f * s, 0.06f * s);
                path.lineTo(0.38f * s, 0.22f * s);
                c.drawPath(path, p);
                break;
            }
            case PALETTE: {
                stroke(p, color, sw);
                c.drawCircle(0f, 0f, 0.36f * s, p);
                fill(p, color);
                c.drawCircle(-0.14f * s, -0.14f * s, 0.06f * s, p);
                c.drawCircle(0.12f * s, -0.17f * s, 0.06f * s, p);
                c.drawCircle(0.2f * s, 0.06f * s, 0.06f * s, p);
                c.drawCircle(-0.2f * s, 0.08f * s, 0.06f * s, p);
                break;
            }
            case HEART:
            case HEART_FILLED: {
                path.reset();
                path.moveTo(0f, 0.34f * s);
                path.cubicTo(-0.56f * s, -0.04f * s, -0.34f * s, -0.42f * s, 0f, -0.15f * s);
                path.cubicTo(0.34f * s, -0.42f * s, 0.56f * s, -0.04f * s, 0f, 0.34f * s);
                path.close();
                if (kind == HEART_FILLED) {
                    fill(p, color);
                    c.drawPath(path, p);
                }
                stroke(p, color, sw);
                c.drawPath(path, p);
                break;
            }
            case THUMB:
            case THUMB_FILLED: {
                path.reset();
                path.moveTo(-0.12f * s, 0.32f * s);
                path.lineTo(-0.12f * s, -0.04f * s);
                path.lineTo(0.05f * s, -0.34f * s);
                path.quadTo(0.22f * s, -0.34f * s, 0.18f * s, -0.12f * s);
                path.lineTo(0.16f * s, -0.06f * s);
                path.lineTo(0.3f * s, -0.06f * s);
                path.quadTo(0.42f * s, -0.06f * s, 0.38f * s, 0.08f * s);
                path.lineTo(0.32f * s, 0.26f * s);
                path.quadTo(0.28f * s, 0.32f * s, 0.2f * s, 0.32f * s);
                path.close();
                rf.set(-0.38f * s, -0.04f * s, -0.2f * s, 0.32f * s);
                if (kind == THUMB_FILLED) {
                    fill(p, color);
                    c.drawPath(path, p);
                    c.drawRoundRect(rf, s * 0.04f, s * 0.04f, p);
                }
                stroke(p, color, sw);
                c.drawPath(path, p);
                c.drawRoundRect(rf, s * 0.04f, s * 0.04f, p);
                break;
            }
            case TRENDING: {
                stroke(p, color, sw * 1.2f);
                path.reset();
                path.moveTo(-0.38f * s, 0.22f * s);
                path.lineTo(-0.1f * s, -0.06f * s);
                path.lineTo(0.06f * s, 0.1f * s);
                path.lineTo(0.34f * s, -0.2f * s);
                c.drawPath(path, p);
                path.reset();
                path.moveTo(0.14f * s, -0.22f * s);
                path.lineTo(0.36f * s, -0.22f * s);
                path.lineTo(0.36f * s, 0f);
                c.drawPath(path, p);
                break;
            }
            case CLOCK: {
                stroke(p, color, sw);
                c.drawCircle(0f, 0f, 0.36f * s, p);
                c.drawLine(0f, 0f, 0f, -0.2f * s, p);
                c.drawLine(0f, 0f, 0.15f * s, 0.1f * s, p);
                break;
            }
            default:
                break;
        }
        c.restore();
    }
}

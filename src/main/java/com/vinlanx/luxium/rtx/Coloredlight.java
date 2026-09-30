/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.util.Mth
 */
package com.vinlanx.luxium.rtx;

import com.vinlanx.luxium.mixin.LightTextureAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class Coloredlight {
    private static volatile boolean worldRenderActive = false;
    private static volatile boolean enabled = false;
    private static volatile int red = 255;
    private static volatile int green = 255;
    private static volatile int blue = 255;
    private static volatile AnimationMode mode = AnimationMode.NONE;
    private static volatile int spectreSpeed = 50;
    private static volatile long animationStartTime = 0L;

    private Coloredlight() {
    }

    public static void setColor(int newRed, int newGreen, int newBlue) {
        mode = AnimationMode.NONE;
        red = Mth.m_14045_((int)newRed, (int)0, (int)255);
        green = Mth.m_14045_((int)newGreen, (int)0, (int)255);
        blue = Mth.m_14045_((int)newBlue, (int)0, (int)255);
        enabled = true;
        Coloredlight.requestLightTextureRefresh();
    }

    public static void enableSpectre(int speed) {
        mode = AnimationMode.SPECTRE;
        spectreSpeed = Mth.m_14045_((int)speed, (int)1, (int)100);
        animationStartTime = System.currentTimeMillis();
        enabled = true;
        Coloredlight.requestLightTextureRefresh();
    }

    public static void enablePolice() {
        mode = AnimationMode.POLICE;
        animationStartTime = System.currentTimeMillis();
        enabled = true;
        Coloredlight.requestLightTextureRefresh();
    }

    public static void enablePolice2() {
        mode = AnimationMode.POLICE2;
        animationStartTime = System.currentTimeMillis();
        enabled = true;
        Coloredlight.requestLightTextureRefresh();
    }

    public static void disableSpectre() {
        Coloredlight.reset();
    }

    public static void reset() {
        mode = AnimationMode.NONE;
        red = 255;
        green = 255;
        blue = 255;
        enabled = false;
        Coloredlight.requestLightTextureRefresh();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void beginWorldRender() {
        worldRenderActive = true;
        if (mode == AnimationMode.SPECTRE) {
            Coloredlight.updateSpectreColors();
        } else if (mode == AnimationMode.POLICE) {
            Coloredlight.updatePoliceColors();
        } else if (mode == AnimationMode.POLICE2) {
            Coloredlight.updatePolice2Colors();
        }
    }

    public static void endWorldRender() {
        worldRenderActive = false;
    }

    public static boolean isWorldRenderActive() {
        return worldRenderActive;
    }

    public static int getRed() {
        return red;
    }

    public static int getGreen() {
        return green;
    }

    public static int getBlue() {
        return blue;
    }

    public static float getRedMultiplier() {
        return (float)red / 255.0f;
    }

    public static float getGreenMultiplier() {
        return (float)green / 255.0f;
    }

    public static float getBlueMultiplier() {
        return (float)blue / 255.0f;
    }

    private static void updateSpectreColors() {
        long elapsed = System.currentTimeMillis() - animationStartTime;
        double cycleDuration = 10000.0 / (double)spectreSpeed;
        double phase = (double)(elapsed % (long)cycleDuration) / cycleDuration;
        float hue = (float)phase;
        float saturation = 1.0f;
        float value = 1.0f;
        int rgb = Coloredlight.hsvToRgb(hue, saturation, value);
        red = rgb >> 16 & 0xFF;
        green = rgb >> 8 & 0xFF;
        blue = rgb & 0xFF;
        Coloredlight.requestLightTextureRefresh();
    }

    private static void updatePoliceColors() {
        long elapsed = System.currentTimeMillis() - animationStartTime;
        long cycleTime = elapsed % 400L;
        if (cycleTime < 200L) {
            red = 255;
            green = 0;
            blue = 0;
        } else {
            red = 0;
            green = 0;
            blue = 255;
        }
        Coloredlight.requestLightTextureRefresh();
    }

    private static void updatePolice2Colors() {
        long elapsed = System.currentTimeMillis() - animationStartTime;
        long cycleTime = elapsed % 2480L;
        if (cycleTime < 240L) {
            long flashTime = cycleTime % 80L;
            if (flashTime < 40L) {
                red = 255;
                green = 0;
                blue = 0;
            } else {
                red = 1;
                green = 1;
                blue = 1;
            }
        } else if (cycleTime < 1240L) {
            red = 1;
            green = 1;
            blue = 1;
        } else if (cycleTime < 1480L) {
            long flashTime = (cycleTime - 1240L) % 80L;
            if (flashTime < 40L) {
                red = 0;
                green = 0;
                blue = 255;
            } else {
                red = 1;
                green = 1;
                blue = 1;
            }
        } else {
            red = 1;
            green = 1;
            blue = 1;
        }
        Coloredlight.requestLightTextureRefresh();
    }

    private static int hsvToRgb(float hue, float saturation, float value) {
        float b;
        float g;
        float r;
        int h = (int)(hue * 6.0f);
        float f = hue * 6.0f - (float)h;
        float p = value * (1.0f - saturation);
        float q = value * (1.0f - f * saturation);
        float t = value * (1.0f - (1.0f - f) * saturation);
        switch (h % 6) {
            case 0: {
                r = value;
                g = t;
                b = p;
                break;
            }
            case 1: {
                r = q;
                g = value;
                b = p;
                break;
            }
            case 2: {
                r = p;
                g = value;
                b = t;
                break;
            }
            case 3: {
                r = p;
                g = q;
                b = value;
                break;
            }
            case 4: {
                r = t;
                g = p;
                b = value;
                break;
            }
            case 5: {
                r = value;
                g = p;
                b = q;
                break;
            }
            default: {
                b = 0.0f;
                g = 0.0f;
                r = 0.0f;
            }
        }
        int ri = (int)(r * 255.0f);
        int gi = (int)(g * 255.0f);
        int bi = (int)(b * 255.0f);
        return ri << 16 | gi << 8 | bi;
    }

    private static void requestLightTextureRefresh() {
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91063_ != null) {
            ((LightTextureAccessor)mc.f_91063_.m_109154_()).luxium$setUpdateLightTexture(true);
        }
    }

    private static enum AnimationMode {
        NONE,
        SPECTRE,
        POLICE,
        POLICE2;

    }
}


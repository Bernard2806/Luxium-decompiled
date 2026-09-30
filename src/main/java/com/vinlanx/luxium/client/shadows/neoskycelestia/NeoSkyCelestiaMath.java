/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package com.vinlanx.luxium.client.shadows.neoskycelestia;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class NeoSkyCelestiaMath {
    private static final Matrix4f BIAS_MATRIX = new Matrix4f(0.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.5f, 0.0f, 0.5f, 0.5f, 0.5f, 1.0f);

    private NeoSkyCelestiaMath() {
    }

    public static Matrix4f buildLightViewRotation(Vec3 normalizedToLight) {
        Vec3 lightForward = normalizedToLight.m_82490_(-1.0);
        Vector3f forward = new Vector3f((float)lightForward.f_82479_, (float)lightForward.f_82480_, (float)lightForward.f_82481_);
        return new Matrix4f().lookAlong((Vector3fc)forward, (Vector3fc)NeoSkyCelestiaMath.chooseStableUp(forward));
    }

    public static Vec3 computeSnappedCenter(Vec3 center, Vec3 toLight, float radius, int resolution) {
        Vec3 normalizedToLight = NeoSkyCelestiaMath.safeNormalize(toLight);
        Matrix4f rotation = NeoSkyCelestiaMath.buildLightViewRotation(normalizedToLight);
        return NeoSkyCelestiaMath.snapCenterToLightTexels(center, rotation, radius, resolution);
    }

    public static Matrix4f buildLightFromMainView(Vec3 mainCamera, Vec3 shadowEye, Matrix4f lightViewRotation, Matrix4f lightProjection, Matrix4f inverseMainViewRotation) {
        Vector3f mainCameraToEye = new Vector3f((float)(mainCamera.f_82479_ - shadowEye.f_82479_), (float)(mainCamera.f_82480_ - shadowEye.f_82480_), (float)(mainCamera.f_82481_ - shadowEye.f_82481_));
        return new Matrix4f((Matrix4fc)BIAS_MATRIX).mul((Matrix4fc)lightProjection).mul((Matrix4fc)lightViewRotation).translate((Vector3fc)mainCameraToEye).mul((Matrix4fc)inverseMainViewRotation);
    }

    static Vec3 snapCenterToLightTexels(Vec3 center, Matrix4f lightRotation, float radius, int resolution) {
        float texelWorldSize = radius * 2.0f / (float)Math.max(1, resolution);
        double anchorX = Math.floor(center.f_82479_ / 1024.0) * 1024.0;
        double anchorY = Math.floor(center.f_82480_ / 1024.0) * 1024.0;
        double anchorZ = Math.floor(center.f_82481_ / 1024.0) * 1024.0;
        Vector3f relative = new Vector3f((float)(center.f_82479_ - anchorX), (float)(center.f_82480_ - anchorY), (float)(center.f_82481_ - anchorZ));
        lightRotation.transformDirection(relative);
        relative.x = (float)Math.round(relative.x / texelWorldSize) * texelWorldSize;
        relative.y = (float)Math.round(relative.y / texelWorldSize) * texelWorldSize;
        new Matrix4f((Matrix4fc)lightRotation).invert().transformDirection(relative);
        return new Vec3(anchorX + (double)relative.x, anchorY + (double)relative.y, anchorZ + (double)relative.z);
    }

    private static Vector3f chooseStableUp(Vector3f forward) {
        Vector3f worldUp = new Vector3f(0.0f, 1.0f, 0.0f);
        return Math.abs(forward.dot((Vector3fc)worldUp)) > 0.94f ? new Vector3f(0.0f, 0.0f, 1.0f) : worldUp;
    }

    public static Vec3 safeNormalize(Vec3 vector) {
        double lengthSqr = vector.m_82556_();
        if (lengthSqr < 1.0E-12 || !Double.isFinite(lengthSqr)) {
            return new Vec3(0.0, 1.0, 0.0);
        }
        return vector.m_82490_(1.0 / Math.sqrt(lengthSqr));
    }

    public static float horizonFade(Vec3 toLight) {
        double height = Mth.m_14008_((double)(Math.abs(toLight.f_82480_) / 0.12), (double)0.0, (double)1.0);
        return (float)Mth.m_14197_((double)height);
    }
}


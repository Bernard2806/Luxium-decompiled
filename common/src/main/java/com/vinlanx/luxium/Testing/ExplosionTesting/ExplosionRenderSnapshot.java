package com.vinlanx.luxium.Testing.ExplosionTesting; final class ExplosionRenderSnapshot { final float[] x; final float[] y; final float[] z; final float[] r; final float[] g; final float[] b; final float[] alpha; final float[] size; final byte[] frame; int smokeCount;
    int totalCount;
    ExplosionRenderSnapshot() {
        this.x = new float[70000];
        this.y = new float[70000];

        this.z = new float[70000];
        this.r = new float[70000];
        this.g = new float[70000];
        this.b = new float[70000];
        this.alpha = new float[70000];
        this.size = new float[70000];
        this.frame = new byte[70000]; }
}

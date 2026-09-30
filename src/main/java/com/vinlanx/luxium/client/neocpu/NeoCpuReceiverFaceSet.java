/*
 * Decompiled with CFR 0.152.
 */
package com.vinlanx.luxium.client.neocpu;

import com.vinlanx.luxium.client.neocpu.NeoCpuFloodBoundaryFace;
import com.vinlanx.luxium.client.neocpu.NeoCpuShadowTypes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

final class NeoCpuReceiverFaceSet {
    private final HashSet<Key> seen = new HashSet();
    private final ArrayList<NeoCpuFloodBoundaryFace> faces = new ArrayList(256);

    NeoCpuReceiverFaceSet() {
    }

    void add(NeoCpuShadowTypes.ReceiverSurface receiver, double distanceSq) {
        Key key = new Key(receiver.blockKey(), receiver.face().ordinal(), NeoCpuReceiverFaceSet.quantize(receiver.plane()), NeoCpuReceiverFaceSet.quantize(receiver.minU()), NeoCpuReceiverFaceSet.quantize(receiver.maxU()), NeoCpuReceiverFaceSet.quantize(receiver.minV()), NeoCpuReceiverFaceSet.quantize(receiver.maxV()));
        if (this.seen.add(key)) {
            this.faces.add(new NeoCpuFloodBoundaryFace(receiver, distanceSq));
        }
    }

    List<NeoCpuFloodBoundaryFace> faces() {
        this.faces.sort(Comparator.comparingDouble(NeoCpuFloodBoundaryFace::distanceToSourceSq));
        return this.faces;
    }

    private static long quantize(double value) {
        return Math.round(value * 1000000.0);
    }

    private record Key(long blockKey, int faceOrdinal, long plane, long minU, long maxU, long minV, long maxV) {
    }
}


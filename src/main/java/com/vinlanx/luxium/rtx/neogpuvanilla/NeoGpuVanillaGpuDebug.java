/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 *  org.lwjgl.opengl.ARBTimerQuery
 *  org.lwjgl.opengl.GL
 *  org.lwjgl.opengl.GL32C
 */
package com.vinlanx.luxium.rtx.neogpuvanilla;

import com.vinlanx.luxium.Config;
import com.vinlanx.luxium.rtx.neogpuvanilla.NeoGpuVanilla;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.ARBTimerQuery;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL32C;

public final class NeoGpuVanillaGpuDebug {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int GL_TIMESTAMP = 36392;
    private static final int GL_QUERY_RESULT = 34918;
    private static final int GL_QUERY_RESULT_AVAILABLE = 34919;
    private static final int MAX_PENDING_QUERIES = 64;
    private static final Deque<PendingQuery> PENDING = new ArrayDeque<PendingQuery>();
    private static boolean supportChecked;
    private static boolean timerSupported;
    private static int activeStartQuery;
    private static Kind activeKind;
    private static long windowStartNs;
    private static long captureGpuNs;
    private static long fineBakeGpuNs;
    private static int capturedFaces;
    private static int fineBakeCount;
    private static int latestDirtyFaces;
    private static int latestSources;
    private static int latestPixelsPerBlock;

    private NeoGpuVanillaGpuDebug() {
    }

    public static void beginCapture() {
        NeoGpuVanillaGpuDebug.begin(Kind.CAPTURE);
    }

    public static void endCapture() {
        NeoGpuVanillaGpuDebug.end(Kind.CAPTURE);
    }

    public static void beginFineBake() {
        NeoGpuVanillaGpuDebug.begin(Kind.FINE_BAKE);
    }

    public static void endFineBake() {
        NeoGpuVanillaGpuDebug.end(Kind.FINE_BAKE);
        if (NeoGpuVanillaGpuDebug.enabled()) {
            ++fineBakeCount;
        }
    }

    public static void recordCapturedFace() {
        if (NeoGpuVanillaGpuDebug.enabled()) {
            ++capturedFaces;
        }
    }

    public static void tick(int sources, int dirtyFaces, int pixelsPerBlock) {
        if (!NeoGpuVanillaGpuDebug.enabled()) {
            if (activeStartQuery != 0 || !PENDING.isEmpty() || windowStartNs != 0L) {
                NeoGpuVanillaGpuDebug.reset();
            }
            return;
        }
        latestSources = Math.max(0, sources);
        latestDirtyFaces = Math.max(0, dirtyFaces);
        latestPixelsPerBlock = Math.max(1, pixelsPerBlock);
        NeoGpuVanillaGpuDebug.pollResults();
        long now = System.nanoTime();
        if (windowStartNs == 0L) {
            windowStartNs = now;
            return;
        }
        if (now - windowStartNs < 1000000000L) {
            return;
        }
        String captureMs = String.format(Locale.ROOT, "%.2f", (double)captureGpuNs / 1000000.0);
        String fineBakeMs = String.format(Locale.ROOT, "%.2f", (double)fineBakeGpuNs / 1000000.0);
        LOGGER.info("[NeoGpuVanilla GPU] capture={}ms | fineBake={}ms | dirtyFaces={} | capturedFaces={} | sources={} | bakes={} | ppb={} | pendingQueries={}", (Object)captureMs, (Object)fineBakeMs, (Object)latestDirtyFaces, (Object)capturedFaces, (Object)latestSources, (Object)fineBakeCount, (Object)latestPixelsPerBlock, (Object)PENDING.size());
        captureGpuNs = 0L;
        fineBakeGpuNs = 0L;
        capturedFaces = 0;
        fineBakeCount = 0;
        windowStartNs = now;
    }

    public static void reset() {
        if (activeStartQuery != 0) {
            NeoGpuVanillaGpuDebug.deleteQuery(activeStartQuery);
            activeStartQuery = 0;
            activeKind = null;
        }
        while (!PENDING.isEmpty()) {
            PendingQuery query = PENDING.removeFirst();
            NeoGpuVanillaGpuDebug.deleteQuery(query.startId());
            NeoGpuVanillaGpuDebug.deleteQuery(query.endId());
        }
        windowStartNs = 0L;
        captureGpuNs = 0L;
        fineBakeGpuNs = 0L;
        capturedFaces = 0;
        fineBakeCount = 0;
        latestDirtyFaces = 0;
        latestSources = 0;
        latestPixelsPerBlock = 1;
    }

    private static void begin(Kind kind) {
        if (!NeoGpuVanillaGpuDebug.enabled() || !NeoGpuVanillaGpuDebug.timerSupported() || activeStartQuery != 0 || PENDING.size() >= 64) {
            return;
        }
        int query = GL32C.glGenQueries();
        if (query == 0) {
            return;
        }
        ARBTimerQuery.glQueryCounter((int)query, (int)36392);
        activeStartQuery = query;
        activeKind = kind;
    }

    private static boolean end(Kind expected) {
        if (activeStartQuery == 0 || activeKind != expected) {
            return false;
        }
        int endQuery = GL32C.glGenQueries();
        if (endQuery == 0) {
            NeoGpuVanillaGpuDebug.deleteQuery(activeStartQuery);
            activeStartQuery = 0;
            activeKind = null;
            return false;
        }
        ARBTimerQuery.glQueryCounter((int)endQuery, (int)36392);
        PENDING.addLast(new PendingQuery(activeStartQuery, endQuery, activeKind));
        activeStartQuery = 0;
        activeKind = null;
        return true;
    }

    private static void pollResults() {
        PendingQuery query;
        if (!NeoGpuVanillaGpuDebug.timerSupported()) {
            return;
        }
        int checks = PENDING.size();
        for (int i = 0; i < checks && (query = PENDING.peekFirst()) != null && GL32C.glGetQueryObjecti((int)query.endId(), (int)34919) != 0; ++i) {
            PENDING.removeFirst();
            long startNs = ARBTimerQuery.glGetQueryObjecti64((int)query.startId(), (int)34918);
            long endNs = ARBTimerQuery.glGetQueryObjecti64((int)query.endId(), (int)34918);
            NeoGpuVanillaGpuDebug.deleteQuery(query.startId());
            NeoGpuVanillaGpuDebug.deleteQuery(query.endId());
            long elapsedNs = Math.max(0L, endNs - startNs);
            if (query.kind() == Kind.CAPTURE) {
                captureGpuNs += elapsedNs;
                continue;
            }
            fineBakeGpuNs += elapsedNs;
        }
    }

    private static void deleteQuery(int query) {
        if (query == 0) {
            return;
        }
        try {
            GL32C.glDeleteQueries((int)query);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private static boolean timerSupported() {
        if (!supportChecked) {
            supportChecked = true;
            try {
                timerSupported = GL.getCapabilities().GL_ARB_timer_query;
            }
            catch (Throwable ignored) {
                timerSupported = false;
            }
            if (NeoGpuVanillaGpuDebug.enabled() && !timerSupported) {
                LOGGER.warn("[NeoGpuVanilla GPU] GL_ARB_timer_query is unavailable; GPU millisecond fields will stay at 0 while counters still log.");
            }
        }
        return timerSupported;
    }

    private static boolean enabled() {
        try {
            return Config.isFeatureEnabled(Config.CLIENT.neoGpuVanillaDebug) && NeoGpuVanilla.isConfiguredEnabled();
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private static enum Kind {
        CAPTURE,
        FINE_BAKE;

    }

    private record PendingQuery(int startId, int endId, Kind kind) {
    }
}


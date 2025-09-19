/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h.model;

import java.util.HashMap;
import java.util.Map;

public enum ActivationStatusEnum {
    STATUS_WAIT_CODE(-1),
    STATUS_WAIT_RETRY(3),
    STATUS_CANCEL(8),
    FINISHED(6),
    STATUS_OK(7);

    private final int id;
    private static final Map<Integer, ActivationStatusEnum> ID_TO_ENUM_MAP;

    private ActivationStatusEnum(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static int toId(ActivationStatusEnum status) {
        return status.id;
    }

    public static ActivationStatusEnum fromId(int id) {
        if (!ID_TO_ENUM_MAP.containsKey(id)) {
            return null;
        }
        return ID_TO_ENUM_MAP.get(id);
    }

    static {
        ID_TO_ENUM_MAP = new HashMap<Integer, ActivationStatusEnum>();
        for (ActivationStatusEnum status : ActivationStatusEnum.values()) {
            ID_TO_ENUM_MAP.put(status.id, status);
        }
    }
}

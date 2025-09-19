/**
 *
 * Copyright (c) 2022, 2023, AP Codes and/or its affiliates. All rights reserved.
 * AP Codes PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package br.com.store24h.store24h.services.core;

/**
 *
 * @author Archer (brainuxdev@gmail.com)
 */
public enum ActivationStatus {
    SOLICITADA(2),
    AGUARDANDO_MENSAGENS(3),
    FINALIZADA(5),
    RECEBIDA(11),
    CANCELADA(7);

    ActivationStatus(int i) {
    }
}

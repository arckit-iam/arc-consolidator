// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcconsolidator.domain.port;

import com.instantaccessprovider.arcconsolidator.dto.IdentityEventDTO;
import com.instantaccessprovider.arcconsolidator.dto.EntitlementEventDTO;
import com.instantaccessprovider.arcconsolidator.dto.AssignmentEventDTO;

/**
 * Port de sortie : publication d'un événement ARC normalisé (par type métier).
 * Implémenté par l'adapter Kafka dans l'infrastructure.
 */
public interface ArcEventOutputPort {
    void publishIdentity(IdentityEventDTO event);
    void publishEntitlement(EntitlementEventDTO event);
    void publishAssignment(AssignmentEventDTO event);
}

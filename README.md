# ARC Consolidator

Ce projet lit les événements bruts depuis les topics Kafka `iam.raw.*`, les consolide/normalise, puis publie les événements au format cible dans des topics dédiés par type d’objet métier (Identity, Entitlement, Assignment).

## Architecture hexagonale
Le code est organisé pour séparer la logique métier (domaine) des détails d’implémentation (infrastructure, frameworks, etc.).

### Structure principale
- `src/main/java/com/instantaccessprovider/arcconsolidator/`
    - `model/` : Modèles métier (ex : ArcEvent)
    - `dto/` : DTOs par topic (IdentityEventDTO, EntitlementEventDTO, AssignmentEventDTO)
    - `mapper/` : Mappers spécifiques par source (ActiveDirectoryEventMapper, IsdsEventMapper, ...)
    - `application/service/` : Services métier par source (ArcConsolidatorService, IsdsConsolidatorService, ...)
    - `kafka/` :
        - Publishers (ArcEventPublisher)
        - Consumers par source (RawAdEventConsumer, RawIsdsEventConsumer, ...)
    - `port/` : Interfaces (ports) pour l’injection de dépendances
    - `Application.java` : Entrypoint Spring Boot
- `src/main/resources/`
    - `application.yml` : Configuration

### Fonctionnement
- Consomme les topics `iam.raw.*` (ex : `iam.raw.activedirectory`, `iam.raw.isds`, ...)
- Utilise un mapper spécifique à la source pour transformer chaque événement brut en un ou plusieurs DTOs métier
- Publie chaque DTO dans le topic cible correspondant (`arc.identity`, `arc.entitlement`, `arc.assignment`)

### Extensibilité
- Pour ajouter une nouvelle source (ex : ISDS, RACF) :
    - Créer un consumer dédié (ex : RawIsdsEventConsumer)
    - Créer un service métier dédié (ex : IsdsConsolidatorService)
    - Créer un mapper dédié (ex : IsdsEventMapper)
    - Réutiliser les DTOs existants ou en ajouter si besoin

### Tests
- Les tests unitaires du domaine sont dans `src/test/java/com/instantaccessprovider/arcconsolidator/domain/`
- Les tests d’intégration des adapters sont dans `src/test/java/com/instantaccessprovider/arcconsolidator/infrastructure/`

---

## Documentation

La documentation d'architecture de la plateforme est dans le dépôt [arc-docs](https://github.com/EtiennSalle/arc-docs) :

- [Modèle abstrait IAM](https://github.com/EtiennSalle/arc-docs/blob/main/docs/00-modele-abstrait.md) : entités Identity / Entitlement / Assignment, catalogue d'événements rc.sync.*, formats CloudEvents et correspondances par source.
- [Architecture globale](https://github.com/EtiennSalle/arc-docs/blob/main/docs/02-architecture-globale.md).

Le déploiement (Helm, Flux, CI/CD) est documenté dans [arc-kubeorchestrator](https://github.com/arckit-iam/arc-kubeorchestrator/tree/main/docs).

## Licence

Copyright © 2026 Dédale Solutions

Licensed under the EUPL

Ce projet est distribué sous la licence [EUPL-1.2](LICENSE) (European Union Public Licence v. 1.2).

---

À compléter selon les besoins métier (enrichissement, gestion d’erreurs, tests, etc.)

